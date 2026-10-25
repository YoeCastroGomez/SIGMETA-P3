package pe.edu.pucp.sigmeta.dao;

import java.sql.SQLException;

// CRUD completo: agrega la eliminacion (baja logica o anulacion) a las operaciones de BaseDAOHistorico
public interface BaseDAO <T, ID> extends BaseDAOHistorico<T, ID> {
    void remove (T t) throws SQLException;
}
