package pe.edu.pucp.sigmeta.bo.almacen;

import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;

import java.sql.SQLException;
import java.util.List;

/**
 * Consulta de los detalles historicos de recepcion.
 * Su registro se realiza exclusivamente mediante RecepcionCompraBO.registrar,
 * junto con las actualizaciones de cantidades, stock y movimientos.
 */
public interface DetalleRecepcionCompraBO {

    DetalleRecepcionCompra obtener(int idDetalleRecepcion) throws SQLException;

    List<DetalleRecepcionCompra> listarTodos() throws SQLException;

    List<DetalleRecepcionCompra> listarPorRecepcion(int idRecepcion) throws SQLException;
}
