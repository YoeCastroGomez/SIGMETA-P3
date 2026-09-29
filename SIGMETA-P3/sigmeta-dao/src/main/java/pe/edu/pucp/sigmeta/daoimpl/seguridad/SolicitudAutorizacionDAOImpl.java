package pe.edu.pucp.sigmeta.daoimpl.seguridad;

import pe.edu.pucp.sigmeta.dao.seguridad.SolicitudAutorizacionDAO;
import pe.edu.pucp.sigmeta.dbmanager.DBManager;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SolicitudAutorizacionDAOImpl implements SolicitudAutorizacionDAO {

    // RF014
    @Override
    public SolicitudAutorizacion load(Integer id) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_obtener(?)}");){
            cs.setInt(1, id);
            try(ResultSet rs = cs.executeQuery();){
                if(rs.next()){
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // RF014
    @Override
    public SolicitudAutorizacion save(SolicitudAutorizacion solicitud) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_insertar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.registerOutParameter(1, Types.INTEGER);
            asignarParametros(cs, solicitud);
            cs.executeUpdate();
            solicitud.setId(cs.getInt(1));
        }
        return solicitud;
    }

    // RF014
    @Override
    public SolicitudAutorizacion update(SolicitudAutorizacion solicitud) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_modificar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.setInt(1, solicitud.getId());
            asignarParametros(cs, solicitud);
            cs.executeUpdate();
        }
        return solicitud;
    }

    // RF014, RNF002
    @Override
    public void remove(SolicitudAutorizacion solicitud) throws SQLException {
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_eliminar(?)}");){
            cs.setInt(1, solicitud.getId());
            cs.executeUpdate();
        }
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarTodos() throws SQLException {
        List<SolicitudAutorizacion> solicitudes = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_listar()}");
            ResultSet rs = cs.executeQuery();){
            while(rs.next()){
                solicitudes.add(mapear(rs));
            }
        }
        return solicitudes;
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarPorEstado(EstadoAutorizacion estado) throws SQLException {
        List<SolicitudAutorizacion> solicitudes = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_listar_por_estado(?)}");){
            cs.setString(1, estado.name());
            try(ResultSet rs = cs.executeQuery();){
                while(rs.next()){
                    solicitudes.add(mapear(rs));
                }
            }
        }
        return solicitudes;
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarPorSolicitante(int idSolicitante) throws SQLException {
        List<SolicitudAutorizacion> solicitudes = new ArrayList<>();
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_listar_por_solicitante(?)}");){
            cs.setInt(1, idSolicitante);
            try(ResultSet rs = cs.executeQuery();){
                while(rs.next()){
                    solicitudes.add(mapear(rs));
                }
            }
        }
        return solicitudes;
    }

    // RF014
    @Override
    public SolicitudAutorizacion buscarVigente(int idSolicitante, String operacionRestringida) throws SQLException {
        // devuelve null si el usuario no tiene una autorizacion aprobada y no vencida
        try(Connection connection = DBManager.getInstance().getConnection();
            CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_buscar_vigente(?, ?)}");){
            cs.setInt(1, idSolicitante);
            cs.setString(2, operacionRestringida);
            try(ResultSet rs = cs.executeQuery();){
                if(rs.next()){
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // el parametro 1 es el id (OUT en insertar, IN en modificar)
    private void asignarParametros(CallableStatement cs, SolicitudAutorizacion solicitud) throws SQLException {
        cs.setInt(2, solicitud.getSolicitante().getId());
        // el administrador es null mientras la solicitud esta pendiente
        if(solicitud.getAdministrador() != null){
            cs.setInt(3, solicitud.getAdministrador().getId());
        } else {
            cs.setNull(3, Types.INTEGER);
        }
        cs.setString(4, solicitud.getOperacionRestringida());
        cs.setString(5, solicitud.getMotivo());
        cs.setTimestamp(6, Timestamp.valueOf(solicitud.getFechaSolicitud()));
        cs.setString(7, solicitud.getEstado().name());
        cs.setTimestamp(8, aTimestamp(solicitud.getFechaResolucion()));
        cs.setInt(9, solicitud.getVigenciaMinutos());
        cs.setTimestamp(10, aTimestamp(solicitud.getFechaVencimiento()));
    }

    private SolicitudAutorizacion mapear(ResultSet rs) throws SQLException {
        // solo se cargan los ids de los usuarios, el resto se obtiene con UsuarioDAO
        Usuario solicitante = new Usuario();
        solicitante.setId(rs.getInt("id_solicitante"));

        Usuario administrador = null;
        int idAdministrador = rs.getInt("id_administrador");
        if(!rs.wasNull()){
            administrador = new Usuario();
            administrador.setId(idAdministrador);
        }

        SolicitudAutorizacion solicitud = new SolicitudAutorizacion();
        solicitud.setId(rs.getInt("id"));
        solicitud.setSolicitante(solicitante);
        solicitud.setAdministrador(administrador);
        solicitud.setOperacionRestringida(rs.getString("operacion_restringida"));
        solicitud.setMotivo(rs.getString("motivo"));
        solicitud.setFechaSolicitud(rs.getTimestamp("fecha_solicitud").toLocalDateTime());
        solicitud.setEstado(EstadoAutorizacion.valueOf(rs.getString("estado")));
        solicitud.setFechaResolucion(aLocalDateTime(rs.getTimestamp("fecha_resolucion")));
        solicitud.setVigenciaMinutos(rs.getInt("vigencia_minutos"));
        solicitud.setFechaVencimiento(aLocalDateTime(rs.getTimestamp("fecha_vencimiento")));
        return solicitud;
    }

    private Timestamp aTimestamp(LocalDateTime fecha) {
        return fecha == null ? null : Timestamp.valueOf(fecha);
    }

    private LocalDateTime aLocalDateTime(Timestamp fecha) {
        return fecha == null ? null : fecha.toLocalDateTime();
    }
}
