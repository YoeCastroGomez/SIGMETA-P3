namespace SigmetaWeb.Components.Shared;

// PATRÓN UX DE SIGMETA (referencia en las pantallas de ventas de Sebastián: RF007, RF008, RF013, RF014).
// Cada pantalla responde tres preguntas sin que el usuario tenga que pensar:
//   1. ¿En qué estado está esto?        -> <Estado>, <PasosDocumento>, <ChipsFiltro>
//   2. ¿Qué puedo hacer ahora?          -> una acción principal visible; <BotonCondicionado> bloquea lo que no se puede y dice por qué
//   3. ¿Qué sigue y quién lo hace?      -> <SiguientePaso> después de guardar y el responsable en <PasosDocumento>
// Estos tipos los usan esos componentes.

// Un paso del recorrido de un documento (Cotización -> Orden -> Venta -> Comprobante -> Cobro -> Despacho)
public enum EstadoPaso
{
    Hecho,      // ya se hizo (verde, con check)
    Actual,     // es lo que toca ahora (color del tema, con el responsable)
    Pendiente,  // vendrá después (gris)
    Omitido,    // no aplica a este documento (por ejemplo, venta directa sin cotización)
    Anulado     // el documento se anuló (rojo)
}

public sealed record PasoDocumento(
    string Titulo,
    EstadoPaso Estado,
    string? Detalle = null,       // número del documento o una nota corta ("COT-2026-0004", "Al crédito")
    string? Responsable = null,   // quién lo hace ("Cajero"); se muestra en el paso actual y en los pendientes
    string? Ruta = null);         // enlace al documento, si ya existe

// Una opción de los filtros rápidos por estado (chips) de una lista
public sealed record OpcionChip(
    string Valor,                 // "" = Todos
    string Texto,
    int? Cantidad = null,         // cuántos registros hay con ese estado (null = no se muestra)
    string Tono = "");            // "verde", "ambar", "rojo", "azul" o "" (gris): mismo estándar que <Estado>

// Colores de una etiqueta según su tono: el mismo estándar que <Estado>, para textos propios de una pantalla
// ("Por cobrar", "Sin comprobante"...) que <Estado> no conoce.
//   <span class="badge rounded-pill border @Tonos.Clase("ambar")">Por cobrar</span>
public static class Tonos
{
    public static string Clase(string? tono) => tono switch
    {
        "verde" => "bg-success-subtle text-success-emphasis border-success-subtle",
        "ambar" => "bg-warning-subtle text-warning-emphasis border-warning-subtle",
        "rojo" => "bg-danger-subtle text-danger-emphasis border-danger-subtle",
        "azul" => "bg-info-subtle text-info-emphasis border-info-subtle",
        _ => "bg-secondary-subtle text-secondary-emphasis border-secondary-subtle"
    };
}

// Búsqueda rápida en una lista ya cargada: sin distinguir mayúsculas ni tildes ("construcción" = "CONSTRUCCION")
public static class Busqueda
{
    public static bool Coincide(string? buscado, params string?[] campos)
    {
        if (string.IsNullOrWhiteSpace(buscado)) return true;
        var t = Normalizar(buscado.Trim());
        return campos.Any(c => c is not null && Normalizar(c).Contains(t, StringComparison.Ordinal));
    }

    private static string Normalizar(string texto) =>
        new string(texto.ToLowerInvariant().Normalize(System.Text.NormalizationForm.FormD)
            .Where(c => System.Globalization.CharUnicodeInfo.GetUnicodeCategory(c)
                        != System.Globalization.UnicodeCategory.NonSpacingMark)
            .ToArray());
}

// Vuelta a una lista conservando sus filtros. Las listas mandan su dirección actual en ?volver=…
// y el formulario regresa ahí (con el aviso de guardado) en lugar de a la lista limpia.
public static class Volver
{
    // Solo se acepta una dirección interna que empiece con la lista esperada; si no, la lista sin filtros
    public static string A(string? volver, string porDefecto) =>
        !string.IsNullOrWhiteSpace(volver) && volver.StartsWith(porDefecto, StringComparison.Ordinal)
            && !volver.StartsWith("//", StringComparison.Ordinal)
            ? volver
            : porDefecto;

    // "/ventas?situacion=x" + nuevo + "VTA-1" -> "/ventas?situacion=x&guardado=nuevo&ref=VTA-1"
    public static string ConAviso(string ruta, string accion, string? referencia)
    {
        var limpia = QuitarParametros(ruta, "guardado", "ref");
        var aviso = AvisoGuardado.Ruta("", accion, referencia).TrimStart('?');
        return limpia + (limpia.Contains('?') ? "&" : "?") + aviso;
    }

    // Para armar enlaces "…/editar/5?volver=…" desde la lista
    public static string Parametro(string rutaActual) => Uri.EscapeDataString(QuitarParametros(rutaActual, "guardado", "ref"));

    private static string QuitarParametros(string ruta, params string[] nombres)
    {
        var partes = ruta.Split('?', 2);
        if (partes.Length < 2) return ruta;
        var quedan = partes[1].Split('&', StringSplitOptions.RemoveEmptyEntries)
            .Where(p => !nombres.Contains(p.Split('=', 2)[0], StringComparer.OrdinalIgnoreCase))
            .ToArray();
        return quedan.Length == 0 ? partes[0] : partes[0] + "?" + string.Join("&", quedan);
    }
}
