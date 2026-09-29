package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.DetalleCotizacionDAO;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleCotizacion;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DetalleCotizacionDAOImpl implements DetalleCotizacionDAO {


    private DetalleCotizacion mapearDetalle(ResultSet rs) throws SQLException {
        DetalleCotizacion d = new DetalleCotizacion();
        d.setId(rs.getInt("id"));
        d.setNumeroLinea(rs.getInt("numero_linea"));
        d.setCantidad(rs.getDouble("cantidad"));
        d.setPrecioUnitario(rs.getDouble("precio_unitario"));
        d.setDescuento(rs.getDouble("descuento"));
        d.setImporte(rs.getDouble("importe"));

        Producto p = new Producto();
        p.setId(rs.getInt("id_producto"));
        d.setProducto(p);

        Cotizacion c = new Cotizacion();
        c.setId(rs.getInt("id_cotizacion"));
        d.setCotizacion(c);

        return d;
    }


    @Override
    public List<DetalleCotizacion> listar_por_cotizacion(Integer idCotizacion) throws SQLException {
        String sql = "{CALL sp_detalle_cotizacion_listar(?)}";
        List<DetalleCotizacion> detalles = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idCotizacion);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) detalles.add(mapearDetalle(rs));
            }
        }
        return detalles;
    }

    @Override
    public DetalleCotizacion load(Integer id) throws SQLException {
        String sql = "{CALL sp_detalle_cotizacion_obtener(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) return mapearDetalle(rs);
            }
        }
        return null;
    }

    @Override
    public DetalleCotizacion save(DetalleCotizacion det) throws SQLException {
        String sql = "{CALL sp_detalle_cotizacion_insertar(?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            // El BO se encarga de setear el id de la cabecera en el detalle
            cs.setInt(2, det.getCotizacion().getId());
            cs.setInt(3, det.getNumeroLinea());
            cs.setInt(4, det.getProducto().getId());
            cs.setDouble(5, det.getCantidad());
            cs.setDouble(6, det.getPrecioUnitario());
            cs.setDouble(7, det.getDescuento());
            cs.setDouble(8, det.getImporte());
            cs.execute();
            det.setId(cs.getInt(1));
            return det;
        }
    }

    @Override
    public DetalleCotizacion update(DetalleCotizacion det) throws SQLException {
        String sql = "{CALL sp_detalle_cotizacion_modificar(?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, det.getId());
            cs.setInt(2, det.getNumeroLinea());
            cs.setInt(3, det.getProducto().getId());
            cs.setDouble(4, det.getCantidad());
            cs.setDouble(5, det.getPrecioUnitario());
            cs.setDouble(6, det.getDescuento());
            cs.setDouble(7, det.getImporte());
            cs.executeUpdate();
            return det;
        }
    }

    @Override
    public void remove(DetalleCotizacion det) throws SQLException {
        String sql = "{CALL sp_detalle_cotizacion_eliminar(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, det.getId() );
            cs.execute();
        }
    }
}
