package pe.edu.pucp.sigmeta.bo.seguridad;

import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Rol;

import java.sql.SQLException;
import java.util.List;

public interface RolBO {
    Rol obtener(int idRol) throws SQLException;
    List<Rol> listarTodos() throws SQLException;
    Rol buscarPorTipo(TipoRol tipo) throws SQLException;
}
