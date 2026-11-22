// Código de la página Reportes (parte 1 de 1). La vista está en Reportes.razor: es la misma clase (partial).
using SigmetaWeb.Components.Pages.Almacen;

namespace SigmetaWeb.Components.Pages.Reportes;

public partial class Reportes
{
    private DateTime desde = new(DateTime.Today.Year, DateTime.Today.Month, 1);
    private DateTime hasta = DateTime.Today;
    private string reporte = "comercial";
    private ReporteComercial? comercial;
    private ReporteAbastecimiento? abastecimiento;
    private ReporteCaja? caja;
    private bool cargando;
    private string? error;
    private string? csv;

    private string nombreCsv => $"sigmeta-{reporte}-{Formato.Iso(desde)}-a-{Formato.Iso(hasta)}.csv";

    // ---------------- Descarga en PDF (RF015, JasperReports en el Java) ----------------

    private bool descargando;

    private bool HayReporte => !string.IsNullOrEmpty(csv) && reporte switch
    {
        "abastecimiento" => abastecimiento is not null,
        "caja" => caja is not null,
        _ => comercial is not null
    };

    // GET /api/reportes/{tipo}/pdf: el Java arma el PDF con JasperReports y aquí solo se descarga el archivo
    private async Task DescargarPdf()
    {
        if (!HayReporte) return;
        error = null;
        descargando = true;
        try
        {
            var pdf = await Api.GetArchivo($"reportes/{reporte}/pdf?desde={Formato.Iso(desde)}&hasta={Formato.Iso(hasta)}");
            var nombre = $"SIGMETA-reporte-{reporte}-{Formato.Iso(desde)}-a-{Formato.Iso(hasta)}.pdf";
            await JS.InvokeVoidAsync("sigmetaDescargar", nombre, "application/pdf", Convert.ToBase64String(pdf));
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        catch (JSException)
        {
            error = "El PDF se generó, pero el navegador no pudo descargarlo. Vuelva a intentarlo.";
        }
        finally
        {
            descargando = false;
        }
    }


    internal static string Ancho(double valor, double maximo) =>
        maximo <= 0 ? "0%" : (Math.Max(valor / maximo, 0.01) * 100).ToString("0.#", System.Globalization.CultureInfo.InvariantCulture) + "%";

    protected override async Task OnInitializedAsync() => await Consultar();

    private async Task Cambiar(string cual)
    {
        reporte = cual;
        await Consultar();
    }

    private async Task Atajo(string cual)
    {
        var hoy = DateTime.Today;
        (desde, hasta) = cual switch
        {
            "hoy" => (hoy, hoy),
            "anterior" => (new DateTime(hoy.Year, hoy.Month, 1).AddMonths(-1), new DateTime(hoy.Year, hoy.Month, 1).AddDays(-1)),
            "90" => (hoy.AddDays(-89), hoy),
            _ => (new DateTime(hoy.Year, hoy.Month, 1), hoy)
        };
        await Consultar();
    }

    private async Task Consultar()
    {
        error = null;
        csv = null;
        if (desde > hasta)
        {
            error = "La fecha de inicio no puede ser posterior a la fecha de fin.";
            return;
        }
        cargando = true;
        var periodo = $"desde={Formato.Iso(desde)}&hasta={Formato.Iso(hasta)}";
        try
        {
            switch (reporte)
            {
                case "abastecimiento":
                    abastecimiento = await Api.Get<ReporteAbastecimiento>($"reportes/abastecimiento?{periodo}");
                    break;
                case "caja":
                    caja = await Api.Get<ReporteCaja>($"reportes/caja?{periodo}");   // la tabla de cierres vuelve sola a la página 1
                    break;
                default:
                    comercial = await Api.Get<ReporteComercial>($"reportes/comercial?{periodo}");
                    break;
            }
            csv = ArmarCsv();
        }
        catch (ErrorApi e)
        {
            error = e.Message;
        }
        finally
        {
            cargando = false;
        }
    }

    // CSV del reporte que se está viendo: separador ";" y números con punto decimal
    private string ArmarCsv()
    {
        static string N(double n) => n.ToString("0.00", System.Globalization.CultureInfo.InvariantCulture);
        var filas = new List<string> { Formato.Csv(new[] { "SIGMETA", $"Reporte {reporte}", $"Del {Formato.Iso(desde)} al {Formato.Iso(hasta)}" }), "" };
        if (reporte == "comercial" && comercial is not null)
        {
            filas.Add(Formato.Csv(new[] { "Ventas por día" }));
            filas.Add(Formato.Csv(new[] { "Fecha", "Ventas", "Subtotal", "IGV", "Total" }));
            filas.AddRange(comercial.PorDia.Select(d => Formato.Csv(new[] { d.Fecha, N(d.Ventas), N(d.SubTotal), N(d.Igv), N(d.Total) })));
            filas.Add("");
            filas.Add(Formato.Csv(new[] { "Ventas por vendedor" }));
            filas.Add(Formato.Csv(new[] { "Usuario", "Vendedor", "Ventas", "Total" }));
            filas.AddRange(comercial.PorVendedor.Select(v => Formato.Csv(new[] { v.Usuario, v.Vendedor, N(v.Ventas), N(v.Total) })));
            filas.Add("");
            filas.Add(Formato.Csv(new[] { "Clientes que más compraron" }));
            filas.Add(Formato.Csv(new[] { "Cliente", "Documento", "Ventas", "Total" }));
            filas.AddRange(comercial.PorCliente.Select(c => Formato.Csv(new[] { c.Cliente, c.NumeroDocumento, N(c.Ventas), N(c.Total) })));
            filas.Add("");
            filas.Add(Formato.Csv(new[] { "Crédito de clientes" }));
            filas.Add(Formato.Csv(new[] { "Cliente", "Documento", "Calificación", "Límite", "Deuda", "Vencido", "Días de atraso", "Disponible" }));
            filas.AddRange(comercial.Credito.Select(c => Formato.Csv(new[] { c.Cliente, c.NumeroDocumento, c.CalificacionCrediticia,
                N(c.LimiteCredito), N(c.SaldoPendiente), N(c.SaldoVencido), N(c.DiasAtraso), N(c.CreditoDisponible) })));
        }
        else if (reporte == "abastecimiento" && abastecimiento is not null)
        {
            filas.Add(Formato.Csv(new[] { "Compras a proveedores" }));
            filas.Add(Formato.Csv(new[] { "Proveedor", "RUC", "Compras", "Por recibir", "Total" }));
            filas.AddRange(abastecimiento.Compras.Select(c => Formato.Csv(new[] { c.Proveedor, c.Ruc, N(c.Compras), N(c.PorRecibir), N(c.Total) })));
            filas.Add("");
            filas.Add(Formato.Csv(new[] { "Productos de mayor movimiento" }));
            filas.Add(Formato.Csv(new[] { "Código", "Producto", "Unidades", "Stock" }));
            filas.AddRange(abastecimiento.MayorMovimiento.Select(p => Formato.Csv(new[] { p.CodigoInterno, p.Producto, N(p.Unidades), N(p.StockActual) })));
            filas.Add("");
            filas.Add(Formato.Csv(new[] { "Productos sin movimiento" }));
            filas.Add(Formato.Csv(new[] { "Código", "Producto", "Stock", "Valor al costo", "Última salida" }));
            filas.AddRange(abastecimiento.SinMovimiento.Select(p => Formato.Csv(new[] { p.CodigoInterno, p.Producto, N(p.StockActual), N(p.ValorInmovilizado), p.UltimaSalida })));
            filas.Add("");
            filas.Add(Formato.Csv(new[] { "Inventario valorizado" }));
            filas.Add(Formato.Csv(new[] { "Categoría", "Productos", "En su mínimo", "Valor al costo", "Valor a precio de venta" }));
            filas.AddRange(abastecimiento.Inventario.Select(i => Formato.Csv(new[] { i.Categoria, N(i.Productos), N(i.EnMinimo), N(i.ValorCosto), N(i.ValorVenta) })));
        }
        else if (reporte == "caja" && caja is not null)
        {
            filas.Add(Formato.Csv(new[] { "Cierres de caja" }));
            filas.Add(Formato.Csv(new[] { "Caja", "Cajero", "Cierre", "Calculado", "Declarado", "Diferencia", "Estado", "Reabrió", "Motivo" }));
            filas.AddRange(caja.Cierres.Select(c => Formato.Csv(new[] { c.IdCaja.ToString(), c.Cajero, c.FechaCierre, N(c.MontoCalculado),
                N(c.MontoDeclarado), N(c.Diferencia), c.Estado, c.UsuarioReapertura, c.MotivoReapertura })));
            filas.Add("");
            filas.Add(Formato.Csv(new[] { "Dinero por medio de pago" }));
            filas.Add(Formato.Csv(new[] { "Medio", "Cobros contado", "Cobros crédito", "Ingresos", "Egresos", "Neto" }));
            filas.AddRange(caja.PorMedioPago.Select(m => Formato.Csv(new[] { m.MedioPago, N(m.CobrosContado), N(m.CobrosCredito), N(m.Ingresos), N(m.Egresos), N(m.Neto) })));
            filas.Add("");
            filas.Add(Formato.Csv(new[] { "Diferencias por cajero" }));
            filas.Add(Formato.Csv(new[] { "Cajero", "Cierres", "Con faltante", "Con sobrante", "Faltantes", "Sobrantes", "Diferencia neta" }));
            filas.AddRange(caja.PorCajero.Select(c => Formato.Csv(new[] { c.Cajero, N(c.Cierres), N(c.ConFaltante), N(c.ConSobrante), N(c.Faltantes), N(c.Sobrantes), N(c.DiferenciaNeta) })));
        }
        return string.Join("\r\n", filas);
    }
}
