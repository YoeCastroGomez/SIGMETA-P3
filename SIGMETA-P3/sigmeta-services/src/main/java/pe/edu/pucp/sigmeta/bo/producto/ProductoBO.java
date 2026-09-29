package pe.edu.pucp.sigmeta.bo.producto;

import pe.edu.pucp.sigmeta.model.producto.Producto;

import java.sql.SQLException;
import java.util.List;

// RF005
public interface ProductoBO {
    // idResponsable: usuario en sesion; debe ser Administrador (incluye la modificacion del precio)
    Producto registrar(Producto producto, int idResponsable) throws SQLException;
    Producto modificar(Producto producto, int idResponsable) throws SQLException;
    void desactivar(int idProducto, int idResponsable) throws SQLException;

    // consultas abiertas a todos los roles
    Producto obtener(int idProducto) throws SQLException;
    List<Producto> listarTodos() throws SQLException;
    // busca por cualquiera de los tres codigos (lector de codigo de barras o busqueda escrita)
    Producto buscarPorCodigo(String codigo) throws SQLException;
    List<Producto> buscarPorNombre(String nombre) throws SQLException;
    List<Producto> listarPorCategoria(int idCategoria) throws SQLException;
    List<Producto> listarPorEstado(boolean estado) throws SQLException;

    // RF012: productos en su stock minimo o por debajo
    List<Producto> listarConStockBajo() throws SQLException;
}
