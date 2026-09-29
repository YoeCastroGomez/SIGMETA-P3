package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.CotizacionBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.maestros.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.CotizacionDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleCotizacionDAO;
import pe.edu.pucp.sigmeta.daoimpl.maestros.ClienteDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.CotizacionDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.DetalleCotizacionDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoCotizacion;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleCotizacion;
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

public class CotizacionBOImpl implements CotizacionBO {

    private static final BigDecimal TASA_IGV = new BigDecimal("0.18");

    private final CotizacionDAO cotizacionDAO;
    private final DetalleCotizacionDAO detalleCotizacionDAO;
    private final ProductoDAO productoDAO;
    private final ClienteDAO clienteDAO;
    private final UsuarioBO usuarioBO;

    public CotizacionBOImpl() {
        this.cotizacionDAO = new CotizacionDAOImpl();
        this.detalleCotizacionDAO = new DetalleCotizacionDAOImpl();
        this.productoDAO = new ProductoDAOImpl();
        this.clienteDAO = new ClienteDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF007, RNF001 (usuario y fecha de registro), RNF002 (cabecera y detalle en una transaccion)
    @Override
    public Cotizacion registrar(Cotizacion cotizacion, int idResponsable) throws SQLException {
        Usuario responsable = verificarPermiso(idResponsable);
        validarCabecera(cotizacion);
        validarDetalles(cotizacion.getDetalles());
        verificarClienteActivo(cotizacion.getCliente().getId());

        cotizacion.setEstado(EstadoCotizacion.REGISTRADA);
        cotizacion.setAnulado(false);
        cotizacion.setMotivoAnulacion(null);
        cotizacion.setFechaAnulacion(null);
        cotizacion.setUsuarioRegistro(responsable);
        cotizacion.setFechaRegistro(LocalDateTime.now());
        try {
            verificarProductosActivos(cotizacion.getDetalles());
            calcularTotales(cotizacion);
            cotizacionDAO.save(cotizacion);
            guardarDetalles(cotizacion);
            transactionContext.commit();
            return cotizacion;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF007
    @Override
    public Cotizacion modificar(Cotizacion cotizacion, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validarCabecera(cotizacion);
        validarDetalles(cotizacion.getDetalles());
        verificarClienteActivo(cotizacion.getCliente().getId());
        try {
            Cotizacion actual = obtenerExistente(cotizacion.getId());
            if (actual.isAnulado() || actual.getEstado() != EstadoCotizacion.REGISTRADA) {
                throw new IllegalStateException("Solo se puede modificar una cotizacion en estado REGISTRADA");
            }
            verificarProductosActivos(cotizacion.getDetalles());
            // los datos de auditoria y el estado no se cambian desde la edicion
            cotizacion.setEstado(actual.getEstado());
            cotizacion.setAnulado(false);
            cotizacion.setUsuarioRegistro(actual.getUsuarioRegistro());
            cotizacion.setFechaRegistro(actual.getFechaRegistro());
            calcularTotales(cotizacion);

            cotizacionDAO.update(cotizacion);
            reemplazarDetalles(cotizacion);
            transactionContext.commit();
            return cotizacion;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF007, RNF002: anulacion logica con motivo
    @Override
    public void anular(int idCotizacion, String motivo, int idResponsable) throws SQLException {
        // el Administrador tambien gestiona anulaciones (perfil del actor)
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.ADMINISTRADOR);
        String motivoAnulacion = Validador.textoObligatorio(motivo, "motivo de anulacion", 255);
        try {
            Cotizacion cotizacion = obtenerExistente(idCotizacion);
            if (cotizacion.isAnulado()) {
                throw new IllegalStateException("La cotizacion ya esta anulada");
            }
            if (cotizacion.getEstado() == EstadoCotizacion.ACEPTADA) {
                throw new IllegalStateException("No se puede anular una cotizacion aceptada");
            }
            cotizacion.setMotivoAnulacion(motivoAnulacion);
            cotizacionDAO.remove(cotizacion);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF007
    @Override
    public Cotizacion obtener(int idCotizacion) throws SQLException {
        try {
            Cotizacion cotizacion = cotizacionDAO.load(idCotizacion);
            if (cotizacion != null) {
                List<DetalleCotizacion> detalles = detalleCotizacionDAO.listar_por_cotizacion(idCotizacion);
                for (DetalleCotizacion detalle : detalles) {
                    detalle.setCotizacion(cotizacion);
                }
                cotizacion.setDetalles(detalles);
            }
            return cotizacion;
        } finally {
            transactionContext.close();
        }
    }

    // RF007: solo cotizaciones no anuladas
    @Override
    public List<Cotizacion> listarTodos() throws SQLException {
        try {
            return cotizacionDAO.listarTodos();
        } finally {
            transactionContext.close();
        }
    }

    // RF007
    @Override
    public List<Cotizacion> listarPorCliente(int idCliente) throws SQLException {
        try {
            return cotizacionDAO.listarTodos().stream()
                    .filter(c -> c.getCliente().getId() == idCliente)
                    .toList();
        } finally {
            transactionContext.close();
        }
    }

    // RF007
    @Override
    public List<Cotizacion> listarPorEstado(EstadoCotizacion estado) throws SQLException {
        Validador.obligatorio(estado, "estado");
        try {
            return cotizacionDAO.listarTodos().stream()
                    .filter(c -> c.getEstado() == estado)
                    .toList();
        } finally {
            transactionContext.close();
        }
    }

    // RF007
    @Override
    public Cotizacion enviar(int idCotizacion, int idResponsable) throws SQLException {
        return cambiarEstado(idCotizacion, idResponsable, EstadoCotizacion.REGISTRADA, EstadoCotizacion.ENVIADA);
    }

    // RF007
    @Override
    public Cotizacion aceptar(int idCotizacion, int idResponsable) throws SQLException {
        return cambiarEstado(idCotizacion, idResponsable, EstadoCotizacion.ENVIADA, EstadoCotizacion.ACEPTADA);
    }

    // RF007
    @Override
    public Cotizacion rechazar(int idCotizacion, int idResponsable) throws SQLException {
        return cambiarEstado(idCotizacion, idResponsable, EstadoCotizacion.ENVIADA, EstadoCotizacion.RECHAZADA);
    }

    // RF007
    @Override
    public int marcarVencidas() throws SQLException {
        LocalDate hoy = LocalDate.now();
        int vencidas = 0;
        try {
            for (Cotizacion cotizacion : cotizacionDAO.listarTodos()) {
                boolean pendiente = cotizacion.getEstado() == EstadoCotizacion.REGISTRADA
                        || cotizacion.getEstado() == EstadoCotizacion.ENVIADA;
                if (pendiente && cotizacion.getFechaVigencia() != null && cotizacion.getFechaVigencia().isBefore(hoy)) {
                    cotizacion.setEstado(EstadoCotizacion.VENCIDA);
                    cotizacionDAO.update(cotizacion);
                    vencidas++;
                }
            }
            transactionContext.commit();
            return vencidas;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    private Cotizacion cambiarEstado(int idCotizacion, int idResponsable,
                                     EstadoCotizacion estadoRequerido, EstadoCotizacion estadoNuevo) throws SQLException {
        verificarPermiso(idResponsable);
        try {
            Cotizacion cotizacion = obtenerExistente(idCotizacion);
            if (cotizacion.isAnulado() || cotizacion.getEstado() != estadoRequerido) {
                throw new IllegalStateException("La cotizacion debe estar " + estadoRequerido
                        + " para pasar a " + estadoNuevo + " (estado actual: " + cotizacion.getEstado() + ")");
            }
            if (estadoNuevo == EstadoCotizacion.ACEPTADA && cotizacion.getFechaVigencia().isBefore(LocalDate.now())) {
                throw new IllegalStateException("La cotizacion vencio el " + cotizacion.getFechaVigencia());
            }
            cotizacion.setEstado(estadoNuevo);
            cotizacionDAO.update(cotizacion);
            transactionContext.commit();
            return cotizacion;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF007: la cotizacion la gestiona el Vendedor
    private Usuario verificarPermiso(int idResponsable) throws SQLException {
        return usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
    }

    private Cotizacion obtenerExistente(int idCotizacion) throws SQLException {
        Cotizacion cotizacion = cotizacionDAO.load(idCotizacion);
        if (cotizacion == null) {
            throw new IllegalArgumentException("No existe la cotizacion con id " + idCotizacion);
        }
        return cotizacion;
    }

    // RF003: un cliente desactivado no puede seleccionarse en nuevos documentos
    private void verificarClienteActivo(int idCliente) throws SQLException {
        Cliente cliente = clienteDAO.load(idCliente);
        if (cliente == null || !cliente.isEstado()) {
            throw new IllegalArgumentException("El cliente " + idCliente + " no existe o esta desactivado");
        }
    }

    private void verificarProductosActivos(List<DetalleCotizacion> detalles) throws SQLException {
        for (DetalleCotizacion detalle : detalles) {
            // RF005: un producto desactivado no puede usarse en documentos nuevos
            Producto producto = productoDAO.load(detalle.getProducto().getId());
            if (producto == null || !producto.isEstado()) {
                throw new IllegalArgumentException("El producto " + detalle.getProducto().getId()
                        + " no existe o esta desactivado");
            }
        }
    }

    // RNF002: las lineas que siguen se actualizan en su lugar (conservan su id) y las nuevas se insertan.
    // detalle_cotizacion no tiene columna de baja logica: una linea quitada solo puede borrarse fisicamente,
    // y eso se permite unicamente mientras la cotizacion esta REGISTRADA (borrador).
    private void reemplazarDetalles(Cotizacion cotizacion) throws SQLException {
        Map<Integer, DetalleCotizacion> anteriores = new HashMap<>();
        for (DetalleCotizacion anterior : detalleCotizacionDAO.listar_por_cotizacion(cotizacion.getId())) {
            anteriores.put(anterior.getNumeroLinea(), anterior);
        }
        for (DetalleCotizacion detalle : cotizacion.getDetalles()) {
            detalle.setCotizacion(cotizacion);
            DetalleCotizacion anterior = anteriores.remove(detalle.getNumeroLinea());
            if (anterior != null) {
                detalle.setId(anterior.getId());
                detalleCotizacionDAO.update(detalle);
            } else {
                detalleCotizacionDAO.save(detalle);
            }
        }
        for (DetalleCotizacion quitada : anteriores.values()) {
            detalleCotizacionDAO.remove(quitada);
        }
    }

    private void guardarDetalles(Cotizacion cotizacion) throws SQLException {
        for (DetalleCotizacion detalle : cotizacion.getDetalles()) {
            detalle.setCotizacion(cotizacion);
            detalleCotizacionDAO.save(detalle);
        }
    }

    // RF007: subtotal, IGV y total se calculan aqui; no se confia en los que manda la interfaz
    private void calcularTotales(Cotizacion cotizacion) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (DetalleCotizacion detalle : cotizacion.getDetalles()) {
            BigDecimal bruto = BigDecimal.valueOf(detalle.getCantidad())
                    .multiply(BigDecimal.valueOf(detalle.getPrecioUnitario()));
            BigDecimal importe = bruto.subtract(BigDecimal.valueOf(detalle.getDescuento()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (importe.signum() < 0) {
                throw new IllegalArgumentException("El descuento de la linea " + detalle.getNumeroLinea()
                        + " supera el importe de la linea");
            }
            detalle.setImporte(importe.doubleValue());
            subTotal = subTotal.add(importe);
        }
        BigDecimal igv = subTotal.multiply(TASA_IGV).setScale(2, RoundingMode.HALF_UP);
        cotizacion.setSubTotal(subTotal.doubleValue());
        cotizacion.setIgv(igv.doubleValue());
        cotizacion.setTotal(subTotal.add(igv).doubleValue());
    }

    private void validarCabecera(Cotizacion cotizacion) {
        Validador.obligatorio(cotizacion, "cotizacion");
        Validador.obligatorio(cotizacion.getCliente(), "cliente");
        Validador.obligatorio(cotizacion.getFechaEmision(), "fecha de emision");
        Validador.obligatorio(cotizacion.getFechaVigencia(), "fecha de vigencia");
        // RF007: los importes de la cotizacion se expresan en soles
        cotizacion.setMoneda(Moneda.SOLES);
        cotizacion.setNumero(Validador.textoObligatorio(cotizacion.getNumero(), "numero", 20));
        cotizacion.setObservaciones(Validador.textoOpcional(cotizacion.getObservaciones(), "observaciones", 255));
        if (cotizacion.getFechaVigencia().isBefore(cotizacion.getFechaEmision())) {
            throw new IllegalArgumentException("La fecha de vigencia no puede ser anterior a la fecha de emision");
        }
    }

    private void validarDetalles(List<DetalleCotizacion> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La cotizacion debe tener al menos un detalle");
        }
        Set<Integer> lineas = new HashSet<>();
        for (DetalleCotizacion detalle : detalles) {
            Validador.obligatorio(detalle, "detalle de cotizacion");
            Validador.obligatorio(detalle.getProducto(), "producto");
            if (detalle.getNumeroLinea() <= 0 || !lineas.add(detalle.getNumeroLinea())) {
                throw new IllegalArgumentException("Los numeros de linea deben ser positivos y unicos");
            }
            if (!Double.isFinite(detalle.getCantidad())
                    || !Double.isFinite(detalle.getPrecioUnitario())
                    || !Double.isFinite(detalle.getDescuento())
                    || detalle.getCantidad() <= 0
                    || detalle.getPrecioUnitario() <= 0
                    || detalle.getDescuento() < 0) {
                throw new IllegalArgumentException("La linea " + detalle.getNumeroLinea()
                        + " debe tener cantidad y precio mayores a 0 y descuento no negativo");
            }
        }
    }
}
