package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.almacen.DetalleDespacho;

import java.sql.SQLException;
import java.util.List;

public interface DetalleDespachoDAO extends BaseDAO<DetalleDespacho, Integer> {
    List<DetalleDespacho> listarTodos() throws SQLException;
    List<DetalleDespacho> listarPorDespacho(int idDespacho) throws SQLException;
}
