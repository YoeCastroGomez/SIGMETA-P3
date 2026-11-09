package pe.edu.pucp.sigmeta.ws.catalogo;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.edu.pucp.sigmeta.bo.producto.ProductoBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.producto.ProductoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Consulta de productos para seleccionar articulos en las compras (RF011).
@Path("productos")
@Produces(MediaType.APPLICATION_JSON)
public class ProductoConsultaRS extends VentanillaBase {
    private final ProductoBO productoBO = new ProductoBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("incluirInactivos") boolean incluirInactivos) {
        return ejecutar(ticket, idUsuario -> {
            usuarioBO.obtenerAdministradorActivo(idUsuario);
            List<Producto> productos = new ArrayList<>(productoBO.listarTodos());
            if (incluirInactivos) {
                productos.addAll(productoBO.listarPorEstado(false));
            }
            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Producto producto : productos) {
                resultado.add(dto(producto));
            }
            return resultado;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket,
                            @PathParam("id") int idProducto) {
        Response respuesta = ejecutar(ticket, idUsuario -> {
            usuarioBO.obtenerAdministradorActivo(idUsuario);
            Producto producto = productoBO.obtener(idProducto);
            return producto == null ? null : dto(producto);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe el producto con id " + idProducto);
        }
        return respuesta;
    }

    private Map<String, Object> dto(Producto producto) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", producto.getId());
        datos.put("codigoInterno", producto.getCodigoInterno());
        datos.put("nombre", producto.getNombre());
        datos.put("unidadCompra", producto.getUnidadCompra() == null ? null : producto.getUnidadCompra().name());
        datos.put("factorConversion", producto.getFactorConversion());
        datos.put("costoUnitario", producto.getCostoUnitario());
        datos.put("estado", producto.isEstado());
        return datos;
    }
}
