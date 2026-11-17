package pe.edu.pucp.sigmeta.pruebas;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;
import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.almacen.DespachoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.boimpl.socio.ClienteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.ComprobanteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.VentaBOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.DetalleDespacho;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoDocumentoIdentidad;
import pe.edu.pucp.sigmeta.model.enums.TipoPersona;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

/**
 * H14: datos y pasos comunes de las pruebas de integracion de ventas y notas de credito.
 *
 * Las pruebas llaman a los BO reales contra MySQL (los BO crean sus DAO con "new", asi que no se pueden
 * reemplazar por simulaciones). Por eso escriben datos: se ejecutan SOLO sobre una base local de pruebas,
 * con los usuarios de los datos demo (1 Vendedor, 2 Cajero, 3 Almacenero, 4 Administrador).
 * Cada prueba crea su propio cliente, asi no depende de lo que hicieron las demas.
 */
final class Datos {

    static final int VENDEDOR = 1;
    static final int CAJERO = 2;
    static final int ALMACENERO = 3;
    static final int ADMINISTRADOR = 4;
    static final double CENTAVO = 0.005;

    static final ClienteBOImpl clientes = new ClienteBOImpl();
    static final VentaBOImpl ventas = new VentaBOImpl();
    static final ComprobanteBOImpl comprobantes = new ComprobanteBOImpl();
    static final DespachoBOImpl despachos = new DespachoBOImpl();
    static final SolicitudAutorizacionBOImpl autorizaciones = new SolicitudAutorizacionBOImpl();

    private Datos() {
    }

    /** Las pruebas escriben datos: se niegan a correr sobre una base que no sea local (la compartida, por ejemplo). */
    static void exigirBaseLocal() {
        String host = System.getProperty("sigmeta.db.host", ResourceBundle.getBundle("db").getString("db.host")).trim();
        boolean local = host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1");
        if (!local && !Boolean.getBoolean("sigmeta.integracion.permitirRemota")) {
            throw new IllegalStateException("Las pruebas de integracion escriben datos y la base configurada no es local ("
                    + host + "). Use una base local de pruebas o -Dsigmeta.db.host=localhost");
        }
    }

    // ---------------------------------------------------------------- productos y stock

    /** Un producto activo con al menos "minimo" unidades disponibles y precio de lista por encima del costo. */
    static Producto productoConDisponible(double minimo) throws SQLException {
        List<Map<String, Object>> filas = Transaccion.consultar(
                () -> new ConsultaDAOImpl().consultar("sp_stock_listar_productos"));
        for (Map<String, Object> fila : filas) {
            double disponible = ((Number) fila.get("disponible")).doubleValue();
            if (disponible < minimo) {
                continue;
            }
            Producto p = producto(((Number) fila.get("id")).intValue());
            if (p != null && p.isEstado() && p.getPrecioVenta() > p.getCostoUnitario() && p.getCostoUnitario() > 1) {
                return p;
            }
        }
        throw new IllegalStateException("La base de pruebas no tiene un producto con " + minimo
                + " unidades disponibles; cargue 4_datos_demo/datos_demo.sql");
    }

    static Producto producto(int id) throws SQLException {
        return Transaccion.consultar(() -> new ProductoDAOImpl().load(id));
    }

    static double stockFisico(int idProducto) throws SQLException {
        return producto(idProducto).getStockActual();
    }

    /** Stock fisico menos lo vendido y todavia no despachado (mismo calculo que VentaBOImpl). */
    static double disponible(int idProducto) throws SQLException {
        BigDecimal comprometido = Transaccion.consultar(
                () -> new ConsultaDAOImpl().numero("sp_stock_comprometido", idProducto, 0));
        return stockFisico(idProducto) - comprometido.doubleValue();
    }

    // ---------------------------------------------------------------- clientes

    /** Empresa con RUC nuevo (admite factura). Sin linea de credito: empieza al contado. */
    static Cliente empresaNueva() throws SQLException {
        String ruc = "20" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
        Cliente c = new Cliente();
        c.setTipoPersona(TipoPersona.JURIDICA);
        c.setTipoDocumento(TipoDocumentoIdentidad.RUC);
        c.setNumeroDocumento(ruc);
        c.setRazonSocial("Empresa de prueba JUnit " + ruc);
        return clientes.registrar(c, ADMINISTRADOR);
    }

    static Cliente empresaConCredito(double limite) throws SQLException {
        Cliente c = empresaNueva();
        clientes.asignarLineaCredito(c.getId(), limite, 30, "A", ADMINISTRADOR);
        return clientes.obtener(c.getId());
    }

    // ---------------------------------------------------------------- ventas

    static Venta venta(Cliente cliente, Producto producto, double cantidad, double precio, boolean credito) {
        Venta v = new Venta();
        v.setCliente(cliente);
        v.setFechaEmision(LocalDate.now());
        v.setCondicionPago(credito ? CondicionPago.CREDITO : CondicionPago.CONTADO);
        DetalleVenta d = new DetalleVenta();
        d.setNumeroLinea(1);
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio);
        d.setDescuento(0);
        v.getDetalles().add(d);
        return v;
    }

    /** Anula una venta de prueba para que su stock deje de figurar como comprometido (si ya no se puede, la deja). */
    static void anularSiSePuede(Venta v) {
        if (v == null || v.getId() <= 0) {
            return;
        }
        try {
            ventas.anular(v.getId(), "Limpieza de prueba JUnit", VENDEDOR);
        } catch (Exception e) {
            // tiene comprobante o despacho vigente: queda registrada en la base de pruebas
        }
    }

    static double saldoCuenta(int idVenta) throws SQLException {
        return Transaccion.consultar(() -> new CuentaPorCobrarDAOImpl().obtenerPorVenta(idVenta)).getSaldoPendiente();
    }

    // ---------------------------------------------------------------- comprobante y despacho

    static Comprobante factura(Venta v) throws SQLException {
        return comprobantes.emitir(v.getId(), TipoComprobante.FACTURA, VENDEDOR);
    }

    /** Despacha "cantidad" de la primera linea de la venta y devuelve esa linea ya leida de la base. */
    static DetalleVenta despachar(Venta v, double cantidad) throws SQLException {
        DetalleVenta linea = ventas.obtener(v.getId()).getDetalles().get(0);
        Despacho despacho = new Despacho();
        despacho.setVenta(v);
        DetalleDespacho d = new DetalleDespacho();
        d.setDetalleVenta(linea);
        d.setCantidadDespachada(cantidad);
        despacho.getDetalles().add(d);
        despachos.registrar(despacho, ALMACENERO);
        return ventas.obtener(v.getId()).getDetalles().get(0);
    }

    // ---------------------------------------------------------------- autorizaciones (RF014)

    /** Retira las solicitudes pendientes o aprobadas que el usuario tenga para esa operacion (de corridas anteriores). */
    static void sinAutorizaciones(int idUsuario, String operacion) throws SQLException {
        for (SolicitudAutorizacion s : autorizaciones.listarPorSolicitante(idUsuario)) {
            if (operacion.equals(s.getOperacionRestringida())
                    && (s.getEstado() == EstadoAutorizacion.PENDIENTE || s.getEstado() == EstadoAutorizacion.APROBADA)) {
                autorizaciones.anular(s.getId(), idUsuario);
            }
        }
    }

    /** El usuario solicita la operacion y el Administrador la aprueba por 30 minutos. */
    static SolicitudAutorizacion autorizar(int idUsuario, String operacion) throws SQLException {
        sinAutorizaciones(idUsuario, operacion);
        SolicitudAutorizacion s = autorizaciones.solicitar(idUsuario, operacion, "Prueba JUnit");
        return autorizaciones.aprobar(s.getId(), ADMINISTRADOR, 30);
    }
}
