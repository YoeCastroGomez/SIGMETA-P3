package pe.edu.pucp.sigmeta.ws.seguridad;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF014: servicio REST de las solicitudes de autorizacion temporal.
//
//   GET  /api/solicitudes                    todas (Administrador); ?estado=PENDIENTE solo las pendientes
//   GET  /api/solicitudes/mias               las del usuario de la sesion (Vendedor, Cajero, Almacenero)
//   GET  /api/solicitudes/en-curso?operacion=EMITIR_NOTA_CREDITO   las que se pueden retomar (con sus datos y sin
//                                            usar): las propias, o las de todos si es el Administrador
//   POST /api/solicitudes/7/descartar        su solicitante ya no la retoma (si estaba pendiente o aprobada, se anula)
//   GET  /api/solicitudes/operaciones        operaciones restringidas que puede pedir el usuario de la sesion
//   GET  /api/solicitudes/vigente?operacion=EMITIR_NOTA_CREDITO   su autorizacion aprobada y vigente (204 si no hay)
//   GET  /api/solicitudes/7                  una solicitud (su solicitante o el Administrador)
//   POST /api/solicitudes                    solicitar: {"operacion": "AJUSTE_INVENTARIO", "motivo": "..."}
//   POST /api/solicitudes/7/aprobar          aprobar con vigencia: {"vigenciaMinutos": 30}  (Administrador)
//   POST /api/solicitudes/7/rechazar         rechazar (Administrador)
//   POST /api/solicitudes/7/anular           anular o revocar (su solicitante o el Administrador)
//
// El solicitante y el administrador son siempre el usuario de la sesion (ticket). Las reglas son del BO.
@Path("solicitudes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SolicitudRS extends VentanillaBase {

    private final SolicitudAutorizacionBO solicitudBO = new SolicitudAutorizacionBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket, @QueryParam("estado") String estado) {
        return ejecutar(ticket, id -> {
            usuarioBO.obtenerAdministradorActivo(id);
            EstadoAutorizacion filtro = Utilidades.leerEnum(EstadoAutorizacion.class, estado, "el estado");
            List<SolicitudAutorizacion> solicitudes = filtro == EstadoAutorizacion.PENDIENTE
                    ? solicitudBO.listarPendientes() : solicitudBO.listarTodos();
            return convertir(solicitudes, filtro);
        });
    }

    @GET
    @Path("mias")
    public Response listarMias(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> convertir(solicitudBO.listarPorSolicitante(id), null));
    }

    // RF014: operaciones que quedaron a medias con su solicitud (pendiente, aprobada, rechazada o vencida) y
    // que se pueden retomar con los datos que se prepararon. El Administrador ve las de todos.
    @GET
    @Path("en-curso")
    public Response enCurso(@HeaderParam("ticket") String ticket, @QueryParam("operacion") String operacion) {
        return ejecutar(ticket, id -> {
            Usuario usuario = usuarioBO.verificarRol(id, TipoRol.values());
            int solicitante = usuario.getRol().getTipo() == TipoRol.ADMINISTRADOR ? 0 : id;
            return convertir(solicitudBO.listarEnCurso(solicitante, operacion), null);
        });
    }

    @POST
    @Path("{id}/descartar")
    public Response descartar(@HeaderParam("ticket") String ticket, @PathParam("id") int idSolicitud) {
        return ejecutar(ticket, id -> {
            solicitudBO.descartar(idSolicitud, id);
            return null; // 204 sin cuerpo
        });
    }

    @GET
    @Path("operaciones")
    public Response operaciones(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> {
            Usuario usuario = usuarioBO.verificarRol(id, TipoRol.values());
            List<Map<String, String>> lista = new ArrayList<>();
            for (Map.Entry<String, String> e : SolicitudAutorizacionBOImpl.operacionesDe(usuario.getRol().getTipo()).entrySet()) {
                Map<String, String> fila = new HashMap<>();
                fila.put("codigo", e.getKey());
                fila.put("descripcion", e.getValue());
                lista.add(fila);
            }
            return lista;
        });
    }

    @GET
    @Path("vigente")
    public Response vigente(@HeaderParam("ticket") String ticket, @QueryParam("operacion") String operacion) {
        return ejecutar(ticket, id -> {
            SolicitudAutorizacion s = solicitudBO.obtenerAutorizacionVigente(id, operacion);
            return s == null ? null : convertir(s, new HashMap<>());
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idSolicitud) {
        Response respuesta = ejecutar(ticket, id -> {
            SolicitudAutorizacion s = solicitudBO.obtener(idSolicitud);
            if (s == null) {
                return null;
            }
            // RF014: la ve quien la pidio o un Administrador
            if (s.getSolicitante() == null || s.getSolicitante().getId() != id) {
                usuarioBO.obtenerAdministradorActivo(id);
            }
            return convertir(s, new HashMap<>());
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe la solicitud con id " + idSolicitud);
        }
        return respuesta;
    }

    // RF014: la autorizacion ya no se pide suelta: se pide desde la operacion, con sus datos
    // (POST /api/notas-credito/solicitar, /api/ventas/solicitar-autorizacion, /api/almacen/ajustes/solicitar)
    @POST
    public Response solicitar(@HeaderParam("ticket") String ticket, SolicitudDatos datos) {
        return error(Response.Status.BAD_REQUEST,
                "La autorización se pide desde la operación (nota de crédito, venta o ajuste), con sus datos");
    }

    // Lo que responden los POST .../solicitar de cada operacion. Guarda tambien lo que preparo la pantalla
    // (datosOperacion, JSON) para que el solicitante pueda retomar la operacion sin volver a pedirla.
    public static Map<String, Object> resumen(SolicitudAutorizacion s, int idSolicitante, String datosOperacion)
            throws SQLException {
        if (datosOperacion != null && !datosOperacion.isBlank()) {
            new SolicitudAutorizacionBOImpl().guardarDatosOperacion(s.getId(), idSolicitante, datosOperacion);
        }
        return resumen(s);
    }

    public static Map<String, Object> resumen(SolicitudAutorizacion s) {
        Map<String, Object> r = new HashMap<>();
        r.put("id", s.getId());
        r.put("estado", s.getEstado() == null ? null : s.getEstado().name());
        r.put("referencia", s.getReferencia());
        r.put("operacion", s.getOperacionRestringida());
        return r;
    }

    @POST
    @Path("{id}/aprobar")
    public Response aprobar(@HeaderParam("ticket") String ticket, @PathParam("id") int idSolicitud, SolicitudDatos datos) {
        int vigencia = datos == null ? 0 : datos.vigenciaMinutos;
        return ejecutar(ticket, id -> convertir(solicitudBO.aprobar(idSolicitud, id, vigencia), new HashMap<>()));
    }

    @POST
    @Path("{id}/rechazar")
    public Response rechazar(@HeaderParam("ticket") String ticket, @PathParam("id") int idSolicitud) {
        return ejecutar(ticket, id -> convertir(solicitudBO.rechazar(idSolicitud, id), new HashMap<>()));
    }

    @POST
    @Path("{id}/anular")
    public Response anular(@HeaderParam("ticket") String ticket, @PathParam("id") int idSolicitud) {
        return ejecutar(ticket, id -> {
            solicitudBO.anular(idSolicitud, id);
            return null; // 204 sin cuerpo
        });
    }

    // ----- conversiones -----

    private List<SolicitudDatos> convertir(List<SolicitudAutorizacion> solicitudes, EstadoAutorizacion filtro)
            throws SQLException {
        Map<Integer, Usuario> usuarios = new HashMap<>();
        List<SolicitudDatos> lista = new ArrayList<>();
        for (SolicitudAutorizacion s : solicitudes) {
            SolicitudDatos d = convertir(s, usuarios);
            if (filtro == null || filtro.name().equals(d.estado)) {
                lista.add(d);
            }
        }
        // las mas recientes primero
        lista.sort((a, b) -> Integer.compare(b.id, a.id));
        return lista;
    }

    // Los DAO devuelven el solicitante y el administrador solo con su id: aqui se completan sus nombres.
    private SolicitudDatos convertir(SolicitudAutorizacion s, Map<Integer, Usuario> usuarios) throws SQLException {
        SolicitudDatos d = new SolicitudDatos();
        d.id = s.getId();
        d.operacion = s.getOperacionRestringida();
        d.descripcionOperacion = SolicitudAutorizacionBOImpl.descripcionDe(s.getOperacionRestringida());
        d.motivo = s.getMotivo();
        d.fechaSolicitud = Utilidades.fechaHora(s.getFechaSolicitud());
        d.fechaResolucion = Utilidades.fechaHora(s.getFechaResolucion());
        d.fechaVencimiento = Utilidades.fechaHora(s.getFechaVencimiento());
        d.vigenciaMinutos = s.getVigenciaMinutos();
        d.referencia = s.getReferencia();
        d.detalle = s.getDetalle();
        d.datosOperacion = s.getDatosOperacion();
        Usuario solicitante = usuario(s.getSolicitante(), usuarios);
        if (solicitante != null) {
            d.idSolicitante = solicitante.getId();
            d.solicitante = solicitante.getNombreUsuario();
            d.nombreSolicitante = solicitante.getNombres() + " " + solicitante.getApellidos();
            d.rolSolicitante = solicitante.getRol() != null && solicitante.getRol().getTipo() != null
                    ? solicitante.getRol().getTipo().name() : null;
        }
        Usuario administrador = usuario(s.getAdministrador(), usuarios);
        if (administrador != null) {
            d.administrador = administrador.getNombreUsuario();
        }
        // Una APROBADA cuya vigencia ya paso se guarda como APROBADA pero ya no sirve: se muestra VENCIDA
        EstadoAutorizacion estado = s.getEstado();
        if (estado == EstadoAutorizacion.APROBADA && s.getFechaVencimiento() != null
                && !s.getFechaVencimiento().isAfter(LocalDateTime.now())) {
            estado = EstadoAutorizacion.VENCIDA;
        }
        d.estado = estado != null ? estado.name() : null;
        d.vigente = estado == EstadoAutorizacion.APROBADA;
        // RF014: el anular de su solicitante la deja RECHAZADA sin Administrador: fue una cancelacion
        d.cancelada = estado == EstadoAutorizacion.RECHAZADA && s.getAdministrador() == null;
        return d;
    }

    private Usuario usuario(Usuario referencia, Map<Integer, Usuario> usuarios) throws SQLException {
        if (referencia == null || referencia.getId() <= 0) {
            return null;
        }
        Usuario usuario = usuarios.get(referencia.getId());
        if (usuario == null) {
            usuario = usuarioBO.obtener(referencia.getId());
            if (usuario != null) {
                usuarios.put(usuario.getId(), usuario);
            }
        }
        return usuario;
    }
}
