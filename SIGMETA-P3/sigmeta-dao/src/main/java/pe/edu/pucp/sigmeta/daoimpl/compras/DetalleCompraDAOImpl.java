
package pe.edu.pucp.sigmeta.daoimpl.compras;

import pe.edu.pucp.sigmeta.dao.compras.DetalleCompraDAO;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.UnidadMedida;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DetalleCompraDAOImpl implements DetalleCompraDAO {

    @Override
    public DetalleCompra save(DetalleCompra detalle) throws SQLException {
        String sql = "{CALL sp_detalle_compra_insertar(?,?,?,?,?,?,?,?,?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, detalle.getCompra().getId());
            cs.setInt(2, detalle.getNumeroLinea());
            cs.setInt(3, detalle.getProducto().getId());
            cs.setDouble(4, detalle.getCantidad());
            cs.setDouble(5, detalle.getPrecioUnitario());
            cs.setDouble(6, detalle.getDescuento());
            cs.setDouble(7, detalle.getImporte());
            cs.setString(8, detalle.getUnidadCompra().name());
            cs.setDouble(9, detalle.getFactorConversion());
            cs.registerOutParameter(10, Types.INTEGER);

            cs.execute();

            detalle.setId(cs.getInt(10));
            detalle.setCantidadRecibida(0);
            return detalle;
        }
    }

    @Override
    public DetalleCompra load(Integer id) throws SQLException {
        String sql = "{CALL sp_detalle_compra_obtener(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapearDetalleCompra(rs);
                }
            }
        }

        return null;
    }

    @Override
    public DetalleCompra update(DetalleCompra detalle) throws SQLException {
        String sql = "{CALL sp_detalle_compra_modificar(?,?,?,?,?,?,?,?,?,?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, detalle.getId());
            cs.setInt(2, detalle.getCompra().getId());
            cs.setInt(3, detalle.getNumeroLinea());
            cs.setInt(4, detalle.getProducto().getId());
            cs.setDouble(5, detalle.getCantidad());
            cs.setDouble(6, detalle.getPrecioUnitario());
            cs.setDouble(7, detalle.getDescuento());
            cs.setDouble(8, detalle.getImporte());
            cs.setString(9, detalle.getUnidadCompra().name());
            cs.setDouble(10, detalle.getFactorConversion());
            cs.setDouble(11, detalle.getCantidadRecibida());

            cs.execute();
            return detalle;
        }
    }

    @Override
    public void remove(DetalleCompra detalle) throws SQLException {
        String sql = "{CALL sp_detalle_compra_eliminar(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, detalle.getId());
            cs.execute();
        }
    }

    @Override
    public List<DetalleCompra> listAll() throws SQLException {
        String sql = "{CALL sp_detalle_compra_listar()}";

        List<DetalleCompra> detalles = new ArrayList<>();
        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {

            while (rs.next()) {
                detalles.add(mapearDetalleCompra(rs));
            }
        }

        return detalles;
    }

    private DetalleCompra mapearDetalleCompra(ResultSet rs) throws SQLException {
        DetalleCompra detalle = new DetalleCompra();

        detalle.setId(rs.getInt("id"));
        detalle.setNumeroLinea(rs.getInt("numero_linea"));
        detalle.setCantidad(rs.getDouble("cantidad"));
        detalle.setPrecioUnitario(rs.getDouble("precio_unitario"));
        detalle.setDescuento(rs.getDouble("descuento"));
        detalle.setImporte(rs.getDouble("importe"));

        detalle.setUnidadCompra(
                UnidadMedida.valueOf(rs.getString("unidad_compra"))
        );

        detalle.setFactorConversion(rs.getDouble("factor_conversion"));
        detalle.setCantidadRecibida(rs.getDouble("cantidad_recibida"));

        Compra compra = new Compra();
        compra.setId(rs.getInt("id_compra"));
        detalle.setCompra(compra);

        Producto producto = new Producto();
        producto.setId(rs.getInt("id_producto"));
        detalle.setProducto(producto);

        return detalle;
    }
}
