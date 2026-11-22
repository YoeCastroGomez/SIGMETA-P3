// Código de la página CompraForm (parte 2 de 2). La vista está en CompraForm.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Catalogo;
using SigmetaWeb.Components.Pages.Compras;

namespace SigmetaWeb.Components.Pages.Compras;

public partial class CompraForm
{

    private async Task Guardar()
    {
        error = null;
        if (!v.Intentar(Errores()))
        {
            error = "Revise los campos marcados en rojo.";
            return;
        }
        guardando = true;
        try
        {
            var pedido = new SolicitudCompraRest
            {
                Numero = EsNuevo ? "" : numero,
                IdProveedor = proveedorId,
                FechaEmision = fecha.ToString("yyyy-MM-dd"),
                FechaRecepcionEstimada = fechaEstimada?.ToString("yyyy-MM-dd"),
                Observaciones = observaciones,
                Detalles = lineas.Select((l, i) => new SolicitudDetalleCompraRest
                {
                    NumeroLinea = i + 1,
                    IdProducto = l.IdProducto,
                    Cantidad = l.Cantidad,
                    PrecioUnitario = l.PrecioUnitario,
                    Descuento = l.Descuento,
                    UnidadCompra = l.UnidadCompra,
                    FactorConversion = l.FactorConversion
                }).ToList()
            };
            var guardada = EsNuevo
                ? await Api.Post<CompraRest>("compras", pedido)
                : await Api.Put<CompraRest>($"compras/{Id}", pedido);
            Nav.NavigateTo(AvisoGuardado.Ruta("/compras",
                EsNuevo ? AvisoGuardado.Nuevo : AvisoGuardado.Editado,
                guardada?.Numero ?? numero));
        }
        catch (ErrorApi e) { error = e.Message; }
        finally { guardando = false; }
    }
}
