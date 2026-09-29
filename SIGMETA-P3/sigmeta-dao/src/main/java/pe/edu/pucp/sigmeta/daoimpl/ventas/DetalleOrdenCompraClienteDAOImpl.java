package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.DetalleOrdenCompraClienteDAO;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.ventas.DetalleOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetalleOrdenCompraClienteDAOImpl implements DetalleOrdenCompraClienteDAO {

    private DetalleOrdenCompraCliente mapear(ResultSet rs) throws SQLException {
        DetalleOrdenCompraCliente d = new DetalleOrdenCompraCliente();
        d.setId(rs.getInt("id"));

        OrdenCompraCliente oc = new OrdenCompraCliente();
        oc.setId(rs.getInt("id_orden_compra_cliente"));
        d.setOrdenCompraCliente(oc);

        d.setNumeroLinea(rs.getInt("numero_linea"));

        Producto p = new Producto();
        p.setId(rs.getInt("id_producto"));
        d.setProducto(p);

        d.setCantidad(rs.getDouble("cantidad"));
        d.setPrecioUnitario(rs.getDouble("precio_unitario"));
        d.setDescuento(rs.getDouble("descuento"));
        d.setImporte(rs.getDouble("importe"));
        d.setCantidadAtendida(rs.getDouble("cantidad_atendida"));

        return d;
    }

    @Override
    public DetalleOrdenCompraCliente load(Integer id) throws SQLException {
        String sql = "{CALL sp_detalle_orden_compra_cliente_obtener(?)}";
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
    public DetalleOrdenCompraCliente save(DetalleOrdenCompraCliente d) throws SQLException {
        String sql = "{CALL sp_detalle_orden_compra_cliente_insertar(?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getOrdenCompraCliente().getId());
            cs.setInt(2, d.getNumeroLinea());
            cs.setInt(3, d.getProducto().getId());
            cs.setDouble(4, d.getCantidad());
            cs.setDouble(5, d.getPrecioUnitario());
            cs.setDouble(6, d.getDescuento());
            cs.setDouble(7, d.getImporte());
            cs.setDouble(8, d.getCantidadAtendida());
            cs.registerOutParameter(9, Types.INTEGER);
            cs.execute();
            d.setId(cs.getInt(9));
            return d;
        }
    }

    @Override
    public DetalleOrdenCompraCliente update(DetalleOrdenCompraCliente d) throws SQLException {
        String sql = "{CALL sp_detalle_orden_compra_cliente_modificar(?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.setInt(2, d.getNumeroLinea());
            cs.setInt(3, d.getProducto().getId());
            cs.setDouble(4, d.getCantidad());
            cs.setDouble(5, d.getPrecioUnitario());
            cs.setDouble(6, d.getDescuento());
            cs.setDouble(7, d.getImporte());
            cs.setDouble(8, d.getCantidadAtendida());
            cs.executeUpdate();
            return d;
        }
    }

    @Override
    public void remove(DetalleOrdenCompraCliente d) throws SQLException {
        String sql = "{CALL sp_detalle_orden_compra_cliente_eliminar(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public List<DetalleOrdenCompraCliente> listarTodos() throws SQLException {
        String sql = "{CALL sp_detalle_orden_compra_cliente_listar()}";
        List<DetalleOrdenCompraCliente> lista = new ArrayList<>();
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
    public List<DetalleOrdenCompraCliente> listarPorOrden(int idOrdenCompraCliente) throws SQLException {
        String sql = "{CALL sp_detalle_orden_compra_cliente_listar_por_orden(?)}";
        List<DetalleOrdenCompraCliente> lista = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idOrdenCompraCliente);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public void actualizarCantidadAtendida(int idDetalle, double cantidadAtendida) throws SQLException {
        String sql = "{CALL sp_detalle_orden_compra_cliente_actualizar_atendida(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idDetalle);
            cs.setDouble(2, cantidadAtendida);
            cs.executeUpdate();
        }
    }
}
