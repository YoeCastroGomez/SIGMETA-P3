// Código de la página NotaCreditoForm (continuación de NotaCreditoForm.razor.cs). La vista está en NotaCreditoForm.razor: es la misma clase (partial).
using System.Text.Json;
using SigmetaWeb.Components.Pages.Almacen;
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class NotaCreditoForm
{

    private async Task CargarLineas()
    {
        lineas = new();
        error = null;
        if (idComprobante == 0) return;
        try
        {
            lineas = await Api.Get<List<LineaDevolvible>>($"notas-credito/comprobantes/{idComprobante}/lineas") ?? new();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }

    private void DevolverTodo()
    {
        foreach (var l in lineas.Where(l => l.Devolvible > 0))
        {
            l.Devolver = l.Devolvible;
        }
    }

    private void Limpiar()
    {
        foreach (var l in lineas)
        {
            l.Devolver = 0;
        }
    }

    private async Task Guardar()
    {
        intento = true;
        error = null;
        if (Errores().Count > 0 || lineas.Any(l => Problema(l) is not null))
        {
            error = "Revise los campos marcados en rojo.";
            return;
        }
        if (!lineas.Any(l => l.Devolver > 0))
        {
            error = "Escriba la cantidad que se devuelve en al menos un producto.";
            return;
        }
        guardando = true;
        try
        {
            var guardada = await Api.Post<NotaGuardada>("notas-credito", Cuerpo());
            Nav.NavigateTo(AvisoGuardado.Ruta("/notas-credito", AvisoGuardado.Nuevo, guardada?.Nota));
        }
        catch (ErrorApi e)
        {
            // por ejemplo la caja cerrada: no se registró nada y la autorización sigue aprobada
            error = e.Message;
            if (e.Message.Contains("caja está cerrada", StringComparison.OrdinalIgnoreCase))
            {
                cajaCerrada = true;
            }
        }
        finally
        {
            guardando = false;
        }
    }

    private class NotaGuardada
    {
        public int Id { get; set; }
        public string? Nota { get; set; }
    }
}

// RF014: la nota tal como la preparó el Cajero, guardada en su solicitud (datosOperacion) para retomarla.
// También la usa la lista de notas de crédito para mostrar las que están en trámite.
public class DatosNotaGuardada
{
    public int IdComprobante { get; set; }
    public string? Comprobante { get; set; }
    public string? Tipo { get; set; }
    public string? FechaEmision { get; set; }
    public string? Cliente { get; set; }
    public string? Documento { get; set; }
    public string? Venta { get; set; }
    public string? CondicionPago { get; set; }
    public double TotalComprobante { get; set; }
    public string? Motivo { get; set; }
    public double Total { get; set; }
    public List<LineaGuardada> Lineas { get; set; } = new();
}

public class LineaGuardada
{
    public int IdLinea { get; set; }
    public double Cantidad { get; set; }
    public string? Producto { get; set; }
}
