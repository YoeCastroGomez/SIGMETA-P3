package pe.edu.pucp.sigmeta.bo.ventas;

import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;

import java.sql.SQLException;
import java.util.List;

public interface OrdenCompraClienteBO {
    OrdenCompraCliente generarDesdeCotizacion(int idCotizacion, String numeroOrdenCliente, int idResponsable) throws SQLException;
    OrdenCompraCliente modificar(OrdenCompraCliente oc, int idResponsable) throws SQLException;
    void anular(int idOrden, String motivo, int idResponsable) throws SQLException;
    OrdenCompraCliente obtener(int id) throws SQLException;
    List<OrdenCompraCliente> listarTodos() throws SQLException;

    // RNF003: ids de las órdenes vigentes a las que les queda algo por VENDER, en una sola consulta
    java.util.Set<Integer> idsPorVender() throws SQLException;
    // H09: numero que tendra la proxima orden (OCC-AAAA-NNNN), sin reservarlo
    String proponerNumero(int idResponsable) throws SQLException;
}
