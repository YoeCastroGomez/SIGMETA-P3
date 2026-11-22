// Código de la página MovimientosCaja (parte 1 de 1). La vista está en MovimientosCaja.razor: es la misma clase (partial).
namespace SigmetaWeb.Components.Pages.Caja;

public partial class MovimientosCaja
{
    // UI/UX: una acción visible por fila y el resto en «⋯» (mismo patrón en todas las listas)
    private Task Ejecutar(AccionFila a) => a.Hacer?.Invoke() ?? Task.CompletedTask;

    private int paginaActual = 1;
    private int tamanoPagina = 10;
    private IEnumerable<T> Paginar<T>(IEnumerable<T>? registros, int pagina, int tamano)
    {
        if (registros is null) return Enumerable.Empty<T>();
        if (tamano == 0) return registros;
        int total = registros.Count();
        int paginaSegura = Math.Clamp(pagina, 1, Math.Max(1, (total + tamano - 1) / tamano));
        return registros.Skip((paginaSegura - 1) * tamano).Take(tamano);
    }

    private CajaDatos? caja;
    private List<MovimientoDatos> movimientos = new();
    private MovimientoDatos? formulario;   // null = formulario cerrado; Id 0 = nuevo
    private bool cargando = true;
    private bool guardando;
    private string? error;
    private string? exito;

    protected override async Task OnInitializedAsync()
    {
        await Cargar();
        cargando = false;
    }

    private async Task Cargar()
    {
        try
        {
            caja = await Api.Get<CajaDatos>("cajas/abierta");
            if (caja is not null)
            {
                movimientos = await Api.Get<List<MovimientoDatos>>($"cajas/{caja.Id}/movimientos") ?? new();
            }
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }

    // ---------------- Validación en pantalla (RNF004) ----------------
    // Monto mayor a 0 y concepto obligatorio (igual que el BO). Lo demás (efectivo suficiente, caja abierta) es del BO.
    private readonly ValidacionEnVivo v = new();

    private Dictionary<string, string> Errores()
    {
        var e = new Dictionary<string, string>();
        if (formulario is null)
        {
            return e;
        }
        if (formulario.Monto <= 0)
        {
            e["monto"] = "El monto debe ser mayor a 0.";
        }
        else if (ValidacionEnVivo.MasDecimales(formulario.Monto, 2))
        {
            e["monto"] = "Máximo 2 decimales.";
        }
        if (string.IsNullOrWhiteSpace(formulario.Concepto))
        {
            e["concepto"] = "El concepto es obligatorio.";
        }
        return e;
    }

    private void Nuevo()
    {
        exito = null;
        v.Reiniciar();
        formulario = new MovimientoDatos();
    }

    private void Editar(MovimientoDatos m)
    {
        exito = null;
        v.Reiniciar();
        formulario = new MovimientoDatos { Id = m.Id, Tipo = m.Tipo, MedioPago = m.MedioPago, Monto = m.Monto, Concepto = m.Concepto };
    }

    // las validaciones (monto mayor a 0, concepto, efectivo suficiente para el egreso, caja abierta) son del BO
    private async Task Guardar()
    {
        if (caja is null || formulario is null) return;
        error = null;
        exito = null;
        if (!v.Intentar(Errores()))
        {
            error = "Revise los campos marcados en rojo.";
            return;
        }
        guardando = true;
        try
        {
            var datos = new { formulario.Tipo, formulario.MedioPago, formulario.Monto, formulario.Concepto };
            if (formulario.Id == 0)
            {
                await Api.Post<MovimientoDatos>($"cajas/{caja.Id}/movimientos", datos);
                exito = $"Se registró el {TextosCaja.Texto(formulario.Tipo).ToLower()} de S/ {formulario.Monto:N2}.";
            }
            else
            {
                await Api.Put<MovimientoDatos>($"cajas/{caja.Id}/movimientos/{formulario.Id}", datos);
                exito = "Se corrigió el concepto del movimiento.";
            }
            formulario = null;
            await Cargar();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        guardando = false;
    }

    private MovimientoDatos? porAnular;   // movimiento esperando la confirmación

    private void PedirAnular(MovimientoDatos m)
    {
        error = null;
        exito = null;
        porAnular = m;
    }

    // RF010, RNF002: no se borra; el Java registra el movimiento contrario en la misma caja
    private async Task ConfirmarAnular()
    {
        var m = porAnular;
        porAnular = null;
        if (caja is null || m is null) return;
        try
        {
            await Api.Post<MovimientoDatos>($"cajas/{caja.Id}/movimientos/{m.Id}/anular", new { });
            exito = $"Se anuló el movimiento \"{m.Concepto}\": quedó registrado su {(m.EsEgreso ? "ingreso" : "egreso")} contrario.";
            await Cargar();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
    }
}
