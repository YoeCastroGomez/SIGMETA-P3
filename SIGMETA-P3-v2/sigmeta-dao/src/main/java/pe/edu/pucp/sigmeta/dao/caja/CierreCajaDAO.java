package pe.edu.pucp.sigmeta.dao.caja;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.pucp.sigmeta.dao.BaseDAOHistorico;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;

public interface CierreCajaDAO extends BaseDAOHistorico<CierreCaja, Integer> {
    // cierre VIGENTE de la caja (null si la caja esta abierta)
    CierreCaja obtenerPorCaja(int idCaja) throws SQLException;
    // historial de cierres de la caja: el vigente y los revertidos al reabrirla
    List<CierreCaja> listarPorCaja(int idCaja) throws SQLException;
    // RF010: marca el cierre como REVERTIDO (no lo borra) y vuelve a abrir la caja
    void reabrir(int idCierre, int idUsuarioReapertura, String motivoReapertura) throws SQLException;
    double calcularMontoEsperado(int idCaja) throws SQLException;
    List<CierreCaja> buscarPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException;
    List<CierreCaja> listarTodosConDetalle() throws SQLException;
}
