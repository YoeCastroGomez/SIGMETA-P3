package pe.edu.pucp.sigmeta.dao.cobranza;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.cobranza.ResumenCuentasPorCobrar;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;

public interface CuentaPorCobrarDAO extends BaseDAO<CuentaPorCobrar, Integer> {

    CuentaPorCobrar obtenerPorVenta(int idVenta) throws SQLException;

    List<CuentaPorCobrar> listarPorCliente(int idCliente) throws SQLException;

    List<CuentaPorCobrar> listarPorEstado(EstadoCuentaPorCobrar estado) throws SQLException;

    List<CuentaPorCobrar> listarVencidas() throws SQLException;

    List<CuentaPorCobrar> listarTodasConDetalle() throws SQLException;

    // RF009, H10: busquedas filtradas en la base (antes se filtraba en el BO)
    List<CuentaPorCobrar> buscarPorComprobante(String texto) throws SQLException;

    List<CuentaPorCobrar> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin, boolean porVencimiento) throws SQLException;

    ResumenCuentasPorCobrar obtenerResumen() throws SQLException;

    // comprobante vigente (FACTURA o BOLETA) de cada venta; idsVenta null = de todas las cuentas
    Map<Integer, List<Comprobante>> listarComprobantesVigentes(Collection<Integer> idsVenta) throws SQLException;
}