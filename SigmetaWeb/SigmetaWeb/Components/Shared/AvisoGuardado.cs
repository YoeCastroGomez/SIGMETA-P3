namespace SigmetaWeb.Components.Shared;

// Patrón único de SIGMETA para los avisos de éxito (RNF004):
//   "Se registró / Se modificó / Se anuló / Se desactivó + el qué + el identificador."
//   Ejemplos: "Se registró el proveedor Aceros del Perú S.A.", "Se modificó la cotización COT-2026-0012."
// Un formulario que guarda y vuelve a su lista arma la ruta con Ruta(); la lista lee los parámetros
// ?guardado=…&ref=… y muestra el texto con Texto() en su aviso verde.
public static class AvisoGuardado
{
    public const string Nuevo = "nuevo";
    public const string Editado = "editado";
    public const string Anulado = "anulado";
    public const string Desactivado = "desactivado";
    public const string Enviado = "enviado";   // RF007: la cotización se marcó como enviada al cliente
    public const string Cobrado = "cobrado";      // cobro al contado de una venta: "Se cobró la venta V-2026-0012."

    // "/proveedores" + "nuevo" + "Aceros del Perú S.A." -> "/proveedores?guardado=nuevo&ref=Aceros%20del%20Per%C3%BA%20S.A."
    public static string Ruta(string ruta, string accion, string? referencia)
    {
        var destino = $"{ruta}?guardado={accion}";
        return string.IsNullOrWhiteSpace(referencia) ? destino : $"{destino}&ref={Uri.EscapeDataString(referencia.Trim())}";
    }

    // Devuelve null si la página no llegó desde un formulario (o si el parámetro no es uno de los conocidos).
    // "que" lleva su artículo: "el proveedor", "la cotización".
    public static string? Texto(string? accion, string que, string? referencia)
    {
        var verbo = accion switch
        {
            Nuevo => "Se registró",
            Editado => "Se modificó",
            Anulado => "Se anuló",
            Desactivado => "Se desactivó",
            Enviado => "Se envió",
            Cobrado => "Se cobró",
            _ => null
        };
        if (verbo is null) return null;
        var identificador = string.IsNullOrWhiteSpace(referencia) ? "" : " " + referencia.Trim().TrimEnd('.');
        return $"{verbo} {que}{identificador}.";
    }
}
