package pe.edu.pucp.sigmeta.bo.caja;

import java.time.LocalDateTime;
import java.util.List;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;

public interface CierreCajaBO {

    CierreCaja registrarCierre(CierreCaja cierreCaja) throws Exception;

    CierreCaja modificar(CierreCaja cierreCaja) throws Exception;

    void eliminar(CierreCaja cierreCaja) throws Exception;

    CierreCaja obtenerPorId(int id) throws Exception;

    CierreCaja obtenerPorCaja(int idCaja) throws Exception;

    double calcularMontoEsperado(int idCaja) throws Exception;

    List<CierreCaja> buscarPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Exception;

    List<CierreCaja> listarTodosConDetalle() throws Exception;
}
