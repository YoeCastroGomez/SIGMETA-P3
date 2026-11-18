package pe.edu.pucp.sigmeta.boimpl.ventas;

import java.math.BigDecimal;
import java.math.RoundingMode;

// RF007, RF008 y RF013: los precios de venta del catalogo YA INCLUYEN el IGV (18 %). El total de una cotizacion,
// una venta o una nota de credito es la suma de los importes de sus lineas, y el valor de venta (sub_total) y el
// IGV se desglosan de ese total:
//     valor de venta = total / 1.18 (2 decimales, HALF_UP)        IGV = total - valor de venta
// El costo del producto es sin IGV (viene de las compras), por eso un precio se compara con el costo sin su IGV.
final class PreciosIgv {

    static final BigDecimal FACTOR = new BigDecimal("1.18");

    private PreciosIgv() {
    }

    static BigDecimal valorVenta(BigDecimal total) {
        return total.divide(FACTOR, 2, RoundingMode.HALF_UP);
    }

    static BigDecimal igv(BigDecimal total) {
        return total.subtract(valorVenta(total));
    }

    static double sinIgv(double precio) {
        return precio / FACTOR.doubleValue();
    }
}
