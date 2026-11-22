// Código de la página OrdenForm (parte 1 de 1). La vista está en OrdenForm.razor: es la misma clase (partial).
namespace SigmetaWeb.Components.Pages.Ventas;

public partial class OrdenForm
{
    [Parameter] public int? Id { get; set; }

    // viene de la lista de cotizaciones: /ordenes/nueva?cotizacion=ID
    [SupplyParameterFromQuery(Name = "cotizacion")] public int? IdCotizacionInicial { get; set; }
    [SupplyParameterFromQuery(Name = "volver")] public string? VolverQ { get; set; }
    // recién generada: /ordenes/editar/5?nueva=true muestra el siguiente paso
    [SupplyParameterFromQuery(Name = "nueva")] public bool Nueva { get; set; }

    private bool EsNuevo => Id is null;
    private string Titulo => EsNuevo ? "Nueva orden de compra del cliente" : $"Orden {orden?.Numero}";
    private string? Subtitulo => EsNuevo ? null : orden?.Cliente;
    private string VolverA => Volver.A(VolverQ, "/ordenes");
    private static DateOnly Hoy => DateOnly.FromDateTime(DateTime.Today);
    // RF007: solo el Vendedor modifica la orden (el Administrador la ve y puede anularla desde la lista)
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";
    private bool EditablePendiente => orden?.Estado == "PENDIENTE" && !orden.Anulado && Rol == "Vendedor";

    private List<CotizacionDatos> cotizacionesLibres = new();
    private int idCotizacion;
    private CotizacionDatos? cotizacion;
    private string? numeroOrdenCliente;
    private string siguienteNumeroOrden = "";
    private sealed class NumeroPropuesto { public string Numero { get; set; } = ""; }

    private OrdenDatos? orden;

    private bool cargando = true;
    private bool guardando;
    private string? error;
    private string? exito;
    private string? exitoDetalle;
    private string? exitoAccion;
    private string? exitoRuta;

    private int? idCargado;
    private bool primeraCarga = true;

    // /ordenes/nueva y /ordenes/editar/{Id} son esta misma pantalla: al generar una orden se pasa de una a la otra
    // SIN crear el componente de nuevo (OnInitializedAsync no se repite). Por eso se carga cada vez que cambia el Id;
    // si no, después de "Generar orden" la pantalla quedaba vacía.
    protected override async Task OnParametersSetAsync()
    {
        if (!primeraCarga && Id == idCargado) return;
        primeraCarga = false;
        idCargado = Id;
        cargando = true;
        orden = null;
        error = null;
        exito = null;
        exitoAccion = null;
        exitoRuta = null;
        await Cargar();
    }

    private async Task Cargar()
    {
        try
        {
            if (EsNuevo)
            {
                // cotizaciones aceptadas que aún no tienen una orden vigente (una cotización tiene una sola orden activa)
                var aceptadas = await Api.Get<List<CotizacionDatos>>("cotizaciones?estado=ACEPTADA") ?? new();
                var ordenes = await Api.Get<List<OrdenDatos>>("ordenes") ?? new();
                // H09: el número sale del correlativo del Java (GET /api/ordenes/siguiente-numero), no de contar órdenes
                var propuesto = await Api.Get<NumeroPropuesto>("ordenes/siguiente-numero");
                siguienteNumeroOrden = propuesto?.Numero ?? "";
                var conOrden = ordenes.Where(o => o.Estado != "ANULADA" && !o.Anulado).Select(o => o.IdCotizacion).ToHashSet();
                cotizacionesLibres = aceptadas.Where(c => !conOrden.Contains(c.Id)).ToList();

                if (IdCotizacionInicial is int inicial && cotizacionesLibres.Any(c => c.Id == inicial))
                {
                    await ElegirCotizacion(inicial);
                }
                else if (cotizacionesLibres.Count == 1)
                {
                    await ElegirCotizacion(cotizacionesLibres[0].Id);   // si hay una sola, ya queda elegida
                }
            }
            else
            {
                orden = await Api.Get<OrdenDatos>($"ordenes/{Id}");   // GET /api/ordenes/{Id}
                if (Nueva && orden is not null)
                {
                    exito = $"Se generó la orden {orden.Numero}.";
                    if (Rol == "Vendedor")
                    {
                        exitoAccion = "Registrar venta";
                        exitoRuta = $"/ventas/nueva?orden={orden.Id}";
                    }
                }
            }
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        cargando = false;
    }

    // ---------------- Validación en pantalla (RNF004) ----------------
    private readonly ValidacionEnVivo v = new();

    private Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (EsNuevo && idCotizacion <= 0)
        {
            e["cotizacion"] = "Elija la cotización aceptada de la que sale la orden.";
        }
        return e;
    }

    private async Task CambiarCotizacion(ChangeEventArgs e)
    {
        v.Tocado("cotizacion");
        var id = int.TryParse(e.Value?.ToString(), out var valor) ? valor : 0;
        await ElegirCotizacion(id);
    }

    // GET /api/cotizaciones/{id}: para mostrar el detalle que se va a copiar
    private async Task ElegirCotizacion(int id)
    {
        idCotizacion = id;
        cotizacion = null;
        if (id <= 0) return;
        try
        {
            cotizacion = await Api.Get<CotizacionDatos>($"cotizaciones/{id}");
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }

    // POST /api/ordenes {"idCotizacion", "numeroOrdenCliente"}
    private async Task Generar()
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
            var nueva = await Api.Post<OrdenDatos>("ordenes", new OrdenDatos { IdCotizacion = idCotizacion, NumeroOrdenCliente = numeroOrdenCliente });
            // siguiente paso: la orden recién generada, con el botón para registrar su venta
            Nav.NavigateTo(nueva is not null ? $"/ordenes/editar/{nueva.Id}?nueva=true" : Volver.ConAviso(VolverA, AvisoGuardado.Nuevo, null));
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "Ya existe una orden de compra activa para la cotizacion ..."
        }
        finally
        {
            guardando = false;
        }
    }

    // PUT /api/ordenes/{Id}: número del cliente y observaciones
    private async Task GuardarCambios()
    {
        error = null;
        guardando = true;
        try
        {
            await Api.Put<OrdenDatos>($"ordenes/{Id}", orden);
            Nav.NavigateTo(Volver.ConAviso(VolverA, AvisoGuardado.Editado, orden?.Numero));
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "Solo se puede modificar una orden de compra en estado PENDIENTE"
        }
        finally
        {
            guardando = false;
        }
    }
}
