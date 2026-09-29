package pe.edu.pucp.sigmeta.daoimpl.seguridad;

import pe.edu.pucp.sigmeta.dao.seguridad.UsuarioDAO;
import pe.edu.pucp.sigmeta.dbmanager.DBManager;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Rol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAOImpl implements UsuarioDAO {

    // RF002
    @Override
    public Usuario load(Integer id) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_usuario_obtener(?)}");){
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
    public Usuario save(Usuario usuario) throws SQLException {
        // la fecha de registro la asigna el procedimiento con NOW()
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_usuario_insertar(?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.registerOutParameter(1, Types.INTEGER);
            asignarParametros(cs, usuario);
            cs.executeUpdate();
            usuario.setId(cs.getInt(1));
        }
        usuario.setEstado(true);
        return usuario;
    }

    // RF002
    @Override
    public Usuario update(Usuario usuario) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_usuario_modificar(?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.setInt(1, usuario.getId());
            asignarParametros(cs, usuario);
            cs.setBoolean(9, usuario.isEstado());
            cs.executeUpdate();
        }
        return usuario;
    }

    // RF002, RNF002
    @Override
    public void remove(Usuario usuario) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_usuario_eliminar(?)}");){
            cs.setInt(1, usuario.getId());
            cs.executeUpdate();
        }
        usuario.setEstado(false);
    }

    // RF002
    @Override
    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_usuario_listar()}");
            ResultSet rs = cs.executeQuery();){
            while(rs.next()){
                usuarios.add(mapear(rs));
            }
        }
        return usuarios;
    }

    // RF001
    @Override
    public Usuario buscarPorNombreUsuario(String nombreUsuario) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_usuario_buscar_por_nombre_usuario(?)}");){
            cs.setString(1, nombreUsuario);
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
    public List<Usuario> listarPorEstado(boolean estado) throws SQLException {
        List<Usuario> usuarios = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_usuario_listar_por_estado(?)}");){
            cs.setBoolean(1, estado);
            try(ResultSet rs = cs.executeQuery();){
                while(rs.next()){
                    usuarios.add(mapear(rs));
                }
            }
        }
        return usuarios;
    }

    // el parametro 1 es el id (OUT en insertar, IN en modificar)
    private void asignarParametros(CallableStatement cs, Usuario usuario) throws SQLException {
        cs.setString(2, usuario.getNombreUsuario());
        cs.setString(3, usuario.getClaveHash());
        cs.setString(4, usuario.getSalt());
        cs.setString(5, usuario.getNombres());
        cs.setString(6, usuario.getApellidos());
        cs.setString(7, usuario.getCorreo());
        cs.setInt(8, usuario.getRol().getId());
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Rol rol = new Rol();
        rol.setId(rs.getInt("id_rol"));
        rol.setTipo(TipoRol.valueOf(rs.getString("tipo")));
        rol.setDescripcion(rs.getString("descripcion"));
        rol.setEstado(rs.getBoolean("estado_rol"));

        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id"));
        usuario.setNombreUsuario(rs.getString("nombre_usuario"));
        usuario.setClaveHash(rs.getString("clave_hash"));
        usuario.setSalt(rs.getString("salt"));
        usuario.setNombres(rs.getString("nombres"));
        usuario.setApellidos(rs.getString("apellidos"));
        usuario.setCorreo(rs.getString("correo"));
        usuario.setRol(rol);
        usuario.setEstado(rs.getBoolean("estado"));
        usuario.setFechaRegistro(rs.getTimestamp("fecha_registro").toLocalDateTime());
        return usuario;
    }
}
