namespace SigmetaWeb.Components.Pages.Caja;

// RF010 y RF009: mismo formato que el JSON de /api/cajas, /api/cierres, /api/cuentas-por-cobrar y /api/cobros
// (clases CajaDatos, MovimientoDatos, CierreDatos, CuentaDatos y CobroDatos del Java, paquete ws.caja y ws.cobranza).
// Los enums viajan como texto ("EFECTIVO", "INGRESO_MANUAL", "VIGENTE"...) y las fechas como AAAA-MM-DD.
// Las reglas del negocio las valida el BO en el Java; aquí solo se muestran sus mensajes.

// RF010: caja con el resumen de la jornada
public class CajaDatos
{
    public int Id { get; set; }
    public DateTime? FechaApertura { get; set; }
    public decimal MontoInicial { get; set; }
    public string? UsuarioApertura { get; set; }
    public bool Abierta { get; set; }

    public decimal TotalCobrosContado { get; set; }
    public decimal TotalCobrosCredito { get; set; }
    public decimal TotalIngresosManuales { get; set; }
    public decimal TotalEgresosManuales { get; set; }
    public decimal TotalDevoluciones { get; set; }
    public decimal MontoEsperado { get; set; }
    public List<TotalMedioPago> PorMedioPago { get; set; } = new();
}

public class TotalMedioPago
{
    public string MedioPago { get; set; } = "";
    public decimal Ingresos { get; set; }
    public decimal Egresos { get; set; }
    public decimal Neto { get; set; }
}

// RF010: movimiento de caja. Para registrar o modificar un ingreso o egreso manual se mandan tipo, medioPago, monto y concepto.
public class MovimientoDatos
{
    public int Id { get; set; }
    public int IdCaja { get; set; }
    public string Tipo { get; set; } = "INGRESO_MANUAL";
    public DateTime? FechaMovimiento { get; set; }
    public string MedioPago { get; set; } = "EFECTIVO";
    public decimal Monto { get; set; }
    public string? Concepto { get; set; }
    public string? DocumentoOrigen { get; set; }
    public int IdDocumentoOrigen { get; set; }
    public string? UsuarioRegistro { get; set; }
    // true = ingreso o egreso que registró el cajero; es el único que se modifica o elimina
    public bool Manual { get; set; }

    public bool EsEgreso => Tipo == "EGRESO_MANUAL";
    // egreso que registró el sistema (venta anulada, nota de crédito o cobro anulado): no cuenta como cobro
    public bool EsDevolucion => EsEgreso && DocumentoOrigen is not null;
}

// RF010: venta al contado que todavía no se cobra
public class VentaPorCobrar
{
    public int Id { get; set; }
    public string? Numero { get; set; }
    public DateOnly? FechaEmision { get; set; }
    public string? Cliente { get; set; }
    public string? DocumentoCliente { get; set; }
    public string? Comprobante { get; set; }
    public decimal Total { get; set; }
}

// RF010: lo que se manda a POST /api/cajas/{id}/cobros-contado (el monto lo pone el BO con el total de la venta)
public class CobroContado
{
    public int IdVenta { get; set; }
    public string MedioPago { get; set; } = "EFECTIVO";
}

// RF010: cierre de caja. Para cerrar o corregir se manda montoDeclarado; para reabrir, motivo.
public class CierreDatos
{
    public int Id { get; set; }
    public int IdCaja { get; set; }
    public DateTime? FechaCierre { get; set; }
    public decimal MontoCalculado { get; set; }
    public decimal MontoDeclarado { get; set; }
    public decimal Diferencia { get; set; }
    public string? UsuarioCierre { get; set; }
    public string? Estado { get; set; }
    public DateTime? FechaReapertura { get; set; }
    public string? UsuarioReapertura { get; set; }
    public string? MotivoReapertura { get; set; }
    public string? Motivo { get; set; }
}

// RF009: cuenta por cobrar (la crea el BO al registrar una venta al crédito)
public class CuentaDatos
{
    public int Id { get; set; }
    public int IdVenta { get; set; }
    public string? Venta { get; set; }
    public string? Comprobante { get; set; }
    public int IdCliente { get; set; }
    public string? Cliente { get; set; }
    public string? DocumentoCliente { get; set; }
    public DateOnly? FechaEmision { get; set; }
    public DateOnly? FechaVencimiento { get; set; }
    public decimal MontoOriginal { get; set; }
    public decimal MontoPagado { get; set; }
    public decimal SaldoPendiente { get; set; }
    public string? Estado { get; set; }
    public bool Vencida { get; set; }
    public List<CobroDatos>? Cobros { get; set; }
}

// RF009: total de saldos pendientes y vencidos (solo el Administrador)
public class ResumenCuentas
{
    public decimal TotalSaldoPendiente { get; set; }
    public decimal TotalSaldoVencido { get; set; }
    public int CuentasConSaldo { get; set; }
    public int CuentasVencidas { get; set; }
}

// RF009: cobro de una cuenta. Para registrar se mandan idCuentaPorCobrar, monto, medioPago, fechaCobro y referencia;
// para modificar solo cuentan fechaCobro y referencia.
public class CobroDatos
{
    public int Id { get; set; }
    public int IdCuentaPorCobrar { get; set; }
    public DateOnly? FechaCobro { get; set; }
    public string MedioPago { get; set; } = "EFECTIVO";
    public decimal Monto { get; set; }
    public string? Referencia { get; set; }
    public string? UsuarioRegistro { get; set; }
    public DateTime? FechaRegistro { get; set; }
    public bool Anulado { get; set; }
    public DateTime? FechaAnulacion { get; set; }
    public string? Venta { get; set; }
    public string? Comprobante { get; set; }
    public string? Cliente { get; set; }
}

public static class TextosCaja
{
    public static readonly string[] MediosPago = { "EFECTIVO", "TRANSFERENCIA", "DEPOSITO", "CHEQUE", "TARJETA" };

    // Texto en español para los valores que vienen del Java
    public static string Texto(string? valor) => valor switch
    {
        "COBRO_CONTADO" => "Cobro al contado", "COBRO_CREDITO" => "Cobro de crédito",
        "INGRESO_MANUAL" => "Ingreso manual", "EGRESO_MANUAL" => "Egreso manual",
        "EFECTIVO" => "Efectivo", "TRANSFERENCIA" => "Transferencia", "DEPOSITO" => "Depósito",
        "CHEQUE" => "Cheque", "TARJETA" => "Tarjeta",
        "VIGENTE" => "Vigente", "REVERTIDO" => "Revertido",
        "VENTA" => "Venta al contado", "COBRO" => "Cobro de cuenta por cobrar",
        "ANULACION_VENTA" => "Anulación de venta", "NOTA_CREDITO" => "Nota de crédito",
        "ANULACION_NOTA_CREDITO" => "Anulación de nota de crédito", "ANULACION_COBRO" => "Anulación de cobro",
        "PENDIENTE" => "Pendiente", "PAGADA_PARCIAL" => "Pagada parcial", "PAGADA" => "Pagada", "VENCIDA" => "Vencida",
        _ => valor ?? ""
    };

    public static string Fecha(DateOnly? f) => f?.ToString("dd/MM/yyyy") ?? "—";
    public static string FechaHora(DateTime? f) => f?.ToString("dd/MM/yyyy HH:mm") ?? "—";
}
