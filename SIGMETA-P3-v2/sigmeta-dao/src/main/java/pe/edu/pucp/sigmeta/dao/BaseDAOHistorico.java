package pe.edu.pucp.sigmeta.dao;

import java.sql.SQLException;

// Operaciones de los registros historicos (movimientos de inventario, recepciones, cierres de caja):
// se insertan, consultan y corrigen, pero nunca se eliminan (RNF002). Lo que se deshace se revierte
// con un registro compensatorio o con un cambio de estado.
public interface BaseDAOHistorico <T, ID> {
    T load (ID id) throws SQLException;
    T save (T t) throws SQLException;
    T update (T t) throws SQLException;
}
