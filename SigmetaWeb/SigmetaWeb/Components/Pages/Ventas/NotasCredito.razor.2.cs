// Código de la página NotasCredito (continuación de NotasCredito.razor.cs). La vista está en NotasCredito.razor: es la misma clase (partial).
using System.Text.Json;
using SigmetaWeb.Components.Pages.Almacen;
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class NotasCredito
{

    private void CambiarEstado(string estado)
    {
        filtroEstado = estado;
        paginaActual = 1;
    }

    private async Task VerDetalle(FilaNota f)
    {
        if (detalleDe == f.Clave)
        {
            detalleDe = null;
            return;
        }
        if (f.Nota is null)
        {
            detalleDe = f.Clave;   // los productos de una nota en trámite ya vienen en sus datos
            return;
        }
        try
        {
            detalle = await Api.Get<List<NotaCreditoLinea>>($"notas-credito/{f.Nota.Id}/lineas") ?? new();
            detalleDe = f.Clave;
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

    private Task PedirDescartar(FilaNota f)
    {
        error = exito = null;
        porDescartar = f;
        return Task.CompletedTask;
    }

    // POST /api/solicitudes/{id}/descartar: el Cajero ya no registrará esa nota
    private async Task ConfirmarDescartar()
    {
        var f = porDescartar;
        porDescartar = null;
        if (f?.Solicitud is null) return;
        trabajando = true;
        try
        {
            await Api.Post<object>($"solicitudes/{f.Solicitud.Id}/descartar", null);
            await Buscar();
            exito = $"Se descartó la nota en trámite sobre {f.Comprobante}.";
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        finally
        {
            trabajando = false;
        }
    }
}
