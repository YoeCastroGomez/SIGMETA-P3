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
    // H09: siguiente numero del anio, reservado de forma atomica en correlativo_documento (tipo OCC)
    long siguienteCorrelativo(int anio) throws SQLException;
    // numero que tendria la proxima orden del anio, sin reservarlo (para mostrarlo en el formulario)
    long proximoCorrelativo(int anio) throws SQLException;
}
