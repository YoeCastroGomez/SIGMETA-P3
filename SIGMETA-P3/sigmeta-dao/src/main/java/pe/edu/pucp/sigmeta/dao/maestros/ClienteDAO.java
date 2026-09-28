package pe.edu.pucp.sigmeta.dao.maestros;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.socio.Cliente;

import java.sql.SQLException;
import java.util.List;

public interface ClienteDAO extends BaseDAO<Cliente, Integer> {
    List<Cliente> listarTodos() throws SQLException;
    Cliente buscarPorDocumento(String numeroDocumento) throws SQLException;
    List<Cliente> buscarPorRazonSocial(String razonSocial) throws SQLException;
    List<Cliente> listarPorEstado(boolean estado) throws SQLException;
}
