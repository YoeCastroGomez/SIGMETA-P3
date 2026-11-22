package pe.edu.pucp.sigmeta.boimpl.seguridad;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.dao.seguridad.SolicitudAutorizacionDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

// RF014: retomar una operacion con su solicitud (guardar lo que se preparo en la pantalla, listar las que estan
// en curso y descartarlas). Parte de SolicitudAutorizacionBOImpl: la clase publica conserva su API y delega aqui.
final class AutorizacionRetomable {

    private final SolicitudAutorizacionDAO solicitudDAO;

    AutorizacionRetomable(SolicitudAutorizacionDAO solicitudDAO) {
        this.solicitudDAO = solicitudDAO;
    }

    // tope del JSON de la pantalla (la columna es TEXT: 64 KB)
    private static final int DATOS_OPERACION_MAXIMO = 60000;

    void guardarDatosOperacion(int idSolicitud, int idSolicitante, String datosOperacion) throws SQLException {
        if (datosOperacion == null || datosOperacion.isBlank()) {
            return; // la pantalla no mando datos: la solicitud vale igual, solo no se podra retomar
        }
        if (datosOperacion.length() > DATOS_OPERACION_MAXIMO) {
            throw new IllegalArgumentException("Los datos de la operación son demasiado grandes para guardarlos");
        }
        Transaccion.ejecutar(() -> {
            SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
            if (solicitud.getSolicitante() == null || solicitud.getSolicitante().getId() != idSolicitante) {
                throw new IllegalStateException("Solo quien pidió la autorización guarda los datos de su operación");
            }
            if (solicitud.getEstado() != EstadoAutorizacion.PENDIENTE && solicitud.getEstado() != EstadoAutorizacion.APROBADA) {
                return null; // ya resuelta: no se cambia
            }
            solicitudDAO.guardarDatosOperacion(idSolicitud, datosOperacion);
            // se volvio a pedir la MISMA operacion (misma huella) despues de un rechazo o un vencimiento: la solicitud
            // anterior ya no se retoma (si no, la operacion apareceria dos veces en la lista)
            LocalDateTime ahora = LocalDateTime.now();
            for (SolicitudAutorizacion anterior : solicitudDAO.listarPorSolicitante(idSolicitante)) {
                boolean mismaOperacion = anterior.getId() != idSolicitud && anterior.getDatosOperacion() != null
                        && solicitud.getHuella() != null && solicitud.getHuella().equals(anterior.getHuella())
                        && solicitud.getOperacionRestringida().equals(anterior.getOperacionRestringida());
                boolean yaNoSirve = anterior.getEstado() == EstadoAutorizacion.RECHAZADA
                        || anterior.getEstado() == EstadoAutorizacion.VENCIDA
                        || (anterior.getEstado() == EstadoAutorizacion.APROBADA && anterior.getFechaVencimiento() != null
                            && !anterior.getFechaVencimiento().isAfter(ahora));
                if (mismaOperacion && yaNoSirve) {
                    solicitudDAO.guardarDatosOperacion(anterior.getId(), null);
                }
            }
            return null;
        });
    }

    List<SolicitudAutorizacion> listarEnCurso(int idSolicitante, String operacionRestringida) throws SQLException {
        return Transaccion.consultar(() -> {
            List<SolicitudAutorizacion> todas = idSolicitante > 0
                    ? solicitudDAO.listarPorSolicitante(idSolicitante) : solicitudDAO.listarTodos();
            String operacion = operacionRestringida == null || operacionRestringida.isBlank() ? null
                    : operacionRestringida.trim().toUpperCase(java.util.Locale.ROOT);
            return todas.stream()
                    .filter(s -> s.getDatosOperacion() != null && s.getEstado() != EstadoAutorizacion.USADA)
                    .filter(s -> operacion == null || operacion.equals(s.getOperacionRestringida()))
                    .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
                    .toList();
        });
    }

    void descartar(int idSolicitud, int idResponsable) throws SQLException {
        Transaccion.ejecutar(() -> {
            SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
            if (solicitud.getSolicitante() == null || solicitud.getSolicitante().getId() != idResponsable) {
                throw new IllegalStateException("Solo quien pidió la autorización puede descartarla");
            }
            if (solicitud.getEstado() == EstadoAutorizacion.USADA) {
                throw new IllegalStateException("Esta autorización ya se usó en su operación");
            }
            if (solicitud.getEstado() == EstadoAutorizacion.PENDIENTE || solicitud.getEstado() == EstadoAutorizacion.APROBADA) {
                solicitudDAO.remove(solicitud); // pendiente: queda cancelada; aprobada: ya no se puede usar
            }
            solicitudDAO.guardarDatosOperacion(idSolicitud, null);
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
