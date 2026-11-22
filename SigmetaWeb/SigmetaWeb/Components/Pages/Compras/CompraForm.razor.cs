// Código de la página CompraForm (parte 1 de 2). La vista está en CompraForm.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Catalogo;
using SigmetaWeb.Components.Pages.Compras;

namespace SigmetaWeb.Components.Pages.Compras;

public partial class CompraForm
{
    [Parameter] public int? Id { get; set; }

    private bool EsNuevo => Id is null;
    private string Titulo => EsNuevo ? "Nueva compra" : "Editar compra";
    private List<Proveedor> proveedores = new();
    private List<ProductoCompraRest> productos = new();
    private List<SolicitudDetalleCompraRest> lineas = new();
    private readonly Dictionary<SolicitudDetalleCompraRest, string> textosProductos = new();
    private string numero = "";
    private int proveedorId;
    private DateTime fecha = DateTime.Today;
    private DateTime? fechaEstimada;
    private string? observaciones;
    private bool cargando;
    private bool guardando;
    private string? error;

    private decimal Importe(SolicitudDetalleCompraRest l) =>
        Math.Round(l.Cantidad * l.PrecioUnitario - l.Descuento, 2);
    private decimal SubTotal => lineas.Sum(Importe);
    private decimal Igv => Math.Round(SubTotal * 0.18m, 2, MidpointRounding.AwayFromZero);
    private decimal Total => SubTotal + Igv;

    protected override async Task OnParametersSetAsync()
    {
        cargando = true;
        error = null;
        try
        {
            proveedores = await Api.Get<List<Proveedor>>("proveedores") ?? new();
            productos = await Api.Get<List<ProductoCompraRest>>("productos") ?? new();
            if (EsNuevo)
            {
                numero = "";
                proveedorId = 0;
                fecha = DateTime.Today;
                fechaEstimada = null;
                observaciones = null;
                textosProductos.Clear();
                lineas = new() { NuevaLinea() };
                await ObtenerCorrelativo();
            }
            else
            {
                CompraRest? compra = await Api.Get<CompraRest>($"compras/{Id}");
                if (compra is null)
                {
                    error = "No se encontró la compra solicitada.";
                    return;
                }
                if (compra.Estado != "REGISTRADA" || compra.Anulado)
                {
                    error = "Solo se pueden modificar compras registradas y no anuladas.";
                    return;
                }
                numero = compra.Numero;
                proveedorId = compra.IdProveedor;
                fecha = DateTime.TryParse(compra.FechaEmision, out var f) ? f : DateTime.Today;
                fechaEstimada = DateTime.TryParse(compra.FechaRecepcionEstimada, out var fe) ? fe : null;
                observaciones = compra.Observaciones;
                textosProductos.Clear();
                lineas = compra.Detalles.Select(d => new SolicitudDetalleCompraRest
                {
                    NumeroLinea = d.NumeroLinea,
                    IdProducto = d.IdProducto,
                    Cantidad = d.Cantidad,
                    PrecioUnitario = d.PrecioUnitario,
                    Descuento = d.Descuento,
                    UnidadCompra = d.UnidadCompra,
                    FactorConversion = d.FactorConversion
                }).ToList();
                foreach (var linea in lineas)
                {
                    textosProductos[linea] = productos.FirstOrDefault(p => p.Id == linea.IdProducto) is { } p
                        ? EtiquetaProducto(p)
                        : "";
                }
            }
        }
        catch (ErrorApi e) { error = e.Message; }
        finally { cargando = false; }
    }

    private async Task AlCambiarFecha()
    {
        v.Tocado("fecha");
        if (EsNuevo)
            await ObtenerCorrelativo();
    }

    private async Task ObtenerCorrelativo()
    {
        var fechaSolicitada = fecha.Date;
        numero = "Calculando...";
        try
        {
            var propuesta = await Api.Get<CorrelativoCompraRest>(
                $"compras/siguiente-numero?fechaEmision={fechaSolicitada:yyyy-MM-dd}");
            if (fecha.Date == fechaSolicitada)
                numero = propuesta?.Numero ?? "";
        }
        catch (ErrorApi e)
        {
            if (fecha.Date == fechaSolicitada)
            {
                numero = "";
                error = e.Message;
            }
        }
    }

    private static string EtiquetaProducto(ProductoCompraRest p) =>
        $"{p.CodigoInterno} · {p.Nombre}";

    private string TextoProducto(SolicitudDetalleCompraRest linea) =>
        textosProductos.TryGetValue(linea, out var texto) ? texto : "";

    private void CambiarProducto(SolicitudDetalleCompraRest linea, string? texto)
    {
        texto = texto?.Trim() ?? "";
        textosProductos[linea] = texto;
        // No enviamos el texto al Java: solo el ID de un producto elegido de la lista.
        var producto = productos.FirstOrDefault(p => (p.Estado || p.Id == linea.IdProducto)
            && string.Equals(EtiquetaProducto(p), texto, StringComparison.OrdinalIgnoreCase));
        if (producto is null)
        {
            linea.IdProducto = 0;
            return;
        }
        if (linea.IdProducto != producto.Id)
        {
            linea.IdProducto = producto.Id;
            linea.UnidadCompra = producto.UnidadCompra;
            linea.FactorConversion = producto.FactorConversion;
            linea.PrecioUnitario = producto.CostoUnitario;
        }
    }

    private void EscribiendoProducto(SolicitudDetalleCompraRest linea, ChangeEventArgs e)
    {
        v.Tocado(Clave(linea) + "producto");
        CambiarProducto(linea, e.Value?.ToString());
    }

    private SolicitudDetalleCompraRest NuevaLinea()
    {
        var linea = new SolicitudDetalleCompraRest { Cantidad = 1, IdProducto = 0 };
        textosProductos[linea] = "";
        return linea;
    }
    private void Agregar() => lineas.Add(NuevaLinea());
    private void Quitar(SolicitudDetalleCompraRest linea)
    {
        if (lineas.Count > 1)
        {
            lineas.Remove(linea);
            textosProductos.Remove(linea);
        }
    }

    // ---------------- Validación en pantalla (RNF004) ----------------
    // Mismas reglas que CompraBOImpl. Cada error sale debajo de su cuadro; el BO vuelve a validar todo.
    private readonly ValidacionEnVivo v = new();

    // Clave de una línea para sus errores (las líneas se agregan y se quitan, por eso no se usa la posición)
    private readonly Dictionary<SolicitudDetalleCompraRest, int> claves = new();
    private int ultimaClave;
    private string Clave(SolicitudDetalleCompraRest l)
    {
        if (!claves.TryGetValue(l, out var c))
        {
            c = claves[l] = ++ultimaClave;
        }
        return $"l{c}.";
    }

    private Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (proveedorId <= 0)
        {
            e["proveedor"] = "Seleccione el proveedor.";
        }
        else if (EsNuevo && !proveedores.Any(p => p.Id == proveedorId && p.Estado))
        {
            e["proveedor"] = "El proveedor está desactivado: elija uno activo.";
        }
        if (fechaEstimada.HasValue && fechaEstimada.Value.Date < fecha.Date)
        {
            e["fechaEstimada"] = "La recepción estimada no puede ser anterior a la emisión.";
        }
        foreach (var l in lineas)
        {
            var n = Clave(l);
            var elegido = l.IdProducto > 0 && productos.Any(p => p.Id == l.IdProducto &&
                string.Equals(EtiquetaProducto(p), TextoProducto(l), StringComparison.OrdinalIgnoreCase));
            if (!elegido)
            {
                e[n + "producto"] = string.IsNullOrWhiteSpace(TextoProducto(l))
                    ? "Elija el producto."
                    : "Elija un producto de la lista.";
            }
            if (l.Cantidad <= 0)
            {
                e[n + "cantidad"] = "La cantidad debe ser mayor a 0.";
            }
            else if (ValidacionEnVivo.MasDecimales(l.Cantidad, 3))
            {
                e[n + "cantidad"] = "Máximo 3 decimales.";
            }
            if (l.PrecioUnitario < 0)
            {
                e[n + "precio"] = "El precio no puede ser negativo.";
            }
            else if (ValidacionEnVivo.MasDecimales(l.PrecioUnitario, 2))
            {
                e[n + "precio"] = "Máximo 2 decimales.";
            }
            if (l.Descuento < 0)
            {
                e[n + "descuento"] = "El descuento no puede ser negativo.";
            }
            else if (ValidacionEnVivo.MasDecimales(l.Descuento, 2))
            {
                e[n + "descuento"] = "Máximo 2 decimales.";
            }
            else if (l.Descuento > l.Cantidad * l.PrecioUnitario)
            {
                e[n + "descuento"] = "El descuento no puede ser mayor que cantidad × precio.";
            }
        }
        return e;
    }
}
