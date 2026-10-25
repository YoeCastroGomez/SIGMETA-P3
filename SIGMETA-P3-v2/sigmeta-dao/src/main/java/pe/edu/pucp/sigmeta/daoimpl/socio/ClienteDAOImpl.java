package pe.edu.pucp.sigmeta.daoimpl.socio;

import pe.edu.pucp.sigmeta.dao.socio.ClienteDAO;
import pe.edu.pucp.sigmeta.transaction.transactionContext;
import pe.edu.pucp.sigmeta.model.comun.DatosContacto;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.TipoDocumentoIdentidad;
import pe.edu.pucp.sigmeta.model.socio.Cliente;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAOImpl implements ClienteDAO {

    // RF003
    @Override
    public Cliente load(Integer id) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_cliente_obtener(?)}");){
            cs.setInt(1, id);
            try(ResultSet rs = cs.executeQuery();){
                if(rs.next()){
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // RF003
    @Override
    public Cliente save(Cliente cliente) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_cliente_insertar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.registerOutParameter(1, Types.INTEGER);
            asignarParametros(cs, cliente);
            cs.setString(15, cliente.getTipoPersona().name());
            cs.executeUpdate();
            cliente.setId(cs.getInt(1));
        }
        cliente.setEstado(true);
        return cliente;
    }

    // RF003
    @Override
    public Cliente update(Cliente cliente) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_cliente_modificar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.setInt(1, cliente.getId());
            asignarParametros(cs, cliente);
            cs.setBoolean(15, cliente.isEstado());
            cs.setString(16, cliente.getTipoPersona().name());
            cs.executeUpdate();
        }
        return cliente;
    }

    // RF003, RNF002
    @Override
    public void remove(Cliente cliente) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_cliente_eliminar(?)}");){
            cs.setInt(1, cliente.getId());
            cs.executeUpdate();
        }
        cliente.setEstado(false);
    }

    // RF003
    @Override
    public List<Cliente> listarTodos() throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_cliente_listar()}");
            ResultSet rs = cs.executeQuery();){
            while(rs.next()){
                clientes.add(mapear(rs));
            }
        }
        return clientes;
    }

    // RF003
    @Override
    public Cliente buscarPorDocumento(String numeroDocumento) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_cliente_buscar_por_documento(?)}");){
            cs.setString(1, numeroDocumento);
            try(ResultSet rs = cs.executeQuery();){
                if(rs.next()){
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // RF003
    @Override
    public List<Cliente> buscarPorRazonSocial(String razonSocial) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_cliente_buscar_por_razon_social(?)}");){
            cs.setString(1, razonSocial);
            try(ResultSet rs = cs.executeQuery();){
                while(rs.next()){
                    clientes.add(mapear(rs));
                }
            }
        }
        return clientes;
    }

    // RF003
    @Override
    public List<Cliente> listarPorEstado(boolean estado) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_cliente_listar_por_estado(?)}");){
            cs.setBoolean(1, estado);
            try(ResultSet rs = cs.executeQuery();){
                while(rs.next()){
                    clientes.add(mapear(rs));
                }
            }
        }
        return clientes;
    }

    // el parametro 1 es el id (OUT en insertar, IN en modificar)
    private void asignarParametros(CallableStatement cs, Cliente cliente) throws SQLException {
        cs.setString(2, cliente.getRazonSocial());
        cs.setString(3, cliente.getNombres());
        cs.setString(4, cliente.getApellidos());
        cs.setString(5, cliente.getContacto().getDireccion());
        cs.setString(6, cliente.getContacto().getTelefono());
        cs.setString(7, cliente.getContacto().getCorreo());
        cs.setString(8, cliente.getTipoDocumento().name());
        cs.setString(9, cliente.getNumeroDocumento());
        cs.setString(10, cliente.getContacto().getNombreContacto());
        cs.setString(11, cliente.getCondicionPago().name());
        cs.setInt(12, cliente.getPlazoCreditoDias());
        cs.setBigDecimal(13, cliente.getLimiteCredito());
        cs.setString(14, cliente.getCalificacionCrediticia());
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setId(rs.getInt("id"));
        cliente.setTipoPersona(pe.edu.pucp.sigmeta.model.enums.TipoPersona.valueOf(rs.getString("tipo_persona")));
        cliente.setRazonSocial(rs.getString("razon_social"));
        cliente.setNombres(rs.getString("nombres"));
        cliente.setApellidos(rs.getString("apellidos"));
        cliente.setContacto(new DatosContacto(rs.getString("direccion"), rs.getString("telefono"), rs.getString("correo")));
        cliente.setEstado(rs.getBoolean("estado"));
        cliente.setTipoDocumento(TipoDocumentoIdentidad.valueOf(rs.getString("tipo_documento")));
        cliente.setNumeroDocumento(rs.getString("numero_documento"));
        cliente.getContacto().setNombreContacto(rs.getString("contacto_nombre"));
        cliente.setCondicionPago(CondicionPago.valueOf(rs.getString("condicion_pago")));
        cliente.setPlazoCreditoDias(rs.getInt("plazo_credito_dias"));
        cliente.setLimiteCredito(rs.getBigDecimal("limite_credito"));
        cliente.setCalificacionCrediticia(rs.getString("calificacion_crediticia"));
        return cliente;
    }
}
