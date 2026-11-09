package pe.edu.pucp.sigmeta.ws.producto;

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
import pe.edu.pucp.sigmeta.bo.producto.CategoriaBO;
import pe.edu.pucp.sigmeta.bo.producto.ProductoBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.producto.CategoriaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.producto.ProductoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RF005: servicio REST de productos.
//
//   GET    /api/productos                    listar todos (activos e inactivos)
//   GET    /api/productos?buscar=INT-001     buscador rapido (lector de codigo de barras o teclado):
//                                            primero busca el codigo exacto (buscarPorCodigo); si no hay,
//                                            busca por parte del nombre (buscarPorNombre). Solo activos.
//   GET    /api/productos?codigo=INT-001     buscar por codigo interno, de fabricante o de proveedor
//   GET    /api/productos?nombre=martillo    buscar por nombre (coincidencia parcial, activos)
//   GET    /api/productos?categoria=3        listar por categoria (activos)
//   GET    /api/productos?estado=true        listar por estado (true activos, false inactivos)
//   GET    /api/productos/7                  obtener uno por id
//   POST   /api/productos                    registrar (solo Administrador)
//   PUT    /api/productos/7                  modificar, incluido el precio (solo Administrador)
//   DELETE /api/productos/7                  desactivar (solo Administrador, baja logica)
//
// Las consultas las hacen los 4 roles y ven el precio y el stock.
// Registrar, modificar y desactivar los valida el BO (rol, codigo repetido, categoria desactivada,
// precio, costo, stock minimo, factor y unidad base). Los mensajes del BO llegan tal cual a Blazor.
@Path("productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoRS extends VentanillaBase {

    private ProductoBO productoBO = new ProductoBOImpl();
    private CategoriaBO categoriaBO = new CategoriaBOImpl();
    private UsuarioBO usuarioBO = new UsuarioBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("buscar") String buscar,
                           @QueryParam("codigo") String codigo,
                           @QueryParam("nombre") String nombre,
                           @QueryParam("categoria") Integer idCategoria,
                           @QueryParam("estado") Boolean estado) {
        return ejecutar(ticket, id -> {
            List<Producto> lista;
            if (buscar != null && !buscar.isBlank()) {
                lista = buscarRapido(buscar.trim());
            } else if (codigo != null && !codigo.isBlank()) {
                lista = new ArrayList<>();
                Producto producto = productoBO.buscarPorCodigo(codigo.trim());
                if (producto != null) {
                    lista.add(producto);
                }
            } else if (nombre != null && !nombre.isBlank()) {
                lista = productoBO.buscarPorNombre(nombre.trim());
            } else if (idCategoria != null) {
                lista = productoBO.listarPorCategoria(idCategoria);
            } else if (estado != null) {
                lista = productoBO.listarPorEstado(estado);
            } else {
                // listarTodos del BO solo trae los activos; la pantalla muestra todos con su estado
                lista = new ArrayList<>(productoBO.listarPorEstado(true));
                lista.addAll(productoBO.listarPorEstado(false));
            }
            completar(lista);
            return lista;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idProducto) {
        Response respuesta = ejecutar(ticket, id -> {
            Producto producto = productoBO.obtener(idProducto);
            if (producto != null) {
                completar(List.of(producto));
            }
            return producto;
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe el producto con id " + idProducto);
        }
        return respuesta;
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, Producto producto) {
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            Producto registrado = productoBO.registrar(limpiarAuditoria(producto), id);
            completar(List.of(registrado));
            return registrado;
        });
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket, @PathParam("id") int idProducto,
                              Producto producto) {
        if (producto == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del producto");
        }
        producto.setId(idProducto); // el id sale de la direccion, no del cuerpo
        return ejecutar(ticket, id -> {
            // el BO pone como responsable al usuario de la sesion, no lo que mande Blazor
            Producto modificado = productoBO.modificar(limpiarAuditoria(producto), id);
            completar(List.of(modificado));
            return modificado;
        });
    }

    @DELETE
    @Path("{id}")
    public Response desactivar(@HeaderParam("ticket") String ticket, @PathParam("id") int idProducto) {
        return ejecutar(ticket, id -> {
            productoBO.desactivar(idProducto, id);
            return null; // 204 sin cuerpo
        });
    }

    // Buscador de productos (RF005, lector de codigo de barras): el lector escribe el codigo y presiona
    // Enter. Si el texto es un codigo exacto devuelve ese producto; si no, los que coinciden por nombre.
    private List<Producto> buscarRapido(String texto) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        if (texto.length() <= 50) { // 50 es el largo maximo de un codigo en el BO
            Producto porCodigo = productoBO.buscarPorCodigo(texto);
            if (porCodigo != null) {
                lista.add(porCodigo);
                return lista;
            }
        }
        lista.addAll(productoBO.buscarPorNombre(texto));
        return lista;
    }

    // Los datos de auditoria (quien y cuando modifico) los pone el BO, nunca Blazor.
    private Producto limpiarAuditoria(Producto producto) {
        if (producto != null) {
            producto.setUsuarioModificacion(null);
            producto.setFechaModificacion(null);
        }
        return producto;
    }

    // El DAO solo trae el id de la categoria y el id del usuario que modifico. Para que la pantalla
    // muestre "Herramientas" y "Ultima modificacion: aquispe" se completan aqui con los BO.
    // Del usuario solo se mandan su usuario, nombres y apellidos (nunca la clave ni el id).
    private void completar(List<Producto> productos) throws SQLException {
        if (productos.isEmpty()) {
            return;
        }
        Map<Integer, Categoria> categorias = new HashMap<>();
        for (Categoria c : categoriaBO.listarPorEstado(true)) {
            categorias.put(c.getId(), c);
        }
        for (Categoria c : categoriaBO.listarPorEstado(false)) {
            categorias.put(c.getId(), c);
        }
        Map<Integer, Usuario> usuarios = new HashMap<>();
        for (Producto p : productos) {
            if (p.getCategoria() != null) {
                Categoria categoria = categorias.get(p.getCategoria().getId());
                if (categoria != null) {
                    p.setCategoria(new Categoria(categoria.getId(), categoria.getNombre(),
                            categoria.getDescripcion(), categoria.isEstado()));
                }
            }
            if (p.getUsuarioModificacion() != null) {
                int idUsuario = p.getUsuarioModificacion().getId();
                Usuario visible = usuarios.get(idUsuario);
                if (visible == null) {
                    visible = new Usuario();
                    Usuario completo = usuarioBO.obtener(idUsuario);
                    if (completo != null) {
                        visible.setNombreUsuario(completo.getNombreUsuario());
                        visible.setNombres(completo.getNombres());
                        visible.setApellidos(completo.getApellidos());
                    }
                    usuarios.put(idUsuario, visible);
                }
                p.setUsuarioModificacion(visible);
            }
        }
    }
}
