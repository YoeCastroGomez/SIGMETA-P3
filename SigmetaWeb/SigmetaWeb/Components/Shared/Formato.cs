using System.Globalization;

namespace SigmetaWeb.Components.Shared;

// Formatos de presentación comunes a todas las pantallas (RNF004): fechas dd/MM/yyyy, importes en soles con
// dos decimales y cantidades sin ceros de más. El Java manda las fechas como texto AAAA-MM-DD o
// AAAA-MM-DDTHH:MM:SS.
public static class Formato
{
    private static readonly CultureInfo Peru = CultureInfo.GetCultureInfo("es-PE");

    // "2026-11-10" o "2026-11-10T10:15:00" -> "10/11/2026"
    public static string Fecha(string? texto) =>
        DateTime.TryParse(texto, CultureInfo.InvariantCulture, DateTimeStyles.None, out var f)
            ? f.ToString("dd/MM/yyyy", CultureInfo.InvariantCulture) : "—";

    // "2026-11-10T10:15:00" -> "10/11/2026 10:15"
    public static string FechaHora(string? texto) =>
        DateTime.TryParse(texto, CultureInfo.InvariantCulture, DateTimeStyles.None, out var f)
            ? f.ToString("dd/MM/yyyy HH:mm", CultureInfo.InvariantCulture) : "—";

    // 1234.5 -> "S/ 1,234.50"
    public static string Soles(double? monto) => "S/ " + (monto ?? 0).ToString("N2", Peru);

    // 12 -> "12", 12.5 -> "12.5", 12.125 -> "12.125"
    public static string Cantidad(double? cantidad) => (cantidad ?? 0).ToString("0.###", Peru);

    // fecha para mandar al Java en la dirección: AAAA-MM-DD (vacío si no hay)
    public static string Iso(DateTime? fecha) => fecha?.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture) ?? "";

    // "RECIBIDA_PARCIAL" -> "Recibida parcial", "INGRESO_COMPRA" -> "Ingreso compra"
    public static string Legible(string? codigo)
    {
        if (string.IsNullOrWhiteSpace(codigo)) return "—";
        var texto = codigo.Replace('_', ' ').ToLowerInvariant();
        return char.ToUpperInvariant(texto[0]) + texto[1..];
    }

    // Para armar un CSV que Excel en español abre bien: separador ";" y comillas dobles
    public static string Csv(IEnumerable<string?> celdas) =>
        string.Join(";", celdas.Select(c => "\"" + (c ?? "").Replace("\"", "\"\"") + "\""));
}
