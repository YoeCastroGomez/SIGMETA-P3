package pe.edu.pucp.sigmeta.bo.cobranza;

import java.util.List;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;

public interface CuentaPorCobrarBO {

    CuentaPorCobrar insertar(CuentaPorCobrar cxc) throws Exception;

    CuentaPorCobrar modificar(CuentaPorCobrar cxc) throws Exception;

    void eliminar(CuentaPorCobrar cxc) throws Exception;

    CuentaPorCobrar obtenerPorId(int id) throws Exception;

    CuentaPorCobrar obtenerPorVenta(int idVenta) throws Exception;

    List<CuentaPorCobrar> listarPorCliente(int idCliente) throws Exception;

    List<CuentaPorCobrar> listarPorEstado(EstadoCuentaPorCobrar estado) throws Exception;

    List<CuentaPorCobrar> listarVencidas() throws Exception;

    List<CuentaPorCobrar> listarTodasConDetalle() throws Exception;
}
