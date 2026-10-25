package pe.edu.pucp.sigmeta.dao.almacen;

import pe.edu.pucp.sigmeta.dao.BaseDAOHistorico;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;

import java.sql.SQLException;
import java.util.List;

public interface RecepcionCompraDAO extends BaseDAOHistorico<RecepcionCompra, Integer> {

    List<RecepcionCompra> listAll() throws SQLException;
    void marcarAnulada(int idRecepcion) throws SQLException;

}
