// Código de la página CotizacionForm (parte 1 de 1). La vista está en CotizacionForm.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Ventas;
using SigmetaWeb.Components.Pages.Clientes;

namespace SigmetaWeb.Components.Pages.Catalogo;

public partial class CotizacionForm
{
    [Parameter] public int? Id { get; set; }
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";
    [SupplyParameterFromQuery(Name = "volver")] public string? VolverQ { get; set; }
    private bool EsVendedor => Rol == "Vendedor";

    private CotizacionDatos? cotizacion;
    private OrdenDatos? ordenGenerada;
    private List<Cliente> clientes = new();
    private string? error;
    private bool guardando;
    // stock en almacén de los productos agregados (solo informativo: cotizar no reserva stock)
    private readonly Dictionary<int, decimal> stock = new();

    private sealed record Pendiente(string Texto, bool Listo);

    private bool EsNuevo => Id is null;
    private string Titulo => EsNuevo ? "Nueva cotización" : Editable ? $"Editar cotización {cotizacion?.Numero}" : $"Cotización {cotizacion?.Numero}";
    private string? Subtitulo => EsNuevo ? null : cotizacion?.Cliente;
    private string VolverA => Volver.A(VolverQ, "/cotizaciones");
    // RF007: solo el Vendedor modifica, y solo una cotización REGISTRADA (el Administrador la consulta)
    private bool Editable => EsVendedor && (EsNuevo || (cotizacion?.Estado == "REGISTRADA" && !cotizacion.Anulado));
    private static DateOnly Hoy => DateOnly.FromDateTime(DateTime.Today);

    private int? DiasDeVigencia => cotizacion?.FechaEmision is DateOnly e && cotizacion.FechaVigencia is DateOnly f
        ? f.DayNumber - e.DayNumber : null;

    private sealed class NumeroPropuesto
    {
        public string Numero { get; set; } = "";
    }

    private void ElegirCliente(int id)
    {
        cotizacion!.IdCliente = id;
        v.Tocado("cliente");
    }

    private void FijarVigencia(int dias)
    {
        if (cotizacion?.FechaEmision is DateOnly e)
        {
            cotizacion.FechaVigencia = e.AddDays(dias);
            v.Tocado("fechaVigencia");
        }
    }

    private async Task AlCambiarFecha()
    {
        v.Tocado("fechaEmision");
        if (EsNuevo) await ObtenerCorrelativo();
    }

    private async Task ObtenerCorrelativo()
    {
        if (cotizacion?.FechaEmision is not DateOnly solicitada) return;
        cotizacion.Numero = "";
        try
        {
            var propuesto = await Api.Get<NumeroPropuesto>($"cotizaciones/siguiente-numero?fechaEmision={solicitada:yyyy-MM-dd}");
            if (cotizacion?.FechaEmision == solicitada) cotizacion.Numero = propuesto?.Numero ?? "";
        }
        catch (ErrorApi e) { error = e.Message; }
    }

    // Mientras se edita se muestran los importes de referencia; una cotización ya cerrada muestra los del BO.
    private decimal SubTotal => Editable ? TextosVentas.SubTotal(cotizacion!.Lineas) : cotizacion!.SubTotal;
    private decimal Igv => Editable ? TextosVentas.Igv(cotizacion!.Lineas) : cotizacion!.Igv;
    private decimal Total => Editable ? TextosVentas.Total(cotizacion!.Lineas) : cotizacion!.Total;

    private List<Pendiente> Pendientes
    {
        get
        {
            var e = Errores();
            return new List<Pendiente>
            {
                new("Cliente elegido", !e.ContainsKey("cliente")),
                new("Fechas de emisión y vigencia válidas", !e.ContainsKey("fechaEmision") && !e.ContainsKey("fechaVigencia")),
                new("Al menos un producto", !e.ContainsKey("lineas"))
            };
        }
    }

    protected override async Task OnInitializedAsync()
    {
        try
        {
            clientes = await Api.Get<List<Cliente>>("clientes?estado=true") ?? new();   // solo clientes activos
            if (EsNuevo)
            {
                cotizacion = new CotizacionDatos { FechaEmision = Hoy, FechaVigencia = Hoy.AddDays(15), Estado = "REGISTRADA" };
                await ObtenerCorrelativo();
            }
            else
            {
                cotizacion = await Api.Get<CotizacionDatos>($"cotizaciones/{Id}");   // GET /api/cotizaciones/{Id}
                emisionOriginal = cotizacion?.FechaEmision;
                // si el cliente de la cotización ya no está activo, igual se muestra en la lista
                if (cotizacion is not null && clientes.All(c => c.Id != cotizacion.IdCliente))
                {
                    clientes.Add(new Cliente { Id = cotizacion.IdCliente, TipoPersona = "JURIDICA", RazonSocial = cotizacion.Cliente, NumeroDocumento = cotizacion.DocumentoCliente ?? "" });
                }
                if (cotizacion?.Estado == "ACEPTADA")
                {
                    // para el recorrido: la orden vigente que salió de esta cotización
                    var ordenes = await Api.Get<List<OrdenDatos>>("ordenes") ?? new();
                    ordenGenerada = ordenes.FirstOrDefault(o => o.IdCotizacion == cotizacion.Id && o.Estado != "ANULADA" && !o.Anulado);
                }
            }
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }

    // Lo llama el BuscadorProducto con el producto elegido: nueva línea con su precio de venta (o +1 si ya estaba)
    private void AgregarProducto(Producto p)
    {
        stock[p.Id] = p.StockActual;
        var existente = cotizacion!.Lineas.FirstOrDefault(l => l.IdProducto == p.Id);
        if (existente is not null)
        {
            existente.Cantidad += 1;
            return;
        }
        cotizacion.Lineas.Add(new LineaDatos
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

    private void Quitar(LineaDatos l) => cotizacion!.Lineas.Remove(l);

    // Avisos de una línea (cantidad y descuento) en una sola frase, para la fila de avisos de la tabla
    private string? AvisoLinea(LineaDatos l)
    {
        var e = Errores();
        var n = v.Clave(l);
        var partes = new List<string>();
        if (v.Error(n + "cantidad", e) is { } c) partes.Add("Cantidad: " + c);
        if (v.Error(n + "descuento", e, n + "cantidad", n + "precio") is { } d) partes.Add("Descuento: " + d);
        return partes.Count == 0 ? null : string.Join("  ·  ", partes);
    }

    // ---------------- Validación en pantalla (RNF004) ----------------
    // Mismas reglas que CotizacionBOImpl. Cada error sale debajo de su cuadro; el BO vuelve a validar todo.
    private readonly ValidacionEnVivo v = new();
    private DateOnly? emisionOriginal;   // al editar: la fecha ya registrada se puede conservar aunque sea pasada

    private Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (cotizacion is null || !Editable)
        {
            return e;
        }
        if (cotizacion.IdCliente <= 0)
        {
            e["cliente"] = "Seleccione el cliente.";
        }
        if (cotizacion.FechaEmision is null)
        {
            e["fechaEmision"] = "La fecha de emisión es obligatoria.";
        }
        else if (cotizacion.FechaEmision < Hoy && (EsNuevo || cotizacion.FechaEmision != emisionOriginal))
        {
            // igual que el BO: no se emite con fecha pasada (al editar se puede conservar la ya registrada)
            e["fechaEmision"] = "La fecha de emisión no puede ser anterior a hoy.";
        }
        if (cotizacion.FechaVigencia is null)
        {
            e["fechaVigencia"] = "La fecha de vigencia es obligatoria.";
        }
        else if (cotizacion.FechaEmision is not null && cotizacion.FechaVigencia < cotizacion.FechaEmision)
        {
            e["fechaVigencia"] = "La vigencia no puede ser anterior a la emisión.";
        }
        else if (cotizacion.FechaVigencia < Hoy)
        {
            e["fechaVigencia"] = "La vigencia no puede ser anterior a hoy.";
        }
        if (cotizacion.Lineas.Count == 0)
        {
            e["lineas"] = "Agregue al menos un producto con el buscador.";
        }
        foreach (var l in cotizacion.Lineas)
        {
            ValidacionEnVivo.ErroresLinea(e, v.Clave(l), l.Cantidad, l.PrecioUnitario, l.Descuento);
            if (l.CantidadNoEntera && !e.ContainsKey(v.Clave(l) + "cantidad"))
            {
                e[v.Clave(l) + "cantidad"] = "Solo cantidades enteras.";
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
            // las líneas se numeran en el orden de la pantalla
            for (var i = 0; i < cotizacion!.Lineas.Count; i++)
            {
                cotizacion.Lineas[i].NumeroLinea = i + 1;
            }

            CotizacionDatos? guardada;
            if (EsNuevo)
            {
                guardada = await Api.Post<CotizacionDatos>("cotizaciones", cotizacion);          // POST /api/cotizaciones
            }
            else
            {
                guardada = await Api.Put<CotizacionDatos>($"cotizaciones/{Id}", cotizacion);    // PUT /api/cotizaciones/{Id}
            }
            // vuelve a la lista (con sus filtros) y allí sale el aviso verde con el siguiente paso
            Nav.NavigateTo(Volver.ConAviso(VolverA, EsNuevo ? AvisoGuardado.Nuevo : AvisoGuardado.Editado,
                guardada?.Numero ?? cotizacion.Numero));
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // mensaje del BO, por ejemplo "La línea 1 (...) queda en S/ 8.00 por unidad, por debajo de su costo"
        }
        finally
        {
            guardando = false;
        }
    }
}
