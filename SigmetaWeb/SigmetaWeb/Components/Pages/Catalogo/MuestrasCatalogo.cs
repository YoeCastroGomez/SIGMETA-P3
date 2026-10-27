namespace SigmetaWeb.Components.Pages.Catalogo;

// Datos de ejemplo fijos (Lab 9: sin BD ni servicios). Se reemplazan al integrar con Java.
// Los nombres de los campos son los mismos que en las clases Proveedor, Categoria, Producto y Cotizacion del Java.

// RF004 · Proveedor (los datos de contacto vienen de DatosContacto).
public record ProveedorFila(int Id, string Ruc, string RazonSocial, string? Rubro,
                            string? Direccion, string? Telefono, string? Correo, string? NombreContacto,
                            int PlazoEntregaDias, string CondicionPago, bool Activo);

// RF006 · Categoría.
public record CategoriaFila(int Id, string Nombre, string? Descripcion, bool Activo);

// RF005 · Producto.
public record ProductoFila(int Id, string CodigoInterno, string? CodigoFabricante, string? CodigoProveedor,
                           string Nombre, string? Descripcion, string Categoria,
                           string UnidadCompra, string UnidadVenta, decimal FactorConversion,
                           decimal PrecioVenta, decimal CostoUnitario, decimal? PrecioReferencial,
                           decimal StockActual, decimal StockMinimo, bool Activo);

// RF007 · Cotización y su detalle (solo en soles).
public record LineaCotizacionFila(int NumeroLinea, string Producto, decimal Cantidad, decimal PrecioUnitario,
                                  decimal Descuento, decimal Importe);

public record CotizacionFila(int Id, string Numero, string Cliente, string FechaEmision, string FechaVigencia,
                             string Estado, string? Observaciones,
                             decimal SubTotal, decimal Igv, decimal Total,
                             LineaCotizacionFila[] Lineas);

public static class MuestrasCatalogo
{
    public static readonly ProveedorFila[] Proveedores =
    {
        new(1, "20431298765", "Importaciones Eléctricas Delta S.A.C.", "Material eléctrico industrial",
            "Av. Argentina 3450, Callao", "01 452 0011", "ventas@delta.com.pe", "César Bravo",
            5, "CREDITO", true),
        new(2, "20512346789", "Ferretería Industrial Huachipa E.I.R.L.", "Fijaciones y abrasivos",
            "Carretera Central km 9, Lurigancho-Chosica", "01 371 0044", "contacto@fihuachipa.pe", "Nelly Fuentes",
            2, "CONTADO", true),
        new(3, "20601234567", "Aceros y Fierros del Centro S.A.", "Construcción",
            "Av. Industrial 780, Ate", "01 326 8899", null, "Julio Armas",
            7, "CREDITO", false),
    };

    public static readonly CategoriaFila[] Categorias =
    {
        new(1, "Herramientas Manuales", "Martillos, alicates, destornilladores", true),
        new(2, "Materiales Eléctricos", "Cables, enchufes industriales", true),
        new(3, "Construcción", "Cemento, fierro, agregados", true),
        new(4, "Pinturas", null, false),
    };

    public static readonly ProductoFila[] Productos =
    {
        new(1, "INT-001", "FAB-101", "PROV-11", "Martillo de Acero", "Martillo de uña con mango de fibra",
            "Herramientas Manuales", "UNIDAD", "UNIDAD", 1m, 45.00m, 25.00m, 49.90m, 100m, 10m, true),
        new(2, "INT-002", "FAB-102", "PROV-22", "Alicate de Presión", null,
            "Herramientas Manuales", "UNIDAD", "UNIDAD", 1m, 35.00m, 18.00m, null, 50m, 5m, true),
        new(3, "INT-003", "FAB-103", "PROV-33", "Cable Mellizo 2x14", "Cable mellizo para uso doméstico",
            "Materiales Eléctricos", "METRO", "METRO", 1m, 2.50m, 1.20m, 2.80m, 500m, 100m, true),
        new(4, "INT-004", "FAB-104", "PROV-44", "Enchufe Mennekes", null,
            "Materiales Eléctricos", "UNIDAD", "UNIDAD", 1m, 85.00m, 50.00m, null, 30m, 5m, true),
        new(5, "INT-005", "FAB-105", "PROV-55", "Bolsa Cemento Sol", "Bolsa de 42.5 kg",
            "Construcción", "UNIDAD", "UNIDAD", 1m, 28.50m, 24.00m, 29.90m, 40m, 50m, true),
        new(6, "INT-006", "FAB-106", "PROV-66", "Fierro Corrugado", "Barra de 3/8\" x 9 m",
            "Construcción", "UNIDAD", "UNIDAD", 1m, 32.00m, 26.00m, null, 150m, 30m, false),
    };

    // Los nombres de cliente son los mismos de la lista de clientes de ejemplo.
    public static readonly string[] Clientes =
    {
        "Constructora Andina S.A.C.",
        "Metalmecánica Chosica E.I.R.L.",
        "Pedro Huamaní Flores",
        "Instalaciones Eléctricas del Sur S.A.",
    };

    public static readonly string[] Unidades =
        { "UNIDAD", "CAJA", "KILOGRAMO", "METRO", "LITRO", "MILLAR", "GALON", "JUEGO" };

    public static readonly string[] EstadosCotizacion =
        { "REGISTRADA", "ENVIADA", "ACEPTADA", "RECHAZADA", "VENCIDA", "ANULADA" };

    public static readonly CotizacionFila[] Cotizaciones =
    {
        new(1, "COT-2026-0001", "Metalmecánica Chosica E.I.R.L.", "01/09/2026", "15/09/2026", "ACEPTADA",
            "Entrega en obra, Chosica.", 1300.00m, 234.00m, 1534.00m,
            new LineaCotizacionFila[]
            {
                new(1, "INT-001 · Martillo de Acero", 20m, 45.00m, 0m, 900.00m),
                new(2, "INT-002 · Alicate de Presión", 10m, 35.00m, 0m, 350.00m),
                new(3, "INT-003 · Cable Mellizo 2x14", 20m, 2.50m, 0m, 50.00m),
            }),
        new(2, "COT-2026-0002", "Constructora Andina S.A.C.", "10/09/2026", "24/09/2026", "ENVIADA",
            null, 570.00m, 102.60m, 672.60m,
            new LineaCotizacionFila[]
            {
                new(1, "INT-005 · Bolsa Cemento Sol", 20m, 28.50m, 0m, 570.00m),
            }),
        new(3, "COT-2026-0003", "Pedro Huamaní Flores", "14/09/2026", "28/09/2026", "REGISTRADA",
            "Cliente pide precio por mayor.", 325.00m, 58.50m, 383.50m,
            new LineaCotizacionFila[]
            {
                new(1, "INT-004 · Enchufe Mennekes", 3m, 85.00m, 0m, 255.00m),
                new(2, "INT-002 · Alicate de Presión", 2m, 35.00m, 0m, 70.00m),
            }),
        new(4, "COT-2026-0004", "Instalaciones Eléctricas del Sur S.A.", "20/08/2026", "03/09/2026", "VENCIDA",
            null, 250.00m, 45.00m, 295.00m,
            new LineaCotizacionFila[]
            {
                new(1, "INT-003 · Cable Mellizo 2x14", 100m, 2.50m, 0m, 250.00m),
            }),
        new(5, "COT-2026-0005", "Metalmecánica Chosica E.I.R.L.", "22/09/2026", "06/10/2026", "RECHAZADA",
            null, 640.00m, 115.20m, 755.20m,
            new LineaCotizacionFila[]
            {
                new(1, "INT-006 · Fierro Corrugado", 20m, 32.00m, 0m, 640.00m),
            }),

        // Agregadas por Sebastian (rama lab09_sebastian_correcciones) para el flujo de Ventas:
        // COT-0006 y COT-0007 ya tienen orden de compra de cliente; COT-0008 todavía no. Pendiente revisar con Claudia.
        new(6, "COT-2026-0006", "Constructora Andina S.A.C.", "25/09/2026", "09/10/2026", "ACEPTADA",
            "Entrega en almacén del cliente, Ate.", 1080.00m, 194.40m, 1274.40m,
            new LineaCotizacionFila[]
            {
                new(1, "INT-005 · Bolsa Cemento Sol", 30m, 28.50m, 25.00m, 830.00m),
                new(2, "INT-003 · Cable Mellizo 2x14", 100m, 2.50m, 0m, 250.00m),
            }),
        new(7, "COT-2026-0007", "Metalmecánica Chosica E.I.R.L.", "05/10/2026", "19/10/2026", "ACEPTADA",
            null, 210.00m, 37.80m, 247.80m,
            new LineaCotizacionFila[]
            {
                new(1, "INT-002 · Alicate de Presión", 6m, 35.00m, 0m, 210.00m),
            }),
        new(8, "COT-2026-0008", "Constructora Andina S.A.C.", "20/10/2026", "03/11/2026", "ACEPTADA",
            null, 790.00m, 142.20m, 932.20m,
            new LineaCotizacionFila[]
            {
                new(1, "INT-004 · Enchufe Mennekes", 4m, 85.00m, 0m, 340.00m),
                new(2, "INT-001 · Martillo de Acero", 10m, 45.00m, 0m, 450.00m),
            }),
    };

    // Texto que se muestra en pantalla para los valores de los enums del Java.
    public static string Texto(string valor) => valor switch
    {
        "CONTADO" => "Contado", "CREDITO" => "Crédito",
        "UNIDAD" => "Unidad", "CAJA" => "Caja", "KILOGRAMO" => "Kilogramo", "METRO" => "Metro",
        "LITRO" => "Litro", "MILLAR" => "Millar", "GALON" => "Galón", "JUEGO" => "Juego",
        "REGISTRADA" => "Registrada", "ENVIADA" => "Enviada", "ACEPTADA" => "Aceptada",
        "RECHAZADA" => "Rechazada", "VENCIDA" => "Vencida", "ANULADA" => "Anulada",
        _ => valor
    };
}
