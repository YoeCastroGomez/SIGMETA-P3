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
import pe.edu.pucp.sigmeta.bo.socio.ClienteBO;
import pe.edu.pucp.sigmeta.boimpl.socio.ClienteBOImpl;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;

import java.util.ArrayList;
import java.util.List;

// RF003: servicio REST de clientes. Es el EJEMPLO que copian los demas servicios.
//
//   GET    /api/clientes                     listar todos
//   GET    /api/clientes?estado=true         listar por estado (true activos, false desactivados)
//   GET    /api/clientes?documento=4578...   buscar por documento
//   GET    /api/clientes?razonSocial=andina  buscar por razon social o nombre
//   GET    /api/clientes?razonSocial=andina&estado=false   los dos filtros juntos (ClienteBO.buscar)
//   GET    /api/clientes/5                   obtener uno por id
//   POST   /api/clientes                     registrar (Vendedor o Administrador)
//   PUT    /api/clientes/5                   modificar
//   DELETE /api/clientes/5                   desactivar (baja logica)
//   PUT    /api/clientes/5/linea-credito     asignar linea de credito (solo Administrador)
//   DELETE /api/clientes/5/linea-credito     retirar linea de credito (solo Administrador)
//
// Todos reciben el ticket en el encabezado "ticket". Las validaciones y los mensajes son del BO.
@Path("clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClienteRS extends VentanillaBase {

    private ClienteBO clienteBO = new ClienteBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("estado") Boolean estado,
                           @QueryParam("documento") String documento,
                           @QueryParam("razonSocial") String razonSocial) {
        return ejecutar(ticket, id -> {
            if (documento != null && !documento.isBlank()) {
                List<Cliente> lista = new ArrayList<>();
                Cliente cliente = clienteBO.buscarPorDocumento(documento.trim());
                if (cliente != null) {
                    lista.add(cliente);
                }
                return lista;
            }
            // razon social y estado se combinan (cada uno opcional)
            return clienteBO.buscar(razonSocial, estado);
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idCliente) {
        Response respuesta = ejecutar(ticket, id -> clienteBO.obtener(idCliente));
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe el cliente con id " + idCliente);
        }
        return respuesta;
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, Cliente cliente) {
        return ejecutar(ticket, Response.Status.CREATED, id -> clienteBO.registrar(cliente, id));
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCliente, Cliente cliente) {
        if (cliente == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del cliente");
        }
        cliente.setId(idCliente); // no cambiar el id
        return ejecutar(ticket, id -> clienteBO.modificar(cliente, id));
    }

    @DELETE
    @Path("{id}")
    public Response desactivar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCliente) {
        return ejecutar(ticket, id -> {
            clienteBO.desactivar(idCliente, id);
            return null; // 204 sin cuerpo
        });
    }

    @PUT
    @Path("{id}/linea-credito")
    public Response asignarLineaCredito(@HeaderParam("ticket") String ticket, @PathParam("id") int idCliente,
                                        LineaCredito linea) {
        if (linea == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la linea de credito");
        }
        return ejecutar(ticket, id -> clienteBO.asignarLineaCredito(idCliente, linea.getLimiteCredito(),
                linea.getPlazoCreditoDias(), linea.getCalificacionCrediticia(), id));
    }

    @DELETE
    @Path("{id}/linea-credito")
    public Response retirarLineaCredito(@HeaderParam("ticket") String ticket, @PathParam("id") int idCliente) {
        return ejecutar(ticket, id -> clienteBO.retirarLineaCredito(idCliente, id));
    }
}
