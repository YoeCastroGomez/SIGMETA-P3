namespace SigmetaWeb.Components.Pages.Ventas;

// Datos de ejemplo fijos (Lab 9: sin BD ni servicios). Se reemplazan al integrar con Java.
// Los nombres de los campos siguen las clases Java: OrdenCompraCliente, Venta, DetalleVenta,
// Comprobante, Despacho y DetalleNotaCredito (paquete pe.edu.pucp.sigmeta.model.ventas / almacen).
// Todos los importes son en soles (PEN).
// Productos, clientes y cotizaciones son los mismos de MuestrasCatalogo y MuestrasClientes, para que el flujo
// cotización → orden → venta → despacho → nota de crédito muestre los mismos datos en todas las pantallas.

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

    // Arma una línea con el código, nombre, unidad de venta y precio del producto del catálogo de ejemplo.
    private static LineaFila Linea(int numeroLinea, string codigo, decimal cantidad, decimal descuento = 0m)
    {
        var p = global::SigmetaWeb.Components.Pages.Catalogo.MuestrasCatalogo.Productos.First(x => x.CodigoInterno == codigo);
        return new(numeroLinea, p.CodigoInterno, p.Nombre,
                   global::SigmetaWeb.Components.Pages.Catalogo.MuestrasCatalogo.Texto(p.UnidadVenta),
                   cantidad, p.PrecioVenta, descuento);
    }

    // COT-2026-0001 (aceptada) → OCC-2026-0001
    private static readonly LineaFila[] LineasCot0001 =
    {
        Linea(1, "INT-001", 20m), Linea(2, "INT-002", 10m), Linea(3, "INT-003", 20m),
    };

    // COT-2026-0006 (aceptada) → OCC-2026-0002 → VTA-2026-0002
    private static readonly LineaFila[] LineasCot0006 =
    {
        Linea(1, "INT-005", 30m, 25.00m), Linea(2, "INT-003", 100m),
    };

    // COT-2026-0007 (aceptada) → OCC-2026-0003 (pendiente, sin ventas)
    private static readonly LineaFila[] LineasCot0007 = { Linea(1, "INT-002", 6m) };

    // COT-2026-0008 (aceptada, todavía sin orden)
    private static readonly LineaFila[] LineasCot0008 = { Linea(1, "INT-004", 4m), Linea(2, "INT-001", 10m) };

    // VTA-2026-0001: atiende parte de OCC-2026-0001 (el cable queda pendiente)
    private static readonly LineaFila[] LineasVenta1 = { Linea(1, "INT-001", 20m), Linea(2, "INT-002", 10m) };

    // VTA-2026-0003: venta directa de mostrador
    private static readonly LineaFila[] LineasMostrador = { Linea(1, "INT-001", 1m), Linea(2, "INT-002", 1m, 3.00m) };

    // VTA-2026-0004 (anulada) y VTA-2026-0005 (registrada): ventas directas
    private static readonly LineaFila[] LineasVenta4 = { Linea(1, "INT-003", 10m) };
    private static readonly LineaFila[] LineasVenta5 = { Linea(1, "INT-003", 15m), Linea(2, "INT-004", 1m) };

    // Cotizaciones ACEPTADAS que todavía no tienen orden (las que se ofrecen en "Nueva orden")
    public static readonly CotizacionFila[] CotizacionesAceptadas =
    {
        new(8, "COT-2026-0008", "Constructora Andina S.A.C.", new DateOnly(2026, 10, 20), LineasCot0008),
    };

    // El estado de la orden avanza con lo DESPACHADO de sus ventas (DespachoBOImpl), no con la venta registrada
    public static readonly OrdenFila[] Ordenes =
    {
        new(1, "OCC-2026-0001", "OC-MCH-0087", "Metalmecánica Chosica E.I.R.L.", "COT-2026-0001",
            new DateOnly(2026, 9, 2), "ATENDIDA_PARCIAL", LineasCot0001),
        new(2, "OCC-2026-0002", "OC-ANDINA-4521", "Constructora Andina S.A.C.", "COT-2026-0006",
            new DateOnly(2026, 9, 26), "ATENDIDA", LineasCot0006),
        new(3, "OCC-2026-0003", "OC-MCH-0102", "Metalmecánica Chosica E.I.R.L.", "COT-2026-0007",
            new DateOnly(2026, 10, 6), "PENDIENTE", LineasCot0007),
    };

    public static readonly VentaFila[] Ventas =
    {
        new(1, "VTA-2026-0001", "Metalmecánica Chosica E.I.R.L.", "OCC-2026-0001", new DateOnly(2026, 9, 3),
            "CREDITO", 15, "DESPACHADA_PARCIAL", null, LineasVenta1),
        new(2, "VTA-2026-0002", "Constructora Andina S.A.C.", "OCC-2026-0002", new DateOnly(2026, 9, 26),
            "CREDITO", 30, "DESPACHADA", "F001-00000123", LineasCot0006),
        new(3, "VTA-2026-0003", "Pedro Huamaní Flores", null, new DateOnly(2026, 10, 24),
            "CONTADO", 0, "DESPACHADA", "B001-00000045", LineasMostrador),
        new(4, "VTA-2026-0004", "Andrea Morales Pérez", null, new DateOnly(2026, 10, 24),
            "CONTADO", 0, "ANULADA", null, LineasVenta4),
        new(5, "VTA-2026-0005", "Andrea Morales Pérez", null, new DateOnly(2026, 10, 26),
            "CONTADO", 0, "REGISTRADA", null, LineasVenta5),
    };

    public static readonly ComprobanteFila[] Comprobantes =
    {
        new(1, 2, "FACTURA", "F001", "00000123", new DateOnly(2026, 9, 26), "EMITIDO", "CORREO", new DateTime(2026, 9, 26, 11, 5, 0)),
        new(2, 3, "BOLETA", "B001", "00000045", new DateOnly(2026, 10, 24), "EMITIDO", null, null),
    };

    public static readonly DespachoFila[] Despachos =
    {
        new(1, 1, "T001", "00000041", new DateOnly(2026, 9, 5), "Carretera Central km 33, Chosica",
            "Transportes Rápidos del Centro S.A.C.", false),
        new(2, 2, "T001", "00000045", new DateOnly(2026, 9, 27), "Av. Nicolás Ayllón 4520, Ate",
            "Transportes Rápidos del Centro S.A.C.", false),
        new(3, 3, "T001", "00000052", new DateOnly(2026, 10, 24), null, "Recojo en tienda", false),
    };

    // DetalleVenta.cantidadDespachada por (venta, numero de linea)
    private static readonly Dictionary<(int Venta, int Linea), decimal> Despachado = new()
    {
        [(1, 1)] = 20m,
        [(2, 1)] = 30m, [(2, 2)] = 100m,
        [(3, 1)] = 1m, [(3, 2)] = 1m,
    };

    // Importe de la NC = importe de la línea (con descuento) × cantidad devuelta ÷ cantidad vendida (ComprobanteBOImpl)
    public static readonly NotaCreditoFila[] NotasCredito =
    {
        new(1, "FC01", "00000012", "F001-00000123", "Constructora Andina S.A.C.", new DateOnly(2026, 10, 4),
            "Devolución de bolsas de cemento con humedad", "aquispe", "EMITIDO",
            new DetalleNotaCreditoFila[] { new(1, 1, "Bolsa Cemento Sol", 2m, 28.50m, 1.67m, 55.33m) }),
        new(2, "BC01", "00000003", "B001-00000045", "Pedro Huamaní Flores", new DateOnly(2026, 10, 25),
            "Alicate con falla en el mecanismo", "aquispe", "ANULADO",
            new DetalleNotaCreditoFila[] { new(1, 2, "Alicate de Presión", 1m, 35.00m, 3.00m, 32.00m) }),
    };

    // Cantidad ya acreditada por (venta, numero de linea) en notas de credito vigentes
    private static readonly Dictionary<(int Venta, int Linea), decimal> Acreditado = new()
    {
        [(2, 1)] = 2m,
    };

    public static decimal CantidadAcreditada(int idVenta, int numeroLinea) =>
        Acreditado.GetValueOrDefault((idVenta, numeroLinea));

    public static decimal CantidadDespachada(int idVenta, int numeroLinea) =>
        Despachado.GetValueOrDefault((idVenta, numeroLinea));

    public static VentaFila? Venta(int id) => Ventas.FirstOrDefault(v => v.Id == id);
    public static ComprobanteFila? ComprobanteDeVenta(int idVenta) =>
        Comprobantes.FirstOrDefault(c => c.IdVenta == idVenta && c.Estado != "ANULADO");

    // OrdenCompraClienteBOImpl.anular: no se anula una orden con ventas vigentes
    public static bool OrdenTieneVentasVigentes(string numeroOrden) =>
        Ventas.Any(v => v.OrdenCompraCliente == numeroOrden && v.Estado != "ANULADA");

    // ComprobanteBOImpl.anular: no se anula un comprobante con notas de crédito activas
    public static bool TieneNotasCreditoVigentes(string serieNumero) =>
        NotasCredito.Any(nc => nc.ComprobanteOrigen == serieNumero && nc.Estado != "ANULADO");

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
