package pe.edu.pucp.model.almacen;

import pe.edu.pucp.model.compras.DetalleCompra;

/**
 * Detalle de los productos recibidos en una recepcion de compra (RF012).
 */
public class DetalleRecepcionCompra {
    private int idDetalleRecepcionCompra;
    private RecepcionCompra recepcionCompra;
    private DetalleCompra detalleCompra;
    private double cantidadRecibida;

    public DetalleRecepcionCompra() {
    }

    public DetalleRecepcionCompra(int idDetalleRecepcionCompra,
                                     RecepcionCompra recepcionCompra,
                                     DetalleCompra detalleCompra,
                                     double cantidadRecibida) {
        this.idDetalleRecepcionCompra = idDetalleRecepcionCompra;
        this.recepcionCompra = recepcionCompra;
        this.detalleCompra = detalleCompra;
        this.cantidadRecibida = cantidadRecibida;
    }

    public int getIdDetalleRecepcionCompra() {
        return idDetalleRecepcionCompra;
    }

    public void setIdDetalleRecepcionCompra(int idDetalleRecepcionCompra) {
        this.idDetalleRecepcionCompra = idDetalleRecepcionCompra;
    }

    public RecepcionCompra getRecepcionCompra() {
        return recepcionCompra;
    }

    public void setRecepcionCompra(RecepcionCompra recepcionCompra) {
        this.recepcionCompra = recepcionCompra;
    }

    public DetalleCompra getDetalleCompra() {
        return detalleCompra;
    }

    public void setDetalleCompra(DetalleCompra detalleCompra) {
        this.detalleCompra = detalleCompra;
    }

    public double getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(double cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }
}
