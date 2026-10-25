
package pe.edu.pucp.sigmeta.dao.compras;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface CompraDAO extends BaseDAO<Compra, Integer> {

    List<Compra> listAll() throws SQLException;

    void bloquearCompra(int idCompra) throws SQLException;
    void actualizarEstadoPorReversion(int idCompra, EstadoCompra estado) throws SQLException;

    List<Compra> listarPorProveedorYFechas(int idProveedor, LocalDate fechaInicio, LocalDate fechaFin) throws SQLException;

    void actualizarEstado(int idCompra, EstadoCompra estado)
            throws SQLException;

}
