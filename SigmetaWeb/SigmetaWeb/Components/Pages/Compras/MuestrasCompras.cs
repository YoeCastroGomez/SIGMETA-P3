using SigmetaWeb.Components.Pages.Catalogo;

namespace SigmetaWeb.Components.Pages.Compras;

// Datos de ejemplo fijos (Lab 9: sin BD ni servicios). Se reemplazan al integrar con Java.
// Compras, recepciones y Kardex comparten estos datos y usan los proveedores y productos
// (códigos INT-xxx) de MuestrasCatalogo, de modo que las cantidades y el stock coinciden en todas las pantallas.

// RF011 · Línea de compra. Recibido = suma de las recepciones registradas hasta ahora.
public record LineaCompraFila(string CodigoInterno, decimal Cantidad, decimal CostoUnitario, decimal Recibido)
{
    public decimal Importe => Cantidad * CostoUnitario;
    public decimal Pendiente => Cantidad - Recibido;
}

// RF011 · Compra a proveedor (solo en soles).
public record CompraFila(int Id, string Numero, int ProveedorId, DateOnly Fecha, DateOnly FechaEstimada,
                         string Estado, LineaCompraFila[] Lineas)
{
    public decimal SubTotal => Lineas.Sum(l => l.Importe);
    public decimal Igv => Math.Round(SubTotal * MuestrasCompras.TasaIgv, 2);
    public decimal Total => SubTotal + Igv;
    public string Proveedor => MuestrasCompras.NombreProveedor(ProveedorId);
}

// RF012 · Movimiento de inventario (una fila del Kardex).
public record MovimientoFila(int Id, DateOnly Fecha, string CodigoInterno, string Tipo, decimal Cantidad,
                             decimal StockResultante, string Referencia, string Usuario);

public static class MuestrasCompras
{
    public const decimal TasaIgv = 0.18m;

    // Compras: una sin recibir, una con recepción parcial y una recibida completa.
    public static readonly CompraFila[] Compras =
    {
        new(1, "COMP-001", 2, new DateOnly(2026, 10, 20), new DateOnly(2026, 10, 27), "REGISTRADA",
            new LineaCompraFila[]
            {
                new("INT-005", 30m, 24.00m, 0m),
            }),
        new(2, "COMP-002", 2, new DateOnly(2026, 10, 22), new DateOnly(2026, 10, 30), "RECIBIDA_PARCIAL",
            new LineaCompraFila[]
            {
                new("INT-001", 40m, 25.00m, 15m),
            }),
        new(3, "COMP-003", 1, new DateOnly(2026, 10, 15), new DateOnly(2026, 10, 18), "RECIBIDA",
            new LineaCompraFila[]
            {
                new("INT-003", 200m, 1.20m, 200m),
                new("INT-004", 10m, 50.00m, 10m),
            }),
    };

    // Movimientos en orden cronológico. El stock resultante final de cada producto es el StockActual de
    // MuestrasCatalogo; las entradas por compra son las cantidades recibidas de Compras y las salidas son
    // los despachos de MuestrasVentas (T001-...), más la devolución de la nota de crédito FC01-00000012.
    public static readonly MovimientoFila[] Movimientos =
    {
        new(1, new DateOnly(2026, 8, 1), "INT-001", "ENTRADA", 106m, 106m, "Saldo inicial", "admin"),
        new(2, new DateOnly(2026, 8, 1), "INT-002", "ENTRADA", 67m, 67m, "Saldo inicial", "admin"),
        new(3, new DateOnly(2026, 8, 1), "INT-003", "ENTRADA", 310m, 310m, "Saldo inicial", "admin"),
        new(4, new DateOnly(2026, 8, 1), "INT-004", "ENTRADA", 20m, 20m, "Saldo inicial", "admin"),
        new(5, new DateOnly(2026, 8, 1), "INT-005", "ENTRADA", 40m, 40m, "Saldo inicial", "admin"),
        new(6, new DateOnly(2026, 8, 1), "INT-006", "ENTRADA", 150m, 150m, "Saldo inicial", "admin"),
        new(7, new DateOnly(2026, 8, 21), "INT-001", "SALIDA", 10m, 96m, "T001-00000030 (VTA-2026-0005)", "lpaucar"),
        new(8, new DateOnly(2026, 8, 21), "INT-002", "SALIDA", 10m, 57m, "T001-00000030 (VTA-2026-0005)", "lpaucar"),
        new(9, new DateOnly(2026, 9, 3), "INT-001", "SALIDA", 12m, 84m, "T001-00000045 (VTA-2026-0001)", "lpaucar"),
        new(10, new DateOnly(2026, 9, 3), "INT-002", "SALIDA", 6m, 51m, "T001-00000045 (VTA-2026-0001)", "lpaucar"),
        new(11, new DateOnly(2026, 9, 3), "INT-003", "SALIDA", 10m, 300m, "T001-00000045 (VTA-2026-0001)", "lpaucar"),
        new(12, new DateOnly(2026, 9, 4), "INT-001", "ENTRADA", 2m, 86m, "FC01-00000012 (devolución)", "lpaucar"),
        new(13, new DateOnly(2026, 10, 18), "INT-003", "ENTRADA", 200m, 500m, "REC-001 (COMP-003)", "lpaucar"),
        new(14, new DateOnly(2026, 10, 18), "INT-004", "ENTRADA", 10m, 30m, "REC-001 (COMP-003)", "lpaucar"),
        new(15, new DateOnly(2026, 10, 24), "INT-001", "SALIDA", 1m, 85m, "T001-00000052 (VTA-2026-0003)", "lpaucar"),
        new(16, new DateOnly(2026, 10, 24), "INT-002", "SALIDA", 1m, 50m, "T001-00000052 (VTA-2026-0003)", "lpaucar"),
        new(17, new DateOnly(2026, 10, 26), "INT-001", "ENTRADA", 15m, 100m, "REC-002 (COMP-002)", "lpaucar"),
    };

    // Solo los proveedores activos del catálogo pueden elegirse en una compra nueva.
    public static IEnumerable<ProveedorFila> ProveedoresActivos => MuestrasCatalogo.Proveedores.Where(p => p.Activo);

    public static IEnumerable<ProductoFila> ProductosActivos => MuestrasCatalogo.Productos.Where(p => p.Activo);

    public static string NombreProveedor(int id) =>
        MuestrasCatalogo.Proveedores.FirstOrDefault(p => p.Id == id)?.RazonSocial ?? "—";

    public static ProductoFila? Producto(string codigoInterno) =>
        MuestrasCatalogo.Productos.FirstOrDefault(p => p.CodigoInterno == codigoInterno);

    public static string NombreProducto(string codigoInterno) =>
        Producto(codigoInterno)?.Nombre ?? codigoInterno;

    public static IEnumerable<MovimientoFila> MovimientosDe(string codigoInterno) =>
        Movimientos.Where(m => m.CodigoInterno == codigoInterno);
}
