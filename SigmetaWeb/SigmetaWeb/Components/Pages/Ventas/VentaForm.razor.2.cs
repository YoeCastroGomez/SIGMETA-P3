// Código de la página VentaForm (parte 2 de 2). La vista está en VentaForm.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Catalogo;
using SigmetaWeb.Components.Pages.Clientes;
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class VentaForm
{

    // Lo que falta VENDER de la línea de la orden (pedido - ya vendido en ventas vigentes): no se puede vender más.
    // Es el mismo límite que aplica VentaBOImpl (cantidadComprometida), no lo despachado.
    private decimal PendienteEnOrden(LineaDatos l) =>
        ordenElegida?.Lineas.FirstOrDefault(o => o.IdLinea == l.IdDetalleOrden) is { } lo
            ? Math.Max(0, lo.Cantidad - lo.CantidadVendida)
            : decimal.MaxValue;

    internal Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (venta is null || !Editable)
        {
            return e;
        }
        if (desdeOrden)
        {
            if (EsNuevo && venta.IdOrden <= 0)
            {
                e["orden"] = "Elija la orden de compra del cliente.";
            }
        }
        else if (venta.IdCliente <= 0)
        {
            e["cliente"] = "Seleccione el cliente.";
        }
        if (venta.FechaEmision is null)
        {
            e["fecha"] = "La fecha de emisión es obligatoria.";
        }
        else if (EsNuevo && (venta.FechaEmision > Hoy || venta.FechaEmision < FechaMinima))
        {
            // misma regla que VentaBOImpl: ni futura ni con más de 7 días de atraso
            e["fecha"] = $"La fecha debe estar entre {FechaMinima:dd/MM/yyyy} y hoy.";
        }
        if (venta.CondicionPago == "CREDITO")
        {
            if (venta.PlazoCreditoDias < 0)
            {
                e["plazo"] = "El plazo no puede ser negativo.";
            }
            else if (EsNuevo && ClienteActual is { } c && venta.PlazoCreditoDias > c.PlazoCreditoDias)
            {
                e["plazo"] = $"Máximo {c.PlazoCreditoDias} días (plazo autorizado del cliente).";
            }
        }
        if (venta.Lineas.Count == 0)
        {
            e["lineas"] = desdeOrden ? "La orden elegida no tiene nada pendiente de vender." : "Agregue al menos un producto con el buscador.";
        }
        foreach (var l in venta.Lineas)
        {
            var n = v.Clave(l);
            ValidacionEnVivo.ErroresLinea(e, n, l.Cantidad, l.PrecioUnitario, l.Descuento);
            if (l.CantidadNoEntera && !e.ContainsKey(n + "cantidad"))
            {
                e[n + "cantidad"] = "Solo cantidades enteras.";
            }
            // en una venta nueva desde una orden, no más de lo que falta entregar de esa línea
            if (EsNuevo && desdeOrden && !e.ContainsKey(n + "cantidad") && l.Cantidad > PendienteEnOrden(l))
            {
                e[n + "cantidad"] = $"No puede pasar de lo pendiente en la orden ({PendienteEnOrden(l):0.###}).";
            }
        }
        return e;
    }

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
            for (var i = 0; i < venta!.Lineas.Count; i++)
            {
                venta.Lineas[i].NumeroLinea = i + 1;
            }

            if (EsNuevo)
            {
                var nueva = await Api.Post<VentaDatos>("ventas", venta);           // POST /api/ventas
                // siguiente paso del flujo: emitir su comprobante (allí se avisa que se registró)
                Nav.NavigateTo(nueva is not null
                    ? $"/ventas/{nueva.Id}/comprobante?nueva=true&volver={Volver.Parametro(VolverA)}"
                    : VolverA);
            }
            else
            {
                var guardada = await Api.Put<VentaDatos>($"ventas/{Id}", venta);   // PUT /api/ventas/{Id}
                Nav.NavigateTo(Volver.ConAviso(VolverA, AvisoGuardado.Editado, guardada?.Numero ?? venta.Numero));
            }
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "Stock insuficiente para ..." o "La venta supera el límite de crédito del cliente"
            if (e.Message.Contains("requiere autorización del Administrador") || e.Message.Contains("en espera del Administrador"))
            {
                // RF014: no es un error: la línea se marca y el botón pasa a «Solicitar autorización»
                error = null;
                await RevisarCosto();
                requiereAutorizacion = true;
            }
        }
        finally
        {
            guardando = false;
        }
    }

    // RF014: pide la autorización de ESTA venta con sus datos (mismo cuerpo que guardarla)
    private async Task<SolicitudDatos?> SolicitarAutorizacion()
    {
        error = null;
        for (var i = 0; i < venta!.Lineas.Count; i++)
        {
            venta.Lineas[i].NumeroLinea = i + 1;
        }
        try
        {
            // RF014: se guarda también la venta tal como está en pantalla, para retomarla sin pedir otra autorización
            venta.DatosOperacion = null;
            venta.DatosOperacion = System.Text.Json.JsonSerializer.Serialize(venta,
                new System.Text.Json.JsonSerializerOptions(System.Text.Json.JsonSerializerDefaults.Web));
            return await Api.Post<SolicitudDatos>("ventas/solicitar-autorizacion", venta);
        }
        catch (ErrorApi e) when (e.Message.Contains("no hace falta autorización"))
        {
            // el Vendedor corrigió el descuento: ya puede guardar sin autorización
            requiereAutorizacion = false;
            error = "Ya ninguna línea queda bajo el costo: puede guardar.";
            return null;
        }
        finally
        {
            venta.DatosOperacion = null;   // no viaja al registrar la venta
        }
    }
}
