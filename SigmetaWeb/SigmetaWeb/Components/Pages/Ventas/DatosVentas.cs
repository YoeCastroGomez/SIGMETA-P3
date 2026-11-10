using System.Text.Json.Serialization;

namespace SigmetaWeb.Components.Pages.Ventas;

// RF007 y RF008: mismo formato que el JSON de /api/cotizaciones, /api/ordenes, /api/ventas y /api/comprobantes
// (clases CotizacionDatos, OrdenDatos, VentaDatos, ComprobanteDatos y LineaDatos del Java, paquete ws.ventas).
// Los enums viajan como texto ("REGISTRADA", "CONTADO", "BOLETA"...) y las fechas como AAAA-MM-DD.
// Los totales, el estado y los numeros correlativos los calcula el BO en el Java; aqui solo se muestran.
// Los nombres no chocan con los de MuestrasVentas (datos de ejemplo que aun usan Despachos y Notas de credito).

// Una linea de cotizacion, de orden o de venta
public class LineaDatos
{
    public int NumeroLinea { get; set; }
    public int IdProducto { get; set; }
    public string? Producto { get; set; }
    public string? CodigoProducto { get; set; }
    public decimal Cantidad { get; set; }
    public decimal PrecioUnitario { get; set; }
    public decimal Descuento { get; set; }
    public decimal Importe { get; set; }

    // en ordenes y ventas: id de la linea; en ordenes, cuanto ya se atendio (despacho)
    public int IdLinea { get; set; }
    public decimal CantidadAtendida { get; set; }

    // en ventas: linea de la orden que atiende (0 en venta directa) y lo despachado
    public int IdDetalleOrden { get; set; }
    public decimal CantidadDespachada { get; set; }

    // importe de referencia mientras se edita (el que vale es el que calcula el BO al guardar)
    [JsonIgnore]
    public decimal ImporteCalculado => Math.Round(Cantidad * PrecioUnitario - Descuento, 2);
}

// RF007: cotizacion
public class CotizacionDatos
{
    public int Id { get; set; }
    public string? Numero { get; set; }
    public int IdCliente { get; set; }
    public string? Cliente { get; set; }
    public string? DocumentoCliente { get; set; }
    public DateOnly? FechaEmision { get; set; }
    public DateOnly? FechaVigencia { get; set; }
    public string? Estado { get; set; }
    public string? Observaciones { get; set; }
    public decimal SubTotal { get; set; }
    public decimal Igv { get; set; }
    public decimal Total { get; set; }
    public string? UsuarioRegistro { get; set; }
    public DateTime? FechaRegistro { get; set; }
    public bool Anulado { get; set; }
    public string? MotivoAnulacion { get; set; }
    public DateTime? FechaAnulacion { get; set; }
    public List<LineaDatos> Lineas { get; set; } = new();
}

// RF007: orden de compra del cliente (se genera desde una cotizacion ACEPTADA)
public class OrdenDatos
{
    public int Id { get; set; }
    public string? Numero { get; set; }
    public string? NumeroOrdenCliente { get; set; }
    public int IdCotizacion { get; set; }
    public string? NumeroCotizacion { get; set; }
    public int IdCliente { get; set; }
    public string? Cliente { get; set; }
    public string? DocumentoCliente { get; set; }
    public DateOnly? FechaEmision { get; set; }
    public string? Estado { get; set; }
    public string? Observaciones { get; set; }
    public decimal SubTotal { get; set; }
    public decimal Igv { get; set; }
    public decimal Total { get; set; }
    public string? UsuarioRegistro { get; set; }
    public DateTime? FechaRegistro { get; set; }
    public bool Anulado { get; set; }
    public string? MotivoAnulacion { get; set; }
    public DateTime? FechaAnulacion { get; set; }
    public List<LineaDatos> Lineas { get; set; } = new();
}

// RF008: venta (directa o desde una orden; contado o credito)
public class VentaDatos
{
    public int Id { get; set; }
    public string? Numero { get; set; }
    public int IdCliente { get; set; }
    public string? Cliente { get; set; }
    public string? DocumentoCliente { get; set; }
    public int IdOrden { get; set; }
    public string? NumeroOrden { get; set; }
    public DateOnly? FechaEmision { get; set; }
    public string CondicionPago { get; set; } = "CONTADO";
    public int PlazoCreditoDias { get; set; }
    public string? Estado { get; set; }
    public string? Observaciones { get; set; }
    public decimal SubTotal { get; set; }
    public decimal Igv { get; set; }
    public decimal Total { get; set; }
    public string? UsuarioRegistro { get; set; }
    public DateTime? FechaRegistro { get; set; }
    public bool Anulado { get; set; }
    public string? MotivoAnulacion { get; set; }
    public DateTime? FechaAnulacion { get; set; }
    public int IdComprobante { get; set; }
    public string? TipoComprobante { get; set; }
    public string? Comprobante { get; set; }
    public List<LineaDatos> Lineas { get; set; } = new();
}

// RF008: boleta o factura de una venta
public class ComprobanteDatos
{
    public int Id { get; set; }
    public int IdVenta { get; set; }
    public string? NumeroVenta { get; set; }
    public string? CondicionPago { get; set; }
    public string? Tipo { get; set; }
    public string? Serie { get; set; }
    public string? Numero { get; set; }
    public string? Comprobante { get; set; }
    public DateOnly? FechaEmision { get; set; }
    public string? Estado { get; set; }
    public decimal SubTotal { get; set; }
    public decimal Igv { get; set; }
    public decimal Total { get; set; }
    public string? MedioEnvio { get; set; }
    public DateTime? FechaEnvio { get; set; }
    public DateTime? FechaRegistro { get; set; }
    public int IdCliente { get; set; }
    public string? Cliente { get; set; }
    public string? TipoDocumentoCliente { get; set; }
    public string? DocumentoCliente { get; set; }
    public string? DireccionCliente { get; set; }
    public string? CorreoCliente { get; set; }
    public List<LineaDatos> Lineas { get; set; } = new();
}

// Lo que se manda a POST .../anular
public class AnulacionDatos
{
    public string? Motivo { get; set; }
}

public static class TextosVentas
{
    public static readonly string[] EstadosCotizacion = { "REGISTRADA", "ENVIADA", "ACEPTADA", "RECHAZADA", "VENCIDA" };

    // Texto en espanol para los valores que vienen del Java
    public static string Texto(string? valor) => valor switch
    {
        "REGISTRADA" => "Registrada", "ENVIADA" => "Enviada", "ACEPTADA" => "Aceptada", "RECHAZADA" => "Rechazada",
        "VENCIDA" => "Vencida", "ANULADA" => "Anulada",
        "PENDIENTE" => "Pendiente", "ATENDIDA_PARCIAL" => "Atendida parcial", "ATENDIDA" => "Atendida",
        "DESPACHADA_PARCIAL" => "Despachada parcial", "DESPACHADA" => "Despachada",
        "CONTADO" => "Contado", "CREDITO" => "Crédito",
        "FACTURA" => "Factura", "BOLETA" => "Boleta",
        "EMITIDO" => "Emitido", "ACEPTADO" => "Aceptado", "RECHAZADO" => "Rechazado", "ANULADO" => "Anulado",
        "CORREO" => "Correo electrónico", "MENSAJERIA" => "Mensajería al contacto", "IMPRESO" => "Formato imprimible",
        "RUC" => "RUC", "DNI" => "DNI", "CARNET_EXTRANJERIA" => "Carné de extranjería", "PASAPORTE" => "Pasaporte",
        _ => valor ?? ""
    };

    public static string Fecha(DateOnly? f) => f?.ToString("dd/MM/yyyy") ?? "—";
    public static string FechaHora(DateTime? f) => f?.ToString("dd/MM/yyyy HH:mm") ?? "—";

    // totales de referencia mientras se editan las lineas (los definitivos los calcula el BO)
    public const decimal TasaIgv = 0.18m;
    public static decimal SubTotal(IEnumerable<LineaDatos> lineas) => lineas.Sum(l => l.ImporteCalculado);
    public static decimal Igv(IEnumerable<LineaDatos> lineas) => Math.Round(SubTotal(lineas) * TasaIgv, 2);
    public static decimal Total(IEnumerable<LineaDatos> lineas) => SubTotal(lineas) + Igv(lineas);
}
