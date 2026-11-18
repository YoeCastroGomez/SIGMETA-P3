using System.Text.Json.Serialization;

namespace SigmetaWeb.Components.Pages.Ventas;

// RF007 y RF008: mismo formato que el JSON de /api/cotizaciones, /api/ordenes, /api/ventas y /api/comprobantes
// (clases CotizacionDatos, OrdenDatos, VentaDatos, ComprobanteDatos y LineaDatos del Java, paquete ws.ventas).
// Los enums viajan como texto ("REGISTRADA", "CONTADO", "BOLETA"...) y las fechas como AAAA-MM-DD.
// Los totales, el estado y los numeros correlativos los calcula el BO en el Java; aqui solo se muestran.
// Despachos usa DatosAlmacen.cs y las notas de credito DatosNotaCredito.cs.

// Una linea de cotizacion, de orden o de venta
public class LineaDatos
{
    public int NumeroLinea { get; set; }
    public int IdProducto { get; set; }
    public string? Producto { get; set; }
    public string? CodigoProducto { get; set; }
    public string? UnidadVenta { get; set; }
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
    public decimal ImporteCalculado => Math.Max(0, Math.Round(Cantidad * PrecioUnitario - Descuento, 2));   // nunca negativo mientras se escribe

    // RF007/RF008: solo kilogramo, metro y litro admiten decimales; lo demás se vende entero (mismo criterio que
    // UnidadesVenta en el BO). Sin unidad conocida no se restringe.
    [JsonIgnore]
    public bool AdmiteDecimales => UnidadVenta is null or "KILOGRAMO" or "METRO" or "LITRO";

    [JsonIgnore]
    public bool CantidadNoEntera => !AdmiteDecimales && Cantidad != decimal.Truncate(Cantidad);

    // onkeydown de la cantidad: sin signos y, si la unidad es entera, sin punto ni coma
    [JsonIgnore]
    public string TeclasCantidad => AdmiteDecimales ? SinSignos : SoloEnteros;


    [JsonIgnore]
    // con step="1", un mínimo de 0 deja las flechas en números enteros también en kg, m y L (data-positivo impide el 0)
    public string MinimoCantidad => AdmiteDecimales ? "0" : "1";

    // unidad abreviada junto a la cantidad y al stock
    [JsonIgnore]
    public string UnidadCorta => UnidadVenta switch
    {
        "UNIDAD" => "und", "CAJA" => "caja", "KILOGRAMO" => "kg", "METRO" => "m", "LITRO" => "L",
        "MILLAR" => "mill", "GALON" => "gal", "JUEGO" => "jgo", _ => ""
    };

    // descuento en soles: las flechas van de 1 en 1, se pueden escribir céntimos y nunca pasa del importe bruto
    [JsonIgnore]
    public string TeclasDescuento => SinSignos;

    [JsonIgnore]
    public decimal Bruto => Math.Round(Cantidad * PrecioUnitario, 2);

    [JsonIgnore]
    public string DescuentoMaximo => Bruto.ToString(System.Globalization.CultureInfo.InvariantCulture);

    // Al salir de un cuadro se corrige lo que no tiene sentido: cantidad 0 o negativa (vuelve al mínimo),
    // descuento negativo (0) o mayor que el importe bruto (el bruto: la línea queda en 0, nunca negativa).
    public void Ajustar()
    {
        if (Cantidad <= 0)
        {
            Cantidad = 1;
        }
        if (Descuento < 0)
        {
            Descuento = 0;
        }
        if (Descuento > Bruto)
        {
            Descuento = Bruto;
        }
    }

    // oninput de cantidad y descuento: si lo escrito no es válido (menor que min, mayor que max o con más
    // decimales de los permitidos) se deshace en el acto y vuelve al último valor válido. Así el 0 en la cantidad
    // o un descuento mayor que el importe no se pueden escribir. Las flechas van de 1 en 1 (step="1") y el
    // navegador no las deja salir de min y max.
    public const string Validar =
        "var v = this.value, n = Number(v), d = Number(this.dataset.decimales || 3);" +
        "var ok = v === '' || (new RegExp('^[0-9]*[.]?[0-9]{0,' + d + '}$').test(v) && n >= Number(this.min || 0)" +
        " && (!this.dataset.positivo || n > 0)" +
        " && (this.max === '' || n <= Number(this.max)));" +
        "if (ok) { this.dataset.prev = v; } else { this.value = this.dataset.prev ?? this.getAttribute('value') ?? ''; }";

    // onfocusout: si el cuadro quedó vacío, vuelve al último valor válido y avisa a Blazor (evento change)
    public const string Restaurar =
        "if (this.value === '') { this.value = this.dataset.prev || this.getAttribute('value') || this.min || '0';" +
        " this.dispatchEvent(new Event('change', { bubbles: true })); }";

    private const string SinSignos = "if (['-', '+', 'e', 'E'].includes(event.key)) { event.preventDefault(); }";
    private const string SoloEnteros = "if (['-', '+', 'e', 'E', '.', ','].includes(event.key)) { event.preventDefault(); }";
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
    // RF008/RF010: venta al contado ya cobrada en caja; el BO ya no la deja modificar
    public bool CobradaContado { get; set; }
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
    // RF007: los seis estados del catálogo (filtro de la lista de cotizaciones)
    public static readonly string[] EstadosCotizacion = { "REGISTRADA", "ENVIADA", "ACEPTADA", "RECHAZADA", "VENCIDA", "ANULADA" };

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

    // mismo formato que el resto del sistema (AAAA-MM-DD), igual que TextosCaja
    public static string Fecha(DateOnly? f) => f?.ToString("yyyy-MM-dd") ?? "—";
    public static string FechaHora(DateTime? f) => f?.ToString("yyyy-MM-dd HH:mm") ?? "—";

    // totales de referencia mientras se editan las lineas (los definitivos los calcula el BO, PreciosIgv).
    // Los precios incluyen IGV: el total es la suma de los importes y se desglosa en valor de venta + IGV.
    public const decimal TasaIgv = 0.18m;
    public static decimal Total(IEnumerable<LineaDatos> lineas) => lineas.Sum(l => l.ImporteCalculado);
    public static decimal SubTotal(IEnumerable<LineaDatos> lineas) => Math.Round(Total(lineas) / (1 + TasaIgv), 2, MidpointRounding.AwayFromZero);
    public static decimal Igv(IEnumerable<LineaDatos> lineas) => Total(lineas) - SubTotal(lineas);
}
