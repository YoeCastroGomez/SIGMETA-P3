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
import java.util.List;
import java.util.Map;

public class DespachoBOImpl implements DespachoBO {

    static final double EPSILON = 0.000001;

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
    private final ComprobanteDAO comprobanteDAO;
    private final DetalleNotaCreditoDAO detalleNotaCreditoDAO;
    // partes de esta clase (mismo paquete)
    private final DespachoAnulacion anulacionDespacho;

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
        this.comprobanteDAO = new ComprobanteDAOImpl();
        this.detalleNotaCreditoDAO = new DetalleNotaCreditoDAOImpl();
        this.anulacionDespacho = new DespachoAnulacion(despachoDAO, detalleDespachoDAO, ventaDAO, detalleVentaDAO, ordenCompraClienteDAO, detalleOrdenCompraClienteDAO, movimientoDAO, movimientoBO, usuarioBO, comprobanteDAO, detalleNotaCreditoDAO);
    }

    // RF012: Despacho parcial o total con SALIDA_DESPACHO y actualizacion de stock
    @Override
    public Despacho registrar(Despacho despacho, int idAlmacenero) throws SQLException {
        Usuario almacenero = usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);
        Validador.obligatorio(despacho, "despacho");
        Validador.obligatorio(despacho.getVenta(), "venta");

        List<DetalleDespacho> detalles = despacho.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("El despacho debe tener al menos una línea de detalle");
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
            // RF012: comprobante emitido y, al contado, cobro en caja; cantidades por linea (DespachoValidacion)
            DespachoValidacion.exigirComprobanteYCobro(comprobanteDAO, venta, idVenta);
            List<DetalleVenta> lineasVenta = DespachoValidacion.validarLineas(detalleVentaDAO, idVenta, detalles);

            String serie = "T001";
            String correlativo = despachoDAO.siguienteCorrelativo(serie);

            despacho.setVenta(venta);
            despacho.setSerieGuia(serie);
            despacho.setNumeroGuia(correlativo);
            if (despacho.getFechaDespacho() == null) {
                despacho.setFechaDespacho(LocalDate.now());
            }
            despacho.setDireccionEntrega(Validador.textoOpcional(despacho.getDireccionEntrega(), "dirección de entrega", 200));
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

            actual.setDireccionEntrega(Validador.textoOpcional(despacho.getDireccionEntrega(), "dirección de entrega", 200));
            actual.setTransportista(Validador.textoOpcional(despacho.getTransportista(), "transportista", 100));

            despachoDAO.update(actual);
            return actual;
        });
    }

    @Override
    public void anular(int idDespacho, String motivo, int idAlmacenero) throws SQLException {
        anulacionDespacho.anular(idDespacho, motivo, idAlmacenero);
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
