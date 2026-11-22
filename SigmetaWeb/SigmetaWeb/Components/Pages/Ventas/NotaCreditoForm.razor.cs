// Código de la página NotaCreditoForm (parte 1 de 1). La vista está en NotaCreditoForm.razor: es la misma clase (partial).
using System.Text.Json;
using SigmetaWeb.Components.Pages.Almacen;
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class NotaCreditoForm
{
    private const double TasaIgv = 0.18;
    private const int MaximoVisibles = 8;
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);

    // motivos que más se repiten en una ferretería; se pueden editar después de elegirlos
    private static readonly string[] MotivosFrecuentes =
    {
        "Producto con falla de fábrica",
        "Medida o modelo equivocado",
        "Producto dañado en el transporte",
        "El cliente ya no lo necesita"
    };

    // viene del comprobante de la venta: /notas-credito/nueva?comprobante=ID
    [SupplyParameterFromQuery(Name = "comprobante")] public int? IdComprobanteInicial { get; set; }
    // RF014: retoma una nota ya solicitada (desde la lista o Mis solicitudes): /notas-credito/nueva?solicitud=ID
    [SupplyParameterFromQuery(Name = "solicitud")] public int? IdSolicitud { get; set; }

    private string? estadoAutorizacion;                 // RF014: null, PENDIENTE, APROBADA, RECHAZADA, VENCIDA o USADA
    private SolicitudDatos? retomada;                   // la solicitud que se retomó (null en una nota nueva)
    private List<ComprobanteParaNota> comprobantes = new();
    private List<SolicitudDatos> enCurso = new();       // otras notas en trámite del Cajero
    private List<LineaDevolvible> lineas = new();
    private int idComprobante;
    private string motivo = "";
    private string buscarComprobante = "";
    private bool cajaCerrada;
    private bool cargado;
    private bool intento;
    private bool guardando;
    private string? error;
    private string? exito;
    private int? solicitudCargada = -1;                 // para cargar una sola vez por dirección

    private ComprobanteParaNota? Origen => comprobantes.FirstOrDefault(c => c.Id == idComprobante);
    // el importe devuelto ya incluye IGV (precio cobrado): el total es su suma y el IGV se desglosa, igual que el BO
    private double Total => Math.Round(lineas.Where(l => l.Devolver > 0).Sum(l => l.ImporteDevolucion), 2);
    private double SubTotal => Math.Round(Total / (1 + TasaIgv), 2, MidpointRounding.AwayFromZero);
    private double Igv => Math.Round(Total - SubTotal, 2);
    private string TipoNota => Origen is not null && Math.Abs(Total - Origen.Total) <= 0.01 ? "DEVOLUCION_TOTAL" : "DEVOLUCION_POR_ITEM";

    private bool CantidadesValidas => lineas.Any(l => l.Devolver > 0) && lineas.All(l => Problema(l) is null);
    private bool Preparada => Origen is not null && !string.IsNullOrWhiteSpace(motivo) && CantidadesValidas;

    private bool Autorizada => estadoAutorizacion == "APROBADA";
    // mientras espera o ya está autorizada, la nota no se cambia (se autorizó lo que se mandó)
    private bool EsperandoOAutorizada => estadoAutorizacion is "PENDIENTE" or "APROBADA";

    private string Titulo => retomada is null ? "Nueva nota de crédito" : "Nota de crédito en trámite";
    // estado de la nota mientras no existe (lo que sigue a la solicitud)
    private string? EstadoTexto => estadoAutorizacion switch
    {
        "PENDIENTE" => "Solicitada",
        "APROBADA" => "Autorizada",
        "RECHAZADA" => "Rechazada",
        "VENCIDA" => "Vencida",
        _ => null
    };

    // texto del estado de una nota en trámite (sus datos están en la solicitud)
    internal static string EstadoNota(SolicitudDatos s) => s.Cancelada ? "Cancelada" : s.Estado switch
    {
        "PENDIENTE" => "Solicitada",
        "APROBADA" => "Autorizada",
        "RECHAZADA" => "Rechazada",
        "VENCIDA" => "Vencida",
        _ => TextosSeguridad.Texto(s.Estado)
    };

    // búsqueda del comprobante: cliente, documento, comprobante o venta; los más recientes primero
    private List<ComprobanteParaNota> Encontrados => comprobantes
        .Where(c => Busqueda.Coincide(buscarComprobante, c.Cliente, c.NumeroDocumento, c.Comprobante, c.Venta))
        .OrderByDescending(c => c.FechaEmision).ThenByDescending(c => c.Id)
        .ToList();

    private List<string> Faltan => new List<(string Texto, bool Listo)>
        {
            ("Elegir el comprobante", Origen is not null),
            ("Escribir el motivo", !string.IsNullOrWhiteSpace(motivo)),
            ("Indicar qué se devuelve", CantidadesValidas)
        }.Where(f => !f.Listo).Select(f => f.Texto).ToList();

    // Recorrido: preparar -> autorización (la da el Administrador) -> registrar
    private List<PasoDocumento> Pasos => new()
    {
        new("Preparar la nota", Preparada || EsperandoOAutorizada ? EstadoPaso.Hecho : EstadoPaso.Actual, Origen?.Comprobante),
        new("Autorización", Autorizada ? EstadoPaso.Hecho : Preparada ? EstadoPaso.Actual : EstadoPaso.Pendiente,
            estadoAutorizacion switch { "PENDIENTE" => "Solicitada", "RECHAZADA" => "Rechazada", "VENCIDA" => "Vencida", _ => null },
            estadoAutorizacion == "PENDIENTE" ? "Administrador" : null),
        new("Registrar", Preparada && Autorizada ? EstadoPaso.Actual : EstadoPaso.Pendiente, null, Autorizada ? "Cajero" : null)
    };

    // misma regla que el BO: solo lo entregado y todavía no devuelto
    private static string? Problema(LineaDevolvible l) =>
        l.Devolver < 0 ? "No puede ser negativa."
        : l.Devolver > l.Devolvible + 0.0005 ? $"Máximo {Formato.Cantidad(l.Devolvible)}."
        : null;

    private Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (Origen is null) e["comprobante"] = "Elija el comprobante que se corrige.";
        if (string.IsNullOrWhiteSpace(motivo)) e["motivo"] = "El motivo es obligatorio.";
        return e;
    }

    private string? E(string campo) => intento && Errores().TryGetValue(campo, out var m) ? m : null;
    private string Invalido(string campo) => E(campo) is null ? "" : "is-invalid";

    // otra nota en trámite (sin usar) del mismo comprobante
    private SolicitudDatos? EnCursoDe(int idComprobanteOrigen) => enCurso
        .Where(s => s.Id != retomada?.Id && s.Estado is "PENDIENTE" or "APROBADA")
        .FirstOrDefault(s => LeerDatos(s)?.IdComprobante == idComprobanteOrigen);

    protected override async Task OnParametersSetAsync()
    {
        // la misma página se reutiliza al ir de una nota a otra: se carga cada vez que cambia la solicitud
        if (cargado && solicitudCargada == IdSolicitud) return;
        cargado = false;
        solicitudCargada = IdSolicitud;
        retomada = null;
        estadoAutorizacion = null;
        idComprobante = 0;
        motivo = "";
        lineas = new();
        intento = false;
        try
        {
            comprobantes = await Api.Get<List<ComprobanteParaNota>>("notas-credito/comprobantes") ?? new();
            enCurso = await Api.Get<List<SolicitudDatos>>($"solicitudes/en-curso?operacion={TextosSeguridad.OperacionNotaCredito}") ?? new();
            if (IdSolicitud is int id)
            {
                await Retomar(id);
            }
            else if (IdComprobanteInicial is int inicial && comprobantes.Any(c => c.Id == inicial))
            {
                await ElegirComprobante(inicial);
            }
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        try
        {
            // RF010: si la nota devuelve dinero, sale de la caja abierta del Cajero; se avisa antes de registrar
            cajaCerrada = await Api.Get<object>("cajas/abierta") is null;   // 204 = el Cajero no tiene caja abierta
        }
        catch (ErrorApi)
        {
            cajaCerrada = false;   // no se pudo saber: el BO lo valida al registrar
        }
        cargado = true;
    }

    // RF014: vuelve a armar la nota tal como se pidió y sigue su autorización (no se pide otra vez)
    private async Task Retomar(int idSolicitud)
    {
        var s = await Api.Get<SolicitudDatos>($"solicitudes/{idSolicitud}");
        var datos = s is null ? null : LeerDatos(s);
        if (s is null || datos is null || s.Operacion != TextosSeguridad.OperacionNotaCredito)
        {
            error = "No se encontraron los datos de esa solicitud. Prepare la nota otra vez.";
            return;
        }
        if (s.Estado == "USADA")
        {
            error = "Esa autorización ya se usó: la nota ya está registrada.";
            return;
        }
        // si el comprobante ya no aparece (por ejemplo, ya se devolvió todo), se muestra con lo guardado
        if (comprobantes.All(c => c.Id != datos.IdComprobante))
        {
            comprobantes.Add(new ComprobanteParaNota
            {
                Id = datos.IdComprobante, Comprobante = datos.Comprobante ?? "", Cliente = datos.Cliente ?? "",
                NumeroDocumento = datos.Documento, Venta = datos.Venta, CondicionPago = datos.CondicionPago ?? "",
                Tipo = datos.Tipo ?? "", FechaEmision = datos.FechaEmision, Total = datos.TotalComprobante
            });
        }
        idComprobante = datos.IdComprobante;
        motivo = datos.Motivo ?? s.Motivo ?? "";
        await CargarLineas();
        foreach (var l in lineas)
        {
            l.Devolver = datos.Lineas.FirstOrDefault(g => g.IdLinea == l.IdDetalleVenta)?.Cantidad ?? 0;
        }
        retomada = s;
        estadoAutorizacion = s.Estado;
    }

    private static DatosNotaGuardada? LeerDatos(SolicitudDatos s)
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

    private void AlCambiarAutorizacion(string? estado)
    {
        estadoAutorizacion = estado;
        if (estado is null)
        {
            retomada = null;   // se descartó la solicitud: la nota se vuelve a preparar
        }
    }

    // RF014: pide la autorización de ESTA nota con sus datos (mismo cuerpo que registrarla) y guarda la pantalla
    private Task<SolicitudDatos?> SolicitarAutorizacion()
    {
        intento = true;
        error = null;
        var o = Origen!;
        var datos = new DatosNotaGuardada
        {
            IdComprobante = idComprobante, Comprobante = o.Comprobante, Tipo = o.Tipo, FechaEmision = o.FechaEmision,
            Cliente = o.Cliente, Documento = o.NumeroDocumento, Venta = o.Venta, CondicionPago = o.CondicionPago,
            TotalComprobante = o.Total, Motivo = motivo.Trim(), Total = Total,
            Lineas = lineas.Where(l => l.Devolver > 0)
                .Select(l => new LineaGuardada { IdLinea = l.IdDetalleVenta, Cantidad = l.Devolver, Producto = l.Producto }).ToList()
        };
        return Api.Post<SolicitudDatos>("notas-credito/solicitar", new
        {
            idComprobanteOrigen = idComprobante,
            motivo = motivo.Trim(),
            lineas = Lineas(),
            datosOperacion = JsonSerializer.Serialize(datos, Json)
        });
    }

    private object Cuerpo() => new
    {
        idComprobanteOrigen = idComprobante,
        motivo = motivo.Trim(),
        lineas = Lineas()
    };

    private object Lineas() => lineas.Where(l => l.Devolver > 0).Select(l => new { idLinea = l.IdDetalleVenta, cantidad = l.Devolver }).ToList();

    private async Task ElegirComprobante(int id)
    {
        idComprobante = id;
        buscarComprobante = "";
        await CargarLineas();
    }

    private void CambiarComprobante()
    {
        idComprobante = 0;
        lineas = new();
    }

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
