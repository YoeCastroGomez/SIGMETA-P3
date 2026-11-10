using System.ComponentModel.DataAnnotations;
using System.Globalization;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace SigmetaWeb.Components.Pages.Catalogo;

// RF005: mismo formato que el JSON de /api/productos (clase Producto del Java).
// Las unidades viajan como texto: "UNIDAD", "CAJA", "METRO"...
// En la pantalla solo se validan los textos obligatorios. Los montos (precio en 0, costo o stock mínimo
// negativo, factor que no corresponde) los valida el BO a propósito, para que salga su mensaje.
public class Producto
{
    public int Id { get; set; }

    [Required(ErrorMessage = "El código interno es obligatorio")]
    [StringLength(50, ErrorMessage = "El código interno es demasiado largo (máximo 50)")]
    public string CodigoInterno { get; set; } = "";

    [StringLength(50, ErrorMessage = "El código de fabricante es demasiado largo (máximo 50)")]
    public string? CodigoFabricante { get; set; }

    [StringLength(50, ErrorMessage = "El código de proveedor es demasiado largo (máximo 50)")]
    public string? CodigoProveedor { get; set; }

    [Required(ErrorMessage = "El nombre es obligatorio")]
    [StringLength(150, ErrorMessage = "El nombre es demasiado largo (máximo 150)")]
    public string Nombre { get; set; } = "";

    [StringLength(255, ErrorMessage = "La descripción es demasiado larga (máximo 255)")]
    public string? Descripcion { get; set; }

    // al Java solo le importa el id; el nombre lo completa la ventanilla para mostrarlo en la tabla
    public Categoria Categoria { get; set; } = new();

    public string UnidadCompra { get; set; } = "UNIDAD";
    public string UnidadVenta { get; set; } = "UNIDAD";
    public decimal FactorConversion { get; set; } = 1;
    public decimal PrecioVenta { get; set; }
    public decimal PrecioReferencial { get; set; }
    public decimal CostoUnitario { get; set; }
    public decimal StockActual { get; set; }
    public decimal StockMinimo { get; set; }

    // por ahora la imagen es solo el nombre o la ruta del archivo (texto)
    [StringLength(255, ErrorMessage = "El nombre de la imagen es demasiado largo (máximo 255)")]
    public string? Imagen { get; set; }

    [JsonPropertyName("estado")]
    public bool Activo { get; set; } = true;

    // RNF001: quién y cuándo hizo la última modificación. Los pone el BO; Blazor nunca los manda
    // (WhenWritingNull: si están en null no se envían).
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public UsuarioVisible? UsuarioModificacion { get; set; }

    // la fecha llega como texto "2026-11-09T10:15:30"; se lee como JsonElement para no fallar con otro formato
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public JsonElement? FechaModificacion { get; set; }

    // ---- Solo para la pantalla (no se mandan al Java) ----

    [JsonIgnore]
    public bool StockBajo => StockActual <= StockMinimo;

    [JsonIgnore]
    public string StockTexto => $"{StockActual.ToString("0.###")} {TextoUnidad(UnidadVenta).ToLowerInvariant()}";

    // "aquispe · 09/11/2026 10:15" o null si nunca se modificó
    [JsonIgnore]
    public string? UltimaModificacion
    {
        get
        {
            var fecha = FechaTexto(FechaModificacion);
            if (UsuarioModificacion is null && fecha is null)
            {
                return null;
            }
            var usuario = UsuarioModificacion?.NombreUsuario ?? "—";
            return fecha is null ? usuario : $"{usuario} · {fecha}";
        }
    }

    public static readonly string[] Unidades =
        { "UNIDAD", "CAJA", "KILOGRAMO", "METRO", "LITRO", "MILLAR", "GALON", "JUEGO" };

    public static string TextoUnidad(string? valor) => valor switch
    {
        "UNIDAD" => "Unidad", "CAJA" => "Caja", "KILOGRAMO" => "Kilogramo", "METRO" => "Metro",
        "LITRO" => "Litro", "MILLAR" => "Millar", "GALON" => "Galón", "JUEGO" => "Juego",
        _ => valor ?? ""
    };

    // toma "AAAA-MM-DDTHH:mm" del texto que manda el Java y lo muestra como 09/11/2026 10:15
    private static string? FechaTexto(JsonElement? valor)
    {
        if (valor is not JsonElement elemento || elemento.ValueKind != JsonValueKind.String)
        {
            return null;
        }
        var texto = elemento.GetString();
        if (texto is null || texto.Length < 16)
        {
            return texto;
        }
        return DateTime.TryParseExact(texto[..16], "yyyy-MM-dd'T'HH:mm", CultureInfo.InvariantCulture,
                                      DateTimeStyles.None, out var fecha)
            ? fecha.ToString("yyyy-MM-dd HH:mm")
            : texto;
    }
}

// Lo que la ventanilla manda del usuario que modificó: sin id y sin clave
public class UsuarioVisible
{
    public string? NombreUsuario { get; set; }
    public string? Nombres { get; set; }
    public string? Apellidos { get; set; }
}
