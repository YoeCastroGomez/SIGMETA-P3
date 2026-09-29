package pe.edu.pucp.sigmeta.bo.ventas;

import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.sql.SQLException;
import java.util.List;

public interface VentaBO {
    Venta registrar(Venta venta, int idResponsable) throws SQLException;
    Venta modificar(Venta venta, int idResponsable) throws SQLException;
    void anular(int idVenta, String motivo, int idResponsable) throws SQLException;
    Venta obtener(int id) throws SQLException;
    List<Venta> listarTodos() throws SQLException;
}
