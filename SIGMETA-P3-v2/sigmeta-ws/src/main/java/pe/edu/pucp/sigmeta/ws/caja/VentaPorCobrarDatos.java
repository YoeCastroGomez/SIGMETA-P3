package pe.edu.pucp.sigmeta.ws.caja;

// RF010: venta al contado vigente que todavia no tiene su cobro en caja
public class VentaPorCobrarDatos {

    public int id;
    public String numero;
    public String fechaEmision;
    public String cliente;
    public String documentoCliente;
    public String comprobante;
    public double total;

    public VentaPorCobrarDatos() {
    }
}
