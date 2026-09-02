package pe.edu.pucp.model.almacen;

import pe.edu.pucp.model.compras.DetalleCompraDto;

/**
 * Detalle de los productos recibidos en una recepcion de compra (RF012).
 */
public class DetalleRecepcionCompraDto {
    private int idDetalleRecepcionCompra;
    private RecepcionCompraDto recepcionCompra;
    private DetalleCompraDto detalleCompra;
    private double cantidadRecibida;

    public DetalleRecepcionCompraDto() {
    }

    public DetalleRecepcionCompraDto(int idDetalleRecepcionCompra,
                                     RecepcionCompraDto recepcionCompra,
                                     DetalleCompraDto detalleCompra,
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

    public RecepcionCompraDto getRecepcionCompra() {
        return recepcionCompra;
    }

    public void setRecepcionCompra(RecepcionCompraDto recepcionCompra) {
        this.recepcionCompra = recepcionCompra;
    }

    public DetalleCompraDto getDetalleCompra() {
        return detalleCompra;
    }

    public void setDetalleCompra(DetalleCompraDto detalleCompra) {
        this.detalleCompra = detalleCompra;
    }

    public double getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(double cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }
}
