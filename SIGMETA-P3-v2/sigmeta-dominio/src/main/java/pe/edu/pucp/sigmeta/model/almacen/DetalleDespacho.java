package pe.edu.pucp.sigmeta.model.almacen;

import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;

/**
 * Cantidad entregada en un despacho de una linea de venta concreta (RF012).
 * El producto se obtiene de la linea de venta, como en DetalleRecepcionCompra con DetalleCompra.
 */
public class DetalleDespacho {

    private int id;
    private Despacho despacho;
    private DetalleVenta detalleVenta;
    private double cantidadDespachada;

    public DetalleDespacho() {
    }

    public DetalleDespacho(int id, Despacho despacho, DetalleVenta detalleVenta, double cantidadDespachada) {
        this.id = id;
        this.despacho = despacho;
        this.detalleVenta = detalleVenta;
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

    public DetalleVenta getDetalleVenta() {
        return detalleVenta;
    }

    public void setDetalleVenta(DetalleVenta detalleVenta) {
        this.detalleVenta = detalleVenta;
    }

    public double getCantidadDespachada() {
        return cantidadDespachada;
    }

    public void setCantidadDespachada(double cantidadDespachada) {
        this.cantidadDespachada = cantidadDespachada;
    }
}
