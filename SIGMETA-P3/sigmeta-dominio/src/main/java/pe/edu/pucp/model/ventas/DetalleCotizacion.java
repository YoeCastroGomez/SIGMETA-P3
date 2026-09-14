package pe.edu.pucp.model.ventas;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.producto.Producto;

/**
 * Linea de detalle de una cotizacion.
 */
public class DetalleCotizacion extends LineaDocumento {

    private int idDetalleCotizacion;
    private Cotizacion cotizacion;

    public DetalleCotizacion() {
        super();
    }

    public DetalleCotizacion(int numeroLinea, Producto producto, double cantidad, double precioUnitario, double descuento, double importe, int idDetalleCotizacion, Cotizacion cotizacion) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.idDetalleCotizacion = idDetalleCotizacion;
        this.cotizacion = cotizacion;
    }

    public int getIdDetalleCotizacion() {
        return idDetalleCotizacion;
    }

    public void setIdDetalleCotizacion(int idDetalleCotizacion) {
        this.idDetalleCotizacion = idDetalleCotizacion;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

}
