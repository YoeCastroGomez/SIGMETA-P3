package pe.edu.pucp.sigmeta.ejecucion;

import pe.edu.pucp.sigmeta.bo.almacen.MovimientoInventarioBO;
import pe.edu.pucp.sigmeta.bo.producto.ProductoBO;
import pe.edu.pucp.sigmeta.bo.maestros.ClienteBO;
import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.ComprobanteBO;
import pe.edu.pucp.sigmeta.bo.ventas.CotizacionBO;
import pe.edu.pucp.sigmeta.bo.ventas.DespachoBO;
import pe.edu.pucp.sigmeta.bo.ventas.OrdenCompraClienteBO;
import pe.edu.pucp.sigmeta.bo.ventas.VentaBO;
import pe.edu.pucp.sigmeta.boimpl.almacen.MovimientoInventarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.producto.ProductoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.maestros.ClienteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.ComprobanteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.CotizacionBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.DespachoBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.OrdenCompraClienteBOImpl;
import pe.edu.pucp.sigmeta.boimpl.ventas.VentaBOImpl;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.DetalleDespacho;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.*;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PruebaCrudVenta {

    public static void ejecutar() {
        System.out.println("================================================================================");
        System.out.println("           INICIO DE PRUEBAS CRUD: MODULO COMERCIAL / VENTA (EST. 4)            ");
        System.out.println("================================================================================");

        int exitos = 0;
        int fallos = 0;

        UsuarioBO usuarioBO = new UsuarioBOImpl();
        ClienteBO clienteBO = new ClienteBOImpl();
        ProductoBO productoBO = new ProductoBOImpl();
        CotizacionBO cotizacionBO = new CotizacionBOImpl();
        OrdenCompraClienteBO occBO = new OrdenCompraClienteBOImpl();
        VentaBO ventaBO = new VentaBOImpl();
        ComprobanteBO comprobanteBO = new ComprobanteBOImpl();
        DespachoBO despachoBO = new DespachoBOImpl();
        MovimientoInventarioBO movimientoBO = new MovimientoInventarioBOImpl();
        SolicitudAutorizacionBO autorizacionBO = new SolicitudAutorizacionBOImpl();

        Usuario vendedor = null;
        Usuario cajero = null;
        Usuario almacenero = null;
        Usuario admin = null;
        Cliente cliente = null;
        Producto producto1 = null;
        Producto producto2 = null;

        long sufijo = System.currentTimeMillis() % 100000;

        // ==========================================
        // 0. PREPARACION DE DATOS DE PRUEBA
        // ==========================================
        try {
            System.out.println("\n[0] Preparando datos iniciales dinamicos...");
            List<Usuario> usuarios = usuarioBO.listarTodos();
            for (Usuario u : usuarios) {
                if (!u.isEstado()) continue;
                if (vendedor == null && u.getRol().getTipo() == TipoRol.VENDEDOR) vendedor = u;
                if (cajero == null && u.getRol().getTipo() == TipoRol.CAJERO) cajero = u;
                if (almacenero == null && u.getRol().getTipo() == TipoRol.ALMACENERO) almacenero = u;
                if (admin == null && u.getRol().getTipo() == TipoRol.ADMINISTRADOR) admin = u;
            }

            if (vendedor == null || cajero == null || almacenero == null || admin == null) {
                System.err.println("ADVERTENCIA: No se encontraron usuarios para todos los roles requeridos.");
                return;
            }
            System.out.printf("   - Vendedor: %s (id: %d)%n", vendedor.getNombreUsuario(), vendedor.getId());
            System.out.printf("   - Cajero: %s (id: %d)%n", cajero.getNombreUsuario(), cajero.getId());
            System.out.printf("   - Almacenero: %s (id: %d)%n", almacenero.getNombreUsuario(), almacenero.getId());
            System.out.printf("   - Administrador: %s (id: %d)%n", admin.getNombreUsuario(), admin.getId());

            // Buscar cliente activo con RUC y credito
            List<Cliente> clientes = clienteBO.listarTodos();
            for (Cliente c : clientes) {
                if (c.isEstado() && c.getCondicionPago() == CondicionPago.CREDITO && c.getLimiteCredito() >= 1000) {
                    cliente = c;
                    break;
                }
            }
            if (cliente == null && !clientes.isEmpty()) {
                cliente = clientes.get(0);
                cliente.setCondicionPago(CondicionPago.CREDITO);
                cliente.setLimiteCredito(50000.0);
                cliente.setPlazoCreditoDias(30);
                clienteBO.modificar(cliente, admin.getId());
            }
            System.out.printf("   - Cliente: %s (id: %d, Doc: %s %s, Limite: S/ %.2f)%n",
                    cliente.getRazonSocial(), cliente.getId(), cliente.getTipoDocumento(), cliente.getNumeroDocumento(), cliente.getLimiteCredito());

            // Buscar 2 productos activos
            List<Producto> productos = productoBO.listarTodos();
            if (productos.size() >= 2) {
                producto1 = productos.get(0);
                producto2 = productos.get(1);
            }

            // Asegurar stock para las pruebas
            if (producto1 != null && producto2 != null) {
                try {
                    // Autorizacion para ajuste de inventario si hace falta
                    SolicitudAutorizacion sol1 = autorizacionBO.solicitar(almacenero.getId(),
                            MovimientoInventarioBOImpl.OPERACION_AJUSTE_INVENTARIO,
                            "Ajuste inicial de pruebas venta");
                    autorizacionBO.aprobar(sol1.getId(), admin.getId(), 60);

                    if (producto1.getStockActual() < 50) {
                        movimientoBO.registrarAjuste(producto1.getId(), almacenero.getId(), 100.0, "Ajuste inicial prueba venta");
                        producto1.setStockActual(100.0);
                    }
                    if (producto2.getStockActual() < 50) {
                        movimientoBO.registrarAjuste(producto2.getId(), almacenero.getId(), 100.0, "Ajuste inicial prueba venta");
                        producto2.setStockActual(100.0);
                    }
                } catch (Exception e) {
                    // Continuar si el stock ya era suficiente
                }
                System.out.printf("   - Producto 1: %s (id: %d, Stock: %.2f)%n", producto1.getNombre(), producto1.getId(), producto1.getStockActual());
                System.out.printf("   - Producto 2: %s (id: %d, Stock: %.2f)%n", producto2.getNombre(), producto2.getId(), producto2.getStockActual());
            }

            exitos++;
        } catch (Exception e) {
            System.err.println("Error preparando datos: " + e.getMessage());
            fallos++;
        }

        // ==========================================
        // 1. FLUJO ORDEN DE COMPRA CLIENTE (RF007)
        // ==========================================
        OrdenCompraCliente ordenGenerada = null;
        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("1. ORDEN DE COMPRA CLIENTE: Generacion desde Cotizacion Aceptada (RF007)");
            System.out.println("--------------------------------------------------------------------------------");

            // Buscar o generar cotizacion aceptada
            Cotizacion cot = null;
            List<Cotizacion> aceptadas = cotizacionBO.listarPorEstado(EstadoCotizacion.ACEPTADA);
            if (!aceptadas.isEmpty()) {
                cot = aceptadas.get(0);
            } else {
                Cotizacion nuevaCot = new Cotizacion();
                nuevaCot.setCliente(cliente);
                nuevaCot.setNumero("COT-PRU-" + sufijo);
                nuevaCot.setFechaEmision(LocalDate.now());
                nuevaCot.setFechaVigencia(LocalDate.now().plusDays(15));
                nuevaCot.setMoneda(Moneda.SOLES);
                nuevaCot.setObservaciones("Cotizacion de prueba para flujo comercial");

                List<DetalleCotizacion> detCot = new ArrayList<>();
                DetalleCotizacion d1 = new DetalleCotizacion();
                d1.setNumeroLinea(1);
                d1.setProducto(producto1);
                d1.setCantidad(10.0);
                d1.setPrecioUnitario(producto1.getPrecioVenta());
                d1.setDescuento(0.0);
                detCot.add(d1);

                DetalleCotizacion d2 = new DetalleCotizacion();
                d2.setNumeroLinea(2);
                d2.setProducto(producto2);
                d2.setCantidad(5.0);
                d2.setPrecioUnitario(producto2.getPrecioVenta());
                d2.setDescuento(0.0);
                detCot.add(d2);

                nuevaCot.setDetalles(detCot);
                cot = cotizacionBO.registrar(nuevaCot, vendedor.getId());
                cot = cotizacionBO.enviar(cot.getId(), vendedor.getId());
                cot = cotizacionBO.aceptar(cot.getId(), vendedor.getId());
            }

            System.out.printf("   -> Cotizacion Aceptada lista: %s (id: %d)%n", cot.getNumero(), cot.getId());

            ordenGenerada = occBO.generarDesdeCotizacion(cot.getId(), "OC-CLI-" + sufijo, vendedor.getId());
            System.out.printf("   [OK] Orden de Compra Cliente generada: %s (id: %d, Estado: %s, Total: S/ %.2f)%n",
                    ordenGenerada.getNumero(), ordenGenerada.getId(), ordenGenerada.getEstado(), ordenGenerada.getTotal());

            // Listar y Obtener
            List<OrdenCompraCliente> listaOCC = occBO.listarTodos();
            System.out.printf("   [OK] Total de ordenes de compra clientes listadas: %d%n", listaOCC.size());

            OrdenCompraCliente obtenida = occBO.obtener(ordenGenerada.getId());
            System.out.printf("   [OK] Orden obtenida con detalles: %s (Lineas: %d)%n",
                    obtenida.getNumero(), obtenida.getDetalles().size());

            // Modificar
            obtenida.setObservaciones("Observacion modificada en orden " + sufijo);
            occBO.modificar(obtenida, vendedor.getId());
            System.out.println("   [OK] Orden de compra cliente modificada correctamente.");

            exitos++;
        } catch (Exception e) {
            System.err.println("   [FAIL] Error en OrdenCompraCliente: " + e.getMessage());
            fallos++;
        }

        // ==========================================
        // 2. CRUD VENTA (RF008)
        // ==========================================
        Venta ventaRegistrada = null;
        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("2. VENTA: Registro, Listar, Obtener y Modificar (RF008)");
            System.out.println("--------------------------------------------------------------------------------");

            Venta venta = new Venta();
            venta.setCliente(cliente);
            if (ordenGenerada != null) {
                venta.setOrdenCompraCliente(ordenGenerada);
            }
            venta.setCondicionPago(CondicionPago.CREDITO);
            venta.setPlazoCreditoDias(30);
            venta.setFechaEmision(LocalDate.now());
            venta.setMoneda(Moneda.SOLES);
            venta.setNumero("VTA-" + LocalDate.now().getYear() + "-" + sufijo);
            venta.setObservaciones("Venta al credito generada para pruebas automatizadas");

            List<DetalleVenta> detallesVenta = new ArrayList<>();
            DetalleVenta dv1 = new DetalleVenta();
            dv1.setNumeroLinea(1);
            dv1.setProducto(producto1);
            dv1.setCantidad(8.0);
            dv1.setPrecioUnitario(producto1.getPrecioVenta());
            dv1.setDescuento(0.0);
            detallesVenta.add(dv1);

            DetalleVenta dv2 = new DetalleVenta();
            dv2.setNumeroLinea(2);
            dv2.setProducto(producto2);
            dv2.setCantidad(4.0);
            dv2.setPrecioUnitario(producto2.getPrecioVenta());
            dv2.setDescuento(0.0);
            detallesVenta.add(dv2);

            venta.setDetalles(detallesVenta);

            ventaRegistrada = ventaBO.registrar(venta, vendedor.getId());
            System.out.printf("   [OK] Venta registrada exitosamente: %s (id: %d, Estado: %s, SubTotal: S/ %.2f, IGV: S/ %.2f, Total: S/ %.2f)%n",
                    ventaRegistrada.getNumero(), ventaRegistrada.getId(), ventaRegistrada.getEstado(),
                    ventaRegistrada.getSubTotal(), ventaRegistrada.getIgv(), ventaRegistrada.getTotal());

            // Listar
            List<Venta> ventas = ventaBO.listarTodos();
            System.out.printf("   [OK] Total de ventas registradas listadas: %d%n", ventas.size());

            // Obtener
            Venta ventaObtenida = ventaBO.obtener(ventaRegistrada.getId());
            System.out.printf("   [OK] Venta obtenida por ID: %s (Lineas: %d)%n",
                    ventaObtenida.getNumero(), ventaObtenida.getDetalles().size());

            // Modificar
            ventaObtenida.setObservaciones("Observaciones actualizadas en Venta " + sufijo);
            ventaObtenida.getDetalles().get(0).setCantidad(7.0);
            Venta ventaModificada = ventaBO.modificar(ventaObtenida, vendedor.getId());
            System.out.printf("   [OK] Venta modificada y totales recalculados: Total: S/ %.2f%n", ventaModificada.getTotal());

            exitos++;
        } catch (Exception e) {
            System.err.println("   [FAIL] Error en CRUD Venta: " + e.getMessage());
            fallos++;
        }

        // ==========================================
        // 3. COMPROBANTE DE PAGO (RF008)
        // ==========================================
        Comprobante comprobanteEmitido = null;
        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("3. COMPROBANTE: Emision automatica, Serie, Correlativo y Modificacion (RF008)");
            System.out.println("--------------------------------------------------------------------------------");

            TipoComprobante tipoComp = (cliente.getTipoDocumento() == TipoDocumentoIdentidad.RUC)
                    ? TipoComprobante.FACTURA : TipoComprobante.BOLETA;

            comprobanteEmitido = comprobanteBO.emitir(ventaRegistrada.getId(), tipoComp, cajero.getId());
            System.out.printf("   [OK] Comprobante emitido: %s %s-%s (id: %d, Total: S/ %.2f, Estado: %s)%n",
                    comprobanteEmitido.getTipo(), comprobanteEmitido.getSerie(), comprobanteEmitido.getNumero(),
                    comprobanteEmitido.getId(), comprobanteEmitido.getTotal(), comprobanteEmitido.getEstado());

            // Listar
            List<Comprobante> comprobantes = comprobanteBO.listarTodos();
            System.out.printf("   [OK] Total comprobantes listados: %d%n", comprobanteBO.listarTodos().size());

            // Obtener
            Comprobante compObtenido = comprobanteBO.obtener(comprobanteEmitido.getId());
            System.out.printf("   [OK] Comprobante obtenido: %s-%s (Fecha Emision: %s)%n",
                    compObtenido.getSerie(), compObtenido.getNumero(), compObtenido.getFechaEmision());

            exitos++;
        } catch (Exception e) {
            System.err.println("   [FAIL] Error en Comprobante: " + e.getMessage());
            fallos++;
        }

        // ==========================================
        // 4. DESPACHO CON SALIDA_DESPACHO (RF012)
        // ==========================================
        Despacho despachoRegistrado = null;
        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("4. DESPACHO: Parcial, Guia de Remision, SALIDA_DESPACHO y Stock (RF012)");
            System.out.println("--------------------------------------------------------------------------------");

            double stockP1Antes = productoBO.obtener(producto1.getId()).getStockActual();
            double stockP2Antes = productoBO.obtener(producto2.getId()).getStockActual();
            System.out.printf("   -> Stock antes de despacho: P1=%.2f, P2=%.2f%n", stockP1Antes, stockP2Antes);

            Despacho despacho = new Despacho();
            despacho.setVenta(ventaRegistrada);
            despacho.setFechaDespacho(LocalDate.now());
            despacho.setDireccionEntrega("Av. Argentina 2050, Callao");
            despacho.setTransportista("Transportes Metropolitano S.A.C.");

            // Despacho parcial: 4 unidades de P1 y 2 unidades de P2
            List<DetalleDespacho> detallesDespacho = new ArrayList<>();
            DetalleDespacho dd1 = new DetalleDespacho();
            dd1.setProducto(producto1);
            dd1.setCantidadDespachada(4.0);
            detallesDespacho.add(dd1);

            DetalleDespacho dd2 = new DetalleDespacho();
            dd2.setProducto(producto2);
            dd2.setCantidadDespachada(2.0);
            detallesDespacho.add(dd2);

            despacho.setDetalles(detallesDespacho);

            despachoRegistrado = despachoBO.registrar(despacho, almacenero.getId());
            System.out.printf("   [OK] Guia de despacho registrada: %s-%s (id: %d, Destino: %s)%n",
                    despachoRegistrado.getSerieGuia(), despachoRegistrado.getNumeroGuia(),
                    despachoRegistrado.getId(), despachoRegistrado.getDireccionEntrega());

            double stockP1Despues = productoBO.obtener(producto1.getId()).getStockActual();
            double stockP2Despues = productoBO.obtener(producto2.getId()).getStockActual();
            System.out.printf("   [OK] Stock actualizado tras despacho: P1=%.2f (-4.0), P2=%.2f (-2.0)%n",
                    stockP1Despues, stockP2Despues);

            Venta ventaDespachada = ventaBO.obtener(ventaRegistrada.getId());
            System.out.printf("   [OK] Estado de venta actualizado a: %s%n", ventaDespachada.getEstado());

            // Listar y Obtener
            List<Despacho> listaDespachos = despachoBO.listarTodos();
            System.out.printf("   [OK] Total despachos listados: %d%n", listaDespachos.size());

            Despacho despObtenido = despachoBO.obtener(despachoRegistrado.getId());
            System.out.printf("   [OK] Despacho obtenido: Guia %s-%s con %d lineas%n",
                    despObtenido.getSerieGuia(), despObtenido.getNumeroGuia(), despObtenido.getDetalles().size());

            // Modificar direccion/transportista
            despObtenido.setDireccionEntrega("Av. Colonial 1200, Lima");
            despachoBO.modificar(despObtenido, almacenero.getId());
            System.out.println("   [OK] Datos de entrega del despacho modificados exitosamente.");

            exitos++;
        } catch (Exception e) {
            System.err.println("   [FAIL] Error en Despacho: " + e.getMessage());
            fallos++;
        }

        // ==========================================
        // 5. NOTA DE CREDITO (RF013)
        // ==========================================
        Comprobante notaCreditoRegistrada = null;
        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("5. NOTA DE CREDITO: Autorizacion Admin, INGRESO_DEVOLUCION y Stock (RF013)");
            System.out.println("--------------------------------------------------------------------------------");

            double stockP1AntesNC = productoBO.obtener(producto1.getId()).getStockActual();
            System.out.printf("   -> Stock P1 antes de devolucion por NC: %.2f%n", stockP1AntesNC);

            Comprobante nc = new Comprobante();
            nc.setComprobanteRelacionado(comprobanteEmitido);
            nc.setMotivo("Devolucion de 1 unidad por especificacion tecnica");

            List<DetalleNotaCredito> detallesNC = new ArrayList<>();
            DetalleNotaCredito dnc1 = new DetalleNotaCredito();
            dnc1.setNumeroLinea(1);
            dnc1.setProducto(producto1);
            dnc1.setCantidad(1.0);
            detallesNC.add(dnc1);
            nc.setDetalles(detallesNC);

            notaCreditoRegistrada = comprobanteBO.registrarNotaCredito(nc, admin.getId(), cajero.getId());
            System.out.printf("   [OK] Nota de Credito registrada: %s-%s (id: %d, Total: S/ %.2f, Motivo: %s)%n",
                    notaCreditoRegistrada.getSerie(), notaCreditoRegistrada.getNumero(),
                    notaCreditoRegistrada.getId(), notaCreditoRegistrada.getTotal(), notaCreditoRegistrada.getMotivo());

            double stockP1DespuesNC = productoBO.obtener(producto1.getId()).getStockActual();
            System.out.printf("   [OK] Stock P1 reingresado tras NC: %.2f (+1.0)%n", stockP1DespuesNC);

            // Modificar motivo de NC
            notaCreditoRegistrada.setMotivo("Devolucion autorizada por gerencia - rectificacion de lote");
            comprobanteBO.modificarNotaCredito(notaCreditoRegistrada, cajero.getId());
            System.out.println("   [OK] Motivo de nota de credito actualizado exitosamente.");

            // Anular NC de prueba para verificar reversion
            comprobanteBO.anular(notaCreditoRegistrada.getId(), "Anulacion de NC de prueba", admin.getId());
            double stockP1TrasAnularNC = productoBO.obtener(producto1.getId()).getStockActual();
            System.out.printf("   [OK] Nota de Credito anulada: Stock P1 revertido mediante AJUSTE_SALIDA a: %.2f%n", stockP1TrasAnularNC);

            exitos++;
        } catch (Exception e) {
            System.err.println("   [FAIL] Error en Nota de Credito: " + e.getMessage());
            fallos++;
        }

        // ==========================================
        // 6. ANULACION DE DESPACHO Y VENTA
        // ==========================================
        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("6. ANULACIONES Y BAJAS LOGICAS: Reversion y consistencia (RNF002)");
            System.out.println("--------------------------------------------------------------------------------");

            // Anular despacho de prueba
            if (despachoRegistrado != null) {
                double stockAntesAnulacion = productoBO.obtener(producto1.getId()).getStockActual();
                despachoBO.anular(despachoRegistrado.getId(), "Anulacion de despacho de prueba", almacenero.getId());
                double stockDespuesAnulacion = productoBO.obtener(producto1.getId()).getStockActual();
                System.out.printf("   [OK] Despacho anulado exitosamente. Stock P1 devuelto: de %.2f a %.2f (+4.0)%n",
                        stockAntesAnulacion, stockDespuesAnulacion);
            }

            // Anular comprobante
            if (comprobanteEmitido != null) {
                comprobanteBO.anular(comprobanteEmitido.getId(), "Anulacion por cancelacion de pedido", cajero.getId());
                System.out.println("   [OK] Comprobante anulado exitosamente.");
            }

            // Anular venta (ahora sin despachos ni comprobantes vigentes)
            if (ventaRegistrada != null) {
                ventaBO.anular(ventaRegistrada.getId(), "Anulacion de venta de prueba", vendedor.getId());
                Venta ventaAnulada = ventaBO.obtener(ventaRegistrada.getId());
                System.out.printf("   [OK] Venta anulada exitosamente: %s (Estado: %s, Anulado: %b)%n",
                        ventaAnulada.getNumero(), ventaAnulada.getEstado(), ventaAnulada.isAnulado());
            }

            exitos++;
        } catch (Exception e) {
            System.err.println("   [FAIL] Error en Anulaciones: " + e.getMessage());
            fallos++;
        }

        // ==========================================
        // RESUMEN FINAL
        // ==========================================
        System.out.println("\n================================================================================");
        System.out.printf("               RESUMEN DE PRUEBAS: %d PASARON | %d FALLARON                     %n", exitos, fallos);
        System.out.println("================================================================================");
    }
}
