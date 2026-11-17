package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.edu.pucp.sigmeta.pruebas.Datos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pe.edu.pucp.sigmeta.boimpl.ventas.CotizacionBOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoCotizacion;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleCotizacion;

/**
 * H14 - RF007: reglas de la cotizacion (CotizacionBOImpl).
 *   calculo: importe de linea = cantidad x precio - descuento (2 decimales, HALF_UP); IGV 18 % del subtotal
 *   numero COT-AAAA-NNNN generado por el sistema y distinto en cada cotizacion
 *   fechas: no se emite con fecha pasada; la vigencia no es anterior a la emision
 *   ninguna linea por debajo del costo; cliente activo; al menos una linea
 *   ciclo REGISTRADA -> ENVIADA -> ACEPTADA; una cotizacion aceptada no se anula; no se anula dos veces
 *   solo el Vendedor registra cotizaciones
 *
 * Integracion contra una base LOCAL de pruebas:
 *   mvn test -pl sigmeta-ejecucion -am -Dsigmeta.integracion=true -Dsurefire.failIfNoSpecifiedTests=false
 */
@EnabledIfSystemProperty(named = "sigmeta.integracion", matches = "true")
@DisplayName("Cotizaciones: calculo, numeracion, fechas y ciclo de estados")
class CotizacionBOTest {

    private static final CotizacionBOImpl cotizaciones = new CotizacionBOImpl();
    private static final BigDecimal IGV = new BigDecimal("0.18");

    /** cotizaciones registradas por cada prueba: se anulan al final si todavia se puede */
    private final List<Cotizacion> creadas = new ArrayList<>();

    @BeforeAll
    static void soloBaseLocal() {
        exigirBaseLocal();
    }

    @AfterEach
    void limpiar() {
        for (Cotizacion c : creadas) {
            try {
                cotizaciones.anular(c.getId(), "Limpieza de CotizacionBOTest", VENDEDOR);
            } catch (Exception ignorada) {
                // ya anulada o aceptada (una aceptada no se anula)
            }
        }
    }

    // ------------------------------------------------------------------ datos de prueba

    private static DetalleCotizacion linea(int numero, Producto producto, double cantidad, double precio, double descuento) {
        DetalleCotizacion d = new DetalleCotizacion();
        d.setNumeroLinea(numero);
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio);
        d.setDescuento(descuento);
        return d;
    }

    private static Cotizacion cotizacion(Cliente cliente, LocalDate emision, LocalDate vigencia, DetalleCotizacion... lineas) {
        Cotizacion c = new Cotizacion();
        c.setCliente(cliente);
        c.setFechaEmision(emision);
        c.setFechaVigencia(vigencia);
        c.setDetalles(new ArrayList<>(List.of(lineas)));
        return c;
    }

    /** Cotizacion valida de hoy, vigente 15 dias, con una linea al precio de lista. */
    private static Cotizacion cotizacionSimple(Cliente cliente, Producto producto) {
        LocalDate hoy = LocalDate.now();
        return cotizacion(cliente, hoy, hoy.plusDays(15), linea(1, producto, 2, producto.getPrecioVenta(), 0));
    }

    private Cotizacion registrar(Cotizacion c) throws Exception {
        Cotizacion guardada = cotizaciones.registrar(c, VENDEDOR);
        creadas.add(guardada);
        return guardada;
    }

    /** Mismo redondeo que CotizacionBOImpl.calcularTotales. */
    private static BigDecimal importe(double cantidad, double precio, double descuento) {
        return BigDecimal.valueOf(cantidad).multiply(BigDecimal.valueOf(precio))
                .subtract(BigDecimal.valueOf(descuento)).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ calculo

    @Test
    @DisplayName("Subtotal, IGV (18 % del subtotal) y total los calcula el sistema, no la pantalla")
    void calculaTotales() throws Exception {
        Producto p = productoConDisponible(1);
        double precio = p.getPrecioVenta();
        // descuento permitido en una unidad: la mitad del margen, sin bajar del costo
        double descuento = BigDecimal.valueOf((precio - p.getCostoUnitario()) / 2)
                .setScale(2, RoundingMode.DOWN).doubleValue();
        LocalDate hoy = LocalDate.now();
        Cotizacion c = cotizacion(empresaNueva(), hoy, hoy.plusDays(10),
                linea(1, p, 3, precio, 0),
                linea(2, p, 1, precio, descuento));
        c.setSubTotal(1);   // valores falsos, como si los mandara la pantalla
        c.setIgv(1);
        c.setTotal(1);

        Cotizacion guardada = registrar(c);

        BigDecimal subTotal = importe(3, precio, 0).add(importe(1, precio, descuento));
        BigDecimal igv = subTotal.multiply(IGV).setScale(2, RoundingMode.HALF_UP);
        assertEquals(subTotal.doubleValue(), guardada.getSubTotal(), CENTAVO);
        assertEquals(igv.doubleValue(), guardada.getIgv(), CENTAVO);
        assertEquals(subTotal.add(igv).doubleValue(), guardada.getTotal(), CENTAVO);
        assertEquals(EstadoCotizacion.REGISTRADA, guardada.getEstado());
    }

    // ------------------------------------------------------------------ numeracion

    @Test
    @DisplayName("El numero COT-AAAA-NNNN lo pone el sistema y no se repite")
    void numeroAutomaticoYDistinto() throws Exception {
        Producto p = productoConDisponible(1);
        Cliente cliente = empresaNueva();

        Cotizacion primera = cotizacionSimple(cliente, p);
        primera.setNumero("COT-INVENTADO");   // la pantalla no decide el numero
        Cotizacion a = registrar(primera);
        Cotizacion b = registrar(cotizacionSimple(cliente, p));

        String patron = "COT-" + LocalDate.now().getYear() + "-\\d+";
        assertTrue(a.getNumero().matches(patron), a.getNumero());
        assertTrue(b.getNumero().matches(patron), b.getNumero());
        assertNotEquals(a.getNumero(), b.getNumero());
    }

    // ------------------------------------------------------------------ validaciones

    @Test
    @DisplayName("No se emite una cotizacion con fecha pasada")
    void emisionPasada() throws Exception {
        Producto p = productoConDisponible(1);
        LocalDate ayer = LocalDate.now().minusDays(1);
        Cotizacion c = cotizacion(empresaNueva(), ayer, ayer.plusDays(15), linea(1, p, 1, p.getPrecioVenta(), 0));

        assertThrows(IllegalArgumentException.class, () -> cotizaciones.registrar(c, VENDEDOR));
    }

    @Test
    @DisplayName("La vigencia no puede ser anterior a la emision")
    void vigenciaAntesDeEmision() throws Exception {
        Producto p = productoConDisponible(1);
        LocalDate hoy = LocalDate.now();
        Cotizacion c = cotizacion(empresaNueva(), hoy.plusDays(5), hoy.plusDays(2), linea(1, p, 1, p.getPrecioVenta(), 0));

        assertThrows(IllegalArgumentException.class, () -> cotizaciones.registrar(c, VENDEDOR));
    }

    @Test
    @DisplayName("Una cotizacion sin lineas se rechaza")
    void sinLineas() throws Exception {
        LocalDate hoy = LocalDate.now();
        Cotizacion c = cotizacion(empresaNueva(), hoy, hoy.plusDays(15));

        assertThrows(IllegalArgumentException.class, () -> cotizaciones.registrar(c, VENDEDOR));
    }

    @Test
    @DisplayName("Ninguna linea por debajo del costo del producto")
    void lineaBajoElCosto() throws Exception {
        Producto p = productoConDisponible(1);
        LocalDate hoy = LocalDate.now();
        double bajoCosto = BigDecimal.valueOf(p.getCostoUnitario() - 0.50).setScale(2, RoundingMode.HALF_UP).doubleValue();
        Cotizacion c = cotizacion(empresaNueva(), hoy, hoy.plusDays(15), linea(1, p, 1, bajoCosto, 0));

        assertThrows(IllegalArgumentException.class, () -> cotizaciones.registrar(c, VENDEDOR));
    }

    @Test
    @DisplayName("No se cotiza a un cliente desactivado")
    void clienteDesactivado() throws Exception {
        Producto p = productoConDisponible(1);
        Cliente cliente = empresaNueva();
        clientes.desactivar(cliente.getId(), ADMINISTRADOR);

        assertThrows(IllegalArgumentException.class, () -> cotizaciones.registrar(cotizacionSimple(cliente, p), VENDEDOR));
    }

    @Test
    @DisplayName("Solo el Vendedor registra cotizaciones")
    void soloVendedorRegistra() throws Exception {
        Producto p = productoConDisponible(1);
        Cliente cliente = empresaNueva();

        assertThrows(IllegalStateException.class, () -> cotizaciones.registrar(cotizacionSimple(cliente, p), CAJERO));
        assertThrows(IllegalStateException.class, () -> cotizaciones.registrar(cotizacionSimple(cliente, p), ALMACENERO));
    }

    // ------------------------------------------------------------------ ciclo de estados

    @Test
    @DisplayName("REGISTRADA -> ENVIADA -> ACEPTADA; una aceptada no se anula")
    void cicloHastaAceptada() throws Exception {
        Cotizacion c = registrar(cotizacionSimple(empresaNueva(), productoConDisponible(1)));

        // no se acepta sin haberla enviado
        assertThrows(IllegalStateException.class, () -> cotizaciones.aceptar(c.getId(), VENDEDOR));

        assertEquals(EstadoCotizacion.ENVIADA, cotizaciones.enviar(c.getId(), VENDEDOR).getEstado());
        assertEquals(EstadoCotizacion.ACEPTADA, cotizaciones.aceptar(c.getId(), VENDEDOR).getEstado());

        assertThrows(IllegalStateException.class,
                () -> cotizaciones.anular(c.getId(), "Prueba de anulacion", VENDEDOR));
    }

    @Test
    @DisplayName("Solo se modifica en REGISTRADA y no se anula dos veces")
    void modificarYAnular() throws Exception {
        Cotizacion c = registrar(cotizacionSimple(empresaNueva(), productoConDisponible(1)));
        cotizaciones.enviar(c.getId(), VENDEDOR);

        Cotizacion enviada = cotizaciones.obtener(c.getId());
        assertThrows(IllegalStateException.class, () -> cotizaciones.modificar(enviada, VENDEDOR));

        cotizaciones.anular(c.getId(), "Prueba de anulacion", VENDEDOR);
        assertThrows(IllegalStateException.class,
                () -> cotizaciones.anular(c.getId(), "Otra vez", VENDEDOR));
    }
}
