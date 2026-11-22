// Código de la página Categorias (parte 1 de 1). La vista está en Categorias.razor: es la misma clase (partial).
namespace SigmetaWeb.Components.Pages.Catalogo;

public partial class Categorias
{
    // UI/UX: una acción visible por fila y el resto en «⋯» (menos ruido en tablas densas; mismo patrón que Ventas)
    private List<AccionFila> OtrasAcciones(Categoria c) => c.Activo
        ? new() { new("Desactivar", Hacer: () => { PedirDesactivar(c); return Task.CompletedTask; }, Peligro: true) }
        : new() { new("Activar", Hacer: () => Activar(c)) };
    private Task Ejecutar(AccionFila a) => a.Hacer?.Invoke() ?? Task.CompletedTask;

    private int paginaActual = 1;
    private int tamanoPagina = 10;
    private IEnumerable<T> Paginar<T>(IEnumerable<T>? registros, int pagina, int tamano)
    {
        if (registros is null) return Enumerable.Empty<T>();
        if (tamano == 0) return registros;
        int total = registros.Count();
        int paginaSegura = Math.Clamp(pagina, 1, Math.Max(1, (total + tamano - 1) / tamano));
        return registros.Skip((paginaSegura - 1) * tamano).Take(tamano);
    }

    private List<Categoria>? categorias;
    private Categoria? formulario;      // null = formulario cerrado; Id 0 = nueva
    private string filtroNombre = "";
    private string filtroEstado = "";
    private string? error;
    private string? exito;
    private bool guardando;

    // al abrir la pantalla se piden las categorías al Java
    protected override async Task OnInitializedAsync()
    {
        await Cargar("categorias");
    }

    private async Task Buscar()
    {
        exito = null;
        // GET /api/categorias?nombre=..&estado=.. : los filtros llenos se combinan (CategoriaBO.buscar)
        var parametros = new List<string>();
        if (!string.IsNullOrWhiteSpace(filtroNombre))
        {
            parametros.Add($"nombre={Uri.EscapeDataString(filtroNombre.Trim())}");
        }
        if (filtroEstado != "")
        {
            parametros.Add($"estado={filtroEstado}");
        }
        await Cargar(parametros.Count == 0 ? "categorias" : "categorias?" + string.Join("&", parametros));
    }

    private async Task Limpiar()
    {
        filtroNombre = filtroEstado = "";
        exito = null;
        await Cargar("categorias");
    }

    private async Task Cargar(string ruta)
    {
        error = null;
        try
        {
            categorias = await Api.Get<List<Categoria>>(ruta) ?? new List<Categoria>();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
            categorias ??= new List<Categoria>();
        }
    }

    // Desactivar pide confirmación: primero se guarda cuál es y se muestra el cuadro
    private Categoria? porDesactivar;

    private void PedirDesactivar(Categoria c)
    {
        error = exito = null;
        porDesactivar = c;
    }

    private async Task ConfirmarDesactivar()
    {
        var c = porDesactivar;
        porDesactivar = null;   // cierra el cuadro
        if (c is not null)
        {
            await Desactivar(c);
        }
    }

    private void Nueva()
    {
        error = exito = null;
        formulario = new Categoria();
        nombreEscrito = null;
        intentoGuardar = false;
    }

    private void Editar(Categoria c)
    {
        error = exito = null;
        formulario = c.Copia();
        nombreEscrito = null;
        intentoGuardar = false;
    }

    private void Cerrar() => formulario = null;

    // POST /api/categorias o PUT /api/categorias/{id}. Solo se ejecuta si pasan las validaciones de la pantalla.
    // Validación en pantalla, con el mismo estándar que el formulario de productos: el error sale en rojo
    // debajo del cuadro mientras se escribe (o al pulsar Guardar si no se tocó). El BO vuelve a validar.
    private string? nombreEscrito;    // lo que se está escribiendo (InputText pasa el valor recién al salir)
    private bool intentoGuardar;

    private string? ErrorNombre =>
        formulario is not null && (intentoGuardar || nombreEscrito is not null)
        && string.IsNullOrWhiteSpace(nombreEscrito ?? formulario.Nombre)
            ? "El nombre es obligatorio."
            : null;

    private async Task Guardar()
    {
        error = exito = null;
        intentoGuardar = true;
        if (ErrorNombre is not null)
        {
            return;   // el mensaje ya está debajo del cuadro
        }
        guardando = true;
        try
        {
            // el BO preprocesa los textos (sin espacios de más) y devuelve la categoria ya guardada
            var nueva = formulario!.Id == 0;
            var guardada = nueva
                ? await Api.Post<Categoria>("categorias", formulario)
                : await Api.Put<Categoria>($"categorias/{formulario.Id}", formulario);
            var nombre = guardada?.Nombre ?? formulario.Nombre;
            formulario = null;
            await Buscar();   // vuelve a pedir la lista al Java para ver el cambio guardado
            exito = nueva ? $"Se registró la categoría {nombre}." : $"Se modificó la categoría {nombre}.";
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "Ya existe la categoria Herramientas" o "El rol VENDEDOR no tiene permiso..."
        }
        catch (Exception)
        {
            // cualquier otro error (por ejemplo al leer la respuesta del Java) también sale en el aviso rojo
            error = "No se pudo guardar la categoría. Inténtalo otra vez; si el problema continúa, avisa al Administrador.";
        }
        finally
        {
            guardando = false;
        }
        await SubirAlAviso();
    }

    // Sube la página hasta los avisos (están arriba de todo)
    private async Task SubirAlAviso()
    {
        try
        {
            await JS.InvokeVoidAsync("window.scrollTo", 0, 0);
        }
        catch (Exception)
        {
            // si no se puede subir no pasa nada: el aviso igual está en pantalla
        }
    }

    // POST /api/categorias/{id}/activar: vuelve a activar una categoría desactivada.
    private async Task Activar(Categoria c)
    {
        error = exito = null;
        try
        {
            await Api.Post<Categoria>($"categorias/{c.Id}/activar", null);
            await Buscar();
            exito = $"Se activó la categoría {c.Nombre}.";
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        catch (Exception)
        {
            error = "No se pudo completar la operación. Inténtalo otra vez; si el problema continúa, avisa al Administrador.";
        }
        await SubirAlAviso();
    }

    // DELETE /api/categorias/{id}: baja lógica. Si tiene productos activos el BO responde
    // "La categoria tiene productos activos; reasignelos o desactivelos primero".
    private async Task Desactivar(Categoria c)
    {
        error = exito = null;
        try
        {
            await Api.Delete($"categorias/{c.Id}");
            await Buscar();
            exito = $"Se desactivó la categoría {c.Nombre}.";
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        catch (Exception)
        {
            error = "No se pudo completar la operación. Inténtalo otra vez; si el problema continúa, avisa al Administrador.";
        }
        await SubirAlAviso();   // el botón Desactivar está en la tabla y el aviso arriba
    }
}
