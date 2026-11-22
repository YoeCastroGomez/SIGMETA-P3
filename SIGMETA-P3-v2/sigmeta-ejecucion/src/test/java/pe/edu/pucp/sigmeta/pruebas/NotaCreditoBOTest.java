package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.edu.pucp.sigmeta.pruebas.Datos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.daoimpl.almacen.MovimientoInventarioDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

/**
 * H14 - RF013/RF014: notas de credito (ComprobanteBOImpl).
 *
 * Cada prueba parte de una venta AL CREDITO de 4 unidades, con factura emitida y 3 unidades despachadas.
 * Al credito la nota rebaja la cuenta por cobrar y no mueve caja, asi la prueba no depende de una caja abierta.
 *
 * Integracion contra una base LOCAL de pruebas:
 *   mvn test -pl sigmeta-ejecucion -am -Dsigmeta.integracion=true -Dsurefire.failIfNoSpecifiedTests=false
 */
@EnabledIfSystemProperty(named = "sigmeta.integracion", matches = "true")
@DisplayName("Notas de credito: autorizacion, montos, stock, deuda y anulacion")
class NotaCreditoBOTest {

    private static final String NOTA = SolicitudAutorizacionBOImpl.OPERACION_NOTA_CREDITO;
    private static final BigDecimal IGV = new BigDecimal("0.18");

    private Producto producto;
    private Venta venta;
    private Comprobante factura;
    private DetalleVenta linea;          // la linea de la venta, con 3 de 4 unidades entregadas
    private final List<Comprobante> notas = new ArrayList<>();

    @BeforeAll
    static void soloBaseLocal() {
        exigirBaseLocal();
    }

    @BeforeEach
    void ventaFacturadaYDespachada() throws Exception {
        sinAutorizaciones(CAJERO, NOTA);
        producto = productoConDisponible(6);
        venta = ventas.registrar(venta(empresaConCredito(1_000_000), producto, 4, producto.getPrecioVenta(), true), VENDEDOR);
        factura = factura(venta);
        linea = despachar(venta, 3);
    }

    @AfterEach
    void limpiar() throws Exception {
        // las notas vigentes se anulan para que la venta de prueba no quede con devoluciones colgadas
        for (Comprobante n : notas) {
            try {
                comprobantes.anular(n.getId(), "Limpieza de prueba JUnit", CAJERO);
            } catch (IllegalStateException yaAnulada) {
                // la prueba ya la anulo
            }
        }
        sinAutorizaciones(CAJERO, NOTA);
    }

    private Comprobante pedido(double cantidad) {
        Comprobante nota = new Comprobante();
        Comprobante origen = new Comprobante();
        origen.setId(factura.getId());
        nota.setComprobanteRelacionado(origen);
        nota.setMotivo("Devolución de prueba JUnit");
        DetalleNotaCredito d = new DetalleNotaCredito();
        DetalleVenta lineaDevuelta = new DetalleVenta();
        lineaDevuelta.setId(linea.getId());
        d.setDetalleVenta(lineaDevuelta);
        d.setNumeroLinea(1);
        d.setCantidad(cantidad);
        List<DetalleNotaCredito> detalles = new ArrayList<>();
        detalles.add(d);
        nota.setDetalles(detalles);
        return nota;
    }

    private Comprobante emitir(double cantidad) throws Exception {
        Comprobante nota = comprobantes.registrarNotaCredito(pedido(cantidad), CAJERO);
        notas.add(nota);
        return nota;
    }

    /** RF014: el Cajero pide la autorizacion de ESTA nota (mismas lineas y cantidades) y el Administrador la aprueba. */
    private SolicitudAutorizacion autorizarNota(double cantidad) throws Exception {
        SolicitudAutorizacion s = comprobantes.solicitarNotaCredito(pedido(cantidad), CAJERO);
        return autorizaciones.aprobar(s.getId(), ADMINISTRADOR, 30);
    }

    /** Lo que deberia devolver la nota: importe de la linea (con descuento) por unidad devuelta; ya incluye IGV. */
    private double totalEsperado(double cantidad) {
        BigDecimal sub = BigDecimal.valueOf(linea.getImporte()).multiply(BigDecimal.valueOf(cantidad))
                .divide(BigDecimal.valueOf(linea.getCantidad()), 2, RoundingMode.HALF_UP);
        return sub.doubleValue();   // el importe cobrado ya incluye IGV
    }

    // ------------------------------------------------------------------ autorizacion (RF014)

    @Test
    @DisplayName("RF013: sin autorizacion del Administrador no se emite")
    void sinAutorizacionNoSeEmite() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> emitir(1));
        assertTrue(e.getMessage().contains("autorización"), e.getMessage());
    }

    @Test
    @DisplayName("RF014: la autorizacion sirve para una sola nota")
    void autorizacionDeUnSoloUso() throws Exception {
        SolicitudAutorizacion autorizacion = autorizarNota(1);
        emitir(1);
        assertEquals(EstadoAutorizacion.VENCIDA, autorizaciones.obtener(autorizacion.getId()).getEstado());
        assertThrows(IllegalStateException.class, () -> emitir(1), "la segunda nota necesita otra autorizacion");
    }

    @Test
    @DisplayName("RF014: la autorizacion es de ESA nota: otra cantidad necesita otra autorizacion")
    void autorizacionSoloParaLaNotaPedida() throws Exception {
        SolicitudAutorizacion autorizacion = autorizarNota(1);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> emitir(2));
        assertTrue(e.getMessage().contains("autorización"), e.getMessage());
        assertEquals(EstadoAutorizacion.APROBADA, autorizaciones.obtener(autorizacion.getId()).getEstado(),
                "la autorizacion de 1 unidad sigue sin usar");
        assertEquals(autorizacion.getId(), comprobantes.solicitarNotaCredito(pedido(1), CAJERO).getId(),
                "pedir otra vez la misma nota devuelve la misma solicitud");
    }

    @Test
    @DisplayName("RF014: la vigencia de una autorizacion va de 1 a 1440 minutos")
    void vigenciaDentroDelLimite() throws Exception {
        sinAutorizaciones(CAJERO, NOTA);
        SolicitudAutorizacion s = comprobantes.solicitarNotaCredito(pedido(1), CAJERO);
        assertThrows(IllegalArgumentException.class, () -> autorizaciones.aprobar(s.getId(), ADMINISTRADOR, 0));
        assertThrows(IllegalArgumentException.class, () -> autorizaciones.aprobar(s.getId(), ADMINISTRADOR, 1441));
        assertEquals(EstadoAutorizacion.PENDIENTE, autorizaciones.obtener(s.getId()).getEstado(), "sigue pendiente");
        assertEquals(EstadoAutorizacion.APROBADA, autorizaciones.aprobar(s.getId(), ADMINISTRADOR, 1440).getEstado());
    }

    // ------------------------------------------------------------------ cantidades y montos

    @Test
    @DisplayName("RF013: solo se devuelve lo entregado; si falla, la autorizacion no se gasta")
    void soloSeDevuelveLoEntregado() throws Exception {
        SolicitudAutorizacion autorizacion = autorizarNota(1);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> emitir(4),
                "se vendieron 4 pero solo se entregaron 3");
        assertTrue(e.getMessage().contains("Solo se puede devolver lo entregado"), e.getMessage());
        assertEquals(EstadoAutorizacion.APROBADA, autorizaciones.obtener(autorizacion.getId()).getEstado(),
                "el rollback deja la autorizacion sin usar");
    }

    @Test
    @DisplayName("RF013: lo ya acreditado no se vuelve a devolver")
    void loAcreditadoNoSeDevuelveDosVeces() throws Exception {
        autorizarNota(2);
        emitir(2);
        // la cantidad se valida antes que la autorizacion: no hace falta pedirla para que se rechace
        assertThrows(IllegalArgumentException.class, () -> emitir(2), "entregadas 3, ya acreditadas 2: queda 1");
        autorizarNota(1);
        Comprobante ultima = emitir(1);
        assertTrue(ultima.getId() > 0);
    }

    @Test
    @DisplayName("RF013: la nota calcula el monto, reingresa el stock y rebaja la deuda")
    void montoStockYDeuda() throws Exception {
        autorizarNota(2);
        double stockAntes = stockFisico(producto.getId());
        double saldoAntes = saldoCuenta(venta.getId());

        Comprobante nota = emitir(2);

        assertEquals(totalEsperado(2), nota.getTotal(), CENTAVO, "precio cobrado por unidad devuelta (con IGV incluido)");
        assertTrue(nota.getTotal() <= factura.getTotal(), "nunca supera el comprobante de origen");
        assertTrue(nota.getSerie().equals("FC01"), "una nota sobre factura usa la serie FC01");
        assertEquals(stockAntes + 2, stockFisico(producto.getId()), 0.001, "las 2 unidades vuelven al almacen");
        assertEquals(saldoAntes - nota.getTotal(), saldoCuenta(venta.getId()), CENTAVO, "la deuda baja en el total de la nota");
    }

    // ------------------------------------------------------------------ anular y modificar

    @Test
    @DisplayName("RF013: anular la nota devuelve el stock y la deuda a como estaban")
    void anularRevierteStockYDeuda() throws Exception {
        autorizarNota(2);
        double stockAntes = stockFisico(producto.getId());
        double saldoAntes = saldoCuenta(venta.getId());
        Comprobante nota = emitir(2);

        comprobantes.anular(nota.getId(), "Prueba de anulación", CAJERO);

        assertEquals(EstadoComprobante.ANULADO, comprobantes.obtener(nota.getId()).getEstado());
        assertEquals(stockAntes, stockFisico(producto.getId()), 0.001);
        assertEquals(saldoAntes, saldoCuenta(venta.getId()), CENTAVO);
        List<MovimientoInventario> movimientos = Transaccion.consultar(() ->
                new MovimientoInventarioDAOImpl().listarPorComprobante(nota.getId()));
        List<MovimientoInventario> ingresos = movimientos.stream()
                .filter(m -> m.getTipo() == TipoMovimientoInventario.INGRESO_DEVOLUCION).toList();
        List<MovimientoInventario> reversiones = movimientos.stream()
                .filter(m -> m.getTipo() == TipoMovimientoInventario.SALIDA_REVERSION_DEVOLUCION).toList();
        assertEquals(1, ingresos.size(), "la nota registra un ingreso de devolucion");
        assertEquals(1, reversiones.size(), "la anulacion registra una reversion especifica");
        assertEquals(ingresos.get(0).getId(), reversiones.get(0).getIdMovimientoRevertido(),
                "la salida debe apuntar al ingreso original");
        assertEquals(ingresos.get(0).getCantidad(), reversiones.get(0).getCantidad(), 0.001);
        assertThrows(IllegalStateException.class,
                () -> comprobantes.anular(nota.getId(), "Otra vez", CAJERO), "no se anula dos veces");
    }

    @Test
    @DisplayName("RF013: el Vendedor no anula notas de credito")
    void vendedorNoAnulaNotas() throws Exception {
        autorizarNota(1);
        Comprobante nota = emitir(1);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> comprobantes.anular(nota.getId(), "Intento del vendedor", VENDEDOR));
        assertTrue(e.getMessage().startsWith("El rol"), "el servicio REST lo responde como 403: " + e.getMessage());
        assertEquals(EstadoComprobante.EMITIDO, comprobantes.obtener(nota.getId()).getEstado());
    }

    @Test
    @DisplayName("RF013: la factura con nota de credito vigente no se anula")
    void facturaConNotaNoSeAnula() throws Exception {
        autorizarNota(1);
        emitir(1);
        assertThrows(IllegalStateException.class,
                () -> comprobantes.anular(factura.getId(), "Intento con nota vigente", CAJERO));
    }

    @Test
    @DisplayName("RF013: modificar cambia solo el motivo y no se permite en una nota anulada")
    void modificarSoloCambiaElMotivo() throws Exception {
        autorizarNota(1);
        Comprobante nota = emitir(1);
        Comprobante antes = comprobantes.obtener(nota.getId());

        Comprobante cambios = new Comprobante();
        cambios.setId(nota.getId());
        cambios.setMotivo("Motivo corregido en prueba JUnit");
        comprobantes.modificarNotaCredito(cambios, CAJERO);

        Comprobante despues = comprobantes.obtener(nota.getId());
        assertEquals("Motivo corregido en prueba JUnit", despues.getMotivo());
        assertEquals(antes.getTotal(), despues.getTotal(), CENTAVO, "los montos no cambian");
        assertEquals(antes.getMedioEnvio(), despues.getMedioEnvio(), "el envio se conserva si no se manda");
        assertEquals(antes.getFechaEnvio(), despues.getFechaEnvio());

        Comprobante vacio = new Comprobante();
        vacio.setId(nota.getId());
        vacio.setMotivo("   ");
        assertThrows(IllegalArgumentException.class, () -> comprobantes.modificarNotaCredito(vacio, CAJERO),
                "el motivo es obligatorio");

        comprobantes.anular(nota.getId(), "Prueba de anulación", CAJERO);
        assertThrows(IllegalStateException.class, () -> comprobantes.modificarNotaCredito(cambios, CAJERO),
                "una nota anulada ya no se modifica");
    }
}
