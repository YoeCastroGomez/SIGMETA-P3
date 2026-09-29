package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.DetalleDespachoDAO;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.DetalleDespacho;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetalleDespachoDAOImpl implements DetalleDespachoDAO {

    private DetalleDespacho mapear(ResultSet rs) throws SQLException {
        DetalleDespacho d = new DetalleDespacho();
        d.setId(rs.getInt("id"));

        Despacho desp = new Despacho();
        desp.setId(rs.getInt("id_despacho"));
        d.setDespacho(desp);

        Producto p = new Producto();
        p.setId(rs.getInt("id_producto"));
        d.setProducto(p);

        d.setCantidadDespachada(rs.getDouble("cantidad_despachada"));

        return d;
    }

    @Override
    public DetalleDespacho load(Integer id) throws SQLException {
        String sql = "{CALL sp_detalle_despacho_obtener(?)}";
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
    public DetalleDespacho save(DetalleDespacho d) throws SQLException {
        String sql = "{CALL sp_detalle_despacho_insertar(?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getDespacho().getId());
            cs.setInt(2, d.getProducto().getId());
            cs.setDouble(3, d.getCantidadDespachada());
            cs.registerOutParameter(4, Types.INTEGER);
            cs.execute();
            d.setId(cs.getInt(4));
            return d;
        }
    }

    @Override
    public DetalleDespacho update(DetalleDespacho d) throws SQLException {
        String sql = "{CALL sp_detalle_despacho_modificar(?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.setInt(2, d.getProducto().getId());
            cs.setDouble(3, d.getCantidadDespachada());
            cs.executeUpdate();
            return d;
        }
    }

    @Override
    public void remove(DetalleDespacho d) throws SQLException {
        String sql = "{CALL sp_detalle_despacho_eliminar(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public List<DetalleDespacho> listarTodos() throws SQLException {
        String sql = "{CALL sp_detalle_despacho_listar()}";
        List<DetalleDespacho> lista = new ArrayList<>();
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
    public List<DetalleDespacho> listarPorDespacho(int idDespacho) throws SQLException {
        String sql = "{CALL sp_detalle_despacho_listar_por_despacho(?)}";
        List<DetalleDespacho> lista = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idDespacho);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }
}
