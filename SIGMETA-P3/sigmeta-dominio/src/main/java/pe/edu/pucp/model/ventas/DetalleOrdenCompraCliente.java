package pe.edu.pucp.model.ventas;

import pe.edu.pucp.model.comun.LineaDocumento;
import pe.edu.pucp.model.producto.Producto;

/**
 * Linea de una orden de compra de cliente, con el avance de atencion.
 */
public class DetalleOrdenCompraCliente extends LineaDocumento {

    private int id;
    private OrdenCompraCliente ordenCompraCliente;
    private double cantidadAtendida;

    public DetalleOrdenCompraCliente() {
        super();
    }

    public DetalleOrdenCompraCliente(int numeroLinea, Producto producto, double cantidad, double precioUnitario, double descuento, double importe, int id, OrdenCompraCliente ordenCompraCliente, double cantidadAtendida) {
        super(numeroLinea, producto, cantidad, precioUnitario, descuento, importe);
        this.id = id;
        this.ordenCompraCliente = ordenCompraCliente;
        this.cantidadAtendida = cantidadAtendida;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public OrdenCompraCliente getOrdenCompraCliente() {
        return ordenCompraCliente;
    }

    public void setOrdenCompraCliente(OrdenCompraCliente ordenCompraCliente) {
        this.ordenCompraCliente = ordenCompraCliente;
    }

    public double getCantidadAtendida() {
        return cantidadAtendida;
    }

    public void setCantidadAtendida(double cantidadAtendida) {
        this.cantidadAtendida = cantidadAtendida;
    }

}
