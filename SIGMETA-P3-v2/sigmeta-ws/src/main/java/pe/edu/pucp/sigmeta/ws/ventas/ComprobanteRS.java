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
//   POST   /api/comprobantes              emitir: {"idVenta": 2, "tipo": "BOLETA"} (la serie y el numero los pone el BO)
//   POST   /api/comprobantes/5/envio      registrar el envio: {"medioEnvio": "CORREO"} (el BO guarda la fecha)
//
// Las reglas (factura solo con RUC, un solo comprobante vigente por venta, venta anulada...) son del BO.
// Las notas de credito (RF013) no se presentan en este laboratorio.
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
