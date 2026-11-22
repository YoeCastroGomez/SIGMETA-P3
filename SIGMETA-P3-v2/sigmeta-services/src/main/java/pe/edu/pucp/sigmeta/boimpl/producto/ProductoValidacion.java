package pe.edu.pucp.sigmeta.boimpl.producto;

import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.producto.CategoriaDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import static pe.edu.pucp.sigmeta.boimpl.producto.ProductoBOImpl.UNIDADES_ENTERAS;

// RF005: validaciones del producto (categoria activa, datos, decimales, codigos y nombre unicos).
// Parte de ProductoBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class ProductoValidacion {

    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;

    ProductoValidacion(ProductoDAO productoDAO, CategoriaDAO categoriaDAO) {
        this.productoDAO = productoDAO;
        this.categoriaDAO = categoriaDAO;
    }

    void verificarCategoriaActiva(int idCategoria) throws SQLException {
        Categoria categoria = categoriaDAO.load(idCategoria);
        if (categoria == null || !categoria.isEstado()) {
            throw new IllegalArgumentException("La categoría " + idCategoria + " no existe o está desactivada");
        }
    }

    void validarDatos(Producto producto) {
        Validador.obligatorio(producto, "producto");
        Validador.obligatorio(producto.getCategoria(), "categoría");
        Validador.obligatorio(producto.getUnidadCompra(), "unidad de compra");
        Validador.obligatorio(producto.getUnidadVenta(), "unidad de venta");
        // Nombres y descripciones en mayúsculas; la ruta de imagen conserva su formato.
        if (producto.getCodigoInterno() != null && !producto.getCodigoInterno().isBlank()) {
            producto.setCodigoInterno(TextoNormalizado.codigo(
                    Validador.textoObligatorio(producto.getCodigoInterno(), "código interno", 50)));
        }
        producto.setCodigoFabricante(TextoNormalizado.codigo(
                Validador.textoOpcional(producto.getCodigoFabricante(), "código de fabricante", 50)));
        producto.setCodigoProveedor(TextoNormalizado.codigo(
                Validador.textoOpcional(producto.getCodigoProveedor(), "código de proveedor", 50)));
        producto.setNombre(TextoNormalizado.nombre(Validador.textoObligatorio(producto.getNombre(), "nombre", 150)));
        producto.setDescripcion(TextoNormalizado.opcional(
                Validador.textoOpcional(producto.getDescripcion(), "descripción", 255)));
        producto.setImagen(Validador.textoOpcional(producto.getImagen(), "imagen", 255));

        if (!Double.isFinite(producto.getFactorConversion()) || producto.getFactorConversion() <= 0) {
            throw new IllegalArgumentException("El factor de conversión debe ser mayor a 0");
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
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
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
        validarDecimales(producto.getFactorConversion(), 3, "El factor de conversión");
        validarDecimales(producto.getStockMinimo(), 3, "El stock mínimo");

        // Si la unidad de venta no se parte (UNIDAD, CAJA, JUEGO), el factor y el stock minimo van enteros:
        // una caja de 12 unidades tiene factor 12, no 12.5
        if (UNIDADES_ENTERAS.contains(producto.getUnidadVenta())) {
            if (!esEntero(producto.getFactorConversion())) {
                throw new IllegalArgumentException("La unidad de venta " + producto.getUnidadVenta()
                        + " no se parte: el factor de conversión debe ser un número entero");
            }
            if (!esEntero(producto.getStockMinimo())) {
                throw new IllegalArgumentException("La unidad de venta " + producto.getUnidadVenta()
                        + " no se parte: el stock mínimo debe ser un número entero");
            }
        }
    }

    static void validarDecimales(double valor, int maximo, String campo) {
        if (BigDecimal.valueOf(valor).stripTrailingZeros().scale() > maximo) {
            throw new IllegalArgumentException(campo + " admite como máximo " + maximo + " decimales");
        }
    }

    static boolean esEntero(double valor) {
        return valor == Math.rint(valor);
    }

    // RF005: el lector de codigo de barras busca en los 3 codigos a la vez (interno, fabricante y proveedor).
    // Si un codigo de este producto fuera igual a un codigo de otro, el lector no sabria cual elegir y en una
    // venta se cobraria el producto equivocado. Por eso:
    //   - ninguno de sus codigos puede ser igual a ningun codigo de otro producto activo;
    //   - el codigo interno es UNIQUE en la tabla: tampoco se repite frente a los desactivados.
    // Los codigos de fabricante y proveedor de un producto desactivado si se pueden volver a usar: el lector
    // solo busca entre los activos, y un producto desactivado ya no se puede modificar para liberarlos.
    void validarCodigosUnicos(Producto producto) throws SQLException {
        for (Producto otro : productoDAO.listarPorEstado(false)) {
            if (otro.getId() != producto.getId()
                    && producto.getCodigoInterno().equalsIgnoreCase(otro.getCodigoInterno())) {
                throw new IllegalArgumentException("Ya existe un producto desactivado con el código "
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
                        throw new IllegalArgumentException("Ya existe un producto con el código " + propio
                                + ": lo usa " + otro.getCodigoInterno() + " - " + otro.getNombre());
                    }
                }
            }
        }
    }

    // los codigos que tiene el producto (el de fabricante y el de proveedor son opcionales)
    static List<String> codigos(Producto producto) {
        List<String> codigos = new ArrayList<>();
        for (String codigo : new String[]{producto.getCodigoInterno(), producto.getCodigoFabricante(),
                producto.getCodigoProveedor()}) {
            if (codigo != null && !codigo.isBlank()) {
                codigos.add(codigo);
            }
        }
        return codigos;
    }

    // el nombre tampoco se repite, también frente a los productos desactivados. Se comparan las claves de los
    // nombres: "Martillo", "MARTILLO" y "Martíllo" son el mismo producto
    void validarNombreUnico(Producto producto) throws SQLException {
        List<Producto> productos = new ArrayList<>(productoDAO.listarPorEstado(true));
        productos.addAll(productoDAO.listarPorEstado(false));
        String nuevo = TextoNormalizado.clave(producto.getNombre());
        for (Producto otro : productos) {
            if (TextoNormalizado.clave(otro.getNombre()).equals(nuevo) && otro.getId() != producto.getId()) {
                throw new IllegalArgumentException("Ya existe un producto con el nombre " + producto.getNombre()
                        + " (código " + otro.getCodigoInterno() + ")");
            }
        }
    }
}
