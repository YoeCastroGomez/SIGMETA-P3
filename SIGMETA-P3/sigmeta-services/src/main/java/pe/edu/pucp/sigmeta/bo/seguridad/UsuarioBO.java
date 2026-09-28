package pe.edu.pucp.sigmeta.bo.seguridad;

import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.SQLException;
import java.util.List;

public interface UsuarioBO {
    // RF001
    Usuario iniciarSesion(String nombreUsuario, String clave) throws SQLException;

    // RF002: la gestion de usuarios es exclusiva del Administrador
    Usuario registrar(Usuario usuario, String clave, int idAdministrador) throws SQLException;
    Usuario modificar(Usuario usuario, int idAdministrador) throws SQLException;
    void cambiarClave(int idUsuario, String claveActual, String claveNueva) throws SQLException;
    void restablecerClave(int idUsuario, String claveNueva, int idAdministrador) throws SQLException;
    void desactivar(int idUsuario, int idAdministrador) throws SQLException;
    Usuario obtener(int idUsuario) throws SQLException;
    List<Usuario> listarTodos() throws SQLException;
    List<Usuario> listarPorEstado(boolean estado) throws SQLException;

    // RF001: rechaza la operacion si el usuario no esta activo o su rol no es uno de los permitidos
    Usuario verificarRol(int idUsuario, TipoRol... rolesPermitidos) throws SQLException;
    Usuario obtenerAdministradorActivo(int idUsuario) throws SQLException;
}
