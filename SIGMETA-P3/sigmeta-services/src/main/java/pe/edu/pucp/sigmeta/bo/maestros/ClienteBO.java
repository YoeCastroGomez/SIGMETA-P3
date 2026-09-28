package pe.edu.pucp.sigmeta.bo.maestros;

import pe.edu.pucp.sigmeta.model.socio.Cliente;

import java.sql.SQLException;
import java.util.List;

// RF003
public interface ClienteBO {
    // idResponsable: usuario en sesion; debe ser Vendedor o Administrador
    Cliente registrar(Cliente cliente, int idResponsable) throws SQLException;
    Cliente modificar(Cliente cliente, int idResponsable) throws SQLException;
    void desactivar(int idCliente, int idResponsable) throws SQLException;
    Cliente obtener(int idCliente) throws SQLException;
    List<Cliente> listarTodos() throws SQLException;
    Cliente buscarPorDocumento(String numeroDocumento) throws SQLException;
    List<Cliente> buscarPorRazonSocial(String razonSocial) throws SQLException;
    List<Cliente> listarPorEstado(boolean estado) throws SQLException;

    // solo el Administrador asigna o retira la linea de credito
    Cliente asignarLineaCredito(int idCliente, double limiteCredito, int plazoCreditoDias,
                                String calificacionCrediticia, int idAdministrador) throws SQLException;
    Cliente retirarLineaCredito(int idCliente, int idAdministrador) throws SQLException;
}
