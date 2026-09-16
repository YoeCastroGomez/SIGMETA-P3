package pe.edu.pucp.model.almacen;

import pe.edu.pucp.model.producto.Producto;

/**
 * Linea de un despacho con la cantidad efectivamente entregada.
 */
public class DetalleDespacho {

    private int id;
    private Despacho despacho;
    private Producto producto;
    private double cantidadDespachada;

    public DetalleDespacho() {
    }

    public DetalleDespacho(int id, Despacho despacho, Producto producto, double cantidadDespachada) {
        this.id = id;
        this.despacho = despacho;
        this.producto = producto;
        this.cantidadDespachada = cantidadDespachada;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Despacho getDespacho() {
        return despacho;
    }

    public void setDespacho(Despacho despacho) {
        this.despacho = despacho;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public double getCantidadDespachada() {
        return cantidadDespachada;
    }

    public void setCantidadDespachada(double cantidadDespachada) {
        this.cantidadDespachada = cantidadDespachada;
    }

}
