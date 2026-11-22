package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.boimpl.cobranza.CuentaPorCobrarBOImpl;
import pe.edu.pucp.sigmeta.dao.cobranza.CobroDAO;
import pe.edu.pucp.sigmeta.dao.cobranza.CuentaPorCobrarDAO;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CobroDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.ventas.DetalleOrdenCompraCliente;
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
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

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
        validarDetalles(venta.getDetalles());
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
            enlazarLineasOrden(venta);
            fijarPreciosYCantidades(venta.getDetalles());

            // Validar existencia de productos y stock disponible (RF008)
            verificarProductosYStock(venta.getDetalles(), 0);

            // Calcular totales
            calcularTotales(venta);

            // RF005/RF014: ninguna linea se vende bajo el costo sin autorizacion del Administrador
            Integer idAutorizacionPrecio = verificarPrecios(venta, idResponsable);

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
            guardarDetalles(venta);
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
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        validarCabecera(venta);
        validarDetalles(venta.getDetalles());

        return Transaccion.ejecutar(() -> {
            Venta actual = ventaDAO.load(venta.getId());
            if (actual == null) {
                throw new IllegalArgumentException("No existe la venta con id " + venta.getId());
            }
            if (actual.isAnulado() || actual.getEstado() != EstadoVenta.REGISTRADA) {
                throw new IllegalStateException("Solo se puede modificar una venta en estado REGISTRADA");
            }

            // Verificar que no tenga comprobantes ni despachos VIGENTES: si se anulo la factura (por ejemplo para
            // corregir un error) o el despacho, la venta vuelve a poder corregirse antes de facturarla de nuevo
            if (comprobanteDAO.listarPorVenta(venta.getId()).stream()
                    .anyMatch(c -> c.getEstado() != pe.edu.pucp.sigmeta.model.enums.EstadoComprobante.ANULADO)) {
                throw new IllegalStateException("No se puede modificar una venta con comprobante vigente; anule primero el comprobante");
            }
            if (despachoDAO.listarPorVenta(venta.getId()).stream().anyMatch(d -> !d.isAnulado())) {
                throw new IllegalStateException("No se puede modificar una venta que ya tiene despachos registrados");
            }
            // RF010: una venta al contado ya cobrada en caja no cambia su total (el cobro quedaria descuadrado)
            if (actual.getCondicionPago() == CondicionPago.CONTADO
                    && consultaDAO.numero("sp_venta_contar_cobros_contado", venta.getId()).signum() > 0) {
                throw new IllegalStateException("La venta ya se cobró en caja y no se puede modificar; "
                        + "si hay un error, anule la venta (el cobro se devuelve en caja) y registre otra");
            }

            Cliente cliente = clienteDAO.load(venta.getCliente().getId());
            if (cliente == null || !cliente.isEstado()) {
                throw new IllegalArgumentException("El cliente está inactivo o no existe");
            }

            // la condicion de pago y la orden de origen no se cambian al modificar
            venta.setCondicionPago(actual.getCondicionPago());
            venta.setPlazoCreditoDias(actual.getPlazoCreditoDias());
            if (actual.getCondicionPago() == CondicionPago.CREDITO && !venta.getFechaEmision().equals(actual.getFechaEmision())) {
                throw new IllegalArgumentException("No se cambia la fecha de emisión de una venta al crédito; refinancie su cuenta");
            }
            if (!venta.getFechaEmision().equals(actual.getFechaEmision())) {
                validarFechaEmision(venta.getFechaEmision());
            }
            venta.setOrdenCompraCliente(actual.getOrdenCompraCliente());
            if (venta.getOrdenCompraCliente() != null) {
                OrdenCompraCliente oc = ordenCompraClienteDAO.load(venta.getOrdenCompraCliente().getId());
                if (oc == null || oc.isAnulado() || oc.getCliente().getId() != cliente.getId()) {
                    throw new IllegalArgumentException("Orden anulada, inexistente o de otro cliente");
                }
                venta.setOrdenCompraCliente(oc);
            }
            if (actual.getCondicionPago() == CondicionPago.CREDITO && cliente.getId() != actual.getCliente().getId()) {
                throw new IllegalStateException("No se puede cambiar el cliente de una venta al crédito con cuenta por cobrar generada");
            }
            enlazarLineasOrden(venta);
            fijarPreciosYCantidades(venta.getDetalles());

            verificarProductosYStock(venta.getDetalles(), venta.getId());
            calcularTotales(venta);
            Integer idAutorizacionPrecio = verificarPrecios(venta, idResponsable);
            if (venta.getCondicionPago() == CondicionPago.CREDITO) {
                actualizarCuentaPorCobrar(venta, cliente);
            }

            venta.setEstado(actual.getEstado());
            venta.setAnulado(false);
            venta.setUsuarioRegistro(actual.getUsuarioRegistro());
            venta.setFechaRegistro(actual.getFechaRegistro());

            ventaDAO.update(venta);
            reemplazarDetalles(venta);
            if (idAutorizacionPrecio != null) {
                autorizacionBO.consumir(idAutorizacionPrecio);   // un solo uso (RF014)
            }

            return venta;
        });
    }

    @Override
    public void anular(int idVenta, String motivo, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.ADMINISTRADOR);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo de anulación", 200);

        Transaccion.ejecutar(() -> {
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

            // RF010: el cobro al contado de una venta anulada no es un cobro valido: se compensa en su caja
            for (MovimientoCaja cobro : compensacionCaja.buscar(CompensacionCaja.DOCUMENTO_VENTA, idVenta)) {
                compensacionCaja.compensar(cobro, "Devolución por anulación de la venta " + venta.getNumero(),
                        CompensacionCaja.DOCUMENTO_ANULACION_VENTA, idVenta, responsable);
            }
            // La cuenta solo se puede retirar despues de anular la venta, en la misma transaccion.
            venta.setMotivoAnulacion(motivoValidado);
            ventaDAO.remove(venta);
            if (venta.getCondicionPago() == CondicionPago.CREDITO) {
                cerrarCuentaPorCobrar(idVenta);
            }
            return null;
        });
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

    private void validarCabecera(Venta venta) {
        Validador.obligatorio(venta, "venta");
        Validador.obligatorio(venta.getCliente(), "cliente");
        Validador.obligatorio(venta.getFechaEmision(), "fecha de emisión");
        Validador.obligatorio(venta.getCondicionPago(), "condición de pago");
        venta.setObservaciones(Validador.textoOpcional(venta.getObservaciones(), "observaciones", 300));
    }

    private static void validarFechaEmision(LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        if (fecha.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de emisión no puede ser posterior a hoy (" + hoy + ")");
        }
        if (fecha.isBefore(hoy.minusDays(DIAS_ATRASO_MAXIMO))) {
            throw new IllegalArgumentException("La fecha de emisión no puede tener más de " + DIAS_ATRASO_MAXIMO
                    + " días de antigüedad (mínimo " + hoy.minusDays(DIAS_ATRASO_MAXIMO) + ")");
        }
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
                throw new IllegalArgumentException("Los números de línea deben ser positivos y únicos");
            }
            if (!Double.isFinite(d.getCantidad()) || d.getCantidad() <= 0
                    || !Double.isFinite(d.getPrecioUnitario()) || d.getPrecioUnitario() <= 0
                    || !Double.isFinite(d.getDescuento()) || d.getDescuento() < 0) {
                throw new IllegalArgumentException("La línea " + d.getNumeroLinea()
                        + " debe tener cantidad y precio mayores a 0 y descuento no negativo");
            }
        }
    }

    // RF005: el precio de lista lo fija el Administrador; la pantalla no decide el precio. Una linea que atiende una
    // orden del cliente se vende al precio de esa orden (el de la cotizacion aceptada); las demas, al precio de venta
    // vigente del producto. El Vendedor solo ajusta con descuento (ver verificarPrecios). Se llama despues de
    // enlazarLineasOrden. Tambien exige cantidades enteras en los productos que se venden por unidad.
    private void fijarPreciosYCantidades(List<DetalleVenta> detalles) throws SQLException {
        for (DetalleVenta d : detalles) {
            Producto producto = productoDAO.load(d.getProducto().getId());
            if (producto == null || !producto.isEstado()) {
                throw new IllegalArgumentException("El producto no existe o está inactivo: " + d.getProducto().getId());
            }
            UnidadesVenta.validarCantidad(producto, d.getCantidad(), d.getNumeroLinea());
            d.setPrecioUnitario(d.getDetalleOrdenCompraCliente() != null
                    ? d.getDetalleOrdenCompraCliente().getPrecioUnitario()
                    : producto.getPrecioVenta());
        }
    }

    // RF008: se vende contra el stock DISPONIBLE = stock fisico - stock comprometido, donde el comprometido es
    // lo ya vendido y todavia no despachado en otras ventas vigentes. Asi dos ventas no prometen las mismas
    // unidades. idVentaExcluida: la venta que se esta modificando (0 al registrar).
    private void verificarProductosYStock(List<DetalleVenta> detalles, int idVentaExcluida) throws SQLException {
        Map<Integer, BigDecimal> cantidades = new HashMap<>();
        for (DetalleVenta d : detalles) {
            cantidades.merge(d.getProducto().getId(), BigDecimal.valueOf(d.getCantidad()), BigDecimal::add);
        }
        for (var entrada : cantidades.entrySet()) {
            Producto producto = productoDAO.load(entrada.getKey());
            if (producto == null || !producto.isEstado()) {
                throw new IllegalArgumentException("El producto no existe o está inactivo: " + entrada.getKey());
            }
            BigDecimal comprometido = consultaDAO.numero("sp_stock_comprometido", producto.getId(), idVentaExcluida);
            BigDecimal disponible = BigDecimal.valueOf(producto.getStockActual()).subtract(comprometido);
            if (entrada.getValue().compareTo(disponible) > 0) {
                throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre()
                        + " (en almacén: " + sinCeros(BigDecimal.valueOf(producto.getStockActual()))
                        + ", comprometido en otras ventas: " + sinCeros(comprometido)
                        + ", disponible: " + sinCeros(disponible.max(BigDecimal.ZERO))
                        + ", solicitado: " + sinCeros(entrada.getValue()) + ")");
            }
        }
    }

    private static String sinCeros(BigDecimal numero) {
        return numero.stripTrailingZeros().toPlainString();
    }

    // RF005: el precio de lista lo fija el Administrador. El Vendedor puede dar descuento, pero el precio
    // efectivo de una linea (importe / cantidad, ya con su descuento) no puede quedar por debajo del costo
    // unitario del producto. RF014: con una autorizacion vigente DESCUENTO_ESPECIAL del Administrador si se
    // permite: la autorizacion aprobada de ESTA venta (misma huella); se devuelve su id para consumirla al final
    // de la transaccion (null si no hizo falta). Se llama despues de calcularTotales, cuando cada linea tiene su importe.
    private Integer verificarPrecios(Venta venta, int idResponsable) throws SQLException {
        List<String> bajoCosto = lineasBajoCosto(venta.getDetalles());
        if (bajoCosto.isEmpty()) {
            return null;
        }
        try {
            return autorizacionBO.requerirAutorizacion(idResponsable,
                    pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.OPERACION_DESCUENTO_ESPECIAL,
                    huellaVenta(venta)).getId();
        } catch (IllegalStateException e) {
            // se dice que linea queda bajo el costo y lo que falta (solicitar o esperar al Administrador)
            throw new IllegalStateException(bajoCosto.get(0) + ". " + e.getMessage());
        }
    }

    // RF005: lineas cuyo precio efectivo sin IGV queda por debajo del costo (texto para el mensaje y para el Administrador)
    private List<String> lineasBajoCosto(List<DetalleVenta> detalles) throws SQLException {
        List<String> lineas = new java.util.ArrayList<>();
        for (DetalleVenta d : detalles) {
            Producto producto = productoDAO.load(d.getProducto().getId());
            // el precio incluye IGV y el costo no: se comparan ambos sin IGV (PreciosIgv)
            BigDecimal efectivo = BigDecimal.valueOf(d.getImporte())
                    .divide(BigDecimal.valueOf(d.getCantidad()), 4, RoundingMode.HALF_UP)
                    .divide(PreciosIgv.FACTOR, 4, RoundingMode.HALF_UP);
            if (efectivo.compareTo(BigDecimal.valueOf(producto.getCostoUnitario())) < 0) {
                lineas.add(String.format(java.util.Locale.ROOT,
                        "La línea %d (%s) queda en S/ %.2f por unidad sin IGV, por debajo de su costo (S/ %.2f)",
                        d.getNumeroLinea(), producto.getNombre(), efectivo.doubleValue(), producto.getCostoUnitario()));
            }
        }
        return lineas;
    }

    // RF005/RF014: mientras el Vendedor arma la venta, que lineas quedan bajo el costo con los precios que fijara
    // el BO (lista u orden) y su descuento. Asi la pantalla pide la autorizacion ANTES de intentar registrar.
    // Si la venta todavia esta incompleta, no se informa nada: las reglas completas se validan al registrar.
    @Override
    public List<Map<String, Object>> revisarCosto(Venta venta, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        if (venta == null || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return Transaccion.consultar(() -> {
            List<Map<String, Object>> resultado = new java.util.ArrayList<>();
            try {
                if (venta.getId() > 0) {
                    Venta actual = ventaDAO.load(venta.getId());
                    if (actual != null) {
                        venta.setOrdenCompraCliente(actual.getOrdenCompraCliente());
                    }
                }
                if (venta.getOrdenCompraCliente() != null && venta.getOrdenCompraCliente().getId() > 0) {
                    venta.setOrdenCompraCliente(ordenCompraClienteDAO.load(venta.getOrdenCompraCliente().getId()));
                } else {
                    venta.setOrdenCompraCliente(null);
                }
                enlazarLineasOrden(venta);
                fijarPreciosYCantidades(venta.getDetalles());
                calcularTotales(venta);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return resultado;
            }
            for (DetalleVenta d : venta.getDetalles()) {
                if (d.getCantidad() <= 0) {
                    continue;
                }
                Producto producto = productoDAO.load(d.getProducto().getId());
                BigDecimal efectivo = BigDecimal.valueOf(d.getImporte())
                        .divide(BigDecimal.valueOf(d.getCantidad()), 4, RoundingMode.HALF_UP)
                        .divide(PreciosIgv.FACTOR, 4, RoundingMode.HALF_UP);
                if (producto != null && efectivo.compareTo(BigDecimal.valueOf(producto.getCostoUnitario())) < 0) {
                    Map<String, Object> linea = new java.util.LinkedHashMap<>();
                    linea.put("numeroLinea", d.getNumeroLinea());
                    linea.put("producto", producto.getNombre());
                    linea.put("precioSinIgv", efectivo.setScale(2, RoundingMode.HALF_UP).doubleValue());
                    linea.put("costo", producto.getCostoUnitario());
                    resultado.add(linea);
                }
            }
            return resultado;
        });
    }

    // RF014: huella de la venta = venta (0 si es nueva), cliente, condicion de pago y cada linea
    // (producto, cantidad e importe). Si el Vendedor cambia algo despues de la aprobacion, hay que volver a pedirla.
    private static String huellaVenta(Venta venta) {
        StringBuilder datos = new StringBuilder("VENTA|").append(venta.getId())
                .append('|').append(venta.getCliente().getId())
                .append('|').append(venta.getCondicionPago());
        venta.getDetalles().stream()
                .sorted(java.util.Comparator.comparingInt((DetalleVenta d) -> d.getProducto().getId())
                        .thenComparingInt(DetalleVenta::getNumeroLinea))
                .forEach(d -> datos.append('|').append(d.getProducto().getId())
                        .append(':').append(String.format(java.util.Locale.ROOT, "%.3f", d.getCantidad()))
                        .append(':').append(String.format(java.util.Locale.ROOT, "%.2f", d.getImporte())));
        return pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.huella(datos.toString());
    }

    // RF005/RF014: el Vendedor arma la venta y, si una linea queda bajo el costo, pide la autorizacion de ESA venta.
    // No registra nada: el Administrador ve el cliente, el total y las lineas bajo el costo; al aprobar, el Vendedor
    // registra (o guarda) la misma venta.
    @Override
    public pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarDescuentoEspecial(Venta venta, int idResponsable)
            throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        validarCabecera(venta);
        validarDetalles(venta.getDetalles());
        return Transaccion.ejecutar(() -> {
            Cliente cliente = clienteDAO.load(venta.getCliente().getId());
            if (cliente == null || !cliente.isEstado()) {
                throw new IllegalArgumentException("El cliente no existe o está inactivo");
            }
            venta.setCliente(cliente);
            String referencia = "Venta nueva";
            if (venta.getId() > 0) {
                Venta actual = ventaDAO.load(venta.getId());
                if (actual == null) {
                    throw new IllegalArgumentException("No existe la venta con id " + venta.getId());
                }
                referencia = actual.getNumero();
                venta.setCondicionPago(actual.getCondicionPago());
                venta.setOrdenCompraCliente(actual.getOrdenCompraCliente());
            }
            if (venta.getOrdenCompraCliente() != null && venta.getOrdenCompraCliente().getId() > 0) {
                venta.setOrdenCompraCliente(ordenCompraClienteDAO.load(venta.getOrdenCompraCliente().getId()));
            } else {
                venta.setOrdenCompraCliente(null);
            }
            enlazarLineasOrden(venta);
            fijarPreciosYCantidades(venta.getDetalles());
            calcularTotales(venta);

            List<String> bajoCosto = lineasBajoCosto(venta.getDetalles());
            if (bajoCosto.isEmpty()) {
                throw new IllegalStateException("Ninguna línea queda por debajo del costo: no hace falta autorización");
            }
            StringBuilder detalle = new StringBuilder()
                    .append("Cliente: ").append(cliente.getNombreMostrado()).append('\n')
                    .append(String.format(java.util.Locale.ROOT, "Total: S/ %.2f (%s)", venta.getTotal(),
                            venta.getCondicionPago() == CondicionPago.CREDITO ? "crédito" : "contado"));
            for (String linea : bajoCosto) {
                detalle.append('\n').append("- ").append(linea);
            }
            return autorizacionBO.solicitarOperacion(idResponsable,
                    pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.OPERACION_DESCUENTO_ESPECIAL,
                    "Venta con descuento bajo el costo para " + cliente.getNombreMostrado(),
                    referencia + " · " + cliente.getNombreMostrado(), detalle.toString(), huellaVenta(venta));
        });
    }

    private void calcularTotales(Venta venta) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (DetalleVenta d : venta.getDetalles()) {
            BigDecimal bruto = BigDecimal.valueOf(d.getCantidad())
                    .multiply(BigDecimal.valueOf(d.getPrecioUnitario()));
            BigDecimal importe = bruto.subtract(BigDecimal.valueOf(d.getDescuento()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (importe.signum() < 0) {
                throw new IllegalArgumentException("El descuento de la línea " + d.getNumeroLinea()
                        + " supera el importe de la línea");
            }
            d.setImporte(importe.doubleValue());
            subTotal = subTotal.add(importe);
        }
        // los precios incluyen IGV: el total es la suma de los importes y se desglosa en valor de venta + IGV
        BigDecimal total = subTotal;
        venta.setSubTotal(PreciosIgv.valorVenta(total).doubleValue());
        venta.setIgv(PreciosIgv.igv(total).doubleValue());
        venta.setTotal(total.doubleValue());
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
            try {
                detalleVentaDAO.remove(quitada);
            } catch (java.sql.SQLIntegrityConstraintViolationException e) {
                // la linea figura en un despacho o una nota de credito anulados (historial): no se borra
                throw new IllegalStateException("La línea " + quitada.getNumeroLinea() + " tiene despachos o notas de crédito "
                        + "anulados en su historial y no se puede quitar. Modifique su cantidad o anule la venta y registre otra");
            }
        }
    }

    // RF007/RF008: cada linea de una venta que atiende una orden apunta a la linea de orden concreta
    private void enlazarLineasOrden(Venta venta) throws SQLException {
        OrdenCompraCliente oc = venta.getOrdenCompraCliente();
        if (oc == null || oc.getId() <= 0) {
            for (DetalleVenta d : venta.getDetalles()) {
                d.setDetalleOrdenCompraCliente(null);
            }
            return;
        }
        List<DetalleOrdenCompraCliente> lineasOrden = detalleOrdenCompraClienteDAO.listarPorOrden(oc.getId());
        Set<Integer> usadas = new HashSet<>();
        for (DetalleVenta d : venta.getDetalles()) {
            DetalleOrdenCompraCliente lineaOrden = null;
            if (d.getDetalleOrdenCompraCliente() != null && d.getDetalleOrdenCompraCliente().getId() > 0) {
                int idLinea = d.getDetalleOrdenCompraCliente().getId();
                lineaOrden = lineasOrden.stream().filter(l -> l.getId() == idLinea).findFirst().orElse(null);
                if (lineaOrden == null) {
                    throw new IllegalArgumentException("La línea de orden " + idLinea + " no pertenece a la orden de la venta");
                }
            } else {
                // si no se indica, se toma la primera linea de la orden con el mismo producto aun no usada
                for (DetalleOrdenCompraCliente l : lineasOrden) {
                    if (l.getProducto().getId() == d.getProducto().getId() && !usadas.contains(l.getId())) {
                        lineaOrden = l;
                        break;
                    }
                }
                if (lineaOrden == null) {
                    throw new IllegalArgumentException("El producto " + d.getProducto().getId() + " no está en la orden de compra del cliente");
                }
            }
            if (lineaOrden.getProducto().getId() != d.getProducto().getId()) {
                throw new IllegalArgumentException("La línea " + d.getNumeroLinea() + " no corresponde al producto de su línea de orden");
            }
            if (!usadas.add(lineaOrden.getId())) {
                throw new IllegalArgumentException("Dos líneas de la venta no pueden atender la misma línea de la orden");
            }
            double comprometida = ventaDAO.cantidadComprometida(lineaOrden.getId(), venta.getId());
            if (d.getCantidad() + comprometida > lineaOrden.getCantidad() + 0.0001) {
                throw new IllegalArgumentException("La línea " + d.getNumeroLinea() + " vende más de lo pedido en la orden ("
                        + lineaOrden.getCantidad() + ")");
            }
            d.setDetalleOrdenCompraCliente(lineaOrden);
        }
    }

    // RF009: al modificar una venta al credito su cuenta por cobrar refleja el nuevo total
    // y se vuelve a validar el limite de credito del cliente
    private void actualizarCuentaPorCobrar(Venta venta, Cliente cliente) throws SQLException {
        CuentaPorCobrar cxc = cuentaPorCobrarDAO.obtenerPorVenta(venta.getId());
        if (cxc == null) {
            throw new IllegalStateException("La venta al crédito no tiene cuenta por cobrar; repare la integridad antes de modificar");
        }
        // RF013: lo que acreditaron las notas de credito sigue descontandose del nuevo total
        double deudaNeta = venta.getTotal() - cxc.getMontoAcreditado();
        if (deudaNeta + 0.005 < cxc.getMontoPagado()) {
            throw new IllegalStateException("El nuevo total (S/ " + venta.getTotal() + ") es menor que lo ya cobrado (S/ "
                    + cxc.getMontoPagado() + ")");
        }
        double nuevoSaldo = CuentaPorCobrarBOImpl.redondear(deudaNeta - cxc.getMontoPagado());
        BigDecimal deudaCliente = ventaDAO.obtenerSaldoPendienteCliente(cliente.getId());
        if (deudaCliente.subtract(BigDecimal.valueOf(cxc.getSaldoPendiente())).add(BigDecimal.valueOf(nuevoSaldo)).compareTo(cliente.getLimiteCredito()) > 0) {
            throw new IllegalStateException("La venta modificada supera el límite de crédito del cliente (Límite: "
                    + cliente.getLimiteCredito() + ")");
        }
        cxc.setMontoOriginal(venta.getTotal());
        cxc.setSaldoPendiente(nuevoSaldo);
        // Conservar el vencimiento vigente, incluyendo refinanciaciones autorizadas.
        cxc.setEstado(CuentaPorCobrarBOImpl.calcularEstado(cxc));
        cuentaPorCobrarDAO.update(cxc);
    }

    // RF009, RNF002: una venta al credito anulada deja de generar deuda. Con cobros vigentes no se puede anular.
    // Su cuenta NO se borra (antes se borraba si no tenia cobros y, si los tenia anulados, quedaba "PAGADA" sin
    // que nadie pagara): queda ANULADA, con su monto original (lo que fue la venta) y saldo 0. Igual en los dos casos.
    private void cerrarCuentaPorCobrar(int idVenta) throws SQLException {
        CuentaPorCobrar cxc = cuentaPorCobrarDAO.obtenerPorVenta(idVenta);
        if (cxc == null) {
            return;
        }
        if (cxc.getMontoPagado() > 0.005) {
            throw new IllegalStateException("La venta tiene cobros registrados; anule primero los cobros de su cuenta por cobrar");
        }
        cxc.setSaldoPendiente(0);
        cxc.setEstado(pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar.ANULADA);
        cuentaPorCobrarDAO.update(cxc);
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
