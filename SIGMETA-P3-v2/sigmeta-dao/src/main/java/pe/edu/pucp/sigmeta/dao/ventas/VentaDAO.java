package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface VentaDAO extends BaseDAO<Venta, Integer> {
    List<Venta> listarTodos() throws SQLException;
    void bloquearVenta(int id) throws SQLException;
    void actualizarEstado(int id, EstadoVenta estado) throws SQLException;
    java.math.BigDecimal obtenerSaldoPendienteCliente(int idCliente) throws SQLException;
    void generarCuentaPorCobrar(int idVenta, int idCliente, LocalDate fechaEmision, LocalDate fechaVencimiento, double montoOriginal) throws SQLException;
    long siguienteCorrelativo(int anio) throws SQLException;
    double cantidadComprometida(int idLineaOrden, int idVentaExcluir) throws SQLException;
    List<Venta> listarPorCliente(int idCliente) throws SQLException;

}
