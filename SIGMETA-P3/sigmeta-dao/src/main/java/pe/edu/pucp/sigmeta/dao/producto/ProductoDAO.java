package pe.edu.pucp.sigmeta.dao.producto;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.producto.Producto;

import java.sql.SQLException;
import java.util.List;

public interface ProductoDAO extends BaseDAO<Producto, Integer> {
    List<Producto> listarTodos() throws SQLException;
    List<Producto> listarPorEstado(boolean estado) throws SQLException;
}
