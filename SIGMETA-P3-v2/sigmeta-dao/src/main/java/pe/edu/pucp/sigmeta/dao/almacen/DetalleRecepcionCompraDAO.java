package pe.edu.pucp.sigmeta.dao.almacen;

import pe.edu.pucp.sigmeta.dao.BaseDAOHistorico;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;

import java.sql.SQLException;
import java.util.List;

public interface DetalleRecepcionCompraDAO extends BaseDAOHistorico<DetalleRecepcionCompra, Integer> {

    List<DetalleRecepcionCompra> listAll() throws SQLException;
    List<DetalleRecepcionCompra> listarPorRecepcion(int idRecepcion) throws SQLException;

}
