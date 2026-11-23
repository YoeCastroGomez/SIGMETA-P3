namespace SigmetaWeb.Components.Pages.Reportes;

// RF015: lo que responde /api/reportes (ReporteRS). Cada reporte trae sus bloques ya sumados por la base.
// Los números se leen como double porque la base los manda con o sin decimales según la columna.

public class ReporteComercial
{
    public TotalesVentas Totales { get; set; } = new();
    public List<VentaDia> PorDia { get; set; } = new();
    public List<VentaVendedor> PorVendedor { get; set; } = new();
    public List<VentaCliente> PorCliente { get; set; } = new();
    public List<CreditoCliente> Credito { get; set; } = new();
    // RF013: seguimiento de las notas de crédito del periodo
    public List<NotaCreditoReporte> NotasCredito { get; set; } = new();
}

public class TotalesVentas
{
    public double VentasContado { get; set; }
    public double MontoContado { get; set; }
    public double VentasCredito { get; set; }
    public double MontoCredito { get; set; }
    public double VentasAnuladas { get; set; }
    public double NotasCredito { get; set; }
    public double MontoNotasCredito { get; set; }
    public double SaldoPorCobrar { get; set; }
    public double SaldoVencido { get; set; }

    public double MontoVendido => MontoContado + MontoCredito;
    public double VentaNeta => MontoVendido - MontoNotasCredito;
}

public class VentaDia
{
    public string? Fecha { get; set; }
    public double Ventas { get; set; }
    public double SubTotal { get; set; }
    public double Igv { get; set; }
    public double Total { get; set; }
}

public class VentaVendedor
{
    public string? Usuario { get; set; }
    public string? Vendedor { get; set; }
    public double Ventas { get; set; }
    public double Total { get; set; }
}

public class VentaCliente
{
    public string? Cliente { get; set; }
    public string? NumeroDocumento { get; set; }
    public double Ventas { get; set; }
    public double Total { get; set; }
}

public class CreditoCliente
{
    public string? Cliente { get; set; }
    public string? NumeroDocumento { get; set; }
    public double LimiteCredito { get; set; }
    public double PlazoCreditoDias { get; set; }
    public string? CalificacionCrediticia { get; set; }
    public double SaldoPendiente { get; set; }
    public double SaldoVencido { get; set; }
    public double DiasAtraso { get; set; }
    public double CreditoDisponible { get; set; }
}

public class ReporteAbastecimiento
{
    public List<CompraProveedor> Compras { get; set; } = new();
    public List<ProductoMovimiento> MayorMovimiento { get; set; } = new();
    public List<ProductoSinMovimiento> SinMovimiento { get; set; } = new();
    public List<InventarioCategoria> Inventario { get; set; } = new();
}

public class CompraProveedor
{
    public string? Proveedor { get; set; }
    public string? Ruc { get; set; }
    public double Compras { get; set; }
    public double Total { get; set; }
    public double PorRecibir { get; set; }
}

public class ProductoMovimiento
{
    public string? CodigoInterno { get; set; }
    public string? Producto { get; set; }
    public string? UnidadVenta { get; set; }
    public double Unidades { get; set; }
    public double ImporteReferencial { get; set; }
    public double StockActual { get; set; }
}

public class ProductoSinMovimiento
{
    public string? CodigoInterno { get; set; }
    public string? Producto { get; set; }
    public string? UnidadVenta { get; set; }
    public double StockActual { get; set; }
    public double ValorInmovilizado { get; set; }
    public string? UltimaSalida { get; set; }
}

public class InventarioCategoria
{
    public string? Categoria { get; set; }
    public double Productos { get; set; }
    public double EnMinimo { get; set; }
    public double ValorCosto { get; set; }
    public double ValorVenta { get; set; }
}

public class ReporteCaja
{
    public List<CierreReporte> Cierres { get; set; } = new();
    public List<MedioPagoReporte> PorMedioPago { get; set; } = new();
    public List<CajeroReporte> PorCajero { get; set; } = new();
    // RF009 + RF013 + RNF001: cobros, notas de crédito y cuentas anuladas en el periodo
    public List<AnulacionReporte> Anulaciones { get; set; } = new();
}

public class CierreReporte
{
    public int Id { get; set; }
    public int IdCaja { get; set; }
    public string? FechaApertura { get; set; }
    public string? FechaCierre { get; set; }
    public double MontoInicial { get; set; }
    public double MontoCalculado { get; set; }
    public double MontoDeclarado { get; set; }
    public double Diferencia { get; set; }
    public string Estado { get; set; } = "";
    public string? Cajero { get; set; }
    public string? UsuarioReapertura { get; set; }
    public string? MotivoReapertura { get; set; }
}

public class MedioPagoReporte
{
    public string? MedioPago { get; set; }
    public double CobrosContado { get; set; }
    public double CobrosCredito { get; set; }
    public double Ingresos { get; set; }
    public double Egresos { get; set; }
    public double Neto { get; set; }
}

public class CajeroReporte
{
    public string? Cajero { get; set; }
    public double Cierres { get; set; }
    public double ConFaltante { get; set; }
    public double ConSobrante { get; set; }
    public double Faltantes { get; set; }
    public double Sobrantes { get; set; }
    public double DiferenciaNeta { get; set; }
}

// RF013 + RF015: nota de crédito emitida en el periodo (reporte comercial, sp_reporte_notas_credito)
public class NotaCreditoReporte
{
    public int Id { get; set; }
    public string? Nota { get; set; }
    public string? FechaEmision { get; set; }
    public string? Comprobante { get; set; }
    public string? Venta { get; set; }
    public string? CondicionPago { get; set; }
    public string? Cliente { get; set; }
    public string? Motivo { get; set; }
    public double Total { get; set; }
    public string Estado { get; set; } = "";
    public string? Autorizo { get; set; }
    public int? IdCuentaPorCobrar { get; set; }
}

// RF009 + RF013 + RF015 + RNF001: anulación del periodo (reporte de caja, sp_reporte_anulaciones)
public class AnulacionReporte
{
    public string? Fecha { get; set; }
    public string Tipo { get; set; } = "";
    public string? Documento { get; set; }
    public string? Venta { get; set; }
    public string? Cliente { get; set; }
    public double Monto { get; set; }
    public string? Motivo { get; set; }
    public string? Usuario { get; set; }

    public string TipoTexto => Tipo switch
    {
        "COBRO" => "Cobro",
        "NOTA_CREDITO" => "Nota de crédito",
        "CUENTA_POR_COBRAR" => "Cuenta por cobrar",
        _ => Tipo
    };
}
