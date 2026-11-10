package pe.edu.pucp.sigmeta.bo.caja;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;

// RF010
public interface CierreCajaBO {

    // el Cajero que abrio la caja la cierra; el monto calculado y la diferencia los obtiene el sistema
    CierreCaja registrarCierre(CierreCaja cierreCaja, int idResponsable) throws SQLException;

    // solo el Administrador corrige el monto declarado de una caja cerrada
    CierreCaja modificar(CierreCaja cierreCaja, int idResponsable) throws SQLException;

    // solo el Administrador; elimina el cierre y reabre la caja
    // RF010: solo el Administrador reabre la caja. El cierre no se borra: queda REVERTIDO con fecha,
    // responsable y motivo, y la caja vuelve a estar abierta (luego puede cerrarse con un cierre nuevo)
    CierreCaja reabrir(int idCierre, String motivo, int idResponsable) throws SQLException;

    CierreCaja obtenerPorId(int id) throws SQLException;

    // cierre vigente de la caja
    CierreCaja obtenerPorCaja(int idCaja) throws SQLException;

    // historial de cierres de la caja (vigente y revertidos)
    List<CierreCaja> listarPorCaja(int idCaja) throws SQLException;

    double calcularMontoEsperado(int idCaja) throws SQLException;

    List<CierreCaja> buscarPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException;

    List<CierreCaja> listarTodosConDetalle() throws SQLException;
}
