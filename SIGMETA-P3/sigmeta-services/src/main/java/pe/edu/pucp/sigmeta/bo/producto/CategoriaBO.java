package pe.edu.pucp.sigmeta.bo.producto;

import pe.edu.pucp.sigmeta.model.producto.Categoria;

import java.sql.SQLException;
import java.util.List;

// RF006
public interface CategoriaBO {
    // idResponsable: usuario en sesion; debe ser Administrador
    Categoria registrar(Categoria categoria, int idResponsable) throws SQLException;
    Categoria modificar(Categoria categoria, int idResponsable) throws SQLException;
    void desactivar(int idCategoria, int idResponsable) throws SQLException;
    Categoria obtener(int idCategoria) throws SQLException;
    List<Categoria> listarTodos() throws SQLException;
    List<Categoria> buscarPorNombre(String nombre) throws SQLException;
    List<Categoria> listarPorEstado(boolean estado) throws SQLException;
}
