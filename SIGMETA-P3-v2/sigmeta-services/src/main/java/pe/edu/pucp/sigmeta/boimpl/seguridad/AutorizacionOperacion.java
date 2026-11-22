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
import java.time.LocalDateTime;
import static pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.ROL_POR_OPERACION;
import static pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.MARCA_REQUIERE_AUTORIZACION;

// RF014: pedir y exigir la autorizacion de UNA operacion concreta (identificada por su huella).
// Parte de SolicitudAutorizacionBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class AutorizacionOperacion {

    private final SolicitudAutorizacionDAO solicitudDAO;
    private final UsuarioBO usuarioBO;

    AutorizacionOperacion(SolicitudAutorizacionDAO solicitudDAO, UsuarioBO usuarioBO) {
        this.solicitudDAO = solicitudDAO;
        this.usuarioBO = usuarioBO;
    }

    public SolicitudAutorizacion solicitarOperacion(int idSolicitante, String operacionRestringida, String motivo,
                                                    String referencia, String detalle, String huella) throws SQLException {
        Validador.obligatorio(huella, "datos de la operación");
        return Transaccion.ejecutar(() -> {
            Usuario solicitante = usuarioBO.obtener(idSolicitante);
            if (solicitante == null || !solicitante.isEstado()) {
                throw new IllegalArgumentException("El solicitante no existe o está desactivado");
            }
            String operacion = operacionRestringida.toUpperCase(java.util.Locale.ROOT);
            TipoRol rolDeLaOperacion = ROL_POR_OPERACION.get(operacion);
            if (rolDeLaOperacion == null) {
                throw new IllegalArgumentException("La operación restringida no existe");
            }
            if (rolDeLaOperacion != solicitante.getRol().getTipo()) {
                throw new IllegalStateException("El rol " + solicitante.getRol().getTipo()
                        + " no realiza la operación " + operacion);
            }
            // la misma operacion ya pedida: no se duplica (doble clic o se volvio a la pantalla)
            SolicitudAutorizacion existente = solicitudDAO.buscarPorHuella(idSolicitante, operacion, huella, EstadoAutorizacion.APROBADA);
            if (existente == null) {
                existente = solicitudDAO.buscarPorHuella(idSolicitante, operacion, huella, EstadoAutorizacion.PENDIENTE);
            }
            if (existente != null) {
                return existente;
            }
            SolicitudAutorizacion solicitud = new SolicitudAutorizacion();
            solicitud.setSolicitante(solicitante);
            solicitud.setOperacionRestringida(operacion);
            solicitud.setMotivo(Validador.textoObligatorio(motivo, "motivo", 200));
            solicitud.setFechaSolicitud(LocalDateTime.now());
            solicitud.setEstado(EstadoAutorizacion.PENDIENTE);
            solicitud.setVigenciaMinutos(0);
            solicitud.setReferencia(Validador.textoOpcional(recortar(referencia, 80), "referencia", 80));
            solicitud.setDetalle(recortar(detalle, 1000));
            solicitud.setHuella(huella);
            return solicitudDAO.save(solicitud);
        });
    }

    public SolicitudAutorizacion requerirAutorizacion(int idUsuario, String operacionRestringida, String huella) throws SQLException {
        return Transaccion.ejecutar(() -> {
            SolicitudAutorizacion aprobada = solicitudDAO.buscarPorHuella(idUsuario, operacionRestringida, huella, EstadoAutorizacion.APROBADA);
            if (aprobada != null && aprobada.getAdministrador() != null) {
                Usuario administrador = usuarioBO.obtener(aprobada.getAdministrador().getId());
                if (administrador != null && administrador.isEstado() && administrador.getRol().isEstado()
                        && administrador.getRol().getTipo() == TipoRol.ADMINISTRADOR) {
                    aprobada.setAdministrador(administrador);
                    aprobada.setSolicitante(usuarioBO.obtener(idUsuario));
                    return aprobada;
                }
            }
            if (solicitudDAO.buscarPorHuella(idUsuario, operacionRestringida, huella, EstadoAutorizacion.PENDIENTE) != null) {
                throw new IllegalStateException("La solicitud de autorización de esta operación está en espera del Administrador");
            }
            throw new IllegalStateException("Esta operación " + MARCA_REQUIERE_AUTORIZACION
                    + ". Use «Solicitar autorización»: el Administrador verá estos mismos datos");
        });
    }

    static String recortar(String texto, int maximo) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.length() <= maximo ? t : t.substring(0, maximo - 1) + "…";
    }
}
