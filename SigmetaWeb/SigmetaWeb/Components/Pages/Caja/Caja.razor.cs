// Código de la página Caja (parte 1 de 1). La vista está en Caja.razor: es la misma clase (partial).
namespace SigmetaWeb.Components.Pages.Caja;

public partial class Caja
{
    private int paginaActual = 1;
    private int tamanoPagina = 10;
    private IEnumerable<T> Paginar<T>(IEnumerable<T> registros, int pagina, int tamano)
    {
        if (tamano == 0) return registros;
        int total = registros.Count();
        int segura = Math.Clamp(pagina, 1, Math.Max(1, (total + tamano - 1) / tamano));
        return registros.Skip((segura - 1) * tamano).Take(tamano);
    }

    private CajaDatos? caja;
    private List<VentaPorCobrar> porCobrar = new();
    private decimal? montoInicial;
    private bool cargando = true;
    private bool guardando;
    private string? error;
    private string? exito;

    // Aviso de éxito con el patrón único (AvisoGuardado): el cobro al contado vuelve aquí con ?guardado=cobrado&ref=…
    [SupplyParameterFromQuery(Name = "guardado")] public string? Guardado { get; set; }
    [SupplyParameterFromQuery(Name = "ref")] public string? Referencia { get; set; }

    protected override async Task OnInitializedAsync()
    {
        exito = AvisoGuardado.Texto(Guardado, "la venta", Referencia);
        await Cargar();
        cargando = false;
    }

    private async Task Cargar()
    {
        try
        {
            caja = await Api.Get<CajaDatos>("cajas/abierta");    // null si el cajero no tiene caja abierta
            porCobrar = caja is null
                ? new List<VentaPorCobrar>()
                : await Api.Get<List<VentaPorCobrar>>("cajas/ventas-por-cobrar") ?? new List<VentaPorCobrar>();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }

    // ---------------- Validación en pantalla (RNF004) ----------------
    private readonly ValidacionEnVivo v = new();

    private Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (montoInicial is null)
        {
            e["montoInicial"] = "Ingrese el monto inicial (puede ser 0).";
        }
        else if (montoInicial < 0)
        {
            e["montoInicial"] = "El monto inicial no puede ser negativo.";
        }
        else if (ValidacionEnVivo.MasDecimales(montoInicial.Value, 2))
        {
            e["montoInicial"] = "Máximo 2 decimales.";
        }
        return e;
    }

    // POST /api/cajas: el BO valida el monto (no negativo) y que el cajero no tenga otra caja abierta
    private async Task Abrir()
    {
        error = null;
        exito = null;
        if (!v.Intentar(Errores()))
        {
            error = "Revise los campos marcados en rojo.";
            return;
        }
        guardando = true;
        try
        {
            var nueva = await Api.Post<CajaDatos>("cajas", new { montoInicial });
            exito = $"Se abrió la caja N.º {nueva?.Id} con S/ {montoInicial:N2}.";
            await Cargar();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        guardando = false;
    }
}
