
package pe.edu.pucp.sigmeta.daoimpl.almacen;

import pe.edu.pucp.sigmeta.dao.almacen.DetalleRecepcionCompraDAO;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DetalleRecepcionCompraDAOImpl implements DetalleRecepcionCompraDAO {

    @Override
    public DetalleRecepcionCompra save(DetalleRecepcionCompra detalle)
            throws SQLException {

        String sql = "{CALL sp_detalle_recepcion_compra_insertar(?,?,?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, detalle.getRecepcionCompra().getId());
            cs.setInt(2, detalle.getDetalleCompra().getId());
            cs.setDouble(3, detalle.getCantidadRecibida());
            cs.registerOutParameter(4, Types.INTEGER);

            cs.execute();

            detalle.setId(cs.getInt(4));
            return detalle;
        }
    }

    @Override
    public DetalleRecepcionCompra load(Integer id) throws SQLException {
        String sql = "{CALL sp_detalle_recepcion_compra_obtener(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapearDetalleRecepcion(rs);
                }
            }
        }

        return null;
    }

    @Override
    public DetalleRecepcionCompra update(DetalleRecepcionCompra detalle)
            throws SQLException {

        String sql = "{CALL sp_detalle_recepcion_compra_modificar(?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, detalle.getId());
            cs.setDouble(2, detalle.getCantidadRecibida());

            cs.execute();
            return detalle;
        }
    }

    @Override
    public void remove(DetalleRecepcionCompra detalle) throws SQLException {
        String sql = "{CALL sp_detalle_recepcion_compra_eliminar(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, detalle.getId());
            cs.execute();
        }
    }

    @Override
    public List<DetalleRecepcionCompra> listAll() throws SQLException {
        String sql = "{CALL sp_detalle_recepcion_compra_listar()}";

        List<DetalleRecepcionCompra> detalles = new ArrayList<>();
        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {

            while (rs.next()) {
                detalles.add(mapearDetalleRecepcion(rs));
            }
        }

        return detalles;
    }

    private DetalleRecepcionCompra mapearDetalleRecepcion(ResultSet rs)
            throws SQLException {

        DetalleRecepcionCompra detalle = new DetalleRecepcionCompra();

        detalle.setId(rs.getInt("id"));
        detalle.setCantidadRecibida(rs.getDouble("cantidad_recibida"));

        RecepcionCompra recepcion = new RecepcionCompra();
        recepcion.setId(rs.getInt("id_recepcion_compra"));
        detalle.setRecepcionCompra(recepcion);

        DetalleCompra detalleCompra = new DetalleCompra();
        detalleCompra.setId(rs.getInt("id_detalle_compra"));
        detalle.setDetalleCompra(detalleCompra);

        return detalle;
    }
}
