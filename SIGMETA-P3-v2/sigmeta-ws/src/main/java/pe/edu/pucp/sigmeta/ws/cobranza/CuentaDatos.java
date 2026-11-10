package pe.edu.pucp.sigmeta.ws.cobranza;

import java.util.List;

// RF009: lo que responde /api/cuentas-por-cobrar. Las cuentas no se registran a mano:
// las crea el BO al registrar una venta al credito.
public class CuentaDatos {

    public int id;
    public int idVenta;
    public String venta;
    public String comprobante;
    public int idCliente;
    public String cliente;
    public String documentoCliente;
    public String fechaEmision;
    public String fechaVencimiento;
    public double montoOriginal;
    public double montoPagado;
    public double saldoPendiente;
    // PENDIENTE, PAGADA_PARCIAL, PAGADA o VENCIDA
    public String estado;
    public boolean vencida;
    // solo en GET /api/cuentas-por-cobrar/{id}
    public List<CobroDatos> cobros;

    public CuentaDatos() {
    }
}
