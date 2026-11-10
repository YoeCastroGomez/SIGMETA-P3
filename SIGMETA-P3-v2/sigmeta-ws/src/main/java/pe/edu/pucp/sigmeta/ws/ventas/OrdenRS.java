package pe.edu.pucp.sigmeta.ws.ventas;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.CotizacionBO;
import pe.edu.pucp.sigmeta.bo.ventas.OrdenCompraClienteBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.CotizacionBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.OrdenCompraClienteBOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF007: servicio REST de ordenes de compra de clientes (Vendedor; anular tambien el Administrador).
//
//   GET    /api/ordenes               listar todas, con su estado de atencion
//   GET    /api/ordenes/4             una orden con sus lineas (cantidad pedida y atendida)
//   POST   /api/ordenes               generar desde una cotizacion ACEPTADA: {"idCotizacion": 2, "numeroOrdenCliente": "..."}
//   PUT    /api/ordenes/4             modificar numero del cliente y observaciones (solo PENDIENTE)
//   POST   /api/ordenes/4/anular      anular con motivo (solo PENDIENTE y sin ventas vigentes): {"motivo": "..."}
//
// El responsable es siempre el usuario de la sesion (ticket). Las reglas y sus mensajes son del BO.
@Path("ordenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrdenRS extends VentanillaBase {

    private final OrdenCompraClienteBO ordenBO = new OrdenCompraClienteBOImpl();
    private final CotizacionBO cotizacionBO = new CotizacionBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> {
            permitirConsulta(id);
            Map<Integer, String> numerosCotizacion = new HashMap<>();
            for (Cotizacion c : cotizacionBO.listarTodos()) {
                numerosCotizacion.put(c.getId(), c.getNumero());
            }
            Nombres nombres = new Nombres();
            List<OrdenDatos> lista = new ArrayList<>();
            for (OrdenCompraCliente oc : ordenBO.listarTodos()) {
                lista.add(convertir(oc, nombres, numerosCotizacion, false));
            }
            return lista;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idOrden) {
        Response respuesta = ejecutar(ticket, id -> {
            permitirConsulta(id);
            OrdenCompraCliente oc = ordenBO.obtener(idOrden);
            return oc == null ? null : conDetalle(oc);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe la orden de compra con id " + idOrden);
        }
        return respuesta;
    }

    @POST
    public Response generar(@HeaderParam("ticket") String ticket, OrdenDatos datos) {
        if (datos == null || datos.idCotizacion <= 0) {
            return error(Response.Status.BAD_REQUEST, "Elija la cotizacion aceptada de la que sale la orden");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            OrdenCompraCliente oc = ordenBO.generarDesdeCotizacion(datos.idCotizacion, datos.numeroOrdenCliente, id);
            return conDetalle(ordenBO.obtener(oc.getId()));
        });
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket, @PathParam("id") int idOrden, OrdenDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la orden de compra");
        }
        return ejecutar(ticket, id -> {
            OrdenCompraCliente oc = new OrdenCompraCliente();
            oc.setId(idOrden); // el id es el de la direccion, no el del cuerpo
            oc.setNumeroOrdenCliente(datos.numeroOrdenCliente);
            oc.setObservaciones(datos.observaciones);
            ordenBO.modificar(oc, id);
            return conDetalle(ordenBO.obtener(idOrden));
        });
    }

    @POST
    @Path("{id}/anular")
    public Response anular(@HeaderParam("ticket") String ticket, @PathParam("id") int idOrden,
                           AnulacionDatos datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> {
            ordenBO.anular(idOrden, motivo, id);
            return null; // 204 sin cuerpo
        });
    }

    // ----- conversiones -----

    // RF001: las ordenes las consultan el Vendedor y el Administrador
    private void permitirConsulta(int idUsuario) throws SQLException {
        usuarioBO.verificarRol(idUsuario, TipoRol.VENDEDOR, TipoRol.ADMINISTRADOR);
    }

    private OrdenDatos conDetalle(OrdenCompraCliente oc) throws SQLException {
        Map<Integer, String> numerosCotizacion = new HashMap<>();
        if (oc.getCotizacion() != null) {
            Cotizacion cotizacion = cotizacionBO.obtener(oc.getCotizacion().getId());
            if (cotizacion != null) {
                numerosCotizacion.put(cotizacion.getId(), cotizacion.getNumero());
            }
        }
        return convertir(oc, new Nombres(), numerosCotizacion, true);
    }

    static OrdenDatos convertir(OrdenCompraCliente oc, Nombres nombres, Map<Integer, String> numerosCotizacion,
                                boolean conLineas) throws SQLException {
        OrdenDatos d = new OrdenDatos();
        d.id = oc.getId();
        d.numero = oc.getNumero();
        d.numeroOrdenCliente = oc.getNumeroOrdenCliente();
        if (oc.getCotizacion() != null) {
            d.idCotizacion = oc.getCotizacion().getId();
            d.numeroCotizacion = numerosCotizacion.get(d.idCotizacion);
        }
        if (oc.getCliente() != null) {
            d.idCliente = oc.getCliente().getId();
            Cliente cliente = nombres.cliente(d.idCliente);
            if (cliente != null) {
                d.cliente = cliente.getNombreMostrado();
                d.documentoCliente = cliente.getNumeroDocumento();
            }
        }
        d.fechaEmision = Utilidades.fecha(oc.getFechaEmision());
        d.anulado = oc.isAnulado();
        d.estado = oc.isAnulado() ? EstadoOrdenCompraCliente.ANULADA.name()
                : (oc.getEstado() != null ? oc.getEstado().name() : null);
        d.observaciones = oc.getObservaciones();
        d.subTotal = oc.getSubTotal();
        d.igv = oc.getIgv();
        d.total = oc.getTotal();
        d.usuarioRegistro = nombres.usuario(oc.getUsuarioRegistro());
        d.fechaRegistro = Utilidades.fechaHora(oc.getFechaRegistro());
        d.motivoAnulacion = oc.getMotivoAnulacion();
        d.fechaAnulacion = Utilidades.fechaHora(oc.getFechaAnulacion());
        if (conLineas && oc.getDetalles() != null) {
            for (DetalleOrdenCompraCliente det : oc.getDetalles()) {
                LineaDatos l = CotizacionRS.convertirLinea(det.getNumeroLinea(), det.getProducto(), det.getCantidad(),
                        det.getPrecioUnitario(), det.getDescuento(), det.getImporte(), nombres);
                l.idLinea = det.getId();
                l.cantidadAtendida = det.getCantidadAtendida();
                d.lineas.add(l);
            }
        }
        return d;
    }
}
