package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;

import java.sql.SQLException;
import java.util.List;

public interface DetalleVentaDAO extends BaseDAO<DetalleVenta, Integer> {
    List<DetalleVenta> listarTodos() throws SQLException;
    List<DetalleVenta> listarPorVenta(int idVenta) throws SQLException;
    void actualizarCantidadDespachada(int idDetalle, double cantidadDespachada) throws SQLException;
}
