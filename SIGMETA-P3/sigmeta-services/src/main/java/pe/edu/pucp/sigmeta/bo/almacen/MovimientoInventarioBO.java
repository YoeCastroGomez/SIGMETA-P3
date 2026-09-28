package pe.edu.pucp.sigmeta.bo.almacen;

import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;

import java.sql.SQLException;
import java.util.List;

/**
 * Los movimientos de compra, despacho y produccion los registran sus procesos de origen.
 * Este BO permite consultar el historial y registrar ajustes por conteo fisico.
 */
public interface MovimientoInventarioBO {

    MovimientoInventario registrarAjuste(int idProducto, int idUsuario,
                                         double cantidadContada, String motivo) throws SQLException;

    MovimientoInventario modificarMotivo(int idMovimiento, String motivo) throws SQLException;

    MovimientoInventario obtener(int idMovimiento) throws SQLException;

    List<MovimientoInventario> listarTodos() throws SQLException;

    List<MovimientoInventario> listarPorProducto(int idProducto) throws SQLException;
}
