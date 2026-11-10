namespace SigmetaWeb.Components.Pages.Compras;

public class ProductoCompraRest
{
    public int Id { get; set; }
    public string CodigoInterno { get; set; } = "";
    public string Nombre { get; set; } = "";
    public string UnidadCompra { get; set; } = "UNIDAD";
    public decimal FactorConversion { get; set; } = 1m;
    public decimal CostoUnitario { get; set; }
    public bool Estado { get; set; }
}
