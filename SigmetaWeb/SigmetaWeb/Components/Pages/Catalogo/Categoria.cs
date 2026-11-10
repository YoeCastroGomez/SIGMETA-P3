using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SigmetaWeb.Components.Pages.Catalogo;

// RF006: mismo formato que el JSON de /api/categorias (clase Categoria del Java).
// En el Java el campo se llama "estado" (isEstado); aquí se llama Activo para que se lea mejor.
// Las anotaciones validan lo básico en la pantalla; el nombre repetido, el permiso del rol y la regla
// de "no desactivar con productos activos" las valida el BO en el Java.
public class Categoria
{    
    public int Id { get ; set; }

    [Required(ErrorMessage = "El nombre es obligatorio")]
    [StringLength(100, ErrorMessage = "El nombre es demasiado largo (máximo 100)")]
    public string Nombre { get; set; } = "";

    [StringLength(255, ErrorMessage = "La descripción es demasiado larga (máximo 255)")]
    public string? Descripcion { get; set; }

    [JsonPropertyName("estado")]
    public bool Activo { get; set; } = true;

    // copia para editar en el formulario sin tocar la fila de la tabla hasta que se guarde
    public Categoria Copia() => new() { Id = Id, Nombre = Nombre, Descripcion = Descripcion, Activo = Activo };
}
