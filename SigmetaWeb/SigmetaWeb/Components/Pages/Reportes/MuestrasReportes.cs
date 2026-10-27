namespace SigmetaWeb.Components.Pages.Reportes;

// Datos de ejemplo fijos para los reportes (Lab 9: sin BD ni servicios). Se reemplazan al integrar con Java.
// Todos los importes son en soles (PEN). Los cierres de caja se toman de Caja/MuestrasCaja.cs.

// Reporte comercial: ventas del periodo
public record VentaReporteFila(string Numero, DateOnly FechaEmision, string Cliente, string CondicionPago, decimal Total, string Estado);

// Reporte comercial: crédito por cliente
public record CreditoClienteFila(string Cliente, decimal LimiteCredito, decimal SaldoPendiente, decimal SaldoVencido)
{
    public decimal Disponible => LimiteCredito - SaldoPendiente;
}

// Reporte de abastecimiento: compras a proveedores del periodo
public record CompraReporteFila(string Numero, DateOnly Fecha, string Proveedor, decimal Total, string Estado);

// Reporte de inventario: rotación de productos
public record RotacionFila(string Codigo, string Producto, decimal UnidadesSalida, decimal Stock, DateOnly? UltimoMovimiento);

public static class MuestrasReportes
{
    public static readonly VentaReporteFila[] Ventas =
    {
        new("VTA-2026-0001", new DateOnly(2026, 9, 2), "Constructora Andina S.A.C.", "CREDITO", 1534.00m, "Despachada parcial"),
        new("VTA-2026-0002", new DateOnly(2026, 10, 16), "Metalmecánica Chosica E.I.R.L.", "CREDITO", 747.65m, "Registrada"),
        new("VTA-2026-0003", new DateOnly(2026, 10, 24), "Pedro Huamaní Flores", "CONTADO", 94.40m, "Despachada"),
        new("VTA-2026-0004", new DateOnly(2026, 10, 24), "Andrea Morales Pérez", "CONTADO", 94.40m, "Anulada"),
        new("VTA-2026-0006", new DateOnly(2026, 10, 25), "Metalmecánica Chosica E.I.R.L.", "CREDITO", 412.90m, "Registrada"),
    };

    // Las ventas anuladas no suman en el reporte.
    public static decimal TotalVentas(string condicion) =>
        Ventas.Where(v => v.CondicionPago == condicion && v.Estado != "Anulada").Sum(v => v.Total);

    public static readonly CreditoClienteFila[] Creditos =
    {
        new("Constructora Andina S.A.C.", 25000.00m, 234.00m, 234.00m),
        new("Metalmecánica Chosica E.I.R.L.", 8000.00m, 860.55m, 0m),
    };

    public static readonly CompraReporteFila[] Compras =
    {
        new("COM-2026-0001", new DateOnly(2026, 10, 1), "Distribuidora Ferretera Lima S.A.C.", 3540.00m, "Recibida"),
        new("COM-2026-0002", new DateOnly(2026, 10, 12), "Aceros del Pacífico S.A.", 5782.00m, "Recibida parcial"),
        new("COM-2026-0003", new DateOnly(2026, 10, 20), "Importaciones Industriales Norte E.I.R.L.", 1298.00m, "Registrada"),
    };

    public static readonly RotacionFila[] MayorRotacion =
    {
        new("INT-0003", "Perno hexagonal 3/8\" x 2\"", 40m, 260m, new DateOnly(2026, 10, 24)),
        new("INT-0005", "Cinta aislante 3M negra", 30m, 85m, new DateOnly(2026, 10, 16)),
        new("INT-0004", "Guante anticorte nivel 5", 24m, 36m, new DateOnly(2026, 10, 16)),
        new("INT-0001", "Martillo de acero 16 oz", 22m, 18m, new DateOnly(2026, 10, 24)),
    };

    public static readonly RotacionFila[] SinMovimiento =
    {
        new("INT-0110", "Llave de impacto neumática 1/2\"", 0m, 4m, new DateOnly(2026, 6, 12)),
        new("INT-0087", "Escalera telescópica 3.8 m", 0m, 2m, new DateOnly(2026, 5, 3)),
        new("INT-0142", "Juego de brocas cobalto 19 piezas", 0m, 6m, null),
    };

    public static string Texto(string valor) => valor switch
    {
        "CONTADO" => "Contado", "CREDITO" => "Crédito",
        _ => valor
    };
}
