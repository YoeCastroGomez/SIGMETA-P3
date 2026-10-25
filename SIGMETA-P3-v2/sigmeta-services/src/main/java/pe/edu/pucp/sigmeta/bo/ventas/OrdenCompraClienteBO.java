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
}
