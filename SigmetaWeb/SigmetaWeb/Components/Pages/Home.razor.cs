// Código de la página Home (parte 1 de 2). La vista está en Home.razor: es la misma clase (partial).
using System.Globalization;
using SigmetaWeb.Components.Pages.Compras;
using SigmetaWeb.Components.Pages.Catalogo;
using SigmetaWeb.Components.Pages.Clientes;
using SigmetaWeb.Components.Pages.Caja;
using SigmetaWeb.Components.Pages.Ventas;

namespace SigmetaWeb.Components.Pages;

public partial class Home
{
    [CascadingParameter(Name = "Rol")] public string Rol { get; set; } = "";
    internal static readonly CultureInfo Cultura = new("es-PE");

    internal record Kpi(string Titulo, string Valor, string Detalle, string Icono, string Ruta);
    internal record Acceso(string Icono, string Titulo, string Descripcion, string Ruta);
    internal record Dato(string Etiqueta, decimal Valor, string Clase = "");
    internal record PuntoGrafico(string X, string Y, string Etiqueta, decimal Valor);

    internal bool EsAdministrador => Rol == "Administrador";
    internal bool EsComercial => Rol is "Administrador" or "Vendedor";
    internal bool cargando;
    internal string? rolCargado;
    internal readonly List<string> Errores = new();

    internal List<VentaDatos>? ventas;
    internal List<TopProducto> topProductos = new();
    internal List<CotizacionDatos>? cotizaciones;
    internal List<OrdenDatos>? ordenes;
    internal List<Cliente>? clientes;
    internal List<Producto>? productos;
    internal List<CobroDatos>? cobros;
    internal List<CuentaDatos>? cuentas;
    internal CajaDatos? cajaAbierta;
    internal bool cajaConsultada;

    internal static DateOnly Hoy => DateOnly.FromDateTime(DateTime.Today);
    internal static bool VentaVigente(VentaDatos venta) => !venta.Anulado && venta.Estado != "ANULADA";
    internal static decimal Porcentaje(decimal valor, decimal maximo) => maximo <= 0 ? 0 : Math.Clamp(valor * 100m / maximo, 0, 100);
    internal static string Dinero(decimal valor) => "S/ " + valor.ToString("N2", Cultura);
    internal static string Dinero(decimal? valor) => valor.HasValue ? Dinero(valor.Value) : "—";
    internal static string TextoConteo(int? valor) => valor?.ToString("N0", Cultura) ?? "—";
    internal static string TextoFecha(DateOnly? fecha) => fecha?.ToString("dd/MM/yyyy") ?? "—";

    internal string DescripcionRol => Rol switch
    {
        "Administrador" => "Supervisa las ventas, las compras y el desempeño del negocio con información actualizada.",
        "Vendedor" => "Tus ventas, cotizaciones y órdenes de clientes desde un solo lugar.",
        "Cajero" => "Consulta los cobros, saldos y el estado de la caja de tu jornada.",
        "Almacenero" => "Controla el catálogo, las existencias y los productos que necesitan reposición.",
        _ => "Tu espacio de trabajo en SIGMETA."
    };

    internal string TituloResumen => Rol switch
    {
        "Administrador" => "Resumen ejecutivo",
        "Vendedor" => "Panorama comercial",
        "Cajero" => "Panorama de caja y cobranza",
        "Almacenero" => "Panorama de inventario",
        _ => "Resumen"
    };

    internal decimal TotalMes(int mesesAtras) => ventas?.Where(v => VentaVigente(v) && v.FechaEmision.HasValue &&
            v.FechaEmision.Value.Year == Hoy.AddMonths(-mesesAtras).Year &&
            v.FechaEmision.Value.Month == Hoy.AddMonths(-mesesAtras).Month)
        .Sum(v => v.Total) ?? 0;

    internal int? VentasMesCantidad => ventas is null ? null :
        ventas.Count(v => VentaVigente(v) && v.FechaEmision.HasValue &&
                          v.FechaEmision.Value.Year == Hoy.Year && v.FechaEmision.Value.Month == Hoy.Month);

    internal string CrecimientoMes
    {
        get
        {
            if (ventas is null) return "—";
            var anterior = TotalMes(1);
            if (anterior <= 0) return "Sin base";
            var variacion = (TotalMes(0) - anterior) * 100m / anterior;
            return variacion.ToString("+0.0;-0.0;0.0", Cultura) + "%";
        }
    }

    internal decimal? TicketPromedio => ventas is null ? null :
        VentasMesCantidad > 0 ? TotalMes(0) / VentasMesCantidad.Value : 0m;

    internal int? CotizacionesActivas => cotizaciones is null ? null :
        cotizaciones.Count(c => !c.Anulado && c.Estado is "REGISTRADA" or "ENVIADA");

    internal int? OrdenesAbiertas => ordenes is null ? null :
        ordenes.Count(o => !o.Anulado && o.Estado is "PENDIENTE" or "ATENDIDA_PARCIAL");

    internal int? CotizacionesPorVencer => cotizaciones is null ? null :
        cotizaciones.Count(c => !c.Anulado && c.Estado is "REGISTRADA" or "ENVIADA" &&
                               c.FechaVigencia.HasValue && c.FechaVigencia.Value >= Hoy &&
                               c.FechaVigencia.Value <= Hoy.AddDays(3));

    internal int? OrdenesAtrasadas => ordenes is null ? null :
        ordenes.Count(o => !o.Anulado && o.Estado is "PENDIENTE" or "ATENDIDA_PARCIAL" &&
                           o.FechaEmision.HasValue && o.FechaEmision.Value < Hoy.AddDays(-2));

    internal List<Kpi> Indicadores => Rol switch
    {
        "Administrador" => new()
        {
            new("Ventas del mes", Dinero(ventas is null ? null : TotalMes(0)), "Ventas vigentes del mes", "VE", "/ventas"),
            new("Crecimiento mensual", CrecimientoMes, "Respecto al mes anterior", "↗", "/ventas"),
            new("Cotizaciones activas", TextoConteo(CotizacionesActivas), "Registradas o enviadas", "CT", "/cotizaciones"),
            new("Órdenes abiertas", TextoConteo(OrdenesAbiertas), "Pendientes de atención", "OC", "/ordenes"),
            new("Clientes activos", TextoConteo(clientes?.Count(c => c.Activo)), "Clientes habilitados", "CL", "/clientes"),
            new("Ticket promedio", Dinero(TicketPromedio), "Promedio de ventas del mes", "S/", "/ventas")
        },
        "Vendedor" => new()
        {
            new("Ventas de hoy", Dinero(ventas is null ? null : ventas.Where(v => VentaVigente(v) && v.FechaEmision == Hoy).Sum(v => v.Total)), "Importe de la jornada", "VE", "/ventas"),
            new("Ventas del mes", Dinero(ventas is null ? null : TotalMes(0)), "Importe acumulado", "S/", "/ventas"),
            new("Cotizaciones activas", TextoConteo(CotizacionesActivas), "Por gestionar", "CT", "/cotizaciones"),
            new("Órdenes abiertas", TextoConteo(OrdenesAbiertas), "Pendientes de atención", "OC", "/ordenes"),
            new("Clientes activos", TextoConteo(clientes?.Count(c => c.Activo)), "Clientes habilitados", "CL", "/clientes"),
            new("Ticket promedio", Dinero(TicketPromedio), "Valor medio de las ventas", "↗", "/ventas")
        },
        "Cajero" => new()
        {
            new("Caja actual", cajaConsultada ? cajaAbierta is null ? "Sin abrir" : "Abierta" : "—", "Estado de la jornada", "CJ", "/caja"),
            new("Cobrado hoy", Dinero(cobros is null ? null : cobros.Where(c => !c.Anulado && c.FechaCobro == Hoy).Sum(c => c.Monto)), "Cobros vigentes de hoy", "S/", "/cobros"),
            new("Cobrado este mes", Dinero(cobros is null ? null : cobros.Where(c => !c.Anulado && c.FechaCobro.HasValue && c.FechaCobro.Value.Year == Hoy.Year && c.FechaCobro.Value.Month == Hoy.Month).Sum(c => c.Monto)), "Cobros del mes", "CB", "/cobros"),
            new("Saldo pendiente", Dinero(cuentas?.Sum(c => Math.Max(0, c.SaldoPendiente))), "Cuentas por cobrar", "CC", "/cobranzas"),
            new("Cuentas vencidas", TextoConteo(CuentasVencidas), "Con saldo sin pagar", "!", "/cobranzas"),
            new("Ventas al contado", Dinero(ventas is null ? null : ventas.Where(v => VentaVigente(v) && v.CondicionPago == "CONTADO" && v.FechaEmision.HasValue && v.FechaEmision.Value.Year == Hoy.Year && v.FechaEmision.Value.Month == Hoy.Month).Sum(v => v.Total)), "Registradas en el mes, no cobros", "VE", "/ventas")
        },
        "Almacenero" => new()
        {
            new("Productos activos", TextoConteo(productos?.Count(p => p.Activo)), "Referencias disponibles", "PD", "/productos"),
            new("Stock crítico", TextoConteo(productos?.Count(p => p.Activo && p.StockActual <= p.StockMinimo)), "Productos en o bajo mínimo", "!", "/almacen/stock-minimo"),
            new("Sin existencias", TextoConteo(productos?.Count(p => p.Activo && p.StockActual <= 0)), "Productos sin stock", "0", "/productos"),
            new("Categorías activas", TextoConteo(productos is null ? null : productos.Where(p => p.Activo).Select(p => p.Categoria.Id).Distinct().Count()), "Categorías con productos", "CT", "/productos"),
            new("Valor de existencias", Dinero(productos is null ? null : productos.Where(p => p.Activo).Sum(p => p.StockActual * p.CostoUnitario)), "Estimado a costo unitario", "S/", "/productos"),
            new("Productos inactivos", TextoConteo(productos?.Count(p => !p.Activo)), "Con historial conservado", "IN", "/productos")
        },
        _ => new()
    };

    internal List<Dato> SerieMensual => ventas is null ? new() :
        Enumerable.Range(0, 12).Select(n => Hoy.AddMonths(n - 11))
            .Select(m => new Dato(m.ToString("MMM", Cultura),
                ventas.Where(v => VentaVigente(v) && v.FechaEmision.HasValue && v.FechaEmision.Value.Year == m.Year && v.FechaEmision.Value.Month == m.Month)
                    .Sum(v => v.Total))).ToList();

    internal List<Dato> SerieDiaria => ventas is null ? new() :
        Enumerable.Range(0, 7).Select(n => Hoy.AddDays(n - 6))
            .Select(d => new Dato(d.ToString("dd MMM", Cultura),
                ventas.Where(v => VentaVigente(v) && v.FechaEmision == d).Sum(v => v.Total))).ToList();

    internal decimal MaximoMensual => Math.Max(1m, SerieMensual.Select(m => m.Valor).DefaultIfEmpty(0).Max());
    internal decimal MaximoCobros => Math.Max(1m, SerieCobros.Select(m => m.Valor).DefaultIfEmpty(0).Max());
    internal decimal MaximoProducto => Math.Max(1m, TopProductos.Select(p => p.Valor).DefaultIfEmpty(0).Max());
    internal decimal MaximoMedio => Math.Max(1m, MediosPago.Select(p => p.Valor).DefaultIfEmpty(0).Max());
    internal decimal MaximoDeuda => Math.Max(1m, MayoresDeudas.Select(p => p.Valor).DefaultIfEmpty(0).Max());
    internal decimal MaximoCategoria => Math.Max(1m, ProductosPorCategoria.Select(p => p.Valor).DefaultIfEmpty(0).Max());

    internal List<PuntoGrafico> PuntosGrafico => SerieDiaria.Select((d, i) =>
        new PuntoGrafico((44 + i * 75).ToString(CultureInfo.InvariantCulture),
            (150m - Porcentaje(d.Valor, Math.Max(1m, SerieDiaria.Max(x => x.Valor))) * 1.22m).ToString("0.0", CultureInfo.InvariantCulture),
            d.Etiqueta, d.Valor)).ToList();
    internal string PuntosLinea => string.Join(" ", PuntosGrafico.Select(p => p.X + "," + p.Y));

    // RNF003: el top lo calcula la base (GET /api/ventas/top-productos), no se piden 8 ventas con su detalle
    internal List<Dato> TopProductos => topProductos.Select(t => new Dato(t.Producto ?? "Producto", t.Importe)).ToList();
    internal sealed class TopProducto { public string? Producto { get; set; } public decimal Importe { get; set; } }

    internal List<VentaDatos> UltimasVentas => ventas is null ? new() :
        ventas.OrderByDescending(v => v.FechaEmision).ThenByDescending(v => v.Id).Take(5).ToList();

    internal List<Dato> Distribucion => new()
    {
        new("Cotizaciones", cotizaciones?.Count ?? 0, "sg-color-a"),
        new("Órdenes", ordenes?.Count ?? 0, "sg-color-b"),
        new("Ventas", ventas?.Count ?? 0, "sg-color-c")
    };
    internal int TotalOperaciones => (cotizaciones?.Count ?? 0) + (ordenes?.Count ?? 0) + (ventas?.Count ?? 0);

    internal string EstiloAnillo
    {
        get
        {
            if (TotalOperaciones == 0) return "background:conic-gradient(var(--chart-track) 0 100%)";
            var a = Porcentaje(cotizaciones?.Count ?? 0, TotalOperaciones);
            var b = a + Porcentaje(ordenes?.Count ?? 0, TotalOperaciones);
            return $"background:conic-gradient(var(--chart-1) 0 {a.ToString(CultureInfo.InvariantCulture)}%, var(--chart-2) {a.ToString(CultureInfo.InvariantCulture)}% {b.ToString(CultureInfo.InvariantCulture)}%, var(--chart-3) {b.ToString(CultureInfo.InvariantCulture)}% 100%)";
        }
    }

    internal List<Dato> SerieCobros => cobros is null ? new() :
        Enumerable.Range(0, 7).Select(n => Hoy.AddDays(n - 6))
            .Select(d => new Dato(d.ToString("dd MMM", Cultura), cobros.Where(c => !c.Anulado && c.FechaCobro == d).Sum(c => c.Monto))).ToList();

    internal List<Dato> MediosPago => cobros is null ? new() :
        cobros.Where(c => !c.Anulado).GroupBy(c => c.MedioPago)
            .Select(g => new Dato(g.Key, g.Sum(c => c.Monto))).OrderByDescending(g => g.Valor).ToList();

    internal List<Dato> MayoresDeudas => cuentas is null ? new() :
        cuentas.Where(c => c.SaldoPendiente > 0)
            .GroupBy(c => c.Cliente ?? $"Cliente #{c.IdCliente}")
            .Select(g => new Dato(g.Key, g.Sum(c => c.SaldoPendiente)))
            .OrderByDescending(g => g.Valor).Take(5).ToList();

    internal int? CuentasVencidas => cuentas is null ? null :
        cuentas.Count(c => c.SaldoPendiente > 0 && (c.Vencida || (c.FechaVencimiento.HasValue && c.FechaVencimiento.Value < Hoy)));

    internal List<Dato> ProductosPorCategoria => productos is null ? new() :
        productos.Where(p => p.Activo).GroupBy(p => p.Categoria?.Nombre ?? "Sin categoría")
            .Select(g => new Dato(g.Key, g.Count())).OrderByDescending(g => g.Valor).Take(7).ToList();

    internal List<Producto> StockCritico => productos is null ? new() :
        productos.Where(p => p.Activo && p.StockActual <= p.StockMinimo)
            .OrderBy(p => p.StockMinimo <= 0 ? 0 : p.StockActual / p.StockMinimo)
            .Take(7).ToList();

    internal string EstiloAnilloInventario
    {
        get
        {
            var activos = productos?.Count(p => p.Activo) ?? 0;
            if (activos == 0) return "background:conic-gradient(var(--chart-track) 0 100%)";
            var bien = productos!.Count(p => p.Activo && p.StockActual > p.StockMinimo);
            var p = Porcentaje(bien, activos).ToString(CultureInfo.InvariantCulture);
            return $"background:conic-gradient(var(--chart-1) 0 {p}%, var(--chart-2) {p}% 100%)";
        }
    }

    internal static readonly Acceso[] Administrador =
    {
        new("VE", "Ventas", "Consultar las ventas registradas.", "/ventas"),
        new("CT", "Cotizaciones", "Seguimiento comercial.", "/cotizaciones"),
        new("OC", "Órdenes de clientes", "Consultar pedidos pendientes.", "/ordenes"),
        new("CO", "Compras", "Control de abastecimiento.", "/compras"),
        new("CL", "Clientes", "Consultar la cartera comercial.", "/clientes"),
        new("PD", "Productos", "Gestionar el catálogo.", "/productos"),
        new("RP", "Reportes", "Revisar información operativa.", "/reportes")
    };
    internal static readonly Acceso[] Vendedor =
    {
        new("CL", "Clientes", "Consulta y registro de clientes.", "/clientes"),
        new("CT", "Cotizaciones", "Prepara y sigue propuestas.", "/cotizaciones"),
        new("OC", "Órdenes de clientes", "Revisa pedidos por atender.", "/ordenes"),
        new("VE", "Ventas", "Registra y consulta ventas.", "/ventas"),
        new("PD", "Productos", "Consulta el catálogo.", "/productos")
    };
    internal static readonly Acceso[] Cajero =
    {
        new("CJ", "Caja", "Consulta la jornada actual.", "/caja"),
        new("CB", "Cobros", "Registra y consulta cobros.", "/cobros"),
        new("CC", "Cuentas por cobrar", "Saldos y vencimientos.", "/cobranzas"),
        new("VE", "Ventas", "Consulta ventas al contado.", "/ventas"),
        new("CI", "Cierres", "Historial de cierres.", "/caja/cierres")
    };
    internal static readonly Acceso[] Almacenero =
    {
        new("RC", "Recepciones", "Compras por recibir.", "/recepciones"),
        new("DP", "Despachos", "Ventas por despachar.", "/despachos"),
        new("KA", "Kardex", "Entradas y salidas de stock.", "/almacen/kardex"),
        new("SM", "Stock mínimo", "Productos por reponer.", "/almacen/stock-minimo"),
        new("PD", "Productos", "Consulta existencias.", "/productos")
    };
    internal Acceso[] AccesosActuales => Rol switch
    {
        "Administrador" => Administrador,
        "Vendedor" => Vendedor,
        "Cajero" => Cajero,
        "Almacenero" => Almacenero,
        _ => Array.Empty<Acceso>()
    };

    protected override async Task OnParametersSetAsync()
    {
        if (!string.Equals(rolCargado, Rol, StringComparison.Ordinal) && !cargando)
            await CargarDashboard();
    }
}
