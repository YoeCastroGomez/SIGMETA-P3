// Código de la página Cierres (parte 1 de 1). La vista está en Cierres.razor: es la misma clase (partial).
namespace SigmetaWeb.Components.Pages.Caja;

public partial class Cierres
{
    // UI/UX: una acción visible por fila y el resto en «⋯» (mismo patrón en todas las listas)
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

    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";

    private List<CierreDatos>? cierres;
    private DateOnly? fDesde, fHasta;
    private CierreDatos? cierreAReabrir;
    private string? motivo;
    private CierreDatos? cierreACorregir;
    private decimal? declaradoCorregido;
    private bool guardando;
    private string? error;
    private string? exito;

    protected override async Task OnInitializedAsync()
    {
        await Cargar();
    }

    private async Task Buscar()
    {
        exito = null;
        await Cargar();
    }

    private async Task Limpiar()
    {
        fDesde = fHasta = null;
        exito = null;
        await Cargar();
    }

    private async Task Cargar()
    {
        error = null;
        try
        {
            var ruta = "cierres";
            if (fDesde is not null || fHasta is not null)
            {
                // el Java valida que estén las dos y que desde no sea posterior a hasta
                ruta += $"?desde={fDesde:yyyy-MM-dd}&hasta={fHasta:yyyy-MM-dd}";
            }
            cierres = await Api.Get<List<CierreDatos>>(ruta) ?? new();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
            cierres ??= new();
        }
    }

    // ---------------- Validación en pantalla (RNF004) ----------------
    // Reabrir pide motivo; corregir, un monto no negativo (igual que CierreCajaBO). Solo se valida el diálogo abierto.
    private readonly ValidacionEnVivo v = new();

    private Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (cierreAReabrir is not null && string.IsNullOrWhiteSpace(motivo))
        {
            e["motivo"] = "Escriba el motivo de la reapertura.";
        }
        if (cierreACorregir is not null)
        {
            if (declaradoCorregido is null)
            {
                e["declarado"] = "Ingrese el monto declarado corregido.";
            }
            else if (declaradoCorregido < 0)
            {
                e["declarado"] = "El monto declarado no puede ser negativo.";
            }
            else if (ValidacionEnVivo.MasDecimales(declaradoCorregido.Value, 2))
            {
                e["declarado"] = "Máximo 2 decimales.";
            }
        }
        return e;
    }

    private void Reabrir(CierreDatos c)
    {
        exito = null;
        v.Reiniciar();
        cierreACorregir = null;
        cierreAReabrir = c;
        motivo = null;
    }

    private void Corregir(CierreDatos c)
    {
        exito = null;
        v.Reiniciar();
        cierreAReabrir = null;
        cierreACorregir = c;
        declaradoCorregido = c.MontoDeclarado;
    }

    private async Task ConfirmarReapertura()
    {
        if (cierreAReabrir is null) return;
        error = null;
        if (!v.Intentar(Errores()))
        {
            return;
        }
        guardando = true;
        try
        {
            await Api.Post<CierreDatos>($"cierres/{cierreAReabrir.Id}/reabrir", new { motivo });
            exito = $"Se reabrió la caja N.º {cierreAReabrir.IdCaja}. Su cierre quedó como revertido.";
            cierreAReabrir = null;
            await Cargar();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        guardando = false;
    }

    private async Task GuardarCorreccion()
    {
        if (cierreACorregir is null) return;
        error = null;
        if (!v.Intentar(Errores()))
        {
            return;
        }
        guardando = true;
        try
        {
            var corregido = await Api.Put<CierreDatos>($"cierres/{cierreACorregir.Id}", new { montoDeclarado = declaradoCorregido });
            exito = $"Se corrigió el cierre de la caja N.º {cierreACorregir.IdCaja}. Nueva diferencia: S/ {corregido?.Diferencia:N2}.";
            cierreACorregir = null;
            await Cargar();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        guardando = false;
    }
}
