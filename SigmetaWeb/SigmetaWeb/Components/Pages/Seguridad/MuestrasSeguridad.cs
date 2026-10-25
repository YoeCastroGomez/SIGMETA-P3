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
        new(1, "admin",    "Rosa",  "Quispe Huamán", "rquispe@grupometa.pe",  "ADMINISTRADOR", true,  "01/09/2026 08:00"),
        new(2, "jtorres",  "Jorge", "Torres Salas",  "jtorres@grupometa.pe",  "VENDEDOR",      true,  "02/09/2026 09:15"),
        new(3, "mvargas",  "María", "Vargas Ccori",  "mvargas@grupometa.pe",  "CAJERO",        true,  "02/09/2026 09:20"),
        new(4, "lpaucar",  "Luis",  "Paucar Rojas",  null,                    "ALMACENERO",    true,  "03/09/2026 10:05"),
        new(5, "cmendoza", "Carla", "Mendoza Ríos",  "cmendoza@grupometa.pe", "VENDEDOR",      false, "05/09/2026 11:30"),
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
        new(1, "jtorres", "VENDEDOR",   "DESCUENTO_ESPECIAL", "Cliente frecuente, compra por volumen",   "24/10/2026 10:15", "PENDIENTE"),
        new(2, "mvargas", "CAJERO",     "NOTA_CREDITO",       "Devolución de 5 enchufes defectuosos",    "24/10/2026 11:40", "APROBADA",  "admin", "24/10/2026 11:45", 30, "24/10/2026 12:15"),
        new(3, "lpaucar", "ALMACENERO", "AJUSTE_INVENTARIO",  "Diferencia de 2 unidades en cable 4 mm",  "23/10/2026 17:05", "RECHAZADA", "admin", "23/10/2026 17:30"),
        new(4, "jtorres", "VENDEDOR",   "ANULAR_VENTA",       "El cliente canceló antes del despacho",   "22/10/2026 09:30", "VENCIDA",   "admin", "22/10/2026 09:35", 15, "22/10/2026 09:50"),
    };

    // Vista del solicitante: un ejemplo fijo por rol.
    public static readonly Dictionary<string, SolicitudFila[]> MisSolicitudes = new()
    {
        ["Vendedor"] = new[] { Solicitudes[0], Solicitudes[3] },
        ["Cajero"] = new[] { Solicitudes[1] },
        ["Almacenero"] = new[] { Solicitudes[2] },
    };

    // Sugerencias que aparecen al escribir la operación (solo visual).
    public static readonly Dictionary<string, string[]> OperacionesSugeridas = new()
    {
        ["Vendedor"] = new[] { "DESCUENTO_ESPECIAL", "ANULAR_VENTA" },
        ["Cajero"] = new[] { "NOTA_CREDITO", "ANULAR_COBRO" },
        ["Almacenero"] = new[] { "AJUSTE_INVENTARIO" },
    };

    public static readonly Dictionary<string, string> UsuarioPorRol = new()
    {
        ["Administrador"] = "admin", ["Vendedor"] = "jtorres", ["Cajero"] = "mvargas", ["Almacenero"] = "lpaucar",
    };

    public static string Texto(string valor) => valor switch
    {
        "ADMINISTRADOR" => "Administrador", "VENDEDOR" => "Vendedor", "CAJERO" => "Cajero", "ALMACENERO" => "Almacenero",
        "PENDIENTE" => "Pendiente", "APROBADA" => "Aprobada", "RECHAZADA" => "Rechazada", "VENCIDA" => "Vencida",
        _ => valor
    };
}
