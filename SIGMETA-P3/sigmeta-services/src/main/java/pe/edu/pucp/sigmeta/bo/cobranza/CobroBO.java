package pe.edu.pucp.sigmeta.bo.cobranza;

import java.util.List;
import pe.edu.pucp.sigmeta.model.cobranza.Cobro;

public interface CobroBO {

    Cobro registrarCobro(Cobro cobro) throws Exception;

    Cobro modificar(Cobro cobro) throws Exception;

    void eliminar(Cobro cobro) throws Exception;

    Cobro obtenerPorId(int id) throws Exception;

    List<Cobro> listarPorCuentaPorCobrar(int idCuentaPorCobrar) throws Exception;

    List<Cobro> listarPorCliente(int idCliente) throws Exception;
}