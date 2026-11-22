package pe.edu.pucp.sigmeta.ws.ventas;

import java.sql.SQLException;
import java.util.Map;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF008: conversion de una venta del modelo a lo que viaja en JSON (VentaDatos), con su situacion.
// Parte de VentaRS: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class VentaConversion {

    static VentaDatos convertir(Venta v, Nombres nombres, Map<Integer, Comprobante> comprobantes,
                                Map<Integer, String> numerosOrden, boolean conLineas) throws SQLException {
        VentaDatos d = new VentaDatos();
        d.id = v.getId();
        d.numero = v.getNumero();
        if (v.getCliente() != null) {
            d.idCliente = v.getCliente().getId();
            Cliente cliente = nombres.cliente(d.idCliente);
            if (cliente != null) {
                d.cliente = cliente.getNombreMostrado();
                d.documentoCliente = cliente.getNumeroDocumento();
            }
        }
        if (v.getOrdenCompraCliente() != null) {
            d.idOrden = v.getOrdenCompraCliente().getId();
            d.numeroOrden = numerosOrden.get(d.idOrden);
        }
        d.fechaEmision = Utilidades.fecha(v.getFechaEmision());
        d.condicionPago = v.getCondicionPago() != null ? v.getCondicionPago().name() : null;
        d.plazoCreditoDias = v.getPlazoCreditoDias();
        d.anulado = v.isAnulado();
        d.estado = v.isAnulado() ? EstadoVenta.ANULADA.name() : (v.getEstado() != null ? v.getEstado().name() : null);
        d.observaciones = v.getObservaciones();
        d.subTotal = v.getSubTotal();
        d.igv = v.getIgv();
        d.total = v.getTotal();
        d.usuarioRegistro = nombres.usuario(v.getUsuarioRegistro());
        d.fechaRegistro = Utilidades.fechaHora(v.getFechaRegistro());
        d.motivoAnulacion = v.getMotivoAnulacion();
        d.fechaAnulacion = Utilidades.fechaHora(v.getFechaAnulacion());
        Comprobante c = comprobantes.get(v.getId());
        if (c != null) {
            d.idComprobante = c.getId();
            d.tipoComprobante = c.getTipo().name();
            d.comprobante = c.getSerie() + "-" + c.getNumero();
        }
        if (conLineas && v.getDetalles() != null) {
            for (DetalleVenta det : v.getDetalles()) {
                LineaDatos l = CotizacionRS.convertirLinea(det.getNumeroLinea(), det.getProducto(), det.getCantidad(),
                        det.getPrecioUnitario(), det.getDescuento(), det.getImporte(), nombres);
                l.idLinea = det.getId();
                l.cantidadDespachada = det.getCantidadDespachada();
                if (det.getDetalleOrdenCompraCliente() != null) {
                    l.idDetalleOrden = det.getDetalleOrdenCompraCliente().getId();
                }
                d.lineas.add(l);
            }
        }
        return d;
    }
}
