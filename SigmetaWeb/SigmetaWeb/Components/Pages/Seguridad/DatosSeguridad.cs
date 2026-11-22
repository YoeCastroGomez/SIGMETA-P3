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
    // RF014: lo que el solicitante preparó en su pantalla (JSON) para retomar la operación; null si no se puede
    public string? DatosOperacion { get; set; }
    // RF014: la canceló su solicitante (no la rechazó un Administrador)
    public bool Cancelada { get; set; }

    // se puede volver a abrir la operación con esta solicitud (no se usó y tiene sus datos)
    public bool SePuedeRetomar => !string.IsNullOrEmpty(DatosOperacion) && Estado is "PENDIENTE" or "APROBADA" or "RECHAZADA" or "VENCIDA";
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
        "USADA" => "Usada",
        OperacionNotaCredito => "Emitir nota de crédito",
        OperacionAjuste => "Ajuste de inventario",
        OperacionDescuento => "Descuento especial",
        null or "" => "—",
        _ => valor
    };

    // RF014: estado tal como se muestra. Una rechazada sin Administrador la canceló su propio solicitante.
    public static string TextoEstado(SolicitudDatos s) => s.Cancelada ? "Cancelada" : Texto(s.Estado);

    // RF014: valor del filtro por estado (las canceladas van aparte de las rechazadas por el Administrador)
    public static string ClaveEstado(SolicitudDatos s) => s.Cancelada ? "CANCELADA" : s.Estado;

    // RF014: qué pasa con la solicitud, en una frase corta (la leen el solicitante y el Administrador)
    public static string QuePasa(SolicitudDatos s) => s.Cancelada ? "La canceló su propio solicitante." : s.Estado switch
    {
        "PENDIENTE" => "Espera la respuesta del Administrador.",
        "APROBADA" => $"Autorizada hasta {Formato.FechaHora(s.FechaVencimiento)}: ya se puede completar la operación.",
        "RECHAZADA" => "El Administrador la rechazó. Se pueden corregir los datos y volver a pedirla.",
        "VENCIDA" => "Pasó su vigencia o el Administrador la revocó sin que se usara.",
        "USADA" => "Se usó en la operación que autorizaba.",
        _ => ""
    };

    // RF014: pantalla donde se retoma la operación con esta solicitud (null si no se puede retomar ahí)
    public static string? RutaRetomar(SolicitudDatos s)
    {
        if (!s.SePuedeRetomar) return null;
        return s.Operacion switch
        {
            OperacionNotaCredito => $"/notas-credito/nueva?solicitud={s.Id}",
            OperacionAjuste => $"/almacen/ajuste?solicitud={s.Id}",
            OperacionDescuento => IdVentaDe(s) is int idVenta and > 0
                ? $"/ventas/editar/{idVenta}?solicitud={s.Id}" : $"/ventas/nueva?solicitud={s.Id}",
            _ => null
        };
    }

    // texto del botón que retoma la operación, según cómo va la solicitud
    public static string TextoRetomar(SolicitudDatos s) => s.Estado switch
    {
        "APROBADA" => "Completar",
        "PENDIENTE" => "Ver espera",
        _ => "Corregir"
    };

    // la venta guardada en los datos de una solicitud de descuento (0 = venta nueva)
    private static int? IdVentaDe(SolicitudDatos s)
    {
        try
        {
            using var doc = System.Text.Json.JsonDocument.Parse(s.DatosOperacion!);
            return doc.RootElement.TryGetProperty("id", out var id) && id.TryGetInt32(out var v) ? v : null;
        }
        catch (System.Text.Json.JsonException)
        {
            return null;
        }
    }

    // "Cliente: X\nVenta: Y\nDevuelve:\n- 8 MARTILLO: S/ 320.00" -> filas para mostrar lo que se autoriza
    public static List<LineaDetalle> LeerDetalle(string? detalle)
    {
        var filas = new List<LineaDetalle>();
        foreach (var crudo in (detalle ?? "").Split('\n'))
        {
            var linea = crudo.Trim();
            if (linea.Length == 0) continue;
            if (linea.StartsWith("- "))
            {
                filas.Add(new LineaDetalle(null, linea[2..], true));
                continue;
            }
            var dosPuntos = linea.IndexOf(':');
            if (dosPuntos > 0 && dosPuntos < 40)
            {
                var valor = linea[(dosPuntos + 1)..].Trim();
                filas.Add(new LineaDetalle(linea[..dosPuntos].Trim(), valor.Length == 0 ? null : valor, false));
            }
            else
            {
                filas.Add(new LineaDetalle(null, linea, false));
            }
        }
        return filas;
    }

    // "hace 5 min", "hace 2 h", "hace 3 días" desde la fecha de la solicitud
    public static string Hace(string? fecha)
    {
        if (!DateTime.TryParse(fecha, System.Globalization.CultureInfo.InvariantCulture,
                System.Globalization.DateTimeStyles.None, out var f)) return "";
        var t = DateTime.Now - f;
        if (t.TotalMinutes < 1) return "hace un momento";
        if (t.TotalMinutes < 60) return $"hace {(int)t.TotalMinutes} min";
        if (t.TotalHours < 24) return $"hace {(int)t.TotalHours} h";
        return $"hace {(int)t.TotalDays} día{((int)t.TotalDays == 1 ? "" : "s")}";
    }
}

// Una fila del detalle de una solicitud: "Clave: valor", un ítem de lista ("- ...") o texto suelto
public sealed record LineaDetalle(string? Clave, string? Valor, bool EsItem);
