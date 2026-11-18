package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static pe.edu.pucp.sigmeta.pruebas.Datos.exigirBaseLocal;

import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pe.edu.pucp.sigmeta.boimpl.caja.CajaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.caja.CierreCajaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.boimpl.caja.MovimientoCajaBOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.EstadoCierreCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;

// H14 - RF010: pruebas del cierre de caja con JUnit 5 sobre la base de pruebas (se activan con -Dsigmeta.integracion=true, solo base local).
// Cada prueba abre su propia caja y, si queda abierta, la cierra al terminar.
@EnabledIfSystemProperty(named = "sigmeta.integracion", matches = "true")
@DisplayName("RF010 - Cierre de caja")
class CierreCajaBOTest {

    private static int cajero;
    private static int administrador;
    private static int vendedor;

    private final CajaBOImpl cajaBO = new CajaBOImpl();
    private final CierreCajaBOImpl cierreBO = new CierreCajaBOImpl();
    private final MovimientoCajaBOImpl movimientoBO = new MovimientoCajaBOImpl();
    private Caja caja;

    @BeforeAll
    static void prepararUsuarios() throws SQLException {
        exigirBaseLocal();
        cajero = DatosCaja.cajeroSinCajaAbierta();
        administrador = DatosCaja.usuarioActivo("ADMINISTRADOR");
        vendedor = DatosCaja.usuarioActivo("VENDEDOR");
        assumeTrue(cajero > 0 && administrador > 0, "Se necesita un cajero sin caja abierta y un administrador");
    }

    @BeforeEach
    void abrirCaja() throws SQLException {
        Caja nueva = new Caja();
        nueva.setMontoInicial(200.00);
        caja = cajaBO.abrirCaja(nueva, cajero);
    }

    @AfterEach
    void cerrarSiQuedoAbierta() throws SQLException {
        int abierta = DatosCaja.cajaAbiertaDe(cajero);
        if (abierta > 0) {
            CierreCaja cierre = nuevoCierre(abierta, cierreBO.calcularMontoEsperado(abierta));
            cierreBO.registrarCierre(cierre, cajero);
        }
    }

    @Test
    @DisplayName("El cierre calcula monto inicial + ingresos - egresos y registra la diferencia y el responsable")
    void cierreCalculaMontoYDiferencia() throws SQLException {
        movimiento(TipoMovimientoCaja.INGRESO_MANUAL, MedioPago.EFECTIVO, 80.50);
        movimiento(TipoMovimientoCaja.INGRESO_MANUAL, MedioPago.TRANSFERENCIA, 120.00);
        movimiento(TipoMovimientoCaja.EGRESO_MANUAL, MedioPago.EFECTIVO, 30.25);

        assertEquals(370.25, cierreBO.calcularMontoEsperado(caja.getId()), 0.001);
        CierreCaja cierre = cierreBO.registrarCierre(nuevoCierre(caja.getId(), 365.00), cajero);

        assertEquals(370.25, cierre.getMontoCalculado(), 0.001);
        assertEquals(-5.25, cierre.getDiferencia(), 0.001);
        assertEquals(EstadoCierreCaja.VIGENTE, cierre.getEstado());
        assertEquals(cajero, cierre.getUsuarioCierre().getId());
        assertNotNull(cierre.getFechaCierre());
        assertFalse(cajaBO.obtenerPorId(caja.getId()).isAbierta());
    }

    @Test
    @DisplayName("Un cobro compensado (venta anulada o nota de credito) no suma en el cierre")
    void movimientoCompensadoNoSuma() throws SQLException {
        MovimientoCaja ingreso = movimiento(TipoMovimientoCaja.INGRESO_MANUAL, MedioPago.EFECTIVO, 50.00);
        // es lo que hacen VentaBO.anular y ComprobanteBO al anular: el movimiento contrario en la misma caja
        pe.edu.pucp.sigmeta.boimpl.Transaccion.ejecutar(() -> new CompensacionCaja().compensar(ingreso,
                "Devolucion por anulacion (prueba)", CompensacionCaja.DOCUMENTO_ANULACION_VENTA, 999999,
                new pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl().obtener(cajero)));

        assertEquals(200.00, cierreBO.calcularMontoEsperado(caja.getId()), 0.001);
        CierreCaja cierre = cierreBO.registrarCierre(nuevoCierre(caja.getId(), 200.00), cajero);
        assertEquals(0.00, cierre.getDiferencia(), 0.001);
    }

    @Test
    @DisplayName("H06: una devolucion en efectivo no puede superar el efectivo de la caja")
    void devolucionNoSuperaElEfectivo() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                pe.edu.pucp.sigmeta.boimpl.Transaccion.ejecutar(() -> new CompensacionCaja().egresoEnCajaAbierta(cajero,
                        MedioPago.EFECTIVO, 250.00, "Devolucion por NC (prueba)", CompensacionCaja.DOCUMENTO_NOTA_CREDITO, 999999,
                        new pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl().obtener(cajero))));
        assertTrue(e.getMessage().contains("efectivo"), e.getMessage());
    }

    @Test
    @DisplayName("Un egreso manual en efectivo no puede superar el efectivo de la caja")
    void egresoManualNoSuperaElEfectivo() {
        assertThrows(IllegalStateException.class, () -> movimiento(TipoMovimientoCaja.EGRESO_MANUAL, MedioPago.EFECTIVO, 200.01));
    }

    @Test
    @DisplayName("Solo el cajero que abrio la caja puede cerrarla; otros roles no cierran")
    void soloElCajeroQueAbrioCierra() throws SQLException {
        int otro = DatosCaja.otroCajero(cajero);
        if (otro > 0) {
            assertThrows(IllegalStateException.class, () -> cierreBO.registrarCierre(nuevoCierre(caja.getId(), 200), otro));
        }
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> cierreBO.registrarCierre(nuevoCierre(caja.getId(), 200), administrador));
        assertTrue(e.getMessage().startsWith("El rol "), e.getMessage());
        if (vendedor > 0) {
            assertThrows(IllegalStateException.class, () -> cierreBO.registrarCierre(nuevoCierre(caja.getId(), 200), vendedor));
        }
    }

    @Test
    @DisplayName("No se acepta un monto declarado negativo ni cerrar dos veces")
    void validacionesDelCierre() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> cierreBO.registrarCierre(nuevoCierre(caja.getId(), -1), cajero));
        cierreBO.registrarCierre(nuevoCierre(caja.getId(), 200), cajero);
        assertThrows(IllegalStateException.class, () -> cierreBO.registrarCierre(nuevoCierre(caja.getId(), 200), cajero));
    }

    @Test
    @DisplayName("Solo el Administrador reabre una caja cerrada; el cierre queda REVERTIDO con motivo")
    void soloElAdministradorReabre() throws SQLException {
        CierreCaja cierre = cierreBO.registrarCierre(nuevoCierre(caja.getId(), 200), cajero);

        IllegalStateException sinPermiso = assertThrows(IllegalStateException.class,
                () -> cierreBO.reabrir(cierre.getId(), "Falto un cobro", cajero));
        assertTrue(sinPermiso.getMessage().startsWith("El rol "), sinPermiso.getMessage());
        assertThrows(IllegalArgumentException.class, () -> cierreBO.reabrir(cierre.getId(), "  ", administrador));

        CierreCaja revertido = cierreBO.reabrir(cierre.getId(), "Falto registrar un cobro", administrador);
        assertEquals(EstadoCierreCaja.REVERTIDO, revertido.getEstado());
        assertEquals(administrador, revertido.getUsuarioReapertura().getId());
        assertEquals("Falto registrar un cobro", revertido.getMotivoReapertura());
        assertTrue(cajaBO.obtenerPorId(caja.getId()).isAbierta());
        assertThrows(IllegalStateException.class, () -> cierreBO.reabrir(cierre.getId(), "otra vez", administrador));

        // al cerrar de nuevo se registra un cierre nuevo; el revertido se conserva en el historial
        CierreCaja nuevo = cierreBO.registrarCierre(nuevoCierre(caja.getId(), 200), cajero);
        assertNotEquals(cierre.getId(), nuevo.getId());
        assertEquals(2, cierreBO.listarPorCaja(caja.getId()).size());
    }

    @Test
    @DisplayName("Solo el Administrador corrige el monto declarado de un cierre; la diferencia se recalcula")
    void soloElAdministradorCorrige() throws SQLException {
        CierreCaja cierre = cierreBO.registrarCierre(nuevoCierre(caja.getId(), 190), cajero);
        assertEquals(-10.00, cierre.getDiferencia(), 0.001);

        CierreCaja correccion = new CierreCaja();
        correccion.setId(cierre.getId());
        correccion.setMontoDeclarado(205.00);
        assertThrows(IllegalStateException.class, () -> cierreBO.modificar(correccion, cajero));

        CierreCaja corregido = cierreBO.modificar(correccion, administrador);
        assertEquals(205.00, corregido.getMontoDeclarado(), 0.001);
        assertEquals(5.00, corregido.getDiferencia(), 0.001);
        assertEquals(200.00, corregido.getMontoCalculado(), 0.001);
    }

    @Test
    @DisplayName("Con la caja cerrada no se registran movimientos")
    void cajaCerradaNoAceptaMovimientos() throws SQLException {
        cierreBO.registrarCierre(nuevoCierre(caja.getId(), 200), cajero);
        assertThrows(IllegalStateException.class, () -> movimiento(TipoMovimientoCaja.INGRESO_MANUAL, MedioPago.EFECTIVO, 10));
    }

    private MovimientoCaja movimiento(TipoMovimientoCaja tipo, MedioPago medio, double monto) throws SQLException {
        MovimientoCaja m = new MovimientoCaja();
        m.setCaja(caja);
        m.setTipo(tipo);
        m.setMedioPago(medio);
        m.setMonto(monto);
        m.setConcepto("Prueba JUnit " + tipo);
        return movimientoBO.registrarMovimiento(m, cajero);
    }

    private static CierreCaja nuevoCierre(int idCaja, double declarado) {
        Caja c = new Caja();
        c.setId(idCaja);
        CierreCaja cierre = new CierreCaja();
        cierre.setCaja(c);
        cierre.setMontoDeclarado(declarado);
        return cierre;
    }
}
