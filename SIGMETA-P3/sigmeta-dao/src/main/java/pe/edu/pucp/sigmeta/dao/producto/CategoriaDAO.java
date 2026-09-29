package pe.edu.pucp.sigmeta.dao.producto;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.producto.Categoria;

import java.sql.SQLException;
import java.util.List;

public interface CategoriaDAO extends BaseDAO<Categoria, Integer> {
    List<Categoria> listarTodos() throws SQLException;
}
