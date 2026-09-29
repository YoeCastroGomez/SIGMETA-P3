package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;

import java.sql.SQLException;
import java.util.List;

public interface DespachoDAO extends BaseDAO<Despacho, Integer> {
    List<Despacho> listarTodos() throws SQLException;
    List<Despacho> listarPorVenta(int idVenta) throws SQLException;
    String siguienteCorrelativo(String serie) throws SQLException;
}
