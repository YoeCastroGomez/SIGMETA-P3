package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.almacen.MovimientoInventarioBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.almacen.DespachoBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.almacen.MovimientoInventarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.almacen.DespachoDAO;
import pe.edu.pucp.sigmeta.dao.almacen.DetalleDespachoDAO;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.dao.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.almacen.DespachoDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.DetalleDespachoDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.MovimientoInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.StockInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.DetalleDespacho;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.DetalleOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DespachoBOImpl implements DespachoBO {

    private static final double EPSILON = 0.000001;

    private final DespachoDAO despachoDAO;
    private final DetalleDespachoDAO detalleDespachoDAO;
    private final VentaDAO ventaDAO;
    private final DetalleVentaDAO detalleVentaDAO;
    private final OrdenCompraClienteDAO ordenCompraClienteDAO;
    private final DetalleOrdenCompraClienteDAO detalleOrdenCompraClienteDAO;
    private final StockInventarioDAO stockDAO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final MovimientoInventarioBO movimientoBO;
    private final UsuarioBO usuarioBO;

    public DespachoBOImpl() {
        this.despachoDAO = new DespachoDAOImpl();
        this.detalleDespachoDAO = new DetalleDespachoDAOImpl();
        this.ventaDAO = new VentaDAOImpl();
        this.detalleVentaDAO = new DetalleVentaDAOImpl();
        this.ordenCompraClienteDAO = new OrdenCompraClienteDAOImpl();
        this.detalleOrdenCompraClienteDAO = new DetalleOrdenCompraClienteDAOImpl();
        this.stockDAO = new StockInventarioDAOImpl();
        this.movimientoDAO = new MovimientoInventarioDAOImpl();
        this.movimientoBO = new MovimientoInventarioBOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF012: Despacho parcial o total con SALIDA_DESPACHO y actualizacion de stock
    @Override
    public Despacho registrar(Despacho despacho, int idAlmacenero) throws SQLException {
        Usuario almacenero = usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);
        Validador.obligatorio(despacho, "despacho");
        Validador.obligatorio(despacho.getVenta(), "venta");

        List<DetalleDespacho> detalles = despacho.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("El despacho debe tener al menos una linea de detalle");
        }

        return Transaccion.ejecutar(() -> {
            int idVenta = despacho.getVenta().getId();
            ventaDAO.bloquearVenta(idVenta);

            Venta venta = ventaDAO.load(idVenta);
            if (venta == null) {
                throw new IllegalArgumentException("No existe la venta con id " + idVenta);
            }
            if (venta.isAnulado()) {
                throw new IllegalStateException("No se puede despachar una venta anulada");
            }
            if (venta.getEstado() != EstadoVenta.REGISTRADA && venta.getEstado() != EstadoVenta.DESPACHADA_PARCIAL) {
                throw new IllegalStateException("La venta no admite nuevos despachos (estado actual: " + venta.getEstado() + ")");
            }

            // RF012: cada linea del despacho apunta a una linea de venta concreta (no a un producto),
            // asi una venta con varias lineas del mismo producto se controla linea por linea
            List<DetalleVenta> lineasVenta = detalleVentaDAO.listarPorVenta(idVenta);
            Map<Integer, DetalleVenta> mapaLineasVenta = new HashMap<>();
            for (DetalleVenta dv : lineasVenta) {
                mapaLineasVenta.put(dv.getId(), dv);
            }

            Set<Integer> lineasEnDespacho = new HashSet<>();
            for (DetalleDespacho d : detalles) {
                Validador.obligatorio(d.getDetalleVenta(), "linea de venta");
                int idLinea = d.getDetalleVenta().getId();
                if (!lineasEnDespacho.add(idLinea)) {
                    throw new IllegalArgumentException("No se puede repetir la misma linea de venta en una guia de despacho");
                }
                DetalleVenta lineaVenta = mapaLineasVenta.get(idLinea);
                if (lineaVenta == null) {
                    throw new IllegalArgumentException("La linea " + idLinea + " no pertenece a la venta");
                }
                double cant = d.getCantidadDespachada();
                if (!Double.isFinite(cant) || cant <= 0) {
                    throw new IllegalArgumentException("La cantidad a despachar debe ser mayor a 0");
                }
                if (lineaVenta.getCantidadDespachada() + cant > lineaVenta.getCantidad() + EPSILON) {
                    throw new IllegalArgumentException("La cantidad excede lo pendiente de despachar en la linea " + lineaVenta.getNumeroLinea()
                            + " (Vendido: " + lineaVenta.getCantidad() + ", Despachado: " + lineaVenta.getCantidadDespachada() + ", Solicitado: " + cant + ")");
                }
                d.setDetalleVenta(lineaVenta);
            }

            String serie = "T001";
            String correlativo = despachoDAO.siguienteCorrelativo(serie);

            despacho.setVenta(venta);
            despacho.setSerieGuia(serie);
            despacho.setNumeroGuia(correlativo);
            if (despacho.getFechaDespacho() == null) {
                despacho.setFechaDespacho(LocalDate.now());
            }
            despacho.setDireccionEntrega(Validador.textoOpcional(despacho.getDireccionEntrega(), "direccion de entrega", 200));
            despacho.setTransportista(Validador.textoOpcional(despacho.getTransportista(), "transportista", 100));
            despacho.setAnulado(false);
            despacho.setUsuarioRegistro(almacenero);
            despacho.setFechaRegistro(LocalDateTime.now());

            despachoDAO.save(despacho);

            // Cargar detalles de orden de compra si la venta proviene de una
            Map<Integer, DetalleOrdenCompraCliente> mapaDetallesOc = new HashMap<>();
            Integer idOc = (venta.getOrdenCompraCliente() != null && venta.getOrdenCompraCliente().getId() > 0)
                    ? venta.getOrdenCompraCliente().getId() : null;
            if (idOc != null) {
                List<DetalleOrdenCompraCliente> docs = detalleOrdenCompraClienteDAO.listarPorOrden(idOc);
                for (DetalleOrdenCompraCliente doc : docs) {
                    mapaDetallesOc.put(doc.getId(), doc);
                }
            }

            for (DetalleDespacho d : detalles) {
                d.setDespacho(despacho);
                detalleDespachoDAO.save(d);

                DetalleVenta lineaVenta = d.getDetalleVenta();
                int idProd = lineaVenta.getProducto().getId();
                double cantDesp = d.getCantidadDespachada();

                // 1 & 2. Validar y descontar stock
                double stockActual = stockDAO.obtenerStockParaActualizar(idProd);
                if (stockActual < cantDesp) {
                    throw new IllegalArgumentException("Stock insuficiente para el producto " + idProd
                            + " (Stock actual: " + stockActual + ", Cantidad a despachar: " + cantDesp + ")");
                }
                double nuevoStock = stockActual - cantDesp;
                stockDAO.actualizarStock(idProd, nuevoStock);

                // 3. Registrar MovimientoInventario SALIDA_DESPACHO
                MovimientoInventario mov = new MovimientoInventario();
                Producto prod = new Producto();
                prod.setId(idProd);
                mov.setProducto(prod);
                mov.setTipo(TipoMovimientoInventario.SALIDA_DESPACHO);
                mov.setCantidad(cantDesp);
                mov.setStockResultante(nuevoStock);
                mov.setDespacho(despacho);
                mov.setUsuarioRegistro(almacenero);
                mov.setMotivo("Salida por despacho " + serie + "-" + correlativo);
                movimientoDAO.save(mov);

                // 4. Actualizar la linea de venta despachada
                double nuevaCantDesp = lineaVenta.getCantidadDespachada() + cantDesp;
                lineaVenta.setCantidadDespachada(nuevaCantDesp);
                detalleVentaDAO.actualizarCantidadDespachada(lineaVenta.getId(), nuevaCantDesp);

                // 5. Actualizar la linea de orden que atiende esta linea de venta (FK detalle_venta -> orden)
                DetalleOrdenCompraCliente lineaOrden = lineaVenta.getDetalleOrdenCompraCliente();
                if (idOc != null && lineaOrden != null && mapaDetallesOc.containsKey(lineaOrden.getId())) {
                    DetalleOrdenCompraCliente doc = mapaDetallesOc.get(lineaOrden.getId());
                    double nuevaAtendida = doc.getCantidadAtendida() + cantDesp;
                    if (nuevaAtendida > doc.getCantidad() + EPSILON) {
                        throw new IllegalArgumentException("El despacho supera la cantidad de la orden");
                    }
                    doc.setCantidadAtendida(nuevaAtendida);
                    detalleOrdenCompraClienteDAO.actualizarCantidadAtendida(doc.getId(), nuevaAtendida);
                }
            }

            // 6. Actualizar estado de venta
            boolean ventaCompleta = true;
            for (DetalleVenta dv : lineasVenta) {
                if (dv.getCantidadDespachada() + EPSILON < dv.getCantidad()) {
                    ventaCompleta = false;
                    break;
                }
            }
            EstadoVenta nuevoEstadoVenta = ventaCompleta ? EstadoVenta.DESPACHADA : EstadoVenta.DESPACHADA_PARCIAL;
            venta.setEstado(nuevoEstadoVenta);
            ventaDAO.actualizarEstado(idVenta, nuevoEstadoVenta);

            // 7. Actualizar estado de orden de compra si aplica
            if (idOc != null) {
                boolean ocCompleta = true;
                for (DetalleOrdenCompraCliente doc : mapaDetallesOc.values()) {
                    if (doc.getCantidadAtendida() + EPSILON < doc.getCantidad()) {
                        ocCompleta = false;
                        break;
                    }
                }
                EstadoOrdenCompraCliente nuevoEstadoOc = ocCompleta ? EstadoOrdenCompraCliente.ATENDIDA : EstadoOrdenCompraCliente.ATENDIDA_PARCIAL;
                ordenCompraClienteDAO.actualizarEstado(idOc, nuevoEstadoOc);
            }

            return despacho;
        });
    }

    @Override
    public Despacho modificar(Despacho despacho, int idAlmacenero) throws SQLException {
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);
        Validador.obligatorio(despacho, "despacho");
        return Transaccion.ejecutar(() -> {
            Despacho actual = despachoDAO.load(despacho.getId());
            if (actual == null) {
                throw new IllegalArgumentException("No existe el despacho con id " + despacho.getId());
            }
            if (actual.isAnulado()) {
                throw new IllegalStateException("No se puede modificar un despacho anulado");
            }

            actual.setDireccionEntrega(Validador.textoOpcional(despacho.getDireccionEntrega(), "direccion de entrega", 200));
            actual.setTransportista(Validador.textoOpcional(despacho.getTransportista(), "transportista", 100));

            despachoDAO.update(actual);
            return actual;
        });
    }

    // RF012 / §4.4 / §6.4: Anular despacho y revertir stock atómicamente
    @Override
    public void anular(int idDespacho, String motivo, int idAlmacenero) throws SQLException {
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO, TipoRol.ADMINISTRADOR);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo de anulacion", 255);

        Transaccion.ejecutar(() -> {
            Despacho despacho = despachoDAO.load(idDespacho);
            if (despacho == null) {
                throw new IllegalArgumentException("No existe el despacho con id " + idDespacho);
            }
            if (despacho.isAnulado()) {
                throw new IllegalStateException("El despacho ya se encuentra anulado");
            }

            int idVenta = despacho.getVenta().getId();
            ventaDAO.bloquearVenta(idVenta);

            // 1. Revertir cada movimiento SALIDA_DESPACHO asociado a este despacho
            List<MovimientoInventario> todosMovimientos = movimientoDAO.listAll();
            for (MovimientoInventario m : todosMovimientos) {
                if (m.getDespacho() != null && m.getDespacho().getId() == idDespacho
                        && m.getTipo() == TipoMovimientoInventario.SALIDA_DESPACHO) {
                    movimientoBO.revertirSalidaDespacho(m.getId(), idDespacho, idAlmacenero, motivoValidado);
                }
            }

            // 2. Marcar despacho anulado (baja logica)
            despachoDAO.remove(despacho);

            // 3. Descontar lo devuelto de detalle_venta.cantidad_despachada
            List<DetalleDespacho> detallesDespacho = detalleDespachoDAO.listarPorDespacho(idDespacho);
            List<DetalleVenta> lineasVenta = detalleVentaDAO.listarPorVenta(idVenta);
            Map<Integer, DetalleVenta> mapaLineasVenta = new HashMap<>();
            for (DetalleVenta dv : lineasVenta) {
                mapaLineasVenta.put(dv.getId(), dv);
            }

            Venta venta = ventaDAO.load(idVenta);
            Integer idOc = (venta != null && venta.getOrdenCompraCliente() != null && venta.getOrdenCompraCliente().getId() > 0)
                    ? venta.getOrdenCompraCliente().getId() : null;
            Map<Integer, DetalleOrdenCompraCliente> mapaDetallesOc = new HashMap<>();
            if (idOc != null) {
                for (DetalleOrdenCompraCliente doc : detalleOrdenCompraClienteDAO.listarPorOrden(idOc)) {
                    mapaDetallesOc.put(doc.getId(), doc);
                }
            }

            // la linea de despacho apunta a su linea de venta, y esta a su linea de orden
            for (DetalleDespacho dd : detallesDespacho) {
                DetalleVenta dv = mapaLineasVenta.get(dd.getDetalleVenta().getId());
                if (dv == null) {
                    continue;
                }
                double restanteDesp = Math.max(0.0, dv.getCantidadDespachada() - dd.getCantidadDespachada());
                dv.setCantidadDespachada(restanteDesp);
                detalleVentaDAO.actualizarCantidadDespachada(dv.getId(), restanteDesp);

                DetalleOrdenCompraCliente lineaOrden = dv.getDetalleOrdenCompraCliente();
                if (idOc != null && lineaOrden != null && mapaDetallesOc.containsKey(lineaOrden.getId())) {
                    DetalleOrdenCompraCliente doc = mapaDetallesOc.get(lineaOrden.getId());
                    double restanteAtendida = Math.max(0.0, doc.getCantidadAtendida() - dd.getCantidadDespachada());
                    doc.setCantidadAtendida(restanteAtendida);
                    detalleOrdenCompraClienteDAO.actualizarCantidadAtendida(doc.getId(), restanteAtendida);
                }
            }

            // 4. Recalcular estado de la venta
            boolean tieneDespachos = false;
            boolean todoDespachado = true;
            for (DetalleVenta dv : lineasVenta) {
                if (dv.getCantidadDespachada() > EPSILON) {
                    tieneDespachos = true;
                }
                if (dv.getCantidadDespachada() + EPSILON < dv.getCantidad()) {
                    todoDespachado = false;
                }
            }
            EstadoVenta nuevoEstadoVenta = todoDespachado ? EstadoVenta.DESPACHADA
                    : (tieneDespachos ? EstadoVenta.DESPACHADA_PARCIAL : EstadoVenta.REGISTRADA);
            ventaDAO.actualizarEstado(idVenta, nuevoEstadoVenta);

            // 5. Recalcular estado de orden de compra si aplica
            if (idOc != null) {
                boolean tieneAtencion = false;
                boolean todoAtendido = true;
                for (DetalleOrdenCompraCliente doc : mapaDetallesOc.values()) {
                    if (doc.getCantidadAtendida() > EPSILON) {
                        tieneAtencion = true;
                    }
                    if (doc.getCantidadAtendida() + EPSILON < doc.getCantidad()) {
                        todoAtendido = false;
                    }
                }
                EstadoOrdenCompraCliente nuevoEstadoOc = todoAtendido ? EstadoOrdenCompraCliente.ATENDIDA
                        : (tieneAtencion ? EstadoOrdenCompraCliente.ATENDIDA_PARCIAL : EstadoOrdenCompraCliente.PENDIENTE);
                ordenCompraClienteDAO.actualizarEstado(idOc, nuevoEstadoOc);
            }
            return null;
        });
    }

    @Override
    public Despacho obtener(int id) throws SQLException {
        return Transaccion.ejecutar(() -> {
            Despacho d = despachoDAO.load(id);
            if (d != null) {
                List<DetalleDespacho> detalles = detalleDespachoDAO.listarPorDespacho(id);
                for (DetalleDespacho dd : detalles) {
                    dd.setDespacho(d);
                }
                d.setDetalles(detalles);
            }
            return d;
        });
    }

    @Override
    public List<Despacho> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return despachoDAO.listarTodos();
        });
    }
}
