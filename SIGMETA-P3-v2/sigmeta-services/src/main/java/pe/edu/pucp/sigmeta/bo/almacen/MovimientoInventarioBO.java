package pe.edu.pucp.sigmeta.bo.almacen;

import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.producto.Producto;

import java.sql.SQLException;
import java.util.List;

/**
 * Los movimientos de compra, despacho y produccion los registran sus procesos de origen.
 * Este BO permite consultar el historial y registrar ajustes por conteo fisico.
 */
public interface MovimientoInventarioBO {

    /**
     * Uso interno por DespachoBO: revierte una salida concreta dentro de la
     * transaccion del llamador. No realiza commit, rollback ni close.
     * El llamador debe anular el despacho y confirmar todo atomicamente.
     */
    MovimientoInventario revertirSalidaDespacho(int idMovimientoSalida, int idDespacho,
                                                 int idAlmacenero, String motivo) throws SQLException;

    List<MovimientoInventario> revertirRecepcionCompra(int idRecepcion, int idAlmacenero,
                                                       String motivo) throws SQLException;

    MovimientoInventario revertirAjuste(int idMovimientoAjuste, int idAlmacenero,
                                        String motivo) throws SQLException;

    MovimientoInventario registrarAjuste(int idProducto, int idUsuario,
                                         double cantidadContada, String motivo) throws SQLException;

    MovimientoInventario modificarMotivo(int idMovimiento, String motivo, int idAdministrador) throws SQLException;

    MovimientoInventario obtener(int idMovimiento) throws SQLException;

    List<MovimientoInventario> listarTodos() throws SQLException;

    List<MovimientoInventario> listarPorProducto(int idProducto) throws SQLException;
    List<Producto> listarEnStockMinimo() throws SQLException;
    // RF012/RF014: piden la autorizacion de UN ajuste (o de su reversion) con sus datos; no mueven el stock
    pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarAjuste(int idProducto, int idUsuario,
            double cantidadContada, String motivo) throws SQLException;
    pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarReversionAjuste(int idMovimientoAjuste,
            int idAlmacenero, String motivo) throws SQLException;
}
