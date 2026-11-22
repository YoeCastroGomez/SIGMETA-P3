package pe.edu.pucp.sigmeta.daoimpl.almacen;

import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

// Lectura de una fila de movimiento_inventario (ResultSet) al modelo MovimientoInventario.
// Parte de MovimientoInventarioDAOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class MovimientoInventarioMapeo {

    static MovimientoInventario mapearMovimiento(ResultSet rs)
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
        int idOriginal = rs.getInt("id_movimiento_revertido");
        if (!rs.wasNull()) movimiento.setIdMovimientoRevertido(idOriginal);

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
            movimiento.setComprobante(comprobante);
        }

        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id_usuario_registro"));
        movimiento.setUsuarioRegistro(usuario);

        return movimiento;
    }
}
