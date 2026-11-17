package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.edu.pucp.sigmeta.pruebas.Datos.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pe.edu.pucp.sigmeta.boimpl.producto.CategoriaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.producto.ProductoBOImpl;
import pe.edu.pucp.sigmeta.model.enums.UnidadMedida;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.model.producto.Producto;

/**
 * H14 - RF005 y RF006: reglas del catalogo (CategoriaBOImpl, ProductoBOImpl).
 *   RF006 nombre de categoria unico (sin importar mayusculas ni tildes)
 *   RF006 no se desactiva una categoria con productos activos; se puede reactivar
 *   RF005 solo el Administrador registra productos; el precio no baja del costo; codigos unicos
 *   RF005 un producto nuevo entra con stock 0; se puede reactivar, pero no en una categoria desactivada
 *
 * Cada prueba crea su propia categoria y sus productos (con un sufijo al azar) y al final los desactiva.
 * Integracion contra una base LOCAL de pruebas:
 *   mvn test -pl sigmeta-ejecucion -am -Dsigmeta.integracion=true -Dsurefire.failIfNoSpecifiedTests=false
 */
@EnabledIfSystemProperty(named = "sigmeta.integracion", matches = "true")
@DisplayName("Catalogo: categorias y productos")
class CatalogoBOTest {

    private static final CategoriaBOImpl categorias = new CategoriaBOImpl();
    private static final ProductoBOImpl productos = new ProductoBOImpl();

    private final List<Producto> productosCreados = new ArrayList<>();
    private final List<Categoria> categoriasCreadas = new ArrayList<>();

    @BeforeAll
    static void soloBaseLocal() {
        exigirBaseLocal();
    }

    /** Deja inactivo todo lo creado: primero los productos, para poder desactivar sus categorias. */
    @AfterEach
    void limpiar() {
        for (Producto p : productosCreados) {
            try {
                productos.desactivar(p.getId(), ADMINISTRADOR);
            } catch (Exception ignorada) {
                // ya estaba desactivado
            }
        }
        for (Categoria c : categoriasCreadas) {
            try {
                categorias.desactivar(c.getId(), ADMINISTRADOR);
            } catch (Exception ignorada) {
                // ya estaba desactivada
            }
        }
    }

    // ------------------------------------------------------------------ datos de prueba

    private static String sufijo() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
    }

    private Categoria categoriaNueva() throws Exception {
        Categoria c = new Categoria();
        c.setNombre("Categoria JUnit " + sufijo());
        c.setDescripcion("Creada por CatalogoBOTest");
        Categoria guardada = categorias.registrar(c, ADMINISTRADOR);
        categoriasCreadas.add(guardada);
        return guardada;
    }

    /** Producto valido: se vende por unidad, precio 20.00 y costo 12.00. */
    private static Producto productoValido(Categoria categoria) {
        String s = sufijo();
        Producto p = new Producto();
        p.setNombre("Producto JUnit " + s);
        p.setCodigoFabricante("FAB-JU-" + s);
        p.setCategoria(categoria);
        p.setUnidadCompra(UnidadMedida.UNIDAD);
        p.setUnidadVenta(UnidadMedida.UNIDAD);
        p.setFactorConversion(1);
        p.setPrecioVenta(20.00);
        p.setPrecioReferencial(20.00);
        p.setCostoUnitario(12.00);
        p.setStockMinimo(5);
        return p;
    }

    private Producto registrar(Producto p) throws Exception {
        Producto guardado = productos.registrar(p, ADMINISTRADOR);
        productosCreados.add(guardado);
        return guardado;
    }

    // ------------------------------------------------------------------ RF006 categorias

    @Test
    @DisplayName("RF006: no se registra otra categoria con el mismo nombre en otras mayusculas")
    void nombreDeCategoriaUnico() throws Exception {
        Categoria original = categoriaNueva();
        Categoria repetida = new Categoria();
        repetida.setNombre(original.getNombre().toUpperCase());

        assertThrows(IllegalArgumentException.class, () -> categorias.registrar(repetida, ADMINISTRADOR));
    }

    @Test
    @DisplayName("RF006: una categoria con productos activos no se desactiva; sin ellos si, y se puede reactivar")
    void categoriaConProductosActivos() throws Exception {
        Categoria categoria = categoriaNueva();
        Producto producto = registrar(productoValido(categoria));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> categorias.desactivar(categoria.getId(), ADMINISTRADOR));
        assertTrue(e.getMessage().contains("productos activos"), e.getMessage());

        productos.desactivar(producto.getId(), ADMINISTRADOR);
        categorias.desactivar(categoria.getId(), ADMINISTRADOR);
        assertFalse(categorias.obtener(categoria.getId()).isEstado());

        categorias.activar(categoria.getId(), ADMINISTRADOR);
        assertTrue(categorias.obtener(categoria.getId()).isEstado());
    }

    @Test
    @DisplayName("RF006: solo el Administrador registra categorias")
    void soloAdministradorRegistraCategorias() {
        Categoria c = new Categoria();
        c.setNombre("Categoria JUnit " + sufijo());

        assertThrows(IllegalStateException.class, () -> categorias.registrar(c, VENDEDOR));
    }

    // ------------------------------------------------------------------ RF005 productos

    @Test
    @DisplayName("RF005: un producto nuevo entra activo y con stock 0 aunque se mande otro valor")
    void productoNuevoConStockCero() throws Exception {
        Producto p = productoValido(categoriaNueva());
        p.setStockActual(999);

        Producto guardado = registrar(p);

        Producto leido = productos.obtener(guardado.getId());
        assertTrue(leido.isEstado());
        assertEquals(0, leido.getStockActual(), CENTAVO);
        assertTrue(leido.getCodigoInterno() != null && !leido.getCodigoInterno().isBlank(),
                "el codigo interno lo asigna el sistema");
    }

    @Test
    @DisplayName("RF005: solo el Administrador registra productos")
    void soloAdministradorRegistraProductos() throws Exception {
        Producto p = productoValido(categoriaNueva());

        assertThrows(IllegalStateException.class, () -> productos.registrar(p, VENDEDOR));
        assertThrows(IllegalStateException.class, () -> productos.registrar(p, ALMACENERO));
    }

    @Test
    @DisplayName("RF005: el precio de venta no puede ser menor que el costo")
    void precioNoMenorQueCosto() throws Exception {
        Producto p = productoValido(categoriaNueva());
        p.setPrecioVenta(10.00);   // costo 12.00

        assertThrows(IllegalArgumentException.class, () -> productos.registrar(p, ADMINISTRADOR));
    }

    @Test
    @DisplayName("RF005: no se repite el codigo de fabricante entre productos")
    void codigoDeFabricanteUnico() throws Exception {
        Categoria categoria = categoriaNueva();
        Producto primero = registrar(productoValido(categoria));
        Producto segundo = productoValido(categoria);
        segundo.setCodigoFabricante(primero.getCodigoFabricante());

        assertThrows(IllegalArgumentException.class, () -> productos.registrar(segundo, ADMINISTRADOR));
    }

    @Test
    @DisplayName("RF005: un producto desactivado se puede reactivar, pero no si su categoria esta desactivada")
    void reactivarProducto() throws Exception {
        Categoria categoria = categoriaNueva();
        Producto producto = registrar(productoValido(categoria));

        productos.desactivar(producto.getId(), ADMINISTRADOR);
        categorias.desactivar(categoria.getId(), ADMINISTRADOR);
        assertThrows(IllegalArgumentException.class, () -> productos.activar(producto.getId(), ADMINISTRADOR));

        categorias.activar(categoria.getId(), ADMINISTRADOR);
        productos.activar(producto.getId(), ADMINISTRADOR);
        assertTrue(productos.obtener(producto.getId()).isEstado());
    }
}
