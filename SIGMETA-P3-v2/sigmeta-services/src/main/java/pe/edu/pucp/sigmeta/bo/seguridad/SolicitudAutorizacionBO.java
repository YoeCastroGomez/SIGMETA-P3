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

    // RF014 (opcion B): la solicitud nace en la operacion restringida y lleva sus datos.
    // referencia = documento o producto; detalle = lo que revisa el Administrador; huella = SHA-256 de los datos.
    // Si ya hay una pendiente o aprobada de esa misma operacion, la devuelve en vez de crear otra.
    SolicitudAutorizacion solicitarOperacion(int idSolicitante, String operacionRestringida, String motivo,
                                             String referencia, String detalle, String huella) throws SQLException;
    // RF014: la autorizacion aprobada y vigente de ESA operacion (misma huella). Si no la hay, lanza el mensaje
    // que corresponde: en espera del Administrador, o que hay que solicitarla.
    SolicitudAutorizacion requerirAutorizacion(int idUsuario, String operacionRestringida, String huella) throws SQLException;
}
