package pe.edu.pucp.sigmeta.boimpl.producto;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.producto.ProductoBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.producto.CategoriaDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.daoimpl.producto.CategoriaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.enums.UnidadMedida;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProductoBOImpl implements ProductoBO {

    // Unidades que no se parten: no se vende media caja ni medio juego
    static final Set<UnidadMedida> UNIDADES_ENTERAS =
            EnumSet.of(UnidadMedida.UNIDAD, UnidadMedida.CAJA, UnidadMedida.JUEGO);

    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;
    private final UsuarioBO usuarioBO;
    // partes de esta clase (mismo paquete)
    private final ProductoValidacion validacionProducto;

    public ProductoBOImpl() {
        this.productoDAO = new ProductoDAOImpl();
        this.categoriaDAO = new CategoriaDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
        this.validacionProducto = new ProductoValidacion(productoDAO, categoriaDAO);
    }

    @Override
    public String proponerCodigoInterno(int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        return Transaccion.consultar(this::siguienteCodigoInterno);
    }

    private String siguienteCodigoInterno() throws SQLException {
        long maximo = 0;
        for (String codigo : productoDAO.listarCodigosInternos()) {
            if (codigo == null || !codigo.regionMatches(true, 0, "INT-", 0, 4)) continue;
            String digitos = codigo.substring(4);
            if (digitos.matches("[0-9]+")) {
                try {
                    maximo = Math.max(maximo, Long.parseLong(digitos));
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Código interno de producto fuera de rango");
                }
            }
        }
        if (maximo == Long.MAX_VALUE) throw new IllegalStateException("Sin códigos internos disponibles");
        return "INT-" + String.format(Locale.ROOT, "%04d", maximo + 1);
    }

    // RF005
    @Override
    public Producto registrar(Producto producto, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validacionProducto.validarDatos(producto);
        // RF012: el stock solo entra por recepciones de compra o ajustes de inventario
        producto.setStockActual(0);
        return Transaccion.ejecutar(() -> {
            validacionProducto.verificarCategoriaActiva(producto.getCategoria().getId());
            producto.setCodigoInterno(siguienteCodigoInterno());
            validacionProducto.validarCodigosUnicos(producto);
            validacionProducto.validarNombreUnico(producto);
            productoDAO.save(producto);
            producto.setEstado(true);
            return producto;
        });
    }

    // RF005
    @Override
    public Producto modificar(Producto producto, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validacionProducto.validarDatos(producto);
        return Transaccion.ejecutar(() -> {
            Producto actual = obtenerExistente(producto.getId());
            if (!actual.isEstado()) {
                throw new IllegalStateException("No se puede modificar un producto desactivado");
            }
            producto.setCodigoInterno(actual.getCodigoInterno());
            if (producto.getUnidadVenta() != actual.getUnidadVenta()) {
                throw new IllegalArgumentException("La unidad base de inventario no puede cambiar; registre otra presentación/producto");
            }
            validacionProducto.verificarCategoriaActiva(producto.getCategoria().getId());
            validacionProducto.validarCodigosUnicos(producto);
            validacionProducto.validarNombreUnico(producto);
            // RF012: el stock no se edita a mano, lo mueven los movimientos de almacen
            producto.setStockActual(actual.getStockActual());
            // RNF001: quien modifica el producto (incluido el precio); la fecha y hora la registra el SP
            Usuario responsable = new Usuario();
            responsable.setId(idResponsable);
            producto.setUsuarioModificacion(responsable);
            productoDAO.update(producto);
            producto.setFechaModificacion(LocalDateTime.now());
            producto.setEstado(actual.isEstado());
            return producto;
        });
    }

    // RF005, RNF002: baja logica
    @Override
    public void desactivar(int idProducto, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        Transaccion.ejecutar(() -> {
            Producto producto = obtenerExistente(idProducto);
            if (!producto.isEstado()) {
                throw new IllegalStateException("El producto " + producto.getCodigoInterno() + " ya está desactivado");
            }
            productoDAO.remove(producto);
            producto.setEstado(false);
            return null;
        });
    }

    // RF005: vuelve a activar un producto desactivado (baja logica reversible). Solo el Administrador.
    @Override
    public Producto activar(int idProducto, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        return Transaccion.ejecutar(() -> {
            Producto producto = obtenerExistente(idProducto);
            if (producto.isEstado()) {
                throw new IllegalStateException("El producto " + producto.getCodigoInterno() + " ya está activo");
            }
            // un producto activo no puede quedar en una categoria desactivada
            validacionProducto.verificarCategoriaActiva(producto.getCategoria().getId());
            // sus codigos o su nombre pudieron registrarse en otro producto mientras estaba desactivado
            validacionProducto.validarCodigosUnicos(producto);
            validacionProducto.validarNombreUnico(producto);
            new pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl().ejecutar("sp_producto_activar", idProducto);
            producto.setEstado(true);
            return producto;
        });
    }

    // RF005
    @Override
    public Producto obtener(int idProducto) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return productoDAO.load(idProducto);
        });
    }

    // RF005: solo productos activos
    @Override
    public List<Producto> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return productoDAO.listarTodos();
        });
    }

    // RF005: el codigo leido puede ser el interno, el del fabricante o el del proveedor
    @Override
    public Producto buscarPorCodigo(String codigo) throws SQLException {
        // se preprocesa igual que al guardar: " int-001 " encuentra "INT-001"
        String buscado = TextoNormalizado.codigo(Validador.textoObligatorio(codigo, "código", 50));
        // Primero el codigo interno, luego el del fabricante y al final el del proveedor. Los codigos ya no
        // se repiten entre productos (validarCodigosUnicos); el orden solo importa para datos antiguos.
        return Transaccion.ejecutar(() -> {
            List<Producto> activos = productoDAO.listarTodos();
            return activos.stream().filter(p -> buscado.equalsIgnoreCase(p.getCodigoInterno())).findFirst()
                    .or(() -> activos.stream().filter(p -> buscado.equalsIgnoreCase(p.getCodigoFabricante())).findFirst())
                    .or(() -> activos.stream().filter(p -> buscado.equalsIgnoreCase(p.getCodigoProveedor())).findFirst())
                    .orElse(null);
        });
    }

    // RF005: coincidencia parcial entre los productos activos, sin importar tildes ni mayusculas
    // ("martíllo" encuentra "MARTILLO DE ACERO")
    @Override
    public List<Producto> buscarPorNombre(String nombre) throws SQLException {
        String texto = TextoNormalizado.clave(Validador.textoObligatorio(nombre, "nombre", 150));
        return Transaccion.ejecutar(() -> {
            return productoDAO.listarTodos().stream()
                    .filter(p -> TextoNormalizado.clave(p.getNombre()).contains(texto))
                    .toList();
        });
    }

    // RF005
    @Override
    public List<Producto> listarPorCategoria(int idCategoria) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return productoDAO.listarTodos().stream()
                    .filter(p -> p.getCategoria().getId() == idCategoria)
                    .toList();
        });
    }

    // RF005
    @Override
    public List<Producto> listarPorEstado(boolean estado) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return productoDAO.listarPorEstado(estado);
        });
    }

    // RF005: busqueda de la pantalla Productos con los filtros combinados (nombre + categoria + estado).
    // Cada filtro es opcional: null no filtra. Ej.: "martillo" + HERRAMIENTAS + inactivos.
    // El nombre se compara por su clave, sin importar mayúsculas ni tildes.
    @Override
    public List<Producto> buscar(String nombre, Integer idCategoria, Boolean estado) throws SQLException {
        String texto = nombre == null || nombre.isBlank() ? null
                : TextoNormalizado.clave(Validador.textoOpcional(nombre, "nombre", 150));
        return Transaccion.ejecutar(() -> {
            List<Producto> productos = new ArrayList<>();
            if (estado == null || estado) {
                productos.addAll(productoDAO.listarPorEstado(true));
            }
            if (estado == null || !estado) {
                productos.addAll(productoDAO.listarPorEstado(false));
            }
            return productos.stream()
                    .filter(p -> idCategoria == null || p.getCategoria().getId() == idCategoria)
                    .filter(p -> texto == null || TextoNormalizado.clave(p.getNombre()).contains(texto))
                    .toList();
        });
    }

    // RF012
    @Override
    public List<Producto> listarConStockBajo() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return productoDAO.listarTodos().stream()
                    .filter(p -> p.getStockActual() <= p.getStockMinimo())
                    .toList();
        });
    }

    // RF005: registrar, modificar (incluido el precio) y desactivar es exclusivo del Administrador
    private void verificarPermiso(int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.ADMINISTRADOR);
    }

    private Producto obtenerExistente(int idProducto) throws SQLException {
        Producto producto = productoDAO.load(idProducto);
        if (producto == null) {
            throw new IllegalArgumentException("No existe el producto con id " + idProducto);
        }
        return producto;
    }

}
