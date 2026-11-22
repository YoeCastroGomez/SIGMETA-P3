// Código de la página Home (parte 2 de 2). La vista está en Home.razor: es la misma clase (partial).
using System.Globalization;
using SigmetaWeb.Components.Pages.Compras;
using SigmetaWeb.Components.Pages.Catalogo;
using SigmetaWeb.Components.Pages.Clientes;
using SigmetaWeb.Components.Pages.Caja;
using SigmetaWeb.Components.Pages.Ventas;

namespace SigmetaWeb.Components.Pages;

public partial class Home
{

    internal async Task<List<T>?> ConsultarLista<T>(string ruta)
    {
        try { return await Api.Get<List<T>>(ruta) ?? new(); }
        catch (Exception) { return null; }
    }
    internal async Task<T?> ConsultarObjeto<T>(string ruta) where T : class
    {
        try { return await Api.Get<T>(ruta); }
        catch (Exception) { return null; }
    }

    internal async Task CargarDashboard()
    {
        cargando = true;
        Errores.Clear();
        ventas = null; cotizaciones = null; ordenes = null; clientes = null;
        productos = null; cobros = null;
        cuentas = null; cajaAbierta = null; cajaConsultada = false;
        topProductos = new();
        var rol = Rol;
        try
        {
            if (rol is "Administrador" or "Vendedor")
            {
                // RNF003: todo en paralelo y una sola consulta por dato (el top de productos ya viene calculado)
                var tareaVentas = ConsultarLista<VentaDatos>("ventas");
                var tareaCotizaciones = ConsultarLista<CotizacionDatos>("cotizaciones");
                var tareaOrdenes = ConsultarLista<OrdenDatos>("ordenes");
                var tareaClientes = ConsultarLista<Cliente>("clientes");
                var tareaTop = ConsultarLista<TopProducto>("ventas/top-productos");
                if (rol == "Administrador")
                {
                    var tareaProductos = ConsultarLista<Producto>("productos");
                    await Task.WhenAll(tareaVentas, tareaCotizaciones, tareaOrdenes, tareaClientes, tareaTop, tareaProductos);
                    productos = await tareaProductos;
                    if (productos is null) Errores.Add("productos");
                }
                else
                {
                    await Task.WhenAll(tareaVentas, tareaCotizaciones, tareaOrdenes, tareaClientes, tareaTop);
                }
                topProductos = await tareaTop ?? new();
                ventas = await tareaVentas;
                cotizaciones = await tareaCotizaciones;
                ordenes = await tareaOrdenes;
                clientes = await tareaClientes;
                if (ventas is null) Errores.Add("ventas");
                if (cotizaciones is null) Errores.Add("cotizaciones");
                if (ordenes is null) Errores.Add("órdenes");
                if (clientes is null) Errores.Add("clientes");

            }
            else if (rol == "Cajero")
            {
                var tareaCobros = ConsultarLista<CobroDatos>("cobros");
                var tareaCuentas = ConsultarLista<CuentaDatos>("cuentas-por-cobrar");
                var tareaCaja = ConsultarObjeto<CajaDatos>("cajas/abierta");
                var tareaVentas = ConsultarLista<VentaDatos>("ventas");
                await Task.WhenAll(tareaCobros, tareaCuentas, tareaCaja, tareaVentas);
                cobros = await tareaCobros;
                cuentas = await tareaCuentas;
                cajaAbierta = await tareaCaja;
                cajaConsultada = true;
                ventas = await tareaVentas;
                if (cobros is null) Errores.Add("cobros");
                if (cuentas is null) Errores.Add("cuentas por cobrar");
                if (ventas is null) Errores.Add("ventas");
            }
            else if (rol == "Almacenero")
            {
                productos = await ConsultarLista<Producto>("productos");
                if (productos is null) Errores.Add("productos");
            }
        }
        finally
        {
            rolCargado = rol;
            cargando = false;
        }
    }
}
