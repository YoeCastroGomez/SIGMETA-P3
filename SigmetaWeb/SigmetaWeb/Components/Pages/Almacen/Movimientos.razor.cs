// Código de la página Movimientos (parte 1 de 1). La vista está en Movimientos.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Almacen;

public partial class Movimientos
{
    // UI/UX: una acción visible por fila y el resto en «⋯» (mismo patrón en todas las listas)
    private Task Ejecutar(AccionFila a) => a.Hacer?.Invoke() ?? Task.CompletedTask;
    private List<AccionFila> AccionesMovimiento(MovimientoFila m)
    {
        var lista = new List<AccionFila>();
        if (EsAdministrador) lista.Add(new("Corregir motivo", Hacer: () => { Pedir(m, "motivo"); return Task.CompletedTask; }));
        if (EsAlmacenero && m.EsAjusteOriginal && m.Revertido == 0) lista.Add(new("Revertir", Hacer: () => { Pedir(m, "revertir"); return Task.CompletedTask; }, Peligro: true));
        return lista;
    }

    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";
    private bool EsAlmacenero => Rol == "Almacenero";
    private bool EsAdministrador => Rol == "Administrador";

    [SupplyParameterFromQuery(Name = "guardado")] public string? Guardado { get; set; }
    [SupplyParameterFromQuery(Name = "ref")] public string? Referencia { get; set; }

    private List<MovimientoFila>? movimientos;
    private string tipo = "";
    private string texto = "";
    private DateTime? desde;
    private DateTime? hasta;
    private MovimientoFila? enAccion;
    private string accion = "";
    private string motivo = "";
    private string? errorMotivo;
    private bool cargando;
    private string? error;
    private string? exito;
    private int paginaActual = 1;
    private int tamanoPagina = 25;

    private List<MovimientoFila> Filtrados => (movimientos ?? new())
        .Where(m => string.IsNullOrWhiteSpace(texto)
                    || m.CodigoInterno.Contains(texto.Trim(), StringComparison.OrdinalIgnoreCase)
                    || m.Producto.Contains(texto.Trim(), StringComparison.OrdinalIgnoreCase))
        .ToList();

    private IEnumerable<MovimientoFila> Pagina => tamanoPagina == 0 ? Filtrados
        : Filtrados.Skip((Math.Max(paginaActual, 1) - 1) * tamanoPagina).Take(tamanoPagina);

    // ---------------- RF014: ajustes que esperan la autorización del Administrador ----------------

    private List<SolicitudDatos> porRegistrar = new();
    private bool trabajando;

    private async Task CargarPorRegistrar()
    {
        if (!EsAlmacenero) return;
        try
        {
            porRegistrar = await Api.Get<List<SolicitudDatos>>($"solicitudes/en-curso?operacion={TextosSeguridad.OperacionAjuste}") ?? new();
        }
        catch (ErrorApi)
        {
            porRegistrar = new();
        }
    }

    // lo contado sale de los datos guardados al pedirla; la diferencia, del detalle que vio el Administrador
    private static string Contado(SolicitudDatos s)
    {
        try
        {
            using var doc = System.Text.Json.JsonDocument.Parse(s.DatosOperacion!);
            var raiz = doc.RootElement;
            var cantidad = raiz.TryGetProperty("cantidadContada", out var c) && c.TryGetDouble(out var v) ? Formato.Cantidad(v) : "—";
            var unidad = raiz.TryGetProperty("unidad", out var u) ? u.GetString() : null;
            return string.IsNullOrEmpty(unidad) ? cantidad : $"{cantidad} {unidad}";
        }
        catch (Exception e) when (e is System.Text.Json.JsonException or InvalidOperationException or ArgumentNullException)
        {
            return "—";
        }
    }

    private static string Diferencia(SolicitudDatos s) =>
        TextosSeguridad.LeerDetalle(s.Detalle).FirstOrDefault(l => l.Clave == "Diferencia")?.Valor ?? "—";

    private static AccionFila PrincipalPorRegistrar(SolicitudDatos s) => s.Estado switch
    {
        "APROBADA" => new("Registrar", Ruta: $"/almacen/ajuste?solicitud={s.Id}"),
        "PENDIENTE" => new("Continuar", Ruta: $"/almacen/ajuste?solicitud={s.Id}", Destacada: false),
        _ => new("Corregir", Ruta: $"/almacen/ajuste?solicitud={s.Id}", Destacada: false)
    };

    private List<AccionFila> SecundariasPorRegistrar(SolicitudDatos s) => new()
    {
        new("Ver solicitud", Ruta: $"/autorizaciones/detalle/{s.Id}"),
        new("Descartar", Hacer: () => Descartar(s), Peligro: true)
    };

    // POST /api/solicitudes/{id}/descartar: el ajuste ya no se registrará
    private async Task Descartar(SolicitudDatos s)
    {
        trabajando = true;
        error = exito = null;
        try
        {
            await Api.Post<object>($"solicitudes/{s.Id}/descartar", null);
            exito = $"Se descartó el ajuste de {s.Referencia}.";
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

    protected override async Task OnInitializedAsync()
    {
        await CargarPorRegistrar();
        await Buscar();
        if (Guardado == "ajuste")
        {
            exito = $"Se registró el ajuste de inventario de {Referencia}.";
        }
    }

    private async Task Buscar()
    {
        error = null;
        if (desde is not null && hasta is not null && desde > hasta)
        {
            error = "La fecha de inicio no puede ser posterior a la fecha de fin.";
            return;
        }
        cargando = true;
        try
        {
            movimientos = await Api.Get<List<MovimientoFila>>(
                $"almacen/kardex?desde={Formato.Iso(desde)}&hasta={Formato.Iso(hasta)}&tipo={tipo}") ?? new();
            paginaActual = 1;
        }
        catch (ErrorApi e)
        {
            error = e.Message;
            movimientos ??= new();
        }
        finally
        {
            cargando = false;
        }
    }

    private void Pedir(MovimientoFila m, string cual)
    {
        error = exito = errorMotivo = null;
        accion = cual;
        motivo = cual == "motivo" ? (m.Motivo ?? "") : "";
        enAccion = m;
    }

    private async Task Confirmar()
    {
        if (string.IsNullOrWhiteSpace(motivo))
        {
            errorMotivo = "El motivo es obligatorio.";
            return;
        }
        var m = enAccion;
        enAccion = null;
        if (m is null) return;
        try
        {
            if (accion == "revertir")
            {
                try
                {
                    await Api.Post<object>($"almacen/ajustes/{m.Id}/revertir", new { motivo = motivo.Trim() });
                    exito = $"Se revirtió el ajuste de {m.CodigoInterno} · {m.Producto}.";
                }
                catch (ErrorApi e) when (e.Message.Contains("requiere autorización del Administrador"))
                {
                    // RF014: se pide la autorización de ESTA reversión; el Administrador ve el producto y el ajuste
                    await Api.Post<object>($"almacen/ajustes/{m.Id}/revertir/solicitar", new { motivo = motivo.Trim() });
                    exito = $"Se pidió la autorización para revertir el ajuste de {m.CodigoInterno}. Cuando el Administrador la apruebe, vuelve a pulsar Revertir.";
                    return;
                }
            }
            else
            {
                await Api.Put<object>($"almacen/movimientos/{m.Id}/motivo", new { motivo = motivo.Trim() });
                exito = $"Se modificó el motivo del movimiento de {m.CodigoInterno} · {m.Producto}.";
            }
            var aviso = exito;
            await Buscar();
            exito = aviso;
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "La reversión del ajuste necesita una autorización vigente del Administrador"
        }
    }
}
