// Código de la página ProductoForm (parte 1 de 1). La vista está en ProductoForm.razor: es la misma clase (partial).
namespace SigmetaWeb.Components.Pages.Catalogo;

public partial class ProductoForm
{
    [Parameter] public int? Id { get; set; }

    private Producto? producto;
    private List<Categoria> categorias = new();
    private string? error;
    private bool guardando;

    private bool EsNuevo => Id is null;
    private string Titulo => EsNuevo ? "Nuevo producto" : "Editar producto";

    private sealed class CodigoPropuesto
    {
        public string Codigo { get; set; } = "";
    }

    private string AyudaFactor => MismaUnidad
        ? "Las dos unidades son iguales: el factor es 1 y no se puede cambiar."
        : "Unidades de venta en una unidad de compra (caja de 12 → 12). Entero si se vende por unidad, caja o juego.";

    // Si se compra y se vende en la misma unidad no hay conversión: el factor queda fijo en 1
    private bool MismaUnidad => producto is not null && producto.UnidadCompra == producto.UnidadVenta;

    // Producto desactivado abierto por su dirección (/productos/editar/5): solo se consulta
    private bool SoloLectura => !EsNuevo && producto is not null && !producto.Activo;

    // Unidades distintas con factor 1 ("1 caja trae 1 unidad"): casi siempre se olvidó poner el factor.
    // Es solo una advertencia: se puede guardar igual.
    private string? AdvertenciaFactor
    {
        get
        {
            if (producto is null || MismaUnidad || Numero("factor", producto.FactorConversion) != 1)
            {
                return null;
            }
            var compra = Producto.TextoUnidad(producto.UnidadCompra).ToLowerInvariant();
            var venta = Producto.TextoUnidad(producto.UnidadVenta).ToLowerInvariant();
            return $"Con factor 1, cada {compra} trae solo 1 {venta}. ¿Es correcto? Por ejemplo, una caja de 12 lleva factor 12.";
        }
    }

    private void AjustarFactor()
    {
        if (MismaUnidad)
        {
            producto!.FactorConversion = 1;
            escrito.Remove("factor");   // lo escrito antes en el cuadro ya no vale: el factor es 1
        }
    }

    // ---------------- Validación en pantalla ----------------
    // Mismas reglas que el BO (ProductoBOImpl.validarDatos), para avisar antes de enviar. Cada error sale en rojo
    // debajo de su cuadro (Campo Error=...) mientras se escribe. Un cuadro que todavía no se tocó recién se marca
    // al pulsar Guardar. El BO vuelve a validar todo (y lo que solo él sabe, como un código repetido).

    // Lo que se está escribiendo en cada cuadro: InputText e InputNumber recién pasan el valor al salir del cuadro,
    // así que se guarda aquí para validar al momento.
    private readonly Dictionary<string, string?> escrito = new();
    private bool intentoGuardar;

    private void Escribiendo(string campo, ChangeEventArgs e) => escrito[campo] = e.Value?.ToString();

    private string Texto(string campo, string? valorModelo) =>
        escrito.TryGetValue(campo, out var t) ? t ?? "" : valorModelo ?? "";

    private decimal Numero(string campo, decimal valorModelo) =>
        !escrito.TryGetValue(campo, out var t) ? valorModelo
        : decimal.TryParse(t, System.Globalization.NumberStyles.Number,
                           System.Globalization.CultureInfo.InvariantCulture, out var v) ? v : 0;

    // UNIDAD, CAJA y JUEGO no se parten: factor y stock mínimo enteros (igual que el BO)
    private bool VentaEntera => producto is not null && producto.UnidadVenta is "UNIDAD" or "CAJA" or "JUEGO";

    private static bool MasDecimales(decimal valor, int maximo) => valor != Math.Round(valor, maximo);

    private Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (producto is null)
        {
            return e;
        }
        if (string.IsNullOrWhiteSpace(Texto("nombre", producto.Nombre)))
        {
            e["nombre"] = "El nombre es obligatorio.";
        }
        if (producto.Categoria.Id == 0)
        {
            e["categoria"] = "Elija la categoría del producto.";
        }

        var unidad = Producto.TextoUnidad(producto.UnidadVenta).ToLowerInvariant();
        var factor = Numero("factor", producto.FactorConversion);
        if (factor <= 0)
        {
            e["factor"] = "El factor de conversión debe ser mayor a 0.";
        }
        else if (MasDecimales(factor, 3))
        {
            e["factor"] = "El factor de conversión admite como máximo 3 decimales.";
        }
        else if (VentaEntera && MasDecimales(factor, 0))
        {
            e["factor"] = $"Debe ser un número entero: la unidad de venta ({unidad}) no se parte.";
        }

        var precio = Numero("precio", producto.PrecioVenta);
        var costo = Numero("costo", producto.CostoUnitario);
        if (precio <= 0)
        {
            e["precio"] = "El precio de venta debe ser mayor a 0.";
        }
        else if (MasDecimales(precio, 2))
        {
            e["precio"] = "El precio de venta admite como máximo 2 decimales.";
        }
        else if (precio < costo)
        {
            e["precio"] = $"No se puede: el precio de venta (S/ {precio:N2}) es menor que el costo (S/ {costo:N2}).";
        }
        if (costo < 0)
        {
            e["costo"] = "El costo unitario no puede ser negativo.";
        }
        else if (MasDecimales(costo, 2))
        {
            e["costo"] = "El costo unitario admite como máximo 2 decimales.";
        }

        var referencial = Numero("precioRef", producto.PrecioReferencial);
        if (referencial <= 0)
        {
            e["precioRef"] = "El precio referencial es obligatorio y debe ser mayor a 0.";
        }
        else if (MasDecimales(referencial, 2))
        {
            e["precioRef"] = "El precio referencial admite como máximo 2 decimales.";
        }

        var stockMinimo = Numero("stockMinimo", producto.StockMinimo);
        if (stockMinimo < 0)
        {
            e["stockMinimo"] = "El stock mínimo no puede ser negativo.";
        }
        else if (MasDecimales(stockMinimo, 3))
        {
            e["stockMinimo"] = "El stock mínimo admite como máximo 3 decimales.";
        }
        else if (VentaEntera && MasDecimales(stockMinimo, 0))
        {
            e["stockMinimo"] = $"Debe ser un número entero: la unidad de venta ({unidad}) no se parte.";
        }
        return e;
    }

    // El error de un campo se muestra si ya se escribió en él o si se pulsó Guardar.
    // El de precio también al cambiar el costo (es la comparación "precio menor que el costo").
    private string? Error(string campo)
    {
        var mostrar = intentoGuardar || escrito.ContainsKey(campo) || (campo == "precio" && escrito.ContainsKey("costo"));
        return mostrar && Errores().TryGetValue(campo, out var mensaje) ? mensaje : null;
    }

    private string ClaseCuadro(string campo, string clase = "form-control") =>
        Error(campo) is null ? clase : clase + " is-invalid";

    // Evita escribir "-", "+" y "e" en los campos de números: no se pueden poner negativos desde el teclado
    // (el mínimo de cada campo también lo controla el navegador y el modelo con [Range]).
    private const string SoloPositivos = "if (['-', '+', 'e', 'E'].includes(event.key)) { event.preventDefault(); }";

    // Sube la página hasta el aviso (el formulario es largo y el botón Guardar queda abajo)
    private async Task SubirAlAviso()
    {
        try
        {
            await JS.InvokeVoidAsync("window.scrollTo", 0, 0);
        }
        catch (Exception)
        {
            // si no se puede subir no pasa nada: el aviso igual está en pantalla
        }
    }

    protected override async Task OnInitializedAsync()
    {
        try
        {
            categorias = await Api.Get<List<Categoria>>("categorias") ?? new List<Categoria>();   // GET /api/categorias
            producto = EsNuevo
                ? new Producto()
                : await Api.Get<Producto>($"productos/{Id}");
            if (EsNuevo && producto is not null)
            {
                var propuesto = await Api.Get<CodigoPropuesto>("productos/siguiente-codigo");
                producto.CodigoInterno = propuesto?.Codigo ?? "";
            }
            AjustarFactor();   // si las unidades son iguales, el factor se muestra fijo en 1                                       // GET /api/productos/{Id}
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // por ejemplo "No existe el producto con id 99"
        }
    }

    private async Task Guardar()
    {
        error = null;
        if (SoloLectura)
        {
            return;   // desactivado: no se modifica (el formulario ya está bloqueado)
        }
        intentoGuardar = true;   // desde ahora se marcan en rojo también los cuadros que no se tocaron
        if (Errores().Count > 0)
        {
            error = "Revise los campos marcados en rojo.";
            await SubirAlAviso();
            return;
        }

        AjustarFactor();
        guardando = true;
        // quién y cuándo modificó lo pone el BO con el usuario de la sesión: no se manda
        var usuarioAnterior = producto!.UsuarioModificacion;
        var fechaAnterior = producto.FechaModificacion;
        producto.UsuarioModificacion = null;
        producto.FechaModificacion = null;
        try
        {
            if (EsNuevo)
            {
                var registrado = await Api.Post<Producto>("productos", producto);          // POST /api/productos
                if (registrado is not null) producto.CodigoInterno = registrado.CodigoInterno;
            }
            else
            {
                await Api.Put<Producto>($"productos/{Id}", producto);     // PUT /api/productos/{Id}
            }
            // vuelve a la lista y allí sale el aviso verde (Productos.razor lee "guardado")
            Nav.NavigateTo(AvisoGuardado.Ruta("/productos", EsNuevo ? AvisoGuardado.Nuevo : AvisoGuardado.Editado,
                $"{producto.CodigoInterno?.Trim().ToUpperInvariant()} · {producto.Nombre?.Trim()}"));
        }
        catch (ErrorApi e)
        {
            error = e.Message;   // mensaje del BO, por ejemplo "Ya existe un producto con el codigo INT-001"
            // si no se guardó, se vuelve a mostrar la última modificación que tenía
            producto.UsuarioModificacion = usuarioAnterior;
            producto.FechaModificacion = fechaAnterior;
            await SubirAlAviso();   // el aviso rojo está arriba y el botón Guardar abajo: se sube para verlo
        }
        catch (Exception)
        {
            // cualquier otro error (por ejemplo al leer la respuesta del Java) también sale en el aviso rojo
            error = "No se pudo guardar el producto. Inténtalo otra vez; si el problema continúa, avisa al Administrador.";
            producto.UsuarioModificacion = usuarioAnterior;
            producto.FechaModificacion = fechaAnterior;
            await SubirAlAviso();
        }
        finally
        {
            guardando = false;
        }
    }
}
