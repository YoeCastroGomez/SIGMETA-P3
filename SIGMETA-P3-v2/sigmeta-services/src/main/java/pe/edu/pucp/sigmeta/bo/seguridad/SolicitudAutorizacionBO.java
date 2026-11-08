package pe.edu.pucp.sigmeta.bo.seguridad;

import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;

import java.sql.SQLException;
import java.util.List;

// RF014
public interface SolicitudAutorizacionBO {
    SolicitudAutorizacion solicitar(int idSolicitante, String operacionRestringida, String motivo) throws SQLException;
    SolicitudAutorizacion aprobar(int idSolicitud, int idAdministrador, int vigenciaMinutos) throws SQLException;
    SolicitudAutorizacion rechazar(int idSolicitud, int idAdministrador) throws SQLException;
    void anular(int idSolicitud, int idResponsable) throws SQLException;
    SolicitudAutorizacion obtener(int idSolicitud) throws SQLException;
    List<SolicitudAutorizacion> listarTodos() throws SQLException;
    List<SolicitudAutorizacion> listarPendientes() throws SQLException;
    List<SolicitudAutorizacion> listarPorSolicitante(int idSolicitante) throws SQLException;
    boolean tieneAutorizacionVigente(int idUsuario, String operacionRestringida) throws SQLException;
    // RF013/RF014: la autorizacion aprobada y vigente, con el administrador que la aprobo (null si no hay)
    SolicitudAutorizacion obtenerAutorizacionVigente(int idUsuario, String operacionRestringida) throws SQLException;
    // RF014: una autorizacion de un solo uso queda VENCIDA despues de usarse
    void consumir(int idSolicitud) throws SQLException;
}
