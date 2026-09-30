package pe.edu.pucp.sigmeta.bo.caja;

import java.sql.SQLException;
import java.util.List;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;

// RF010
public interface CajaBO {

    // idResponsable: usuario en sesion. Abre la caja el Cajero, con un monto inicial
    Caja abrirCaja(Caja caja, int idResponsable) throws SQLException;

    // corrige el monto inicial; con la caja cerrada solo el Administrador
    Caja modificar(Caja caja, int idResponsable) throws SQLException;

    // solo una apertura sin movimientos ni cierre
    void eliminar(Caja caja, int idResponsable) throws SQLException;

    Caja obtenerPorId(int id) throws SQLException;

    Caja obtenerCajaAbiertaPorUsuario(int idUsuario) throws SQLException;

    List<Caja> listarTodasConUsuario() throws SQLException;

    MovimientoCaja registrarMovimiento(MovimientoCaja movimiento, int idResponsable) throws SQLException;

    List<MovimientoCaja> listarMovimientosPorCaja(int idCaja) throws SQLException;

    double calcularMontoCalculado(int idCaja) throws SQLException;

    CierreCaja cerrarCaja(CierreCaja cierreCaja, int idResponsable) throws SQLException;

    CierreCaja obtenerCierrePorCaja(int idCaja) throws SQLException;
}
