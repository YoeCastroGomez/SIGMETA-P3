package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.VentaBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.maestros.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.maestros.ClienteDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VentaBOImpl implements VentaBO {

    private static final BigDecimal TASA_IGV = new BigDecimal("0.18");

    private final VentaDAO ventaDAO;
    private final DetalleVentaDAO detalleVentaDAO;
    private final OrdenCompraClienteDAO ordenCompraClienteDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final DespachoDAO despachoDAO;
    private final ProductoDAO productoDAO;
    private final ClienteDAO clienteDAO;
    private final UsuarioBO usuarioBO;

    public VentaBOImpl() {
        this.ventaDAO = new VentaDAOImpl();
        this.detalleVentaDAO = new DetalleVentaDAOImpl();
        this.ordenCompraClienteDAO = new OrdenCompraClienteDAOImpl();
        this.comprobanteDAO = new ComprobanteDAOImpl();
        this.despachoDAO = new DespachoDAOImpl();
        this.productoDAO = new ProductoDAOImpl();
        this.clienteDAO = new ClienteDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF008: Registro de venta (directa o desde orden de compra)
    @Override
    public Venta registrar(Venta venta, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        validarCabecera(venta);
        validarDetalles(venta.getDetalles());

        try {
            // RF003: Cliente activo
            Cliente cliente = clienteDAO.load(venta.getCliente().getId());
            if (cliente == null || !cliente.isEstado()) {
                throw new IllegalArgumentException("El cliente " + venta.getCliente().getId() + " no existe o esta inactivo");
            }
            venta.setCliente(cliente);

            // Validar si viene de orden de compra
            if (venta.getOrdenCompraCliente() != null && venta.getOrdenCompraCliente().getId() > 0) {
                OrdenCompraCliente oc = ordenCompraClienteDAO.load(venta.getOrdenCompraCliente().getId());
                if (oc == null || oc.isAnulado() || (oc.getEstado() != EstadoOrdenCompraCliente.PENDIENTE && oc.getEstado() != EstadoOrdenCompraCliente.ATENDIDA_PARCIAL)) {
                    throw new IllegalStateException("La orden de compra no esta vigente o no admite ventas");
                }
                venta.setOrdenCompraCliente(oc);
            }

            // Validar existencia de productos y stock disponible (RF008)
            verificarProductosYStock(venta.getDetalles());

            // Calcular totales
            calcularTotales(venta);

            // Validar condiciones de credito (RF008, RF009)
            if (venta.getCondicionPago() == CondicionPago.CREDITO) {
                if (cliente.getCondicionPago() != CondicionPago.CREDITO || cliente.getLimiteCredito() <= 0 || cliente.getPlazoCreditoDias() <= 0) {
                    throw new IllegalStateException("El cliente no tiene linea de credito autorizada");
                }
                if (venta.getPlazoCreditoDias() <= 0) {
                    venta.setPlazoCreditoDias(cliente.getPlazoCreditoDias());
                }
                double saldoPendiente = ventaDAO.obtenerSaldoPendienteCliente(cliente.getId());
                if (saldoPendiente + venta.getTotal() > cliente.getLimiteCredito() + 0.001) {
                    throw new IllegalStateException("La venta supera el limite de credito del cliente (Saldo: "
                            + saldoPendiente + ", Total: " + venta.getTotal() + ", Limite: " + cliente.getLimiteCredito() + ")");
                }
            } else {
                venta.setPlazoCreditoDias(0);
            }

            if (venta.getNumero() == null || venta.getNumero().isBlank()) {
                int count = ventaDAO.listarTodos().size();
                venta.setNumero("VTA-" + LocalDate.now().getYear() + "-" + String.format("%04d", count + 1));
            }

            venta.setEstado(EstadoVenta.REGISTRADA);
            venta.setAnulado(false);
            venta.setUsuarioRegistro(responsable);
            venta.setFechaRegistro(LocalDateTime.now());

            ventaDAO.save(venta);
            guardarDetalles(venta);

            // Si es a credito, generar la cuenta por cobrar asociada (RF009)
            if (venta.getCondicionPago() == CondicionPago.CREDITO) {
                LocalDate fechaVenc = venta.getFechaEmision().plusDays(venta.getPlazoCreditoDias());
                ventaDAO.generarCuentaPorCobrar(venta.getId(), cliente.getId(), venta.getFechaEmision(), fechaVenc, venta.getMoneda(), venta.getTotal());
            }

            transactionContext.commit();
            return venta;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Venta modificar(Venta venta, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        validarCabecera(venta);
        validarDetalles(venta.getDetalles());

        try {
            Venta actual = ventaDAO.load(venta.getId());
            if (actual == null) {
                throw new IllegalArgumentException("No existe la venta con id " + venta.getId());
            }
            if (actual.isAnulado() || actual.getEstado() != EstadoVenta.REGISTRADA) {
                throw new IllegalStateException("Solo se puede modificar una venta en estado REGISTRADA");
            }

            // Verificar que no tenga comprobantes ni despachos
            if (!comprobanteDAO.listarPorVenta(venta.getId()).isEmpty()) {
                throw new IllegalStateException("No se puede modificar una venta que ya tiene comprobantes generados");
            }
            if (!despachoDAO.listarPorVenta(venta.getId()).isEmpty()) {
                throw new IllegalStateException("No se puede modificar una venta que ya tiene despachos registrados");
            }

            Cliente cliente = clienteDAO.load(venta.getCliente().getId());
            if (cliente == null || !cliente.isEstado()) {
                throw new IllegalArgumentException("El cliente esta inactivo o no existe");
            }

            verificarProductosYStock(venta.getDetalles());
            calcularTotales(venta);

            venta.setEstado(actual.getEstado());
            venta.setAnulado(false);
            venta.setUsuarioRegistro(actual.getUsuarioRegistro());
            venta.setFechaRegistro(actual.getFechaRegistro());

            ventaDAO.update(venta);
            reemplazarDetalles(venta);

            transactionContext.commit();
            return venta;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public void anular(int idVenta, String motivo, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.ADMINISTRADOR);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo de anulacion", 200);

        try {
            Venta venta = ventaDAO.load(idVenta);
            if (venta == null) {
                throw new IllegalArgumentException("No existe la venta con id " + idVenta);
            }
            if (venta.isAnulado()) {
                throw new IllegalStateException("La venta ya se encuentra anulada");
            }

            // Verificar si tiene comprobantes vigentes
            if (comprobanteDAO.listarPorVenta(idVenta).stream().anyMatch(c -> c.getEstado() != pe.edu.pucp.sigmeta.model.enums.EstadoComprobante.ANULADO)) {
                throw new IllegalStateException("No se puede anular una venta con comprobantes vigentes");
            }

            // Verificar si tiene despachos vigentes
            if (despachoDAO.listarPorVenta(idVenta).stream().anyMatch(d -> !d.isAnulado())) {
                throw new IllegalStateException("No se puede anular una venta con despachos vigentes");
            }

            venta.setMotivoAnulacion(motivoValidado);
            ventaDAO.remove(venta);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Venta obtener(int id) throws SQLException {
        try {
            Venta venta = ventaDAO.load(id);
            if (venta != null) {
                List<DetalleVenta> detalles = detalleVentaDAO.listarPorVenta(id);
                for (DetalleVenta d : detalles) {
                    d.setVenta(venta);
                }
                venta.setDetalles(detalles);
            }
            return venta;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<Venta> listarTodos() throws SQLException {
        try {
            return ventaDAO.listarTodos();
        } finally {
            transactionContext.close();
        }
    }

    private void validarCabecera(Venta venta) {
        Validador.obligatorio(venta, "venta");
        Validador.obligatorio(venta.getCliente(), "cliente");
        Validador.obligatorio(venta.getFechaEmision(), "fecha de emision");
        Validador.obligatorio(venta.getCondicionPago(), "condicion de pago");
        venta.setMoneda(Moneda.SOLES);
        venta.setObservaciones(Validador.textoOpcional(venta.getObservaciones(), "observaciones", 300));
    }

    private void validarDetalles(List<DetalleVenta> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La venta debe tener al menos un detalle");
        }
        Set<Integer> lineas = new HashSet<>();
        for (DetalleVenta d : detalles) {
            Validador.obligatorio(d, "detalle de venta");
            Validador.obligatorio(d.getProducto(), "producto");
            if (d.getNumeroLinea() <= 0 || !lineas.add(d.getNumeroLinea())) {
                throw new IllegalArgumentException("Los numeros de linea deben ser positivos y unicos");
            }
            if (!Double.isFinite(d.getCantidad()) || d.getCantidad() <= 0
                    || !Double.isFinite(d.getPrecioUnitario()) || d.getPrecioUnitario() <= 0
                    || !Double.isFinite(d.getDescuento()) || d.getDescuento() < 0) {
                throw new IllegalArgumentException("La linea " + d.getNumeroLinea()
                        + " debe tener cantidad y precio mayores a 0 y descuento no negativo");
            }
        }
    }

    private void verificarProductosYStock(List<DetalleVenta> detalles) throws SQLException {
        for (DetalleVenta d : detalles) {
            Producto producto = productoDAO.load(d.getProducto().getId());
            if (producto == null || !producto.isEstado()) {
                throw new IllegalArgumentException("El producto " + d.getProducto().getId() + " no existe o esta inactivo");
            }
            if (producto.getStockActual() < d.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente para el producto " + producto.getNombre()
                        + " (Stock actual: " + producto.getStockActual() + ", Solicitado: " + d.getCantidad() + ")");
            }
        }
    }

    private void calcularTotales(Venta venta) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (DetalleVenta d : venta.getDetalles()) {
            BigDecimal bruto = BigDecimal.valueOf(d.getCantidad())
                    .multiply(BigDecimal.valueOf(d.getPrecioUnitario()));
            BigDecimal importe = bruto.subtract(BigDecimal.valueOf(d.getDescuento()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (importe.signum() < 0) {
                throw new IllegalArgumentException("El descuento de la linea " + d.getNumeroLinea()
                        + " supera el importe de la linea");
            }
            d.setImporte(importe.doubleValue());
            subTotal = subTotal.add(importe);
        }
        BigDecimal igv = subTotal.multiply(TASA_IGV).setScale(2, RoundingMode.HALF_UP);
        venta.setSubTotal(subTotal.doubleValue());
        venta.setIgv(igv.doubleValue());
        venta.setTotal(subTotal.add(igv).doubleValue());
    }

    private void guardarDetalles(Venta venta) throws SQLException {
        for (DetalleVenta d : venta.getDetalles()) {
            d.setVenta(venta);
            d.setCantidadDespachada(0.0);
            detalleVentaDAO.save(d);
        }
    }

    private void reemplazarDetalles(Venta venta) throws SQLException {
        Map<Integer, DetalleVenta> anteriores = new HashMap<>();
        for (DetalleVenta anterior : detalleVentaDAO.listarPorVenta(venta.getId())) {
            anteriores.put(anterior.getNumeroLinea(), anterior);
        }
        for (DetalleVenta d : venta.getDetalles()) {
            d.setVenta(venta);
            DetalleVenta anterior = anteriores.remove(d.getNumeroLinea());
            if (anterior != null) {
                d.setId(anterior.getId());
                detalleVentaDAO.update(d);
            } else {
                detalleVentaDAO.save(d);
            }
        }
        for (DetalleVenta quitada : anteriores.values()) {
            detalleVentaDAO.remove(quitada);
        }
    }
}
