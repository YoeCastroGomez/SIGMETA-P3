package pe.edu.pucp.sigmeta.ws.caja;

// RF010: un movimiento de caja. Para registrar o modificar un ingreso o egreso manual Blazor manda:
// {"tipo": "EGRESO_MANUAL", "medioPago": "EFECTIVO", "monto": 35.00, "concepto": "Compra de utiles"}
public class MovimientoDatos {

    public int id;
    public int idCaja;
    public String tipo;
    public String fechaMovimiento;
    public String medioPago;
    public double monto;
    public String concepto;
    // VENTA, COBRO, ANULACION_VENTA, NOTA_CREDITO, ANULACION_NOTA_CREDITO, ANULACION_COBRO o null (manual)
    public String documentoOrigen;
    public int idDocumentoOrigen;
    public String usuarioRegistro;
    // true = ingreso o egreso que registro el cajero: es el unico que se modifica o elimina
    public boolean manual;

    public MovimientoDatos() {
    }
}
