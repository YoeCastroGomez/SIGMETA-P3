package pe.edu.pucp.sigmeta.ws.cobranza;

// RF009: un cobro sobre una cuenta por cobrar. Para registrar Blazor manda:
// {"idCuentaPorCobrar": 2, "monto": 300, "medioPago": "DEPOSITO", "fechaCobro": "2026-11-09", "referencia": "DEP-80231"}
// Para modificar solo cuentan fechaCobro y referencia.
public class CobroDatos {

    public int id;
    public int idCuentaPorCobrar;
    public String fechaCobro;
    public String medioPago;
    public double monto;
    public String referencia;
    public String usuarioRegistro;
    public String fechaRegistro;
    public boolean anulado;
    public String fechaAnulacion;

    // de la cuenta, para mostrar en la lista de cobros
    public String venta;
    public String comprobante;
    public String cliente;

    public CobroDatos() {
    }
}
