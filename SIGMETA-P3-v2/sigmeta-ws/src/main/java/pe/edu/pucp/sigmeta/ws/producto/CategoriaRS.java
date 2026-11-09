package pe.edu.pucp.sigmeta.ws.producto;

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
import pe.edu.pucp.sigmeta.bo.producto.CategoriaBO;
import pe.edu.pucp.sigmeta.boimpl.producto.CategoriaBOImpl;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;

import java.util.ArrayList;
import java.util.List;

// RF006: servicio REST de categorias de productos.
//
//   GET    /api/categorias                   listar todas (activas e inactivas)
//   GET    /api/categorias?nombre=herra      buscar por nombre (coincidencia parcial, activas e inactivas)
//   GET    /api/categorias?estado=true       listar por estado (true activas, false inactivas)
//   GET    /api/categorias/3                 obtener una por id
//   POST   /api/categorias                   registrar (solo Administrador)
//   PUT    /api/categorias/3                 modificar (solo Administrador)
//   DELETE /api/categorias/3                 desactivar (solo Administrador, baja logica)
//
// Las consultas las puede hacer cualquier rol (el formulario de productos necesita la lista).
// Registrar, modificar y desactivar los valida el BO: si no es Administrador responde
// "El rol X no tiene permiso..." (403). Si la categoria tiene productos activos, el BO responde
// "La categoria tiene productos activos; reasignelos o desactivelos primero" (409).
@Path("categorias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoriaRS extends VentanillaBase {

    private CategoriaBO categoriaBO = new CategoriaBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("nombre") String nombre,
                           @QueryParam("estado") Boolean estado) {
        return ejecutar(ticket, id -> {
            if (nombre != null && !nombre.isBlank()) {
                return categoriaBO.buscarPorNombre(nombre.trim());
            }
            if (estado != null) {
                return categoriaBO.listarPorEstado(estado);
            }
            // listarTodos del BO solo trae las activas; la pantalla muestra todas con su estado
            List<Categoria> todas = new ArrayList<>(categoriaBO.listarPorEstado(true));
            todas.addAll(categoriaBO.listarPorEstado(false));
            return todas;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idCategoria) {
        Response respuesta = ejecutar(ticket, id -> categoriaBO.obtener(idCategoria));
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe la categoria con id " + idCategoria);
        }
        return respuesta;
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, Categoria categoria) {
        return ejecutar(ticket, Response.Status.CREATED, id -> categoriaBO.registrar(categoria, id));
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCategoria,
                              Categoria categoria) {
        if (categoria == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la categoria");
        }
        categoria.setId(idCategoria); // el id sale de la direccion, no del cuerpo
        return ejecutar(ticket, id -> categoriaBO.modificar(categoria, id));
    }

    @DELETE
    @Path("{id}")
    public Response desactivar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCategoria) {
        return ejecutar(ticket, id -> {
            categoriaBO.desactivar(idCategoria, id);
            return null; // 204 sin cuerpo
        });
    }
}