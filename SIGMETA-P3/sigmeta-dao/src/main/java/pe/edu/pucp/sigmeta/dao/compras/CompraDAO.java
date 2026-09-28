
package pe.edu.pucp.sigmeta.dao.compras;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.compras.Compra;

import java.sql.SQLException;
import java.util.List;

public interface CompraDAO extends BaseDAO<Compra, Integer> {

    List<Compra> listAll() throws SQLException;

}
