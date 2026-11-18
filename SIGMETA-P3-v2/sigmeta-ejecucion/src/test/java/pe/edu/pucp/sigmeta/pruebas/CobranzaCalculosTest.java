package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.sigmeta.boimpl.cobranza.CuentaPorCobrarBOImpl;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;

// H14: pruebas unitarias puras (no usan la base): reglas de calculo de cobranza (RF009). Corren siempre con "mvn test".
@DisplayName("RF009 - Calculos de cobranza sin base de datos")
class CobranzaCalculosTest {

    @Test
    @DisplayName("Estado de la cuenta: PAGADA, VENCIDA, PAGADA_PARCIAL o PENDIENTE segun saldo y vencimiento")
    void estadoDeLaCuenta() {
        assertEquals(EstadoCuentaPorCobrar.PAGADA, CuentaPorCobrarBOImpl.calcularEstado(cuenta(100, 100, 5)));
        assertEquals(EstadoCuentaPorCobrar.PAGADA, CuentaPorCobrarBOImpl.calcularEstado(cuenta(100, 99.996, -5)));
        assertEquals(EstadoCuentaPorCobrar.VENCIDA, CuentaPorCobrarBOImpl.calcularEstado(cuenta(100, 40, -1)));
        assertEquals(EstadoCuentaPorCobrar.PAGADA_PARCIAL, CuentaPorCobrarBOImpl.calcularEstado(cuenta(100, 40, 0)));
        assertEquals(EstadoCuentaPorCobrar.PENDIENTE, CuentaPorCobrarBOImpl.calcularEstado(cuenta(100, 0, 10)));
    }

    @Test
    @DisplayName("Una cuenta vencida es la que tiene saldo y paso su fecha de vencimiento")
    void cuentaVencida() {
        assertTrue(cuenta(100, 40, -1).isVencida());
        assertFalse(cuenta(100, 100, -1).isVencida());
        assertFalse(cuenta(100, 40, 0).isVencida());
    }

    @Test
    @DisplayName("Los importes se redondean a 2 decimales (mitad hacia arriba), sin errores de double")
    void redondeo() {
        assertEquals(0.30, CuentaPorCobrarBOImpl.redondear(0.1 + 0.2), 0.0);
        assertEquals(2.68, CuentaPorCobrarBOImpl.redondear(2.675), 0.0);
        assertEquals(-5.25, CuentaPorCobrarBOImpl.redondear(-5.249999), 0.0);
        assertEquals(1534.00, CuentaPorCobrarBOImpl.redondear(1300.00 * 1.18), 0.0);
    }


    // cuenta de monto original con lo pagado y vencimiento a "dias" de hoy (negativo = ya vencio)
    private static CuentaPorCobrar cuenta(double original, double pagado, int dias) {
        CuentaPorCobrar c = new CuentaPorCobrar();
        c.setMontoOriginal(original);
        c.setMontoPagado(pagado);
        c.setSaldoPendiente(CuentaPorCobrarBOImpl.redondear(Math.max(0, original - pagado)));
        c.setFechaEmision(LocalDate.now().minusDays(30));
        c.setFechaVencimiento(LocalDate.now().plusDays(dias));
        return c;
    }
}
