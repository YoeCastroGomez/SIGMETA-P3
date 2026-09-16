package pe.edu.pucp.model.ventas;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.producto.Producto;

/**
 * Linea de una nota de credito que detalla los productos devueltos o corregidos.
 */
public class DetalleNotaCredito extends LineaDocumento {

    private int id;
    private Comprobante comprobante;

    public DetalleNotaCredito() {
        super();
    }

    public DetalleNotaCredito(int numeroLinea, Producto producto, double cantidad, double precioUnitario, double descuento, double importe, int id, Comprobante comprobante) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.id = id;
        this.comprobante = comprobante;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Comprobante getComprobante() {
        return comprobante;
    }

    public void setComprobante(Comprobante comprobante) {
        this.comprobante = comprobante;
    }

}
