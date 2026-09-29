package pe.edu.pucp.sigmeta.bo.caja;

import java.util.List;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;

public interface CajaBO {

    Caja abrirCaja(Caja caja) throws Exception;

    Caja modificar(Caja caja) throws Exception;

    void eliminar(Caja caja) throws Exception;

    Caja obtenerPorId(int id) throws Exception;

    Caja obtenerCajaAbiertaPorUsuario(int idUsuario) throws Exception;

    List<Caja> listarTodasConUsuario() throws Exception;

    MovimientoCaja registrarMovimiento(MovimientoCaja movimiento) throws Exception;

    List<MovimientoCaja> listarMovimientosPorCaja(int idCaja) throws Exception;

    double calcularMontoCalculado(int idCaja) throws Exception;

    CierreCaja cerrarCaja(CierreCaja cierreCaja) throws Exception;

    CierreCaja obtenerCierrePorCaja(int idCaja) throws Exception;
}