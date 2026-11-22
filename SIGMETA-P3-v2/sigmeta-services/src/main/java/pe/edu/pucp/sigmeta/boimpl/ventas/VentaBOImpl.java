package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.dao.cobranza.CobroDAO;
import pe.edu.pucp.sigmeta.dao.cobranza.CuentaPorCobrarDAO;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CobroDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.VentaBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.socio.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.almacen.DespachoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.socio.ClienteDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.DespachoDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VentaBOImpl implements VentaBO {

    // RF008: una venta se registra con la fecha del dia o, como mucho, con algunos dias de atraso (venta tomada
    // fuera del sistema). Nunca a futuro ni con mas atraso: la cuenta por cobrar naceria vencida y el comprobante,
    // que se emite con la fecha de hoy, no coincidiria con la venta.
    static final int DIAS_ATRASO_MAXIMO = 7;

    private final VentaDAO ventaDAO;
    private final DetalleVentaDAO detalleVentaDAO;
    private final OrdenCompraClienteDAO ordenCompraClienteDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final DespachoDAO despachoDAO;
    private final ProductoDAO productoDAO;
    private final ClienteDAO clienteDAO;
    private final UsuarioBO usuarioBO;
    private final DetalleOrdenCompraClienteDAO detalleOrdenCompraClienteDAO;
    private final CuentaPorCobrarDAO cuentaPorCobrarDAO;
    private final CobroDAO cobroDAO;
    private final CompensacionCaja compensacionCaja;
    private final pe.edu.pucp.sigmeta.dao.ConsultaDAO consultaDAO = new pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl();
    private final pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO autorizacionBO =
            new pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl();
    // partes de esta clase (mismo paquete)
    private final VentaPrecios controlPrecios;
    private final VentaLineas calculoLineas;
    private final VentaCobranza cobranzaVenta;
    // partes de esta clase (mismo paquete)
    private final VentaEdicion edicionVenta;

    public VentaBOImpl() {
        this.ventaDAO = new VentaDAOImpl();
        this.detalleVentaDAO = new DetalleVentaDAOImpl();
        this.ordenCompraClienteDAO = new OrdenCompraClienteDAOImpl();
        this.comprobanteDAO = new ComprobanteDAOImpl();
        this.despachoDAO = new DespachoDAOImpl();
        this.productoDAO = new ProductoDAOImpl();
        this.clienteDAO = new ClienteDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
        this.detalleOrdenCompraClienteDAO = new DetalleOrdenCompraClienteDAOImpl();
        this.cuentaPorCobrarDAO = new CuentaPorCobrarDAOImpl();
        this.cobroDAO = new CobroDAOImpl();
        this.compensacionCaja = new CompensacionCaja();
        this.calculoLineas = new VentaLineas(ventaDAO, detalleVentaDAO, productoDAO, detalleOrdenCompraClienteDAO, consultaDAO);
        this.controlPrecios = new VentaPrecios(ventaDAO, ordenCompraClienteDAO, productoDAO, clienteDAO, usuarioBO, autorizacionBO, calculoLineas);
        this.cobranzaVenta = new VentaCobranza(ventaDAO, comprobanteDAO, despachoDAO, usuarioBO, cuentaPorCobrarDAO, compensacionCaja);
        this.edicionVenta = new VentaEdicion(ventaDAO, ordenCompraClienteDAO, comprobanteDAO, despachoDAO, clienteDAO, usuarioBO, consultaDAO, autorizacionBO, controlPrecios, calculoLineas, cobranzaVenta);
    }

    @Override
    public String proponerNumero(int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.ADMINISTRADOR);
        return Transaccion.consultar(() -> {
            int anio = LocalDate.now().getYear();
            return "VTA-" + anio + "-" + String.format("%04d", ventaDAO.proximoCorrelativo(anio));
        });
    }

    // RF008: Registro de venta (directa o desde orden de compra)
    @Override
    public Venta registrar(Venta venta, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        validarCabecera(venta);
        validarFechaEmision(venta.getFechaEmision());
        calculoLineas.validarDetalles(venta.getDetalles());
        if (venta.getId() != 0) throw new IllegalArgumentException("Una venta nueva no debe tener ID");

        return Transaccion.ejecutar(() -> {
            // RF003: Cliente activo
            Cliente cliente = clienteDAO.load(venta.getCliente().getId());
            if (cliente == null || !cliente.isEstado()) {
                throw new IllegalArgumentException("El cliente " + venta.getCliente().getId() + " no existe o está inactivo");
            }
            venta.setCliente(cliente);

            // Validar si viene de orden de compra
            if (venta.getOrdenCompraCliente() != null && venta.getOrdenCompraCliente().getId() > 0) {
                OrdenCompraCliente oc = ordenCompraClienteDAO.load(venta.getOrdenCompraCliente().getId());
                if (oc == null || oc.isAnulado() || (oc.getEstado() != EstadoOrdenCompraCliente.PENDIENTE && oc.getEstado() != EstadoOrdenCompraCliente.ATENDIDA_PARCIAL)) {
                    throw new IllegalStateException("La orden de compra no está vigente o no admite ventas");
                }
                if (oc.getCliente().getId() != cliente.getId()) {
                    throw new IllegalArgumentException("La orden pertenece a otro cliente");
                }
                venta.setOrdenCompraCliente(oc);
            }
            calculoLineas.enlazarLineasOrden(venta);
            calculoLineas.fijarPreciosYCantidades(venta.getDetalles());

            // Validar existencia de productos y stock disponible (RF008)
            calculoLineas.verificarProductosYStock(venta.getDetalles(), 0);

            // Calcular totales
            calculoLineas.calcularTotales(venta);

            // RF005/RF014: ninguna linea se vende bajo el costo sin autorizacion del Administrador
            Integer idAutorizacionPrecio = controlPrecios.verificarPrecios(venta, idResponsable);

            // Validar condiciones de credito (RF008, RF009)
            if (venta.getCondicionPago() == CondicionPago.CREDITO) {
                if (cliente.getCondicionPago() != CondicionPago.CREDITO || cliente.getLimiteCredito().signum() <= 0 || cliente.getPlazoCreditoDias() <= 0) {
                    throw new IllegalStateException("El cliente no tiene línea de crédito autorizada");
                }
                if (venta.getPlazoCreditoDias() <= 0) {
                    venta.setPlazoCreditoDias(cliente.getPlazoCreditoDias());
                }
                if (venta.getPlazoCreditoDias() > cliente.getPlazoCreditoDias()) {
                    throw new IllegalArgumentException("El plazo supera el máximo autorizado del cliente");
                }
                BigDecimal saldoPendiente = ventaDAO.obtenerSaldoPendienteCliente(cliente.getId());
                if (saldoPendiente.add(BigDecimal.valueOf(venta.getTotal())).compareTo(cliente.getLimiteCredito()) > 0) {
                    throw new IllegalStateException("La venta supera el límite de crédito del cliente (Saldo: "
                            + saldoPendiente + ", Total: " + venta.getTotal() + ", Límite: " + cliente.getLimiteCredito() + ")");
                }
            } else {
                venta.setPlazoCreditoDias(0);
            }

            if (venta.getNumero() == null || venta.getNumero().isBlank()) {
                int anio = LocalDate.now().getYear();
                long siguiente = ventaDAO.siguienteCorrelativo(anio);
                venta.setNumero("VTA-" + anio + "-" + String.format("%04d", siguiente));
            }

            venta.setEstado(EstadoVenta.REGISTRADA);
            venta.setAnulado(false);
            venta.setUsuarioRegistro(responsable);
            venta.setFechaRegistro(LocalDateTime.now());

            ventaDAO.save(venta);
            calculoLineas.guardarDetalles(venta);
            if (idAutorizacionPrecio != null) {
                autorizacionBO.consumir(idAutorizacionPrecio);   // un solo uso (RF014)
            }

            // Si es a credito, generar la cuenta por cobrar asociada (RF009)
            if (venta.getCondicionPago() == CondicionPago.CREDITO) {
                LocalDate fechaVenc = venta.getFechaEmision().plusDays(venta.getPlazoCreditoDias());
                ventaDAO.generarCuentaPorCobrar(venta.getId(), cliente.getId(), venta.getFechaEmision(), fechaVenc, venta.getTotal());
            }

            return venta;
        });
    }

    @Override
    public Venta modificar(Venta venta, int idResponsable) throws SQLException {
        return edicionVenta.modificar(venta, idResponsable);
    }

    @Override
    public void anular(int idVenta, String motivo, int idResponsable) throws SQLException {
        cobranzaVenta.anular(idVenta, motivo, idResponsable);
    }

    @Override
    public Venta obtener(int id) throws SQLException {
        return Transaccion.ejecutar(() -> {
            Venta venta = ventaDAO.load(id);
            if (venta != null) {
                List<DetalleVenta> detalles = detalleVentaDAO.listarPorVenta(id);
                for (DetalleVenta d : detalles) {
                    d.setVenta(venta);
                }
                venta.setDetalles(detalles);
            }
            return venta;
        });
    }

    @Override
    public List<Venta> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return ventaDAO.listarTodos();
        });
    }

    static void validarCabecera(Venta venta) {
        Validador.obligatorio(venta, "venta");
        Validador.obligatorio(venta.getCliente(), "cliente");
        Validador.obligatorio(venta.getFechaEmision(), "fecha de emisión");
        Validador.obligatorio(venta.getCondicionPago(), "condición de pago");
        venta.setObservaciones(Validador.textoOpcional(venta.getObservaciones(), "observaciones", 300));
    }

    static void validarFechaEmision(LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        if (fecha.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de emisión no puede ser posterior a hoy (" + hoy + ")");
        }
        if (fecha.isBefore(hoy.minusDays(DIAS_ATRASO_MAXIMO))) {
            throw new IllegalArgumentException("La fecha de emisión no puede tener más de " + DIAS_ATRASO_MAXIMO
                    + " días de antigüedad (mínimo " + hoy.minusDays(DIAS_ATRASO_MAXIMO) + ")");
        }
    }

    @Override
    public pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarDescuentoEspecial(Venta venta, int idResponsable) throws SQLException {
        return controlPrecios.solicitarDescuentoEspecial(venta, idResponsable);
    }

    // RF005/RF014: lineas bajo el costo mientras el Vendedor arma la venta (VentaPrecios)
    @Override
    public List<Map<String, Object>> revisarCosto(Venta venta, int idResponsable) throws SQLException {
        return controlPrecios.revisarCosto(venta, idResponsable);
    }

    @Override
    public List<Venta> listarPorCliente(int idCliente) throws SQLException {
        if (idCliente <= 0) throw new IllegalArgumentException("Cliente inválido");
        return Transaccion.ejecutar(() -> ventaDAO.listarPorCliente(idCliente));
    }

    // RNF003, panel de inicio: top de productos por importe de las últimas ventas vigentes, calculado en la base
    @Override
    public List<Map<String, Object>> topProductos(int ventas, int limite) throws SQLException {
        return Transaccion.consultar(() -> consultaDAO.consultar("sp_panel_top_productos",
                Math.max(1, ventas), Math.max(1, limite)));
    }

    // RF008/RF010: mismo criterio que la regla de modificar (sp_venta_contar_cobros_contado), en una sola consulta
    @Override
    public Set<Integer> idsCobradasAlContado() throws SQLException {
        return Transaccion.consultar(() -> {
            Set<Integer> ids = new HashSet<>();
            for (Map<String, Object> fila : consultaDAO.consultar("sp_venta_ids_cobradas_contado")) {
                if (fila.get("idVenta") instanceof Number id) {
                    ids.add(id.intValue());
                }
            }
            return ids;
        });
    }

}
