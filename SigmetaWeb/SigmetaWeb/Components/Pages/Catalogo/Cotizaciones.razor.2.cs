// Código de la página Cotizaciones (parte 2 de 2). La vista está en Cotizaciones.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Ventas;
using SigmetaWeb.Components.Pages.Clientes;

namespace SigmetaWeb.Components.Pages.Catalogo;

public partial class Cotizaciones
{

    // POST /api/cotizaciones/{id}/aceptar | rechazar  (enviar se hace desde CotizacionEnviar.razor)
    private async Task Accion(CotizacionDatos c, string accion, string mensaje, string? textoAccion = null, string? ruta = null)
    {
        CerrarAviso();
        trabajando = true;
        try
        {
            await Api.Post<CotizacionDatos>($"cotizaciones/{c.Id}/{accion}", null);
            MostrarExito(mensaje, textoAccion, ruta);
            await Cargar(forzar: true);
        }
        catch (ErrorApi e)
        {
            MostrarError(e.Message);   // por ejemplo "La cotización venció el 2026-11-10"
        }
        finally
        {
            trabajando = false;
        }
    }

    private Task AbrirAnulacion(CotizacionDatos c)
    {
        CerrarAviso();
        anulando = c;
        return Task.CompletedTask;
    }

    // POST /api/cotizaciones/{id}/anular {"motivo": ...}
    private async Task ConfirmarAnulacion(string motivo)
    {
        if (anulando is null) return;
        trabajando = true;
        try
        {
            await Api.Post<object>($"cotizaciones/{anulando.Id}/anular", new AnulacionDatos { Motivo = motivo });
            MostrarExito($"Se anuló la cotización {anulando.Numero}.");
            anulando = null;
            await Cargar(forzar: true);
        }
        catch (ErrorApi e)
        {
            anulando = null;
            MostrarError(e.Message);   // por ejemplo "No se puede anular una cotización aceptada"
        }
        finally
        {
            trabajando = false;
        }
    }
}
