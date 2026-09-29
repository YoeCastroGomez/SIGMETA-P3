package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.ventas.DetalleOrdenCompraCliente;

import java.sql.SQLException;
import java.util.List;

public interface DetalleOrdenCompraClienteDAO extends BaseDAO<DetalleOrdenCompraCliente, Integer> {
    List<DetalleOrdenCompraCliente> listarTodos() throws SQLException;
    List<DetalleOrdenCompraCliente> listarPorOrden(int idOrdenCompraCliente) throws SQLException;
    void actualizarCantidadAtendida(int idDetalle, double cantidadAtendida) throws SQLException;
}
