package pe.edu.pucp.ejecucion;

import pe.edu.pucp.model.comercial.Cotizacion;
import pe.edu.pucp.model.comercial.Venta;
import pe.edu.pucp.model.socio.Cliente;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * ESTUDIANTE 4.
 * Tablas: orden_compra_cliente, detalle_orden_compra_cliente, venta,
 * detalle_venta, comprobante, despacho, detalle_despacho.
 *
 * Debe crear: la orden de compra del cliente a partir de la cotizacion
 * aceptada, copiando su detalle sin volver a digitarlo; la venta al
 * credito; la factura con su serie y numero correlativo; un despacho
 * parcial con guia de remision; y la nota de credito sobre esa factura.
 *
 * Devuelve la venta, que es lo que usa la prueba de caja.
 */
public class PruebaVenta {

    public static Venta ejecutar(Cotizacion cotizacion, Cliente cliente, Usuario registro) {
        System.out.println("== PRUEBA 4: venta, comprobantes y despacho ==");

        // TODO estudiante 4

        Venta venta = new Venta();
        return venta;
    }
}
