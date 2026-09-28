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
}
