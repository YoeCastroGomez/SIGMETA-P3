using System.Globalization;
using Microsoft.AspNetCore.Components;

namespace SigmetaWeb.Components.Shared;

// Estándar de SIGMETA para los errores de un campo (RNF004): texto rojo en español debajo del cuadro, mientras
// se escribe. Cada formulario arma sus reglas en un método Errores() (las mismas que valida su BO) y usa esta clase
// para saber qué mostrar:
//   - un cuadro muestra su error recién cuando se escribió en él, o cuando se pulsó Guardar (Intentar());
//   - InputText e InputNumber pasan el valor al modelo solo al salir del cuadro, por eso lo que se va escribiendo
//     se guarda aquí (@oninput="@(e => v.Escribiendo("campo", e))") y se valida al momento.
// Uso en la pantalla:
//   <Campo ... Error="@(v.Error("ruc", Errores()))">
//       <InputText class="@(v.Clase("ruc", Errores()))" @bind-Value="..." @oninput="@(e => v.Escribiendo("ruc", e))" />
// Los errores del BO (Java) siguen saliendo en el aviso rojo de arriba (AvisoError).
public sealed class ValidacionEnVivo
{
    private readonly Dictionary<string, string?> escrito = new();

    // true después de pulsar Guardar: desde ahí se marcan también los cuadros que no se tocaron
    public bool Intentado { get; private set; }

    public void Escribiendo(string campo, ChangeEventArgs e) => escrito[campo] = e.Value?.ToString();

    // Para un select o un check: no hay texto a medio escribir, solo se marca como tocado
    public void Tocado(string campo) => escrito[campo] = null;

    // Lo escrito en el cuadro ya no vale (por ejemplo, el cuadro se deshabilitó o se llenó solo)
    public void Olvidar(string campo) => escrito.Remove(campo);

    public bool FueTocado(string campo) => escrito.ContainsKey(campo);

    // Marca todos los cuadros y dice si se puede enviar
    public bool Intentar(IReadOnlyDictionary<string, string> errores)
    {
        Intentado = true;
        return errores.Count == 0;
    }

    // Vuelve al estado inicial (por ejemplo, al cerrar un diálogo o después de guardar)
    public void Reiniciar()
    {
        escrito.Clear();
        Intentado = false;
    }

    // Texto actual del cuadro: lo que se está escribiendo o, si no se tocó, el valor del modelo
    public string Texto(string campo, string? valorModelo) =>
        escrito.TryGetValue(campo, out var t) && t is not null ? t : valorModelo ?? "";

    // Número actual del cuadro. Si lo escrito no es un número, devuelve null.
    public decimal? Numero(string campo, decimal? valorModelo)
    {
        if (!escrito.TryGetValue(campo, out var t) || t is null)
        {
            return valorModelo;
        }
        if (string.IsNullOrWhiteSpace(t))
        {
            return null;
        }
        return decimal.TryParse(t, NumberStyles.Number, CultureInfo.InvariantCulture, out var v) ? v : null;
    }

    public string? Error(string campo, IReadOnlyDictionary<string, string> errores, params string[] tambienAlTocar)
    {
        var mostrar = Intentado || escrito.ContainsKey(campo) || tambienAlTocar.Any(escrito.ContainsKey);
        return mostrar && errores.TryGetValue(campo, out var mensaje) ? mensaje : null;
    }

    public string Clase(string campo, IReadOnlyDictionary<string, string> errores, string clase = "form-control",
                        params string[] tambienAlTocar) =>
        Error(campo, errores, tambienAlTocar) is null ? clase : clase + " is-invalid";

    // Clave estable de una línea de una tabla (las líneas se agregan y se quitan, por eso no se usa la posición).
    // Uso: var n = v.Clave(linea);  ...  v.Error(n + "cantidad", Errores())
    private readonly Dictionary<object, int> claves = new(ReferenceEqualityComparer.Instance);
    private int ultimaClave;

    public string Clave(object linea)
    {
        if (!claves.TryGetValue(linea, out var c))
        {
            c = claves[linea] = ++ultimaClave;
        }
        return $"l{c}.";
    }

    // Reglas de una línea de cotización o venta (las mismas del BO): cantidad > 0 con 3 decimales, precio >= 0 con
    // 2 decimales, descuento entre 0 y cantidad × precio.
    public static void ErroresLinea(Dictionary<string, string> e, string n, decimal cantidad, decimal precio, decimal descuento)
    {
        if (cantidad <= 0)
        {
            e[n + "cantidad"] = "Debe ser mayor a 0.";
        }
        else if (MasDecimales(cantidad, 3))
        {
            e[n + "cantidad"] = "Máximo 3 decimales.";
        }
        if (precio < 0)
        {
            e[n + "precio"] = "No puede ser negativo.";
        }
        else if (MasDecimales(precio, 2))
        {
            e[n + "precio"] = "Máximo 2 decimales.";
        }
        if (descuento < 0)
        {
            e[n + "descuento"] = "No puede ser negativo.";
        }
        else if (MasDecimales(descuento, 2))
        {
            e[n + "descuento"] = "Máximo 2 decimales.";
        }
        else if (cantidad > 0 && precio >= 0 && descuento > cantidad * precio)
        {
            e[n + "descuento"] = "No puede pasar de cantidad × precio.";
        }
    }

    // ---------------- Reglas comunes ----------------

    public static bool MasDecimales(decimal valor, int maximo) => valor != Math.Round(valor, maximo);

    public static bool SoloDigitos(string texto) => texto.Length > 0 && texto.All(char.IsAsciiDigit);

    // Mismo formato que acepta el BO: algo@algo.algo, sin espacios
    public static bool CorreoValido(string correo) =>
        System.Text.RegularExpressions.Regex.IsMatch(correo, @"^[^@\s]+@[^@\s]+\.[^@\s]+$");

    // RUC peruano: 11 dígitos que empiezan en 10, 15, 16, 17 o 20 (igual que el BO)
    public static string? ErrorRuc(string ruc)
    {
        if (string.IsNullOrWhiteSpace(ruc))
        {
            return "El RUC es obligatorio.";
        }
        if (!SoloDigitos(ruc) || ruc.Length != 11)
        {
            return "El RUC debe tener 11 dígitos.";
        }
        if (ruc[..2] is not ("10" or "15" or "16" or "17" or "20"))
        {
            return "El RUC debe empezar en 10, 15, 16, 17 o 20.";
        }
        return null;
    }

    // Documento de identidad según su tipo (mismas reglas que Validador.numeroDocumento del BO)
    public static string? ErrorDocumento(string? tipo, string numero)
    {
        if (tipo == "RUC")
        {
            return ErrorRuc(numero);
        }
        if (string.IsNullOrWhiteSpace(numero))
        {
            return "El número de documento es obligatorio.";
        }
        var alfanumerico = numero.All(char.IsAsciiLetterOrDigit);
        return tipo switch
        {
            "DNI" when !SoloDigitos(numero) || numero.Length != 8 => "El DNI debe tener 8 dígitos.",
            "CARNET_EXTRANJERIA" when !alfanumerico || numero.Length is < 9 or > 12
                => "El carné de extranjería debe tener de 9 a 12 letras o dígitos.",
            "PASAPORTE" when !alfanumerico || numero.Length is < 6 or > 12
                => "El pasaporte debe tener de 6 a 12 letras o dígitos.",
            _ => null
        };
    }

    // Opcional: vacío vale. Solo dígitos, espacios, + y - (igual que el BO)
    public static string? ErrorTelefono(string telefono) =>
        string.IsNullOrWhiteSpace(telefono) || telefono.Trim().All(c => char.IsAsciiDigit(c) || c is ' ' or '+' or '-')
            ? null
            : "El teléfono solo puede tener dígitos, espacios, + y -.";

    // Opcional: vacío vale
    public static string? ErrorCorreo(string correo) =>
        string.IsNullOrWhiteSpace(correo) || CorreoValido(correo.Trim())
            ? null
            : "El correo no tiene un formato válido (ejemplo: compras@empresa.pe).";

    // Filtros por rango de fechas: el error sale debajo de "hasta" en cuanto se eligen las dos fechas
    public static string? ErrorRango<T>(T? desde, T? hasta) where T : struct, IComparable<T> =>
        desde is { } d && hasta is { } h && d.CompareTo(h) > 0
            ? "La fecha final no puede ser anterior a la inicial."
            : null;

    public static string ClaseRango<T>(T? desde, T? hasta) where T : struct, IComparable<T> =>
        ErrorRango(desde, hasta) is null ? "form-control" : "form-control is-invalid";

    // Evita escribir "-", "+" y "e" en los campos de números
    public const string SoloPositivos = "if (['-', '+', 'e', 'E'].includes(event.key)) { event.preventDefault(); }";
}
