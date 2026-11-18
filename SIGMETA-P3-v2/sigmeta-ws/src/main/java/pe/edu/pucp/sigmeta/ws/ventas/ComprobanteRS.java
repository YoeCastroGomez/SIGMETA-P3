package pe.edu.pucp.sigmeta.ws.ventas;

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
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.ComprobanteBO;
import pe.edu.pucp.sigmeta.bo.ventas.VentaBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.ComprobanteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.VentaBOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF008: servicio REST de comprobantes electronicos (Vendedor o Cajero).
//
//   GET    /api/comprobantes?venta=2      boletas y facturas de una venta (vigentes y anuladas)
//   GET    /api/comprobantes/5            un comprobante con el cliente y las lineas de su venta
//   GET    /api/comprobantes/5/pdf        su representacion impresa en PDF (JasperReports), para descargar
//   POST   /api/comprobantes              emitir: {"idVenta": 2, "tipo": "BOLETA"} (la serie y el numero los pone el BO)
//   POST   /api/comprobantes/5/envio      registrar el envio: {"medioEnvio": "CORREO" | "MENSAJERIA" | "IMPRESO"}
//                                         (el BO guarda la fecha; IMPRESO lo manda el boton Imprimir de Blazor)
//   POST   /api/comprobantes/5/anular     anular la factura o boleta: {"motivo": "..."} (solo sin despachos vigentes;
//                                         despues la venta se puede corregir, volver a facturar o anular)
//
// Las reglas (factura solo con RUC, un solo comprobante vigente por venta, venta anulada...) son del BO.
// Las notas de credito (RF013) estan en NotaCreditoRS.
@Path("comprobantes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ComprobanteRS extends VentanillaBase {

    private final ComprobanteBO comprobanteBO = new ComprobanteBOImpl();
    private final VentaBO ventaBO = new VentaBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket, @QueryParam("venta") Integer idVenta) {
        if (idVenta == null) {
            return error(Response.Status.BAD_REQUEST, "Indique la venta: /api/comprobantes?venta=ID");
        }
        return ejecutar(ticket, id -> {
            permitirConsulta(id);
            Venta venta = ventaBO.obtener(idVenta);
            if (venta == null) {
                throw new IllegalArgumentException("No existe la venta con id " + idVenta);
            }
            Nombres nombres = new Nombres();
            List<ComprobanteDatos> lista = new ArrayList<>();
            for (Comprobante c : comprobanteBO.listarTodos()) {
                if (c.getVenta() != null && c.getVenta().getId() == idVenta
                        && (c.getTipo() == TipoComprobante.FACTURA || c.getTipo() == TipoComprobante.BOLETA)) {
                    lista.add(convertir(c, venta, nombres, false));
                }
            }
            return lista;
        });
    }

    @GET
    @Path("siguiente-numero")
    public Response siguienteNumero(@HeaderParam("ticket") String ticket, @QueryParam("serie") String serie) {
        return ejecutar(ticket, id -> {
            permitirConsulta(id);
            if (!"F001".equals(serie) && !"B001".equals(serie)) {
                throw new IllegalArgumentException("Serie de comprobante inválida");
            }
            long mayor = 0;
            for (Comprobante c : comprobanteBO.listarTodos()) {
                if (!serie.equals(c.getSerie()) || c.getNumero() == null || !c.getNumero().matches("[0-9]+")) {
                    continue;
                }
                mayor = Math.max(mayor, Long.parseLong(c.getNumero()));
            }
            return java.util.Map.of("numero", String.format(java.util.Locale.ROOT, "%08d", mayor + 1));
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idComprobante) {
        Response respuesta = ejecutar(ticket, id -> {
            permitirConsulta(id);
            Comprobante c = comprobanteBO.obtener(idComprobante);
            return c == null ? null : completo(c);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe el comprobante con id " + idComprobante);
        }
        return respuesta;
    }

    @POST
    public Response emitir(@HeaderParam("ticket") String ticket, ComprobanteDatos datos) {
        if (datos == null || datos.idVenta <= 0) {
            return error(Response.Status.BAD_REQUEST, "Indique la venta del comprobante");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            TipoComprobante tipo = Utilidades.leerEnum(TipoComprobante.class, datos.tipo, "tipo de comprobante");
            Comprobante emitido = comprobanteBO.emitir(datos.idVenta, tipo, id);
            return completo(comprobanteBO.obtener(emitido.getId()));
        });
    }

    @POST
    @Path("{id}/envio")
    public Response registrarEnvio(@HeaderParam("ticket") String ticket, @PathParam("id") int idComprobante,
                                   ComprobanteDatos datos) {
        String medio = datos == null ? null : datos.medioEnvio;
        return ejecutar(ticket, id -> {
            comprobanteBO.registrarEnvio(idComprobante, medio, id);
            return completo(comprobanteBO.obtener(idComprobante));
        });
    }

    // RF008: anular la factura o boleta (Vendedor, Cajero o Administrador; las reglas son del BO)
    @POST
    @Path("{id}/anular")
    public Response anular(@HeaderParam("ticket") String ticket, @PathParam("id") int idComprobante, AnulacionDatos datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> {
            Comprobante c = comprobanteBO.obtener(idComprobante);
            if (c != null && c.getTipo() == TipoComprobante.NOTA_CREDITO) {
                throw new IllegalArgumentException("Las notas de crédito se anulan desde /api/notas-credito");
            }
            comprobanteBO.anular(idComprobante, motivo, id);
            return null; // 204 sin cuerpo
        });
    }

    // RF008: el comprobante en PDF (JasperReports). Responde application/pdf con Content-Disposition para que se
    // descargue con su nombre (por ejemplo F001-00000074.pdf). Si algo falla responde JSON con el mensaje.
    @GET
    @Path("{id}/pdf")
    @Produces({"application/pdf", MediaType.APPLICATION_JSON})
    public Response pdf(@HeaderParam("ticket") String ticket, @PathParam("id") int idComprobante) {
        Response respuesta = ejecutar(ticket, id -> comprobanteBO.pdf(idComprobante, id));
        if (respuesta.getStatus() != Response.Status.OK.getStatusCode()) {
            return Response.fromResponse(respuesta).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        return Response.fromResponse(respuesta)
                .type("application/pdf")
                .header("Content-Disposition", "attachment; filename=\"comprobante-" + idComprobante + ".pdf\"")
                .header("Cache-Control", "no-store")
                .build();
    }

    // ----- conversiones -----

    // RF001: los comprobantes los manejan el Vendedor y el Cajero; el Administrador puede consultarlos
    private void permitirConsulta(int idUsuario) throws SQLException {
        usuarioBO.verificarRol(idUsuario, TipoRol.VENDEDOR, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
    }

    private ComprobanteDatos completo(Comprobante c) throws SQLException {
        Venta venta = c.getVenta() != null ? ventaBO.obtener(c.getVenta().getId()) : null;
        return convertir(c, venta, new Nombres(), true);
    }

    static ComprobanteDatos convertir(Comprobante c, Venta venta, Nombres nombres, boolean conLineas) throws SQLException {
        ComprobanteDatos d = new ComprobanteDatos();
        d.id = c.getId();
        d.tipo = c.getTipo() != null ? c.getTipo().name() : null;
        d.serie = c.getSerie();
        d.numero = c.getNumero();
        d.comprobante = c.getSerie() + "-" + c.getNumero();
        d.fechaEmision = Utilidades.fecha(c.getFechaEmision());
        d.estado = c.getEstado() != null ? c.getEstado().name() : null;
        d.subTotal = c.getSubTotal();
        d.igv = c.getIgv();
        d.total = c.getTotal();
        d.medioEnvio = c.getMedioEnvio();
        d.fechaEnvio = Utilidades.fechaHora(c.getFechaEnvio());
        d.fechaRegistro = Utilidades.fechaHora(c.getFechaRegistro());
        if (venta != null) {
            d.idVenta = venta.getId();
            d.numeroVenta = venta.getNumero();
            d.condicionPago = venta.getCondicionPago() != null ? venta.getCondicionPago().name() : null;
            if (venta.getCliente() != null) {
                d.idCliente = venta.getCliente().getId();
                Cliente cliente = nombres.cliente(d.idCliente);
                if (cliente != null) {
                    d.cliente = cliente.getNombreMostrado();
                    d.tipoDocumentoCliente = cliente.getTipoDocumento() != null ? cliente.getTipoDocumento().name() : null;
                    d.documentoCliente = cliente.getNumeroDocumento();
                    if (cliente.getContacto() != null) {
                        d.direccionCliente = cliente.getContacto().getDireccion();
                        d.correoCliente = cliente.getContacto().getCorreo();
                    }
                }
            }
            if (conLineas && venta.getDetalles() != null) {
                for (DetalleVenta det : venta.getDetalles()) {
                    d.lineas.add(CotizacionRS.convertirLinea(det.getNumeroLinea(), det.getProducto(), det.getCantidad(),
                            det.getPrecioUnitario(), det.getDescuento(), det.getImporte(), nombres));
                }
            }
        } else if (c.getVenta() != null) {
            d.idVenta = c.getVenta().getId();
        }
        return d;
    }
}
