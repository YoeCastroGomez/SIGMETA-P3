// Código de la página NotaCreditoForm (parte 1 de 1). La vista está en NotaCreditoForm.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Almacen;
using SigmetaWeb.Components.Pages.Seguridad;

namespace SigmetaWeb.Components.Pages.Ventas;

public partial class NotaCreditoForm
{
    private const double TasaIgv = 0.18;

    // viene del comprobante de la venta: /notas-credito/nueva?comprobante=ID
    [SupplyParameterFromQuery(Name = "comprobante")] public int? IdComprobanteInicial { get; set; }

    private string? estadoAutorizacion;                 // RF014: null, PENDIENTE, APROBADA, RECHAZADA o VENCIDA
    private List<ComprobanteParaNota> comprobantes = new();
    private List<LineaDevolvible> lineas = new();
    private int idComprobante;
    private string motivo = "";
    private bool cargado;
    private bool intento;
    private bool guardando;
    private string? error;
    private string? exito;

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

    private List<string> Faltan => new List<(string Texto, bool Listo)>
        {
            ("Elegir el comprobante", Origen is not null),
            ("Escribir el motivo", !string.IsNullOrWhiteSpace(motivo)),
            ("Indicar qué se devuelve", CantidadesValidas)
        }.Where(f => !f.Listo).Select(f => f.Texto).ToList();

    // Recorrido: preparar -> autorización (la da el Administrador) -> registrar
    private List<PasoDocumento> Pasos => new()
    {
        new("Preparar la nota", Preparada ? EstadoPaso.Hecho : EstadoPaso.Actual, Origen?.Comprobante),
        new("Autorización", Autorizada ? EstadoPaso.Hecho : Preparada ? EstadoPaso.Actual : EstadoPaso.Pendiente,
            estadoAutorizacion == "PENDIENTE" ? "En espera" : null, estadoAutorizacion == "PENDIENTE" ? "Administrador" : null),
        new("Registrar", Preparada && Autorizada ? EstadoPaso.Actual : EstadoPaso.Pendiente)
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

    protected override async Task OnInitializedAsync()
    {
        try
        {
            comprobantes = await Api.Get<List<ComprobanteParaNota>>("notas-credito/comprobantes") ?? new();
            if (IdComprobanteInicial is int inicial && comprobantes.Any(c => c.Id == inicial))
            {
                idComprobante = inicial;
                await AlElegirComprobante();
            }
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        cargado = true;
    }

    // RF014: pide la autorización de ESTA nota con sus datos (mismo cuerpo que registrarla)
    private Task<SolicitudDatos?> SolicitarAutorizacion()
    {
        intento = true;
        error = null;
        return Api.Post<SolicitudDatos>("notas-credito/solicitar", Cuerpo());
    }

    private object Cuerpo() => new
    {
        idComprobanteOrigen = idComprobante,
        motivo = motivo.Trim(),
        lineas = lineas.Where(l => l.Devolver > 0).Select(l => new { idLinea = l.IdDetalleVenta, cantidad = l.Devolver })
    };

    private async Task AlElegirComprobante()
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
            error = e.Message;   // por ejemplo "Para emitir la nota de crédito se necesita una autorización vigente del Administrador"
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
