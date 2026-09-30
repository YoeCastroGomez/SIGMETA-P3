package pe.edu.pucp.sigmeta.bo.cobranza;

import java.sql.SQLException;
import java.util.List;
import pe.edu.pucp.sigmeta.model.cobranza.Cobro;

// RF009
public interface CobroBO {

    // el Cajero cobra con su caja abierta: actualiza el saldo de la cuenta y registra el ingreso en caja
    Cobro registrarCobro(Cobro cobro, int idResponsable) throws SQLException;

    // solo fecha y referencia; para cambiar monto o medio de pago se anula y se registra otro cobro
    Cobro modificar(Cobro cobro, int idResponsable) throws SQLException;

    // anula el cobro: devuelve el saldo a la cuenta y retira el ingreso de la caja (si sigue abierta)
    void eliminar(Cobro cobro, int idResponsable) throws SQLException;

    Cobro obtenerPorId(int id) throws SQLException;

    List<Cobro> listarTodos() throws SQLException;

    List<Cobro> listarPorCuentaPorCobrar(int idCuentaPorCobrar) throws SQLException;

    List<Cobro> listarPorCliente(int idCliente) throws SQLException;
}
