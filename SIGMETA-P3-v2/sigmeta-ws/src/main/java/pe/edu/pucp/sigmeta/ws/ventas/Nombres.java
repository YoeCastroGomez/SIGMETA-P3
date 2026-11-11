package pe.edu.pucp.sigmeta.ws.ventas;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.producto.ProductoBO;
import pe.edu.pucp.sigmeta.bo.socio.ClienteBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.producto.ProductoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.socio.ClienteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF007 y RF008: los DAO de ventas devuelven el cliente, el producto y el usuario solo con su id.
// Esta clase completa los textos para mostrar (razon social, nombre del producto, nombre de usuario)
// usando los BO de cada uno, sin modificarlos. Se crea una por pedido y guarda lo que ya busco,
// para no consultar la base una vez por cada linea.
public class Nombres {

    private final ClienteBO clienteBO = new ClienteBOImpl();
    private final ProductoBO productoBO = new ProductoBOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    private Map<Integer, Cliente> clientes;
    private Map<Integer, Producto> productos;
    private final Map<Integer, String> usuarios = new HashMap<>();

    public Cliente cliente(int idCliente) throws SQLException {
        if (clientes == null) {
            clientes = new HashMap<>();
            for (Cliente c : clienteBO.listarTodos()) {
                clientes.put(c.getId(), c);
            }
        }
        return clientes.get(idCliente);
    }

    public Producto producto(int idProducto) throws SQLException {
        if (productos == null) {
            productos = new HashMap<>();
            for (Producto p : productoBO.listarTodos()) {
                productos.put(p.getId(), p);
            }
        }
        return productos.get(idProducto);
    }

    // solo el nombre de usuario: nunca se manda el Usuario completo (tiene el hash de la clave)
    public String usuario(Usuario usuario) throws SQLException {
        if (usuario == null) {
            return null;
        }
        if (usuario.getNombreUsuario() != null) {
            return usuario.getNombreUsuario();
        }
        String nombre = usuarios.get(usuario.getId());
        if (nombre == null) {
            nombre = Utilidades.usuario(usuarioBO.obtener(usuario.getId()));
            usuarios.put(usuario.getId(), nombre);
        }
        return nombre;
    }
}
