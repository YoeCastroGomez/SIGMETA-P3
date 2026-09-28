package pe.edu.pucp.sigmeta.dao.maestros;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;

import java.sql.SQLException;
import java.util.List;

public interface ProveedorDAO extends BaseDAO<Proveedor, Integer> {
    List<Proveedor> listarTodos() throws SQLException;
    Proveedor buscarPorRuc(String ruc) throws SQLException;
    List<Proveedor> buscarPorRazonSocial(String razonSocial) throws SQLException;
    List<Proveedor> listarPorEstado(boolean estado) throws SQLException;
}
