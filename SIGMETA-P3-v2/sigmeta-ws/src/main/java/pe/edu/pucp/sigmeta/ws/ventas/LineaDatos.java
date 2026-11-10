package pe.edu.pucp.sigmeta.ws.ventas;

// RF007 y RF008: una linea de cotizacion, de orden de compra del cliente o de venta, tal como viaja en JSON.
//
// Lo que Blazor manda al registrar o modificar:
//   {"numeroLinea": 1, "idProducto": 4, "cantidad": 2, "precioUnitario": 45.0, "descuento": 2.0}
// El importe no se manda: lo calcula el BO. El producto viaja solo con su id; el nombre y el codigo
// los agrega la ventanilla al responder (nunca se manda la clase Producto completa).
public class LineaDatos {

    public int numeroLinea;
    public int idProducto;
    public String producto;
    public String codigoProducto;
    public double cantidad;
    public double precioUnitario;
    public double descuento;
    public double importe;

    // solo en ordenes: id de la linea (la venta la usa para atenderla) y cuanto ya se vendio de ella
    public int idLinea;
    public double cantidadAtendida;

    // solo en ventas: linea de la orden del cliente que atiende (0 en una venta directa) y avance del despacho
    public int idDetalleOrden;
    public double cantidadDespachada;

    public LineaDatos() {
    }
}
