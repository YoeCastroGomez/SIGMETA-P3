package pe.edu.pucp.sigmeta.bo.caja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;

public interface MovimientoCajaBO {

    MovimientoCaja registrarMovimiento(MovimientoCaja movimiento) throws Exception;

    MovimientoCaja modificar(MovimientoCaja movimiento) throws Exception;

    void eliminar(MovimientoCaja movimiento) throws Exception;

    MovimientoCaja obtenerPorId(int id) throws Exception;

    List<MovimientoCaja> listarPorCaja(int idCaja) throws Exception;

    Map<MedioPago, Double> obtenerTotalesPorMedioPago(int idCaja) throws Exception;

    List<MovimientoCaja> buscarPorRangoFechas(int idCaja, LocalDateTime inicio, LocalDateTime fin) throws Exception;

    List<MovimientoCaja> listarTodosConDetalle() throws Exception;
}
