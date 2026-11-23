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

    // nombre de los archivos que se descargan: SIGMETA-reporte-caja-2026-11-01-a-2026-11-23.pdf / .xlsx / .xml
    private string NombreArchivo(string extension) => $"SIGMETA-reporte-{reporte}-{Formato.Iso(desde)}-a-{Formato.Iso(hasta)}.{extension}";

    private string TituloReporte => reporte switch
    {
        "abastecimiento" => "Reporte de abastecimiento e inventario",
        "caja" => "Reporte de caja",
        _ => "Reporte comercial"
    };

    // ---------------- Descarga en PDF (RF015, JasperReports en el Java) ----------------

    private bool descargando;

    private bool HayReporte => !cargando && reporte switch
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
            if (pdf.Length < 5 || pdf[0] != (byte)'%' || pdf[1] != (byte)'P' || pdf[2] != (byte)'D' || pdf[3] != (byte)'F')
            {
                error = "El servidor no devolvió un PDF. Revise que GlassFish tenga desplegada la última versión (mvn clean install).";
                return;
            }
            await JS.InvokeVoidAsync("sigmetaDescargar", NombreArchivo("pdf"), "application/pdf", Convert.ToBase64String(pdf));
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


    // RF015: los gráficos se dibujan "a base del total": porcentaje de la fila sobre el total del bloque
    internal static double Participacion(double valor, double total) =>
        total <= 0 || valor <= 0 ? 0 : valor / total * 100;

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

    // ---------------- Descarga en Excel y XML (RF015) ----------------
    // Se arman en Blazor con las mismas tablas que se ven en pantalla (ExportarReporte.cs), sin volver a consultar.

    private async Task DescargarExcel()
    {
        if (!HayReporte) return;
        await Descargar(NombreArchivo("xlsx"), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ExportarReporte.Excel(TituloReporte, desde, hasta, Secciones()));
    }

    private async Task DescargarXml()
    {
        if (!HayReporte) return;
        await Descargar(NombreArchivo("xml"), "application/xml", ExportarReporte.Xml(reporte, desde, hasta, Secciones()));
    }

    private async Task Descargar(string nombre, string tipo, byte[] contenido)
    {
        error = null;
        try
        {
            await JS.InvokeVoidAsync("sigmetaDescargar", nombre, tipo, Convert.ToBase64String(contenido));
        }
        catch (JSException)
        {
            error = "El archivo se generó, pero el navegador no pudo descargarlo. Vuelva a intentarlo.";
        }
    }

    // Las tablas del reporte que se está viendo. Los importes van como número (double) y los conteos como entero.
    private List<SeccionReporte> Secciones()
    {
        static double P(double valor, double total) => Math.Round(Participacion(valor, total), 2);
        var lista = new List<SeccionReporte>();
        if (reporte == "comercial" && comercial is not null)
        {
            var t = comercial.Totales;
            var vendido = t.MontoVendido;
            lista.Add(new("Resumen", new[] { "Concepto", "Cantidad", "Monto (S/)", "% del total vendido" }, new()
            {
                new object?[] { "Ventas al contado", (int)t.VentasContado, t.MontoContado, P(t.MontoContado, vendido) },
                new object?[] { "Ventas al crédito", (int)t.VentasCredito, t.MontoCredito, P(t.MontoCredito, vendido) },
                new object?[] { "Total vendido", (int)(t.VentasContado + t.VentasCredito), vendido, 100.0 },
                new object?[] { "Notas de crédito", (int)t.NotasCredito, t.MontoNotasCredito, P(t.MontoNotasCredito, vendido) },
                new object?[] { "Venta neta", null, t.VentaNeta, P(t.VentaNeta, vendido) },
                new object?[] { "Por cobrar a la fecha", null, t.SaldoPorCobrar, null },
                new object?[] { "Vencido a la fecha", null, t.SaldoVencido, null }
            }));
            var totalDias = comercial.PorDia.Sum(d => d.Total);
            lista.Add(new("Ventas por día", new[] { "Fecha", "Ventas", "Subtotal", "IGV", "Total", "% del total" },
                comercial.PorDia.Select(d => new object?[] { d.Fecha, (int)d.Ventas, d.SubTotal, d.Igv, d.Total, P(d.Total, totalDias) }).ToList()));
            var totalVendedores = comercial.PorVendedor.Sum(v => v.Total);
            lista.Add(new("Ventas por vendedor", new[] { "Usuario", "Vendedor", "Ventas", "Total", "% del total" },
                comercial.PorVendedor.Select(v => new object?[] { v.Usuario, v.Vendedor, (int)v.Ventas, v.Total, P(v.Total, totalVendedores) }).ToList()));
            lista.Add(new("Clientes que más compraron", new[] { "Cliente", "Documento", "Ventas", "Total", "% del total vendido" },
                comercial.PorCliente.Select(c => new object?[] { c.Cliente, c.NumeroDocumento, (int)c.Ventas, c.Total, P(c.Total, vendido) }).ToList()));
            lista.Add(new("Crédito de clientes", new[] { "Cliente", "Documento", "Calificación", "Límite", "Deuda", "Vencido", "Días de atraso", "Disponible" },
                comercial.Credito.Select(c => new object?[] { c.Cliente, c.NumeroDocumento, c.CalificacionCrediticia, c.LimiteCredito,
                    c.SaldoPendiente, c.SaldoVencido, (int)c.DiasAtraso, c.CreditoDisponible }).ToList()));
            lista.Add(new("Notas de crédito", new[] { "Nota", "Emisión", "Corrige", "Venta", "Condición", "Cliente", "Motivo", "Autorizó", "Total", "Estado" },
                comercial.NotasCredito.Select(n => new object?[] { n.Nota, n.FechaEmision, n.Comprobante, n.Venta, Formato.Legible(n.CondicionPago),
                    n.Cliente, n.Motivo, n.Autorizo, n.Total, n.Estado == "ANULADO" ? "Anulada" : "Vigente" }).ToList()));
        }
        else if (reporte == "abastecimiento" && abastecimiento is not null)
        {
            var totalCompras = abastecimiento.Compras.Sum(c => c.Total);
            lista.Add(new("Compras a proveedores", new[] { "Proveedor", "RUC", "Compras", "Por recibir", "Total", "% del total" },
                abastecimiento.Compras.Select(c => new object?[] { c.Proveedor, c.Ruc, (int)c.Compras, (int)c.PorRecibir, c.Total, P(c.Total, totalCompras) }).ToList()));
            var totalUnidades = abastecimiento.MayorMovimiento.Sum(p => p.Unidades);
            lista.Add(new("Mayor movimiento", new[] { "Código", "Producto", "Unidades", "Stock", "% de las unidades" },
                abastecimiento.MayorMovimiento.Select(p => new object?[] { p.CodigoInterno, p.Producto, p.Unidades, p.StockActual, P(p.Unidades, totalUnidades) }).ToList()));
            lista.Add(new("Sin movimiento", new[] { "Código", "Producto", "Stock", "Valor al costo", "Última salida" },
                abastecimiento.SinMovimiento.Select(p => new object?[] { p.CodigoInterno, p.Producto, p.StockActual, p.ValorInmovilizado, p.UltimaSalida ?? "Nunca" }).ToList()));
            var totalCosto = abastecimiento.Inventario.Sum(i => i.ValorCosto);
            lista.Add(new("Inventario valorizado", new[] { "Categoría", "Productos", "En su mínimo", "Valor al costo", "Valor a precio de venta", "% del valor al costo" },
                abastecimiento.Inventario.Select(i => new object?[] { i.Categoria, (int)i.Productos, (int)i.EnMinimo, i.ValorCosto, i.ValorVenta, P(i.ValorCosto, totalCosto) }).ToList()));
        }
        else if (reporte == "caja" && caja is not null)
        {
            lista.Add(new("Cierres de caja", new[] { "Caja", "Cajero", "Cierre", "Calculado", "Declarado", "Diferencia", "Estado", "Reabrió", "Motivo" },
                caja.Cierres.Select(c => new object?[] { c.IdCaja, c.Cajero, Formato.FechaHora(c.FechaCierre), c.MontoCalculado, c.MontoDeclarado,
                    c.Diferencia, Formato.Legible(c.Estado), c.UsuarioReapertura, c.MotivoReapertura }).ToList()));
            var netoPositivo = caja.PorMedioPago.Where(m => m.Neto > 0).Sum(m => m.Neto);
            lista.Add(new("Dinero por medio de pago", new[] { "Medio", "Cobros contado", "Cobros crédito", "Ingresos", "Egresos", "Neto", "% del neto total" },
                caja.PorMedioPago.Select(m => new object?[] { Formato.Legible(m.MedioPago), m.CobrosContado, m.CobrosCredito, m.Ingresos, m.Egresos, m.Neto, P(m.Neto, netoPositivo) }).ToList()));
            lista.Add(new("Diferencias por cajero", new[] { "Cajero", "Cierres", "Con faltante", "Con sobrante", "Faltantes", "Sobrantes", "Diferencia neta" },
                caja.PorCajero.Select(c => new object?[] { c.Cajero, (int)c.Cierres, (int)c.ConFaltante, (int)c.ConSobrante, c.Faltantes, c.Sobrantes, c.DiferenciaNeta }).ToList()));
            lista.Add(new("Anulaciones", new[] { "Fecha", "Tipo", "Documento", "Venta", "Cliente", "Monto", "Motivo", "Usuario" },
                caja.Anulaciones.Select(a => new object?[] { Formato.FechaHora(a.Fecha), a.TipoTexto, a.Documento, a.Venta, a.Cliente, a.Monto, a.Motivo, a.Usuario }).ToList()));
        }
        return lista;
    }
}
