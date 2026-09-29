package pe.edu.pucp.sigmeta.dao.caja;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;

public interface CierreCajaDAO extends BaseDAO<CierreCaja, Integer> {
    CierreCaja obtenerPorCaja(int idCaja) throws SQLException;
    double calcularMontoEsperado(int idCaja) throws SQLException;
    List<CierreCaja> buscarPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException;
    List<CierreCaja> listarTodosConDetalle() throws SQLException;
}
