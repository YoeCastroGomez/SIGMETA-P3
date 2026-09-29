package pe.edu.pucp.sigmeta.dao.cobranza;

import java.sql.SQLException;
import java.util.List;
import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;

public interface CuentaPorCobrarDAO extends BaseDAO<CuentaPorCobrar, Integer> {

    CuentaPorCobrar obtenerPorVenta(int idVenta) throws SQLException;

    List<CuentaPorCobrar> listarPorCliente(int idCliente) throws SQLException;

    List<CuentaPorCobrar> listarPorEstado(EstadoCuentaPorCobrar estado) throws SQLException;

    List<CuentaPorCobrar> listarVencidas() throws SQLException;

    List<CuentaPorCobrar> listarTodasConDetalle() throws SQLException;
}