package pe.edu.pucp.sigmeta.boimpl.compras;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;

/** Alcance del proyecto: compras gravadas al 18%; descuento absoluto por linea. */
final class CalculoCompra {
    private static final BigDecimal TASA = new BigDecimal("0.18");
    private CalculoCompra() { }
    static void linea(DetalleCompra d) {
        if (!Double.isFinite(d.getCantidad()) || d.getCantidad() <= 0
                || !Double.isFinite(d.getPrecioUnitario()) || d.getPrecioUnitario() < 0
                || !Double.isFinite(d.getDescuento()) || d.getDescuento() < 0) {
            throw new IllegalArgumentException("Cantidad, precio o descuento invalido");
        }
        BigDecimal cantidad = BigDecimal.valueOf(d.getCantidad()).setScale(3, RoundingMode.HALF_UP);
        BigDecimal precio = BigDecimal.valueOf(d.getPrecioUnitario()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal descuento = BigDecimal.valueOf(d.getDescuento()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal importe = cantidad.multiply(precio).subtract(descuento);
        if (cantidad.signum() <= 0 || importe.signum() < 0) throw new IllegalArgumentException("Cantidad redondeada nula o descuento mayor al bruto");
        d.setCantidad(cantidad.doubleValue());d.setPrecioUnitario(precio.doubleValue());d.setDescuento(descuento.doubleValue());
        d.setImporte(importe.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }
    static void totales(Compra c, List<DetalleCompra> detalles) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleCompra d : detalles) { linea(d);subtotal = subtotal.add(BigDecimal.valueOf(d.getImporte())); }
        BigDecimal igv = subtotal.multiply(TASA).setScale(2, RoundingMode.HALF_UP);
        c.setSubTotal(subtotal.doubleValue());c.setIgv(igv.doubleValue());c.setTotal(subtotal.add(igv).doubleValue());
    }
}
