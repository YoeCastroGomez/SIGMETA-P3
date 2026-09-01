package pe.edu.pucp.model.comun;

import pe.edu.pucp.model.producto.ProductoDto;

/**
 * Superclase de las lineas de detalle de los documentos comerciales.
 */
public abstract class LineaDocumento {

    private int numeroLinea;
    private ProductoDto producto;
    private double cantidad;
    private double precioUnitario;
    private double descuento;
    private double importe;

    public LineaDocumento() {
    }

    public LineaDocumento(int numeroLinea, ProductoDto producto, double cantidad, double precioUnitario, double descuento, double importe) {
        this.numeroLinea = numeroLinea;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.descuento = descuento;
        this.importe = importe;
    }

    public int getNumeroLinea() {
        return numeroLinea;
    }

    public void setNumeroLinea(int numeroLinea) {
        this.numeroLinea = numeroLinea;
    }

    public ProductoDto getProducto() {
        return producto;
    }

    public void setProducto(ProductoDto producto) {
        this.producto = producto;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getDescuento() {
        return descuento;
    }

    public void setDescuento(double descuento) {
        this.descuento = descuento;
    }

    public double getImporte() {
        return importe;
    }

    public void setImporte(double importe) {
        this.importe = importe;
    }

}
