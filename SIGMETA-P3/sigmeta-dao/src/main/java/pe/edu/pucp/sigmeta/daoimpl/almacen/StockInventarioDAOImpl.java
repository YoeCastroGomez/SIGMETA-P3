package pe.edu.pucp.sigmeta.daoimpl.almacen;

import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StockInventarioDAOImpl implements StockInventarioDAO {

    @Override
    public double obtenerStockParaActualizar(int idProducto) throws SQLException {
        Connection conn = transactionContext.getConnection();
        String sql = "SELECT stock_actual FROM producto WHERE id = ? FOR UPDATE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("No existe el producto con ID " + idProducto);
                }
                return rs.getDouble("stock_actual");
            }
        }
    }

    @Override
    public void actualizarStock(int idProducto, double nuevoStock) throws SQLException {
        Connection conn = transactionContext.getConnection();
        String sql = "UPDATE producto SET stock_actual = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, nuevoStock);
            ps.setInt(2, idProducto);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No se pudo actualizar el stock del producto " + idProducto);
            }
        }
    }
}
