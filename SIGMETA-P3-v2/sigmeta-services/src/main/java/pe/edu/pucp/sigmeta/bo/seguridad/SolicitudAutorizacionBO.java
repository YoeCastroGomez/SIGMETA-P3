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
    // RF014: una autorizacion de un solo uso queda USADA despues de usarse
    void consumir(int idSolicitud) throws SQLException;

    // RF014: guarda lo que el solicitante preparo en la pantalla (JSON) para retomar la operacion
    void guardarDatosOperacion(int idSolicitud, int idSolicitante, String datosOperacion) throws SQLException;
    // RF014: solicitudes que se pueden retomar (tienen datos y no se usaron). idSolicitante = 0: de todos
    // (solo el Administrador). operacion = null: de cualquier operacion
    List<SolicitudAutorizacion> listarEnCurso(int idSolicitante, String operacionRestringida) throws SQLException;
    // RF014: el solicitante ya no la va a retomar: si estaba pendiente o aprobada se anula, y deja de listarse
    void descartar(int idSolicitud, int idResponsable) throws SQLException;

    // RF014 (opcion B): la solicitud nace en la operacion restringida y lleva sus datos.
    // referencia = documento o producto; detalle = lo que revisa el Administrador; huella = SHA-256 de los datos.
    // Si ya hay una pendiente o aprobada de esa misma operacion, la devuelve en vez de crear otra.
    SolicitudAutorizacion solicitarOperacion(int idSolicitante, String operacionRestringida, String motivo,
                                             String referencia, String detalle, String huella) throws SQLException;
    // RF014: la autorizacion aprobada y vigente de ESA operacion (misma huella). Si no la hay, lanza el mensaje
    // que corresponde: en espera del Administrador, o que hay que solicitarla.
    SolicitudAutorizacion requerirAutorizacion(int idUsuario, String operacionRestringida, String huella) throws SQLException;
}
