
package pe.edu.pucp.sigmeta.daoimpl.almacen;

import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioDAOImpl implements MovimientoInventarioDAO {

    @Override
    public MovimientoInventario save(MovimientoInventario movimiento)
            throws SQLException {

        String sql = "{CALL sp_movimiento_inventario_insertar(?,?,?,?,?,?,?,?,?,?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, movimiento.getProducto().getId());
            cs.setString(2, movimiento.getTipo().name());
            cs.setDouble(3, movimiento.getCantidad());
            cs.setDouble(4, movimiento.getStockResultante());

            if (movimiento.getRecepcionCompra() != null) {
                cs.setInt(5, movimiento.getRecepcionCompra().getId());
            } else {
                cs.setNull(5, Types.INTEGER);
            }

            if (movimiento.getDespacho() != null) {
                cs.setInt(6, movimiento.getDespacho().getId());
            } else {
                cs.setNull(6, Types.INTEGER);
            }

            if (movimiento.getNotaCredito() != null) {
                cs.setInt(7, movimiento.getNotaCredito().getId());
            } else {
                cs.setNull(7, Types.INTEGER);
            }

            cs.setInt(8, movimiento.getUsuarioRegistro().getId());
            cs.setString(9, movimiento.getMotivo());

            if (movimiento.getTipo() == TipoMovimientoInventario.AJUSTE_INGRESO
                    || movimiento.getTipo() == TipoMovimientoInventario.AJUSTE_SALIDA) {
                cs.setDouble(10, movimiento.getCantidadContada());
            } else {
                cs.setNull(10, Types.DECIMAL);
            }

            cs.registerOutParameter(11, Types.INTEGER);
            cs.execute();

            movimiento.setId(cs.getInt(11));
            return movimiento;
        }
    }

    @Override
    public MovimientoInventario load(Integer id) throws SQLException {
        String sql = "{CALL sp_movimiento_inventario_obtener(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapearMovimiento(rs);
                }
            }
        }

        return null;
    }

    @Override
    public MovimientoInventario update(MovimientoInventario movimiento)
            throws SQLException {

        String sql = "{CALL sp_movimiento_inventario_modificar(?,?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, movimiento.getId());
            cs.setString(2, movimiento.getMotivo());

            if (movimiento.getTipo() == TipoMovimientoInventario.AJUSTE_INGRESO
                    || movimiento.getTipo() == TipoMovimientoInventario.AJUSTE_SALIDA) {
                cs.setDouble(3, movimiento.getCantidadContada());
            } else {
                cs.setNull(3, Types.DECIMAL);
            }

            cs.execute();
            return movimiento;
        }
    }

    @Override
    public void remove(MovimientoInventario movimiento) throws SQLException {
        String sql = "{CALL sp_movimiento_inventario_eliminar(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, movimiento.getId());
            cs.execute();
        }
    }

    @Override
    public List<MovimientoInventario> listAll() throws SQLException {
        String sql = "{CALL sp_movimiento_inventario_listar()}";

        List<MovimientoInventario> movimientos = new ArrayList<>();
        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {

            while (rs.next()) {
                movimientos.add(mapearMovimiento(rs));
            }
        }

        return movimientos;
    }

    @Override
    public List<MovimientoInventario> listarPorProducto(int idProducto) throws SQLException {
        String sql = "SELECT * FROM movimiento_inventario WHERE id_producto = ? ORDER BY fecha_movimiento, id";
        List<MovimientoInventario> movimientos = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(mapearMovimiento(rs));
                }
            }
        }
        return movimientos;
    }

    private MovimientoInventario mapearMovimiento(ResultSet rs)
            throws SQLException {

        MovimientoInventario movimiento = new MovimientoInventario();

        movimiento.setId(rs.getInt("id"));

        movimiento.setTipo(
                TipoMovimientoInventario.valueOf(rs.getString("tipo"))
        );

        Timestamp fechaMovimiento = rs.getTimestamp("fecha_movimiento");
        if (fechaMovimiento != null) {
            movimiento.setFechaMovimiento(fechaMovimiento.toLocalDateTime());
        }

        movimiento.setCantidad(rs.getDouble("cantidad"));
        movimiento.setStockResultante(rs.getDouble("stock_resultante"));
        movimiento.setMotivo(rs.getString("motivo"));
        movimiento.setCantidadContada(rs.getDouble("cantidad_contada"));

        Producto producto = new Producto();
        producto.setId(rs.getInt("id_producto"));
        movimiento.setProducto(producto);

        int idRecepcion = rs.getInt("id_recepcion_compra");
        if (!rs.wasNull()) {
            RecepcionCompra recepcion = new RecepcionCompra();
            recepcion.setId(idRecepcion);
            movimiento.setRecepcionCompra(recepcion);
        }

        int idDespacho = rs.getInt("id_despacho");
        if (!rs.wasNull()) {
            Despacho despacho = new Despacho();
            despacho.setId(idDespacho);
            movimiento.setDespacho(despacho);
        }

        int idComprobante = rs.getInt("id_comprobante");
        if (!rs.wasNull()) {
            Comprobante comprobante = new Comprobante();
            comprobante.setId(idComprobante);
            movimiento.setNotaCredito(comprobante);
        }

        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id_usuario_registro"));
        movimiento.setUsuarioRegistro(usuario);

        return movimiento;
    }
}

