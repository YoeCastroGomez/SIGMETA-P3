package pe.edu.pucp.sigmeta.boimpl.seguridad;

import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.seguridad.SolicitudAutorizacionDAO;
import pe.edu.pucp.sigmeta.daoimpl.seguridad.SolicitudAutorizacionDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class SolicitudAutorizacionBOImpl implements SolicitudAutorizacionBO {

    private final SolicitudAutorizacionDAO solicitudDAO;
    private final UsuarioBO usuarioBO;

    public SolicitudAutorizacionBOImpl() {
        this.solicitudDAO = new SolicitudAutorizacionDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF014
    @Override
    public SolicitudAutorizacion solicitar(int idSolicitante, String operacionRestringida, String motivo) throws SQLException {
        Usuario solicitante = usuarioBO.obtener(idSolicitante);
        if(solicitante == null || !solicitante.isEstado()){
            throw new IllegalArgumentException("El solicitante no existe o esta desactivado");
        }
        // RF014: solicitan el Vendedor, el Cajero y el Almacenero
        if(solicitante.getRol().getTipo() == TipoRol.ADMINISTRADOR){
            throw new IllegalStateException("El Administrador no necesita solicitar autorizacion");
        }
        String operacion = Validador.textoObligatorio(operacionRestringida, "operacion restringida", 60);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo", 200);

        boolean yaHayPendiente = solicitudDAO.listarPorSolicitante(idSolicitante).stream()
                .anyMatch(s -> s.getEstado() == EstadoAutorizacion.PENDIENTE
                        && s.getOperacionRestringida().equals(operacion));
        if(yaHayPendiente){
            throw new IllegalStateException("Ya existe una solicitud pendiente para esta operacion");
        }

        SolicitudAutorizacion solicitud = new SolicitudAutorizacion();
        solicitud.setSolicitante(solicitante);
        solicitud.setOperacionRestringida(operacion);
        solicitud.setMotivo(motivoValidado);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setEstado(EstadoAutorizacion.PENDIENTE);
        solicitud.setVigenciaMinutos(0);
        return solicitudDAO.save(solicitud);
    }

    // RF014
    @Override
    public SolicitudAutorizacion aprobar(int idSolicitud, int idAdministrador, int vigenciaMinutos) throws SQLException {
        if(vigenciaMinutos <= 0){
            throw new IllegalArgumentException("La vigencia debe ser mayor a 0 minutos");
        }
        Usuario administrador = usuarioBO.obtenerAdministradorActivo(idAdministrador);
        SolicitudAutorizacion solicitud = obtenerPendiente(idSolicitud);
        LocalDateTime ahora = LocalDateTime.now();
        solicitud.setAdministrador(administrador);
        solicitud.setEstado(EstadoAutorizacion.APROBADA);
        solicitud.setFechaResolucion(ahora);
        solicitud.setVigenciaMinutos(vigenciaMinutos);
        solicitud.setFechaVencimiento(ahora.plusMinutes(vigenciaMinutos));
        return solicitudDAO.update(solicitud);
    }

    // RF014
    @Override
    public SolicitudAutorizacion rechazar(int idSolicitud, int idAdministrador) throws SQLException {
        Usuario administrador = usuarioBO.obtenerAdministradorActivo(idAdministrador);
        SolicitudAutorizacion solicitud = obtenerPendiente(idSolicitud);
        solicitud.setAdministrador(administrador);
        solicitud.setEstado(EstadoAutorizacion.RECHAZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        return solicitudDAO.update(solicitud);
    }

    // una pendiente queda rechazada y una aprobada queda vencida (baja logica en sp_solicitud_autorizacion_eliminar)
    // RF014, RNF002
    @Override
    public void anular(int idSolicitud, int idResponsable) throws SQLException {
        SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
        // la anula quien la pidio (ya no la necesita) o un Administrador (revoca el permiso)
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.values());
        if(responsable.getId() != solicitud.getSolicitante().getId()
                && responsable.getRol().getTipo() != TipoRol.ADMINISTRADOR){
            throw new IllegalStateException("Solo el solicitante o un Administrador pueden anular la solicitud");
        }
        if(solicitud.getEstado() != EstadoAutorizacion.PENDIENTE && solicitud.getEstado() != EstadoAutorizacion.APROBADA){
            throw new IllegalStateException("Solo se puede anular una solicitud pendiente o aprobada");
        }
        solicitudDAO.remove(solicitud);
    }

    // RF014
    @Override
    public SolicitudAutorizacion obtener(int idSolicitud) throws SQLException {
        return solicitudDAO.load(idSolicitud);
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarTodos() throws SQLException {
        return solicitudDAO.listarTodos();
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarPendientes() throws SQLException {
        return solicitudDAO.listarPorEstado(EstadoAutorizacion.PENDIENTE);
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarPorSolicitante(int idSolicitante) throws SQLException {
        return solicitudDAO.listarPorSolicitante(idSolicitante);
    }

    // RF014
    @Override
    public boolean tieneAutorizacionVigente(int idUsuario, String operacionRestringida) throws SQLException {
        String operacion = Validador.textoObligatorio(operacionRestringida, "operacion restringida", 60);
        return solicitudDAO.buscarVigente(idUsuario, operacion) != null;
    }

    private SolicitudAutorizacion obtenerExistente(int idSolicitud) throws SQLException {
        SolicitudAutorizacion solicitud = solicitudDAO.load(idSolicitud);
        if(solicitud == null){
            throw new IllegalArgumentException("No existe la solicitud con id " + idSolicitud);
        }
        return solicitud;
    }

    private SolicitudAutorizacion obtenerPendiente(int idSolicitud) throws SQLException {
        SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
        if(solicitud.getEstado() != EstadoAutorizacion.PENDIENTE){
            throw new IllegalStateException("La solicitud ya fue resuelta (" + solicitud.getEstado() + ")");
        }
        return solicitud;
    }
}
