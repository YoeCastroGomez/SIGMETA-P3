package pe.edu.pucp.model.comercial;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.producto.Producto;

/**
 * Linea de una nota de credito que detalla los productos devueltos o corregidos.
 */
public class DetalleNotaCredito extends LineaDocumento {

    private int idDetalleNotaCredito;
    private Comprobante comprobante;

    public DetalleNotaCredito() {
        super();
    }

    public DetalleNotaCredito(int numeroLinea, Producto producto, double cantidad, double precioUnitario, double descuento, double importe, int idDetalleNotaCredito, Comprobante comprobante) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.idDetalleNotaCredito = idDetalleNotaCredito;
        this.comprobante = comprobante;
    }

    public int getIdDetalleNotaCredito() {
        return idDetalleNotaCredito;
    }

    public void setIdDetalleNotaCredito(int idDetalleNotaCredito) {
        this.idDetalleNotaCredito = idDetalleNotaCredito;
    }

    public Comprobante getComprobante() {
        return comprobante;
    }

    public void setComprobante(Comprobante comprobante) {
        this.comprobante = comprobante;
    }

}
