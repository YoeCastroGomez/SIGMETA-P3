package pe.edu.pucp.sigmeta.daoimpl.seguridad;

import pe.edu.pucp.sigmeta.dao.seguridad.RolDAO;
import pe.edu.pucp.sigmeta.dbmanager.DBManager;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Rol;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class RolDAOImpl implements RolDAO {

    // RF002
    @Override
    public Rol load(Integer id) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_rol_obtener(?)}");){
            cs.setInt(1, id);
            try(ResultSet rs = cs.executeQuery();){
                if(rs.next()){
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // RF002
    @Override
    public Rol save(Rol rol) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_rol_insertar(?, ?, ?)}");){
            cs.registerOutParameter(1, Types.INTEGER);
            asignarParametros(cs, rol);
            cs.executeUpdate();
            rol.setId(cs.getInt(1));
        }
        rol.setEstado(true);
        return rol;
    }

    // RF002
    @Override
    public Rol update(Rol rol) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_rol_modificar(?, ?, ?, ?)}");){
            cs.setInt(1, rol.getId());
            asignarParametros(cs, rol);
            cs.setBoolean(4, rol.isEstado());
            cs.executeUpdate();
        }
        return rol;
    }

    // RF002
    @Override
    public void remove(Rol rol) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_rol_eliminar(?)}");){
            cs.setInt(1, rol.getId());
            cs.executeUpdate();
        }
        rol.setEstado(false);
    }

    // RF002
    @Override
    public List<Rol> listarTodos() throws SQLException {
        List<Rol> roles = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_rol_listar()}");
            ResultSet rs = cs.executeQuery();){
            while(rs.next()){
                roles.add(mapear(rs));
            }
        }
        return roles;
    }

    // RF002
    @Override
    public Rol buscarPorTipo(TipoRol tipo) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_rol_buscar_por_tipo(?)}");){
            cs.setString(1, tipo.name());
            try(ResultSet rs = cs.executeQuery();){
                if(rs.next()){
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // el parametro 1 es el id (OUT en insertar, IN en modificar)
    private void asignarParametros(CallableStatement cs, Rol rol) throws SQLException {
        cs.setString(2, rol.getTipo().name());
        cs.setString(3, rol.getDescripcion());
    }

    private Rol mapear(ResultSet rs) throws SQLException {
        Rol rol = new Rol();
        rol.setId(rs.getInt("id_rol"));
        rol.setTipo(TipoRol.valueOf(rs.getString("tipo")));
        rol.setDescripcion(rs.getString("descripcion"));
        rol.setEstado(rs.getBoolean("estado"));
        return rol;
    }
}
