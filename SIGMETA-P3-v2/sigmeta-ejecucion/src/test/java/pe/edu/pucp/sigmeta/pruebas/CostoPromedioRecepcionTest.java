package pe.edu.pucp.sigmeta.pruebas;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import pe.edu.pucp.sigmeta.boimpl.almacen.CostoPromedioRecepcion;
import static org.junit.jupiter.api.Assertions.*;

class CostoPromedioRecepcionTest {
    private static BigDecimal d(String n) { return new BigDecimal(n); }

    @Test void promedioConStockExistente() {
        assertEquals(d("25.00"), CostoPromedioRecepcion.calcular(
                d("10"), d("20.00"), d("10"), d("30.00")));
    }

    @Test void primeraRecepcionSinStock() {
        assertEquals(d("30.00"), CostoPromedioRecepcion.calcular(
                d("0"), d("15.00"), d("10"), d("30.00")));
    }

    @Test void promedioSeRedondeaEnDosDecimales() {
        assertEquals(d("13.33"), CostoPromedioRecepcion.calcular(
                d("2"), d("10.00"), d("1"), d("20.00")));
    }

    @Test void permiteRestaurarSoloCostoEsperado() {
        assertDoesNotThrow(() -> CostoPromedioRecepcion.validarCostoActual(d("25.00"), d("25.00")));
        assertThrows(IllegalStateException.class,
                () -> CostoPromedioRecepcion.validarCostoActual(d("26.00"), d("25.00")));
    }

    @Test void rechazaDatosNoValidos() {
        assertThrows(IllegalArgumentException.class,
                () -> CostoPromedioRecepcion.calcular(d("-1"), d("20"), d("2"), d("10")));
        assertThrows(IllegalArgumentException.class,
                () -> CostoPromedioRecepcion.calcular(d("2"), d("20"), d("0"), d("10")));
        assertThrows(IllegalArgumentException.class,
                () -> CostoPromedioRecepcion.calcular(d("2"), d("-20"), d("2"), d("10")));
    }
}
