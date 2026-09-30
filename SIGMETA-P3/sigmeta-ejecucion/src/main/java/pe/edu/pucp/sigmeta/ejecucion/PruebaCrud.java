package pe.edu.pucp.sigmeta.ejecucion;

import pe.edu.pucp.sigmeta.bo.caja.CajaBO;
import pe.edu.pucp.sigmeta.bo.compras.CompraBO;
import pe.edu.pucp.sigmeta.bo.maestros.ClienteBO;
import pe.edu.pucp.sigmeta.bo.maestros.ProveedorBO;
import pe.edu.pucp.sigmeta.bo.producto.CategoriaBO;
import pe.edu.pucp.sigmeta.bo.producto.ProductoBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.CotizacionBO;
import pe.edu.pucp.sigmeta.bo.ventas.VentaBO;
import pe.edu.pucp.sigmeta.boimpl.caja.CajaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.compras.CompraBOImpl;
import pe.edu.pucp.sigmeta.boimpl.maestros.ClienteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.maestros.ProveedorBOImpl;
import pe.edu.pucp.sigmeta.boimpl.producto.CategoriaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.producto.ProductoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.CotizacionBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.VentaBOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCotizacion;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.enums.TipoDocumentoIdentidad;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.enums.UnidadMedida;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleCotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Evidencia de las operaciones CRUD de las 6 entidades principales del proyecto, ejecutadas a
 * traves de la capa de negocio (BO) contra la base de datos:
 * Cliente (est. 1), Producto y Cotizacion (est. 2), Compra (est. 3), Venta (est. 4) y Caja (est. 5).
 * Cada entidad se inserta, lista, obtiene, modifica y elimina (baja logica o anulacion, RNF002).
 * Despues de cada operacion se vuelve a leer de la BD y se verifica el resultado: si no coincide,
 * la prueba de esa entidad falla. La prueba 7 comprueba el rollback de una transaccion (RNF002).
 * Requiere la base cargada con sigmeta.sql y los procedimientos de sql_sp.
 */
public class PruebaCrud {

    private static final UsuarioBO usuarioBO = new UsuarioBOImpl();
    private static final ClienteBO clienteBO = new ClienteBOImpl();
    private static final ProveedorBO proveedorBO = new ProveedorBOImpl();
    private static final CategoriaBO categoriaBO = new CategoriaBOImpl();
    private static final ProductoBO productoBO = new ProductoBOImpl();
    private static final CotizacionBO cotizacionBO = new CotizacionBOImpl();
    private static final CompraBO compraBO = new CompraBOImpl();
    private static final VentaBO ventaBO = new VentaBOImpl();
    private static final CajaBO cajaBO = new CajaBOImpl();

    // datos base tomados de la BD (sigmeta.sql)
    private static Usuario admin;
    private static Usuario vendedor;
    private static Usuario cajero;
    private static Cliente clienteBase;
    private static Proveedor proveedorBase;
    private static Categoria categoriaBase;
    private static Producto productoBase;

    // sufijo para que la prueba pueda repetirse sin chocar con los campos unicos
    private static final long SUFIJO = System.currentTimeMillis() % 10_000_000L;

    private static int exitos = 0;
    private static int fallos = 0;

    public static void ejecutar() {
        System.out.println("================================================================================");
        System.out.println("            PRUEBAS CRUD DE 6 ENTIDADES (CAPA DE NEGOCIO + BASE DE DATOS)        ");
        System.out.println("================================================================================");

        if (!prepararDatosBase()) {
            return;
        }

        probar("1. CLIENTE (RF003) - estudiante 1", PruebaCrud::crudCliente);
        probar("2. PRODUCTO (RF005) - estudiante 2", PruebaCrud::crudProducto);
        probar("3. COTIZACION (RF007) - estudiante 2", PruebaCrud::crudCotizacion);
        probar("4. COMPRA A PROVEEDOR (RF011) - estudiante 3", PruebaCrud::crudCompra);
        probar("5. VENTA (RF008) - estudiante 4", PruebaCrud::crudVenta);
        probar("6. CAJA (RF010) - estudiante 5", PruebaCrud::crudCaja);
        probar("7. ROLLBACK DE UNA TRANSACCION (RNF002)", PruebaCrud::pruebaRollback);

        System.out.println("\n================================================================================");
        System.out.printf("          RESUMEN: %d PRUEBAS OK | %d CON ERROR (de 7)%n", exitos, fallos);
        System.out.println("================================================================================");
    }

    // ==========================================
    // 0. DATOS BASE
    // ==========================================
    private static boolean prepararDatosBase() {
        System.out.println("\n[0] Datos base desde la BD");
        try {
            for (Usuario u : usuarioBO.listarTodos()) {
                if (admin == null && u.getRol().getTipo() == TipoRol.ADMINISTRADOR) admin = u;
                if (vendedor == null && u.getRol().getTipo() == TipoRol.VENDEDOR) vendedor = u;
                if (cajero == null && u.getRol().getTipo() == TipoRol.CAJERO) cajero = u;
            }
            clienteBase = clienteBO.listarTodos().stream().findFirst().orElse(null);
            proveedorBase = proveedorBO.listarTodos().stream().findFirst().orElse(null);
            categoriaBase = categoriaBO.listarTodos().stream().findFirst().orElse(null);
            // producto con stock para poder venderlo
            productoBase = productoBO.listarTodos().stream()
                    .filter(p -> p.getStockActual() >= 2)
                    .findFirst().orElse(null);
        } catch (SQLException e) {
            System.err.println("   [ERROR] No se pudo leer la BD: " + e.getMessage());
            return false;
        }
        if (admin == null || vendedor == null || cajero == null || clienteBase == null || proveedorBase == null
                || categoriaBase == null || productoBase == null) {
            System.err.println("   [ERROR] Faltan datos base: cargue sigmeta.sql (usuarios, cliente, proveedor, categoria y producto con stock).");
            return false;
        }
        System.out.printf("   Administrador: %s | Vendedor: %s | Cajero: %s | Cliente: %s | Proveedor: %s | Producto: %s%n",
                admin.getNombreUsuario(), vendedor.getNombreUsuario(), cajero.getNombreUsuario(), clienteBase.getRazonSocial(),
                proveedorBase.getRazonSocial(), productoBase.getNombre());
        return true;
    }

    // ==========================================
    // 1. CLIENTE
    // ==========================================
    private static void crudCliente() throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setRazonSocial("Maria Elena Prueba Crud");
        cliente.setTipoDocumento(TipoDocumentoIdentidad.DNI);
        cliente.setNumeroDocumento(String.format("7%07d", SUFIJO));
        cliente.setDireccion("Av. Prueba 123, Lima");
        cliente.setTelefono("987000111");
        cliente.setCorreo("mprueba@correo.pe");
        cliente.setContactoNombre("Maria Prueba");
        clienteBO.registrar(cliente, vendedor.getId());
        ok("Insertar", cliente.getId() > 0, "cliente id " + cliente.getId() + ", DNI " + cliente.getNumeroDocumento());

        boolean listado = clienteBO.listarTodos().stream().anyMatch(c -> c.getId() == cliente.getId());
        ok("Listar", listado, "el cliente nuevo aparece entre los activos");

        Cliente leido = clienteBO.obtener(cliente.getId());
        ok("Obtener", leido != null && cliente.getNumeroDocumento().equals(leido.getNumeroDocumento())
                && leido.getCondicionPago() == CondicionPago.CONTADO,
                leido.getRazonSocial() + " (" + leido.getCondicionPago() + ")");

        leido.setDireccion("Jr. Actualizado 456, Lima");
        clienteBO.modificar(leido, vendedor.getId());
        String direccion = clienteBO.obtener(cliente.getId()).getDireccion();
        ok("Modificar", "Jr. Actualizado 456, Lima".equals(direccion), "direccion = " + direccion);

        clienteBO.desactivar(cliente.getId(), vendedor.getId());
        boolean activo = clienteBO.obtener(cliente.getId()).isEstado();
        ok("Eliminar", !activo, "baja logica, activo = " + activo);
    }

    // ==========================================
    // 2. PRODUCTO
    // ==========================================
    private static void crudProducto() throws SQLException {
        Producto producto = new Producto();
        producto.setCodigoInterno("INT-C" + SUFIJO);
        producto.setCodigoFabricante("FAB-C" + SUFIJO);
        producto.setCodigoProveedor("PRV-C" + SUFIJO);
        producto.setNombre("Guante Anticorte Prueba Crud");
        producto.setDescripcion("Guante nivel 5");
        producto.setCategoria(categoriaBase);
        producto.setUnidadCompra(UnidadMedida.CAJA);
        producto.setUnidadVenta(UnidadMedida.UNIDAD);
        producto.setFactorConversion(12);
        producto.setPrecioVenta(18.90);
        producto.setCostoUnitario(9.50);
        producto.setStockMinimo(24);
        producto.setPrecioReferencial(20.00);
        productoBO.registrar(producto, admin.getId());
        ok("Insertar", producto.getId() > 0, "producto id " + producto.getId() + ", codigo " + producto.getCodigoInterno());

        boolean listado = productoBO.listarTodos().stream().anyMatch(p -> p.getId() == producto.getId());
        ok("Listar", listado, "el producto nuevo aparece entre los activos");

        Producto leido = productoBO.obtener(producto.getId());
        // RF012: el producto nuevo entra con stock 0; el stock solo lo mueven los movimientos de almacen
        ok("Obtener", leido != null && igual(leido.getPrecioVenta(), 18.90) && igual(leido.getStockActual(), 0)
                        && leido.getCategoria().getId() == categoriaBase.getId(),
                leido.getNombre() + ", precio S/ " + formato(leido.getPrecioVenta()) + ", stock " + leido.getStockActual());

        leido.setPrecioVenta(17.50);
        productoBO.modificar(leido, admin.getId());
        double precio = productoBO.obtener(producto.getId()).getPrecioVenta();
        ok("Modificar", igual(precio, 17.50), "precio = S/ " + formato(precio));

        productoBO.desactivar(producto.getId(), admin.getId());
        boolean activo = productoBO.obtener(producto.getId()).isEstado();
        ok("Eliminar", !activo, "baja logica, activo = " + activo);
    }

    // ==========================================
    // 3. COTIZACION (cabecera + detalle en una transaccion)
    // ==========================================
    private static void crudCotizacion() throws SQLException {
        double precio = productoBase.getPrecioVenta();
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setNumero("COT-C" + SUFIJO);
        cotizacion.setFechaEmision(LocalDate.now());
        cotizacion.setFechaVigencia(LocalDate.now().plusDays(15));
        cotizacion.setCliente(clienteBase);
        DetalleCotizacion detalle = new DetalleCotizacion();
        detalle.setNumeroLinea(1);
        detalle.setProducto(productoBase);
        detalle.setCantidad(3);
        detalle.setPrecioUnitario(precio);
        detalle.setDescuento(0);
        cotizacion.getDetalles().add(detalle);
        cotizacionBO.registrar(cotizacion, vendedor.getId());
        // RF007: el BO calcula subtotal, IGV (18%) y total
        ok("Insertar", cotizacion.getId() > 0 && igual(cotizacion.getSubTotal(), redondear(3 * precio))
                        && igual(cotizacion.getTotal(), redondear(cotizacion.getSubTotal() + cotizacion.getIgv())),
                cotizacion.getNumero() + " (id " + cotizacion.getId() + "), total S/ " + formato(cotizacion.getTotal()));

        boolean listada = cotizacionBO.listarTodos().stream().anyMatch(c -> c.getId() == cotizacion.getId());
        ok("Listar", listada, "la cotizacion nueva aparece entre las vigentes");

        Cotizacion leida = cotizacionBO.obtener(cotizacion.getId());
        ok("Obtener", leida != null && leida.getDetalles().size() == 1 && leida.getEstado() == EstadoCotizacion.REGISTRADA,
                leida.getNumero() + " con " + leida.getDetalles().size() + " linea(s), estado " + leida.getEstado());

        leida.getDetalles().get(0).setCantidad(5);
        cotizacionBO.modificar(leida, vendedor.getId());
        Cotizacion modificada = cotizacionBO.obtener(cotizacion.getId());
        ok("Modificar", igual(modificada.getSubTotal(), redondear(5 * precio)),
                "cantidad 3 -> 5, subtotal = S/ " + formato(modificada.getSubTotal()) + ", total = S/ " + formato(modificada.getTotal()));

        cotizacionBO.anular(cotizacion.getId(), "Prueba CRUD", vendedor.getId());
        Cotizacion anulada = cotizacionBO.obtener(cotizacion.getId());
        ok("Eliminar", anulada.isAnulado() && anulada.getEstado() == EstadoCotizacion.ANULADA,
                "anulacion logica, estado = " + anulada.getEstado());
    }

    // ==========================================
    // 4. COMPRA A PROVEEDOR (cabecera + detalle en una transaccion)
    // ==========================================
    private static void crudCompra() throws SQLException {
        Compra compra = nuevaCompra("OC-C" + SUFIJO, productoBase);
        compraBO.registrar(compra);
        ok("Insertar", compra.getId() > 0, compra.getNumero() + " (id " + compra.getId() + "), total S/ " + formato(compra.getTotal()));

        boolean listada = compraBO.listarTodos().stream().anyMatch(c -> c.getId() == compra.getId());
        ok("Listar", listada, "la compra nueva aparece en el listado");

        Compra leida = compraBO.obtener(compra.getId());
        ok("Obtener", leida != null && compra.getNumero().equals(leida.getNumero()) && leida.getEstado() == EstadoCompra.REGISTRADA,
                leida.getNumero() + ", estado " + leida.getEstado());

        leida.setObservaciones("Entregar en almacen principal");
        leida.setUsuarioRegistro(admin);
        compraBO.modificar(leida, admin.getId());
        String observaciones = compraBO.obtener(compra.getId()).getObservaciones();
        ok("Modificar", "Entregar en almacen principal".equals(observaciones), "observaciones = " + observaciones);

        compraBO.anular(compra.getId(), admin.getId());
        Compra anulada = compraBO.obtener(compra.getId());
        ok("Eliminar", anulada.isAnulado() && anulada.getEstado() == EstadoCompra.ANULADA,
                "anulacion logica, estado = " + anulada.getEstado());
    }

    // ==========================================
    // 5. VENTA (cabecera + detalle en una transaccion)
    // ==========================================
    private static void crudVenta() throws SQLException {
        double precio = productoBase.getPrecioVenta();
        Venta venta = new Venta();
        venta.setCliente(clienteBase);
        venta.setCondicionPago(CondicionPago.CONTADO);
        venta.setFechaEmision(LocalDate.now());
        venta.setMoneda(Moneda.SOLES);
        venta.setNumero("VTA-C" + SUFIJO);
        DetalleVenta detalle = new DetalleVenta();
        detalle.setNumeroLinea(1);
        detalle.setProducto(productoBase);
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(precio);
        detalle.setDescuento(0);
        venta.getDetalles().add(detalle);
        ventaBO.registrar(venta, vendedor.getId());
        ok("Insertar", venta.getId() > 0 && igual(venta.getSubTotal(), redondear(precio)),
                venta.getNumero() + " (id " + venta.getId() + "), total S/ " + formato(venta.getTotal()));

        boolean listada = ventaBO.listarTodos().stream().anyMatch(v -> v.getId() == venta.getId());
        ok("Listar", listada, "la venta nueva aparece en el listado");

        Venta leida = ventaBO.obtener(venta.getId());
        ok("Obtener", leida != null && leida.getDetalles().size() == 1 && leida.getEstado() == EstadoVenta.REGISTRADA,
                leida.getNumero() + " con " + leida.getDetalles().size() + " linea(s), estado " + leida.getEstado());

        leida.setObservaciones("Cliente recoge en tienda");
        leida.getDetalles().get(0).setCantidad(2);
        ventaBO.modificar(leida, vendedor.getId());
        Venta modificada = ventaBO.obtener(venta.getId());
        ok("Modificar", igual(modificada.getSubTotal(), redondear(2 * precio)),
                "cantidad 1 -> 2, subtotal = S/ " + formato(modificada.getSubTotal()) + ", total = S/ " + formato(modificada.getTotal()));

        ventaBO.anular(venta.getId(), "Prueba CRUD", vendedor.getId());
        Venta anulada = ventaBO.obtener(venta.getId());
        ok("Eliminar", anulada.isAnulado() && anulada.getEstado() == EstadoVenta.ANULADA,
                "anulacion logica, estado = " + anulada.getEstado());
    }

    // ==========================================
    // 6. CAJA
    // ==========================================
    private static void crudCaja() throws SQLException {
        // RF010: un cajero no puede tener dos cajas abiertas; si quedo una de una ejecucion anterior, se cierra
        Caja previa = cajaBO.obtenerCajaAbiertaPorUsuario(cajero.getId());
        if (previa != null) {
            try {
                CierreCaja cierre = new CierreCaja();
                cierre.setCaja(previa);
                cierre.setMontoDeclarado(cajaBO.calcularMontoCalculado(previa.getId()));
                cajaBO.cerrarCaja(cierre, cajero.getId());
                System.out.println("   (se cerro la caja " + previa.getId() + " que el cajero tenia abierta)");
            } catch (IllegalArgumentException | IllegalStateException e) {
                // otra ejecucion simultanea con el mismo cajero la cerro o la elimino primero
                System.out.println("   (la caja " + previa.getId() + " ya no estaba abierta: " + e.getMessage() + ")");
            }
        }

        Caja caja = new Caja();
        caja.setMontoInicial(300.00);
        cajaBO.abrirCaja(caja, cajero.getId());
        ok("Insertar", caja.getId() > 0 && caja.isAbierta(),
                "caja id " + caja.getId() + " abierta por " + cajero.getNombreUsuario() + " con S/ " + formato(caja.getMontoInicial()));

        boolean listada = cajaBO.listarTodasConUsuario().stream().anyMatch(c -> c.getId() == caja.getId());
        ok("Listar", listada, "la caja nueva aparece en el listado");

        Caja leida = cajaBO.obtenerPorId(caja.getId());
        ok("Obtener", leida != null && leida.isAbierta() && igual(leida.getMontoInicial(), 300.00)
                        && leida.getUsuarioApertura().getId() == cajero.getId(),
                "caja " + leida.getId() + ", abierta = " + leida.isAbierta() + ", monto inicial S/ " + formato(leida.getMontoInicial()));

        leida.setMontoInicial(350.00);
        cajaBO.modificar(leida, cajero.getId());
        double monto = cajaBO.obtenerPorId(caja.getId()).getMontoInicial();
        ok("Modificar", igual(monto, 350.00), "monto inicial = S/ " + formato(monto));

        // una apertura sin movimientos ni cierre se puede eliminar (RF010)
        cajaBO.eliminar(leida, cajero.getId());
        boolean existe = cajaBO.obtenerPorId(caja.getId()) != null;
        ok("Eliminar", !existe, "apertura sin movimientos eliminada, existe = " + existe);
    }

    // ==========================================
    // 7. ROLLBACK (RNF002)
    // ==========================================
    // Una compra cuya linea apunta a un producto inexistente: la cabecera se inserta, la linea falla
    // por la FK y el BO hace rollback. La compra no debe quedar guardada a medias.
    private static void pruebaRollback() throws SQLException {
        Producto inexistente = new Producto();
        inexistente.setId(999_999);
        inexistente.setUnidadCompra(UnidadMedida.UNIDAD);
        inexistente.setFactorConversion(1);
        inexistente.setCostoUnitario(10.00);
        String numero = "OC-R" + SUFIJO;

        boolean fallo = false;
        try {
            compraBO.registrar(nuevaCompra(numero, inexistente));
        } catch (SQLException | RuntimeException e) {
            fallo = true;
            System.out.println("   La operacion fallo como se esperaba: " + e.getMessage());
        }
        ok("Error", fallo, "la segunda operacion de la transaccion (detalle) fallo");

        boolean quedoCabecera = compraBO.listarTodos().stream().anyMatch(c -> numero.equals(c.getNumero()));
        ok("Rollback", !quedoCabecera, "la cabecera " + numero + " no quedo registrada = " + !quedoCabecera);
    }

    // ==========================================
    // utilitarios
    // ==========================================
    private static Compra nuevaCompra(String numero, Producto producto) {
        double precio = producto.getCostoUnitario() > 0 ? producto.getCostoUnitario() : 10.00;
        double importe = redondear(10 * precio);
        double igv = redondear(importe * 0.18);

        Compra compra = new Compra();
        compra.setProveedor(proveedorBase);
        compra.setNumero(numero);
        compra.setFechaEmision(LocalDate.now());
        compra.setFechaRecepcionEstimada(LocalDate.now().plusDays(5));
        compra.setMoneda(Moneda.SOLES);
        compra.setSubTotal(importe);
        compra.setIgv(igv);
        compra.setTotal(redondear(importe + igv));
        compra.setUsuarioRegistro(admin);
        compra.setEstado(EstadoCompra.REGISTRADA);
        DetalleCompra detalle = new DetalleCompra();
        detalle.setNumeroLinea(1);
        detalle.setProducto(producto);
        detalle.setCantidad(10);
        detalle.setPrecioUnitario(precio);
        detalle.setDescuento(0);
        detalle.setImporte(importe);
        detalle.setUnidadCompra(producto.getUnidadCompra());
        detalle.setFactorConversion(producto.getFactorConversion());
        detalle.setCantidadRecibida(0);
        compra.getDetalles().add(detalle);
        return compra;
    }

    private static void probar(String titulo, Paso paso) {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(titulo);
        System.out.println("--------------------------------------------------------------------------------");
        try {
            paso.ejecutar();
            exitos++;
        } catch (Exception e) {
            System.err.println("   [ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            fallos++;
        }
    }

    // imprime el resultado y detiene la prueba de la entidad si la verificacion no se cumple
    private static void ok(String operacion, boolean verificacion, String detalle) {
        if (!verificacion) {
            throw new IllegalStateException("Verificacion fallida en " + operacion + ": " + detalle);
        }
        System.out.printf("   [OK] %-9s -> %s%n", operacion, detalle);
    }

    private static boolean igual(double a, double b) {
        return Math.abs(a - b) < 0.005;
    }

    private static double redondear(double monto) {
        return Math.round(monto * 100) / 100.0;
    }

    private static String formato(double monto) {
        return String.format("%.2f", monto);
    }

    @FunctionalInterface
    private interface Paso {
        void ejecutar() throws SQLException;
    }
}
