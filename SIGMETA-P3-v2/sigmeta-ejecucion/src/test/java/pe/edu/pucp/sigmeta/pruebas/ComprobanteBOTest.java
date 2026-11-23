package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.caja.CajaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.boimpl.caja.MovimientoCajaBOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

/**
 * H14 - RF008: comprobantes (ComprobanteBOImpl): anular factura o boleta y registrar su envio.
 *
 *   Una venta con comprobante se corrige asi: si la mercaderia NO salio, se anula el comprobante y luego la venta;
 *   si ya salio, la devolucion es con nota de credito (el comprobante no se anula).
 *
 * Integracion contra una base LOCAL de pruebas:
 *   mvn test -pl sigmeta-ejecucion -am "-Dsigmeta.integracion=true"
 */
@EnabledIfSystemProperty(named = "sigmeta.integracion", matches = "true")
@DisplayName("Comprobantes: anulacion de factura o boleta y envio")
class ComprobanteBOTest {

    private final List<Venta> creadas = new ArrayList<>();

    @BeforeAll
    static void soloBaseLocal() {
        exigirBaseLocal();
    }

    @AfterEach
    void limpiar() {
        for (Venta v : creadas) {
            anularSiSePuede(v);
        }
    }

    /** Venta al credito de 3 unidades con su factura emitida (sin despachar). */
    private Venta ventaFacturada(Producto p) throws Exception {
        Venta v = ventas.registrar(venta(empresaConCredito(1_000_000), p, 3, p.getPrecioVenta(), true), VENDEDOR);
        creadas.add(v);
        return v;
    }

    @Test
    @DisplayName("RF008: sin despacho, se anula la factura y despues la venta (deuda y stock liberados)")
    void anularFacturaYLuegoLaVenta() throws Exception {
        Producto p = productoConDisponible(5);
        double disponibleAntes = disponible(p.getId());
        Venta v = ventaFacturada(p);
        Comprobante factura = factura(v);

        assertThrows(IllegalStateException.class, () -> ventas.anular(v.getId(), "Con factura vigente", VENDEDOR),
                "con factura vigente la venta no se anula");

        comprobantes.anular(factura.getId(), "El cliente desistió antes del despacho", VENDEDOR);
        assertEquals(EstadoComprobante.ANULADO, comprobantes.obtener(factura.getId()).getEstado());

        ventas.anular(v.getId(), "El cliente desistió antes del despacho", VENDEDOR);
        assertTrue(ventas.obtener(v.getId()).isAnulado());
        // RF009 + RNF002: la cuenta no se borra; queda ANULADA con saldo 0 y la anulacion registrada (RNF001)
        var cuenta = Transaccion.consultar(() -> new CuentaPorCobrarDAOImpl().obtenerPorVenta(v.getId()));
        assertEquals(pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar.ANULADA, cuenta.getEstado(), "la cuenta por cobrar queda anulada");
        assertEquals(0.0, cuenta.getSaldoPendiente(), 0.001, "sin saldo pendiente");
        assertEquals(1, DatosCaja.contar("SELECT COUNT(*) FROM registro_anulacion WHERE tipo_documento='CUENTA_POR_COBRAR' AND id_documento="
                + cuenta.getId() + " AND motivo='El cliente desistió antes del despacho'"), "la anulacion queda registrada");
        assertEquals(disponibleAntes, disponible(p.getId()), 0.001, "el stock comprometido se libera");
    }

    @Test
    @DisplayName("RF008: tras anular la factura se puede emitir otra con un numero nuevo")
    void reemitirTrasAnular() throws Exception {
        Venta v = ventaFacturada(productoConDisponible(5));
        Comprobante primera = factura(v);
        comprobantes.anular(primera.getId(), "Datos del cliente corregidos", CAJERO);

        Comprobante segunda = factura(v);
        assertNotEquals(primera.getNumero(), segunda.getNumero(), "el numero anulado no se reutiliza");
        assertEquals(EstadoComprobante.EMITIDO, segunda.getEstado());
        comprobantes.anular(segunda.getId(), "Limpieza de prueba JUnit", CAJERO);
    }

    @Test
    @DisplayName("RF008/RF012: con mercaderia despachada la factura no se anula (se usa nota de credito)")
    void conDespachoNoSeAnulaLaFactura() throws Exception {
        Venta v = ventaFacturada(productoConDisponible(5));
        Comprobante factura = factura(v);
        despachar(v, 1);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> comprobantes.anular(factura.getId(), "Intento con despacho", VENDEDOR));
        assertTrue(e.getMessage().contains("nota de crédito"), e.getMessage());
        assertEquals(EstadoComprobante.EMITIDO, comprobantes.obtener(factura.getId()).getEstado());
    }

    @Test
    @DisplayName("RF008: tras anular la factura la venta se puede corregir (y su deuda se actualiza)")
    void corregirVentaTrasAnularFactura() throws Exception {
        Venta v = ventaFacturada(productoConDisponible(5));
        Comprobante factura = factura(v);
        Venta edicion = ventas.obtener(v.getId());
        edicion.getDetalles().get(0).setCantidad(2);
        assertThrows(IllegalStateException.class, () -> ventas.modificar(edicion, VENDEDOR), "con factura vigente no se edita");

        comprobantes.anular(factura.getId(), "Cantidad equivocada", VENDEDOR);
        Venta corregida = ventas.obtener(v.getId());
        corregida.getDetalles().get(0).setCantidad(2);
        ventas.modificar(corregida, VENDEDOR);

        Venta leida = ventas.obtener(v.getId());
        assertEquals(2, leida.getDetalles().get(0).getCantidad(), 0.001);
        assertEquals(leida.getTotal(), saldoCuenta(v.getId()), CENTAVO, "la cuenta por cobrar toma el nuevo total");
        Comprobante nueva = factura(v);
        assertEquals(leida.getTotal(), nueva.getTotal(), CENTAVO, "se vuelve a facturar con el total corregido");
        comprobantes.anular(nueva.getId(), "Limpieza de prueba JUnit", VENDEDOR);
    }

    @Test
    @DisplayName("RF008/RF010: una venta al contado ya cobrada no se modifica aunque se anule su comprobante")
    void ventaContadoCobradaNoSeModifica() throws Exception {
        Producto p = productoConDisponible(3);
        Venta v = ventas.registrar(venta(empresaNueva(), p, 1, p.getPrecioVenta(), false), VENDEDOR);
        creadas.add(v);
        Comprobante factura = factura(v);

        CajaBOImpl cajas = new CajaBOImpl();
        Caja caja = cajas.obtenerCajaAbiertaPorUsuario(CAJERO);
        if (caja == null) {
            caja = cajas.abrirCaja(new Caja(), CAJERO);
        }
        MovimientoCaja cobro = new MovimientoCaja();
        cobro.setCaja(caja);
        cobro.setTipo(TipoMovimientoCaja.COBRO_CONTADO);
        cobro.setMedioPago(MedioPago.EFECTIVO);
        cobro.setConcepto("Cobro de prueba JUnit");
        cobro.setDocumentoOrigen(CompensacionCaja.DOCUMENTO_VENTA);
        cobro.setIdDocumentoOrigen(v.getId());
        new MovimientoCajaBOImpl().registrarMovimiento(cobro, CAJERO);

        comprobantes.anular(factura.getId(), "Prueba de venta cobrada", CAJERO);
        Venta edicion = ventas.obtener(v.getId());
        edicion.getDetalles().get(0).setCantidad(2);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ventas.modificar(edicion, VENDEDOR));
        assertTrue(e.getMessage().contains("cobró"), e.getMessage());
        // limpieza: anular la venta devuelve el cobro en la misma caja (abierta)
    }

    @Test
    @DisplayName("RF008: el motivo de anulacion es obligatorio y no se anula dos veces")
    void motivoObligatorioYSinDobleAnulacion() throws Exception {
        Venta v = ventaFacturada(productoConDisponible(5));
        Comprobante factura = factura(v);

        assertThrows(IllegalArgumentException.class, () -> comprobantes.anular(factura.getId(), "  ", VENDEDOR));
        comprobantes.anular(factura.getId(), "Prueba", VENDEDOR);
        assertThrows(IllegalStateException.class, () -> comprobantes.anular(factura.getId(), "Otra vez", VENDEDOR));
    }

    @Test
    @DisplayName("RF008: correo y mensajeria exigen el dato de contacto del cliente; el impreso no")
    void envioSegunDatosDeContacto() throws Exception {
        Venta v = ventaFacturada(productoConDisponible(5));   // las empresas de prueba no tienen correo ni telefono
        Comprobante factura = factura(v);

        IllegalArgumentException sinCorreo = assertThrows(IllegalArgumentException.class,
                () -> comprobantes.registrarEnvio(factura.getId(), "CORREO", VENDEDOR));
        assertTrue(sinCorreo.getMessage().contains("correo"), sinCorreo.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> comprobantes.registrarEnvio(factura.getId(), "MENSAJERIA", VENDEDOR));

        Comprobante impreso = comprobantes.registrarEnvio(factura.getId(), "IMPRESO", VENDEDOR);
        assertEquals("IMPRESO", impreso.getMedioEnvio());
        assertTrue(impreso.getFechaEnvio() != null, "se registra la fecha del envio");

        comprobantes.anular(factura.getId(), "Limpieza de prueba JUnit", VENDEDOR);
    }
}
