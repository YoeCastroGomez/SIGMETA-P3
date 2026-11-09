namespace SigmetaWeb.Components.Pages.Seguridad;

// Datos de ejemplo fijos (Lab 9: sin BD ni servicios). Se reemplazan al integrar con Java.
// Los nombres de los campos son los mismos que en las clases Usuario, Rol y SolicitudAutorizacion del proyecto Java.
public record UsuarioFila(int Id, string NombreUsuario, string Nombres, string Apellidos, string? Correo,
                          string Rol, bool Estado, string FechaRegistro);

public record RolFila(string Tipo, string Descripcion, bool Estado);

public record SolicitudFila(int Id, string Solicitante, string Rol, string OperacionRestringida, string Motivo,
                            string FechaSolicitud, string Estado, string? Administrador = null,
                            string? FechaResolucion = null, int? VigenciaMinutos = null, string? FechaVencimiento = null);

public static class MuestrasSeguridad
{
    public static readonly string[] Roles = { "ADMINISTRADOR", "VENDEDOR", "CAJERO", "ALMACENERO" };

    public static readonly UsuarioFila[] Usuarios =
    {
        // Mismos usuarios que el script SQL (01_maestros.sql), clave Meta2026
        new(1, "lrojas",   "Lucía", "Rojas Vega",     "lrojas@grupometa.pe",   "VENDEDOR",      true,  "20/08/2026 08:00"),
        new(2, "mchavez",  "Marco", "Chávez Ríos",    "mchavez@grupometa.pe",  "CAJERO",        true,  "20/08/2026 08:00"),
        new(3, "jtorres",  "Jorge", "Torres Peña",    "jtorres@grupometa.pe",  "ALMACENERO",    true,  "20/08/2026 08:00"),
        new(4, "aquispe",  "Ana",   "Quispe Mendoza", "aquispe@grupometa.pe",  "ADMINISTRADOR", true,  "20/08/2026 08:00"),
        new(5, "cmendoza", "Carla", "Mendoza Ríos",   "cmendoza@grupometa.pe", "VENDEDOR",      false, "05/09/2026 11:30"),
    };

    public static readonly RolFila[] RolesDetalle =
    {
        new("ADMINISTRADOR", "Gestiona usuarios, maestros, crédito, compras y reportes; aprueba autorizaciones.", true),
        new("VENDEDOR", "Registra clientes, cotizaciones, órdenes de clientes y ventas.", true),
        new("CAJERO", "Opera la caja, registra cobros y notas de crédito.", true),
        new("ALMACENERO", "Registra recepciones, despachos y ajustes de inventario.", true),
    };

    // Vista del Administrador.
    public static readonly SolicitudFila[] Solicitudes =
    {
        new(1, "lrojas", "VENDEDOR",   "DESCUENTO_ESPECIAL", "Cliente frecuente, compra por volumen",   "24/10/2026 10:15", "PENDIENTE"),
        // RF013/RF014: autorización vigente del Cajero para emitir una nota de crédito
        new(2, "mchavez", "CAJERO",     OperacionNotaCredito, "Devolución de 2 alicates con falla",      "25/10/2026 15:10", "APROBADA",  "aquispe", "25/10/2026 15:15", 30, "25/10/2026 15:45"),
        new(3, "jtorres", "ALMACENERO", "AJUSTE_INVENTARIO",  "Diferencia de 2 unidades en cable 4 mm",  "23/10/2026 17:05", "RECHAZADA", "aquispe", "23/10/2026 17:30"),
        new(4, "lrojas", "VENDEDOR",   "ANULAR_VENTA",       "El cliente canceló antes del despacho",   "22/10/2026 09:30", "VENCIDA",   "aquispe", "22/10/2026 09:35", 15, "22/10/2026 09:50"),
        // Autorización ya usada: al registrar la nota BC01-00000003 se consumió (APROBADA -> VENCIDA)
        new(5, "mchavez", "CAJERO",     OperacionNotaCredito, "Alicate con falla en el mecanismo (B001-00000045)", "25/10/2026 09:00", "VENCIDA", "aquispe", "25/10/2026 09:05", 30, "25/10/2026 09:35"),
    };

    // ComprobanteBOImpl.OPERACION_NOTA_CREDITO
    public const string OperacionNotaCredito = "EMITIR_NOTA_CREDITO";

    // Autorización APROBADA y vigente del usuario para la operación (SolicitudAutorizacionBO.obtenerAutorizacionVigente).
    public static SolicitudFila? AutorizacionVigente(string usuario, string operacion) =>
        Solicitudes.FirstOrDefault(s => s.Solicitante == usuario && s.OperacionRestringida == operacion && s.Estado == "APROBADA");

    // Vista del solicitante: un ejemplo fijo por rol.
    public static readonly Dictionary<string, SolicitudFila[]> MisSolicitudes = new()
    {
        ["Vendedor"] = new[] { Solicitudes[0], Solicitudes[3] },
        ["Cajero"] = new[] { Solicitudes[1], Solicitudes[4] },
        ["Almacenero"] = new[] { Solicitudes[2] },
    };

    // Sugerencias que aparecen al escribir la operación (solo visual).
    public static readonly Dictionary<string, string[]> OperacionesSugeridas = new()
    {
        ["Vendedor"] = new[] { "DESCUENTO_ESPECIAL", "ANULAR_VENTA" },
        ["Cajero"] = new[] { OperacionNotaCredito, "ANULAR_COBRO" },
        ["Almacenero"] = new[] { "AJUSTE_INVENTARIO" },
    };

    public static readonly Dictionary<string, string> UsuarioPorRol = new()
    {
        ["Administrador"] = "aquispe", ["Vendedor"] = "lrojas", ["Cajero"] = "mchavez", ["Almacenero"] = "jtorres",
    };

    public static string Texto(string valor) => valor switch
    {
        "ADMINISTRADOR" => "Administrador", "VENDEDOR" => "Vendedor", "CAJERO" => "Cajero", "ALMACENERO" => "Almacenero",
        "PENDIENTE" => "Pendiente", "APROBADA" => "Aprobada", "RECHAZADA" => "Rechazada", "VENCIDA" => "Vencida",
        _ => valor
    };
}
