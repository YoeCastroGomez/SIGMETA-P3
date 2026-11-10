package pe.edu.pucp.sigmeta.ws.socio;

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
import pe.edu.pucp.sigmeta.bo.socio.ProveedorBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.socio.ProveedorBOImpl;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// RF004: gestion de proveedores. Solo el Administrador puede usar este servicio.
//
// GET    /api/proveedores                         listar activos e inactivos
// GET    /api/proveedores?estado=true             filtrar por estado
// GET    /api/proveedores?ruc=20431298765        buscar por RUC
// GET    /api/proveedores?razonSocial=industrial  buscar por razon social
// GET    /api/proveedores/{id}                    consultar uno
// POST   /api/proveedores                         registrar
// PUT    /api/proveedores/{id}                    modificar
// DELETE /api/proveedores/{id}                    desactivar (baja logica)
//
// Todos los pedidos reciben el ticket en la cabecera HTTP "ticket".
// La identidad del Administrador viene de la sesion, nunca del JSON de Blazor.
@Path("proveedores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProveedorRS extends VentanillaBase {

    private final ProveedorBO proveedorBO = new ProveedorBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("ruc") String ruc,
                           @QueryParam("razonSocial") String razonSocial,
                           @QueryParam("estado") Boolean estado) {
        return ejecutar(ticket, id -> {
            usuarioBO.obtenerAdministradorActivo(id);

            List<Proveedor> resultado;
            if (ruc != null && !ruc.isBlank()) {
                Proveedor encontrado = proveedorBO.buscarPorRuc(ruc.trim());
                resultado = new ArrayList<>();
                if (encontrado != null) {
                    resultado.add(encontrado);
                }
            } else if (razonSocial != null && !razonSocial.isBlank()) {
                resultado = proveedorBO.buscarPorRazonSocial(razonSocial.trim());
            } else if (estado != null) {
                return proveedorBO.listarPorEstado(estado);
            } else {
                // listarTodos() solo trae activos; "Todos" debe incluir inactivos.
                resultado = new ArrayList<>(proveedorBO.listarPorEstado(true));
                resultado.addAll(proveedorBO.listarPorEstado(false));
            }

            // Permite combinar RUC, razon social y estado desde los filtros de Blazor.
            if (ruc != null && !ruc.isBlank() && razonSocial != null && !razonSocial.isBlank()) {
                String texto = razonSocial.trim().toLowerCase(Locale.ROOT);
                resultado.removeIf(p -> p.getRazonSocial() == null
                        || !p.getRazonSocial().toLowerCase(Locale.ROOT).contains(texto));
            }
            if (estado != null) {
                resultado.removeIf(p -> p.isEstado() != estado);
            }
            return resultado;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket,
                            @PathParam("id") int idProveedor) {
        Response respuesta = ejecutar(ticket, id -> {
            usuarioBO.obtenerAdministradorActivo(id);
            return proveedorBO.obtener(idProveedor);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe el proveedor con id " + idProveedor);
        }
        return respuesta;
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, Proveedor proveedor) {
        if (proveedor == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del proveedor");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            // El cliente no define ni el ID ni el estado inicial.
            proveedor.setId(0);
            proveedor.setEstado(true);
            proveedor.setCompras(new ArrayList<>());
            return proveedorBO.registrar(proveedor, id);
        });
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket,
                              @PathParam("id") int idProveedor,
                              Proveedor proveedor) {
        if (proveedor == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del proveedor");
        }
        return ejecutar(ticket, id -> {
            usuarioBO.obtenerAdministradorActivo(id);
            Proveedor actual = proveedorBO.obtener(idProveedor);
            if (actual == null) {
                throw new IllegalArgumentException("No existe el proveedor con id " + idProveedor);
            }
            // Evita que un PUT sin 'estado' desactive al proveedor accidentalmente.
            // Las bajas logicas se hacen exclusivamente con DELETE.
            proveedor.setId(idProveedor);
            proveedor.setEstado(actual.isEstado());
            proveedor.setCompras(new ArrayList<>());
            return proveedorBO.modificar(proveedor, id);
        });
    }

    @DELETE
    @Path("{id}")
    public Response desactivar(@HeaderParam("ticket") String ticket,
                               @PathParam("id") int idProveedor) {
        return ejecutar(ticket, id -> {
            proveedorBO.desactivar(idProveedor, id);
            return null; // 204 sin contenido
        });
    }
}
