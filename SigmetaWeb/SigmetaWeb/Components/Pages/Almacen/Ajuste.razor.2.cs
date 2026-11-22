// Código de la página Ajuste (parte 2 de 2). La vista está en Ajuste.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Almacen;

public partial class Ajuste
{

    // ---------------- autorización y registro ----------------

    // RF014: pide la autorización de ESTE ajuste (mismo cuerpo que registrarlo) y guarda la pantalla para retomarla
    private Task<SolicitudDatos?> Solicitar()
    {
        intento = true;
        error = null;
        var p = Producto!;
        var datos = new DatosAjusteGuardado
        {
            IdProducto = idProducto, CantidadContada = contado ?? 0, Motivo = motivo.Trim(),
            Codigo = p.CodigoInterno, Producto = p.Nombre, Unidad = TextosAlmacen.Unidad(p.UnidadVenta), StockSistema = p.StockActual
        };
        return Api.Post<SolicitudDatos>("almacen/ajustes/solicitar", new
        {
            idProducto, cantidadContada = contado, motivo = motivo.Trim(),
            datosOperacion = System.Text.Json.JsonSerializer.Serialize(datos, Json)
        });
    }

    private async Task Guardar()
    {
        intento = true;
        error = null;
        if (Errores().Count > 0) return;
        guardando = true;
        try
        {
            await Api.Post<object>("almacen/ajustes", new { idProducto, cantidadContada = contado, motivo = motivo.Trim() });
            Nav.NavigateTo(AvisoGuardado.Ruta("/almacen/movimientos", "ajuste", $"{Producto!.CodigoInterno} · {Producto.Nombre}"));
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        finally
        {
            guardando = false;
        }
    }
}
