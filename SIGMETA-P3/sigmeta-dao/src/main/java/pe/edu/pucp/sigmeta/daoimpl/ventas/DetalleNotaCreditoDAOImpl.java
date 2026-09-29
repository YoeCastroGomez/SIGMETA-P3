package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.DetalleNotaCreditoDAO;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetalleNotaCreditoDAOImpl implements DetalleNotaCreditoDAO {

    private DetalleNotaCredito mapear(ResultSet rs) throws SQLException {
        DetalleNotaCredito d = new DetalleNotaCredito();
        d.setId(rs.getInt("id"));

        Comprobante c = new Comprobante();
        c.setId(rs.getInt("id_comprobante"));
        d.setComprobante(c);

        d.setNumeroLinea(rs.getInt("numero_linea"));

        Producto p = new Producto();
        p.setId(rs.getInt("id_producto"));
        d.setProducto(p);

        d.setCantidad(rs.getDouble("cantidad"));
        d.setPrecioUnitario(rs.getDouble("precio_unitario"));
        d.setDescuento(rs.getDouble("descuento"));
        d.setImporte(rs.getDouble("importe"));

        return d;
    }

    @Override
    public DetalleNotaCredito load(Integer id) throws SQLException {
        String sql = "{CALL sp_detalle_nota_credito_obtener(?)}";
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
    public DetalleNotaCredito save(DetalleNotaCredito d) throws SQLException {
        String sql = "{CALL sp_detalle_nota_credito_insertar(?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getComprobante().getId());
            cs.setInt(2, d.getNumeroLinea());
            cs.setInt(3, d.getProducto().getId());
            cs.setDouble(4, d.getCantidad());
            cs.setDouble(5, d.getPrecioUnitario());
            cs.setDouble(6, d.getDescuento());
            cs.setDouble(7, d.getImporte());
            cs.registerOutParameter(8, Types.INTEGER);
            cs.execute();
            d.setId(cs.getInt(8));
            return d;
        }
    }

    @Override
    public DetalleNotaCredito update(DetalleNotaCredito d) throws SQLException {
        String sql = "{CALL sp_detalle_nota_credito_modificar(?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.setInt(2, d.getNumeroLinea());
            cs.setInt(3, d.getProducto().getId());
            cs.setDouble(4, d.getCantidad());
            cs.setDouble(5, d.getPrecioUnitario());
            cs.setDouble(6, d.getDescuento());
            cs.setDouble(7, d.getImporte());
            cs.executeUpdate();
            return d;
        }
    }

    @Override
    public void remove(DetalleNotaCredito d) throws SQLException {
        String sql = "{CALL sp_detalle_nota_credito_eliminar(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public List<DetalleNotaCredito> listarTodos() throws SQLException {
        String sql = "{CALL sp_detalle_nota_credito_listar()}";
        List<DetalleNotaCredito> lista = new ArrayList<>();
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
    public List<DetalleNotaCredito> listarPorComprobante(int idComprobante) throws SQLException {
        String sql = "{CALL sp_detalle_nota_credito_listar_por_comprobante(?)}";
        List<DetalleNotaCredito> lista = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idComprobante);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }
}
