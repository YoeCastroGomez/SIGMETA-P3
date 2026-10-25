package pe.edu.pucp.sigmeta.daoimpl.almacen;

import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockInventarioDAOImpl implements StockInventarioDAO {

    @Override
    public double obtenerStockParaActualizar(int idProducto) throws SQLException {
        Connection conn = transactionContext.getConnection();
        String sql = "{call sp_stock_obtener_para_actualizar(?)}";

        try (CallableStatement ps = conn.prepareCall(sql)) {
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
        String sql = "{call sp_stock_actualizar(?, ?)}";

        try (CallableStatement ps = conn.prepareCall(sql)) {
            ps.setDouble(1, nuevoStock);
            ps.setInt(2, idProducto);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No se pudo actualizar el stock del producto " + idProducto);
            }
        }
    }
    @Override
    public List<Producto> listarEnStockMinimo() throws SQLException {
        String sql = "{call sp_stock_listar_minimos()}";
        List<Producto> productos = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement ps = conn.prepareCall(sql);
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
