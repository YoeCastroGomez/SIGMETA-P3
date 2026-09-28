package pe.edu.pucp.sigmeta.dao.seguridad;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.SQLException;
import java.util.List;

public interface UsuarioDAO extends BaseDAO<Usuario, Integer> {
    List<Usuario> listarTodos() throws SQLException;
    Usuario buscarPorNombreUsuario(String nombreUsuario) throws SQLException;
    List<Usuario> listarPorEstado(boolean estado) throws SQLException;
}
