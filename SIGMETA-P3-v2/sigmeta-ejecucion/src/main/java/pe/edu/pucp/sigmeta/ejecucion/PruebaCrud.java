package pe.edu.pucp.sigmeta.ejecucion;

import pe.edu.pucp.sigmeta.bo.almacen.DespachoBO;
import pe.edu.pucp.sigmeta.bo.caja.CajaBO;
import pe.edu.pucp.sigmeta.bo.caja.CierreCajaBO;
import pe.edu.pucp.sigmeta.bo.ventas.ComprobanteBO;
import pe.edu.pucp.sigmeta.bo.compras.CompraBO;
import pe.edu.pucp.sigmeta.bo.socio.ClienteBO;
import pe.edu.pucp.sigmeta.bo.socio.ProveedorBO;
import pe.edu.pucp.sigmeta.bo.producto.CategoriaBO;
import pe.edu.pucp.sigmeta.bo.producto.ProductoBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.CotizacionBO;
import pe.edu.pucp.sigmeta.bo.ventas.VentaBO;
import pe.edu.pucp.sigmeta.boimpl.almacen.DespachoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.caja.CajaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.caja.CierreCajaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.ComprobanteBOImpl;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.DetalleDespacho;
import pe.edu.pucp.sigmeta.model.comun.DatosContacto;
import pe.edu.pucp.sigmeta.model.enums.EstadoCierreCaja;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.boimpl.compras.CompraBOImpl;
import pe.edu.pucp.sigmeta.boimpl.socio.ClienteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.socio.ProveedorBOImpl;
import pe.edu.pucp.sigmeta.boimpl.producto.CategoriaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.producto.ProductoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Evidencia de las operaciones CRUD de las 6 entidades principales del proyecto, ejecutadas a
 * traves de la capa de negocio (BO) contra la base de datos:
 * Cliente (est. 1), Producto y Cotizacion (est. 2), Compra (est. 3), Venta (est. 4) y Caja (est. 5).
 * Cada entidad se inserta, lista, obtiene, modifica y elimina (baja logica o anulacion, RNF002).
 * Despues de cada operacion se vuelve a leer de la BD y se verifica el resultado: si no coincide,
 * la prueba de esa entidad falla. La prueba 7 comprueba el rollback de una transaccion (RNF002).
 * Las pruebas 8 a 11 demuestran las correcciones del Laboratorio 4: razon social unica, despacho por
 * linea de venta, nota de credito sobre el precio con descuento y cierre de caja persistente.
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
    private static final CierreCajaBO cierreCajaBO = new CierreCajaBOImpl();
    private static final DespachoBO despachoBO = new DespachoBOImpl();
    private static final ComprobanteBO comprobanteBO = new ComprobanteBOImpl();

    // datos base tomados de la BD (sigmeta.sql)
    private static Usuario admin;
    private static Usuario vendedor;
    private static Usuario cajero;
    private static Usuario almacenero;
    private static Cliente clienteBase;
    private static Proveedor proveedorBase;
    private static Categoria categoriaBase;
    private static Producto productoBase;

    // sufijo para que la prueba pueda repetirse sin chocar con los campos unicos
    private static final long SUFIJO = System.currentTimeMillis() % 10_000_000L;

    // venta y despacho que comparten las pruebas 9 y 10
    private static Venta ventaFlujo;
    private static Despacho despachoFlujo;

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
        probar("8. RAZON SOCIAL UNICA (correccion Lab 4)", PruebaCrud::pruebaRazonSocialUnica);
        probar("9. DESPACHO POR LINEA DE VENTA (correccion Lab 4)", PruebaCrud::pruebaDespachoPorLinea);
        probar("10. NOTA DE CREDITO CON DESCUENTO (correccion Lab 4)", PruebaCrud::pruebaNotaCreditoConDescuento);
        probar("11. CIERRE Y REAPERTURA DE CAJA (correccion Lab 4)", PruebaCrud::pruebaReaperturaCaja);

        System.out.println("\n================================================================================");
        System.out.printf("          RESUMEN: %d PRUEBAS OK | %d CON ERROR (de 11)%n", exitos, fallos);
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
                if (almacenero == null && u.getRol().getTipo() == TipoRol.ALMACENERO) almacenero = u;
            }
            clienteBase = clienteBO.listarTodos().stream().findFirst().orElse(null);
            proveedorBase = proveedorBO.listarTodos().stream().findFirst().orElse(null);
            categoriaBase = categoriaBO.listarTodos().stream().findFirst().orElse(null);
            // producto con stock para poder venderlo
            productoBase = productoBO.listarTodos().stream()
                    .filter(p -> p.getStockActual() >= 5)
                    .findFirst().orElse(null);
        } catch (SQLException e) {
            System.err.println("   [ERROR] No se pudo leer la BD: " + e.getMessage());
            return false;
        }
        if (admin == null || vendedor == null || cajero == null || almacenero == null || clienteBase == null || proveedorBase == null
                || categoriaBase == null || productoBase == null) {
            System.err.println("   [ERROR] Faltan datos base: cargue sigmeta.sql (usuarios, cliente, proveedor, categoria y producto con stock).");
            return false;
        }
        System.out.printf("   Administrador: %s | Vendedor: %s | Cajero: %s | Cliente: %s | Proveedor: %s | Producto: %s%n",
                admin.getNombreUsuario(), vendedor.getNombreUsuario(), cajero.getNombreUsuario(), clienteBase.getNombreMostrado(),
                proveedorBase.getRazonSocial(), productoBase.getNombre());
        return true;
    }

    // ==========================================
    // 1. CLIENTE
    // ==========================================
    private static void crudCliente() throws SQLException {
        Cliente cliente = new Cliente();
        // persona natural: nombres y apellidos, sin razon social
        cliente.setNombres("Maria Elena");
        cliente.setApellidos("Prueba Crud");
        cliente.setTipoPersona(pe.edu.pucp.sigmeta.model.enums.TipoPersona.NATURAL);
        cliente.setTipoDocumento(TipoDocumentoIdentidad.DNI);
        cliente.setNumeroDocumento(String.format("7%07d", SUFIJO));
        cliente.setContacto(new DatosContacto("Av. Prueba 123, Lima", "987000111", "mprueba@correo.pe"));
        cliente.getContacto().setNombreContacto("Maria Prueba");
        clienteBO.registrar(cliente, vendedor.getId());
        ok("Insertar", cliente.getId() > 0, "cliente id " + cliente.getId() + ", DNI " + cliente.getNumeroDocumento());

        boolean listado = clienteBO.listarTodos().stream().anyMatch(c -> c.getId() == cliente.getId());
        ok("Listar", listado, "el cliente nuevo aparece entre los activos");

        Cliente leido = clienteBO.obtener(cliente.getId());
        ok("Obtener", leido != null && cliente.getNumeroDocumento().equals(leido.getNumeroDocumento())
                && leido.getCondicionPago() == CondicionPago.CONTADO && leido.getRazonSocial() == null,
                leido.getNombreMostrado() + " (" + leido.getCondicionPago() + ", sin razon social)");

        leido.getContacto().setDireccion("Jr. Actualizado 456, Lima");
        clienteBO.modificar(leido, vendedor.getId());
        String direccion = clienteBO.obtener(cliente.getId()).getContacto().getDireccion();
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
        cerrarCajaPrevia();

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

        // solo cuenta si falla la FK del detalle (MySQL 1452): cualquier otro error (una validacion
        // del BO o un parametro mal enviado) ocurriria antes y no probaria el rollback de la cabecera
        boolean falloPorDetalle = false;
        try {
            compraBO.registrar(nuevaCompra(numero, inexistente));
        } catch (SQLException e) {
            falloPorDetalle = e.getErrorCode() == 1452 && e.getMessage().contains("fk_detalle_compra_producto");
            System.out.println("   La operacion fallo: " + e.getMessage());
        }
        ok("Error", falloPorDetalle, "fallo la segunda operacion (detalle) por la FK del producto");

        boolean quedoCabecera = compraBO.listarTodos().stream().anyMatch(c -> numero.equals(c.getNumero()));
        ok("Rollback", !quedoCabecera, "la cabecera " + numero + " no quedo registrada = " + !quedoCabecera);
    }

    // RF010: un cajero no puede tener dos cajas abiertas; si quedo una de una ejecucion anterior
    // (u otra ejecucion simultanea con el mismo cajero), se cierra antes de abrir la nueva
    private static void cerrarCajaPrevia() throws SQLException {
        Caja previa = cajaBO.obtenerCajaAbiertaPorUsuario(cajero.getId());
        if (previa != null) {
            try {
                CierreCaja cierre = new CierreCaja();
                cierre.setCaja(previa);
                cierre.setMontoDeclarado(cajaBO.calcularMontoCalculado(previa.getId()));
                cajaBO.cerrarCaja(cierre, cajero.getId());
                System.out.println("   (se cerro la caja " + previa.getId() + " que el cajero tenia abierta)");
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("   (la caja " + previa.getId() + " ya no estaba abierta: " + e.getMessage() + ")");
            }
        }
    }

    // ==========================================
    // 8. RAZON SOCIAL UNICA
    // ==========================================
    // La razon social es UNIQUE en la tabla y el BO lo valida con un mensaje claro
    private static void pruebaRazonSocialUnica() throws SQLException {
        Proveedor duplicado = new Proveedor();
        duplicado.setRazonSocial(proveedorBase.getRazonSocial());
        duplicado.setRuc(String.format("20%09d", SUFIJO));
        duplicado.setCondicionPago(CondicionPago.CONTADO);
        boolean rechazado = false;
        try {
            proveedorBO.registrar(duplicado, admin.getId());
        } catch (IllegalArgumentException e) {
            rechazado = true;
            System.out.println("   Rechazado como se esperaba: " + e.getMessage());
        }
        ok("Proveedor", rechazado, "no se registra un segundo proveedor '" + proveedorBase.getRazonSocial() + "'");
        boolean noSeGuardo = proveedorBO.buscarPorRuc(duplicado.getRuc()) == null;
        ok("BD", noSeGuardo, "el proveedor duplicado no quedo en la base");
    }

    // ==========================================
    // 9. DESPACHO POR LINEA DE VENTA
    // ==========================================
    // Una venta con dos lineas del mismo producto: el despacho apunta a la linea concreta (FK a detalle_venta)
    private static void pruebaDespachoPorLinea() throws SQLException {
        double precio = productoBase.getPrecioVenta();
        Venta venta = new Venta();
        venta.setCliente(clienteBase);
        venta.setCondicionPago(CondicionPago.CONTADO);
        venta.setFechaEmision(LocalDate.now());
        venta.setNumero("VTA-L" + SUFIJO);
        venta.getDetalles().add(lineaVenta(1, productoBase, 1, precio, 0));
        venta.getDetalles().add(lineaVenta(2, productoBase, 2, precio, 2.00));
        ventaBO.registrar(venta, vendedor.getId());
        Venta leida = ventaBO.obtener(venta.getId());
        DetalleVenta linea1 = linea(leida, 1);
        DetalleVenta linea2 = linea(leida, 2);
        ok("Venta", linea1 != null && linea2 != null, leida.getNumero() + ": dos lineas del producto " + productoBase.getNombre());

        Despacho despacho = new Despacho();
        despacho.setVenta(leida);
        despacho.setDetalles(new ArrayList<>());
        DetalleDespacho dd = new DetalleDespacho();
        dd.setDetalleVenta(linea2);
        dd.setCantidadDespachada(2);
        despacho.getDetalles().add(dd);
        despachoBO.registrar(despacho, almacenero.getId());
        Venta trasDespacho = ventaBO.obtener(venta.getId());
        ok("Despacho", igual(linea(trasDespacho, 1).getCantidadDespachada(), 0) && igual(linea(trasDespacho, 2).getCantidadDespachada(), 2),
                "solo la linea 2 queda despachada (linea 1: " + linea(trasDespacho, 1).getCantidadDespachada()
                        + ", linea 2: " + linea(trasDespacho, 2).getCantidadDespachada() + ")");

        // la linea 1 vendio 1 unidad: despachar 2 de esa linea debe rechazarse aunque el producto tenga mas vendido
        Despacho exceso = new Despacho();
        exceso.setVenta(trasDespacho);
        exceso.setDetalles(new ArrayList<>());
        DetalleDespacho ddExceso = new DetalleDespacho();
        ddExceso.setDetalleVenta(linea(trasDespacho, 1));
        ddExceso.setCantidadDespachada(2);
        exceso.getDetalles().add(ddExceso);
        boolean rechazado = false;
        try {
            despachoBO.registrar(exceso, almacenero.getId());
        } catch (IllegalArgumentException e) {
            rechazado = true;
            System.out.println("   Rechazado como se esperaba: " + e.getMessage());
        }
        ok("Exceso", rechazado, "no se despacha mas de lo vendido en la linea 1");

        ventaFlujo = trasDespacho;
        despachoFlujo = despacho;
    }

    // ==========================================
    // 10. NOTA DE CREDITO CON DESCUENTO
    // ==========================================
    // La devolucion se calcula sobre el precio efectivamente cobrado en la linea (precio menos descuento)
    private static void pruebaNotaCreditoConDescuento() throws SQLException {
        if (ventaFlujo == null) {
            throw new IllegalStateException("Requiere la venta de la prueba 9");
        }
        TipoComprobante tipo = clienteBase.getTipoDocumento() == TipoDocumentoIdentidad.RUC ? TipoComprobante.FACTURA : TipoComprobante.BOLETA;
        Comprobante comprobante = comprobanteBO.emitir(ventaFlujo.getId(), tipo, vendedor.getId());
        ok("Comprobante", comprobante.getId() > 0, tipo + " " + comprobante.getSerie() + "-" + comprobante.getNumero());

        DetalleVenta linea2 = linea(ventaFlujo, 2);
        Comprobante nc = new Comprobante();
        nc.setComprobanteRelacionado(comprobante);
        nc.setMotivo("Devolucion de una unidad con descuento");
        nc.setDetalles(new ArrayList<>());
        DetalleNotaCredito dnc = new DetalleNotaCredito();
        dnc.setNumeroLinea(1);
        dnc.setDetalleVenta(linea2);
        dnc.setCantidad(1);
        nc.getDetalles().add(dnc);
        comprobanteBO.registrarNotaCredito(nc, admin.getId(), cajero.getId());

        Comprobante ncLeida = comprobanteBO.obtener(nc.getId());
        DetalleNotaCredito lineaNc = ncLeida.getDetalles().get(0);
        double esperado = redondear(linea2.getImporte() / linea2.getCantidad());
        ok("Importe", igual(lineaNc.getImporte(), esperado) && lineaNc.getDetalleVenta().getId() == linea2.getId(),
                "devuelve S/ " + formato(lineaNc.getImporte()) + " (precio S/ " + formato(linea2.getPrecioUnitario())
                        + " menos S/ " + formato(lineaNc.getDescuento()) + " de descuento), no el precio de lista");

        // la linea 2 vendio 2 y ya se acredito 1: devolver 2 mas debe rechazarse
        Comprobante exceso = new Comprobante();
        exceso.setComprobanteRelacionado(comprobante);
        exceso.setMotivo("Devolucion excesiva");
        exceso.setDetalles(new ArrayList<>());
        DetalleNotaCredito dncExceso = new DetalleNotaCredito();
        dncExceso.setNumeroLinea(1);
        dncExceso.setDetalleVenta(linea2);
        dncExceso.setCantidad(2);
        exceso.getDetalles().add(dncExceso);
        boolean rechazado = false;
        try {
            comprobanteBO.registrarNotaCredito(exceso, admin.getId(), cajero.getId());
        } catch (IllegalArgumentException e) {
            rechazado = true;
            System.out.println("   Rechazado como se esperaba: " + e.getMessage());
        }
        ok("Exceso", rechazado, "no se devuelve mas de lo vendido en la linea");

        // se revierte todo el flujo con anulaciones (stock y documentos vuelven a su estado)
        comprobanteBO.anular(nc.getId(), "Prueba de correcciones", admin.getId());
        despachoBO.anular(despachoFlujo.getId(), "Prueba de correcciones", almacenero.getId());
        comprobanteBO.anular(comprobante.getId(), "Prueba de correcciones", admin.getId());
        ventaBO.anular(ventaFlujo.getId(), "Prueba de correcciones", vendedor.getId());
        ok("Anulacion", ventaBO.obtener(ventaFlujo.getId()).isAnulado(), "NC, despacho, comprobante y venta anulados (baja logica)");
    }

    // ==========================================
    // 11. CIERRE Y REAPERTURA DE CAJA
    // ==========================================
    // El cierre nunca se elimina: reabrir lo deja REVERTIDO y un nuevo cierre crea otro registro
    private static void pruebaReaperturaCaja() throws SQLException {
        cerrarCajaPrevia();
        Caja caja = new Caja();
        caja.setMontoInicial(150.00);
        cajaBO.abrirCaja(caja, cajero.getId());

        CierreCaja primero = new CierreCaja();
        primero.setCaja(caja);
        primero.setMontoDeclarado(150.00);
        cajaBO.cerrarCaja(primero, cajero.getId());
        ok("Cierre", !cajaBO.obtenerPorId(caja.getId()).isAbierta() && primero.getEstado() == EstadoCierreCaja.VIGENTE,
                "caja " + caja.getId() + " cerrada con el cierre " + primero.getId());

        CierreCaja revertido = cierreCajaBO.reabrir(primero.getId(), "Se encontro un vale sin registrar", admin.getId());
        ok("Reapertura", revertido.getEstado() == EstadoCierreCaja.REVERTIDO && cajaBO.obtenerPorId(caja.getId()).isAbierta()
                        && revertido.getUsuarioReapertura().getId() == admin.getId(),
                "el cierre " + revertido.getId() + " queda " + revertido.getEstado() + " (no se borra) y la caja vuelve a abrirse");

        CierreCaja segundo = new CierreCaja();
        segundo.setCaja(caja);
        segundo.setMontoDeclarado(150.00);
        cajaBO.cerrarCaja(segundo, cajero.getId());
        List<CierreCaja> historial = cierreCajaBO.listarPorCaja(caja.getId());
        long vigentes = historial.stream().filter(c -> c.getEstado() == EstadoCierreCaja.VIGENTE).count();
        long revertidos = historial.stream().filter(c -> c.getEstado() == EstadoCierreCaja.REVERTIDO).count();
        ok("Historial", historial.size() == 2 && vigentes == 1 && revertidos == 1
                        && cajaBO.obtenerCierrePorCaja(caja.getId()).getId() == segundo.getId(),
                "la caja conserva " + historial.size() + " cierres: " + vigentes + " vigente y " + revertidos + " revertido");
    }

    private static DetalleVenta lineaVenta(int numero, Producto producto, double cantidad, double precio, double descuento) {
        DetalleVenta d = new DetalleVenta();
        d.setNumeroLinea(numero);
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio);
        d.setDescuento(descuento);
        return d;
    }

    private static DetalleVenta linea(Venta venta, int numeroLinea) {
        return venta.getDetalles().stream().filter(d -> d.getNumeroLinea() == numeroLinea).findFirst().orElse(null);
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
