package pe.edu.pucp.sigmeta.model.ventas;

import pe.edu.pucp.sigmeta.model.comun.LineaDocumento;
import pe.edu.pucp.sigmeta.model.producto.Producto;

/**
 * Linea de detalle de una cotizacion.
 */
public class DetalleCotizacion extends LineaDocumento {

    private int id;
    private Cotizacion cotizacion;

    public DetalleCotizacion() {
        super();
    }

    public DetalleCotizacion(int numeroLinea, Producto producto, double cantidad, double precioUnitario, double descuento, double importe, int id, Cotizacion cotizacion) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.id = id;
        this.cotizacion = cotizacion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

}
