package pe.edu.pucp.ejecucion;

import java.util.List;
import pe.edu.pucp.model.producto.Producto;
import pe.edu.pucp.model.socio.Proveedor;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * ESTUDIANTE 3.
 * Tablas: compra, detalle_compra, recepcion_compra,
 * detalle_recepcion_compra, movimiento_inventario.
 *
 * Debe crear: una compra al proveedor de dos productos y una recepcion
 * parcial. Hay que verificar que el stockResultante del movimiento de
 * inventario cuadre con el stockActual del producto.
 *
 * Recordar el cambio acordado: el movimiento de inventario ya no usa
 * documentoOrigen ni idDocumentoOrigen, ahora apunta a la recepcion,
 * al despacho o al comprobante.
 */
public class PruebaAbastecimiento {

    public static void ejecutar(Proveedor proveedor, List<Producto> productos, Usuario registro) {
        System.out.println("== PRUEBA 3: compras e inventario ==");

        // TODO estudiante 3
    }
}
