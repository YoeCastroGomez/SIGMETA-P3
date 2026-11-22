package pe.edu.pucp.sigmeta.dao.seguridad;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;

import java.sql.SQLException;
import java.util.List;

public interface SolicitudAutorizacionDAO extends BaseDAO<SolicitudAutorizacion, Integer> {
    List<SolicitudAutorizacion> listarTodos() throws SQLException;
    List<SolicitudAutorizacion> listarPorEstado(EstadoAutorizacion estado) throws SQLException;
    List<SolicitudAutorizacion> listarPorSolicitante(int idSolicitante) throws SQLException;
    SolicitudAutorizacion buscarVigente(int idSolicitante, String operacionRestringida) throws SQLException;
    // RF014: la solicitud de una operacion concreta (su huella) en un estado; APROBADA solo si no vencio
    SolicitudAutorizacion buscarPorHuella(int idSolicitante, String operacionRestringida, String huella,
                                          EstadoAutorizacion estado) throws SQLException;
    // RF014: APROBADA -> USADA (se consumio en su operacion)
    void marcarUsada(int idSolicitud) throws SQLException;
    // RF014: datos de la pantalla para retomar la operacion (null = descartada)
    void guardarDatosOperacion(int idSolicitud, String datosOperacion) throws SQLException;
}
