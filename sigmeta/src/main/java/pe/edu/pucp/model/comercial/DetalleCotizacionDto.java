package pe.edu.pucp.model.comercial;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.producto.ProductoDto;

/**
 * Linea de detalle de una cotizacion.
 */
public class DetalleCotizacionDto extends LineaDocumento {

    private int idDetalleCotizacion;
    private CotizacionDto cotizacion;

    public DetalleCotizacionDto() {
        super();
    }

    public DetalleCotizacionDto(int numeroLinea, ProductoDto producto, double cantidad, double precioUnitario, double descuento, double importe, int idDetalleCotizacion, CotizacionDto cotizacion) {
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

    public CotizacionDto getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(CotizacionDto cotizacion) {
        this.cotizacion = cotizacion;
    }

}
