package pe.edu.pucp.sigmeta.dao.compras;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;

import java.sql.SQLException;
import java.util.List;

public interface DetalleCompraDAO extends BaseDAO<DetalleCompra, Integer> {

    List<DetalleCompra> listAll() throws SQLException;
    void descontarCantidadRecibida(int idDetalleCompra, double cantidad) throws SQLException;

}
