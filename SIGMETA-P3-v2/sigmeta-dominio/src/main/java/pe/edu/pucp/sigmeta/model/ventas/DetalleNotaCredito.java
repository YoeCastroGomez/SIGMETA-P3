package pe.edu.pucp.sigmeta.model.ventas;

/**
 * Linea de una nota de credito. Apunta a la linea de venta concreta que se devuelve (RF013):
 * el producto, el precio y el descuento efectivamente cobrados se toman de ella.
 */
public class DetalleNotaCredito {

    private int id;
    private Comprobante comprobante;
    private int numeroLinea;
    private DetalleVenta detalleVenta;
    private double cantidad;
    private double precioUnitario;
    private double descuento;
    private double importe;

    public DetalleNotaCredito() {
    }

    public DetalleNotaCredito(int id, Comprobante comprobante, int numeroLinea, DetalleVenta detalleVenta,
                              double cantidad, double precioUnitario, double descuento, double importe) {
        this.id = id;
        this.comprobante = comprobante;
        this.numeroLinea = numeroLinea;
        this.detalleVenta = detalleVenta;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.descuento = descuento;
        this.importe = importe;
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

    public int getNumeroLinea() {
        return numeroLinea;
    }

    public void setNumeroLinea(int numeroLinea) {
        this.numeroLinea = numeroLinea;
    }

    public DetalleVenta getDetalleVenta() {
        return detalleVenta;
    }

    public void setDetalleVenta(DetalleVenta detalleVenta) {
        this.detalleVenta = detalleVenta;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public double getDescuento() {
        return descuento;
    }

    public void setDescuento(double descuento) {
        this.descuento = descuento;
    }

    public double getImporte() {
        return importe;
    }

    public void setImporte(double importe) {
        this.importe = importe;
    }
}
