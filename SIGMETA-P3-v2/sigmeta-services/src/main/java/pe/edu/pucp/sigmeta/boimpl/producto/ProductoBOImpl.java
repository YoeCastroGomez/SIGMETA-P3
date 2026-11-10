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
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProductoBOImpl implements ProductoBO {

    // Unidades que no se parten: no se vende media caja ni medio juego
    private static final Set<UnidadMedida> UNIDADES_ENTERAS =
            EnumSet.of(UnidadMedida.UNIDAD, UnidadMedida.CAJA, UnidadMedida.JUEGO);

    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;
    private final UsuarioBO usuarioBO;

    public ProductoBOImpl() {
        this.productoDAO = new ProductoDAOImpl();
        this.categoriaDAO = new CategoriaDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF005
    @Override
    public Producto registrar(Producto producto, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validarDatos(producto);
        // RF012: el stock solo entra por recepciones de compra o ajustes de inventario
        producto.setStockActual(0);
        return Transaccion.ejecutar(() -> {
            verificarCategoriaActiva(producto.getCategoria().getId());
            validarCodigosUnicos(producto);
            validarNombreUnico(producto);
            productoDAO.save(producto);
            producto.setEstado(true);
            return producto;
        });
    }

    // RF005
    @Override
    public Producto modificar(Producto producto, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validarDatos(producto);
        return Transaccion.ejecutar(() -> {
            Producto actual = obtenerExistente(producto.getId());
            if (!actual.isEstado()) {
                throw new IllegalStateException("No se puede modificar un producto desactivado");
            }
            if (producto.getUnidadVenta() != actual.getUnidadVenta()) {
                throw new IllegalArgumentException("La unidad base de inventario no puede cambiar; registre otra presentacion/producto");
            }
            verificarCategoriaActiva(producto.getCategoria().getId());
            validarCodigosUnicos(producto);
            validarNombreUnico(producto);
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
                throw new IllegalStateException("El producto " + producto.getCodigoInterno() + " ya esta desactivado");
            }
            productoDAO.remove(producto);
            producto.setEstado(false);
            return null;
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
        String buscado = TextoNormalizado.codigo(Validador.textoObligatorio(codigo, "codigo", 50));
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
        String texto = TextoNormalizado.nombre(Validador.textoObligatorio(nombre, "nombre", 150));
        return Transaccion.ejecutar(() -> {
            return productoDAO.listarTodos().stream()
                    .filter(p -> TextoNormalizado.nombre(p.getNombre()).contains(texto))
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
    // El nombre se compara normalizado, sin importar mayusculas ni tildes.
    @Override
    public List<Producto> buscar(String nombre, Integer idCategoria, Boolean estado) throws SQLException {
        String texto = nombre == null || nombre.isBlank() ? null
                : TextoNormalizado.nombre(Validador.textoOpcional(nombre, "nombre", 150));
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
                    .filter(p -> texto == null || TextoNormalizado.nombre(p.getNombre()).contains(texto))
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

    private void verificarCategoriaActiva(int idCategoria) throws SQLException {
        Categoria categoria = categoriaDAO.load(idCategoria);
        if (categoria == null || !categoria.isEstado()) {
            throw new IllegalArgumentException("La categoria " + idCategoria + " no existe o esta desactivada");
        }
    }

    private void validarDatos(Producto producto) {
        Validador.obligatorio(producto, "producto");
        Validador.obligatorio(producto.getCategoria(), "categoria");
        Validador.obligatorio(producto.getUnidadCompra(), "unidad de compra");
        Validador.obligatorio(producto.getUnidadVenta(), "unidad de venta");
        // Preprocesado de los textos (TextoNormalizado): todo en MAYUSCULAS.
        //   codigos: sin espacios (" int-001 " -> "INT-001")
        //   nombre y descripcion: sin tildes y con un solo espacio ("Martíllo" -> "MARTILLO")
        //   imagen: tal cual, porque es el nombre de un archivo
        producto.setCodigoInterno(TextoNormalizado.codigo(
                Validador.textoObligatorio(producto.getCodigoInterno(), "codigo interno", 50)));
        producto.setCodigoFabricante(TextoNormalizado.codigo(
                Validador.textoOpcional(producto.getCodigoFabricante(), "codigo de fabricante", 50)));
        producto.setCodigoProveedor(TextoNormalizado.codigo(
                Validador.textoOpcional(producto.getCodigoProveedor(), "codigo de proveedor", 50)));
        producto.setNombre(TextoNormalizado.nombre(Validador.textoObligatorio(producto.getNombre(), "nombre", 150)));
        producto.setDescripcion(TextoNormalizado.opcional(
                Validador.textoOpcional(producto.getDescripcion(), "descripcion", 255)));
        producto.setImagen(Validador.textoOpcional(producto.getImagen(), "imagen", 255));

        if (!Double.isFinite(producto.getFactorConversion()) || producto.getFactorConversion() <= 0) {
            throw new IllegalArgumentException("El factor de conversion debe ser mayor a 0");
        }
        // si se compra y se vende en la misma unidad no hay conversion
        if (producto.getUnidadCompra() == producto.getUnidadVenta() && producto.getFactorConversion() != 1) {
            throw new IllegalArgumentException("Si la unidad de compra y de venta son iguales, el factor debe ser 1");
        }
        if (!Double.isFinite(producto.getPrecioVenta()) || producto.getPrecioVenta() <= 0) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor a 0");
        }
        if (!Double.isFinite(producto.getCostoUnitario()) || producto.getCostoUnitario() < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo");
        }
        if (!Double.isFinite(producto.getStockMinimo()) || producto.getStockMinimo() < 0) {
            throw new IllegalArgumentException("El stock minimo no puede ser negativo");
        }
        // el precio referencial es obligatorio (en Java es double: si no se manda llega 0)
        if (!Double.isFinite(producto.getPrecioReferencial()) || producto.getPrecioReferencial() <= 0) {
            throw new IllegalArgumentException("El precio referencial es obligatorio y debe ser mayor a 0");
        }
        // no se vende por debajo de lo que cuesta
        if (producto.getPrecioVenta() < producto.getCostoUnitario()) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "El precio de venta (S/ %.2f) no puede ser menor que el costo unitario (S/ %.2f)",
                    producto.getPrecioVenta(), producto.getCostoUnitario()));
        }

        // La base guarda los montos con 2 decimales y el factor y el stock con 3: si se mandan mas,
        // MySQL los redondearia sin avisar. Mejor rechazarlos con un mensaje claro.
        validarDecimales(producto.getPrecioVenta(), 2, "El precio de venta");
        validarDecimales(producto.getCostoUnitario(), 2, "El costo unitario");
        validarDecimales(producto.getPrecioReferencial(), 2, "El precio referencial");
        validarDecimales(producto.getFactorConversion(), 3, "El factor de conversion");
        validarDecimales(producto.getStockMinimo(), 3, "El stock minimo");

        // Si la unidad de venta no se parte (UNIDAD, CAJA, JUEGO), el factor y el stock minimo van enteros:
        // una caja de 12 unidades tiene factor 12, no 12.5
        if (UNIDADES_ENTERAS.contains(producto.getUnidadVenta())) {
            if (!esEntero(producto.getFactorConversion())) {
                throw new IllegalArgumentException("La unidad de venta " + producto.getUnidadVenta()
                        + " no se parte: el factor de conversion debe ser un numero entero");
            }
            if (!esEntero(producto.getStockMinimo())) {
                throw new IllegalArgumentException("La unidad de venta " + producto.getUnidadVenta()
                        + " no se parte: el stock minimo debe ser un numero entero");
            }
        }
    }

    private static void validarDecimales(double valor, int maximo, String campo) {
        if (BigDecimal.valueOf(valor).stripTrailingZeros().scale() > maximo) {
            throw new IllegalArgumentException(campo + " admite como maximo " + maximo + " decimales");
        }
    }

    private static boolean esEntero(double valor) {
        return valor == Math.rint(valor);
    }

    // RF005: el lector de codigo de barras busca en los 3 codigos a la vez (interno, fabricante y proveedor).
    // Si un codigo de este producto fuera igual a un codigo de otro, el lector no sabria cual elegir y en una
    // venta se cobraria el producto equivocado. Por eso:
    //   - ninguno de sus codigos puede ser igual a ningun codigo de otro producto activo;
    //   - el codigo interno es UNIQUE en la tabla: tampoco se repite frente a los desactivados.
    // Los codigos de fabricante y proveedor de un producto desactivado si se pueden volver a usar: el lector
    // solo busca entre los activos, y un producto desactivado ya no se puede modificar para liberarlos.
    private void validarCodigosUnicos(Producto producto) throws SQLException {
        for (Producto otro : productoDAO.listarPorEstado(false)) {
            if (otro.getId() != producto.getId()
                    && producto.getCodigoInterno().equalsIgnoreCase(otro.getCodigoInterno())) {
                throw new IllegalArgumentException("Ya existe un producto desactivado con el codigo "
                        + producto.getCodigoInterno());
            }
        }
        List<String> propios = codigos(producto);
        for (Producto otro : productoDAO.listarPorEstado(true)) {
            if (otro.getId() == producto.getId()) {
                continue;
            }
            for (String suyo : codigos(otro)) {
                for (String propio : propios) {
                    if (propio.equalsIgnoreCase(suyo)) {
                        throw new IllegalArgumentException("Ya existe un producto con el codigo " + propio
                                + ": lo usa " + otro.getCodigoInterno() + " - " + otro.getNombre());
                    }
                }
            }
        }
    }

    // los codigos que tiene el producto (el de fabricante y el de proveedor son opcionales)
    private static List<String> codigos(Producto producto) {
        List<String> codigos = new ArrayList<>();
        for (String codigo : new String[]{producto.getCodigoInterno(), producto.getCodigoFabricante(),
                producto.getCodigoProveedor()}) {
            if (codigo != null && !codigo.isBlank()) {
                codigos.add(codigo);
            }
        }
        return codigos;
    }

    // el nombre tampoco se repite, tambien frente a los productos desactivados. Se comparan los nombres
    // normalizados: "Martillo", "MARTILLO" y "Martíllo" son el mismo producto
    private void validarNombreUnico(Producto producto) throws SQLException {
        List<Producto> productos = new ArrayList<>(productoDAO.listarPorEstado(true));
        productos.addAll(productoDAO.listarPorEstado(false));
        String nuevo = TextoNormalizado.nombre(producto.getNombre());
        for (Producto otro : productos) {
            if (TextoNormalizado.nombre(otro.getNombre()).equals(nuevo) && otro.getId() != producto.getId()) {
                throw new IllegalArgumentException("Ya existe un producto con el nombre " + producto.getNombre()
                        + " (codigo " + otro.getCodigoInterno() + ")");
            }
        }
    }
}
