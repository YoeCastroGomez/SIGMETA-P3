// Código de la página Productos (parte 1 de 1). La vista está en Productos.razor: es la misma clase (partial).
namespace SigmetaWeb.Components.Pages.Catalogo;

public partial class Productos
{
    // UI/UX: una acción visible por fila y el resto en «⋯» (menos ruido en tablas densas; mismo patrón que Ventas)
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

    // RF001: el rol sale de la sesión (MainLayout). Solo decide qué botones se ven; el permiso lo valida el BO.
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";
    private bool EsAdministrador => Rol == "Administrador";

    // ProductoForm vuelve aquí con ?guardado=nuevo|editado&ref=código · nombre para mostrar el aviso verde
    // con el patrón único (AvisoGuardado)
    [SupplyParameterFromQuery(Name = "guardado")] public string? Guardado { get; set; }
    [SupplyParameterFromQuery(Name = "ref")] public string? Referencia { get; set; }

    private List<Producto>? productos;
    private List<Categoria> categorias = new();
    private string filtroNombre = "";
    private int filtroCategoria;
    private string filtroEstado = "";
    private string ultimaRuta = "productos";
    private string? descripcion;
    private string? error;
    private string? exito;

    // Desactivar pide confirmación: primero se guarda cuál es y se muestra el cuadro
    private Producto? porDesactivar;

    private void PedirDesactivar(Producto p)
    {
        error = exito = null;
        porDesactivar = p;
    }

    private async Task ConfirmarDesactivar()
    {
        var p = porDesactivar;
        porDesactivar = null;   // cierra el cuadro
        if (p is not null)
        {
            await Desactivar(p);
        }
    }

    protected override async Task OnInitializedAsync()
    {
        try
        {
            categorias = await Api.Get<List<Categoria>>("categorias") ?? new List<Categoria>();   // para el filtro
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        exito = AvisoGuardado.Texto(Guardado, "el producto", Referencia);
        await Cargar("productos", null);
    }

    // Lo que devuelve el BuscadorProducto (Enter en el cuadro Código): buscarPorCodigo o buscarPorNombre
    private void MostrarResultadoBuscador(List<Producto> encontrados)
    {
        error = exito = null;
        productos = encontrados;
        descripcion = "Resultado del buscador por código o nombre";
    }

    // GET /api/productos?nombre=..&categoria=..&estado=.. : los filtros llenos se combinan (ProductoBO.buscar)
    private async Task Buscar()
    {
        exito = null;
        var parametros = new List<string>();
        var texto = new List<string>();
        var nombre = filtroNombre.Trim();
        if (nombre != "")
        {
            parametros.Add($"nombre={Uri.EscapeDataString(nombre)}");
            texto.Add($"nombre contiene \"{nombre}\"");
        }
        if (filtroCategoria != 0)
        {
            parametros.Add($"categoria={filtroCategoria}");
            texto.Add($"categoría {categorias.FirstOrDefault(c => c.Id == filtroCategoria)?.Nombre}");
        }
        if (filtroEstado != "")
        {
            parametros.Add($"estado={filtroEstado}");
            texto.Add(filtroEstado == "true" ? "solo activos" : "solo inactivos");
        }

        if (parametros.Count == 0)
        {
            await Cargar("productos", null);   // todos
        }
        else
        {
            await Cargar("productos?" + string.Join("&", parametros), "Filtro: " + string.Join(" · ", texto));
        }
    }

    private async Task Limpiar()
    {
        filtroNombre = filtroEstado = "";
        filtroCategoria = 0;
        exito = null;
        await Cargar("productos", null);
    }

    private async Task Cargar(string ruta, string? texto)
    {
        error = null;
        try
        {
            productos = await Api.Get<List<Producto>>(ruta) ?? new List<Producto>();
            ultimaRuta = ruta;
            descripcion = texto;
        }
        catch (ErrorApi e)
        {
            error = e.Message;
            productos ??= new List<Producto>();
        }
    }

    // DELETE /api/productos/{id}: baja lógica, el producto queda inactivo
    private async Task Desactivar(Producto p)
    {
        error = exito = null;
        try
        {
            await Api.Delete($"productos/{p.Id}");
            await Cargar(ultimaRuta, descripcion);
            exito = $"Se desactivó el producto {p.CodigoInterno} · {p.Nombre}.";
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

    // POST /api/productos/{id}/activar: vuelve a activar un producto desactivado.
    // El BO lo rechaza si su categoría está desactivada o si su código o nombre ya los usa otro producto.
    private async Task Activar(Producto p)
    {
        error = exito = null;
        try
        {
            await Api.Post<Producto>($"productos/{p.Id}/activar", null);
            await Cargar(ultimaRuta, descripcion);
            exito = $"Se activó el producto {p.CodigoInterno} · {p.Nombre}.";
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
}
