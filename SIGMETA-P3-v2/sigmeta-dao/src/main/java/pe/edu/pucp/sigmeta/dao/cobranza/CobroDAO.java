package pe.edu.pucp.sigmeta.dao.cobranza;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.cobranza.Cobro;

public interface CobroDAO extends BaseDAO<Cobro, Integer> {

    List<Cobro> listarTodos() throws SQLException;

    List<Cobro> listarPorCuentaPorCobrar(int idCuentaPorCobrar) throws SQLException;

    List<Cobro> listarPorCliente(int idCliente) throws SQLException;

    // RF009, H10: cobros con fecha de cobro entre dos fechas (incluidas), filtrados en la base
    List<Cobro> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) throws SQLException;
}