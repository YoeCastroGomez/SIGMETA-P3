package pe.edu.pucp.sigmeta.ws.caja;

import java.util.ArrayList;
import java.util.List;

// RF010: lo que responde /api/cajas. Con la caja va el resumen de la jornada:
// monto inicial + ingresos - egresos, por tipo y por medio de pago.
// (Tambien es lo que Blazor manda al abrir: {"montoInicial": 500})
public class CajaDatos {

    public int id;
    public String fechaApertura;
    public double montoInicial;
    public String usuarioApertura;
    public boolean abierta;

    // resumen (solo en GET /api/cajas/abierta y GET /api/cajas/{id})
    public double totalCobrosContado;
    public double totalCobrosCredito;
    public double totalIngresosManuales;
    public double totalEgresosManuales;
    // egresos que registra el sistema: venta anulada, nota de credito o cobro anulado (no cuentan como cobro)
    public double totalDevoluciones;
    // lo calcula el BO (sp_cierre_caja_calcular_monto_esperado)
    public double montoEsperado;
    public List<TotalMedioPago> porMedioPago = new ArrayList<>();

    public CajaDatos() {
    }

    public static class TotalMedioPago {
        public String medioPago;
        public double ingresos;
        public double egresos;
        public double neto;

        public TotalMedioPago() {
        }
    }
}
