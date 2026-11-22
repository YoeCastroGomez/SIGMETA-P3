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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.almacen.AlmacenConsultaBO;
import pe.edu.pucp.sigmeta.bo.ventas.ComprobanteBO;
import pe.edu.pucp.sigmeta.boimpl.almacen.AlmacenConsultaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.ComprobanteBOImpl;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF013: servicio REST de notas de credito (las emite el Cajero con autorizacion; las consulta el Administrador).
//
//   GET  /api/notas-credito?desde=&hasta=&texto=     buscar por fechas y por cliente, documento o comprobante
//                                                    (cada fila trae "tipoNota": DEVOLUCION_TOTAL o DEVOLUCION_POR_ITEM)
//   GET  /api/notas-credito/9/lineas                 lineas de una nota
//   GET  /api/notas-credito/comprobantes             facturas y boletas vigentes con mercaderia entregada
//   GET  /api/notas-credito/comprobantes/12/lineas   lineas del comprobante: vendido, entregado, ya acreditado y devolvible
//   POST /api/notas-credito                          {"idComprobanteOrigen": 12, "motivo": "...",
//                                                     "lineas": [{"idLinea": 40, "cantidad": 2}]}
//   PUT  /api/notas-credito/9                        {"motivo": "..."}  modificar el motivo (nota no anulada)
//   POST /api/notas-credito/9/anular                 {"motivo": "..."}
//
// La autorizacion no se manda: el BO busca la solicitud EMITIR_NOTA_CREDITO aprobada y vigente del cajero de la
// sesion y la consume. Los importes los calcula el BO con el precio efectivamente cobrado.
@Path("notas-credito")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotaCreditoRS extends VentanillaBase {

    private final ComprobanteBO comprobanteBO = new ComprobanteBOImpl();
    private final AlmacenConsultaBO consultaBO = new AlmacenConsultaBOImpl();

    @GET
    public Response buscar(@HeaderParam("ticket") String ticket, @QueryParam("desde") String desde,
                           @QueryParam("hasta") String hasta, @QueryParam("texto") String texto) {
        return ejecutar(ticket, id -> {
            List<Map<String, Object>> filas = consultaBO.notasCredito(Utilidades.leerFecha(desde, "fecha de inicio"),
                    Utilidades.leerFecha(hasta, "fecha de fin"), texto, id);
            // RF013: tipo de nota (catalogo 09 SUNAT), comparando con el total del comprobante de origen
            Map<Integer, Double> totalesOrigen = new java.util.HashMap<>();
            for (Map<String, Object> fila : filas) {
                if (!(fila.get("idOrigen") instanceof Number idOrigen) || !(fila.get("total") instanceof Number total)) {
                    continue;
                }
                Double totalOrigen = totalesOrigen.get(idOrigen.intValue());
                if (totalOrigen == null) {
                    Comprobante origen = comprobanteBO.obtener(idOrigen.intValue());
                    totalOrigen = origen == null ? 0.0 : origen.getTotal();
                    totalesOrigen.put(idOrigen.intValue(), totalOrigen);
                }
                fila.put("tipoNota", ComprobanteBOImpl.tipoNotaCredito(total.doubleValue(), totalOrigen));
            }
            return filas;
        });
    }

    @GET
    @Path("{id}/lineas")
    public Response lineas(@HeaderParam("ticket") String ticket, @PathParam("id") int idNota) {
        return ejecutar(ticket, id -> consultaBO.detalleDeNota(idNota, id));
    }

    @GET
    @Path("comprobantes")
    public Response comprobantes(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> consultaBO.comprobantesParaNota(id));
    }

    @GET
    @Path("comprobantes/{id}/lineas")
    public Response lineasDevolvibles(@HeaderParam("ticket") String ticket, @PathParam("id") int idComprobante) {
        return ejecutar(ticket, id -> consultaBO.lineasDevolvibles(idComprobante, id));
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, NotaDatos datos) {
        if (datos == null || datos.idComprobanteOrigen <= 0) {
            return error(Response.Status.BAD_REQUEST, "Elija el comprobante sobre el que se emite la nota de crédito");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            Comprobante nota = aNota(datos);
            Comprobante guardada = comprobanteBO.registrarNotaCredito(nota, id);
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("id", guardada.getId());
            respuesta.put("nota", guardada.getSerie() + "-" + guardada.getNumero());
            respuesta.put("total", guardada.getTotal());
            return respuesta;
        });
    }

    // RF013/RF014: pide la autorizacion de ESTA nota (mismo cuerpo que registrar); no mueve stock, deuda ni caja
    @POST
    @Path("solicitar")
    public Response solicitar(@HeaderParam("ticket") String ticket, NotaDatos datos) {
        if (datos == null || datos.idComprobanteOrigen <= 0) {
            return error(Response.Status.BAD_REQUEST, "Elija el comprobante sobre el que se emite la nota de crédito");
        }
        // RF014: tambien se guarda lo que preparo el Cajero, para retomar la nota sin volver a pedir la autorizacion
        return ejecutar(ticket, Response.Status.CREATED, id ->
                pe.edu.pucp.sigmeta.ws.seguridad.SolicitudRS.resumen(comprobanteBO.solicitarNotaCredito(aNota(datos), id),
                        id, datos.datosOperacion));
    }

    private Comprobante aNota(NotaDatos datos) {
        {
            Comprobante nota = new Comprobante();
            Comprobante origen = new Comprobante();
            origen.setId(datos.idComprobanteOrigen);
            nota.setComprobanteRelacionado(origen);
            nota.setMotivo(datos.motivo);
            List<DetalleNotaCredito> detalles = new ArrayList<>();
            int numero = 1;
            for (LineaNota l : datos.lineas) {
                if (l == null || l.cantidad <= 0) {
                    continue; // una linea en 0 no se devuelve
                }
                DetalleNotaCredito d = new DetalleNotaCredito();
                DetalleVenta linea = new DetalleVenta();
                linea.setId(l.idLinea);
                d.setDetalleVenta(linea);
                d.setNumeroLinea(numero++);
                d.setCantidad(l.cantidad);
                detalles.add(d);
            }
            nota.setDetalles(detalles);
            return nota;
        }
    }

    // RF013: modificar la nota de credito (Cajero o Administrador). Solo se edita el motivo: el detalle y los
    // montos ya movieron stock, deuda y caja; si estan mal, se anula la nota y se emite otra.
    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket, @PathParam("id") int idNota, AnulacionDatos datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> {
            Comprobante cambios = new Comprobante();
            cambios.setId(idNota);
            cambios.setMotivo(motivo);
            Comprobante guardada = comprobanteBO.modificarNotaCredito(cambios, id);
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("id", guardada.getId());
            respuesta.put("nota", guardada.getSerie() + "-" + guardada.getNumero());
            respuesta.put("motivo", guardada.getMotivo());
            return respuesta;
        });
    }

    @POST
    @Path("{id}/anular")
    public Response anular(@HeaderParam("ticket") String ticket, @PathParam("id") int idNota, AnulacionDatos datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> {
            comprobanteBO.anular(idNota, motivo, id);
            return null; // 204 sin cuerpo
        });
    }

    public static class NotaDatos {
        public int idComprobanteOrigen;
        public String motivo;
        public List<LineaNota> lineas = new ArrayList<>();
        // RF014: solo en "solicitar": la pantalla tal como la preparo el Cajero (JSON), para retomarla
        public String datosOperacion;

        public NotaDatos() {
        }
    }

    // idLinea: id de la linea de venta que se devuelve
    public static class LineaNota {
        public int idLinea;
        public double cantidad;

        public LineaNota() {
        }
    }
}
