package pe.edu.pucp.model.comercial;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.producto.ProductoDto;

/**
 * Linea de una venta, con el avance de despacho.
 */
public class DetalleVentaDto extends LineaDocumento {

    private int idDetalleVenta;
    private VentaDto venta;
    private double cantidadDespachada;

    public DetalleVentaDto() {
        super();
    }

    public DetalleVentaDto(int numeroLinea, ProductoDto producto, double cantidad, double precioUnitario, double descuento, double importe, int idDetalleVenta, VentaDto venta, double cantidadDespachada) {
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

    public VentaDto getVenta() {
        return venta;
    }

    public void setVenta(VentaDto venta) {
        this.venta = venta;
    }

    public double getCantidadDespachada() {
        return cantidadDespachada;
    }

    public void setCantidadDespachada(double cantidadDespachada) {
        this.cantidadDespachada = cantidadDespachada;
    }

}
