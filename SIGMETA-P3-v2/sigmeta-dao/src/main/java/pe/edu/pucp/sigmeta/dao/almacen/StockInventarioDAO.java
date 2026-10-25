package pe.edu.pucp.sigmeta.dao.almacen;

import java.sql.SQLException;
import java.util.List;
import pe.edu.pucp.sigmeta.model.producto.Producto;

public interface StockInventarioDAO {

    // Bloquea el producto en la transaccion actual y devuelve su existencia.
    double obtenerStockParaActualizar(int idProducto) throws SQLException;

    void actualizarStock(int idProducto, double nuevoStock) throws SQLException;
    List<Producto> listarEnStockMinimo() throws SQLException;
}
