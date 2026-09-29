package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;

import java.sql.SQLException;
import java.util.List;

public interface OrdenCompraClienteDAO extends BaseDAO<OrdenCompraCliente, Integer> {
    List<OrdenCompraCliente> listarTodos() throws SQLException;
    void actualizarEstado(int id, EstadoOrdenCompraCliente estado) throws SQLException;
    OrdenCompraCliente buscarPorCotizacion(int idCotizacion) throws SQLException;
}
