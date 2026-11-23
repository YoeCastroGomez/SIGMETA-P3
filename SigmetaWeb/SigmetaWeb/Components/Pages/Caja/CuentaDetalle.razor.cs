// Código de la página CuentaDetalle (parte 1 de 1). La vista está en CuentaDetalle.razor: es la misma clase (partial).
namespace SigmetaWeb.Components.Pages.Caja;

public partial class CuentaDetalle
{
    // UI/UX: una acción visible por fila y el resto en «⋯» (mismo patrón en todas las listas)
    private Task Ejecutar(AccionFila a) => a.Hacer?.Invoke() ?? Task.CompletedTask;

    private int paginaActual = 1;
    private int tamanoPagina = 10;
    private IEnumerable<T> Paginar<T>(IEnumerable<T> registros, int pagina, int tamano)
    {
        if (tamano == 0) return registros;
        int total = registros.Count();
        int segura = Math.Clamp(pagina, 1, Math.Max(1, (total + tamano - 1) / tamano));
        return registros.Skip((segura - 1) * tamano).Take(tamano);
    }

    [Parameter] public int Id { get; set; }
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";

    private CuentaDatos? cuenta;
    private List<NotaCreditoCuenta>? notas;
    private List<EventoCuenta>? historial;
    private string? error;
    private string? exito;

    // Aviso de éxito con el patrón único (AvisoGuardado): el formulario del cobro vuelve aquí con ?guardado=…&ref=…
    [SupplyParameterFromQuery(Name = "guardado")] public string? Guardado { get; set; }
    [SupplyParameterFromQuery(Name = "ref")] public string? Referencia { get; set; }
    private bool avisoLeido;

    protected override async Task OnParametersSetAsync()
    {
        if (!avisoLeido)
        {
            avisoLeido = true;
            exito = AvisoGuardado.Texto(Guardado, "el cobro", Referencia);
        }
        await Cargar();
    }

    private async Task Cargar()
    {
        try
        {
            cuenta = await Api.Get<CuentaDatos>($"cuentas-por-cobrar/{Id}");
            notas = await Api.Get<List<NotaCreditoCuenta>>($"cuentas-por-cobrar/{Id}/notas-credito") ?? new();
            historial = await Api.Get<List<EventoCuenta>>($"cuentas-por-cobrar/{Id}/historial") ?? new();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }

    // RF009 + RNF001: primero se pide el motivo (ModalMotivo); DELETE /api/cobros/{id}?motivo=...
    private CobroDatos? porAnular;
    private bool anulando;

    private Task Anular(CobroDatos c)
    {
        porAnular = c;
        return Task.CompletedTask;
    }

    private async Task ConfirmarAnular(string motivo)
    {
        if (porAnular is null) return;
        var c = porAnular;
        error = null;
        exito = null;
        anulando = true;
        try
        {
            await Api.Delete($"cobros/{c.Id}?motivo={Uri.EscapeDataString(motivo)}");
            porAnular = null;
            exito = $"Se anuló el cobro de S/ {c.Monto:N2}. El monto volvió al saldo y la anulación quedó en el historial.";
            await Cargar();
        }
        catch (ErrorApi e)
        {
            porAnular = null;
            error = e.Message;
        }
        finally
        {
            anulando = false;
        }
    }
}
