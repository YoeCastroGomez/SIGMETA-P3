package pe.edu.pucp.sigmeta.bo.caja;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;

// RF010
public interface MovimientoCajaBO {

    // idResponsable: el Cajero que abrio la caja
    MovimientoCaja registrarMovimiento(MovimientoCaja movimiento, int idResponsable) throws SQLException;

    // solo ingresos y egresos manuales, con la caja abierta
    MovimientoCaja modificar(MovimientoCaja movimiento, int idResponsable) throws SQLException;

    void eliminar(MovimientoCaja movimiento, int idResponsable) throws SQLException;

    MovimientoCaja obtenerPorId(int id) throws SQLException;

    List<MovimientoCaja> listarPorCaja(int idCaja) throws SQLException;

    Map<MedioPago, Double> obtenerTotalesPorMedioPago(int idCaja) throws SQLException;

    List<MovimientoCaja> buscarPorRangoFechas(int idCaja, LocalDateTime inicio, LocalDateTime fin) throws SQLException;

    List<MovimientoCaja> listarTodosConDetalle() throws SQLException;
}
