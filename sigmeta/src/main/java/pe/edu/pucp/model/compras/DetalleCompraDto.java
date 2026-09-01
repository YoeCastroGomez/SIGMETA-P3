package pe.edu.pucp.model.compras;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.enums.UnidadMedida;
import pe.edu.pucp.model.producto.ProductoDto;

/**
 * Linea de una compra. La cantidad y el costo van en la unidad de compra, y el factor de conversion permite convertirlos a unidad de venta al recibir (RF010, RF011).
 */
public class DetalleCompraDto extends LineaDocumento {

    private int idDetalleCompra;
    private CompraDto compra;
    private UnidadMedida unidadCompra;
    private double factorConversion;
    private double cantidadRecibida;

    public DetalleCompraDto() {
        super();
    }

    public DetalleCompraDto(int numeroLinea, ProductoDto producto, double cantidad, double precioUnitario, double descuento, double importe, int idDetalleCompra, CompraDto compra, UnidadMedida unidadCompra, double factorConversion, double cantidadRecibida) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.idDetalleCompra = idDetalleCompra;
        this.compra = compra;
        this.unidadCompra = unidadCompra;
        this.factorConversion = factorConversion;
        this.cantidadRecibida = cantidadRecibida;
    }

    public int getIdDetalleCompra() {
        return idDetalleCompra;
    }

    public void setIdDetalleCompra(int idDetalleCompra) {
        this.idDetalleCompra = idDetalleCompra;
    }

    public CompraDto getCompra() {
        return compra;
    }

    public void setCompra(CompraDto compra) {
        this.compra = compra;
    }

    public UnidadMedida getUnidadCompra() {
        return unidadCompra;
    }

    public void setUnidadCompra(UnidadMedida unidadCompra) {
        this.unidadCompra = unidadCompra;
    }

    public double getFactorConversion() {
        return factorConversion;
    }

    public void setFactorConversion(double factorConversion) {
        this.factorConversion = factorConversion;
    }

    public double getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(double cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }

}
