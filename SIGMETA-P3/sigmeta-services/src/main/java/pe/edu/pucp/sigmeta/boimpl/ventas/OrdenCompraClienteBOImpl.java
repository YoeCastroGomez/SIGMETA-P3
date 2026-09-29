package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.CotizacionBO;
import pe.edu.pucp.sigmeta.bo.ventas.OrdenCompraClienteBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.CotizacionBOImpl;
import pe.edu.pucp.sigmeta.dao.maestros.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleOrdenCompraClienteDAO;
import pe.edu.pucp.sigmeta.dao.ventas.OrdenCompraClienteDAO;
import pe.edu.pucp.sigmeta.daoimpl.maestros.ClienteDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.DetalleOrdenCompraClienteDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.OrdenCompraClienteDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoCotizacion;
import pe.edu.pucp.sigmeta.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleCotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdenCompraClienteBOImpl implements OrdenCompraClienteBO {

    private final OrdenCompraClienteDAO ordenCompraClienteDAO;
    private final DetalleOrdenCompraClienteDAO detalleOrdenCompraClienteDAO;
    private final ClienteDAO clienteDAO;
    private final CotizacionBO cotizacionBO;
    private final UsuarioBO usuarioBO;

    public OrdenCompraClienteBOImpl() {
        this.ordenCompraClienteDAO = new OrdenCompraClienteDAOImpl();
        this.detalleOrdenCompraClienteDAO = new DetalleOrdenCompraClienteDAOImpl();
        this.clienteDAO = new ClienteDAOImpl();
        this.cotizacionBO = new CotizacionBOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF007: Generar orden de compra desde cotizacion aceptada
    @Override
    public OrdenCompraCliente generarDesdeCotizacion(int idCotizacion, String numeroOrdenCliente, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);

        // Lectura de cotizacion previa antes de abrir transaccion de escritura
        Cotizacion cotizacion = cotizacionBO.obtener(idCotizacion);
        if (cotizacion == null) {
            throw new IllegalArgumentException("No existe la cotizacion con id " + idCotizacion);
        }
        if (cotizacion.isAnulado()) {
            throw new IllegalStateException("La cotizacion se encuentra anulada");
        }
        if (cotizacion.getEstado() != EstadoCotizacion.ACEPTADA) {
            throw new IllegalStateException("La cotizacion debe estar en estado ACEPTADA (estado actual: " + cotizacion.getEstado() + ")");
        }
        if (cotizacion.getDetalles() == null || cotizacion.getDetalles().isEmpty()) {
            throw new IllegalStateException("La cotizacion no tiene lineas de detalle");
        }

        try {
            OrdenCompraCliente existente = ordenCompraClienteDAO.buscarPorCotizacion(idCotizacion);
            if (existente != null) {
                throw new IllegalStateException("Ya existe una orden de compra activa para la cotizacion " + idCotizacion);
            }

            // RF003: El cliente debe estar activo
            Cliente cliente = clienteDAO.load(cotizacion.getCliente().getId());
            if (cliente == null || !cliente.isEstado()) {
                throw new IllegalStateException("El cliente de la cotizacion esta inactivo o no existe");
            }

            OrdenCompraCliente oc = new OrdenCompraCliente();
            oc.setCliente(cliente);
            oc.setCotizacion(cotizacion);
            oc.setNumeroOrdenCliente(Validador.textoOpcional(numeroOrdenCliente, "numero de orden del cliente", 30));
            oc.setEstado(EstadoOrdenCompraCliente.PENDIENTE);

            int totalExistentes = ordenCompraClienteDAO.listarTodos().size();
            oc.setNumero("OCC-" + LocalDate.now().getYear() + "-" + String.format("%04d", totalExistentes + 1));
            oc.setFechaEmision(LocalDate.now());
            oc.setMoneda(Moneda.SOLES);
            oc.setSubTotal(cotizacion.getSubTotal());
            oc.setIgv(cotizacion.getIgv());
            oc.setTotal(cotizacion.getTotal());
            oc.setObservaciones("Orden generada a partir de Cotizacion " + cotizacion.getNumero());
            oc.setUsuarioRegistro(responsable);
            oc.setFechaRegistro(LocalDateTime.now());
            oc.setAnulado(false);

            List<DetalleOrdenCompraCliente> detalles = new ArrayList<>();
            for (DetalleCotizacion dc : cotizacion.getDetalles()) {
                DetalleOrdenCompraCliente doc = new DetalleOrdenCompraCliente();
                doc.setOrdenCompraCliente(oc);
                doc.setNumeroLinea(dc.getNumeroLinea());
                doc.setProducto(dc.getProducto());
                doc.setCantidad(dc.getCantidad());
                doc.setPrecioUnitario(dc.getPrecioUnitario());
                doc.setDescuento(dc.getDescuento());
                doc.setImporte(dc.getImporte());
                doc.setCantidadAtendida(0.0);
                detalles.add(doc);
            }
            oc.setDetalles(detalles);

            ordenCompraClienteDAO.save(oc);
            for (DetalleOrdenCompraCliente doc : detalles) {
                doc.setOrdenCompraCliente(oc);
                detalleOrdenCompraClienteDAO.save(doc);
            }

            transactionContext.commit();
            return oc;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public OrdenCompraCliente modificar(OrdenCompraCliente oc, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        Validador.obligatorio(oc, "orden de compra");
        try {
            OrdenCompraCliente actual = ordenCompraClienteDAO.load(oc.getId());
            if (actual == null) {
                throw new IllegalArgumentException("No existe la orden de compra con id " + oc.getId());
            }
            if (actual.isAnulado()) {
                throw new IllegalStateException("No se puede modificar una orden de compra anulada");
            }
            if (actual.getEstado() != EstadoOrdenCompraCliente.PENDIENTE) {
                throw new IllegalStateException("Solo se puede modificar una orden de compra en estado PENDIENTE");
            }

            actual.setNumeroOrdenCliente(Validador.textoOpcional(oc.getNumeroOrdenCliente(), "numero de orden del cliente", 30));
            actual.setObservaciones(Validador.textoOpcional(oc.getObservaciones(), "observaciones", 300));

            ordenCompraClienteDAO.update(actual);
            transactionContext.commit();
            return actual;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public void anular(int idOrden, String motivo, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.ADMINISTRADOR);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo de anulacion", 200);
        try {
            OrdenCompraCliente actual = ordenCompraClienteDAO.load(idOrden);
            if (actual == null) {
                throw new IllegalArgumentException("No existe la orden de compra con id " + idOrden);
            }
            if (actual.isAnulado()) {
                throw new IllegalStateException("La orden de compra ya esta anulada");
            }
            if (actual.getEstado() != EstadoOrdenCompraCliente.PENDIENTE) {
                throw new IllegalStateException("Solo se puede anular una orden de compra en estado PENDIENTE");
            }

            actual.setMotivoAnulacion(motivoValidado);
            ordenCompraClienteDAO.remove(actual);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public OrdenCompraCliente obtener(int id) throws SQLException {
        try {
            OrdenCompraCliente oc = ordenCompraClienteDAO.load(id);
            if (oc != null) {
                List<DetalleOrdenCompraCliente> detalles = detalleOrdenCompraClienteDAO.listarPorOrden(id);
                for (DetalleOrdenCompraCliente d : detalles) {
                    d.setOrdenCompraCliente(oc);
                }
                oc.setDetalles(detalles);
            }
            return oc;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<OrdenCompraCliente> listarTodos() throws SQLException {
        try {
            return ordenCompraClienteDAO.listarTodos();
        } finally {
            transactionContext.close();
        }
    }
}
