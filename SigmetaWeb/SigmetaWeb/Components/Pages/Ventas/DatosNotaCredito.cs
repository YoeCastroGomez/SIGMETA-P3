namespace SigmetaWeb.Components.Pages.Ventas;

// RF013: filas que responde /api/notas-credito (NotaCreditoRS).

// GET /api/notas-credito
public class NotaCreditoDatos
{
    public int Id { get; set; }
    public string Nota { get; set; } = "";
    public string? FechaEmision { get; set; }
    public string Estado { get; set; } = "";
    public double SubTotal { get; set; }
    public double Igv { get; set; }
    public double Total { get; set; }
    public string? Motivo { get; set; }
    public string? TipoOrigen { get; set; }
    public string? Origen { get; set; }
    public string? Venta { get; set; }
    public string? CondicionPago { get; set; }
    public string Cliente { get; set; } = "";
    public string? NumeroDocumento { get; set; }
    public string? Autorizo { get; set; }
}

// GET /api/notas-credito/{id}/lineas
public class NotaCreditoLinea
{
    public int NumeroLinea { get; set; }
    public string CodigoInterno { get; set; } = "";
    public string Producto { get; set; } = "";
    public string UnidadVenta { get; set; } = "";
    public double Cantidad { get; set; }
    public double PrecioUnitario { get; set; }
    public double Descuento { get; set; }
    public double Importe { get; set; }
}

// GET /api/notas-credito/comprobantes: facturas y boletas con mercadería entregada
public class ComprobanteParaNota
{
    public int Id { get; set; }
    public string Tipo { get; set; } = "";
    public string Comprobante { get; set; } = "";
    public string? FechaEmision { get; set; }
    public double Total { get; set; }
    public string? Venta { get; set; }
    public string CondicionPago { get; set; } = "";
    public string Cliente { get; set; } = "";
    public string? NumeroDocumento { get; set; }
}

// GET /api/notas-credito/comprobantes/{id}/lineas
public class LineaDevolvible
{
    public int IdDetalleVenta { get; set; }
    public int NumeroLinea { get; set; }
    public string CodigoInterno { get; set; } = "";
    public string Producto { get; set; } = "";
    public string UnidadVenta { get; set; } = "";
    public double Cantidad { get; set; }
    public double CantidadDespachada { get; set; }
    public double PrecioUnitario { get; set; }
    public double Descuento { get; set; }
    public double Importe { get; set; }
    public double Acreditado { get; set; }
    public double Devolvible { get; set; }
    public double Devolver { get; set; }   // lo que se escribe en la pantalla

    // precio efectivamente cobrado por unidad (ya con el descuento): es lo que se devuelve
    public double PrecioEfectivo => Cantidad > 0 ? Importe / Cantidad : 0;
    // mismo cálculo que ComprobanteBOImpl: importe de la línea × devuelto ÷ vendido, a 2 decimales
    public double ImporteDevolucion => Cantidad > 0 ? Math.Round(Importe * Devolver / Cantidad, 2, MidpointRounding.AwayFromZero) : 0;
}
