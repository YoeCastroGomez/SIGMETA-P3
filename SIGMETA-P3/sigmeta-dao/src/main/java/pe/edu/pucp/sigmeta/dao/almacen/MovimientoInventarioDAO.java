package pe.edu.pucp.sigmeta.dao.almacen;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;

import java.sql.SQLException;
import java.util.List;

public interface MovimientoInventarioDAO extends BaseDAO<MovimientoInventario, Integer> {

    List<MovimientoInventario> listAll() throws SQLException;
    List<MovimientoInventario> listarPorProducto(int idProducto) throws SQLException;

}
