package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;

import java.sql.SQLException;
import java.util.List;

public interface DetalleNotaCreditoDAO extends BaseDAO<DetalleNotaCredito, Integer> {
    List<DetalleNotaCredito> listarTodos() throws SQLException;
    List<DetalleNotaCredito> listarPorComprobante(int idComprobante) throws SQLException;
}
