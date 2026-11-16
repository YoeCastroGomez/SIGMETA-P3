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

    @POST
    public Response solicitar(@HeaderParam("ticket") String ticket, SolicitudDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la solicitud");
        }
        return ejecutar(ticket, Response.Status.CREATED, id ->
                convertir(solicitudBO.solicitar(id, datos.operacion, datos.motivo), new HashMap<>()));
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
