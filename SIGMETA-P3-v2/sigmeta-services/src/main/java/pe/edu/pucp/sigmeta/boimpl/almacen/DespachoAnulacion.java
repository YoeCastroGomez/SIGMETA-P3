package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.almacen.MovimientoInventarioBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.almacen.DespachoDAO;
import pe.edu.pucp.sigmeta.dao.almacen.DetalleDespachoDAO;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.DetalleDespacho;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.model.ventas.DetalleOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static pe.edu.pucp.sigmeta.boimpl.almacen.DespachoBOImpl.EPSILON;

// RF012: anular un despacho (devuelve el stock y recalcula el estado de la venta y de la orden).
// Parte de DespachoBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class DespachoAnulacion {

    private final DespachoDAO despachoDAO;
    private final DetalleDespachoDAO detalleDespachoDAO;
    private final VentaDAO ventaDAO;
    private final DetalleVentaDAO detalleVentaDAO;
    private final OrdenCompraClienteDAO ordenCompraClienteDAO;
    private final DetalleOrdenCompraClienteDAO detalleOrdenCompraClienteDAO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final MovimientoInventarioBO movimientoBO;
    private final UsuarioBO usuarioBO;
    private final ComprobanteDAO comprobanteDAO;
    private final DetalleNotaCreditoDAO detalleNotaCreditoDAO;

    DespachoAnulacion(DespachoDAO despachoDAO, DetalleDespachoDAO detalleDespachoDAO, VentaDAO ventaDAO, DetalleVentaDAO detalleVentaDAO, OrdenCompraClienteDAO ordenCompraClienteDAO, DetalleOrdenCompraClienteDAO detalleOrdenCompraClienteDAO, MovimientoInventarioDAO movimientoDAO, MovimientoInventarioBO movimientoBO, UsuarioBO usuarioBO, ComprobanteDAO comprobanteDAO, DetalleNotaCreditoDAO detalleNotaCreditoDAO) {
        this.despachoDAO = despachoDAO;
        this.detalleDespachoDAO = detalleDespachoDAO;
        this.ventaDAO = ventaDAO;
        this.detalleVentaDAO = detalleVentaDAO;
        this.ordenCompraClienteDAO = ordenCompraClienteDAO;
        this.detalleOrdenCompraClienteDAO = detalleOrdenCompraClienteDAO;
        this.movimientoDAO = movimientoDAO;
        this.movimientoBO = movimientoBO;
        this.usuarioBO = usuarioBO;
        this.comprobanteDAO = comprobanteDAO;
        this.detalleNotaCreditoDAO = detalleNotaCreditoDAO;
    }

    // RF012 / §4.4 / §6.4: Anular despacho y revertir stock atómicamente
    public void anular(int idDespacho, String motivo, int idAlmacenero) throws SQLException {
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO, TipoRol.ADMINISTRADOR);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo de anulación", 255);

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

            // RF013: si una linea ya tiene devoluciones por nota de credito, al anular el despacho
            // no puede quedar menos entregado que lo ya devuelto (el stock se contaria dos veces)
            validarSinDevolucionesPendientes(idDespacho, idVenta);

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

    void validarSinDevolucionesPendientes(int idDespacho, int idVenta) throws SQLException {
        // lo ya acreditado por linea de venta en notas de credito vigentes de los comprobantes de la venta
        Map<Integer, Double> acreditado = new HashMap<>();
        for (Comprobante c : comprobanteDAO.listarPorVenta(idVenta)) {
            if (c.getTipo() == TipoComprobante.NOTA_CREDITO || c.getEstado() == EstadoComprobante.ANULADO) {
                continue;
            }
            for (Comprobante nc : comprobanteDAO.listarNotasCreditoPorComprobante(c.getId())) {
                for (DetalleNotaCredito dnc : detalleNotaCreditoDAO.listarPorComprobante(nc.getId())) {
                    acreditado.merge(dnc.getDetalleVenta().getId(), dnc.getCantidad(), Double::sum);
                }
            }
        }
        if (acreditado.isEmpty()) {
            return;
        }
        Map<Integer, DetalleVenta> lineas = new HashMap<>();
        for (DetalleVenta dv : detalleVentaDAO.listarPorVenta(idVenta)) {
            lineas.put(dv.getId(), dv);
        }
        for (DetalleDespacho dd : detalleDespachoDAO.listarPorDespacho(idDespacho)) {
            DetalleVenta dv = lineas.get(dd.getDetalleVenta().getId());
            if (dv == null) {
                continue;
            }
            double quedaEntregado = dv.getCantidadDespachada() - dd.getCantidadDespachada();
            if (quedaEntregado + EPSILON < acreditado.getOrDefault(dv.getId(), 0.0)) {
                throw new IllegalStateException("No se puede anular el despacho: la línea " + dv.getNumeroLinea()
                        + " ya tiene devoluciones por nota de crédito. Primero anule la nota de crédito");
            }
        }
    }
}
