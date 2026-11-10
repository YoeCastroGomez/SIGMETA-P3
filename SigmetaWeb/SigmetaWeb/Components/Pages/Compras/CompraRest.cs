using System.ComponentModel.DataAnnotations;

namespace SigmetaWeb.Components.Pages.Compras;

// RF011: contratos C# para el JSON del servicio REST Java /api/compras.
public class CompraRest
{
    public int Id { get; set; }
    public string Numero { get; set; } = "";
    public int IdProveedor { get; set; }
    public int IdUsuarioRegistro { get; set; }
    public string? FechaEmision { get; set; }
    public string? FechaRecepcionEstimada { get; set; }
    public string? FechaRegistro { get; set; }
    public string Estado { get; set; } = "";
    public decimal SubTotal { get; set; }
    public decimal Igv { get; set; }
    public decimal Total { get; set; }
    public string? Observaciones { get; set; }
    public bool Anulado { get; set; }
    public string? MotivoAnulacion { get; set; }
    public List<DetalleCompraRest> Detalles { get; set; } = new();
}

public class DetalleCompraRest
{
    public int Id { get; set; }
    public int NumeroLinea { get; set; }
    public int IdProducto { get; set; }
    public decimal Cantidad { get; set; }
    public decimal PrecioUnitario { get; set; }
    public decimal Descuento { get; set; }
    public decimal Importe { get; set; }
    public string UnidadCompra { get; set; } = "UNIDAD";
    public decimal FactorConversion { get; set; } = 1;
    public decimal CantidadRecibida { get; set; }
}

// Contrato de escritura: nunca se envia el id del usuario ni los importes calculados.
// El servidor obtiene la identidad del ticket y calcula los totales.
public class SolicitudCompraRest
{
    public int IdProveedor { get; set; }
    public string Numero { get; set; } = "";
    public string FechaEmision { get; set; } = DateTime.Today.ToString("yyyy-MM-dd");
    public string? FechaRecepcionEstimada { get; set; }
    public string? Observaciones { get; set; }
    public List<SolicitudDetalleCompraRest> Detalles { get; set; } = new();
}

public class SolicitudDetalleCompraRest
{
    public int NumeroLinea { get; set; }
    public int IdProducto { get; set; }
    public decimal Cantidad { get; set; } = 1;
    public decimal PrecioUnitario { get; set; }
    public decimal Descuento { get; set; }
    public string UnidadCompra { get; set; } = "UNIDAD";
    public decimal FactorConversion { get; set; } = 1;
}
