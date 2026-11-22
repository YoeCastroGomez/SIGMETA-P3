package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.ventas.ComprobanteDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleNotaCreditoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleVentaDAO;
import pe.edu.pucp.sigmeta.dao.ventas.VentaDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// RF013/RF014: valida la nota de credito, calcula sus montos y su huella sin mover stock, deuda ni caja.
// Parte de NotaCreditoEmision: la clase publica conserva sus metodos y delega aqui (mismo paquete, no publica).
final class NotaCreditoPreparacion {

    private final ComprobanteDAO comprobanteDAO;
    private final DetalleNotaCreditoDAO detalleNotaCreditoDAO;
    private final VentaDAO ventaDAO;
    private final DetalleVentaDAO detalleVentaDAO;

    NotaCreditoPreparacion(ComprobanteDAO comprobanteDAO, DetalleNotaCreditoDAO detalleNotaCreditoDAO, VentaDAO ventaDAO, DetalleVentaDAO detalleVentaDAO) {
        this.comprobanteDAO = comprobanteDAO;
        this.detalleNotaCreditoDAO = detalleNotaCreditoDAO;
        this.ventaDAO = ventaDAO;
        this.detalleVentaDAO = detalleVentaDAO;
    }

    // RF013: valida la nota y calcula sus montos SIN mover nada (stock, deuda, caja). La usan registrar y solicitar.
    NotaPreparada prepararNotaCredito(Comprobante notaCredito) throws SQLException {
        List<DetalleNotaCredito> detalles = notaCredito.getDetalles();
            int idOrigen = notaCredito.getComprobanteRelacionado().getId();
            Comprobante origen = comprobanteDAO.load(idOrigen);
            if (origen == null) {
                throw new IllegalArgumentException("No existe el comprobante origen con id " + idOrigen);
            }
            if (origen.getTipo() != TipoComprobante.FACTURA && origen.getTipo() != TipoComprobante.BOLETA) {
                throw new IllegalArgumentException("La nota de crédito solo puede emitirse sobre una FACTURA o BOLETA");
            }
            if (origen.getEstado() != EstadoComprobante.EMITIDO && origen.getEstado() != EstadoComprobante.ACEPTADO) {
                throw new IllegalStateException("El comprobante origen debe estar en estado EMITIDO o ACEPTADO");
            }

            Venta venta = ventaDAO.load(origen.getVenta().getId());
            if (venta == null || venta.isAnulado()) {
                throw new IllegalStateException("La venta asociada al comprobante origen no existe o está anulada");
            }

            // RF013: cada linea de la nota apunta a la linea de venta concreta que se devuelve
            List<DetalleVenta> detallesVenta = detalleVentaDAO.listarPorVenta(venta.getId());
            Map<Integer, DetalleVenta> mapaDetallesVenta = new HashMap<>();
            for (DetalleVenta dv : detallesVenta) {
                mapaDetallesVenta.put(dv.getId(), dv);
            }

            // Cantidades ya acreditadas por linea de venta en notas de credito vigentes
            List<Comprobante> ncsExistentes = comprobanteDAO.listarNotasCreditoPorComprobante(idOrigen);
            Map<Integer, Double> yaAcreditadoPorLinea = new HashMap<>();
            for (Comprobante ncExistente : ncsExistentes) {
                List<DetalleNotaCredito> lineasExistentes = detalleNotaCreditoDAO.listarPorComprobante(ncExistente.getId());
                for (DetalleNotaCredito dnc : lineasExistentes) {
                    yaAcreditadoPorLinea.merge(dnc.getDetalleVenta().getId(), dnc.getCantidad(), Double::sum);
                }
            }

            Set<Integer> lineasEnNC = new HashSet<>();
            BigDecimal subTotalNC = BigDecimal.ZERO;

            for (DetalleNotaCredito d : detalles) {
                Validador.obligatorio(d.getDetalleVenta(), "línea de venta");
                int idLinea = d.getDetalleVenta().getId();
                if (!lineasEnNC.add(idLinea)) {
                    throw new IllegalArgumentException("No se puede repetir la misma línea de venta en la nota de crédito");
                }
                DetalleVenta lineaVenta = mapaDetallesVenta.get(idLinea);
                if (lineaVenta == null) {
                    throw new IllegalArgumentException("La línea " + idLinea + " no pertenece a la venta origen");
                }
                if (!Double.isFinite(d.getCantidad()) || d.getCantidad() <= 0) {
                    throw new IllegalArgumentException("La cantidad devuelta debe ser mayor a 0");
                }

                // solo se devuelve lo ENTREGADO al cliente (despachado), descontando lo ya acreditado:
                // asi el reingreso al stock siempre tiene una salida previa (SALIDA_DESPACHO)
                double yaAcreditado = yaAcreditadoPorLinea.getOrDefault(idLinea, 0.0);
                double entregado = lineaVenta.getCantidadDespachada();
                if (yaAcreditado + d.getCantidad() > entregado + 0.0001) {
                    throw new IllegalArgumentException("Solo se puede devolver lo entregado al cliente en la línea "
                            + lineaVenta.getNumeroLinea() + " (Entregado: " + entregado + ", Ya acreditado: "
                            + yaAcreditado + ", Devuelto: " + d.getCantidad() + ")");
                }

                // se devuelve el precio efectivamente cobrado: importe de la linea (ya con descuento) por unidad.
                // El descuento de la nota es la parte proporcional del descuento de la linea.
                BigDecimal bruto = BigDecimal.valueOf(d.getCantidad())
                        .multiply(BigDecimal.valueOf(lineaVenta.getPrecioUnitario()))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal importe = BigDecimal.valueOf(lineaVenta.getImporte())
                        .multiply(BigDecimal.valueOf(d.getCantidad()))
                        .divide(BigDecimal.valueOf(lineaVenta.getCantidad()), 2, RoundingMode.HALF_UP);
                d.setDetalleVenta(lineaVenta);
                d.setPrecioUnitario(lineaVenta.getPrecioUnitario());
                d.setDescuento(bruto.subtract(importe).doubleValue());
                d.setImporte(importe.doubleValue());
                subTotalNC = subTotalNC.add(importe);
            }

            // los importes ya incluyen IGV (precio cobrado): el total de la nota es su suma y el IGV se desglosa
            BigDecimal totalNC = subTotalNC;
            BigDecimal igvNC = PreciosIgv.igv(totalNC);
            subTotalNC = PreciosIgv.valorVenta(totalNC);

            if (totalNC.doubleValue() > origen.getTotal() + 0.01) {
                throw new IllegalArgumentException("El monto total de la nota de crédito no puede superar el total del comprobante origen");
            }
        return new NotaPreparada(origen, venta, subTotalNC, igvNC, totalNC);
    }

    record NotaPreparada(Comprobante origen, Venta venta, BigDecimal subTotal, BigDecimal igv, BigDecimal total) {
    }

    // RF014: huella de la nota = comprobante de origen + cada linea devuelta con su cantidad
    static String huellaNotaCredito(Comprobante notaCredito) {
        StringBuilder datos = new StringBuilder("NC|").append(notaCredito.getComprobanteRelacionado().getId());
        notaCredito.getDetalles().stream()
                .sorted(java.util.Comparator.comparingInt(d -> d.getDetalleVenta().getId()))
                .forEach(d -> datos.append('|').append(d.getDetalleVenta().getId()).append(':')
                        .append(String.format(java.util.Locale.ROOT, "%.3f", d.getCantidad())));
        return SolicitudAutorizacionBOImpl.huella(datos.toString());
    }
}
