using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SigmetaWeb.Components.Pages.Clientes;

// RF003: mismo formato que el JSON de /api/clientes (clase Cliente del Java).
// Los valores de los enums viajan como texto: "JURIDICA", "RUC", "CONTADO", "A"...
// Las anotaciones validan lo básico en la pantalla (EditForm + DataAnnotationsValidator);
// las reglas del negocio (documento repetido, RUC válido, etc.) las valida el BO en el Java.
public class Cliente
{
    public int Id { get; set; }

    [Required(ErrorMessage = "Elija el tipo de persona")]
    public string TipoPersona { get; set; } = "JURIDICA";

    [StringLength(150, ErrorMessage = "La razón social es demasiado larga")]
    public string? RazonSocial { get; set; }

    [StringLength(100, ErrorMessage = "Los nombres son demasiado largos")]
    public string? Nombres { get; set; }

    [StringLength(100, ErrorMessage = "Los apellidos son demasiado largos")]
    public string? Apellidos { get; set; }

    [Required(ErrorMessage = "Elija el tipo de documento")]
    public string TipoDocumento { get; set; } = "RUC";

    [Required(ErrorMessage = "El número de documento es obligatorio")]
    [StringLength(20, ErrorMessage = "El número de documento es demasiado largo")]
    public string NumeroDocumento { get; set; } = "";

    public DatosContacto Contacto { get; set; } = new();
    public string CondicionPago { get; set; } = "CONTADO";
    public decimal LimiteCredito { get; set; }
    public int PlazoCreditoDias { get; set; }
    public string CalificacionCrediticia { get; set; } = "SIN_EVALUAR";
    public bool Activo { get; set; } = true;

    // Texto para la tabla: razón social o nombres y apellidos (no se manda al Java)
    [JsonIgnore]
    public string NombreMostrado => TipoPersona == "JURIDICA" ? RazonSocial ?? "" : $"{Nombres} {Apellidos}";

    // Texto en español para los valores que vienen del Java
    public static string Texto(string? valor) => valor switch
    {
        "JURIDICA" => "Empresa", "NATURAL" => "Persona natural",
        "CARNET_EXTRANJERIA" => "Carné de extranjería", "PASAPORTE" => "Pasaporte",
        "CONTADO" => "Contado", "CREDITO" => "Crédito",
        "SIN_EVALUAR" => "Sin evaluar", "A" => "A · riesgo bajo", "B" => "B · riesgo medio", "C" => "C · riesgo alto",
        _ => valor ?? ""
    };
}

public class DatosContacto
{
    [StringLength(200, ErrorMessage = "La dirección es demasiado larga")]
    public string? Direccion { get; set; }

    [StringLength(20, ErrorMessage = "El teléfono es demasiado largo")]
    public string? Telefono { get; set; }

    [EmailAddress(ErrorMessage = "El correo no tiene un formato válido")]
    [StringLength(120, ErrorMessage = "El correo es demasiado largo")]
    public string? Correo { get; set; }

    [StringLength(100, ErrorMessage = "El nombre de contacto es demasiado largo")]
    public string? NombreContacto { get; set; }
}

// Lo que se manda a PUT /api/clientes/{id}/linea-credito (clase LineaCredito del Java)
public class LineaCreditoDatos
{
    [Range(0.01, 99999999, ErrorMessage = "El límite de crédito debe ser mayor a 0")]
    public decimal LimiteCredito { get; set; }

    [Range(1, 3650, ErrorMessage = "El plazo debe ser mayor a 0 días")]
    public int PlazoCreditoDias { get; set; }

    [Required(ErrorMessage = "Elija la calificación")]
    public string CalificacionCrediticia { get; set; } = "SIN_EVALUAR";
}
