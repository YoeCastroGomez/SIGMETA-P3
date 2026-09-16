package pe.edu.pucp.model.compras;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.enums.UnidadMedida;
import pe.edu.pucp.model.producto.Producto;

/**
 * Linea de una compra. La cantidad y el costo van en la unidad de compra, y el factor de conversion permite convertirlos a unidad de venta al recibir (RF010, RF011).
 */
public class DetalleCompra extends LineaDocumento {

    private int id;
    private Compra compra;
    private UnidadMedida unidadCompra;
    private double factorConversion;
    private double cantidadRecibida;

    public DetalleCompra() {
        super();
    }

    public DetalleCompra(int numeroLinea, Producto producto, double cantidad, double precioUnitario, double descuento, double importe, int id, Compra compra, UnidadMedida unidadCompra, double factorConversion, double cantidadRecibida) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.id = id;
        this.compra = compra;
        this.unidadCompra = unidadCompra;
        this.factorConversion = factorConversion;
        this.cantidadRecibida = cantidadRecibida;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
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
