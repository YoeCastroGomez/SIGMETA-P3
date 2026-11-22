package pe.edu.pucp.sigmeta.boimpl.seguridad;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.seguridad.SolicitudAutorizacionDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import java.sql.SQLException;

// RF013/RF014: autorizacion vigente de un usuario para una operacion y su consumo (un solo uso).
// Parte de SolicitudAutorizacionBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class AutorizacionVigencia {

    private final SolicitudAutorizacionDAO solicitudDAO;
    private final UsuarioBO usuarioBO;

    AutorizacionVigencia(SolicitudAutorizacionDAO solicitudDAO, UsuarioBO usuarioBO) {
        this.solicitudDAO = solicitudDAO;
        this.usuarioBO = usuarioBO;
    }

    // RF014
    public boolean tieneAutorizacionVigente(int idUsuario, String operacionRestringida) throws SQLException {
        return Transaccion.ejecutar(() -> {
            String operacion = Validador.textoObligatorio(operacionRestringida, "operación restringida", 60);
            return solicitudDAO.buscarVigente(idUsuario, operacion) != null;
        });
    }

    // RF013/RF014: la autorizacion se toma de la solicitud que el Administrador aprobo en su propia sesion,
    // asi la operacion se ejecuta en la sesion del solicitante sin que el Administrador este conectado
    public SolicitudAutorizacion obtenerAutorizacionVigente(int idUsuario, String operacionRestringida) throws SQLException {
        return Transaccion.ejecutar(() -> {
            String operacion = Validador.textoObligatorio(operacionRestringida, "operación restringida", 60);
            SolicitudAutorizacion vigente = solicitudDAO.buscarVigente(idUsuario, operacion);
            if(vigente == null || vigente.getAdministrador() == null){
                return null;
            }
            Usuario administrador = usuarioBO.obtener(vigente.getAdministrador().getId());
            if(administrador == null || !administrador.isEstado() || !administrador.getRol().isEstado()
                    || administrador.getRol().getTipo() != TipoRol.ADMINISTRADOR){
                // el administrador que aprobo ya no esta activo: la autorizacion no sirve
                return null;
            }
            vigente.setAdministrador(administrador);
            vigente.setSolicitante(usuarioBO.obtener(idUsuario));
            return vigente;
        });
    }

    // RF014: APROBADA -> USADA (sp_solicitud_autorizacion_usar). Antes quedaba VENCIDA y no se distinguia de una
    // que paso su vigencia o que se revoco. Corre dentro de la transaccion de quien la usa: si esa operacion
    // falla hay rollback y la autorizacion sigue APROBADA (no hay que volver a pedirla)
    public void consumir(int idSolicitud) throws SQLException {
        Transaccion.ejecutar(() -> {
            SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
            if(solicitud.getEstado() != EstadoAutorizacion.APROBADA){
                throw new IllegalStateException("Solo se puede consumir una autorización aprobada (estado: " + solicitud.getEstado() + ")");
            }
            solicitudDAO.marcarUsada(idSolicitud);
            return null;
        });
    }

    private SolicitudAutorizacion obtenerExistente(int idSolicitud) throws SQLException {
        SolicitudAutorizacion solicitud = solicitudDAO.load(idSolicitud);
        if (solicitud == null) {
            throw new IllegalArgumentException("No existe la solicitud con id " + idSolicitud);
        }
        return solicitud;
    }
}
