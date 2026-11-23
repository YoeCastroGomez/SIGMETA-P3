package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static pe.edu.pucp.sigmeta.pruebas.Datos.exigirBaseLocal;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pe.edu.pucp.sigmeta.boimpl.caja.CajaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.cobranza.CobroBOImpl;
import pe.edu.pucp.sigmeta.boimpl.cobranza.CuentaPorCobrarBOImpl;
import pe.edu.pucp.sigmeta.boimpl.caja.CierreCajaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.cobranza.Cobro;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;

// H14 - RF009/RF010: pruebas del registro de cobros con JUnit 5 sobre la base de pruebas (se activan con -Dsigmeta.integracion=true, solo base local).
// Cada prueba abre una caja, trabaja sobre una cuenta con saldo y anula sus cobros al terminar,
// asi el saldo de la cuenta queda como estaba.
@EnabledIfSystemProperty(named = "sigmeta.integracion", matches = "true")
@DisplayName("RF009 - Registro de cobros")
class CobroBOTest {

    private static int cajero;
    private static int administrador;
    private static int vendedor;
    private static int idCuenta;

    private final CobroBOImpl cobroBO = new CobroBOImpl();
    private final CuentaPorCobrarBOImpl cuentaBO = new CuentaPorCobrarBOImpl();
    private final CajaBOImpl cajaBO = new CajaBOImpl();
    private final CierreCajaBOImpl cierreBO = new CierreCajaBOImpl();
    private Caja caja;
    private double saldoInicial;

    @BeforeAll
    static void prepararDatos() throws SQLException {
        exigirBaseLocal();
        cajero = DatosCaja.cajeroSinCajaAbierta();
        administrador = DatosCaja.usuarioActivo("ADMINISTRADOR");
        vendedor = DatosCaja.usuarioActivo("VENDEDOR");
        idCuenta = DatosCaja.cuentaConSaldo(30.00);
        assumeTrue(cajero > 0 && administrador > 0 && idCuenta > 0,
                "Se necesita un cajero sin caja abierta, un administrador y una cuenta por cobrar con saldo");
    }

    @BeforeEach
    void abrirCaja() throws SQLException {
        Caja nueva = new Caja();
        nueva.setMontoInicial(100.00);
        caja = cajaBO.abrirCaja(nueva, cajero);
        saldoInicial = cuentaBO.obtenerPorId(idCuenta).getSaldoPendiente();
    }

    @AfterEach
    void dejarTodoComoEstaba() throws SQLException {
        // reabre la caja si la prueba la cerro, anula los cobros vigentes de la prueba y cierra la caja
        CierreCaja vigente = cierreBO.obtenerPorCaja(caja.getId());
        if (vigente != null) {
            cierreBO.reabrir(vigente.getId(), "Limpieza de la prueba JUnit", administrador);
        }
        for (Cobro c : cobroBO.listarPorCuentaPorCobrar(idCuenta)) {
            if (!c.isAnulado() && "JUNIT".equals(c.getReferencia())) {
                cobroBO.eliminar(c, cajero);
            }
        }
        if (DatosCaja.cajaAbiertaDe(cajero) > 0) {
            Caja c = new Caja();
            c.setId(caja.getId());
            CierreCaja cierre = new CierreCaja();
            cierre.setCaja(c);
            cierre.setMontoDeclarado(cierreBO.calcularMontoEsperado(caja.getId()));
            cierreBO.registrarCierre(cierre, cajero);
        }
        assertEquals(saldoInicial, cuentaBO.obtenerPorId(idCuenta).getSaldoPendiente(), 0.001, "El saldo de la cuenta debe quedar como estaba");
    }

    @Test
    @DisplayName("El cobro baja el saldo, cambia el estado y entra a la caja como COBRO_CREDITO")
    void cobroActualizaSaldoYCaja() throws SQLException {
        Cobro cobro = cobroBO.registrarCobro(nuevoCobro(10.00, MedioPago.TRANSFERENCIA), cajero);

        CuentaPorCobrar cxc = cuentaBO.obtenerPorId(idCuenta);
        assertEquals(saldoInicial - 10.00, cxc.getSaldoPendiente(), 0.001);
        assertNotEquals(EstadoCuentaPorCobrar.PAGADA, cxc.getEstado());
        assertEquals(cajero, cobro.getUsuarioRegistro().getId());

        List<MovimientoCaja> ingresos = new CompensacionCaja().buscar(CobroBOImpl.DOCUMENTO_COBRO, cobro.getId());
        assertEquals(1, ingresos.size());
        assertEquals(TipoMovimientoCaja.COBRO_CREDITO, ingresos.get(0).getTipo());
        assertEquals(caja.getId(), ingresos.get(0).getCaja().getId());
        assertEquals(10.00, ingresos.get(0).getMonto(), 0.001);
        assertEquals(110.00, cierreBO.calcularMontoEsperado(caja.getId()), 0.001);
    }

    @Test
    @DisplayName("El cobro que completa el saldo deja la cuenta PAGADA y ya no admite mas cobros")
    void cobroTotalPagaLaCuenta() throws SQLException {
        cobroBO.registrarCobro(nuevoCobro(saldoInicial, MedioPago.DEPOSITO), cajero);
        CuentaPorCobrar cxc = cuentaBO.obtenerPorId(idCuenta);
        assertEquals(0.00, cxc.getSaldoPendiente(), 0.001);
        assertEquals(EstadoCuentaPorCobrar.PAGADA, cxc.getEstado());
        assertThrows(IllegalStateException.class, () -> cobroBO.registrarCobro(nuevoCobro(1.00, MedioPago.EFECTIVO), cajero));
    }

    @Test
    @DisplayName("Se rechaza un cobro mayor al saldo, con monto no positivo o con fecha futura")
    void validacionesDelCobro() {
        assertThrows(IllegalArgumentException.class, () -> cobroBO.registrarCobro(nuevoCobro(saldoInicial + 0.01, MedioPago.EFECTIVO), cajero));
        assertThrows(IllegalArgumentException.class, () -> cobroBO.registrarCobro(nuevoCobro(0, MedioPago.EFECTIVO), cajero));
        assertThrows(IllegalArgumentException.class, () -> cobroBO.registrarCobro(nuevoCobro(-5, MedioPago.EFECTIVO), cajero));
        Cobro futuro = nuevoCobro(1.00, MedioPago.EFECTIVO);
        futuro.setFechaCobro(LocalDate.now().plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> cobroBO.registrarCobro(futuro, cajero));
        Cobro sinMedio = nuevoCobro(1.00, null);
        assertThrows(IllegalArgumentException.class, () -> cobroBO.registrarCobro(sinMedio, cajero));
    }

    @Test
    @DisplayName("Solo el Cajero con su caja abierta registra cobros")
    void soloCajeroConCajaAbierta() throws SQLException {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> cobroBO.registrarCobro(nuevoCobro(1.00, MedioPago.EFECTIVO), administrador));
        assertTrue(e.getMessage().startsWith("El rol "), e.getMessage());
        if (vendedor > 0) {
            assertThrows(IllegalStateException.class, () -> cobroBO.registrarCobro(nuevoCobro(1.00, MedioPago.EFECTIVO), vendedor));
        }
        cerrarCaja();
        IllegalStateException sinCaja = assertThrows(IllegalStateException.class,
                () -> cobroBO.registrarCobro(nuevoCobro(1.00, MedioPago.EFECTIVO), cajero));
        assertTrue(sinCaja.getMessage().contains("caja"), sinCaja.getMessage());
    }

    @Test
    @DisplayName("Anular un cobro devuelve el saldo y registra el egreso que lo compensa en la caja")
    void anularCobroDevuelveSaldo() throws SQLException {
        Cobro cobro = cobroBO.registrarCobro(nuevoCobro(12.40, MedioPago.EFECTIVO), cajero);
        cobroBO.eliminar(cobro, cajero);

        assertTrue(cobroBO.obtenerPorId(cobro.getId()).isAnulado());
        assertEquals(saldoInicial, cuentaBO.obtenerPorId(idCuenta).getSaldoPendiente(), 0.001);
        assertEquals(1, new CompensacionCaja().buscar(CobroBOImpl.DOCUMENTO_ANULACION_COBRO, cobro.getId()).size());
        assertEquals(100.00, cierreBO.calcularMontoEsperado(caja.getId()), 0.001);
        assertThrows(IllegalStateException.class, () -> cobroBO.eliminar(cobro, cajero));
    }

    @Test
    @DisplayName("RNF001: anular con motivo deja quien, cuando y por que (registro_anulacion e historial de la cuenta)")
    void anularConMotivoQuedaRegistrado() throws SQLException {
        Cobro cobro = cobroBO.registrarCobro(nuevoCobro(7.25, MedioPago.EFECTIVO), cajero);
        assertThrows(IllegalArgumentException.class, () -> cobroBO.anular(cobro, "  ", cajero));
        cobroBO.anular(cobro, "Se registro en la cuenta equivocada", cajero);

        assertEquals(1, DatosCaja.contar("SELECT COUNT(*) FROM registro_anulacion WHERE tipo_documento='COBRO' AND id_documento="
                + cobro.getId() + " AND motivo='Se registro en la cuenta equivocada' AND id_usuario=" + cajero
                + " AND id_cuenta_por_cobrar=" + idCuenta));
        List<java.util.Map<String, Object>> historial = cuentaBO.historial(idCuenta, cajero);
        assertTrue(historial.stream().anyMatch(e -> "COBRO_ANULADO".equals(e.get("evento"))
                && String.valueOf(e.get("detalle")).contains("Se registro en la cuenta equivocada")));
        assertEquals("CUENTA_GENERADA", historial.get(0).get("evento"));
        assertEquals(saldoInicial, cuentaBO.obtenerPorId(idCuenta).getSaldoPendiente(), 0.001);
        assertThrows(IllegalStateException.class, () -> cuentaBO.historial(idCuenta, vendedor));
    }

    @Test
    @DisplayName("Con la caja ya cerrada el cobro no se anula (el Administrador debe reabrirla)")
    void noSeAnulaConCajaCerrada() throws SQLException {
        Cobro cobro = cobroBO.registrarCobro(nuevoCobro(5.00, MedioPago.EFECTIVO), cajero);
        cerrarCaja();
        assertThrows(IllegalStateException.class, () -> cobroBO.eliminar(cobro, cajero));
        assertEquals(saldoInicial - 5.00, cuentaBO.obtenerPorId(idCuenta).getSaldoPendiente(), 0.001);
    }

    @Test
    @DisplayName("Modificar un cobro solo cambia fecha y referencia; el monto se corrige anulando")
    void modificarSoloFechaYReferencia() throws SQLException {
        Cobro cobro = cobroBO.registrarCobro(nuevoCobro(3.00, MedioPago.EFECTIVO), cajero);
        Cobro cambioMonto = new Cobro();
        cambioMonto.setId(cobro.getId());
        cambioMonto.setMonto(4.00);
        assertThrows(IllegalArgumentException.class, () -> cobroBO.modificar(cambioMonto, cajero));

        Cobro cambioReferencia = new Cobro();
        cambioReferencia.setId(cobro.getId());
        cambioReferencia.setMonto(3.00);
        cambioReferencia.setReferencia("JUNIT");
        cambioReferencia.setFechaCobro(LocalDate.now().minusDays(1));
        Cobro modificado = cobroBO.modificar(cambioReferencia, cajero);
        assertEquals(LocalDate.now().minusDays(1), modificado.getFechaCobro());
        assertEquals(3.00, modificado.getMonto(), 0.001);
    }

    @Test
    @DisplayName("La busqueda por rango de fechas (filtrada en la base) encuentra el cobro del dia")
    void busquedaPorFechas() throws SQLException {
        Cobro cobro = cobroBO.registrarCobro(nuevoCobro(2.00, MedioPago.EFECTIVO), cajero);
        List<Cobro> hoy = cobroBO.buscarPorRangoFechas(LocalDate.now(), LocalDate.now());
        assertTrue(hoy.stream().anyMatch(c -> c.getId() == cobro.getId()));
        assertTrue(hoy.stream().allMatch(c -> c.getFechaCobro().equals(LocalDate.now())));
        assertThrows(IllegalArgumentException.class,
                () -> cobroBO.buscarPorRangoFechas(LocalDate.now(), LocalDate.now().minusDays(1)));
    }

    private Cobro nuevoCobro(double monto, MedioPago medio) {
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(idCuenta);
        Cobro cobro = new Cobro();
        cobro.setCuentaPorCobrar(cxc);
        cobro.setMonto(monto);
        cobro.setMedioPago(medio);
        cobro.setReferencia("JUNIT");
        return cobro;
    }

    private void cerrarCaja() throws SQLException {
        Caja c = new Caja();
        c.setId(caja.getId());
        CierreCaja cierre = new CierreCaja();
        cierre.setCaja(c);
        cierre.setMontoDeclarado(cierreBO.calcularMontoEsperado(caja.getId()));
        cierreBO.registrarCierre(cierre, cajero);
    }
}
