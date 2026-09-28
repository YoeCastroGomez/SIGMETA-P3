package pe.edu.pucp.sigmeta.bo.maestros;

import pe.edu.pucp.sigmeta.model.socio.Proveedor;

import java.sql.SQLException;
import java.util.List;

// RF004
public interface ProveedorBO {
    // solo el Administrador gestiona proveedores
    Proveedor registrar(Proveedor proveedor, int idAdministrador) throws SQLException;
    Proveedor modificar(Proveedor proveedor, int idAdministrador) throws SQLException;
    void desactivar(int idProveedor, int idAdministrador) throws SQLException;
    Proveedor obtener(int idProveedor) throws SQLException;
    List<Proveedor> listarTodos() throws SQLException;
    Proveedor buscarPorRuc(String ruc) throws SQLException;
    List<Proveedor> buscarPorRazonSocial(String razonSocial) throws SQLException;
    List<Proveedor> listarPorEstado(boolean estado) throws SQLException;
}
