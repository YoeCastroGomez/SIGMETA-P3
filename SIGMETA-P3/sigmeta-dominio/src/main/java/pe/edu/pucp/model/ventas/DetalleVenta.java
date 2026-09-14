package pe.edu.pucp.model.ventas;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.producto.Producto;

/**
 * Linea de una venta, con el avance de despacho.
 */
public class DetalleVenta extends LineaDocumento {

    private int idDetalleVenta;
    private Venta venta;
    private double cantidadDespachada;

    public DetalleVenta() {
        super();
    }

    public DetalleVenta(int numeroLinea, Producto producto, double cantidad, double precioUnitario, double descuento, double importe, int idDetalleVenta, Venta venta, double cantidadDespachada) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.idDetalleVenta = idDetalleVenta;
        this.venta = venta;
        this.cantidadDespachada = cantidadDespachada;
    }

    public int getIdDetalleVenta() {
        return idDetalleVenta;
    }

    public void setIdDetalleVenta(int idDetalleVenta) {
        this.idDetalleVenta = idDetalleVenta;
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
