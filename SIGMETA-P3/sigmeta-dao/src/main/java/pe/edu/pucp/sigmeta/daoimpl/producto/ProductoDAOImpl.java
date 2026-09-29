package pe.edu.pucp.sigmeta.daoimpl.producto;

import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.model.enums.UnidadMedida;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;








public class ProductoDAOImpl implements ProductoDAO {


    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setCodigoInterno(rs.getString("codigo_interno"));
        p.setCodigoFabricante(rs.getString("codigo_fabricante"));
        p.setCodigoProveedor(rs.getString("codigo_proveedor"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));

        Categoria cat = new Categoria();
        cat.setId(rs.getInt("id_categoria"));
        p.setCategoria(cat);

        p.setUnidadCompra(UnidadMedida.valueOf(rs.getString("unidad_compra")));
        p.setUnidadVenta(UnidadMedida.valueOf(rs.getString("unidad_venta")));
        p.setFactorConversion(rs.getDouble("factor_conversion"));
        p.setPrecioVenta(rs.getDouble("precio_venta"));
        p.setCostoUnitario(rs.getDouble("costo_unitario"));
        p.setStockActual(rs.getDouble("stock_actual"));
        p.setStockMinimo(rs.getDouble("stock_minimo"));
        p.setImagen(rs.getString("imagen"));
        p.setPrecioReferencial(rs.getDouble("precio_referencial"));
        p.setEstado(rs.getBoolean("estado"));
        return p;
    }


    @Override
    public List<Producto> listarTodos() throws SQLException {
        String sql = "{CALL sp_producto_listar()}";
        List<Producto> productos = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) productos.add(mapearProducto(rs));
        }
        return productos;
    }


    @Override
    public Producto load(Integer id) throws SQLException {
        String sql = "{CALL sp_producto_obtener(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) return mapearProducto(rs);
            }
        }
        return null;
    }

    @Override
    public Producto save(Producto producto) throws SQLException {
        String sql = "{CALL sp_producto_insertar(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, producto.getCodigoInterno());
            cs.setString(3, producto.getCodigoFabricante());
            cs.setString(4, producto.getCodigoProveedor());
            cs.setString(5, producto.getNombre());
            cs.setString(6, producto.getDescripcion());
            cs.setInt(7, producto.getCategoria().getId()    );
            cs.setString(8, producto.getUnidadCompra().name());
            cs.setString(9, producto.getUnidadVenta().name());
            cs.setDouble(10, producto.getFactorConversion());
            cs.setDouble(11, producto.getPrecioVenta());
            cs.setDouble(12, producto.getCostoUnitario());
            cs.setDouble(13, producto.getStockActual());
            cs.setDouble(14, producto.getStockMinimo());
            cs.setString(15, producto.getImagen());
            cs.setDouble(16, producto.getPrecioReferencial());

            cs.execute();
            producto.setId(cs.getInt(1));
            return producto;
        }
    }

    @Override
    public Producto update(Producto producto) throws SQLException {
        String sql = "{CALL sp_producto_modificar(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, producto.getId());
            cs.setString(2, producto.getCodigoInterno());
            cs.setString(3, producto.getCodigoFabricante());
            cs.setString(4, producto.getCodigoProveedor());
            cs.setString(5, producto.getNombre());
            cs.setString(6, producto.getDescripcion());
            cs.setInt(7, producto.getCategoria().getId());
            cs.setString(8, producto.getUnidadCompra().name());
            cs.setString(9, producto.getUnidadVenta().name());
            cs.setDouble(10, producto.getFactorConversion());
            cs.setDouble(11, producto.getPrecioVenta());
            cs.setDouble(12, producto.getCostoUnitario());
            cs.setDouble(13, producto.getStockActual());
            cs.setDouble(14, producto.getStockMinimo());
            cs.setString(15, producto.getImagen());
            cs.setDouble(16, producto.getPrecioReferencial());
            cs.executeUpdate();
            return producto;
        }
    }

    @Override
    public void remove(Producto producto) throws SQLException {
        String sql = "{CALL sp_producto_eliminar(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, producto.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public List<Producto> listarPorEstado(boolean estado) throws SQLException {
        String sql = "{CALL sp_producto_listar_por_estado(?)}";
        List<Producto> lista = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setBoolean(1, estado);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) lista.add(mapearProducto(rs));
            }
        }
        return lista;
    }
}
