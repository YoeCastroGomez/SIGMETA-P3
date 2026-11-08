package pe.edu.pucp.sigmeta.ws.seguridad;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.seguridad.SesionServicio;
import pe.edu.pucp.sigmeta.ws.base.Sesiones;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;

// RF001: inicio y cierre de sesion.
//   POST   /api/sesion   iniciar sesion: recibe Credenciales y devuelve DatosSesion con el ticket
//   GET    /api/sesion   quien esta conectado (con el ticket)
//   DELETE /api/sesion   cerrar sesion (con el ticket)
@Path("sesion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SesionRS extends VentanillaBase {

    private UsuarioBO usuarioBO = new UsuarioBOImpl();

    @POST
    public Response iniciarSesion(Credenciales credenciales) {
        if (credenciales == null) {
            return error(Response.Status.BAD_REQUEST, "Usuario o clave incorrectos");
        }
        try {
            // el BO valida y lanza "Usuario o clave incorrectos", "El usuario se encuentra desactivado"...
            SesionServicio sesion = SesionServicio.iniciar(credenciales.getNombreUsuario(), credenciales.getClave());
            String ticket = Sesiones.guardar(sesion);
            Usuario usuario = sesion.ejecutar(id -> usuarioBO.obtener(id));
            DatosSesion datos = convertir(usuario);
            datos.setTicket(ticket);
            return Response.ok(datos).build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error en el servidor: " + e.getMessage());
        }
    }

    @GET
    public Response obtenerSesion(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> convertir(usuarioBO.obtener(id)));
    }

    @DELETE
    public Response cerrarSesion(@HeaderParam("ticket") String ticket) {
        Sesiones.cerrar(ticket);
        return Response.noContent().build();
    }

    private DatosSesion convertir(Usuario usuario) {
        DatosSesion datos = new DatosSesion();
        datos.setNombreUsuario(usuario.getNombreUsuario());
        datos.setNombres(usuario.getNombres());
        datos.setApellidos(usuario.getApellidos());
        datos.setRol(usuario.getRol().getTipo().name());
        return datos;
    }
}
