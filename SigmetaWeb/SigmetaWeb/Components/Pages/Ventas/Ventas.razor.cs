// Código de la página Ventas (parte 1 de 1). La vista está en Ventas.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Clientes;
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class Ventas
{
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";

    // Filtros en la dirección: /ventas?cliente=3&situacion=POR_COBRAR&q=acme&pagina=2
    [SupplyParameterFromQuery(Name = "cliente")] public int? ClienteQ { get; set; }
    [SupplyParameterFromQuery(Name = "situacion")] public string? SituacionQ { get; set; }
    [SupplyParameterFromQuery(Name = "q")] public string? BusquedaQ { get; set; }
    [SupplyParameterFromQuery(Name = "pagina")] public int? PaginaQ { get; set; }
    // Aviso de éxito con el patrón único (AvisoGuardado): el formulario vuelve aquí con ?guardado=…&ref=…
    [SupplyParameterFromQuery(Name = "guardado")] public string? Guardado { get; set; }
    [SupplyParameterFromQuery(Name = "ref")] public string? Referencia { get; set; }

    private List<VentaDatos>? ventas;
    private List<Cliente> clientes = new();
    private int filtroCliente;
    private int? clienteCargado;
    private string situacion = "";
    private string busqueda = "";
    private int paginaActual = 1;
    private int tamanoPagina = 10;
    private string? urlPropia;

    private string? error;
    private string? exito;
    private bool avisoLeido;
    private bool trabajando;
    private VentaDatos? anulando;

    private string RutaActual => "/" + Nav.ToBaseRelativePath(Nav.Uri);
    private string RutaVenta(VentaDatos v) => $"/ventas/{v.Id}/comprobante?volver={Volver.Parametro(RutaActual)}";
    private bool HayFiltros => filtroCliente > 0 || situacion != "" || !string.IsNullOrWhiteSpace(busqueda);

    // Botón principal según el rol y lo que le falta a la venta; si no hay nada que hacer, "Ver"
    private AccionFila Principal(VentaDatos v)
    {
        // RF010 (Adriano): el Cajero cobra la venta al contado; queda como movimiento COBRO_CONTADO de su caja
        if (Rol == "Cajero" && v.CondicionPago == "CONTADO" && !v.CobradaContado && !FlujoVentas.EstaAnulada(v))
            return new("Cobrar", Ruta: $"/ventas/{v.Id}/cobro?volver=/ventas");
        if (FlujoVentas.Situacion(v) == FlujoVentas.SinComprobante && Rol is "Vendedor" or "Cajero")
            return new("Emitir", Ruta: RutaVenta(v));
        return new("Ver", Ruta: RutaVenta(v), Destacada: false);
    }

    // Lo llama AccionesFila: la acción corre en esta pantalla para que se redibuje (ventana de anular)
    private Task Ejecutar(AccionFila a) => a.Hacer?.Invoke() ?? Task.CompletedTask;

    // Menú «⋯»
    private List<AccionFila> Secundarias(VentaDatos v)
    {
        var lista = new List<AccionFila>();
        if (Principal(v).Texto != "Ver")
            lista.Add(new("Ver detalle", Ruta: RutaVenta(v)));
        if (Rol == "Vendedor" && FlujoVentas.MotivoNoEditable(v) is null)
            lista.Add(new("Editar", Ruta: $"/ventas/editar/{v.Id}?volver={Volver.Parametro(RutaActual)}"));
        // RF008 y actores: anulan el Vendedor y el Administrador ("gestiona anulaciones"; VentaBOImpl.anular)
        if (Rol is "Vendedor" or "Administrador" && FlujoVentas.MotivoNoAnulable(v) is null)
            lista.Add(new("Anular", Hacer: () => AbrirAnulacion(v), Peligro: true));
        return lista;
    }

    // ---------------- Filtrado (en la pantalla, sobre lo que ya devolvió el Java) ----------------

    private List<VentaDatos> PorBusqueda => (ventas ?? new())
        .Where(v => Busqueda.Coincide(busqueda, v.Numero, v.Cliente, v.DocumentoCliente, v.Comprobante, v.NumeroOrden))
        .ToList();

    private List<VentaDatos> Filtradas => PorBusqueda
        .Where(v => situacion == "" || FlujoVentas.Situacion(v) == situacion)
        .OrderByDescending(v => v.FechaEmision).ThenByDescending(v => v.Id)
        .ToList();

    private IReadOnlyList<OpcionChip> Chips
    {
        get
        {
            var base_ = PorBusqueda;
            int Cuantas(string s) => base_.Count(v => FlujoVentas.Situacion(v) == s);
            return new List<OpcionChip>
            {
                new("", "Todas", base_.Count),
                new(FlujoVentas.SinComprobante, "Sin comprobante", Cuantas(FlujoVentas.SinComprobante), "ambar"),
                new(FlujoVentas.PorCobrar, "Por cobrar", Cuantas(FlujoVentas.PorCobrar), "ambar"),
                new(FlujoVentas.PorDespachar, "Por despachar", Cuantas(FlujoVentas.PorDespachar), "azul"),
                new(FlujoVentas.Completa, "Completas", Cuantas(FlujoVentas.Completa), "verde"),
                new(FlujoVentas.Anulada, "Anuladas", Cuantas(FlujoVentas.Anulada), "rojo")
            };
        }
    }

    private IEnumerable<VentaDatos> Pagina(List<VentaDatos> lista)
    {
        if (tamanoPagina == 0) return lista;
        var total = Math.Max(1, (int)Math.Ceiling((double)lista.Count / tamanoPagina));
        return lista.Skip((Math.Clamp(paginaActual, 1, total) - 1) * tamanoPagina).Take(tamanoPagina);
    }

    // ---------------- Carga y filtros en la dirección ----------------

    protected override async Task OnInitializedAsync()
    {
        try
        {
            clientes = await Api.Get<List<Cliente>>("clientes") ?? new();   // para el filtro por cliente
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }

    protected override async Task OnParametersSetAsync()
    {
        if (!avisoLeido)
        {
            avisoLeido = true;
            exito = AvisoGuardado.Texto(Guardado, "la venta", Referencia);
        }
        if (urlPropia is not null && Nav.Uri == urlPropia)
        {
            return;   // el cambio de dirección lo hizo esta misma pantalla: ya tiene los filtros
        }
        filtroCliente = ClienteQ ?? 0;
        situacion = SituacionQ ?? "";
        busqueda = BusquedaQ ?? "";
        paginaActual = Math.Max(1, PaginaQ ?? 1);
        if (ventas is null || clienteCargado != filtroCliente)
        {
            await Cargar();
        }
    }

    private async Task Cargar()
    {
        try
        {
            var ruta = filtroCliente > 0 ? $"ventas?cliente={filtroCliente}" : "ventas";   // listarPorCliente / listarTodos
            ventas = await Api.Get<List<VentaDatos>>(ruta) ?? new();
            clienteCargado = filtroCliente;
        }
        catch (ErrorApi e)
        {
            error = e.Message;
            ventas ??= new();
        }
        await CargarPorRegistrar();
    }

    // ---------------- RF014: ventas nuevas que esperan la autorización del descuento ----------------

    private sealed record PorRegistrar(SolicitudDatos Solicitud, string Cliente, decimal Total);
    private List<PorRegistrar> porRegistrar = new();

    private async Task CargarPorRegistrar()
    {
        if (Rol != "Vendedor") return;
        try
        {
            var enCurso = await Api.Get<List<SolicitudDatos>>($"solicitudes/en-curso?operacion={TextosSeguridad.OperacionDescuento}") ?? new();
            var opciones = new System.Text.Json.JsonSerializerOptions(System.Text.Json.JsonSerializerDefaults.Web);
            porRegistrar = new();
            foreach (var s in enCurso)
            {
                VentaDatos? v = null;
                try { v = System.Text.Json.JsonSerializer.Deserialize<VentaDatos>(s.DatosOperacion!, opciones); }
                catch (System.Text.Json.JsonException) { }
                if (v is null || v.Id > 0) continue;   // las ediciones ya están en la lista de ventas
                var cliente = v.Cliente ?? clientes.FirstOrDefault(c => c.Id == v.IdCliente)?.NombreMostrado ?? "—";
                porRegistrar.Add(new PorRegistrar(s, cliente, TextosVentas.Total(v.Lineas)));
            }
        }
        catch (ErrorApi)
        {
            porRegistrar = new();
        }
    }

    private static string TextoAutorizacion(SolicitudDatos s) => s.Estado switch
    {
        "PENDIENTE" => "Solicitada",
        "APROBADA" => "Autorizada",
        _ => TextosSeguridad.TextoEstado(s)
    };

    private static AccionFila PrincipalPorRegistrar(SolicitudDatos s) => s.Estado switch
    {
        "APROBADA" => new("Registrar", Ruta: $"/ventas/nueva?solicitud={s.Id}"),
        "PENDIENTE" => new("Continuar", Ruta: $"/ventas/nueva?solicitud={s.Id}", Destacada: false),
        _ => new("Corregir", Ruta: $"/ventas/nueva?solicitud={s.Id}", Destacada: false)
    };

    private List<AccionFila> SecundariasPorRegistrar(SolicitudDatos s) => new()
    {
        new("Ver solicitud", Ruta: $"/autorizaciones/detalle/{s.Id}"),
        new("Descartar", Hacer: () => Descartar(s), Peligro: true)
    };

    // POST /api/solicitudes/{id}/descartar: la venta ya no se registrará
    private async Task Descartar(SolicitudDatos s)
    {
        trabajando = true;
        try
        {
            await Api.Post<object>($"solicitudes/{s.Id}/descartar", null);
            exito = "Se descartó la venta por registrar.";
            await CargarPorRegistrar();
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

    private void SincronizarUrl()
    {
        var uri = Nav.GetUriWithQueryParameters(new Dictionary<string, object?>
        {
            ["cliente"] = filtroCliente > 0 ? filtroCliente : (object?)null,
            ["situacion"] = situacion == "" ? null : situacion,
            ["q"] = string.IsNullOrWhiteSpace(busqueda) ? null : busqueda.Trim(),
            ["pagina"] = paginaActual > 1 ? paginaActual : (object?)null,
            ["guardado"] = null,
            ["ref"] = null
        });
        if (uri == Nav.Uri) return;
        urlPropia = uri;
        Nav.NavigateTo(uri, replace: true);
    }

    private async Task CambiarCliente(ChangeEventArgs e)
    {
        filtroCliente = int.TryParse(e.Value?.ToString(), out var id) ? id : 0;
        paginaActual = 1;
        await Cargar();
        SincronizarUrl();
    }

    private void CambiarSituacion(string valor)
    {
        situacion = valor;
        paginaActual = 1;
        SincronizarUrl();
    }

    private void AlBuscar()
    {
        paginaActual = 1;
        SincronizarUrl();
    }

    private void CambiarPagina(int pagina)
    {
        paginaActual = pagina;
        SincronizarUrl();
    }
}
