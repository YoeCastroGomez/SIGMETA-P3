package pe.edu.pucp.sigmeta.ws.caja;

// RF010: cobro de una venta al contado. Blazor manda {"idVenta": 4, "medioPago": "EFECTIVO"}.
// El monto no se manda: el BO usa el total de la venta.
public class CobroContadoDatos {

    public int idVenta;
    public String medioPago;

    public CobroContadoDatos() {
    }
}
