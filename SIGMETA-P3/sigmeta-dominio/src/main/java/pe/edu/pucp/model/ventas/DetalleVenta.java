package pe.edu.pucp.model.ventas;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.producto.Producto;

/**
 * Linea de una venta, con el avance de despacho.
 */
public class DetalleVenta extends LineaDocumento {

    private int id;
    private Venta venta;
    private double cantidadDespachada;

    public DetalleVenta() {
        super();
    }

    public DetalleVenta(int numeroLinea, Producto producto, double cantidad, double precioUnitario, double descuento, double importe, int id, Venta venta, double cantidadDespachada) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.id = id;
        this.venta = venta;
        this.cantidadDespachada = cantidadDespachada;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public double getCantidadDespachada() {
        return cantidadDespachada;
    }

    public void setCantidadDespachada(double cantidadDespachada) {
        this.cantidadDespachada = cantidadDespachada;
    }

}
