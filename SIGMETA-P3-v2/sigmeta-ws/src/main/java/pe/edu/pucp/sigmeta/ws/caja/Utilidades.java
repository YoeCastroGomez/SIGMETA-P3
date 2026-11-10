package pe.edu.pucp.sigmeta.ws.caja;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

// Ayudas comunes de las ventanillas de caja (RF010) y cobranza (RF009).
// Las fechas viajan como texto: AAAA-MM-DD y, con hora, AAAA-MM-DDTHH:MM:SS.
public final class Utilidades {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Utilidades() {
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    public static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? null : fecha.format(FECHA_HORA);
    }

    // texto AAAA-MM-DD a fecha; vacio = null
    public static LocalDate leerFecha(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La " + campo + " debe tener el formato AAAA-MM-DD");
        }
    }

    // texto del enum ("EFECTIVO", "INGRESO_MANUAL"...) a su valor; vacio = null (el BO avisa si es obligatorio)
    public static <E extends Enum<E>> E leerEnum(Class<E> tipo, String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El valor '" + texto + "' no es valido para " + campo);
        }
    }

    // usuario para mostrar: su nombre de usuario (nunca se manda el Usuario completo: tiene la clave)
    public static String usuario(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return usuario.getNombreUsuario() != null ? usuario.getNombreUsuario() : "usuario " + usuario.getId();
    }

    // RF001: las consultas de caja y cobranza son del Cajero y del Administrador. Lo verifica el BO de usuarios
    // con el usuario de la sesion y, si no corresponde, responde "El rol X no tiene permiso..."
    public static void permitirCajeroOAdministrador(int idUsuario) throws SQLException {
        new UsuarioBOImpl().verificarRol(idUsuario, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
    }
}
