package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface VentaDAO extends BaseDAO<Venta, Integer> {
    List<Venta> listarTodos() throws SQLException;
    void bloquearVenta(int id) throws SQLException;
    void actualizarEstado(int id, EstadoVenta estado) throws SQLException;
    double obtenerSaldoPendienteCliente(int idCliente) throws SQLException;
    void generarCuentaPorCobrar(int idVenta, int idCliente, LocalDate fechaEmision, LocalDate fechaVencimiento, Moneda moneda, double montoOriginal) throws SQLException;
}
