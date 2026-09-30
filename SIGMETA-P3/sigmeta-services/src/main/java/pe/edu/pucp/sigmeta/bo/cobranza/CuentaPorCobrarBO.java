package pe.edu.pucp.sigmeta.bo.cobranza;

import java.sql.SQLException;
import java.util.List;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
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
}
