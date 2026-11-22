package pe.edu.pucp.sigmeta.bo.ventas;

import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.sql.SQLException;
import java.util.List;

public interface VentaBO {
    String proponerNumero(int idResponsable) throws SQLException;
    Venta registrar(Venta venta, int idResponsable) throws SQLException;
    Venta modificar(Venta venta, int idResponsable) throws SQLException;
    void anular(int idVenta, String motivo, int idResponsable) throws SQLException;
    Venta obtener(int id) throws SQLException;
    List<Venta> listarTodos() throws SQLException;
    List<Venta> listarPorCliente(int idCliente) throws SQLException;
    // RF008/RF010: ids de las ventas al contado ya cobradas en caja (esas ya no se modifican)
    java.util.Set<Integer> idsCobradasAlContado() throws SQLException;

    // RNF003, panel de inicio: productos con más importe en las últimas 'ventas' ventas vigentes (una consulta)
    java.util.List<java.util.Map<String, Object>> topProductos(int ventas, int limite) throws SQLException;

    // RF005/RF014: pide la autorizacion de ESTA venta con lineas bajo el costo (el Administrador ve sus datos); no registra nada
    pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarDescuentoEspecial(Venta venta, int idResponsable) throws SQLException;
}
