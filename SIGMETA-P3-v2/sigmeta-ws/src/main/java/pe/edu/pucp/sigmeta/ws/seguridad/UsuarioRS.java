package pe.edu.pucp.sigmeta.ws.seguridad;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.usuario.RolBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.RolBOImpl;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.usuario.Rol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF002: servicio REST de usuarios y roles. Todo es del Administrador, menos cambiar la propia clave.
//
//   GET    /api/usuarios                  todos; ?estado=true|false filtra activos o desactivados
//   GET    /api/usuarios/roles            los cuatro roles con su descripcion
//   GET    /api/usuarios/5                un usuario
//   POST   /api/usuarios                  registrar: {"nombreUsuario","nombres","apellidos","correo","idRol","clave"}
//   PUT    /api/usuarios/5                modificar (sin clave): {"nombreUsuario","nombres","apellidos","correo","idRol","estado"}
//   DELETE /api/usuarios/5                desactivar (baja logica)
//   POST   /api/usuarios/5/restablecer-clave   {"claveNueva": "..."}   el Administrador le pone una clave nueva
//   PUT    /api/usuarios/mi-clave         {"claveActual": "...", "claveNueva": "..."}   cada usuario la suya
//
// Nunca se responde el hash ni el salt de la clave: se usa UsuarioDatos, que no los tiene.
@Path("usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioRS extends VentanillaBase {

    private final UsuarioBO usuarioBO = new UsuarioBOImpl();
    private final RolBO rolBO = new RolBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket, @QueryParam("estado") String estado) {
        return ejecutar(ticket, id -> {
            usuarioBO.obtenerAdministradorActivo(id);
            List<Usuario> usuarios = estado == null || estado.isBlank()
                    ? usuarioBO.listarTodos() : usuarioBO.listarPorEstado(Boolean.parseBoolean(estado.trim()));
            List<UsuarioDatos> lista = new ArrayList<>();
            for (Usuario u : usuarios) {
                lista.add(convertir(u));
            }
            return lista;
        });
    }

    @GET
    @Path("roles")
    public Response roles(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> {
            usuarioBO.obtenerAdministradorActivo(id);
            List<Usuario> usuarios = usuarioBO.listarTodos();
            List<RolDatos> lista = new ArrayList<>();
            for (Rol r : rolBO.listarTodos()) {
                RolDatos d = new RolDatos();
                d.id = r.getId();
                d.tipo = r.getTipo() != null ? r.getTipo().name() : null;
                d.descripcion = r.getDescripcion();
                d.estado = r.isEstado();
                for (Usuario u : usuarios) {
                    if (u.getRol() != null && u.getRol().getId() == r.getId()) {
                        d.usuarios++;
                        if (u.isEstado()) {
                            d.usuariosActivos++;
                        }
                    }
                }
                lista.add(d);
            }
            return lista;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idUsuario) {
        Response respuesta = ejecutar(ticket, id -> {
            usuarioBO.obtenerAdministradorActivo(id);
            Usuario u = usuarioBO.obtener(idUsuario);
            return u == null ? null : convertir(u);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe el usuario con id " + idUsuario);
        }
        return respuesta;
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, UsuarioDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del usuario");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            Usuario usuario = armar(datos);
            usuario.setEstado(true);
            return convertir(usuarioBO.registrar(usuario, datos.clave, id));
        });
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket, @PathParam("id") int idUsuario, UsuarioDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del usuario");
        }
        return ejecutar(ticket, id -> {
            Usuario usuario = armar(datos);
            usuario.setId(idUsuario); // el id es el de la direccion, no el del cuerpo
            usuario.setEstado(datos.estado);
            usuarioBO.modificar(usuario, id);
            return convertir(usuarioBO.obtener(idUsuario));
        });
    }

    @DELETE
    @Path("{id}")
    public Response desactivar(@HeaderParam("ticket") String ticket, @PathParam("id") int idUsuario) {
        return ejecutar(ticket, id -> {
            usuarioBO.desactivar(idUsuario, id);
            return null; // 204 sin cuerpo
        });
    }

    @POST
    @Path("{id}/restablecer-clave")
    public Response restablecerClave(@HeaderParam("ticket") String ticket, @PathParam("id") int idUsuario, ClaveDatos datos) {
        String claveNueva = datos == null ? null : datos.claveNueva;
        return ejecutar(ticket, id -> {
            usuarioBO.restablecerClave(idUsuario, claveNueva, id);
            return null;
        });
    }

    // el usuario cuya clave cambia es SIEMPRE el de la sesion (no se recibe ningun id)
    @PUT
    @Path("mi-clave")
    public Response cambiarMiClave(@HeaderParam("ticket") String ticket, ClaveDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan la clave actual y la clave nueva");
        }
        return ejecutar(ticket, id -> {
            usuarioBO.cambiarClave(id, datos.claveActual, datos.claveNueva);
            return null;
        });
    }

    // ----- conversiones -----

    private static Usuario armar(UsuarioDatos datos) {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(datos.nombreUsuario);
        usuario.setNombres(datos.nombres);
        usuario.setApellidos(datos.apellidos);
        usuario.setCorreo(datos.correo);
        if (datos.idRol > 0) {
            Rol rol = new Rol();
            rol.setId(datos.idRol);
            usuario.setRol(rol);
        }
        return usuario;
    }

    private static UsuarioDatos convertir(Usuario u) {
        UsuarioDatos d = new UsuarioDatos();
        d.id = u.getId();
        d.nombreUsuario = u.getNombreUsuario();
        d.nombres = u.getNombres();
        d.apellidos = u.getApellidos();
        d.correo = u.getCorreo();
        d.estado = u.isEstado();
        d.fechaRegistro = Utilidades.fechaHora(u.getFechaRegistro());
        if (u.getRol() != null) {
            d.idRol = u.getRol().getId();
            d.rol = u.getRol().getTipo() != null ? u.getRol().getTipo().name() : null;
        }
        return d;
    }

    // RF002: usuario tal como viaja en JSON. No tiene el hash ni el salt; "clave" solo se recibe al registrar.
    public static class UsuarioDatos {
        public int id;
        public String nombreUsuario;
        public String nombres;
        public String apellidos;
        public String correo;
        public int idRol;
        public String rol;
        public boolean estado;
        public String fechaRegistro;
        public String clave;

        public UsuarioDatos() {
        }
    }

    public static class RolDatos {
        public int id;
        public String tipo;
        public String descripcion;
        public boolean estado;
        public int usuarios;
        public int usuariosActivos;

        public RolDatos() {
        }
    }

    public static class ClaveDatos {
        public String claveActual;
        public String claveNueva;

        public ClaveDatos() {
        }
    }
}
