package pe.edu.pucp.sigmeta.daoimpl.maestros;

import pe.edu.pucp.sigmeta.dao.maestros.ProveedorDAO;
import pe.edu.pucp.sigmeta.dbmanager.DBManager;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAOImpl implements ProveedorDAO {

    // RF004
    @Override
    public Proveedor load(Integer id) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_proveedor_obtener(?)}");){
            cs.setInt(1, id);
            try(ResultSet rs = cs.executeQuery();){
                if(rs.next()){
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // RF004
    @Override
    public Proveedor save(Proveedor proveedor) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_proveedor_insertar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.registerOutParameter(1, Types.INTEGER);
            asignarParametros(cs, proveedor);
            cs.executeUpdate();
            proveedor.setId(cs.getInt(1));
        }
        proveedor.setEstado(true);
        return proveedor;
    }

    // RF004
    @Override
    public Proveedor update(Proveedor proveedor) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_proveedor_modificar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.setInt(1, proveedor.getId());
            asignarParametros(cs, proveedor);
            cs.setBoolean(11, proveedor.isEstado());
            cs.executeUpdate();
        }
        return proveedor;
    }

    // RF004, RNF002
    @Override
    public void remove(Proveedor proveedor) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_proveedor_eliminar(?)}");){
            cs.setInt(1, proveedor.getId());
            cs.executeUpdate();
        }
        proveedor.setEstado(false);
    }

    // RF004
    @Override
    public List<Proveedor> listarTodos() throws SQLException {
        List<Proveedor> proveedores = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_proveedor_listar()}");
            ResultSet rs = cs.executeQuery();){
            while(rs.next()){
                proveedores.add(mapear(rs));
            }
        }
        return proveedores;
    }

    // RF004
    @Override
    public Proveedor buscarPorRuc(String ruc) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_proveedor_buscar_por_ruc(?)}");){
            cs.setString(1, ruc);
            try(ResultSet rs = cs.executeQuery();){
                if(rs.next()){
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // RF004
    @Override
    public List<Proveedor> buscarPorRazonSocial(String razonSocial) throws SQLException {
        List<Proveedor> proveedores = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_proveedor_buscar_por_razon_social(?)}");){
            cs.setString(1, razonSocial);
            try(ResultSet rs = cs.executeQuery();){
                while(rs.next()){
                    proveedores.add(mapear(rs));
                }
            }
        }
        return proveedores;
    }

    // RF004
    @Override
    public List<Proveedor> listarPorEstado(boolean estado) throws SQLException {
        List<Proveedor> proveedores = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_proveedor_listar_por_estado(?)}");){
            cs.setBoolean(1, estado);
            try(ResultSet rs = cs.executeQuery();){
                while(rs.next()){
                    proveedores.add(mapear(rs));
                }
            }
        }
        return proveedores;
    }

    // el parametro 1 es el id (OUT en insertar, IN en modificar)
    private void asignarParametros(CallableStatement cs, Proveedor proveedor) throws SQLException {
        cs.setString(2, proveedor.getRazonSocial());
        cs.setString(3, proveedor.getDireccion());
        cs.setString(4, proveedor.getTelefono());
        cs.setString(5, proveedor.getCorreo());
        cs.setString(6, proveedor.getRuc());
        cs.setString(7, proveedor.getRubro());
        cs.setString(8, proveedor.getContactoNombre());
        cs.setInt(9, proveedor.getPlazoEntregaDias());
        cs.setString(10, proveedor.getCondicionPago().name());
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor proveedor = new Proveedor();
        proveedor.setId(rs.getInt("id_proveedor"));
        proveedor.setRazonSocial(rs.getString("razon_social"));
        proveedor.setDireccion(rs.getString("direccion"));
        proveedor.setTelefono(rs.getString("telefono"));
        proveedor.setCorreo(rs.getString("correo"));
        proveedor.setEstado(rs.getBoolean("estado"));
        proveedor.setRuc(rs.getString("ruc"));
        proveedor.setRubro(rs.getString("rubro"));
        proveedor.setContactoNombre(rs.getString("contacto_nombre"));
        proveedor.setPlazoEntregaDias(rs.getInt("plazo_entrega_dias"));
        proveedor.setCondicionPago(CondicionPago.valueOf(rs.getString("condicion_pago")));
        return proveedor;
    }
}
