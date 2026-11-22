package pe.edu.pucp.sigmeta.daoimpl.seguridad;

import pe.edu.pucp.sigmeta.dao.seguridad.SolicitudAutorizacionDAO;
import pe.edu.pucp.sigmeta.transaction.transactionContext;
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
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_obtener(?)}");){
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
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_insertar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.registerOutParameter(1, Types.INTEGER);
            asignarParametros(cs, solicitud);
            cs.executeUpdate();
            solicitud.setId(cs.getInt(1));
        }
        // RF014: datos de la operacion que se pide autorizar (las solicitudes antiguas no los tienen)
        if (solicitud.getHuella() != null) {
            try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_datos_operacion(?, ?, ?, ?)}");){
                cs.setInt(1, solicitud.getId());
                cs.setString(2, solicitud.getReferencia());
                cs.setString(3, solicitud.getDetalle());
                cs.setString(4, solicitud.getHuella());
                cs.executeUpdate();
            }
        }
        return solicitud;
    }

    // RF014
    @Override
    public SolicitudAutorizacion update(SolicitudAutorizacion solicitud) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_modificar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");){
            cs.setInt(1, solicitud.getId());
            asignarParametros(cs, solicitud);
            cs.executeUpdate();
        }
        return solicitud;
    }

    // RF014, RNF002
    @Override
    public void remove(SolicitudAutorizacion solicitud) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_eliminar(?)}");){
            cs.setInt(1, solicitud.getId());
            cs.executeUpdate();
        }
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarTodos() throws SQLException {
        List<SolicitudAutorizacion> solicitudes = new ArrayList<>();
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_listar()}");
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
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_listar_por_estado(?)}");){
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
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_listar_por_solicitante(?)}");){
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
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_buscar_vigente(?, ?)}");){
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

    // RF014
    @Override
    public SolicitudAutorizacion buscarPorHuella(int idSolicitante, String operacionRestringida, String huella,
                                                 EstadoAutorizacion estado) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_buscar_por_huella(?, ?, ?, ?)}");){
            cs.setInt(1, idSolicitante);
            cs.setString(2, operacionRestringida);
            cs.setString(3, huella);
            cs.setString(4, estado.name());
            try(ResultSet rs = cs.executeQuery();){
                if(rs.next()){
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // RF014: la autorizacion se consumio en su operacion (sp_solicitud_autorizacion_usar)
    @Override
    public void marcarUsada(int idSolicitud) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_usar(?)}");){
            cs.setInt(1, idSolicitud);
            cs.executeUpdate();
        }
    }

    // RF014: datos de la pantalla para retomar la operacion (sp_solicitud_autorizacion_guardar_datos)
    @Override
    public void guardarDatosOperacion(int idSolicitud, String datosOperacion) throws SQLException {
        Connection connection = transactionContext.getConnection();
        try(CallableStatement cs = connection.prepareCall("{call sp_solicitud_autorizacion_guardar_datos(?, ?)}");){
            cs.setInt(1, idSolicitud);
            if (datosOperacion == null) {
                cs.setNull(2, Types.LONGVARCHAR);
            } else {
                cs.setString(2, datosOperacion);
            }
            cs.executeUpdate();
        }
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
        // RF014: columnas de la migracion 11; si la base todavia no la tiene, quedan en null
        solicitud.setReferencia(columnaOpcional(rs, "referencia"));
        solicitud.setDetalle(columnaOpcional(rs, "detalle"));
        solicitud.setHuella(columnaOpcional(rs, "huella"));
        // RF014: columna de 04_SP_sebastian.sql (retomar la operacion); null si la base aun no la tiene
        solicitud.setDatosOperacion(columnaOpcional(rs, "datos_operacion"));
        return solicitud;
    }

    private String columnaOpcional(ResultSet rs, String columna) {
        try {
            return rs.getString(columna);
        } catch (SQLException e) {
            return null;
        }
    }

    private Timestamp aTimestamp(LocalDateTime fecha) {
        return fecha == null ? null : Timestamp.valueOf(fecha);
    }

    private LocalDateTime aLocalDateTime(Timestamp fecha) {
        return fecha == null ? null : fecha.toLocalDateTime();
    }
}
