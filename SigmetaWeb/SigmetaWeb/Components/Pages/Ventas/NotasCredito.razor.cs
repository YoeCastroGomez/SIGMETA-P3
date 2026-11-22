// Código de la página NotasCredito (parte 1 de 1). La vista está en NotasCredito.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Almacen;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class NotasCredito
{
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";
    private bool EsCajero => Rol == "Cajero";
    // RF013: modifican y anulan la nota el Cajero y el Administrador (igual que ComprobanteBOImpl)
    private bool PuedeModificar => Rol == "Cajero" || Rol == "Administrador";

    [SupplyParameterFromQuery(Name = "guardado")] public string? Guardado { get; set; }
    [SupplyParameterFromQuery(Name = "ref")] public string? Referencia { get; set; }

    private List<NotaCreditoDatos>? notas;
    private List<NotaCreditoLinea> detalle = new();
    private int? detalleDe;
    private DateTime? desde;
    private DateTime? hasta;
    private string texto = "";
    private string filtroEstado = "";
    private NotaCreditoDatos? porAnular;
    private NotaCreditoDatos? porEditar;
    private bool cargando;
    private bool trabajando;
    private string? error;
    private string? exito;
    private CancellationTokenSource? esperaBusqueda;
    private int paginaActual = 1;
    private int tamanoPagina = 10;

    private bool HayFiltros => desde is not null || hasta is not null || !string.IsNullOrWhiteSpace(texto);

    // Lo llama AccionesFila: la acción corre en esta pantalla para que se redibuje (detalle, ventanas de motivo y anular)
    private Task Ejecutar(AccionFila a) => a.Hacer?.Invoke() ?? Task.CompletedTask;

    // Menú «⋯»: modificar el motivo y anular (Cajero o Administrador, solo vigentes)
    private List<AccionFila> Secundarias(NotaCreditoDatos n) =>
        n.Estado == "ANULADO" || !PuedeModificar
            ? new()
            : new()
            {
                new("Editar motivo", Hacer: () => PedirEditar(n)),
                new("Anular", Hacer: () => PedirAnular(n), Peligro: true)
            };

    // El Java busca por texto: se espera a que se deje de escribir para no consultar en cada tecla
    private async Task BuscarAlEscribir()
    {
        esperaBusqueda?.Cancel();
        var espera = esperaBusqueda = new CancellationTokenSource();
        try
        {
            await Task.Delay(350, espera.Token);
        }
        catch (TaskCanceledException)
        {
            return;
        }
        await Buscar();
    }

    private List<NotaCreditoDatos> Filtradas => (notas ?? new())
        .Where(n => filtroEstado == "" || (filtroEstado == "ANULADO") == (n.Estado == "ANULADO"))
        .ToList();

    private IReadOnlyList<OpcionChip> Chips => new List<OpcionChip>
    {
        new("", "Todas", notas?.Count),
        new("VIGENTE", "Vigentes", notas?.Count(n => n.Estado != "ANULADO"), "verde"),
        new("ANULADO", "Anuladas", notas?.Count(n => n.Estado == "ANULADO"), "rojo")
    };

    private IEnumerable<NotaCreditoDatos> Pagina(List<NotaCreditoDatos> lista)
    {
        if (tamanoPagina == 0) return lista;
        var total = Math.Max(1, (int)Math.Ceiling((double)lista.Count / tamanoPagina));
        return lista.Skip((Math.Clamp(paginaActual, 1, total) - 1) * tamanoPagina).Take(tamanoPagina);
    }

    protected override async Task OnInitializedAsync()
    {
        await Buscar();
        if (Guardado == AvisoGuardado.Nuevo)
        {
            exito = $"Se registró la nota de crédito {Referencia}.";
        }
    }

    private async Task Buscar()
    {
        error = null;
        if (ValidacionEnVivo.ErrorRango(desde, hasta) is not null)
        {
            return;
        }
        cargando = true;
        try
        {
            notas = await Api.Get<List<NotaCreditoDatos>>(
                $"notas-credito?desde={Formato.Iso(desde)}&hasta={Formato.Iso(hasta)}&texto={Uri.EscapeDataString(texto.Trim())}") ?? new();
            paginaActual = 1;
            detalleDe = null;
        }
        catch (ErrorApi e)
        {
            error = e.Message;
            notas ??= new();
        }
        finally
        {
            cargando = false;
        }
    }

    private async Task Limpiar()
    {
        desde = hasta = null;
        texto = "";
        filtroEstado = "";
        exito = null;
        await Buscar();
    }

    private void CambiarEstado(string estado)
    {
        filtroEstado = estado;
        paginaActual = 1;
    }

    private async Task VerDetalle(NotaCreditoDatos n)
    {
        if (detalleDe == n.Id)
        {
            detalleDe = null;
            return;
        }
        try
        {
            detalle = await Api.Get<List<NotaCreditoLinea>>($"notas-credito/{n.Id}/lineas") ?? new();
            detalleDe = n.Id;
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }

    private Task PedirEditar(NotaCreditoDatos n)
    {
        error = exito = null;
        porEditar = n;
        return Task.CompletedTask;
    }

    // PUT /api/notas-credito/{id} {"motivo"}
    private async Task ConfirmarEditar(string motivo)
    {
        var n = porEditar;
        if (n is null) return;
        trabajando = true;
        try
        {
            await Api.Put<object>($"notas-credito/{n.Id}", new { motivo });
            porEditar = null;
            await Buscar();
            exito = $"Se modificó el motivo de la nota de crédito {n.Nota}.";
        }
        catch (ErrorApi e)
        {
            porEditar = null;
            error = e.Message;   // por ejemplo "No se puede modificar una nota de crédito anulada"
        }
        finally
        {
            trabajando = false;
        }
    }

    private Task PedirAnular(NotaCreditoDatos n)
    {
        error = exito = null;
        porAnular = n;
        return Task.CompletedTask;
    }

    private async Task ConfirmarAnular(string motivo)
    {
        var n = porAnular;
        if (n is null) return;
        trabajando = true;
        try
        {
            await Api.Post<object>($"notas-credito/{n.Id}/anular", new { motivo });
            porAnular = null;
            await Buscar();
            exito = AvisoGuardado.Texto(AvisoGuardado.Anulado, "la nota de crédito", n.Nota);
        }
        catch (ErrorApi e)
        {
            porAnular = null;
            error = e.Message;   // por ejemplo "Stock insuficiente para revertir la nota de crédito..."
        }
        finally
        {
            trabajando = false;
        }
    }
}
