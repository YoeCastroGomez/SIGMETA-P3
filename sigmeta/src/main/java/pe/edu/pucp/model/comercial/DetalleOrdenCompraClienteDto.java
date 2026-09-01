package pe.edu.pucp.model.comercial;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.producto.ProductoDto;

/**
 * Linea de una orden de compra de cliente, con el avance de atencion.
 */
public class DetalleOrdenCompraClienteDto extends LineaDocumento {

    private int idDetalleOrdenCompraCliente;
    private OrdenCompraClienteDto ordenCompraCliente;
    private double cantidadAtendida;

    public DetalleOrdenCompraClienteDto() {
        super();
    }

    public DetalleOrdenCompraClienteDto(int numeroLinea, ProductoDto producto, double cantidad, double precioUnitario, double descuento, double importe, int idDetalleOrdenCompraCliente, OrdenCompraClienteDto ordenCompraCliente, double cantidadAtendida) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.idDetalleOrdenCompraCliente = idDetalleOrdenCompraCliente;
        this.ordenCompraCliente = ordenCompraCliente;
        this.cantidadAtendida = cantidadAtendida;
    }

    public int getIdDetalleOrdenCompraCliente() {
        return idDetalleOrdenCompraCliente;
    }

    public void setIdDetalleOrdenCompraCliente(int idDetalleOrdenCompraCliente) {
        this.idDetalleOrdenCompraCliente = idDetalleOrdenCompraCliente;
    }

    public OrdenCompraClienteDto getOrdenCompraCliente() {
        return ordenCompraCliente;
    }

    public void setOrdenCompraCliente(OrdenCompraClienteDto ordenCompraCliente) {
        this.ordenCompraCliente = ordenCompraCliente;
    }

    public double getCantidadAtendida() {
        return cantidadAtendida;
    }

    public void setCantidadAtendida(double cantidadAtendida) {
        this.cantidadAtendida = cantidadAtendida;
    }

}
