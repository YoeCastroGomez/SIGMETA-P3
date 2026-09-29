package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;

import java.sql.SQLException;
import java.util.List;

public interface ComprobanteDAO extends BaseDAO<Comprobante, Integer> {
    List<Comprobante> listarTodos() throws SQLException;
    List<Comprobante> listarPorVenta(int idVenta) throws SQLException;
    String siguienteCorrelativo(String serie) throws SQLException;
    List<Comprobante> listarNotasCreditoPorComprobante(int idComprobanteRelacionado) throws SQLException;
    void actualizarEstado(int id, EstadoComprobante estado) throws SQLException;
    void revertirSaldoCuentaPorCobrar(int idVenta, double montoReversion) throws SQLException;
}
