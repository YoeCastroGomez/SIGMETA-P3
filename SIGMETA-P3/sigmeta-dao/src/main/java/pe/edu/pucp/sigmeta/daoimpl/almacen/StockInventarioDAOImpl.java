package pe.edu.pucp.sigmeta.daoimpl.almacen;

import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    @Override
    public List<Producto> listarEnStockMinimo() throws SQLException {
        String sql = "SELECT id, codigo_interno, nombre, stock_actual, stock_minimo FROM producto WHERE estado = TRUE AND stock_actual <= stock_minimo ORDER BY stock_actual - stock_minimo, id";
        List<Producto> productos = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto producto = new Producto();
                producto.setId(rs.getInt("id"));
                producto.setCodigoInterno(rs.getString("codigo_interno"));
                producto.setNombre(rs.getString("nombre"));
                producto.setStockActual(rs.getDouble("stock_actual"));
                producto.setStockMinimo(rs.getDouble("stock_minimo"));
                productos.add(producto);
            }
        }
        return productos;
    }
}
