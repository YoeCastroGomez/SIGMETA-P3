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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.ComprobanteBO;
import pe.edu.pucp.sigmeta.bo.ventas.OrdenCompraClienteBO;
import pe.edu.pucp.sigmeta.bo.ventas.VentaBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.ComprobanteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.OrdenCompraClienteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.VentaBOImpl;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF008: servicio REST de ventas (registrar y modificar el Vendedor; anular el Vendedor o el Administrador;
// consultar el Vendedor, el Cajero y el Administrador).
//
//   GET    /api/ventas                lista de ventas, con su comprobante vigente
//   GET    /api/ventas?cliente=3      ventas de un cliente (listarPorCliente)
//   GET    /api/ventas/7              una venta con sus lineas
//   POST   /api/ventas                registrar (directa o desde una orden; contado o credito)
//   PUT    /api/ventas/7              modificar (solo REGISTRADA, sin comprobante ni despacho)
//   POST   /api/ventas/7/anular       anular con motivo: {"motivo": "..."}
//
// El responsable es siempre el usuario de la sesion (ticket). Stock, credito, correlativo, totales
// y la cuenta por cobrar de una venta al credito los maneja el BO.
@Path("ventas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VentaRS extends VentanillaBase {

    private final VentaBO ventaBO = new VentaBOImpl();
    private final OrdenCompraClienteBO ordenBO = new OrdenCompraClienteBOImpl();
    private final ComprobanteBO comprobanteBO = new ComprobanteBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket, @QueryParam("cliente") Integer idCliente) {
        return ejecutar(ticket, id -> {
            permitirConsulta(id);
            List<Venta> ventas = idCliente != null ? ventaBO.listarPorCliente(idCliente) : ventaBO.listarTodos();
            Map<Integer, Comprobante> comprobantes = comprobantesVigentes();
            Map<Integer, String> numerosOrden = numerosOrden();
            Nombres nombres = new Nombres();
            List<VentaDatos> lista = new ArrayList<>();
            for (Venta v : ventas) {
                lista.add(convertir(v, nombres, comprobantes, numerosOrden, false));
            }
            return lista;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idVenta) {
        Response respuesta = ejecutar(ticket, id -> {
            permitirConsulta(id);
            Venta venta = ventaBO.obtener(idVenta);
            return venta == null ? null : conDetalle(venta);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe la venta con id " + idVenta);
        }
        return respuesta;
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, VentaDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la venta");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            Venta registrada = ventaBO.registrar(aVenta(datos), id);
            return conDetalle(ventaBO.obtener(registrada.getId()));
        });
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket, @PathParam("id") int idVenta, VentaDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos de la venta");
        }
        return ejecutar(ticket, id -> {
            Venta venta = aVenta(datos);
            venta.setId(idVenta); // el id es el de la direccion, no el del cuerpo
            // el numero correlativo no se cambia al modificar: se toma de la venta guardada, no de lo que manda Blazor
            // (VentaBOImpl.modificar no lo copia y el SP de modificar lo exige)
            Venta actual = ventaBO.obtener(idVenta);
            if (actual == null) {
                throw new IllegalArgumentException("No existe la venta con id " + idVenta);
            }
            venta.setNumero(actual.getNumero());
            ventaBO.modificar(venta, id);
            return conDetalle(ventaBO.obtener(idVenta));
        });
    }

    @POST
    @Path("{id}/anular")
    public Response anular(@HeaderParam("ticket") String ticket, @PathParam("id") int idVenta, AnulacionDatos datos) {
        String motivo = datos == null ? null : datos.motivo;
        return ejecutar(ticket, id -> {
            ventaBO.anular(idVenta, motivo, id);
            return null; // 204 sin cuerpo
        });
    }

    // ----- conversiones -----

    // RF001: las ventas las consultan el Vendedor, el Cajero (para cobrar y emitir) y el Administrador
    private void permitirConsulta(int idUsuario) throws SQLException {
        usuarioBO.verificarRol(idUsuario, TipoRol.VENDEDOR, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
    }

    private Venta aVenta(VentaDatos datos) {
        Venta venta = new Venta();
        venta.setFechaEmision(Utilidades.leerFecha(datos.fechaEmision, "fecha de emision"));
        venta.setCondicionPago(Utilidades.leerEnum(CondicionPago.class, datos.condicionPago, "condicion de pago"));
        venta.setPlazoCreditoDias(datos.plazoCreditoDias);
        venta.setObservaciones(datos.observaciones);
        if (datos.idCliente > 0) {
            Cliente cliente = new Cliente();
            cliente.setId(datos.idCliente);
            venta.setCliente(cliente); // sin cliente, el BO responde que es obligatorio
        }
        if (datos.idOrden > 0) {
            OrdenCompraCliente orden = new OrdenCompraCliente();
            orden.setId(datos.idOrden);
            venta.setOrdenCompraCliente(orden);
        }
        List<DetalleVenta> detalles = new ArrayList<>();
        if (datos.lineas != null) {
            int numero = 1;
            for (LineaDatos l : datos.lineas) {
                DetalleVenta d = new DetalleVenta();
                d.setNumeroLinea(l.numeroLinea > 0 ? l.numeroLinea : numero);
                Producto producto = new Producto();
                producto.setId(l.idProducto);
                d.setProducto(producto);
                d.setCantidad(l.cantidad);
                d.setPrecioUnitario(l.precioUnitario);
                d.setDescuento(l.descuento);
                if (l.idDetalleOrden > 0) {
                    DetalleOrdenCompraCliente lineaOrden = new DetalleOrdenCompraCliente();
                    lineaOrden.setId(l.idDetalleOrden);
                    d.setDetalleOrdenCompraCliente(lineaOrden);
                }
                detalles.add(d);
                numero++;
            }
        }
        venta.setDetalles(detalles);
        return venta;
    }

    private VentaDatos conDetalle(Venta venta) throws SQLException {
        return convertir(venta, new Nombres(), comprobantesVigentes(), numerosOrden(), true);
    }

    // boleta o factura vigente de cada venta (una venta tiene un solo comprobante vigente)
    private Map<Integer, Comprobante> comprobantesVigentes() throws SQLException {
        Map<Integer, Comprobante> mapa = new HashMap<>();
        for (Comprobante c : comprobanteBO.listarTodos()) {
            if (c.getVenta() != null && c.getEstado() != EstadoComprobante.ANULADO
                    && (c.getTipo() == TipoComprobante.FACTURA || c.getTipo() == TipoComprobante.BOLETA)) {
                mapa.put(c.getVenta().getId(), c);
            }
        }
        return mapa;
    }

    private Map<Integer, String> numerosOrden() throws SQLException {
        Map<Integer, String> mapa = new HashMap<>();
        for (OrdenCompraCliente oc : ordenBO.listarTodos()) {
            mapa.put(oc.getId(), oc.getNumero());
        }
        return mapa;
    }

    static VentaDatos convertir(Venta v, Nombres nombres, Map<Integer, Comprobante> comprobantes,
                                Map<Integer, String> numerosOrden, boolean conLineas) throws SQLException {
        VentaDatos d = new VentaDatos();
        d.id = v.getId();
        d.numero = v.getNumero();
        if (v.getCliente() != null) {
            d.idCliente = v.getCliente().getId();
            Cliente cliente = nombres.cliente(d.idCliente);
            if (cliente != null) {
                d.cliente = cliente.getNombreMostrado();
                d.documentoCliente = cliente.getNumeroDocumento();
            }
        }
        if (v.getOrdenCompraCliente() != null) {
            d.idOrden = v.getOrdenCompraCliente().getId();
            d.numeroOrden = numerosOrden.get(d.idOrden);
        }
        d.fechaEmision = Utilidades.fecha(v.getFechaEmision());
        d.condicionPago = v.getCondicionPago() != null ? v.getCondicionPago().name() : null;
        d.plazoCreditoDias = v.getPlazoCreditoDias();
        d.anulado = v.isAnulado();
        d.estado = v.isAnulado() ? EstadoVenta.ANULADA.name() : (v.getEstado() != null ? v.getEstado().name() : null);
        d.observaciones = v.getObservaciones();
        d.subTotal = v.getSubTotal();
        d.igv = v.getIgv();
        d.total = v.getTotal();
        d.usuarioRegistro = nombres.usuario(v.getUsuarioRegistro());
        d.fechaRegistro = Utilidades.fechaHora(v.getFechaRegistro());
        d.motivoAnulacion = v.getMotivoAnulacion();
        d.fechaAnulacion = Utilidades.fechaHora(v.getFechaAnulacion());
        Comprobante c = comprobantes.get(v.getId());
        if (c != null) {
            d.idComprobante = c.getId();
            d.tipoComprobante = c.getTipo().name();
            d.comprobante = c.getSerie() + "-" + c.getNumero();
        }
        if (conLineas && v.getDetalles() != null) {
            for (DetalleVenta det : v.getDetalles()) {
                LineaDatos l = CotizacionRS.convertirLinea(det.getNumeroLinea(), det.getProducto(), det.getCantidad(),
                        det.getPrecioUnitario(), det.getDescuento(), det.getImporte(), nombres);
                l.idLinea = det.getId();
                l.cantidadDespachada = det.getCantidadDespachada();
                if (det.getDetalleOrdenCompraCliente() != null) {
                    l.idDetalleOrden = det.getDetalleOrdenCompraCliente().getId();
                }
                d.lineas.add(l);
            }
        }
        return d;
    }
}
