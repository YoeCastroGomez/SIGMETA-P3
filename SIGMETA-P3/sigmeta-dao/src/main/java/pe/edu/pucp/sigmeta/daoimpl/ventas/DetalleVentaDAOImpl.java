package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.DetalleVentaDAO;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetalleVentaDAOImpl implements DetalleVentaDAO {

    private DetalleVenta mapear(ResultSet rs) throws SQLException {
        DetalleVenta d = new DetalleVenta();
        d.setId(rs.getInt("id"));

        Venta v = new Venta();
        v.setId(rs.getInt("id_venta"));
        d.setVenta(v);

        d.setNumeroLinea(rs.getInt("numero_linea"));

        Producto p = new Producto();
        p.setId(rs.getInt("id_producto"));
        d.setProducto(p);

        d.setCantidad(rs.getDouble("cantidad"));
        d.setPrecioUnitario(rs.getDouble("precio_unitario"));
        d.setDescuento(rs.getDouble("descuento"));
        d.setImporte(rs.getDouble("importe"));
        d.setCantidadDespachada(rs.getDouble("cantidad_despachada"));

        return d;
    }

    @Override
    public DetalleVenta load(Integer id) throws SQLException {
        String sql = "{CALL sp_detalle_venta_obtener(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    @Override
    public DetalleVenta save(DetalleVenta d) throws SQLException {
        String sql = "{CALL sp_detalle_venta_insertar(?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getVenta().getId());
            cs.setInt(2, d.getNumeroLinea());
            cs.setInt(3, d.getProducto().getId());
            cs.setDouble(4, d.getCantidad());
            cs.setDouble(5, d.getPrecioUnitario());
            cs.setDouble(6, d.getDescuento());
            cs.setDouble(7, d.getImporte());
            cs.setDouble(8, d.getCantidadDespachada());
            cs.registerOutParameter(9, Types.INTEGER);
            cs.execute();
            d.setId(cs.getInt(9));
            return d;
        }
    }

    @Override
    public DetalleVenta update(DetalleVenta d) throws SQLException {
        String sql = "{CALL sp_detalle_venta_modificar(?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.setInt(2, d.getNumeroLinea());
            cs.setInt(3, d.getProducto().getId());
            cs.setDouble(4, d.getCantidad());
            cs.setDouble(5, d.getPrecioUnitario());
            cs.setDouble(6, d.getDescuento());
            cs.setDouble(7, d.getImporte());
            cs.setDouble(8, d.getCantidadDespachada());
            cs.executeUpdate();
            return d;
        }
    }

    @Override
    public void remove(DetalleVenta d) throws SQLException {
        String sql = "{CALL sp_detalle_venta_eliminar(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public List<DetalleVenta> listarTodos() throws SQLException {
        String sql = "{CALL sp_detalle_venta_listar()}";
        List<DetalleVenta> lista = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public List<DetalleVenta> listarPorVenta(int idVenta) throws SQLException {
        String sql = "{CALL sp_detalle_venta_listar_por_venta(?)}";
        List<DetalleVenta> lista = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idVenta);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public void actualizarCantidadDespachada(int idDetalle, double cantidadDespachada) throws SQLException {
        String sql = "{CALL sp_detalle_venta_actualizar_despachado(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idDetalle);
            cs.setDouble(2, cantidadDespachada);
            cs.executeUpdate();
        }
    }
}
