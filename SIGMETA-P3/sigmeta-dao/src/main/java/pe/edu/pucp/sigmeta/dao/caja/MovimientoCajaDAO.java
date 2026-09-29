package pe.edu.pucp.sigmeta.dao.caja;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;

public interface MovimientoCajaDAO extends BaseDAO<MovimientoCaja, Integer> {
    List<MovimientoCaja> listarPorCaja(int idCaja) throws SQLException;
    Map<MedioPago, Double> obtenerTotalesPorMedioPago(int idCaja) throws SQLException;
    List<MovimientoCaja> buscarPorRangoFechas(int idCaja, LocalDateTime inicio, LocalDateTime fin) throws SQLException;
    List<MovimientoCaja> listarTodosConDetalle() throws SQLException;
}