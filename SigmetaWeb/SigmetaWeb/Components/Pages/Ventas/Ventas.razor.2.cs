// Código de la página Ventas (continuación de Ventas.razor.cs). La vista está en Ventas.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Clientes;
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class Ventas
{

    private async Task Limpiar()
    {
        var recargar = filtroCliente > 0;
        filtroCliente = 0;
        situacion = "";
        busqueda = "";
        paginaActual = 1;
        if (recargar) await Cargar();
        SincronizarUrl();
    }

    // ---------------- Anular (RF008) ----------------

    private Task AbrirAnulacion(VentaDatos v)
    {
        error = null;
        exito = null;
        anulando = v;
        return Task.CompletedTask;
    }

    // POST /api/ventas/{id}/anular {"motivo": ...}
    private async Task ConfirmarAnulacion(string motivo)
    {
        if (anulando is null) return;
        trabajando = true;
        try
        {
            await Api.Post<object>($"ventas/{anulando.Id}/anular", new AnulacionDatos { Motivo = motivo });
            exito = $"Se anuló la venta {anulando.Numero}.";
            anulando = null;
            await Cargar();
        }
        catch (ErrorApi e)
        {
            anulando = null;
            error = e.Message;   // por ejemplo "No se puede anular una venta con comprobantes vigentes"
        }
        finally
        {
            trabajando = false;
        }
    }
}
