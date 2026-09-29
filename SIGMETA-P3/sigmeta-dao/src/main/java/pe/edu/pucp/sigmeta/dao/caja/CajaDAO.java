package pe.edu.pucp.sigmeta.dao.caja;

import java.sql.SQLException;
import java.util.List;
import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.caja.Caja;

public interface CajaDAO extends BaseDAO<Caja, Integer> {
    Caja obtenerCajaAbiertaPorUsuario(int idUsuario) throws SQLException;
    List<Caja> listarTodasConUsuario() throws SQLException;
}