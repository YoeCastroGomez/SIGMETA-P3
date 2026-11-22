// Código de la página VentaForm (continuación de VentaForm.razor.cs). La vista está en VentaForm.razor: es la misma clase (partial).
//
// RF008 · Registrar o modificar una venta (Vendedor). PATRÓN UX de formularios (ver Shared/ComponentesFlujo.cs):
//   - Secciones numeradas en el orden en que se llenan: 1 Origen y cliente, 2 Productos, 3 Condiciones.
//   - Resumen fijo a la derecha: totales, LISTA DE LO QUE FALTA y el botón Guardar, que se bloquea hasta que
//     todo esté completo (y dice qué falta).
//   - Información útil al decidir: línea de crédito del cliente (y "Crédito" bloqueado si no tiene),
//     stock en almacén de cada producto, lo pendiente en la orden.
//   - Al guardar va al siguiente paso (emitir el comprobante) o vuelve a donde estaba, con sus filtros.
// Nueva: POST /api/ventas. Desde una orden (?orden=ID o eligiéndola): GET /api/ordenes/{id} y cada línea de la venta
//        apunta a su línea de la orden (idDetalleOrden); directa: productos con el BuscadorProducto de RF005.
// Editar: GET /api/ventas/{Id} y PUT /api/ventas/{Id} (solo REGISTRADA sin comprobante, despacho ni cobro en caja;
//        la condición de pago y la orden no cambian).
// Stock disponible, línea de crédito, límite, plazo, correlativo, totales y la cuenta por cobrar los valida el BO;
// sus mensajes salen en el aviso rojo.
using SigmetaWeb.Components.Pages.Catalogo;
using SigmetaWeb.Components.Pages.Clientes;
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class VentaForm
{

    // GET /api/ordenes/{id}: el cliente y las líneas de la venta salen de la orden
    private async Task ElegirOrden(int id)
    {
        venta!.IdOrden = id;
        venta.Lineas.Clear();
        ordenElegida = null;
        if (id <= 0) return;
        try
        {
            ordenElegida = await Api.Get<OrdenDatos>($"ordenes/{id}");
            if (ordenElegida is null) return;
            venta.IdCliente = ordenElegida.IdCliente;
            venta.Cliente = ordenElegida.Cliente;
            venta.DocumentoCliente = ordenElegida.DocumentoCliente;
            venta.NumeroOrden = ordenElegida.Numero;
            foreach (var lo in ordenElegida.Lineas)
            {
                // se propone lo que falta VENDER (pedido - ya vendido en ventas vigentes), el mismo limite del BO
                var cantidad = lo.Cantidad - lo.CantidadVendida;
                if (cantidad <= 0) continue;
                venta.Lineas.Add(new LineaDatos
                {
                    IdProducto = lo.IdProducto,
                    Producto = lo.Producto,
                    CodigoProducto = lo.CodigoProducto,
                    UnidadVenta = lo.UnidadVenta,
                    Cantidad = cantidad,
                    PrecioUnitario = lo.PrecioUnitario,
                    // el descuento de la línea de la orden en proporción a la cantidad propuesta
                    Descuento = lo.Cantidad > 0 ? Math.Round(lo.Descuento * cantidad / lo.Cantidad, 2) : 0,
                    IdDetalleOrden = lo.IdLinea
                });
            }
            AjustarCondicion();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }

    private decimal CantidadEnOrden(LineaDatos l) =>
        ordenElegida?.Lineas.FirstOrDefault(o => o.IdLinea == l.IdDetalleOrden)?.Cantidad ?? 0;

    // Lo llama el BuscadorProducto con el producto elegido (venta directa). Si ya estaba, suma 1 a su cantidad.
    private void AgregarProducto(Producto p)
    {
        stock[p.Id] = p.StockActual;
        var existente = venta!.Lineas.FirstOrDefault(l => l.IdProducto == p.Id && l.IdDetalleOrden == 0);
        if (existente is not null)
        {
            existente.Cantidad += 1;
            return;
        }
        venta.Lineas.Add(new LineaDatos
        {
            IdProducto = p.Id,
            Producto = p.Nombre,
            CodigoProducto = p.CodigoInterno,
            UnidadVenta = p.UnidadVenta,
            Cantidad = 1,
            PrecioUnitario = p.PrecioVenta,
            Descuento = 0
        });
    }

    private void Quitar(LineaDatos l) => venta!.Lineas.Remove(l);

    // Avisos de una línea (cantidad y descuento) en una sola frase, para la fila de avisos de la tabla
    private string? AvisoLinea(LineaDatos l)
    {
        var e = Errores();
        var n = v.Clave(l);
        var partes = new List<string>();
        if (v.Error(n + "cantidad", e) is { } c) partes.Add("Cantidad: " + c);
        if (v.Error(n + "descuento", e, n + "cantidad", n + "precio") is { } d) partes.Add("Descuento: " + d);
        // RF005/RF014: precio efectivo bajo el costo (lo calcula el BO con /api/ventas/revisar-costo)
        var indice = venta?.Lineas.IndexOf(l) ?? -1;
        if (bajoCosto.FirstOrDefault(b => b.NumeroLinea == indice + 1) is { } bc)
        {
            partes.Add($"Queda en S/ {bc.PrecioSinIgv:N2} sin IGV, bajo su costo (S/ {bc.Costo:N2}): necesita autorización");
        }
        return partes.Count == 0 ? null : string.Join("  ·  ", partes);
    }

    // ---------------- RF005/RF014: precio bajo el costo, antes de registrar ----------------
    private List<LineaBajoCosto> bajoCosto = new();
    private string firmaCosto = "";
    private CancellationTokenSource? esperaCosto;

    private sealed class LineaBajoCosto
    {
        public int NumeroLinea { get; set; }
        public string? Producto { get; set; }
        public decimal PrecioSinIgv { get; set; }
        public decimal Costo { get; set; }
    }

    // lo que decide el precio de cada línea: si cambia, se vuelve a revisar contra el costo
    private string FirmaCosto() => venta is null ? "" : string.Join("|", venta.Lineas.Select(l =>
        $"{l.IdProducto}:{l.Cantidad}:{l.Descuento}:{l.IdDetalleOrden}")) + $"#{venta.IdOrden}";

    protected override async Task OnAfterRenderAsync(bool firstRender)
    {
        if (venta is null || !Editable || BloqueadaPorAutorizacion) return;
        var firma = FirmaCosto();
        if (firma == firmaCosto) return;
        firmaCosto = firma;
        esperaCosto?.Cancel();
        var espera = esperaCosto = new CancellationTokenSource();
        try
        {
            await Task.Delay(400, espera.Token);
        }
        catch (TaskCanceledException)
        {
            return;
        }
        await RevisarCosto();
        StateHasChanged();
    }

    private async Task RevisarCosto()
    {
        if (venta is null || venta.Lineas.Count == 0 || venta.FechaEmision is null)
        {
            bajoCosto = new();
        }
        else
        {
            for (var i = 0; i < venta.Lineas.Count; i++)
            {
                venta.Lineas[i].NumeroLinea = i + 1;
            }
            try
            {
                bajoCosto = await Api.Post<List<LineaBajoCosto>>("ventas/revisar-costo", venta) ?? new();
            }
            catch (ErrorApi)
            {
                bajoCosto = new();   // venta incompleta: lo valida el BO al registrar
            }
        }
        // con una solicitud en curso se sigue mostrando su estado aunque cambien las líneas
        requiereAutorizacion = bajoCosto.Count > 0 || estadoAutorizacion is not null;
    }

    // ---------------- Validación en pantalla (RNF004) ----------------
    // Mismas reglas que VentaBOImpl. Lo que solo sabe el BO (stock disponible, deuda del cliente) sale en el aviso rojo.
    internal readonly ValidacionEnVivo v = new();
}
