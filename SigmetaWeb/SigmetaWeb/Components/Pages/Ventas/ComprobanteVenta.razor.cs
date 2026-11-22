// Código de la página ComprobanteVenta (parte 1 de 1). La vista está en ComprobanteVenta.razor: es la misma clase (partial).
namespace SigmetaWeb.Components.Pages.Ventas;

public partial class ComprobanteVenta
{
    [Parameter] public int Id { get; set; }
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";

    // de dónde se vino (la lista de ventas con sus filtros) y si se acaba de registrar la venta
    [SupplyParameterFromQuery(Name = "volver")] public string? VolverQ { get; set; }
    [SupplyParameterFromQuery(Name = "nueva")] public bool Nueva { get; set; }
    // vuelve del formulario de la venta después de modificarla (?guardado=editado&ref=VTA-...)
    [SupplyParameterFromQuery(Name = "guardado")] public string? Guardado { get; set; }
    [SupplyParameterFromQuery(Name = "ref")] public string? Referencia { get; set; }

    [Inject] private NavigationManager Nav { get; set; } = default!;

    private VentaDatos? venta;
    private OrdenDatos? orden;
    private ComprobanteDatos? comprobante;
    private List<PasoDocumento> pasos = new();
    private bool esFactura;
    private string numeroPropuesto = "";
    private sealed class NumeroPropuesto { public string Numero { get; set; } = ""; }
    private string medioEnvio = "CORREO";
    private bool cargando = true;
    private bool trabajando;
    private string? error;
    private bool pidiendoAnulacion;
    private bool avisoNuevaMostrado;

    // aviso de éxito con el siguiente paso
    private string? exito;
    private string? exitoDetalle;
    private string? exitoAccion;
    private string? exitoRuta;
    private bool exitoImprimir;

    private string VolverA => Volver.A(VolverQ, "/ventas");
    private string RutaActual => "/" + Nav.ToBaseRelativePath(Nav.Uri);
    private string Titulo => venta is null ? "Venta" : $"Venta {venta.Numero}";
    private string Subtitulo => venta is null ? "" : $"{venta.Cliente} · {TextosVentas.Texto(venta.CondicionPago)}";

    // RUC: 11 dígitos (la factura exige RUC; el BO lo vuelve a validar con el tipo de documento real)
    private bool EsRuc => venta?.DocumentoCliente is { Length: 11 } doc && doc.All(char.IsAsciiDigit);
    private bool SinCorreo => string.IsNullOrWhiteSpace(comprobante?.CorreoCliente);
    private (string Que, string? Quien) Siguiente => venta is null ? ("", null) : FlujoVentas.Siguiente(venta);

    protected override async Task OnParametersSetAsync()
    {
        cargando = true;
        await Cargar();
        cargando = false;
        if (!avisoNuevaMostrado && venta is not null && comprobante is null)
        {
            if (Nueva)
            {
                avisoNuevaMostrado = true;
                Avisar($"Se registró la venta {venta.Numero}.", null);
            }
            else if (AvisoGuardado.Texto(Guardado, "la venta", Referencia) is { } editada)
            {
                avisoNuevaMostrado = true;
                Avisar(editada, null);
            }
        }
    }

    private async Task Cargar()
    {
        try
        {
            venta = await Api.Get<VentaDatos>($"ventas/{Id}");          // GET /api/ventas/{Id}
            comprobante = null;
            if (venta is not null && venta.IdComprobante > 0)
            {
                comprobante = await Api.Get<ComprobanteDatos>($"comprobantes/{venta.IdComprobante}");
            }
            if (venta is not null && venta.IdOrden > 0 && orden is null)
            {
                try { orden = await Api.Get<OrdenDatos>($"ordenes/{venta.IdOrden}"); }
                catch (ErrorApi) { orden = null; }   // el recorrido se muestra igual, sin la cotización
            }
            pasos = venta is null ? new() : FlujoVentas.PasosVenta(venta, orden);
            // se propone factura si el cliente tiene RUC y boleta si no
            esFactura = EsRuc;
            if (SinCorreo) medioEnvio = "MENSAJERIA";
            if (comprobante is null && venta is not null && !FlujoVentas.EstaAnulada(venta)) await ObtenerNumeroPropuesto();
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "No existe la venta con id 99"
        }
    }

    private void Avisar(string mensaje, string? detalle = null, string? accion = null, string? ruta = null, bool imprimir = false)
    {
        exito = mensaje;
        exitoDetalle = detalle;
        exitoAccion = accion;
        exitoRuta = ruta;
        exitoImprimir = imprimir;
    }

    private void CerrarAviso() => Avisar(null!);

    private async Task ElegirFactura()
    {
        if (!EsRuc) return;
        esFactura = true;
        await ObtenerNumeroPropuesto();
    }

    private async Task ElegirBoleta()
    {
        esFactura = false;
        await ObtenerNumeroPropuesto();
    }

    private async Task ObtenerNumeroPropuesto()
    {
        string serie = esFactura ? "F001" : "B001";
        numeroPropuesto = "";
        try
        {
            var propuesta = await Api.Get<NumeroPropuesto>($"comprobantes/siguiente-numero?serie={serie}");
            if (serie == (esFactura ? "F001" : "B001"))
                numeroPropuesto = propuesta?.Numero ?? "";
        }
        catch (ErrorApi e) { error = e.Message; }
    }

    // POST /api/comprobantes {"idVenta", "tipo"}
    private async Task Emitir()
    {
        error = null;
        CerrarAviso();
        trabajando = true;
        try
        {
            var emitido = await Api.Post<ComprobanteDatos>("comprobantes",
                new ComprobanteDatos { IdVenta = Id, Tipo = esFactura ? "FACTURA" : "BOLETA" });
            await Cargar();
            Avisar($"Se emitió la {TextosVentas.Texto(emitido?.Tipo).ToLowerInvariant()} {emitido?.Comprobante}.", null,
                "Descargar PDF", imprimir: true);
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "La FACTURA requiere que el cliente cuente con RUC (cliente tiene: DNI)"
        }
        finally
        {
            trabajando = false;
        }
    }

    private void PedirAnulacion()
    {
        error = null;
        CerrarAviso();
        pidiendoAnulacion = true;
    }

    // POST /api/comprobantes/{id}/anular {"motivo"}
    private async Task ConfirmarAnulacion(string motivo)
    {
        if (comprobante is null) return;
        var anulado = comprobante.Comprobante;
        trabajando = true;
        try
        {
            await Api.Post<object>($"comprobantes/{comprobante.Id}/anular", new AnulacionDatos { Motivo = motivo });
            pidiendoAnulacion = false;
            await Cargar();
            if (Rol == "Vendedor")
            {
                Avisar($"Se anuló el comprobante {anulado}.", "Ahora puede corregir la venta y luego emitir un comprobante nuevo.",
                    "Editar venta", $"/ventas/editar/{Id}?volver={Uri.EscapeDataString(RutaActual)}");
            }
            else
            {
                Avisar($"Se anuló el comprobante {anulado}.", "Puede emitir uno nuevo. La venta solo la corrige o anula el Vendedor.");
            }
        }
        catch (ErrorApi e)
        {
            pidiendoAnulacion = false;
            error = e.Message;   // por ejemplo "La venta ya tiene mercadería despachada: para devolverla emita una nota de crédito..."
        }
        finally
        {
            trabajando = false;
        }
    }

    // RF008: formato imprimible. El PDF lo genera el Java con JasperReports (GET /api/comprobantes/{id}/pdf)
    // y se descarga directo; después se registra la entrega con medio IMPRESO y su fecha.
    private async Task Imprimir()
    {
        if (comprobante is null) return;
        error = null;
        trabajando = true;
        try
        {
            var pdf = await Api.GetArchivo($"comprobantes/{comprobante.Id}/pdf");
            await JS.InvokeVoidAsync("sigmetaDescargar", $"{comprobante.Comprobante}.pdf", "application/pdf",
                Convert.ToBase64String(pdf));
            await Api.Post<ComprobanteDatos>($"comprobantes/{comprobante.Id}/envio",
                new ComprobanteDatos { MedioEnvio = "IMPRESO" });
            await Cargar();
            Avisar($"Se descargó el PDF de {comprobante.Comprobante} y se registró la entrega en formato imprimible.", null);
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "El comprobante está anulado"
        }
        catch (JSException)
        {
            error = "El PDF se generó, pero el navegador no pudo descargarlo. Vuelva a intentarlo.";
        }
        finally
        {
            trabajando = false;
        }
    }

    // POST /api/comprobantes/{id}/envio {"medioEnvio"}
    private async Task RegistrarEnvio()
    {
        if (comprobante is null) return;
        error = null;
        CerrarAviso();
        trabajando = true;
        try
        {
            await Api.Post<ComprobanteDatos>($"comprobantes/{comprobante.Id}/envio",
                new ComprobanteDatos { MedioEnvio = medioEnvio });
            await Cargar();
            Avisar($"Se registró el envío por {TextosVentas.Texto(medioEnvio).ToLowerInvariant()}.",
                null);
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "El cliente no tiene teléfono de contacto registrado..."
        }
        finally
        {
            trabajando = false;
        }
    }
}
