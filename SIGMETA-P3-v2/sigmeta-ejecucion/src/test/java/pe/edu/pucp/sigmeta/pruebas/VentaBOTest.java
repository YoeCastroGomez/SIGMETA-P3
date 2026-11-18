package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.edu.pucp.sigmeta.pruebas.Datos.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

/**
 * H14 - RF008: reglas del registro de ventas (VentaBOImpl).
 *   H01  se vende contra el stock DISPONIBLE (fisico - comprometido en ventas sin despachar)
 *   H03  ninguna linea por debajo del costo promedio sin autorizacion DESCUENTO_ESPECIAL (un solo uso)
 *   RF009 la venta al credito exige linea de credito y no puede superar el limite del cliente
 *
 * Integracion contra una base LOCAL de pruebas:
 *   mvn test -pl sigmeta-ejecucion -am -Dsigmeta.integracion=true -Dsurefire.failIfNoSpecifiedTests=false
 */
@EnabledIfSystemProperty(named = "sigmeta.integracion", matches = "true")
@DisplayName("Ventas: stock disponible, piso de costo y limite de credito")
class VentaBOTest {

    private static final String DESCUENTO = SolicitudAutorizacionBOImpl.OPERACION_DESCUENTO_ESPECIAL;

    /** ventas registradas por cada prueba: se anulan al final para liberar el stock comprometido */
    private final List<Venta> creadas = new ArrayList<>();

    @BeforeAll
    static void soloBaseLocal() {
        exigirBaseLocal();
    }

    @AfterEach
    void limpiar() throws Exception {
        for (Venta v : creadas) {
            anularSiSePuede(v);
        }
        sinAutorizaciones(VENDEDOR, DESCUENTO);
    }

    /** Venta de 1 unidad a precio de lista con un descuento que deja la unidad bajo el costo (sin IGV). */
    private static Venta ventaBajoCosto(Cliente cliente, Producto p) {
        Venta v = venta(cliente, p, 1, p.getPrecioVenta(), false);
        v.getDetalles().get(0).setDescuento(round2(p.getPrecioVenta() - p.getCostoUnitario() + 0.50));
        return v;
    }

    private Venta registrar(Venta v) throws Exception {
        Venta guardada = ventas.registrar(v, VENDEDOR);
        creadas.add(guardada);
        return guardada;
    }

    // ------------------------------------------------------------------ H01 stock disponible

    @Test
    @DisplayName("H01: rechaza vender mas de lo disponible")
    void rechazaVenderMasDeLoDisponible() throws Exception {
        Producto p = productoConDisponible(5);
        Cliente cliente = empresaNueva();
        double disponible = disponible(p.getId());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> registrar(venta(cliente, p, disponible + 1, p.getPrecioVenta(), false)));
        assertTrue(e.getMessage().startsWith("Stock insuficiente"), e.getMessage());
    }

    @Test
    @DisplayName("H01: lo vendido y no despachado compromete el stock de las ventas siguientes")
    void loVendidoSinDespacharComprometeElStock() throws Exception {
        Producto p = productoConDisponible(5);
        Cliente cliente = empresaNueva();
        double fisico = stockFisico(p.getId());
        double disponible = disponible(p.getId());
        double primera = Math.floor(disponible) - 2;          // deja al menos 2 unidades libres
        double restante = disponible - primera;

        registrar(venta(cliente, p, primera, p.getPrecioVenta(), false));
        assertEquals(fisico, stockFisico(p.getId()), 0.001, "vender sin despachar no saca mercaderia del almacen");
        assertEquals(restante, disponible(p.getId()), 0.001, "lo vendido sin despachar queda comprometido");

        assertThrows(IllegalArgumentException.class,
                () -> registrar(venta(cliente, p, restante + 1, p.getPrecioVenta(), false)),
                "la segunda venta no puede prometer unidades ya vendidas");
        Venta ultima = registrar(venta(cliente, p, restante, p.getPrecioVenta(), false));
        assertTrue(ultima.getId() > 0, "vender exactamente lo que queda disponible si se permite");
    }

    // ------------------------------------------------------------------ H03 piso de costo promedio

    @Test
    @DisplayName("H03: el precio lo pone el sistema (precio de lista), no la pantalla")
    void ignoraElPrecioQueMandaLaPantalla() throws Exception {
        Producto p = productoConDisponible(1);
        Cliente cliente = empresaNueva();

        // la pantalla manda otro precio (5 soles mas): el BO lo reemplaza por el precio de lista
        Venta v = registrar(venta(cliente, p, 1, p.getPrecioVenta() + 5, false));
        assertEquals(p.getPrecioVenta(), v.getDetalles().get(0).getPrecioUnitario(), CENTAVO, "precio de lista");
    }

    @Test
    @DisplayName("H03: el descuento tampoco puede dejar el precio efectivo bajo el costo")
    void rechazaDescuentoQueDejaElPrecioBajoCosto() throws Exception {
        sinAutorizaciones(VENDEDOR, DESCUENTO);
        Producto p = productoConDisponible(1);
        Cliente cliente = empresaNueva();
        Venta v = venta(cliente, p, 1, p.getPrecioVenta(), false);
        // precio de lista correcto, pero el descuento deja la unidad en (costo - 0.50)
        v.getDetalles().get(0).setDescuento(round2(p.getPrecioVenta() - p.getCostoUnitario() + 0.50));

        assertThrows(IllegalStateException.class, () -> registrar(v));
    }

    @Test
    @DisplayName("H03: vender exactamente al costo se permite")
    void venderAlCostoSePermite() throws Exception {
        sinAutorizaciones(VENDEDOR, DESCUENTO);
        Producto p = productoConDisponible(1);
        Venta v = registrar(venta(empresaNueva(), p, 1, p.getCostoUnitario(), false));
        assertTrue(v.getId() > 0);
    }

    @Test
    @DisplayName("H03/RF014: la autorizacion DESCUENTO_ESPECIAL permite una sola venta bajo el costo")
    void autorizacionDeDescuentoEsDeUnSoloUso() throws Exception {
        Producto p = productoConDisponible(2);
        Cliente cliente = empresaNueva();
        sinAutorizaciones(VENDEDOR, DESCUENTO);
        // sin autorizacion la venta bajo el costo se rechaza
        assertThrows(IllegalStateException.class, () -> registrar(ventaBajoCosto(cliente, p)));

        // el Vendedor pide la autorizacion de ESTA venta y el Administrador la aprueba
        SolicitudAutorizacion autorizacion = ventas.solicitarDescuentoEspecial(ventaBajoCosto(cliente, p), VENDEDOR);
        assertTrue(autorizacion.getDetalle() != null && autorizacion.getDetalle().contains("costo"),
                "el Administrador ve las lineas bajo el costo: " + autorizacion.getDetalle());
        autorizaciones.aprobar(autorizacion.getId(), ADMINISTRADOR, 30);

        Venta conPermiso = registrar(ventaBajoCosto(cliente, p));
        assertTrue(conPermiso.getId() > 0, "con la autorizacion vigente se registra");
        assertEquals(EstadoAutorizacion.VENCIDA, autorizaciones.obtener(autorizacion.getId()).getEstado(),
                "la autorizacion queda consumida");

        assertThrows(IllegalStateException.class,
                () -> registrar(ventaBajoCosto(cliente, p)),
                "la misma autorizacion no sirve para otra venta");
    }

    // ------------------------------------------------------------------ RF008 fecha de emision

    @Test
    @DisplayName("RF008: la fecha de la venta no puede ser futura ni de hace mas de 7 dias")
    void fechaDeEmisionDentroDelRango() throws Exception {
        Producto p = productoConDisponible(3);
        Cliente cliente = empresaNueva();

        Venta futura = venta(cliente, p, 1, p.getPrecioVenta(), false);
        futura.setFechaEmision(java.time.LocalDate.now().plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> registrar(futura), "fecha futura");

        Venta antigua = venta(cliente, p, 1, p.getPrecioVenta(), false);
        antigua.setFechaEmision(java.time.LocalDate.now().minusDays(8));
        assertThrows(IllegalArgumentException.class, () -> registrar(antigua), "mas de 7 dias de atraso");

        Venta conAtraso = venta(cliente, p, 1, p.getPrecioVenta(), false);
        conAtraso.setFechaEmision(java.time.LocalDate.now().minusDays(7));
        assertTrue(registrar(conAtraso).getId() > 0, "7 dias de atraso todavia se aceptan");
    }

    // ------------------------------------------------------------------ RF009 credito

    @Test
    @DisplayName("RF009: un cliente sin linea de credito no compra al credito")
    void clienteSinLineaNoCompraAlCredito() throws Exception {
        Producto p = productoConDisponible(1);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> registrar(venta(empresaNueva(), p, 1, p.getPrecioVenta(), true)));
        assertTrue(e.getMessage().contains("línea de crédito"), e.getMessage());
    }

    @Test
    @DisplayName("RF009: la deuda pendiente mas la nueva venta no superan el limite")
    void creditoNoSuperaElLimite() throws Exception {
        Producto p = productoConDisponible(12);
        double precio = p.getPrecioVenta();
        double diezUnidades = round2(precio * 10);                 // los precios ya incluyen IGV
        Cliente cliente = empresaConCredito(diezUnidades + 1);   // alcanza para 10 unidades

        Venta seis = registrar(venta(cliente, p, 6, precio, true));
        assertEquals(seis.getTotal(), saldoCuenta(seis.getId()), CENTAVO, "la venta al credito genera su cuenta por cobrar");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> registrar(venta(cliente, p, 5, precio, true)), "6 + 5 unidades superan el limite");
        assertTrue(e.getMessage().contains("límite de crédito"), e.getMessage());

        Venta cuatro = registrar(venta(cliente, p, 4, precio, true));
        assertTrue(cuatro.getId() > 0, "6 + 4 unidades caben en el limite");
    }

    private static double round2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
