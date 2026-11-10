namespace SigmetaWeb.Components.Pages.Caja;

// Datos de ejemplo fijos (Lab 9: sin BD ni servicios).
// Lab 11: las pantallas de caja y cobranza (RF010, RF009) ya usan datos reales del Java (ver DatosCaja.cs).
// Este archivo queda solo porque Reportes (RF015, pendiente) y MuestrasVentas todavía lo usan; se borra cuando se conecten.
// Los nombres de los campos siguen las clases Java Caja, MovimientoCaja, CierreCaja (model.caja)
// y CuentaPorCobrar, Cobro (model.cobranza). Todos los importes son en soles (PEN).

// Caja (RF010): fechaApertura, montoInicial, usuarioApertura, abierta
public record CajaFila(int Id, DateTime FechaApertura, decimal MontoInicial, string UsuarioApertura, bool Abierta);

// MovimientoCaja (RF010): tipo, fechaMovimiento, medioPago, monto, concepto, documentoOrigen, idDocumentoOrigen, usuarioRegistro
// documentoOrigen: VENTA (cobro al contado), COBRO (cobro de una cuenta, RF009), ANULACION_VENTA y NOTA_CREDITO
// (egresos que registra el sistema para devolver el dinero, CompensacionCaja) o null (ingreso/egreso manual).
public record MovimientoCajaFila(int Id, string Tipo, DateTime FechaMovimiento, string MedioPago, decimal Monto,
                                 string Concepto, string? DocumentoOrigen, int? IdDocumentoOrigen, string UsuarioRegistro)
{
    // Solo los ingresos y egresos que registró el Cajero a mano se eliminan desde aquí
    // (los cobros se anulan desde su origen y las devoluciones desde la venta o la nota de crédito).
    public bool EsManual => (Tipo is "INGRESO_MANUAL" or "EGRESO_MANUAL") && DocumentoOrigen is null;
    public bool EsEgreso => Tipo == "EGRESO_MANUAL";
    // Devolución de dinero por venta anulada o nota de crédito: no cuenta como cobro en el cierre
    public bool EsDevolucion => DocumentoOrigen is "ANULACION_VENTA" or "NOTA_CREDITO";
    public int? IdVenta => DocumentoOrigen == "VENTA" ? IdDocumentoOrigen : null;
}

// CierreCaja (RF010): caja, fechaCierre, montoCalculado, montoDeclarado, diferencia, usuarioCierre, estado,
// fechaReapertura, usuarioReapertura, motivoReapertura
public record CierreFila(int Id, int IdCaja, DateTime FechaCierre, decimal MontoCalculado, decimal MontoDeclarado,
                         string UsuarioCierre, string Estado,
                         DateTime? FechaReapertura, string? UsuarioReapertura, string? MotivoReapertura)
{
    public decimal Diferencia => MontoDeclarado - MontoCalculado;
}

// CuentaPorCobrar (RF009): venta, cliente, fechaEmision, fechaVencimiento, montoOriginal, montoPagado,
// saldoPendiente, estado
public record CuentaFila(int Id, string Venta, string? Comprobante, string Cliente, DateOnly FechaEmision,
                         DateOnly FechaVencimiento, decimal MontoOriginal, decimal MontoPagado, string Estado)
{
    public decimal SaldoPendiente => MontoOriginal - MontoPagado;
}

// Cobro (RF009): cuentaPorCobrar, fechaCobro, medioPago, monto, referencia, usuarioRegistro, fechaRegistro,
// anulado, fechaAnulacion (baja lógica: el cobro no se borra, se marca anulado)
public record CobroFila(int Id, int IdCuenta, DateOnly FechaCobro, string MedioPago, decimal Monto, string? Referencia,
                        string UsuarioRegistro, DateTime FechaRegistro, bool Anulado, DateTime? FechaAnulacion);

public static class MuestrasCaja
{
    public static readonly string[] MediosPago = { "EFECTIVO", "TRANSFERENCIA", "DEPOSITO", "CHEQUE", "TARJETA" };

    // Caja abierta del cajero de la demostración (mchavez).
    public static readonly CajaFila CajaActual = new(2, new DateTime(2026, 10, 24, 8, 0, 0), 500.00m, "mchavez", true);

    public static readonly MovimientoCajaFila[] Movimientos =
    {
        new(1, "COBRO_CONTADO", new DateTime(2026, 10, 24, 9, 15, 0), "EFECTIVO", 94.40m,
            "Cobro al contado venta VTA-2026-0003 (B001-00000045)", "VENTA", 3, "mchavez"),
        new(2, "COBRO_CONTADO", new DateTime(2026, 10, 24, 9, 40, 0), "EFECTIVO", 94.40m,
            "Cobro al contado venta VTA-2026-0004", "VENTA", 4, "mchavez"),
        new(3, "COBRO_CREDITO", new DateTime(2026, 10, 24, 10, 40, 0), "TRANSFERENCIA", 500.00m,
            "Cobro CxC venta VTA-2026-0001", "COBRO", 3, "mchavez"),
        new(4, "EGRESO_MANUAL", new DateTime(2026, 10, 24, 11, 5, 0), "EFECTIVO", 94.40m,
            "Devolución por anulación de la venta VTA-2026-0004", "ANULACION_VENTA", 4, "mchavez"),
        new(5, "INGRESO_MANUAL", new DateTime(2026, 10, 24, 12, 0, 0), "EFECTIVO", 50.00m,
            "Sencillo entregado por administración", null, null, "mchavez"),
        new(6, "EGRESO_MANUAL", new DateTime(2026, 10, 24, 15, 0, 0), "EFECTIVO", 35.00m,
            "Compra de útiles de oficina", null, null, "mchavez"),
        new(7, "COBRO_CREDITO", new DateTime(2026, 10, 24, 16, 20, 0), "DEPOSITO", 300.00m,
            "Cobro CxC venta VTA-2026-0002", "COBRO", 4, "mchavez"),
    };

    // Totales de la jornada para los resúmenes (solo para mostrar los ejemplos).
    public static decimal Total(string tipo) => Movimientos.Where(m => m.Tipo == tipo && !m.EsDevolucion).Sum(m => m.Monto);
    public static decimal TotalDevoluciones(string documento) =>
        Movimientos.Where(m => m.DocumentoOrigen == documento).Sum(m => m.Monto);
    public static IEnumerable<MovimientoCajaFila> Devoluciones => Movimientos.Where(m => m.EsDevolucion);

    // Mismo cálculo que CierreCajaBO.calcularMontoEsperado: inicial + ingresos − egresos.
    // Las devoluciones son egresos, así el cobro de una venta anulada o devuelta con nota de crédito no queda sumado.
    public static decimal MontoCalculado =>
        CajaActual.MontoInicial + Total("COBRO_CONTADO") + Total("COBRO_CREDITO") + Total("INGRESO_MANUAL")
        - Total("EGRESO_MANUAL") - TotalDevoluciones("ANULACION_VENTA") - TotalDevoluciones("NOTA_CREDITO");

    public static readonly CierreFila[] Cierres =
    {
        new(4, 4, new DateTime(2026, 10, 23, 20, 5, 0), 1872.30m, 1882.30m, "mchavez", "VIGENTE", null, null, null),
        new(3, 3, new DateTime(2026, 10, 23, 19, 40, 0), 2430.50m, 2430.50m, "mchavez", "VIGENTE", null, null, null),
        new(2, 3, new DateTime(2026, 10, 22, 20, 0, 0), 2380.50m, 2380.50m, "mchavez", "REVERTIDO",
            new DateTime(2026, 10, 23, 9, 10, 0), "aquispe", "Faltó registrar un cobro de crédito del día"),
        new(1, 1, new DateTime(2026, 9, 5, 20, 0, 0), 1150.00m, 1145.00m, "mchavez", "VIGENTE", null, null, null),
    };

    public static readonly CuentaFila[] Cuentas =
    {
        new(1, "VTA-2026-0001", "F001-00000123", "Constructora Andina S.A.C.",
            new DateOnly(2026, 9, 2), new DateOnly(2026, 10, 2), 1534.00m, 1300.00m, "VENCIDA"),
        new(2, "VTA-2026-0002", null, "Metalmecánica Chosica E.I.R.L.",
            new DateOnly(2026, 10, 16), new DateOnly(2026, 10, 31), 747.65m, 300.00m, "PAGADA_PARCIAL"),
        new(3, "VTA-2026-0006", "F001-00000141", "Metalmecánica Chosica E.I.R.L.",
            new DateOnly(2026, 10, 25), new DateOnly(2026, 11, 9), 413.00m, 0m, "PENDIENTE"),
        new(4, "VTA-2026-0005", "F001-00000130", "Constructora Andina S.A.C.",
            new DateOnly(2026, 8, 20), new DateOnly(2026, 9, 19), 944.00m, 944.00m, "PAGADA"),
    };

    public static readonly CobroFila[] Cobros =
    {
        new(1, 1, new DateOnly(2026, 9, 5), "TRANSFERENCIA", 800.00m, "OP-458821", "mchavez",
            new DateTime(2026, 9, 5, 9, 30, 0), false, null),
        new(2, 1, new DateOnly(2026, 10, 10), "DEPOSITO", 250.00m, "DEP-77120", "mchavez",
            new DateTime(2026, 10, 10, 11, 5, 0), true, new DateTime(2026, 10, 10, 17, 0, 0)),
        new(3, 1, new DateOnly(2026, 10, 24), "TRANSFERENCIA", 500.00m, "OP-470115", "mchavez",
            new DateTime(2026, 10, 24, 10, 40, 0), false, null),
        new(4, 2, new DateOnly(2026, 10, 24), "DEPOSITO", 300.00m, "DEP-80231", "mchavez",
            new DateTime(2026, 10, 24, 16, 20, 0), false, null),
        new(5, 4, new DateOnly(2026, 8, 30), "EFECTIVO", 944.00m, null, "mchavez",
            new DateTime(2026, 8, 30, 12, 15, 0), false, null),
    };

    public static CuentaFila Cuenta(int id) => Cuentas.FirstOrDefault(c => c.Id == id) ?? Cuentas[0];
    public static CobroFila? Cobro(int id) => Cobros.FirstOrDefault(c => c.Id == id);

    // Texto que se muestra en pantalla para cada valor de los enums Java.
    public static string Texto(string valor) => valor switch
    {
        "COBRO_CONTADO" => "Cobro al contado", "COBRO_CREDITO" => "Cobro de crédito",
        "INGRESO_MANUAL" => "Ingreso manual", "EGRESO_MANUAL" => "Egreso manual",
        "EFECTIVO" => "Efectivo", "TRANSFERENCIA" => "Transferencia", "DEPOSITO" => "Depósito",
        "CHEQUE" => "Cheque", "TARJETA" => "Tarjeta",
        "VIGENTE" => "Vigente", "REVERTIDO" => "Revertido",
        "VENTA" => "Venta al contado", "COBRO" => "Cobro de cuenta por cobrar",
        "ANULACION_VENTA" => "Anulación de venta", "NOTA_CREDITO" => "Nota de crédito",
        "PENDIENTE" => "Pendiente", "PAGADA_PARCIAL" => "Pagada parcial", "PAGADA" => "Pagada", "VENCIDA" => "Vencida",
        _ => valor
    };
}
