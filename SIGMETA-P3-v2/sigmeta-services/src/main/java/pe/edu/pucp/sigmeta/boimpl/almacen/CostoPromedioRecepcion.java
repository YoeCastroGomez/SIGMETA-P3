package pe.edu.pucp.sigmeta.boimpl.almacen;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Costeo promedio ponderado de una recepcion.
 * Guarda el costo anterior sin reconstruirlo a partir de un promedio ya redondeado.
 */
public final class CostoPromedioRecepcion {
    private CostoPromedioRecepcion() {}

    public static BigDecimal calcular(BigDecimal stockAnterior, BigDecimal costoAnterior,
                                      BigDecimal ingreso, BigDecimal costoCompra) {
        if (stockAnterior == null || costoAnterior == null || ingreso == null || costoCompra == null
                || stockAnterior.signum() < 0 || costoAnterior.signum() < 0
                || ingreso.signum() <= 0 || costoCompra.signum() < 0) {
            throw new IllegalArgumentException("Datos invalidos para calcular el costo promedio");
        }
        BigDecimal total = stockAnterior.add(ingreso);
        if (total.signum() <= 0) {
            throw new IllegalArgumentException("El stock total debe ser positivo");
        }
        return stockAnterior.multiply(costoAnterior).add(ingreso.multiply(costoCompra))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    public static void validarCostoActual(BigDecimal actual, BigDecimal posterior) {
        if (actual == null || posterior == null || actual.compareTo(posterior) != 0) {
            throw new IllegalStateException("El costo fue modificado despues de esta recepcion; "
                    + "no se puede restaurar sin conciliar el historial");
        }
    }
}
