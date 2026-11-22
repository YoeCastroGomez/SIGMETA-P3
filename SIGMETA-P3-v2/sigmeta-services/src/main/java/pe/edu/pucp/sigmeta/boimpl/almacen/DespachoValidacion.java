package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.ventas.ComprobanteDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleVentaDAO;
import pe.edu.pucp.sigmeta.model.almacen.DetalleDespacho;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pe.edu.pucp.sigmeta.boimpl.almacen.DespachoBOImpl.EPSILON;

// RF012: validaciones previas a registrar un despacho (comprobante, cobro y cantidades por linea de venta).
// Parte de DespachoBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class DespachoValidacion {

    private DespachoValidacion() {
    }

    // RF012: la mercaderia sale del almacen con su comprobante emitido y, si la venta es al contado,
    // ya cobrada en caja. Al credito basta el comprobante: la deuda esta en la cuenta por cobrar.
    static void exigirComprobanteYCobro(ComprobanteDAO comprobanteDAO, Venta venta, int idVenta) throws SQLException {
        boolean tieneComprobante = comprobanteDAO.listarPorVenta(idVenta).stream()
                .anyMatch(c -> c.getTipo() != pe.edu.pucp.sigmeta.model.enums.TipoComprobante.NOTA_CREDITO
                        && c.getEstado() != pe.edu.pucp.sigmeta.model.enums.EstadoComprobante.ANULADO);
        if (!tieneComprobante) {
            throw new IllegalStateException("La venta " + venta.getNumero()
                    + " todavía no tiene factura ni boleta emitida; emita el comprobante antes de despachar");
        }
        if (venta.getCondicionPago() == pe.edu.pucp.sigmeta.model.enums.CondicionPago.CONTADO
                && new pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl()
                        .numero("sp_venta_contar_cobros_contado", idVenta).signum() == 0) {
            throw new IllegalStateException("La venta " + venta.getNumero()
                    + " es al contado y todavía no se cobró en caja; se despacha después del cobro");
        }
    }

    // RF012: cada linea del despacho apunta a una linea de venta concreta (no a un producto),
    // asi una venta con varias lineas del mismo producto se controla linea por linea.
    // Devuelve las lineas de la venta (se usan despues para recalcular su estado).
    static List<DetalleVenta> validarLineas(DetalleVentaDAO detalleVentaDAO, int idVenta,
            List<DetalleDespacho> detalles) throws SQLException {
        List<DetalleVenta> lineasVenta = detalleVentaDAO.listarPorVenta(idVenta);
        Map<Integer, DetalleVenta> mapaLineasVenta = new HashMap<>();
        for (DetalleVenta dv : lineasVenta) {
            mapaLineasVenta.put(dv.getId(), dv);
        }

        Set<Integer> lineasEnDespacho = new HashSet<>();
        for (DetalleDespacho d : detalles) {
            Validador.obligatorio(d.getDetalleVenta(), "línea de venta");
            int idLinea = d.getDetalleVenta().getId();
            if (!lineasEnDespacho.add(idLinea)) {
                throw new IllegalArgumentException("No se puede repetir la misma línea de venta en una guía de despacho");
            }
            DetalleVenta lineaVenta = mapaLineasVenta.get(idLinea);
            if (lineaVenta == null) {
                throw new IllegalArgumentException("La línea " + idLinea + " no pertenece a la venta");
            }
            double cant = d.getCantidadDespachada();
            if (!Double.isFinite(cant) || cant <= 0) {
                throw new IllegalArgumentException("La cantidad a despachar debe ser mayor a 0");
            }
            if (lineaVenta.getCantidadDespachada() + cant > lineaVenta.getCantidad() + EPSILON) {
                throw new IllegalArgumentException("La cantidad excede lo pendiente de despachar en la línea " + lineaVenta.getNumeroLinea()
                        + " (Vendido: " + lineaVenta.getCantidad() + ", Despachado: " + lineaVenta.getCantidadDespachada() + ", Solicitado: " + cant + ")");
            }
            d.setDetalleVenta(lineaVenta);
        }
        return lineasVenta;
    }
}
