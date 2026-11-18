namespace SigmetaWeb.Components.Pages.Seguridad;

// RF002 y RF014: datos de seguridad tal como los manda el Java (UsuarioRS y SolicitudRS).

// GET /api/usuarios: nunca trae el hash ni el salt de la clave
public class UsuarioDatos
{
    public int Id { get; set; }
    public string NombreUsuario { get; set; } = "";
    public string Nombres { get; set; } = "";
    public string Apellidos { get; set; } = "";
    public string? Correo { get; set; }
    public int IdRol { get; set; }
    public string? Rol { get; set; }
    public bool Estado { get; set; } = true;
    public string? FechaRegistro { get; set; }
    public string? Clave { get; set; }   // solo se manda al registrar
}

// GET /api/usuarios/roles
public class RolDatos
{
    public int Id { get; set; }
    public string Tipo { get; set; } = "";
    public string? Descripcion { get; set; }
    public bool Estado { get; set; }
    public int Usuarios { get; set; }
    public int UsuariosActivos { get; set; }
}

// GET /api/solicitudes
public class SolicitudDatos
{
    public int Id { get; set; }
    public int IdSolicitante { get; set; }
    public string? Solicitante { get; set; }
    public string? NombreSolicitante { get; set; }
    public string? RolSolicitante { get; set; }
    public string Operacion { get; set; } = "";
    public string? DescripcionOperacion { get; set; }
    public string? Motivo { get; set; }
    // RF014: datos de la operacion que se autoriza (comprobante, venta o producto y su detalle)
    public string? Referencia { get; set; }
    public string? Detalle { get; set; }
    public string? FechaSolicitud { get; set; }
    public string Estado { get; set; } = "";
    public bool Vigente { get; set; }
    public string? Administrador { get; set; }
    public string? FechaResolucion { get; set; }
    public int VigenciaMinutos { get; set; }
    public string? FechaVencimiento { get; set; }
}

// GET /api/solicitudes/operaciones: lo que puede pedir el rol del usuario conectado
public class OperacionDatos
{
    public string Codigo { get; set; } = "";
    public string Descripcion { get; set; } = "";
}

public static class TextosSeguridad
{
    // códigos del Java (SolicitudAutorizacionBOImpl)
    public const string OperacionNotaCredito = "EMITIR_NOTA_CREDITO";
    public const string OperacionAjuste = "AJUSTE_INVENTARIO";
    public const string OperacionDescuento = "DESCUENTO_ESPECIAL";

    public static string Texto(string? valor) => valor switch
    {
        "ADMINISTRADOR" => "Administrador", "VENDEDOR" => "Vendedor", "CAJERO" => "Cajero", "ALMACENERO" => "Almacenero",
        "PENDIENTE" => "Pendiente", "APROBADA" => "Aprobada", "RECHAZADA" => "Rechazada", "VENCIDA" => "Vencida",
        OperacionNotaCredito => "Emitir nota de crédito",
        OperacionAjuste => "Ajuste de inventario",
        OperacionDescuento => "Descuento especial",
        null or "" => "—",
        _ => valor
    };
}
