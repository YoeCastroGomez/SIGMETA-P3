package pe.edu.pucp.sigmeta.dao.almacen;

import java.sql.SQLException;

public interface StockInventarioDAO {

    // Bloquea el producto en la transaccion actual y devuelve su existencia.
    double obtenerStockParaActualizar(int idProducto) throws SQLException;

    void actualizarStock(int idProducto, double nuevoStock) throws SQLException;
}
