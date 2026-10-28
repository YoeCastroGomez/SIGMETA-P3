using SigmetaWeb.Components.Pages.Catalogo;
using SigmetaWeb.Components.Pages.Compras;

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
        new("VTA-2026-0005", new DateOnly(2026, 8, 20), "Constructora Andina S.A.C.", "CREDITO", 944.00m, "Despachada"),
        new("VTA-2026-0006", new DateOnly(2026, 10, 25), "Metalmecánica Chosica E.I.R.L.", "CREDITO", 413.00m, "Registrada"),
    };

    // Las ventas anuladas no suman en el reporte.
    public static decimal TotalVentas(string condicion) =>
        Ventas.Where(v => v.CondicionPago == condicion && v.Estado != "Anulada").Sum(v => v.Total);

    public static readonly CreditoClienteFila[] Creditos =
    {
        new("Constructora Andina S.A.C.", 25000.00m, 234.00m, 234.00m),
        new("Metalmecánica Chosica E.I.R.L.", 8000.00m, 860.65m, 0m),
    };

    // Mismas compras de la pantalla Compras (MuestrasCompras).
    public static readonly CompraReporteFila[] Compras = MuestrasCompras.Compras
        .Select(c => new CompraReporteFila(c.Numero, c.Fecha, c.Proveedor, c.Total, c.Estado))
        .ToArray();

    // Rotación calculada con los movimientos del Kardex y las existencias del Catálogo.
    private static RotacionFila Rotacion(ProductoFila p)
    {
        var movs = MuestrasCompras.MovimientosDe(p.CodigoInterno).ToList();
        var salidas = movs.Where(m => m.Tipo == "SALIDA").Sum(m => m.Cantidad);
        return new RotacionFila(p.CodigoInterno, p.Nombre, salidas, p.StockActual, movs.Max(m => m.Fecha));
    }

    public static readonly RotacionFila[] MayorRotacion = MuestrasCatalogo.Productos
        .Select(Rotacion).Where(r => r.UnidadesSalida > 0).OrderByDescending(r => r.UnidadesSalida).ToArray();

    public static readonly RotacionFila[] SinMovimiento = MuestrasCatalogo.Productos
        .Select(Rotacion).Where(r => r.UnidadesSalida == 0).OrderBy(r => r.UltimoMovimiento).ToArray();

    public static string Texto(string valor) => valor switch
    {
        "CONTADO" => "Contado", "CREDITO" => "Crédito",
        _ => valor
    };
}
