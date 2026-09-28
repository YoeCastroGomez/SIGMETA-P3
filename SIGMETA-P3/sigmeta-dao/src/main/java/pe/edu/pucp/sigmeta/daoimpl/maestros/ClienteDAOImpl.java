package pe.edu.pucp.sigmeta.daoimpl.maestros;

import pe.edu.pucp.sigmeta.dao.maestros.ClienteDAO;
import pe.edu.pucp.sigmeta.dbmanager.DBManager;
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
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_cliente_obtener(?)}");){
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
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_cliente_insertar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.registerOutParameter(1, Types.INTEGER);
            asignarParametros(cs, cliente);
            cs.executeUpdate();
            cliente.setId(cs.getInt(1));
        }
        cliente.setEstado(true);
        return cliente;
    }

    // RF003
    @Override
    public Cliente update(Cliente cliente) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_cliente_modificar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.setInt(1, cliente.getId());
            asignarParametros(cs, cliente);
            cs.setBoolean(13, cliente.isEstado());
            cs.executeUpdate();
        }
        return cliente;
    }

    // RF003, RNF002
    @Override
    public void remove(Cliente cliente) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_cliente_eliminar(?)}");){
            cs.setInt(1, cliente.getId());
            cs.executeUpdate();
        }
        cliente.setEstado(false);
    }

    // RF003
    @Override
    public List<Cliente> listarTodos() throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_cliente_listar()}");
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
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_cliente_buscar_por_documento(?)}");){
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
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_cliente_buscar_por_razon_social(?)}");){
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
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_cliente_listar_por_estado(?)}");){
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
        cs.setString(3, cliente.getDireccion());
        cs.setString(4, cliente.getTelefono());
        cs.setString(5, cliente.getCorreo());
        cs.setString(6, cliente.getTipoDocumento().name());
        cs.setString(7, cliente.getNumeroDocumento());
        cs.setString(8, cliente.getContactoNombre());
        cs.setString(9, cliente.getCondicionPago().name());
        cs.setInt(10, cliente.getPlazoCreditoDias());
        cs.setDouble(11, cliente.getLimiteCredito());
        cs.setString(12, cliente.getCalificacionCrediticia());
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setId(rs.getInt("id_cliente"));
        cliente.setRazonSocial(rs.getString("razon_social"));
        cliente.setDireccion(rs.getString("direccion"));
        cliente.setTelefono(rs.getString("telefono"));
        cliente.setCorreo(rs.getString("correo"));
        cliente.setEstado(rs.getBoolean("estado"));
        cliente.setTipoDocumento(TipoDocumentoIdentidad.valueOf(rs.getString("tipo_documento")));
        cliente.setNumeroDocumento(rs.getString("numero_documento"));
        cliente.setContactoNombre(rs.getString("contacto_nombre"));
        cliente.setCondicionPago(CondicionPago.valueOf(rs.getString("condicion_pago")));
        cliente.setPlazoCreditoDias(rs.getInt("plazo_credito_dias"));
        cliente.setLimiteCredito(rs.getDouble("limite_credito"));
        cliente.setCalificacionCrediticia(rs.getString("calificacion_crediticia"));
        return cliente;
    }
}
