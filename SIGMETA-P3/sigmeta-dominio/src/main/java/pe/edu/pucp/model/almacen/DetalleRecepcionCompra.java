package pe.edu.pucp.model.almacen;

import pe.edu.pucp.model.compras.DetalleCompra;

/**
 * Detalle de los productos recibidos en una recepcion de compra (RF012).
 */
public class DetalleRecepcionCompra {
    private int id;
    private RecepcionCompra recepcionCompra;
    private DetalleCompra detalleCompra;
    private double cantidadRecibida;

    public DetalleRecepcionCompra() {
    }

    public DetalleRecepcionCompra(int id,
                                     RecepcionCompra recepcionCompra,
                                     DetalleCompra detalleCompra,
                                     double cantidadRecibida) {
        this.id = id;
        this.recepcionCompra = recepcionCompra;
        this.detalleCompra = detalleCompra;
        this.cantidadRecibida = cantidadRecibida;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
