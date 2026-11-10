using System.ComponentModel.DataAnnotations;

namespace SigmetaWeb.Components.Pages.Catalogo;

// RF004: modelo de intercambio con /api/proveedores (Java).
// En el JSON, el estado se llama "estado", no "activo".
public class Proveedor
{
    public int Id { get; set; }

    [Required(ErrorMessage = "El RUC es obligatorio")]
    [RegularExpression(@"^(10|15|16|17|20)\d{9}$", ErrorMessage = "El RUC debe tener 11 dígitos y un prefijo válido")]
    public string Ruc { get; set; } = "";

    [Required(ErrorMessage = "La razón social es obligatoria")]
    [StringLength(150, ErrorMessage = "La razón social admite máximo 150 caracteres")]
    public string RazonSocial { get; set; } = "";

    [StringLength(100)]
    public string? Rubro { get; set; }

    public DatosContactoProveedor Contacto { get; set; } = new();

    [Range(0, int.MaxValue, ErrorMessage = "El plazo de entrega no puede ser negativo")]
    public int PlazoEntregaDias { get; set; }

    [Required]
    public string CondicionPago { get; set; } = "CONTADO";

    public bool Estado { get; set; } = true;
}

public class DatosContactoProveedor
{
    [StringLength(200)]
    public string? Direccion { get; set; }

    [StringLength(20)]
    public string? Telefono { get; set; }

    [EmailAddress(ErrorMessage = "El correo no tiene un formato válido")]
    [StringLength(120)]
    public string? Correo { get; set; }

    [StringLength(100)]
    public string? NombreContacto { get; set; }
}
