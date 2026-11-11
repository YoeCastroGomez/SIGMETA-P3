package pe.edu.pucp.sigmeta.ws.ventas;

import jakarta.ws.rs.Consumes;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.CotizacionBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.CotizacionBOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoCotizacion;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleCotizacion;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF007: servicio REST de cotizaciones (Vendedor; anular tambien el Administrador).
//
//   GET    /api/cotizaciones                  listar todas (no anuladas); antes de listar marca como VENCIDA
//                                             las REGISTRADA o ENVIADA con vigencia pasada (marcarVencidas)
//   GET    /api/cotizaciones?cliente=3        listar por cliente (listarPorCliente)
//   GET    /api/cotizaciones?estado=ENVIADA   listar por estado (listarPorEstado)
//   GET    /api/cotizaciones/5                una cotizacion con sus lineas
//   POST   /api/cotizaciones                  registrar (cabecera + lineas)
//   PUT    /api/cotizaciones/5                modificar (solo REGISTRADA)
//   POST   /api/cotizaciones/5/enviar         REGISTRADA -> ENVIADA
//   POST   /api/cotizaciones/5/aceptar        ENVIADA -> ACEPTADA (no si ya vencio)
//   POST   /api/cotizaciones/5/rechazar       ENVIADA -> RECHAZADA
//   POST   /api/cotizaciones/5/anular         anular con motivo: {"motivo": "..."}
//
// El responsable es siempre el usuario de la sesion (ticket); Blazor nunca manda su id.
// Las reglas y sus mensajes son del BO. Se responde con CotizacionDatos (y no con la clase Cotizacion)
// para no mandar el ciclo cotizacion <-> detalle ni el Usuario completo con el hash de su clave.
@Path("cotizaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CotizacionRS extends VentanillaBase {

    private final CotizacionBO cotizacionBO = new CotizacionBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("cliente") Integer idCliente,
                           @QueryParam("estado") String estado) {
        return ejecutar(ticket, id -> {
            permitirConsulta(id);
            // RF007: las cotizaciones con vigencia pasada se muestran como VENCIDA
            cotizacionBO.marcarVencidas();
            List<Cotizacion> cotizaciones;
            if (idCliente != null) {
                cotizaciones = cotizacionBO.listarPorCliente(idCliente);
            } else if (estado != null && !estado.isBlank()) {
                cotizaciones = cotizacionBO.listarPorEstado(
                        Utilidades.leerEnum(EstadoCotizacion.class, estado, "estado de la cotizacion"));
            } else {
                cotizaciones = cotizacionBO.listarTodos();
            }
            Nombres nombres = new Nombres();
            List<CotizacionDatos> lista = new ArrayList<>();
            for (Cotizacion c : cotizaciones) {
                lista.add(convertir(c, nombres, false));
            }
            return lista;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idCotizacion) {
        Response respuesta = ejecutar(ticket, id -> {
            permitirConsulta(id);
            Cotizacion cotizacion = cotizacionBO.obtener(idCotizacion);
            return cotizacion == null ? null : convertir(cotizacion, new Nombres(), true);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe la cotizacion con id " + idCotizacion);
        }
        return respuesta;
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, CotizacionDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la cotizacion");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            Cotizacion registrada = cotizacionBO.registrar(aCotizacion(datos), id);
            return convertir(cotizacionBO.obtener(registrada.getId()), new Nombres(), true);
        });
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCotizacion,
                              CotizacionDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la cotizacion");
        }
        return ejecutar(ticket, id -> {
            Cotizacion cotizacion = aCotizacion(datos);
            cotizacion.setId(idCotizacion); // el id es el de la direccion, no el del cuerpo
            cotizacionBO.modificar(cotizacion, id);
            return convertir(cotizacionBO.obtener(idCotizacion), new Nombres(), true);
        });
    }

    // las acciones sin cuerpo aceptan cualquier tipo de contenido: un POST vacio a veces llega sin
    // "application/json" (por ejemplo desde PowerShell) y sin esto GlassFish responde 415
    @POST
    @Path("{id}/enviar")
    @Consumes(MediaType.WILDCARD)
    public Response enviar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCotizacion) {
        return ejecutar(ticket, id -> {
            cotizacionBO.enviar(idCotizacion, id);
            return convertir(cotizacionBO.obtener(idCotizacion), new Nombres(), true);
        });
    }

    @POST
    @Path("{id}/aceptar")
    @Consumes(MediaType.WILDCARD)
    public Response aceptar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCotizacion) {
        return ejecutar(ticket, id -> {
            cotizacionBO.aceptar(idCotizacion, id);
            return convertir(cotizacionBO.obtener(idCotizacion), new Nombres(), true);
        });
    }

    @POST
    @Path("{id}/rechazar")
    @Consumes(MediaType.WILDCARD)
    public Response rechazar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCotizacion) {
        return ejecutar(ticket, id -> {
            cotizacionBO.rechazar(idCotizacion, id);
            return convertir(cotizacionBO.obtener(idCotizacion), new Nombres(), true);
        });
    }

    @POST
    @Path("{id}/anular")
    public Response anular(@HeaderParam("ticket") String ticket, @PathParam("id") int idCotizacion,
                           AnulacionDatos datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> {
            cotizacionBO.anular(idCotizacion, motivo, id);
            return null; // 204 sin cuerpo
        });
    }

    // ----- conversiones entre las clases del modelo y lo que viaja en JSON -----

    // RF001: las cotizaciones las consultan el Vendedor y el Administrador
    private void permitirConsulta(int idUsuario) throws SQLException {
        usuarioBO.verificarRol(idUsuario, TipoRol.VENDEDOR, TipoRol.ADMINISTRADOR);
    }

    private Cotizacion aCotizacion(CotizacionDatos datos) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setNumero(datos.numero);
        cotizacion.setFechaEmision(Utilidades.leerFecha(datos.fechaEmision, "fecha de emision"));
        cotizacion.setFechaVigencia(Utilidades.leerFecha(datos.fechaVigencia, "fecha de vigencia"));
        cotizacion.setObservaciones(datos.observaciones);
        if (datos.idCliente > 0) {
            Cliente cliente = new Cliente();
            cliente.setId(datos.idCliente);
            cotizacion.setCliente(cliente); // sin cliente, el BO responde que es obligatorio
        }
        List<DetalleCotizacion> detalles = new ArrayList<>();
        if (datos.lineas != null) {
            int numero = 1;
            for (LineaDatos l : datos.lineas) {
                DetalleCotizacion d = new DetalleCotizacion();
                // si Blazor no numera las lineas, se numeran en el orden en que llegan
                d.setNumeroLinea(l.numeroLinea > 0 ? l.numeroLinea : numero);
                Producto producto = new Producto();
                producto.setId(l.idProducto);
                d.setProducto(producto);
                d.setCantidad(l.cantidad);
                d.setPrecioUnitario(l.precioUnitario);
                d.setDescuento(l.descuento);
                detalles.add(d);
                numero++;
            }
        }
        cotizacion.setDetalles(detalles);
        return cotizacion;
    }

    static CotizacionDatos convertir(Cotizacion c, Nombres nombres, boolean conLineas) throws SQLException {
        CotizacionDatos d = new CotizacionDatos();
        d.id = c.getId();
        d.numero = c.getNumero();
        if (c.getCliente() != null) {
            d.idCliente = c.getCliente().getId();
            Cliente cliente = nombres.cliente(d.idCliente);
            if (cliente != null) {
                d.cliente = cliente.getNombreMostrado();
                d.documentoCliente = cliente.getNumeroDocumento();
            }
        }
        d.fechaEmision = Utilidades.fecha(c.getFechaEmision());
        d.fechaVigencia = Utilidades.fecha(c.getFechaVigencia());
        d.anulado = c.isAnulado();
        d.estado = c.isAnulado() ? EstadoCotizacion.ANULADA.name()
                : (c.getEstado() != null ? c.getEstado().name() : null);
        d.observaciones = c.getObservaciones();
        d.subTotal = c.getSubTotal();
        d.igv = c.getIgv();
        d.total = c.getTotal();
        d.usuarioRegistro = nombres.usuario(c.getUsuarioRegistro());
        d.fechaRegistro = Utilidades.fechaHora(c.getFechaRegistro());
        d.motivoAnulacion = c.getMotivoAnulacion();
        d.fechaAnulacion = Utilidades.fechaHora(c.getFechaAnulacion());
        if (conLineas && c.getDetalles() != null) {
            for (DetalleCotizacion det : c.getDetalles()) {
                d.lineas.add(convertirLinea(det.getNumeroLinea(), det.getProducto(), det.getCantidad(),
                        det.getPrecioUnitario(), det.getDescuento(), det.getImporte(), nombres));
            }
        }
        return d;
    }

    // comun para las lineas de cotizacion, orden y venta
    static LineaDatos convertirLinea(int numeroLinea, Producto producto, double cantidad, double precioUnitario,
                                     double descuento, double importe, Nombres nombres) throws SQLException {
        LineaDatos l = new LineaDatos();
        l.numeroLinea = numeroLinea;
        if (producto != null) {
            l.idProducto = producto.getId();
            Producto completo = nombres.producto(producto.getId());
            if (completo != null) {
                l.producto = completo.getNombre();
                l.codigoProducto = completo.getCodigoInterno();
            }
        }
        l.cantidad = cantidad;
        l.precioUnitario = precioUnitario;
        l.descuento = descuento;
        l.importe = importe;
        return l;
    }
}
