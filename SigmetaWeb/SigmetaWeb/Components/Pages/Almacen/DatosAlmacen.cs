namespace SigmetaWeb.Components.Pages.Almacen;

// RF012: filas que responde /api/almacen (AlmacenRS). Cada clase corresponde a un listado del Java;
// los nombres son los de las columnas en JSON.

// GET /api/almacen/compras-por-recibir
public class CompraPorRecibir
{
    public int Id { get; set; }
    public string Numero { get; set; } = "";
    public string? FechaEmision { get; set; }
    public string? FechaRecepcionEstimada { get; set; }
    public string Estado { get; set; } = "";
    public double Total { get; set; }
    public string Proveedor { get; set; } = "";
    public string? Ruc { get; set; }
    public int Lineas { get; set; }
    public int LineasPendientes { get; set; }
}

// GET /api/almacen/compras/{id}/lineas
public class LineaCompra
{
    public int IdDetalleCompra { get; set; }
    public int NumeroLinea { get; set; }
    public int IdProducto { get; set; }
    public string CodigoInterno { get; set; } = "";
    public string Producto { get; set; } = "";
    public string UnidadCompra { get; set; } = "";
    public string UnidadVenta { get; set; } = "";
    public double FactorConversion { get; set; }
    public double Cantidad { get; set; }
    public double CantidadRecibida { get; set; }
    public double Pendiente { get; set; }
    public double StockActual { get; set; }
    public double Recibir { get; set; }   // lo que se escribe en la pantalla
}

// GET /api/almacen/recepciones
public class RecepcionFila
{
    public int Id { get; set; }
    public int IdCompra { get; set; }
    public string Compra { get; set; } = "";
    public string Proveedor { get; set; } = "";
    public string? FechaRecepcion { get; set; }
    public string? Observaciones { get; set; }
    public bool Anulada { get; set; }
    public string? FechaRegistro { get; set; }
    public string? Usuario { get; set; }
    public int Lineas { get; set; }
}

// GET /api/almacen/recepciones/{id}/lineas
public class RecepcionLinea
{
    public int NumeroLinea { get; set; }
    public string CodigoInterno { get; set; } = "";
    public string Producto { get; set; } = "";
    public string UnidadCompra { get; set; } = "";
    public string UnidadVenta { get; set; } = "";
    public double FactorConversion { get; set; }
    public double CantidadRecibida { get; set; }
    public double CantidadStock { get; set; }
}

// GET /api/almacen/ventas-por-despachar
public class VentaPorDespachar
{
    public int Id { get; set; }
    public string Numero { get; set; } = "";
    public string? FechaEmision { get; set; }
    public string Estado { get; set; } = "";
    public string CondicionPago { get; set; } = "";
    public double Total { get; set; }
    public string Cliente { get; set; } = "";
    public string? NumeroDocumento { get; set; }
    public string? Direccion { get; set; }
    public string? Comprobante { get; set; }
    public int CobrosContado { get; set; }
    public int LineasPendientes { get; set; }

    // RF012: sale del almacén con comprobante y, al contado, ya cobrada
    public bool SinComprobante => string.IsNullOrEmpty(Comprobante);
    public bool SinCobro => CondicionPago == "CONTADO" && CobrosContado == 0;
    public bool Lista => !SinComprobante && !SinCobro;
}

// GET /api/almacen/ventas/{id}/lineas
public class LineaVentaDespacho
{
    public int IdDetalleVenta { get; set; }
    public int NumeroLinea { get; set; }
    public int IdProducto { get; set; }
    public string CodigoInterno { get; set; } = "";
    public string Producto { get; set; } = "";
    public string UnidadVenta { get; set; } = "";
    public double Cantidad { get; set; }
    public double CantidadDespachada { get; set; }
    public double Pendiente { get; set; }
    public double StockActual { get; set; }
    public double Despachar { get; set; }   // lo que se escribe en la pantalla
}

// GET /api/almacen/despachos
public class DespachoFila
{
    public int Id { get; set; }
    public int IdVenta { get; set; }
    public string Venta { get; set; } = "";
    public string Guia { get; set; } = "";
    public string Cliente { get; set; } = "";
    public string? FechaDespacho { get; set; }
    public string? DireccionEntrega { get; set; }
    public string? Transportista { get; set; }
    public bool Anulado { get; set; }
    public string? FechaRegistro { get; set; }
    public string? Usuario { get; set; }
    public int Lineas { get; set; }
}

// GET /api/almacen/despachos/{id}/lineas
public class DespachoLinea
{
    public int NumeroLinea { get; set; }
    public string CodigoInterno { get; set; } = "";
    public string Producto { get; set; } = "";
    public string UnidadVenta { get; set; } = "";
    public double CantidadVendida { get; set; }
    public double CantidadDespachada { get; set; }
}

// GET /api/almacen/kardex
public class MovimientoFila
{
    public int Id { get; set; }
    public string? FechaMovimiento { get; set; }
    public string Tipo { get; set; } = "";
    public int IdProducto { get; set; }
    public string CodigoInterno { get; set; } = "";
    public string Producto { get; set; } = "";
    public string UnidadVenta { get; set; } = "";
    public double Cantidad { get; set; }
    public double StockResultante { get; set; }
    public double? CantidadContada { get; set; }
    public string? Motivo { get; set; }
    public string? Usuario { get; set; }
    public int Signo { get; set; }        // +1 entra, -1 sale
    public int Revertido { get; set; }    // 1 si ya tiene su movimiento de reversión

    public bool EsEntrada => Signo > 0;
    public bool EsAjusteOriginal => Tipo is "AJUSTE_INGRESO" or "AJUSTE_SALIDA";
}

// GET /api/almacen/existencias
public class ExistenciaFila
{
    public int Id { get; set; }
    public string CodigoInterno { get; set; } = "";
    public string Nombre { get; set; } = "";
    public string? Categoria { get; set; }
    public string UnidadVenta { get; set; } = "";
    public double StockActual { get; set; }
    public double StockMinimo { get; set; }
    public double CostoUnitario { get; set; }
    public double Comprometido { get; set; }
    public double Disponible { get; set; }

    public bool SinStock => StockActual <= 0;
    public bool BajoMinimo => StockActual < StockMinimo;
    public bool EnMinimo => StockActual <= StockMinimo;
    public double Faltante => Math.Max(StockMinimo - StockActual, 0);
    public string Situacion => SinStock ? "Sin stock" : BajoMinimo ? "Bajo el mínimo" : EnMinimo ? "En el mínimo" : "Normal";
}

public static class TextosAlmacen
{
    public static string Unidad(string? unidad) => unidad switch
    {
        "UNIDAD" => "und", "CAJA" => "caja", "KILOGRAMO" => "kg", "METRO" => "m", "LITRO" => "L",
        "MILLAR" => "millar", "GALON" => "gal", "JUEGO" => "juego", _ => unidad ?? ""
    };

    public static string Movimiento(string? tipo) => tipo switch
    {
        "INGRESO_COMPRA" => "Ingreso por compra",
        "SALIDA_DESPACHO" => "Salida por despacho",
        "INGRESO_DEVOLUCION" => "Ingreso por devolución",
        "INGRESO_REVERSION_DESPACHO" => "Reversión de despacho",
        "SALIDA_REVERSION_COMPRA" => "Reversión de recepción",
        "AJUSTE_INGRESO" => "Ajuste (sobrante)",
        "AJUSTE_SALIDA" => "Ajuste (faltante)",
        "REVERSION_AJUSTE_INGRESO" => "Reversión de ajuste (sobrante)",
        "REVERSION_AJUSTE_SALIDA" => "Reversión de ajuste (faltante)",
        "INGRESO_PRODUCCION" => "Ingreso por producción",
        "SALIDA_PRODUCCION" => "Salida por producción",
        _ => Shared.Formato.Legible(tipo)
    };

    // tipos que se ofrecen en los filtros (la ferretería no fabrica: sin producción)
    public static readonly string[] TiposMovimiento =
    {
        "INGRESO_COMPRA", "SALIDA_DESPACHO", "INGRESO_DEVOLUCION", "INGRESO_REVERSION_DESPACHO",
        "SALIDA_REVERSION_COMPRA", "AJUSTE_INGRESO", "AJUSTE_SALIDA", "REVERSION_AJUSTE_INGRESO", "REVERSION_AJUSTE_SALIDA"
    };
}
