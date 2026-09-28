package pe.edu.pucp.sigmeta.dao.seguridad;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Rol;

import java.sql.SQLException;
import java.util.List;

public interface RolDAO extends BaseDAO<Rol, Integer> {
    List<Rol> listarTodos() throws SQLException;
    Rol buscarPorTipo(TipoRol tipo) throws SQLException;
}
