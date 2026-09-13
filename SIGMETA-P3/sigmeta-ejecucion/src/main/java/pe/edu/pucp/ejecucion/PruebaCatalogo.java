package pe.edu.pucp.ejecucion;

import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comercial.Cotizacion;
import pe.edu.pucp.model.producto.Producto;
import pe.edu.pucp.model.socio.Cliente;

/**
 * ESTUDIANTE 2.
 * Tablas: categoria, producto, cotizacion, detalle_cotizacion.
 *
 * Debe crear: tres categorias, seis productos con sus tres codigos, precio,
 * costo, stock actual y stock minimo, y una cotizacion de tres lineas con
 * descuento. Hay que calcular subtotal, IGV (18%) y total, y dejar la
 * cotizacion en estado ACEPTADA.
 *
 * Devuelve los productos y la cotizacion.
 */
public class PruebaCatalogo {

    public static DatosCatalogo ejecutar(Cliente cliente) {
        System.out.println("== PRUEBA 2: catalogo y cotizacion ==");

        // TODO estudiante 2

        List<Producto> productos = new ArrayList<>();
        Cotizacion cotizacion = new Cotizacion();

        return new DatosCatalogo(productos, cotizacion);
    }
}
