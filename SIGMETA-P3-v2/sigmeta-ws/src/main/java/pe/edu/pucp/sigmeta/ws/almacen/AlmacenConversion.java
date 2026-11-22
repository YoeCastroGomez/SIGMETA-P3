package pe.edu.pucp.sigmeta.ws.almacen;

import pe.edu.pucp.sigmeta.model.almacen.DetalleDespacho;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Conversiones de AlmacenRS: de los datos que manda Blazor al modelo y del modelo a la respuesta JSON.
// Parte de AlmacenRS (mismo paquete, no publica): el recurso REST conserva sus rutas y delega aqui.
final class AlmacenConversion {

    private AlmacenConversion() {
    }

    // RF011: recepcion de compra; id = usuario de la sesion
    static RecepcionCompra aRecepcion(DocumentoAlmacen datos, int id) {
        RecepcionCompra recepcion = new RecepcionCompra();
        Compra compra = new Compra();
        compra.setId(datos.idCompra);
        recepcion.setCompra(compra);
        LocalDate fecha = Utilidades.leerFecha(datos.fecha, "fecha de recepción");
        recepcion.setFechaRecepcion(fecha != null ? fecha : LocalDate.now());
        recepcion.setObservaciones(datos.observaciones);
        Usuario usuario = new Usuario();
        usuario.setId(id); // el usuario de la sesion
        recepcion.setUsuarioRegistro(usuario);
        List<DetalleRecepcionCompra> detalles = new ArrayList<>();
        for (LineaAlmacen l : datos.lineas) {
            if (l == null || l.cantidad <= 0) {
                continue; // una linea en 0 significa "no llego nada de este producto"
            }
            DetalleRecepcionCompra d = new DetalleRecepcionCompra();
            DetalleCompra linea = new DetalleCompra();
            linea.setId(l.idLinea);
            d.setDetalleCompra(linea);
            d.setCantidadRecibida(l.cantidad);
            detalles.add(d);
        }
        recepcion.setDetalles(detalles);
        return recepcion;
    }

    // RF012: despacho de venta
    static Despacho aDespacho(DocumentoAlmacen datos) {
        Despacho despacho = new Despacho();
        Venta venta = new Venta();
        venta.setId(datos.idVenta);
        despacho.setVenta(venta);
        despacho.setFechaDespacho(Utilidades.leerFecha(datos.fecha, "fecha de despacho"));
        despacho.setDireccionEntrega(datos.direccionEntrega);
        despacho.setTransportista(datos.transportista);
        List<DetalleDespacho> detalles = new ArrayList<>();
        for (LineaAlmacen l : datos.lineas) {
            if (l == null || l.cantidad <= 0) {
                continue; // una linea en 0 no se despacha en esta guia
            }
            DetalleDespacho d = new DetalleDespacho();
            DetalleVenta linea = new DetalleVenta();
            linea.setId(l.idLinea);
            d.setDetalleVenta(linea);
            d.setCantidadDespachada(l.cantidad);
            detalles.add(d);
        }
        despacho.setDetalles(detalles);
        return despacho;
    }

    static Map<String, Object> resumen(int idRecepcion) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", idRecepcion);
        return respuesta;
    }

    static Map<String, Object> movimiento(MovimientoInventario m) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", m.getId());
        respuesta.put("tipo", m.getTipo() != null ? m.getTipo().name() : null);
        respuesta.put("cantidad", m.getCantidad());
        respuesta.put("stockResultante", m.getStockResultante());
        respuesta.put("motivo", m.getMotivo());
        return respuesta;
    }
}
