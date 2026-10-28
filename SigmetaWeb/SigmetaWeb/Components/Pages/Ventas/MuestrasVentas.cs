namespace SigmetaWeb.Components.Pages.Ventas;

// Datos de ejemplo fijos (Lab 9: sin BD ni servicios). Se reemplazan al integrar con Java.
// Los nombres de los campos siguen las clases Java: OrdenCompraCliente, Venta, DetalleVenta,
// Comprobante, Despacho y DetalleNotaCredito (paquete pe.edu.pucp.sigmeta.model.ventas / almacen).
// Todos los importes son en soles (PEN).

// LineaDocumento: numeroLinea, producto, cantidad, precioUnitario, descuento, importe
public record LineaFila(int NumeroLinea, string CodigoProducto, string Producto, string Unidad,
                        decimal Cantidad, decimal PrecioUnitario, decimal Descuento)
{
    public decimal Importe => Math.Round(Cantidad * PrecioUnitario - Descuento, 2);
}

// Cotizacion en estado ACEPTADA (origen de una orden de compra de cliente, RF007)
public record CotizacionFila(int Id, string Numero, string Cliente, DateOnly FechaEmision, LineaFila[] Detalles);

// OrdenCompraCliente (RF007): numero, numeroOrdenCliente, cliente, cotizacion, fechaEmision, estado, detalles
public record OrdenFila(int Id, string Numero, string NumeroOrdenCliente, string Cliente, string Cotizacion,
                        DateOnly FechaEmision, string Estado, LineaFila[] Detalles);

// Venta (RF008): numero, cliente, ordenCompraCliente, fechaEmision, condicionPago, plazoCreditoDias, estado, detalles
public record VentaFila(int Id, string Numero, string Cliente, string? OrdenCompraCliente, DateOnly FechaEmision,
                        string CondicionPago, int PlazoCreditoDias, string Estado, string? Comprobante, LineaFila[] Detalles);

// Comprobante (RF008): tipo, serie, numero, fechaEmision, estado, medioEnvio, fechaEnvio
public record ComprobanteFila(int Id, int IdVenta, string Tipo, string Serie, string Numero, DateOnly FechaEmision,
                              string Estado, string? MedioEnvio, DateTime? FechaEnvio)
{
    public string SerieNumero => $"{Serie}-{Numero}";
}

// Despacho (RF012): venta, serieGuia, numeroGuia, fechaDespacho, direccionEntrega, transportista, anulado
public record DespachoFila(int Id, int IdVenta, string SerieGuia, string NumeroGuia, DateOnly FechaDespacho,
                           string? DireccionEntrega, string? Transportista, bool Anulado);

// DetalleNotaCredito (RF013): numeroLinea, detalleVenta (linea de venta devuelta), cantidad, precioUnitario, descuento, importe
public record DetalleNotaCreditoFila(int NumeroLinea, int LineaVenta, string Producto, decimal Cantidad,
                                     decimal PrecioUnitario, decimal Descuento, decimal Importe);

// Comprobante de tipo NOTA_CREDITO (RF013): serie, numero, comprobanteRelacionado, motivo, administradorAutoriza, estado
public record NotaCreditoFila(int Id, string Serie, string Numero, string ComprobanteOrigen, string Cliente,
                              DateOnly FechaEmision, string Motivo, string? AdministradorAutoriza, string Estado,
                              DetalleNotaCreditoFila[] Detalles)
{
    public string SerieNumero => $"{Serie}-{Numero}";
    public decimal SubTotal => Detalles.Sum(d => d.Importe);
    public decimal Igv => Math.Round(SubTotal * MuestrasVentas.TasaIgv, 2);
    public decimal Total => SubTotal + Igv;
}

public static class MuestrasVentas
{
    public const decimal TasaIgv = 0.18m;

    public static decimal SubTotal(IEnumerable<LineaFila> lineas) => lineas.Sum(l => l.Importe);
    public static decimal Igv(IEnumerable<LineaFila> lineas) => Math.Round(SubTotal(lineas) * TasaIgv, 2);
    public static decimal Total(IEnumerable<LineaFila> lineas) => SubTotal(lineas) + Igv(lineas);

    // Productos, nombres, unidades y precios del Catálogo (MuestrasCatalogo).
    private static readonly LineaFila[] LineasAndina =
    {
        new(1, "INT-001", "Martillo de Acero", "Unidad", 20m, 45.00m, 0m),
        new(2, "INT-002", "Alicate de Presión", "Unidad", 10m, 35.00m, 0m),
        new(3, "INT-003", "Cable Mellizo 2x14", "Metro", 20m, 2.50m, 0m),
    };

    private static readonly LineaFila[] LineasChosica =
    {
        new(1, "INT-002", "Alicate de Presión", "Unidad", 2m, 35.00m, 0m),
        new(2, "INT-005", "Bolsa Cemento Sol", "Unidad", 20m, 28.50m, 6.40m),
    };

    private static readonly LineaFila[] LineasMostrador =
    {
        new(1, "INT-001", "Martillo de Acero", "Unidad", 1m, 45.00m, 0m),
        new(2, "INT-002", "Alicate de Presión", "Unidad", 1m, 35.00m, 0m),
    };

    private static readonly LineaFila[] LineasAndina2 =
    {
        new(1, "INT-001", "Martillo de Acero", "Unidad", 10m, 45.00m, 0m),
        new(2, "INT-002", "Alicate de Presión", "Unidad", 10m, 35.00m, 0m),
    };

    private static readonly LineaFila[] LineasChosica2 =
    {
        new(1, "INT-004", "Enchufe Mennekes", "Unidad", 2m, 85.00m, 0m),
        new(2, "INT-001", "Martillo de Acero", "Unidad", 4m, 45.00m, 0m),
    };

    // Cotizaciones ACEPTADA del Catálogo que aún no tienen orden de compra del cliente.
    public static readonly CotizacionFila[] CotizacionesAceptadas =
    {
        new(7, "COT-2026-0007", "Metalmecánica Chosica E.I.R.L.", new DateOnly(2026, 10, 24),
            new LineaFila[]
            {
                new(1, "INT-004", "Enchufe Mennekes", "Unidad", 10m, 85.00m, 0m),
            }),
    };

    public static readonly OrdenFila[] Ordenes =
    {
        new(1, "OCC-2026-0001", "OC-ANDINA-4521", "Constructora Andina S.A.C.", "COT-2026-0001",
            new DateOnly(2026, 9, 2), "ATENDIDA", LineasAndina),
        new(2, "OCC-2026-0002", "OC-MCH-0087", "Metalmecánica Chosica E.I.R.L.", "COT-2026-0002",
            new DateOnly(2026, 10, 15), "ATENDIDA_PARCIAL", LineasChosica),
        new(3, "OCC-2026-0003", "OC-ANDINA-4630", "Constructora Andina S.A.C.", "COT-2026-0006",
            new DateOnly(2026, 10, 23), "PENDIENTE",
            new LineaFila[]
            {
                new(1, "INT-003", "Cable Mellizo 2x14", "Metro", 200m, 2.50m, 20.00m),
                new(2, "INT-002", "Alicate de Presión", "Unidad", 15m, 35.00m, 0m),
            }),
    };

    public static readonly VentaFila[] Ventas =
    {
        new(1, "VTA-2026-0001", "Constructora Andina S.A.C.", "OCC-2026-0001", new DateOnly(2026, 9, 2),
            "CREDITO", 30, "DESPACHADA_PARCIAL", "F001-00000123", LineasAndina),
        new(2, "VTA-2026-0002", "Metalmecánica Chosica E.I.R.L.", "OCC-2026-0002", new DateOnly(2026, 10, 16),
            "CREDITO", 15, "REGISTRADA", null, LineasChosica),
        new(3, "VTA-2026-0003", "Pedro Huamaní Flores", null, new DateOnly(2026, 10, 24),
            "CONTADO", 0, "DESPACHADA", "B001-00000045", LineasMostrador),
        new(4, "VTA-2026-0004", "Andrea Morales Pérez", null, new DateOnly(2026, 10, 24),
            "CONTADO", 0, "ANULADA", null, LineasMostrador),
        new(5, "VTA-2026-0005", "Constructora Andina S.A.C.", null, new DateOnly(2026, 8, 20),
            "CREDITO", 30, "DESPACHADA", "F001-00000130", LineasAndina2),
        new(6, "VTA-2026-0006", "Metalmecánica Chosica E.I.R.L.", null, new DateOnly(2026, 10, 25),
            "CREDITO", 15, "REGISTRADA", "F001-00000141", LineasChosica2),
    };

    public static readonly ComprobanteFila[] Comprobantes =
    {
        new(1, 1, "FACTURA", "F001", "00000123", new DateOnly(2026, 9, 2), "EMITIDO", "CORREO", new DateTime(2026, 9, 2, 10, 20, 0)),
        new(3, 3, "BOLETA", "B001", "00000045", new DateOnly(2026, 10, 24), "EMITIDO", null, null),
        new(5, 5, "FACTURA", "F001", "00000130", new DateOnly(2026, 8, 20), "EMITIDO", "CORREO", new DateTime(2026, 8, 20, 11, 0, 0)),
        new(6, 6, "FACTURA", "F001", "00000141", new DateOnly(2026, 10, 25), "EMITIDO", null, null),
    };

    public static readonly DespachoFila[] Despachos =
    {
        new(1, 1, "T001", "00000045", new DateOnly(2026, 9, 3), "Jr. Huaraz 1180, Cercado de Lima",
            "Transportes Rápidos del Centro S.A.C.", false),
        new(2, 3, "T001", "00000052", new DateOnly(2026, 10, 24), null, "Recojo en tienda", false),
        new(3, 5, "T001", "00000030", new DateOnly(2026, 8, 21), "Av. Nicolás Ayllón 4520, Ate",
            "Transportes Rápidos del Centro S.A.C.", false),
    };

    // DetalleVenta.cantidadDespachada por (venta, numero de linea)
    private static readonly Dictionary<(int Venta, int Linea), decimal> Despachado = new()
    {
        [(1, 1)] = 12m, [(1, 2)] = 6m, [(1, 3)] = 10m,
        [(3, 1)] = 1m, [(3, 2)] = 1m,
        [(5, 1)] = 10m, [(5, 2)] = 10m,
    };

    public static readonly NotaCreditoFila[] NotasCredito =
    {
        new(2, "FC01", "00000012", "F001-00000123", "Constructora Andina S.A.C.", new DateOnly(2026, 9, 4),
            "Devolución de producto por defecto de fábrica", "aquispe", "EMITIDO",
            new DetalleNotaCreditoFila[] { new(1, 1, "Martillo de Acero", 2m, 45.00m, 0m, 90.00m) }),
        new(5, "BC01", "00000003", "B001-00000045", "Pedro Huamaní Flores", new DateOnly(2026, 10, 25),
            "Alicate con falla en el mecanismo", "aquispe", "ANULADO",
            new DetalleNotaCreditoFila[] { new(1, 2, "Alicate de Presión", 1m, 35.00m, 0m, 35.00m) }),
    };

    // Cantidad ya acreditada por (venta, numero de linea) en notas de credito vigentes
    private static readonly Dictionary<(int Venta, int Linea), decimal> Acreditado = new()
    {
        [(1, 1)] = 2m,
    };

    public static decimal CantidadAcreditada(int idVenta, int numeroLinea) =>
        Acreditado.GetValueOrDefault((idVenta, numeroLinea));

    public static decimal CantidadDespachada(int idVenta, int numeroLinea) =>
        Despachado.GetValueOrDefault((idVenta, numeroLinea));

    public static VentaFila? Venta(int id) => Ventas.FirstOrDefault(v => v.Id == id);
    public static ComprobanteFila? ComprobanteDeVenta(int idVenta) =>
        Comprobantes.FirstOrDefault(c => c.IdVenta == idVenta && c.Estado != "ANULADO");

    // Texto que se muestra para los valores de los enums de Java
    public static string Texto(string valor) => valor switch
    {
        "PENDIENTE" => "Pendiente",
        "ATENDIDA_PARCIAL" => "Atendida parcial",
        "ATENDIDA" => "Atendida",
        "ANULADA" => "Anulada",
        "REGISTRADA" => "Registrada",
        "DESPACHADA_PARCIAL" => "Despachada parcial",
        "DESPACHADA" => "Despachada",
        "CONTADO" => "Contado",
        "CREDITO" => "Crédito",
        "FACTURA" => "Factura",
        "BOLETA" => "Boleta",
        "NOTA_CREDITO" => "Nota de crédito",
        "EMITIDO" => "Emitido",
        "ACEPTADO" => "Aceptado",
        "RECHAZADO" => "Rechazado",
        "ANULADO" => "Anulado",
        "CORREO" => "Correo electrónico",
        "MENSAJERIA" => "Mensajería al contacto",
        "IMPRESO" => "Impreso",
        _ => valor
    };
}
