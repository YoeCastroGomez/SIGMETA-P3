// Código de la página Ajuste (parte 1 de 2). La vista está en Ajuste.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Almacen;

public partial class Ajuste
{
    private const int MaximoVisibles = 8;
    private const int LargoMotivo = 200;   // mismo límite que MovimientoInventarioBOImpl.solicitarAjuste
    private static readonly System.Text.Json.JsonSerializerOptions Json = new(System.Text.Json.JsonSerializerDefaults.Web);

    // motivos que más se repiten en un conteo; se pueden completar después de elegirlos
    private static readonly string[] MotivosFaltante =
        { "Merma", "Rotura o daño", "Pérdida o robo", "Producto vencido o inservible", "Error en el conteo anterior" };
    private static readonly string[] MotivosSobrante =
        { "Mercadería recibida sin registrar", "Devolución no registrada", "Despacho registrado de más", "Error en el conteo anterior" };
    private static readonly string[] MotivosGenerales =
        { "Error en el conteo anterior", "Merma", "Rotura o daño", "Mercadería recibida sin registrar" };

    private static readonly (string Valor, string Texto)[] Filtros =
        { ("", "Todos"), ("BAJO", "Bajo el mínimo"), ("SIN", "Sin stock"), ("TRAMITE", "En trámite") };

    private List<ExistenciaFila> existencias = new();
    private List<SolicitudDatos> enCurso = new();        // otros ajustes en trámite del Almacenero
    private string? estadoAutorizacion;     // RF014: null, PENDIENTE, APROBADA, RECHAZADA, VENCIDA o USADA
    // RF014: retoma el ajuste con la autorización ya pedida: /almacen/ajuste?solicitud=ID
    [SupplyParameterFromQuery(Name = "solicitud")] public int? IdSolicitud { get; set; }
    private int? solicitudCargada;
    private SolicitudDatos? retomada;
    private int idProducto;
    private double? contado;
    private string motivo = "";
    private string buscar = "";
    private string filtro = "";
    private bool cargado;
    private bool intento;
    private bool guardando;
    private string? error;

    private string Titulo => retomada is null ? "Ajuste por conteo físico" : "Ajuste en trámite";
    // mientras espera o ya está autorizado, el ajuste no se cambia (se autorizó lo que se mandó)
    private bool Bloqueado => estadoAutorizacion is "PENDIENTE" or "APROBADA";
    private bool Autorizada => estadoAutorizacion == "APROBADA";
    private ExistenciaFila? Producto => existencias.FirstOrDefault(p => p.Id == idProducto);
    private double? Diferencia => Producto is null || contado is null ? null : contado - Producto.StockActual;
    private bool HayDiferencia => Diferencia is double d && Math.Abs(d) >= 0.0005;
    private double Valor => HayDiferencia ? Math.Abs(Diferencia!.Value) * Producto!.CostoUnitario : 0;
    private string[] Motivos => !HayDiferencia ? MotivosGenerales : Diferencia > 0 ? MotivosSobrante : MotivosFaltante;

    private string? TextoDiferencia => !HayDiferencia ? Producto?.Nombre
        : $"{(Diferencia > 0 ? "Sobrante" : "Faltante")} de {Formato.Cantidad(Math.Abs(Diferencia!.Value))} {TextosAlmacen.Unidad(Producto!.UnidadVenta)} · {Producto.Nombre}";

    // estado del ajuste mientras no existe (lo que sigue a la solicitud)
    private string? EstadoTexto => estadoAutorizacion switch
    {
        "PENDIENTE" => "Solicitado",
        "APROBADA" => "Autorizado",
        "RECHAZADA" => "Rechazado",
        "VENCIDA" => "Vencido",
        _ => null
    };

    internal static string EstadoAjuste(SolicitudDatos s) => s.Cancelada ? "Cancelado" : s.Estado switch
    {
        "PENDIENTE" => "Solicitado",
        "APROBADA" => "Autorizado",
        "RECHAZADA" => "Rechazado",
        "VENCIDA" => "Vencido",
        _ => TextosSeguridad.Texto(s.Estado)
    };

    private List<PasoDocumento> Pasos => new()
    {
        new("Preparar el ajuste", Errores().Count == 0 || Bloqueado ? EstadoPaso.Hecho : EstadoPaso.Actual, Producto?.CodigoInterno),
        new("Autorización", Autorizada ? EstadoPaso.Hecho : Errores().Count == 0 || estadoAutorizacion is not null ? EstadoPaso.Actual : EstadoPaso.Pendiente,
            estadoAutorizacion switch { "PENDIENTE" => "Solicitada", "RECHAZADA" => "Rechazada", "VENCIDA" => "Vencida", _ => null },
            estadoAutorizacion == "PENDIENTE" ? "Administrador" : null),
        new("Registrar", Autorizada ? EstadoPaso.Actual : EstadoPaso.Pendiente, null, Autorizada ? "Almacenero" : null)
    };

    // ---------------- búsqueda del producto ----------------

    // cada palabra tiene que aparecer en el código, el nombre o la categoría ("cable 14" encuentra CABLE THW 14 AWG)
    private List<ExistenciaFila> Encontrados
    {
        get
        {
            var palabras = buscar.Split(' ', StringSplitOptions.RemoveEmptyEntries);
            return existencias
                .Where(p => PasaFiltro(p, filtro))
                .Where(p => palabras.All(w => Busqueda.Coincide(w, p.CodigoInterno, p.Nombre, p.Categoria)))
                .OrderBy(p => p.Nombre)
                .ToList();
        }
    }

    private bool PasaFiltro(ExistenciaFila p, string valor) => valor switch
    {
        "BAJO" => p.BajoMinimo,
        "SIN" => p.SinStock,
        "TRAMITE" => EnCursoDe(p.Id) is not null,
        _ => true
    };

    private int Cuantos(string valor) => existencias.Count(p => PasaFiltro(p, valor));

    private string SinResultados => string.IsNullOrWhiteSpace(buscar)
        ? "Ningún producto está en este filtro." : $"Ningún producto coincide con «{buscar.Trim()}».";

    // Enter con un solo resultado lo elige
    private void AlTeclear(KeyboardEventArgs e)
    {
        if (e.Key == "Enter" && Encontrados is { Count: 1 } unico)
        {
            Elegir(unico[0].Id);
        }
    }

    private void Elegir(int id)
    {
        idProducto = id;
        contado = null;
    }

    private void CambiarProducto()
    {
        idProducto = 0;
        contado = null;
        intento = false;
    }

    // ---------------- motivo ----------------

    private bool EsMotivo(string m) => motivo.Trim().StartsWith(m, StringComparison.OrdinalIgnoreCase);

    // si ya se escribió un detalle después del motivo elegido, se conserva ("Rotura o daño: 2 al apilar")
    private void ElegirMotivo(string m)
    {
        var actual = motivo.Trim();
        var anterior = MotivosFaltante.Concat(MotivosSobrante).FirstOrDefault(x => actual.StartsWith(x, StringComparison.OrdinalIgnoreCase));
        var detalle = anterior is null ? "" : actual[anterior.Length..].TrimStart(':', ' ');
        if (anterior is null && actual.Length > 0) detalle = actual;
        var nuevo = detalle.Length == 0 ? m : $"{m}: {detalle}";
        motivo = nuevo.Length > LargoMotivo ? nuevo[..LargoMotivo] : nuevo;
    }

    // ---------------- validación (mismas reglas que MovimientoInventarioBOImpl.registrarAjuste) ----------------

    private Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (Producto is null) e["producto"] = "Elige el producto contado";
        if (contado is null) e["contado"] = "Escribe la cantidad contada";
        else if (contado < 0) e["contado"] = "La cantidad contada no puede ser negativa";
        else if (Diferencia is not null && !HayDiferencia) e["contado"] = "Coincide con el stock: no hay nada que ajustar";
        if (string.IsNullOrWhiteSpace(motivo)) e["motivo"] = "Escribe o elige el motivo";
        return e;
    }

    private List<string> Faltan => Errores().Values.ToList();
    private string? E(string campo) => intento && Errores().TryGetValue(campo, out var m) ? m : null;
    private string Invalido(string campo) => E(campo) is null ? "" : "is-invalid";

    // ---------------- carga y retoma ----------------

    protected override async Task OnParametersSetAsync()
    {
        // la misma página se reutiliza al ir de un ajuste a otro: se carga cada vez que cambia la solicitud
        if (cargado && solicitudCargada == IdSolicitud) return;
        cargado = false;
        solicitudCargada = IdSolicitud;
        retomada = null;
        estadoAutorizacion = null;
        idProducto = 0;
        contado = null;
        motivo = "";
        intento = false;
        try
        {
            existencias = await Api.Get<List<ExistenciaFila>>("almacen/existencias") ?? new();
            enCurso = await Api.Get<List<SolicitudDatos>>($"solicitudes/en-curso?operacion={TextosSeguridad.OperacionAjuste}") ?? new();
            if (IdSolicitud is int id)
            {
                await Retomar(id);
            }
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        cargado = true;
    }

    // RF014: vuelve a armar el ajuste tal como se pidió y sigue su autorización (no se pide otra vez)
    private async Task Retomar(int idSolicitud)
    {
        var s = await Api.Get<SolicitudDatos>($"solicitudes/{idSolicitud}");
        var datos = s is null ? null : LeerDatos(s);
        if (s is null || datos is null || s.Operacion != TextosSeguridad.OperacionAjuste)
        {
            error = "No se encontraron los datos de esa solicitud. Prepare el ajuste otra vez.";
            return;
        }
        if (s.Estado == "USADA")
        {
            error = "Esa autorización ya se usó: el ajuste ya está registrado.";
            return;
        }
        if (existencias.All(p => p.Id != datos.IdProducto))
        {
            error = "El producto de esa solicitud ya no está activo.";
            return;
        }
        idProducto = datos.IdProducto;
        contado = datos.CantidadContada;
        motivo = datos.Motivo ?? s.Motivo ?? "";
        retomada = s;
        estadoAutorizacion = s.Estado;
    }

    // lo que se guardó al pedir la autorización (los datos del producto son solo para mostrarlo en las listas)
    private sealed class DatosAjusteGuardado
    {
        public int IdProducto { get; set; }
        public double CantidadContada { get; set; }
        public string? Motivo { get; set; }
        public string? Codigo { get; set; }
        public string? Producto { get; set; }
        public string? Unidad { get; set; }
        public double StockSistema { get; set; }
    }

    private static DatosAjusteGuardado? LeerDatos(SolicitudDatos s)
    {
        if (string.IsNullOrEmpty(s.DatosOperacion)) return null;
        try
        {
            var d = System.Text.Json.JsonSerializer.Deserialize<DatosAjusteGuardado>(s.DatosOperacion, Json);
            return d is { IdProducto: > 0 } ? d : null;
        }
        catch (System.Text.Json.JsonException)
        {
            return null;
        }
    }

    private SolicitudDatos? EnCursoDe(int id) => enCurso
        .Where(s => s.Id != retomada?.Id && s.Estado is "PENDIENTE" or "APROBADA")
        .FirstOrDefault(s => LeerDatos(s)?.IdProducto == id);

    private void AlCambiarAutorizacion(string? estado)
    {
        estadoAutorizacion = estado;
        if (estado is null)
        {
            retomada = null;   // se descartó la solicitud: el ajuste se vuelve a preparar
        }
    }
}
