package pe.edu.pucp.model.almacen;

import pe.edu.pucp.model.producto.ProductoDto;

/**
 * Linea de un despacho con la cantidad efectivamente entregada.
 */
public class DetalleDespachoDto {

    private int idDetalleDespacho;
    private DespachoDto despacho;
    private ProductoDto producto;
    private double cantidadDespachada;

    public DetalleDespachoDto() {
    }

    public DetalleDespachoDto(int idDetalleDespacho, DespachoDto despacho, ProductoDto producto, double cantidadDespachada) {
        this.idDetalleDespacho = idDetalleDespacho;
        this.despacho = despacho;
        this.producto = producto;
        this.cantidadDespachada = cantidadDespachada;
    }

    public int getIdDetalleDespacho() {
        return idDetalleDespacho;
    }

    public void setIdDetalleDespacho(int idDetalleDespacho) {
        this.idDetalleDespacho = idDetalleDespacho;
    }

    public DespachoDto getDespacho() {
        return despacho;
    }

    public void setDespacho(DespachoDto despacho) {
        this.despacho = despacho;
    }

    public ProductoDto getProducto() {
        return producto;
    }

    public void setProducto(ProductoDto producto) {
        this.producto = producto;
    }

    public double getCantidadDespachada() {
        return cantidadDespachada;
    }

    public void setCantidadDespachada(double cantidadDespachada) {
        this.cantidadDespachada = cantidadDespachada;
    }

}
