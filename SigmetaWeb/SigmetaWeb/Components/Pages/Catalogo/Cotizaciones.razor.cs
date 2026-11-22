// Código de la página Cotizaciones (parte 1 de 2). La vista está en Cotizaciones.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Ventas;
using SigmetaWeb.Components.Pages.Clientes;

namespace SigmetaWeb.Components.Pages.Catalogo;

public partial class Cotizaciones
{
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";
    private bool EsVendedor => Rol == "Vendedor";
    private bool PuedeAnular => EsVendedor || Rol == "Administrador";

    // Filtros en la dirección: /cotizaciones?cliente=3&estado=ENVIADA&q=acme&pagina=2
    [SupplyParameterFromQuery(Name = "cliente")] public int? ClienteQ { get; set; }
    [SupplyParameterFromQuery(Name = "estado")] public string? EstadoQ { get; set; }
    [SupplyParameterFromQuery(Name = "q")] public string? BusquedaQ { get; set; }
    [SupplyParameterFromQuery(Name = "pagina")] public int? PaginaQ { get; set; }
    // Aviso de éxito con el patrón único (AvisoGuardado): el formulario vuelve aquí con ?guardado=…&ref=…
    [SupplyParameterFromQuery(Name = "guardado")] public string? Guardado { get; set; }
    [SupplyParameterFromQuery(Name = "ref")] public string? Referencia { get; set; }

    private List<CotizacionDatos>? cotizaciones;       // lo que devolvió el BO con los filtros cliente + estado
    private List<CotizacionDatos> vigentes = new();   // del cliente, sin filtro de estado (para contar)
    private int anuladas;                             // del cliente (el BO las trae solo con estado=ANULADA)
    private List<Cliente> clientes = new();
    private Dictionary<int, string> conOrden = new(); // cotizaciones con orden vigente -> número de la orden
    private int filtroCliente;
    private string filtroEstado = "";
    private string busqueda = "";
    private int paginaActual = 1;
    private int tamanoPagina = 10;
    private string? urlPropia;
    private (int Cliente, string Estado)? cargado;

    // Notificación flotante (éxito o error)
    private string? aviso;
    private bool avisoError;
    private string? avisoAccion;
    private string? avisoRuta;
    private bool avisoLeido;

    private bool trabajando;
    private CotizacionDatos? anulando;
    private CotizacionDatos? respondiendo;

    private string RutaActual => "/" + Nav.ToBaseRelativePath(Nav.Uri);
    private string RutaCotizacion(CotizacionDatos c) => $"/cotizaciones/editar/{c.Id}?volver={Volver.Parametro(RutaActual)}";
    private string RutaDocumento(CotizacionDatos c) => $"/cotizaciones/{c.Id}/enviar?volver={Volver.Parametro(RutaActual)}";
    private bool HayFiltros => filtroCliente > 0 || filtroEstado != "" || !string.IsNullOrWhiteSpace(busqueda);
    private static DateOnly Hoy => DateOnly.FromDateTime(DateTime.Today);

    // Las que todavía se pueden mover y vencen dentro de 3 días
    private static int? DiasParaVencer(CotizacionDatos c) =>
        c.Estado is "REGISTRADA" or "ENVIADA" && c.FechaVigencia is DateOnly f && f >= Hoy && f.DayNumber - Hoy.DayNumber <= 3
            ? f.DayNumber - Hoy.DayNumber
            : null;

    // Botón principal: el siguiente paso del ciclo; si no hay, "Ver"
    private AccionFila Principal(CotizacionDatos c)
    {
        if (EsVendedor && c.Estado == "REGISTRADA")
            return new("Enviar", Ruta: RutaDocumento(c));   // documento para imprimir o PDF + marcar enviada
        if (EsVendedor && c.Estado == "ENVIADA")
            return new("Respuesta", Hacer: () => AbrirRespuesta(c));
        // RF007: la orden de compra del cliente sale de una cotización aceptada; una sola orden activa
        if (EsVendedor && c.Estado == "ACEPTADA" && !conOrden.ContainsKey(c.Id))
            return new("Generar orden", Ruta: $"/ordenes/nueva?cotizacion={c.Id}");
        return new("Ver", Ruta: RutaCotizacion(c), Destacada: false);
    }

    // Lo llama AccionesFila: la acción corre en esta pantalla para que se redibuje (ventanas de respuesta y anular)
    private Task Ejecutar(AccionFila a) => a.Hacer?.Invoke() ?? Task.CompletedTask;

    // Menú «⋯»: lo que no es el siguiente paso
    private List<AccionFila> Secundarias(CotizacionDatos c)
    {
        var lista = new List<AccionFila>();
        var principal = Principal(c);
        if (EsVendedor && c.Estado == "REGISTRADA")
            lista.Add(new("Editar", Ruta: RutaCotizacion(c)));
        else if (principal.Texto != "Ver")
            lista.Add(new("Ver detalle", Ruta: RutaCotizacion(c)));
        // volver a imprimirla o guardarla en PDF (las registradas lo hacen desde «Enviar»)
        if (c.Estado is "ENVIADA" or "ACEPTADA" or "RECHAZADA" or "VENCIDA")
            lista.Add(new("Imprimir o PDF", Ruta: RutaDocumento(c)));
        if (PuedeAnular && c.Estado is "REGISTRADA" or "ENVIADA" or "RECHAZADA" or "VENCIDA")
            lista.Add(new("Anular", Hacer: () => AbrirAnulacion(c), Peligro: true));
        return lista;
    }

    private List<CotizacionDatos> Filtradas => (cotizaciones ?? new())
        .Where(c => Busqueda.Coincide(busqueda, c.Numero, c.Cliente, c.DocumentoCliente))
        .OrderByDescending(c => c.FechaEmision).ThenByDescending(c => c.Id)
        .ToList();

    private IReadOnlyList<OpcionChip> Chips
    {
        get
        {
            int Cuantas(string e) => vigentes.Count(c => c.Estado == e);
            return new List<OpcionChip>
            {
                new("", "Vigentes", vigentes.Count),
                new("REGISTRADA", "Registradas", Cuantas("REGISTRADA")),
                new("ENVIADA", "Enviadas", Cuantas("ENVIADA"), "azul"),
                new("ACEPTADA", "Aceptadas", Cuantas("ACEPTADA"), "verde"),
                new("RECHAZADA", "Rechazadas", Cuantas("RECHAZADA"), "rojo"),
                new("VENCIDA", "Vencidas", Cuantas("VENCIDA"), "ambar"),
                new("ANULADA", "Anuladas", anuladas, "rojo")
            };
        }
    }

    private IEnumerable<CotizacionDatos> Pagina(List<CotizacionDatos> lista)
    {
        if (tamanoPagina == 0) return lista;
        var total = Math.Max(1, (int)Math.Ceiling((double)lista.Count / tamanoPagina));
        return lista.Skip((Math.Clamp(paginaActual, 1, total) - 1) * tamanoPagina).Take(tamanoPagina);
    }

    // ---------------- Carga (los filtros cliente y estado los aplica el BO) ----------------

    protected override async Task OnInitializedAsync()
    {
        try
        {
            clientes = await Api.Get<List<Cliente>>("clientes") ?? new();   // para el filtro por cliente
        }
        catch (ErrorApi e)
        {
            MostrarError(e.Message);
        }
    }

    protected override async Task OnParametersSetAsync()
    {
        if (!avisoLeido)
        {
            avisoLeido = true;
            var texto = AvisoGuardado.Texto(Guardado, "la cotización", Referencia);
            if (texto is not null) MostrarExito(texto);
        }
        if (urlPropia is not null && Nav.Uri == urlPropia)
        {
            return;   // el cambio de dirección lo hizo esta misma pantalla
        }
        filtroCliente = ClienteQ ?? 0;
        filtroEstado = EstadoQ ?? "";
        busqueda = BusquedaQ ?? "";
        paginaActual = Math.Max(1, PaginaQ ?? 1);
        await Cargar();
    }

    private async Task Cargar(bool forzar = false)
    {
        if (!forzar && cargado == (filtroCliente, filtroEstado) && cotizaciones is not null) return;
        var cliente = filtroCliente > 0 ? $"cliente={filtroCliente}" : "";
        string Ruta(string estado) => "cotizaciones" + (cliente == "" && estado == "" ? ""
            : "?" + string.Join("&", new[] { cliente, estado == "" ? "" : $"estado={estado}" }.Where(p => p != "")));
        try
        {
            // RF007: los conteos (vigentes y anuladas del cliente) y la lista filtrada salen del BO
            vigentes = await Api.Get<List<CotizacionDatos>>(Ruta("")) ?? new();
            var listaAnuladas = await Api.Get<List<CotizacionDatos>>(Ruta("ANULADA")) ?? new();
            anuladas = listaAnuladas.Count;
            cotizaciones = filtroEstado == "" ? vigentes
                : filtroEstado == "ANULADA" ? listaAnuladas
                : await Api.Get<List<CotizacionDatos>>(Ruta(filtroEstado)) ?? new();
            if (EsVendedor)
            {
                // RF007: una cotización tiene una sola orden activa; si ya la tiene no se ofrece "Generar orden"
                var ordenes = await Api.Get<List<OrdenDatos>>("ordenes") ?? new();
                conOrden = ordenes.Where(o => o.Estado != "ANULADA" && !o.Anulado)
                    .GroupBy(o => o.IdCotizacion).ToDictionary(g => g.Key, g => g.First().Numero ?? "");
            }
            cargado = (filtroCliente, filtroEstado);
        }
        catch (ErrorApi e)
        {
            MostrarError(e.Message);
            cotizaciones ??= new();
        }
    }

    private void SincronizarUrl()
    {
        var uri = Nav.GetUriWithQueryParameters(new Dictionary<string, object?>
        {
            ["cliente"] = filtroCliente > 0 ? filtroCliente : (object?)null,
            ["estado"] = filtroEstado == "" ? null : filtroEstado,
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

    private async Task CambiarEstado(string estado)
    {
        filtroEstado = estado;
        paginaActual = 1;
        await Cargar();
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

    private async Task Limpiar()
    {
        filtroCliente = 0;
        filtroEstado = "";
        busqueda = "";
        paginaActual = 1;
        await Cargar();
        SincronizarUrl();
    }

    // ---------------- Notificación (patrón RNF004: "Se + verbo + la cotización + número.") ----------------

    private void MostrarExito(string texto, string? accion = null, string? ruta = null)
    {
        aviso = texto;
        avisoError = false;
        avisoAccion = accion;
        avisoRuta = ruta;
    }

    private void MostrarError(string texto) => (aviso, avisoError, avisoAccion, avisoRuta) = (texto, true, null, null);

    private void CerrarAviso() => (aviso, avisoError, avisoAccion, avisoRuta) = (null, false, null, null);

    // ---------------- Ciclo de la cotización ----------------

    private Task AbrirRespuesta(CotizacionDatos c)
    {
        CerrarAviso();
        respondiendo = c;
        return Task.CompletedTask;
    }

    private async Task Aceptar()
    {
        if (respondiendo is not { } c) return;
        await Accion(c, "aceptar", $"Se aceptó la cotización {c.Numero}.", "Generar orden", $"/ordenes/nueva?cotizacion={c.Id}");
        respondiendo = null;
    }

    private async Task Rechazar()
    {
        if (respondiendo is not { } c) return;
        await Accion(c, "rechazar", $"Se rechazó la cotización {c.Numero}.");
        respondiendo = null;
    }
}
