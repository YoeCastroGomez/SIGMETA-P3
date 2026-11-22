// Código de la página NotasCredito (parte 1 de 1). La vista está en NotasCredito.razor: es la misma clase (partial).
using System.Text.Json;
using SigmetaWeb.Components.Pages.Almacen;
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class NotasCredito
{
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);

    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";
    private bool EsCajero => Rol == "Cajero";
    // RF013: modifican y anulan la nota el Cajero y el Administrador (igual que ComprobanteBOImpl)
    private bool PuedeModificar => Rol == "Cajero" || Rol == "Administrador";

    [SupplyParameterFromQuery(Name = "guardado")] public string? Guardado { get; set; }
    [SupplyParameterFromQuery(Name = "ref")] public string? Referencia { get; set; }

    private List<NotaCreditoDatos>? notas;
    private List<SolicitudDatos> enTramite = new();     // RF014: notas pedidas y todavía no registradas
    private List<NotaCreditoLinea> detalle = new();
    private string? detalleDe;
    private DateTime? desde;
    private DateTime? hasta;
    private string texto = "";
    private string filtroEstado = "";
    private NotaCreditoDatos? porAnular;
    private NotaCreditoDatos? porEditar;
    private FilaNota? porDescartar;
    private bool cargando;
    private bool trabajando;
    private string? error;
    private string? exito;
    private CancellationTokenSource? esperaBusqueda;
    private int paginaActual = 1;
    private int tamanoPagina = 10;

    private bool HayFiltros => desde is not null || hasta is not null || !string.IsNullOrWhiteSpace(texto);

    // Una fila de la lista: una nota ya emitida o una nota en trámite (su solicitud de autorización)
    private sealed class FilaNota
    {
        public string Clave { get; init; } = "";
        public NotaCreditoDatos? Nota { get; init; }
        public SolicitudDatos? Solicitud { get; init; }
        public DatosNotaGuardada? Datos { get; init; }
        public string EstadoClave { get; init; } = "";      // SOLICITADA, AUTORIZADA, RECHAZADA, VENCIDA, VIGENTE o ANULADO
        public string EstadoTexto { get; init; } = "";
        public string? EstadoDetalle { get; init; }
        public string? Comprobante { get; init; }
        public string? TipoComprobante { get; init; }
        public string? Venta { get; init; }
        public string Cliente { get; init; } = "";
        public string? Documento { get; init; }
        public double Total { get; init; }
        public string? Fecha { get; init; }
        public bool Apagada => EstadoClave is "ANULADO" or "VENCIDA" or "RECHAZADA";
    }

    private List<FilaNota> Filas
    {
        get
        {
            var filas = new List<FilaNota>();
            // primero lo que está en trámite: es lo que todavía hay que atender
            foreach (var s in enTramite.Where(CumpleFiltros))
            {
                var d = Leer(s);
                var (clave, textoEstado, detalleEstado) = s.Estado switch
                {
                    "PENDIENTE" => ("SOLICITADA", "Solicitada", TextosSeguridad.Hace(s.FechaSolicitud)),
                    "APROBADA" => ("AUTORIZADA", "Autorizada", $"Hasta {Formato.FechaHora(s.FechaVencimiento)}"),
                    "RECHAZADA" => ("RECHAZADA", "Rechazada", string.IsNullOrEmpty(s.Administrador) ? null : $"Por {s.Administrador}"),
                    _ => ("VENCIDA", "Vencida", (string?)null)
                };
                filas.Add(new FilaNota
                {
                    Clave = $"s{s.Id}", Solicitud = s, Datos = d, EstadoClave = clave, EstadoTexto = textoEstado,
                    EstadoDetalle = detalleEstado, Comprobante = d?.Comprobante ?? s.Referencia,
                    TipoComprobante = d is null ? null : Formato.Legible(d.Tipo), Venta = d?.Venta,
                    Cliente = d?.Cliente ?? "—", Documento = d?.Documento, Total = d?.Total ?? 0, Fecha = s.FechaSolicitud
                });
            }
            foreach (var n in notas ?? new())
            {
                var anulada = n.Estado == "ANULADO";
                filas.Add(new FilaNota
                {
                    Clave = $"n{n.Id}", Nota = n, EstadoClave = anulada ? "ANULADO" : "VIGENTE",
                    EstadoTexto = anulada ? "Anulada" : "Vigente",
                    EstadoDetalle = $"{NotaCreditoDatos.TextoTipo(n.TipoNota)}",
                    Comprobante = n.Origen, TipoComprobante = Formato.Legible(n.TipoOrigen), Venta = n.Venta,
                    Cliente = n.Cliente, Documento = n.NumeroDocumento, Total = n.Total, Fecha = n.FechaEmision
                });
            }
            return filas;
        }
    }

    // las notas en trámite se filtran aquí (las emitidas las filtra el Java)
    private bool CumpleFiltros(SolicitudDatos s)
    {
        var d = Leer(s);
        if (!Busqueda.Coincide(texto, d?.Cliente, d?.Documento, d?.Comprobante, d?.Venta, s.Referencia, s.Motivo, s.Solicitante))
        {
            return false;
        }
        if (!DateTime.TryParse(s.FechaSolicitud, System.Globalization.CultureInfo.InvariantCulture,
                System.Globalization.DateTimeStyles.None, out var fecha))
        {
            return true;
        }
        return (desde is null || fecha.Date >= desde.Value.Date) && (hasta is null || fecha.Date <= hasta.Value.Date);
    }

    private static DatosNotaGuardada? Leer(SolicitudDatos s)
    {
        if (string.IsNullOrEmpty(s.DatosOperacion)) return null;
        try
        {
            return JsonSerializer.Deserialize<DatosNotaGuardada>(s.DatosOperacion, Json);
        }
        catch (JsonException)
        {
            return null;
        }
    }

    private List<FilaNota> Filtradas => Filas
        .Where(f => filtroEstado == "" || f.EstadoClave == filtroEstado
                    || (filtroEstado == "POR_CORREGIR" && f.EstadoClave is "RECHAZADA" or "VENCIDA"))
        .ToList();

    private IReadOnlyList<OpcionChip> Chips
    {
        get
        {
            var filas = Filas;
            int Cuantas(params string[] estados) => filas.Count(f => estados.Contains(f.EstadoClave));
            return new List<OpcionChip>
            {
                new("", "Todas", filas.Count),
                new("SOLICITADA", "Solicitadas", Cuantas("SOLICITADA"), "ambar"),
                new("AUTORIZADA", "Autorizadas", Cuantas("AUTORIZADA"), "verde"),
                new("POR_CORREGIR", "Rechazadas o vencidas", Cuantas("RECHAZADA", "VENCIDA"), "rojo"),
                new("VIGENTE", "Vigentes", Cuantas("VIGENTE"), "verde"),
                new("ANULADO", "Anuladas", Cuantas("ANULADO"))
            };
        }
    }

    // Lo llama AccionesFila: la acción corre en esta pantalla para que se redibuje (detalle, ventanas de motivo y anular)
    private Task Ejecutar(AccionFila a) => a.Hacer?.Invoke() ?? Task.CompletedTask;

    // Botón visible de cada fila: el siguiente paso
    private AccionFila Principal(FilaNota f)
    {
        if (f.Nota is not null)
        {
            return new(detalleDe == f.Clave ? "Ocultar" : "Ver", Hacer: () => VerDetalle(f), Destacada: false);
        }
        var s = f.Solicitud!;
        if (!EsCajero)
        {
            return s.Estado == "PENDIENTE"
                ? new("Resolver", Ruta: $"/autorizaciones/aprobar/{s.Id}")
                : new("Ver solicitud", Ruta: $"/autorizaciones/detalle/{s.Id}", Destacada: false);
        }
        var ruta = $"/notas-credito/nueva?solicitud={s.Id}";
        return s.Estado switch
        {
            "APROBADA" => new("Registrar", Ruta: ruta),
            "PENDIENTE" => new("Continuar", Ruta: ruta, Destacada: false),
            _ => new("Corregir", Ruta: ruta, Destacada: false)
        };
    }

    // Menú «⋯»
    private List<AccionFila> Secundarias(FilaNota f)
    {
        if (f.Nota is not null)
        {
            return f.Nota.Estado == "ANULADO" || !PuedeModificar
                ? new()
                : new()
                {
                    new("Editar motivo", Hacer: () => PedirEditar(f.Nota)),
                    new("Anular", Hacer: () => PedirAnular(f.Nota), Peligro: true)
                };
        }
        var s = f.Solicitud!;
        var lista = new List<AccionFila> { new(detalleDe == f.Clave ? "Ocultar detalle" : "Ver detalle", Hacer: () => VerDetalle(f)) };
        if (EsCajero)
        {
            lista.Add(new("Ver solicitud", Ruta: $"/autorizaciones/detalle/{s.Id}"));
            lista.Add(new("Descartar", Hacer: () => PedirDescartar(f), Peligro: true));
        }
        return lista;
    }

    // El Java busca por texto: se espera a que se deje de escribir para no consultar en cada tecla
    private async Task BuscarAlEscribir()
    {
        esperaBusqueda?.Cancel();
        var espera = esperaBusqueda = new CancellationTokenSource();
        try
        {
            await Task.Delay(350, espera.Token);
        }
        catch (TaskCanceledException)
        {
            return;
        }
        await Buscar();
    }

    private IEnumerable<FilaNota> Pagina(List<FilaNota> lista)
    {
        if (tamanoPagina == 0) return lista;
        var total = Math.Max(1, (int)Math.Ceiling((double)lista.Count / tamanoPagina));
        return lista.Skip((Math.Clamp(paginaActual, 1, total) - 1) * tamanoPagina).Take(tamanoPagina);
    }

    protected override async Task OnInitializedAsync()
    {
        await Buscar();
        if (Guardado == AvisoGuardado.Nuevo)
        {
            exito = $"Se registró la nota de crédito {Referencia}.";
        }
    }

    private async Task Buscar()
    {
        error = null;
        if (ValidacionEnVivo.ErrorRango(desde, hasta) is not null)
        {
            return;
        }
        cargando = true;
        try
        {
            notas = await Api.Get<List<NotaCreditoDatos>>(
                $"notas-credito?desde={Formato.Iso(desde)}&hasta={Formato.Iso(hasta)}&texto={Uri.EscapeDataString(texto.Trim())}") ?? new();
            // RF014: las del Cajero conectado (o las de todos si es el Administrador)
            enTramite = await Api.Get<List<SolicitudDatos>>($"solicitudes/en-curso?operacion={TextosSeguridad.OperacionNotaCredito}") ?? new();
            paginaActual = 1;
            detalleDe = null;
        }
        catch (ErrorApi e)
        {
            error = e.Message;
            notas ??= new();
        }
        finally
        {
            cargando = false;
        }
    }

    private async Task Limpiar()
    {
        desde = hasta = null;
        texto = "";
        filtroEstado = "";
        exito = null;
        await Buscar();
    }

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
