package pe.edu.pucp.sigmeta.ejecucion;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import pe.edu.pucp.sigmeta.bo.producto.CategoriaBO;
import pe.edu.pucp.sigmeta.bo.producto.ProductoBO;
import pe.edu.pucp.sigmeta.bo.ventas.CotizacionBO;
import pe.edu.pucp.sigmeta.boimpl.producto.CategoriaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.producto.ProductoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.CotizacionBOImpl;
import pe.edu.pucp.sigmeta.model.enums.UnidadMedida;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleCotizacion;

/**
 * Prueba del bloque del estudiante 2 (RF005, RF006, RF007) a traves de la capa de negocio
 * y contra la base de datos. Usa los datos de prueba del script sigmeta.sql
 * (productos INT-001 a INT-006, cliente 2, vendedor 1, administrador 4).
 */
public class PruebaCatalogo {

    public static DatosCatalogo ejecutar(Cliente cliente, Usuario vendedor, Usuario administrador) {
        System.out.println("== PRUEBA 2: catalogo y cotizacion (capa de negocio) ==");

        CategoriaBO categoriaBO = new CategoriaBOImpl();
        ProductoBO productoBO = new ProductoBOImpl();
        CotizacionBO cotizacionBO = new CotizacionBOImpl();

        // sufijo para que la prueba pueda correrse varias veces sin chocar con el UNIQUE de codigo_interno
        String sufijo = String.valueOf(System.currentTimeMillis() % 1_000_000);

        try {
            // RF006: el Administrador registra una categoria
            Categoria categoria = new Categoria();
            categoria.setNombre("Seguridad Industrial " + sufijo);
            categoria.setDescripcion("Guantes, cascos y lentes de proteccion");
            categoriaBO.registrar(categoria, administrador.getId());
            System.out.println("> Categoria registrada: " + categoria.getId() + " - " + categoria.getNombre());

            // RF005: el Administrador registra un producto con sus tres codigos
            Producto casco = new Producto();
            casco.setCodigoInterno("INT-T" + sufijo);
            casco.setCodigoFabricante("FAB-T" + sufijo);
            casco.setCodigoProveedor("PRV-T" + sufijo);
            casco.setNombre("Casco de Seguridad Dielectrico");
            casco.setDescripcion("Casco clase E con suspension de 4 puntos");
            casco.setCategoria(categoria);
            casco.setUnidadCompra(UnidadMedida.CAJA);
            casco.setUnidadVenta(UnidadMedida.UNIDAD);
            casco.setFactorConversion(12);
            casco.setPrecioVenta(38.90);
            casco.setCostoUnitario(22.50);
            casco.setStockMinimo(24);
            casco.setPrecioReferencial(42.00);
            productoBO.registrar(casco, administrador.getId());
            System.out.println("> Producto registrado: " + casco.getId() + " - " + casco.getNombre()
                    + " (stock inicial " + casco.getStockActual() + ")");

            // RF005: busqueda por cualquiera de los tres codigos (como lo haria el lector de barras)
            Producto leido = productoBO.buscarPorCodigo(casco.getCodigoFabricante());
            System.out.println("> Busqueda por codigo de fabricante " + casco.getCodigoFabricante()
                    + ": " + (leido != null ? leido.getNombre() : "no encontrado"));

            // RF012: el producto nuevo entra con stock 0, por debajo de su minimo
            boolean enStockBajo = productoBO.listarConStockBajo().stream()
                    .anyMatch(p -> p.getId() == casco.getId());
            System.out.println("> Aparece en el listado de stock bajo: " + enStockBajo);

            // validaciones de la capa de negocio
            intentar("Vendedor registra un producto (RF005: solo Administrador)",
                    () -> productoBO.registrar(casco, vendedor.getId()));
            intentar("Desactivar una categoria con productos activos (RF006)",
                    () -> categoriaBO.desactivar(categoria.getId(), administrador.getId()));

            // RF007: el Vendedor registra la cotizacion; el BO calcula importes, IGV y total
            Cotizacion cotizacion = new Cotizacion();
            cotizacion.setNumero("COT-" + sufijo);
            cotizacion.setFechaEmision(LocalDate.now());
            cotizacion.setFechaVigencia(LocalDate.now().plusDays(15));
            cotizacion.setCliente(cliente);
            cotizacion.setObservaciones("Precios sujetos a stock");
            cotizacion.getDetalles().add(linea(1, productoBO.buscarPorCodigo("INT-001"), 2, 5.00));
            cotizacion.getDetalles().add(linea(2, productoBO.buscarPorCodigo("INT-003"), 10, 0));
            cotizacion.getDetalles().add(linea(3, casco, 6, 0));
            cotizacionBO.registrar(cotizacion, vendedor.getId());
            System.out.println("> Cotizacion " + cotizacion.getNumero() + " registrada (id " + cotizacion.getId()
                    + "): subtotal S/ " + formato(cotizacion.getSubTotal())
                    + ", IGV S/ " + formato(cotizacion.getIgv())
                    + ", total S/ " + formato(cotizacion.getTotal())
                    + ", estado " + cotizacion.getEstado());

            intentar("Aceptar una cotizacion que aun no se envia (RF007)",
                    () -> cotizacionBO.aceptar(cotizacion.getId(), vendedor.getId()));

            // RF007: ciclo de vida REGISTRADA -> ENVIADA -> ACEPTADA
            cotizacionBO.enviar(cotizacion.getId(), vendedor.getId());
            cotizacionBO.aceptar(cotizacion.getId(), vendedor.getId());

            // RNF002: se lee de la base para comprobar que cabecera y detalle se guardaron juntos
            Cotizacion guardada = cotizacionBO.obtener(cotizacion.getId());
            System.out.println("> Cotizacion recuperada de la base: " + guardada.getNumero()
                    + " con " + guardada.getDetalles().size() + " lineas, total S/ "
                    + formato(guardada.getTotal()) + ", estado " + guardada.getEstado());
            for (DetalleCotizacion detalle : guardada.getDetalles()) {
                System.out.println("    linea " + detalle.getNumeroLinea() + ": producto " + detalle.getProducto().getId()
                        + " x " + detalle.getCantidad() + " = S/ " + formato(detalle.getImporte()));
            }
            System.out.println();

            List<Producto> productos = productoBO.listarTodos();
            return new DatosCatalogo(productos, guardada);
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo completar la prueba de catalogo contra la base de datos", e);
        }
    }

    private static DetalleCotizacion linea(int numero, Producto producto, double cantidad, double descuento) {
        if (producto == null) {
            throw new IllegalStateException("No se encontro el producto de la linea " + numero
                    + "; cargue los datos de prueba de sigmeta.sql");
        }
        DetalleCotizacion detalle = new DetalleCotizacion();
        detalle.setNumeroLinea(numero);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(producto.getPrecioVenta());
        detalle.setDescuento(descuento);
        return detalle;
    }

    // ejecuta una operacion que la capa de negocio debe rechazar y muestra el motivo
    private static void intentar(String caso, OperacionBO operacion) throws SQLException {
        try {
            operacion.ejecutar();
            System.out.println("  [!] " + caso + ": se permitio y no deberia");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [ok] " + caso + ": rechazado -> " + e.getMessage());
        }
    }

    private static String formato(double monto) {
        return String.format("%.2f", monto);
    }

    @FunctionalInterface
    private interface OperacionBO {
        void ejecutar() throws SQLException;
    }
}
