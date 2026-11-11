package pe.edu.pucp.sigmeta.ws.compras;

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
import pe.edu.pucp.sigmeta.bo.compras.CompraBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.compras.CompraBOImpl;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.enums.UnidadMedida;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// RF011 - Gestion de compras a proveedores. El usuario se obtiene del ticket,
// nunca del cuerpo de la peticion. Se usan DTO para evitar ciclos Compra/DetalleCompra.
@Path("compras")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CompraRS extends VentanillaBase {
    private final CompraBO compraBO = new CompraBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("idProveedor") Integer idProveedor,
                           @QueryParam("fechaInicio") String fechaInicio,
                           @QueryParam("fechaFin") String fechaFin) {
        return ejecutar(ticket, id -> {
            usuarioBO.verificarRol(id, TipoRol.ADMINISTRADOR);
            List<Compra> compras = compraBO.buscar(idProveedor,
                    fechaInicio == null || fechaInicio.isBlank() ? null : fecha(fechaInicio),
                    fechaFin == null || fechaFin.isBlank() ? null : fecha(fechaFin));
            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Compra c : compras) resultado.add(respuesta(c));
            return resultado;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket,
                            @PathParam("id") int idCompra) {
        Response respuesta = ejecutar(ticket, id -> {
            usuarioBO.verificarRol(id, TipoRol.ADMINISTRADOR);
            Compra compra = compraBO.obtener(idCompra);
            return compra == null ? null : respuesta(compra);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe la compra con id " + idCompra);
        }
        return respuesta;
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, Solicitud compra) {
        if (compra == null) return error(Response.Status.BAD_REQUEST, "Faltan los datos de la compra");
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            usuarioBO.verificarRol(id, TipoRol.ADMINISTRADOR);
            Compra nueva = desdeSolicitud(compra, id);
            nueva.setId(0);
            nueva.setEstado(EstadoCompra.REGISTRADA);
            nueva.setAnulado(false);
            return respuesta(compraBO.registrar(nueva));
        });
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket,
                              @PathParam("id") int idCompra, Solicitud solicitud) {
        if (solicitud == null) return error(Response.Status.BAD_REQUEST, "Faltan los datos de la compra");
        return ejecutar(ticket, id -> {
            usuarioBO.verificarRol(id, TipoRol.ADMINISTRADOR);
            Compra nueva = desdeSolicitud(solicitud, id);
            nueva.setId(idCompra);
            nueva.setEstado(EstadoCompra.REGISTRADA);
            nueva.setAnulado(false);
            return respuesta(compraBO.modificar(nueva, id));
        });
    }

    @DELETE
    @Path("{id}")
    public Response anular(@HeaderParam("ticket") String ticket,
                           @PathParam("id") int idCompra) {
        return ejecutar(ticket, id -> {
            compraBO.anular(idCompra, id);
            return null;
        });
    }

    // Fechas como texto ISO (AAAA-MM-DD) para simplificar las peticiones Blazor.
    private LocalDate fecha(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Fecha invalida, utilice AAAA-MM-DD");
        }
    }

    private Compra desdeSolicitud(Solicitud s, int idUsuario) {
        Compra compra = new Compra();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(s.idProveedor);
        compra.setProveedor(proveedor);
        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        compra.setUsuarioRegistro(usuario);
        compra.setNumero(s.numero);
        compra.setFechaEmision(fecha(s.fechaEmision));
        compra.setFechaRecepcionEstimada(s.fechaRecepcionEstimada == null ||
                s.fechaRecepcionEstimada.isBlank() ? null : fecha(s.fechaRecepcionEstimada));
        compra.setObservaciones(s.observaciones);
        List<DetalleCompra> lineas = new ArrayList<>();
        if (s.detalles != null) {
            for (Linea linea : s.detalles) {
                if (linea == null) throw new IllegalArgumentException("Detalle de compra invalido");
                DetalleCompra detalle = new DetalleCompra();
                detalle.setNumeroLinea(linea.numeroLinea);
                Producto producto = new Producto();
                producto.setId(linea.idProducto);
                detalle.setProducto(producto);
                detalle.setCantidad(linea.cantidad);
                detalle.setPrecioUnitario(linea.precioUnitario);
                detalle.setDescuento(linea.descuento);
                detalle.setFactorConversion(linea.factorConversion);
                try {
                    detalle.setUnidadCompra(UnidadMedida.valueOf(linea.unidadCompra));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Unidad de compra invalida");
                }
                lineas.add(detalle);
            }
        }
        compra.setDetalles(lineas);
        return compra;
    }

    private Map<String, Object> respuesta(Compra c) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", c.getId());
        dto.put("numero", c.getNumero());
        dto.put("idProveedor", c.getProveedor() == null ? null : c.getProveedor().getId());
        dto.put("idUsuarioRegistro", c.getUsuarioRegistro() == null ? null : c.getUsuarioRegistro().getId());
        dto.put("fechaEmision", c.getFechaEmision() == null ? null : c.getFechaEmision().toString());
        dto.put("fechaRecepcionEstimada", c.getFechaRecepcionEstimada() == null ? null : c.getFechaRecepcionEstimada().toString());
        dto.put("fechaRegistro", c.getFechaRegistro() == null ? null : c.getFechaRegistro().toString());
        dto.put("estado", c.getEstado() == null ? null : c.getEstado().name());
        dto.put("subTotal", c.getSubTotal());
        dto.put("igv", c.getIgv());
        dto.put("total", c.getTotal());
        dto.put("observaciones", c.getObservaciones());
        dto.put("anulado", c.isAnulado());
        dto.put("motivoAnulacion", c.getMotivoAnulacion());
        List<Map<String, Object>> lineas = new ArrayList<>();
        if (c.getDetalles() != null) {
            for (DetalleCompra d : c.getDetalles()) {
                Map<String, Object> linea = new LinkedHashMap<>();
                linea.put("id", d.getId());
                linea.put("numeroLinea", d.getNumeroLinea());
                linea.put("idProducto", d.getProducto() == null ? null : d.getProducto().getId());
                linea.put("cantidad", d.getCantidad());
                linea.put("precioUnitario", d.getPrecioUnitario());
                linea.put("descuento", d.getDescuento());
                linea.put("importe", d.getImporte());
                linea.put("unidadCompra", d.getUnidadCompra() == null ? null : d.getUnidadCompra().name());
                linea.put("factorConversion", d.getFactorConversion());
                linea.put("cantidadRecibida", d.getCantidadRecibida());
                lineas.add(linea);
            }
        }
        dto.put("detalles", lineas);
        return dto;
    }

    // Clases de entrada JSON. No reciben estado, totales, usuario ni ID:
    // todos esos datos los controla la capa de negocio.
    public static class Solicitud {
        public int idProveedor;
        public String numero;
        public String fechaEmision;
        public String fechaRecepcionEstimada;
        public String observaciones;
        public List<Linea> detalles;
    }

    public static class Linea {
        public int numeroLinea;
        public int idProducto;
        public double cantidad;
        public double precioUnitario;
        public double descuento;
        public String unidadCompra;
        public double factorConversion;
    }
}
