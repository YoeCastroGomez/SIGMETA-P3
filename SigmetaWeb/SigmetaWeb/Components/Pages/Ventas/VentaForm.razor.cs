// Código de la página VentaForm (parte 1 de 2). La vista está en VentaForm.razor: es la misma clase (partial).
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
    [Parameter] public int? Id { get; set; }
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";

    // viene de la lista de órdenes: /ventas/nueva?orden=ID
    [SupplyParameterFromQuery(Name = "orden")] public int? IdOrdenInicial { get; set; }
    // a dónde volver (la lista con sus filtros o la página de la venta)
    [SupplyParameterFromQuery(Name = "volver")] public string? VolverQ { get; set; }
    // RF014: retoma la venta con la autorización de descuento ya pedida (desde Mis solicitudes): ?solicitud=ID
    [SupplyParameterFromQuery(Name = "solicitud")] public int? IdSolicitud { get; set; }
    private SolicitudDatos? retomada;

    internal VentaDatos? venta;
    private List<Cliente> clientes = new();
    private List<OrdenDatos> ordenesAbiertas = new();
    private OrdenDatos? ordenElegida;
    private bool desdeOrden;
    // RF007/RF008: se llegó con "Registrar venta" desde la orden: la orden queda fija (sin elegir origen ni orden)
    private bool OrdenFija => EsNuevo && IdOrdenInicial is not null && ordenElegida is not null
                              && ordenElegida.Id == IdOrdenInicial;
    private string? error;
    private bool guardando;
    private string numeroPropuesto = "";
    // RF014: el BO respondió que una línea queda bajo el costo y hace falta la autorización del Administrador
    private bool requiereAutorizacion;
    private string? estadoAutorizacion;
    private bool BloqueadaPorAutorizacion => estadoAutorizacion is "PENDIENTE" or "APROBADA";
    // stock en almacén de los productos agregados con el buscador (referencial: el BO descuenta lo ya comprometido)
    private readonly Dictionary<int, decimal> stock = new();

    private sealed class NumeroPropuesto
    {
        public string Numero { get; set; } = "";
    }

    private sealed record Pendiente(string Texto, bool Listo);

    internal bool EsNuevo => Id is null;
    // RF008: rango de la fecha de una venta nueva (VentaBOImpl.DIAS_ATRASO_MAXIMO = 7)
    internal static DateOnly Hoy => DateOnly.FromDateTime(DateTime.Today);
    internal static DateOnly FechaMinima => Hoy.AddDays(-7);
    private string Titulo => EsNuevo ? "Nueva venta" : Editable ? $"Editar venta {venta?.Numero}" : $"Venta {venta?.Numero}";
    private string? Subtitulo => EsNuevo ? null : venta?.Cliente;
    private string VolverA => Volver.A(VolverQ, "/ventas");
    // RF008: venta REGISTRADA, sin comprobante vigente, sin despacho y (al contado) sin cobro en caja; las reglas reales están en el BO
    internal bool Editable => EsNuevo || (Rol == "Vendedor" && venta is not null && FlujoVentas.MotivoNoEditable(venta) is null);
    internal string AyudaCondicion => !EsNuevo ? "No se cambia al modificar."
        : TieneLinea ? "El cliente tiene línea de crédito." : "Crédito solo para clientes con línea de crédito autorizada.";

    // Cliente elegido (directo o el de la orden) con su línea de crédito
    internal Cliente? ClienteActual => venta is null ? null : clientes.FirstOrDefault(c => c.Id == venta.IdCliente);
    // Mismo criterio que VentaBOImpl.registrar: condición CREDITO con límite y plazo mayores a 0
    internal bool TieneLinea => ClienteActual is { } c && c.CondicionPago == "CREDITO" && c.LimiteCredito > 0 && c.PlazoCreditoDias > 0;

    private decimal SubTotal => Editable ? TextosVentas.SubTotal(venta!.Lineas) : venta!.SubTotal;
    private decimal Igv => Editable ? TextosVentas.Igv(venta!.Lineas) : venta!.Igv;
    private decimal Total => Editable ? TextosVentas.Total(venta!.Lineas) : venta!.Total;

    // Lista de lo que falta, a partir de las mismas reglas de Errores()
    private List<Pendiente> Pendientes
    {
        get
        {
            var e = Errores();
            var lista = new List<Pendiente>
            {
                new(desdeOrden ? "Orden del cliente elegida" : "Cliente elegido", !e.ContainsKey("orden") && !e.ContainsKey("cliente")),
                new("Al menos un producto", !e.ContainsKey("lineas")),
                new("Fecha de emisión válida", !e.ContainsKey("fecha"))
            };
            if (venta?.CondicionPago == "CREDITO")
            {
                lista.Add(new("Plazo de crédito válido", !e.ContainsKey("plazo")));
            }
            return lista;
        }
    }

    protected override async Task OnInitializedAsync()
    {
        // RF008: solo el Vendedor registra ventas; el Administrador no tiene "nueva venta"
        if (EsNuevo && Rol != "Vendedor")
        {
            Nav.NavigateTo("/ventas", replace: true);
            return;
        }
        try
        {
            clientes = await Api.Get<List<Cliente>>("clientes?estado=true") ?? new();   // solo clientes activos
            if (EsNuevo)
            {
                venta = new VentaDatos { FechaEmision = DateOnly.FromDateTime(DateTime.Today), CondicionPago = "CONTADO" };
                var propuesto = await Api.Get<NumeroPropuesto>("ventas/siguiente-numero");
                numeroPropuesto = propuesto?.Numero ?? "";
                var ordenes = await Api.Get<List<OrdenDatos>>("ordenes") ?? new();
                ordenesAbiertas = ordenes.Where(o => o.Estado is "PENDIENTE" or "ATENDIDA_PARCIAL" && o.PorVender).ToList();
                desdeOrden = IdOrdenInicial is not null;
                if (IdOrdenInicial is int inicial)
                {
                    // viene de "Registrar venta" en una orden: se carga esa orden aunque no esté en la lista
                    // (el Java valida que admita ventas y, si no, lo dice al registrar)
                    await ElegirOrden(inicial);
                    if (ordenElegida is null)
                    {
                        error = $"No se pudo cargar la orden {inicial}. Elíjala de la lista.";
                    }
                    else if (ordenesAbiertas.All(o => o.Id != inicial))
                    {
                        ordenesAbiertas.Add(ordenElegida);   // para que también aparezca si se elige otra y se vuelve
                    }
                }
            }
            else
            {
                venta = await Api.Get<VentaDatos>($"ventas/{Id}");   // GET /api/ventas/{Id}
                desdeOrden = venta is not null && venta.IdOrden > 0;
                if (desdeOrden)
                {
                    ordenElegida = await Api.Get<OrdenDatos>($"ordenes/{venta!.IdOrden}");
                }
                else if (venta is not null && clientes.All(c => c.Id != venta.IdCliente))
                {
                    clientes.Add(new Cliente { Id = venta.IdCliente, TipoPersona = "JURIDICA", RazonSocial = venta.Cliente, NumeroDocumento = venta.DocumentoCliente ?? "" });
                }
            }
        }
        catch (ErrorApi e)
        {
            error = e.Message;
            venta ??= new VentaDatos { FechaEmision = DateOnly.FromDateTime(DateTime.Today) };
        }
        if (IdSolicitud is int idSolicitud && venta is not null)
        {
            await RetomarAutorizacion(idSolicitud);
        }
    }

    // RF014: vuelve a armar la venta tal como se pidió el descuento y sigue su autorización (no se pide otra vez)
    private async Task RetomarAutorizacion(int idSolicitud)
    {
        try
        {
            var s = await Api.Get<SolicitudDatos>($"solicitudes/{idSolicitud}");
            if (s is null || s.Operacion != TextosSeguridad.OperacionDescuento || string.IsNullOrEmpty(s.DatosOperacion) || s.Estado == "USADA")
            {
                error = "No se pudo retomar esa solicitud: prepare la venta otra vez.";
                return;
            }
            var guardada = System.Text.Json.JsonSerializer.Deserialize<VentaDatos>(s.DatosOperacion,
                new System.Text.Json.JsonSerializerOptions(System.Text.Json.JsonSerializerDefaults.Web));
            if (guardada is null) return;
            guardada.DatosOperacion = null;
            venta = guardada;
            desdeOrden = venta.IdOrden > 0;
            if (desdeOrden)
            {
                ordenElegida = await Api.Get<OrdenDatos>($"ordenes/{venta.IdOrden}");
                if (ordenElegida is not null && ordenesAbiertas.All(o => o.Id != ordenElegida.Id))
                {
                    ordenesAbiertas.Add(ordenElegida);
                }
            }
            else if (clientes.All(c => c.Id != venta.IdCliente))
            {
                clientes.Add(new Cliente { Id = venta.IdCliente, TipoPersona = "JURIDICA", RazonSocial = venta.Cliente, NumeroDocumento = venta.DocumentoCliente ?? "" });
            }
            requiereAutorizacion = true;
            retomada = s;
            estadoAutorizacion = s.Estado;
        }
        catch (Exception e) when (e is ErrorApi or System.Text.Json.JsonException)
        {
            error = e.Message;
        }
    }

    private void UsarOrden()
    {
        if (desdeOrden) return;
        desdeOrden = true;
        venta!.Lineas.Clear();
        venta.IdCliente = 0;
        venta.Cliente = null;
        venta.DocumentoCliente = null;
        AjustarCondicion();
    }

    private void UsarDirecta()
    {
        if (!desdeOrden) return;
        desdeOrden = false;
        ordenElegida = null;
        venta!.IdOrden = 0;
        venta.NumeroOrden = null;
        venta.Cliente = null;
        venta.DocumentoCliente = null;
        venta.IdCliente = 0;
        venta.Lineas.Clear();
        AjustarCondicion();
    }

    private void ElegirCliente(int id)
    {
        venta!.IdCliente = id;
        AlCambiarCliente();
    }

    private void AlCambiarCliente()
    {
        v.Tocado("cliente");
        AjustarCondicion();
    }

    // si el cliente nuevo no tiene línea de crédito, la venta vuelve a contado (la opción Crédito queda bloqueada)
    private void AjustarCondicion()
    {
        if (EsNuevo && venta is not null && venta.CondicionPago == "CREDITO" && !TieneLinea)
        {
            venta.CondicionPago = "CONTADO";
        }
    }

    private async Task CambiarOrden(ChangeEventArgs e)
    {
        v.Tocado("orden");
        var id = int.TryParse(e.Value?.ToString(), out var valor) ? valor : 0;
        await ElegirOrden(id);
    }

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
