package pe.edu.pucp.sigmeta.bo.cobranza;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.cobranza.ResumenCuentasPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;

// RF009: la cuenta se genera al registrar una venta al credito; aqui el Administrador la gestiona
public interface CuentaPorCobrarBO {

    CuentaPorCobrar insertar(CuentaPorCobrar cxc, int idResponsable) throws SQLException;

    // solo la fecha de vencimiento (refinanciamiento); los montos los mueven los cobros
    CuentaPorCobrar modificar(CuentaPorCobrar cxc, int idResponsable) throws SQLException;

    // solo una cuenta sin cobros
    void eliminar(CuentaPorCobrar cxc, int idResponsable) throws SQLException;

    CuentaPorCobrar obtenerPorId(int id) throws SQLException;

    CuentaPorCobrar obtenerPorVenta(int idVenta) throws SQLException;

    List<CuentaPorCobrar> listarPorCliente(int idCliente) throws SQLException;

    List<CuentaPorCobrar> listarPorEstado(EstadoCuentaPorCobrar estado) throws SQLException;

    List<CuentaPorCobrar> listarVencidas() throws SQLException;

    List<CuentaPorCobrar> listarTodasConDetalle() throws SQLException;

    // RF009: busqueda por comprobante de la venta (serie-numero, por ejemplo F001-00000123; acepta una parte)
    List<CuentaPorCobrar> buscarPorComprobante(String comprobante) throws SQLException;

    // RF009: cuentas por rango de fechas; porVencimiento = false usa la fecha de emision, true la de vencimiento
    List<CuentaPorCobrar> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin, boolean porVencimiento) throws SQLException;

    // RF009: total de saldos pendientes y de cuentas vencidas; solo el Administrador
    ResumenCuentasPorCobrar obtenerResumen(int idResponsable) throws SQLException;
}
