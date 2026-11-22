package pe.edu.pucp.sigmeta.ws.almacen;

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
import java.util.LinkedHashMap;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.almacen.AlmacenConsultaBO;
import pe.edu.pucp.sigmeta.bo.almacen.DespachoBO;
import pe.edu.pucp.sigmeta.bo.almacen.MovimientoInventarioBO;
import pe.edu.pucp.sigmeta.bo.almacen.RecepcionCompraBO;
import pe.edu.pucp.sigmeta.boimpl.almacen.AlmacenConsultaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.almacen.DespachoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.almacen.MovimientoInventarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.almacen.RecepcionCompraBOImpl;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF012: servicio REST de almacen: recepcion de compras, despacho de ventas, kardex y ajuste por conteo.
//
// Recepciones (Almacenero; consulta el Administrador)
//   GET  /api/almacen/compras-por-recibir            compras que esperan mercaderia
//   GET  /api/almacen/compras/8/lineas               lineas de la compra: pedido, recibido y pendiente
//   GET  /api/almacen/recepciones?compra=8           recepciones (todas, o las de una compra)
//   GET  /api/almacen/recepciones/3/lineas           detalle de una recepcion
//   POST /api/almacen/recepciones                    {"idCompra": 8, "fecha": "2026-11-10", "observaciones": "...",
//                                                     "lineas": [{"idLinea": 21, "cantidad": 2}]}
//   PUT  /api/almacen/recepciones/3                  {"observaciones": "..."}
//   POST /api/almacen/recepciones/3/revertir         {"motivo": "..."}
// Despachos (Almacenero; anular tambien el Administrador)
//   GET  /api/almacen/ventas-por-despachar           ventas con mercaderia pendiente de entrega
//   GET  /api/almacen/ventas/15/lineas               lineas de la venta: vendido, despachado y pendiente
//   GET  /api/almacen/despachos?venta=15             despachos (todos, o los de una venta)
//   GET  /api/almacen/despachos/4/lineas             detalle de un despacho
//   POST /api/almacen/despachos                      {"idVenta": 15, "fecha": "...", "direccionEntrega": "...",
//                                                     "transportista": "...", "lineas": [{"idLinea": 40, "cantidad": 5}]}
//   POST /api/almacen/despachos/4/anular             {"motivo": "..."}
// Kardex y ajustes
//   GET  /api/almacen/kardex?producto=3&desde=&hasta=&tipo=   movimientos (los cuatro roles)
//   GET  /api/almacen/existencias                    stock fisico, comprometido y disponible por producto
//   POST /api/almacen/ajustes                        {"idProducto": 3, "cantidadContada": 48, "motivo": "..."} (con autorizacion)
//   POST /api/almacen/ajustes/90/revertir            {"motivo": "..."} (con autorizacion)
//   PUT  /api/almacen/movimientos/90/motivo          {"motivo": "..."} (Administrador)
//
// El responsable es siempre el usuario de la sesion (ticket). Las reglas y sus mensajes son de los BO.
@Path("almacen")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlmacenRS extends VentanillaBase {

    private final AlmacenConsultaBO consultaBO = new AlmacenConsultaBOImpl();
    private final RecepcionCompraBO recepcionBO = new RecepcionCompraBOImpl();
    private final DespachoBO despachoBO = new DespachoBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();
    private final MovimientoInventarioBO movimientoBO = new MovimientoInventarioBOImpl();

    // ----- recepciones -----

    @GET
    @Path("compras-por-recibir")
    public Response comprasPorRecibir(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> consultaBO.comprasPorRecibir(id));
    }

    @GET
    @Path("compras/{id}/lineas")
    public Response lineasDeCompra(@HeaderParam("ticket") String ticket, @PathParam("id") int idCompra) {
        return ejecutar(ticket, id -> consultaBO.lineasDeCompra(idCompra, id));
    }

    @GET
    @Path("recepciones")
    public Response recepciones(@HeaderParam("ticket") String ticket, @QueryParam("compra") int idCompra) {
        return ejecutar(ticket, id -> consultaBO.recepciones(idCompra, id));
    }

    @GET
    @Path("recepciones/{id}/lineas")
    public Response detalleDeRecepcion(@HeaderParam("ticket") String ticket, @PathParam("id") int idRecepcion) {
        return ejecutar(ticket, id -> consultaBO.detalleDeRecepcion(idRecepcion, id));
    }

    @POST
    @Path("recepciones")
    public Response registrarRecepcion(@HeaderParam("ticket") String ticket, DocumentoAlmacen datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la recepción");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            RecepcionCompra recepcion = AlmacenConversion.aRecepcion(datos, id);
            return AlmacenConversion.resumen(recepcionBO.registrar(recepcion).getId());
        });
    }

    @PUT
    @Path("recepciones/{id}")
    public Response modificarRecepcion(@HeaderParam("ticket") String ticket, @PathParam("id") int idRecepcion,
                                       DocumentoAlmacen datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la recepción");
        }
        return ejecutar(ticket, id -> {
            RecepcionCompra actual = recepcionBO.obtener(idRecepcion);
            if (actual == null) {
                throw new IllegalArgumentException("No existe la recepción con ID " + idRecepcion);
            }
            // solo se corrigen las observaciones: el BO rechaza cualquier otro cambio
            actual.setObservaciones(datos.observaciones);
            recepcionBO.modificar(actual, id);
            return AlmacenConversion.resumen(idRecepcion);
        });
    }

    @POST
    @Path("recepciones/{id}/revertir")
    public Response revertirRecepcion(@HeaderParam("ticket") String ticket, @PathParam("id") int idRecepcion,
                                      DocumentoAlmacen datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> {
            movimientoBO.revertirRecepcionCompra(idRecepcion, id, motivo);
            return null; // 204 sin cuerpo
        });
    }

    // ----- despachos -----

    @GET
    @Path("ventas-por-despachar")
    public Response ventasPorDespachar(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> consultaBO.ventasPorDespachar(id));
    }

    @GET
    @Path("ventas/{id}/lineas")
    public Response lineasDeVenta(@HeaderParam("ticket") String ticket, @PathParam("id") int idVenta) {
        return ejecutar(ticket, id -> consultaBO.lineasDeVenta(idVenta, id));
    }

    @GET
    @Path("despachos")
    public Response despachos(@HeaderParam("ticket") String ticket, @QueryParam("venta") int idVenta) {
        return ejecutar(ticket, id -> consultaBO.despachos(idVenta, id));
    }

    @GET
    @Path("despachos/siguiente-guia")
    public Response siguienteGuia(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> {
            usuarioBO.verificarRol(id, TipoRol.ALMACENERO, TipoRol.ADMINISTRADOR);
            long mayor = 0;
            for (Despacho despacho : despachoBO.listarTodos()) {
                String numero = despacho.getNumeroGuia();
                if (!"T001".equals(despacho.getSerieGuia()) || numero == null || !numero.matches("[0-9]+")) continue;
                mayor = Math.max(mayor, Long.parseLong(numero));
            }
            return Map.of("guia", "T001-" + String.format(java.util.Locale.ROOT, "%08d", mayor + 1));
        });
    }

    @GET
    @Path("despachos/{id}/lineas")
    public Response detalleDeDespacho(@HeaderParam("ticket") String ticket, @PathParam("id") int idDespacho) {
        return ejecutar(ticket, id -> consultaBO.detalleDeDespacho(idDespacho, id));
    }

    @POST
    @Path("despachos")
    public Response registrarDespacho(@HeaderParam("ticket") String ticket, DocumentoAlmacen datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del despacho");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            Despacho despacho = AlmacenConversion.aDespacho(datos);
            Despacho guardado = despachoBO.registrar(despacho, id);
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("id", guardado.getId());
            respuesta.put("guia", guardado.getSerieGuia() + "-" + guardado.getNumeroGuia());
            return respuesta;
        });
    }

    @POST
    @Path("despachos/{id}/anular")
    public Response anularDespacho(@HeaderParam("ticket") String ticket, @PathParam("id") int idDespacho,
                                   DocumentoAlmacen datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> {
            despachoBO.anular(idDespacho, motivo, id);
            return null;
        });
    }

    // ----- kardex y ajustes -----

    @GET
    @Path("kardex")
    public Response kardex(@HeaderParam("ticket") String ticket, @QueryParam("producto") int idProducto,
                           @QueryParam("desde") String desde, @QueryParam("hasta") String hasta,
                           @QueryParam("tipo") String tipo) {
        return ejecutar(ticket, id -> consultaBO.kardex(idProducto,
                Utilidades.leerFecha(desde, "fecha de inicio"), Utilidades.leerFecha(hasta, "fecha de fin"), tipo, id));
    }

    @GET
    @Path("existencias")
    public Response existencias(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> consultaBO.existencias(id));
    }

    @POST
    @Path("ajustes")
    public Response registrarAjuste(@HeaderParam("ticket") String ticket, DocumentoAlmacen datos) {
        if (datos == null || datos.cantidadContada == null) {
            return error(Response.Status.BAD_REQUEST, "Indique el producto y la cantidad contada");
        }
        return ejecutar(ticket, Response.Status.CREATED, id ->
                AlmacenConversion.movimiento(movimientoBO.registrarAjuste(datos.idProducto, id, datos.cantidadContada, datos.motivo)));
    }

    // RF012/RF014: pide la autorizacion de ESTE ajuste (mismo cuerpo que registrarlo); no mueve el stock
    @POST
    @Path("ajustes/solicitar")
    public Response solicitarAjuste(@HeaderParam("ticket") String ticket, DocumentoAlmacen datos) {
        if (datos == null || datos.cantidadContada == null) {
            return error(Response.Status.BAD_REQUEST, "Indique el producto y la cantidad contada");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> pe.edu.pucp.sigmeta.ws.seguridad.SolicitudRS.resumen(
                movimientoBO.solicitarAjuste(datos.idProducto, id, datos.cantidadContada, datos.motivo),
                id, datos.datosOperacion));
    }

    // RF012/RF014: pide la autorizacion para revertir ESTE ajuste
    @POST
    @Path("ajustes/{id}/revertir/solicitar")
    public Response solicitarReversionAjuste(@HeaderParam("ticket") String ticket, @PathParam("id") int idMovimiento,
                                             DocumentoAlmacen datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, Response.Status.CREATED, id -> pe.edu.pucp.sigmeta.ws.seguridad.SolicitudRS.resumen(
                movimientoBO.solicitarReversionAjuste(idMovimiento, id, motivo)));
    }

    @POST
    @Path("ajustes/{id}/revertir")
    public Response revertirAjuste(@HeaderParam("ticket") String ticket, @PathParam("id") int idMovimiento,
                                   DocumentoAlmacen datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> AlmacenConversion.movimiento(movimientoBO.revertirAjuste(idMovimiento, id, motivo)));
    }

    @PUT
    @Path("movimientos/{id}/motivo")
    public Response modificarMotivo(@HeaderParam("ticket") String ticket, @PathParam("id") int idMovimiento,
                                    DocumentoAlmacen datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> AlmacenConversion.movimiento(movimientoBO.modificarMotivo(idMovimiento, motivo, id)));
    }
}
