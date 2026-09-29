package pe.edu.pucp.sigmeta.dao.almacen;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;

import java.sql.SQLException;
import java.util.List;

public interface DetalleRecepcionCompraDAO extends BaseDAO<DetalleRecepcionCompra, Integer> {

    List<DetalleRecepcionCompra> listAll() throws SQLException;
    List<DetalleRecepcionCompra> listarPorRecepcion(int idRecepcion) throws SQLException;

}
