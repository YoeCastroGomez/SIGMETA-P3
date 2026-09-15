package pe.edu.pucp.ejecucion;

import pe.edu.pucp.model.producto.Producto;
import pe.edu.pucp.model.ventas.Cotizacion;
import pe.edu.pucp.model.ventas.DetalleCotizacion;
import pe.edu.pucp.model.enums.EstadoCotizacion;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.Cliente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

        // 1. Crear los 6 Productos (usando double según tu modelo real)
        Producto p1 = new Producto(); p1.setIdProducto(1); p1.setNombre("Martillo de Acero"); p1.setPrecioVenta(45.00); p1.setCostoUnitario(25.00); p1.setStockActual(100.0);
        Producto p2 = new Producto(); p2.setIdProducto(2); p2.setNombre("Alicate de Presión"); p2.setPrecioVenta(35.00); p2.setCostoUnitario(18.00); p2.setStockActual(50.0);
        Producto p3 = new Producto(); p3.setIdProducto(3); p3.setNombre("Cable Mellizo 2x14"); p3.setPrecioVenta(2.50); p3.setCostoUnitario(1.20); p3.setStockActual(500.0);
        Producto p4 = new Producto(); p4.setIdProducto(4); p4.setNombre("Enchufe Mennekes"); p4.setPrecioVenta(85.00); p4.setCostoUnitario(50.00); p4.setStockActual(30.0);
        Producto p5 = new Producto(); p5.setIdProducto(5); p5.setNombre("Cemento Sol 42.5kg"); p5.setPrecioVenta(28.50); p5.setCostoUnitario(24.00); p5.setStockActual(200.0);
        Producto p6 = new Producto(); p6.setIdProducto(6); p6.setNombre("Fierro Corrugado"); p6.setPrecioVenta(32.00); p6.setCostoUnitario(26.00); p6.setStockActual(150.0);

        // Llenar la lista de productos
        productos.add(p1);
        productos.add(p2);
        productos.add(p3);
        productos.add(p4);
        productos.add(p5);
        productos.add(p6);

        // 2. Configurar la Cotización
        cotizacion.setIdCotizacion(1);
        cotizacion.setNumero("COT-0001");
        cotizacion.setFechaEmision(LocalDate.now());
        cotizacion.setFechaRegistro(LocalDateTime.now());
        cotizacion.setMoneda(Moneda.SOLES);
        cotizacion.setCliente(cliente);

        // 3. Crear 3 Líneas de Detalle para la cotización
        DetalleCotizacion det1 = new DetalleCotizacion();
        det1.setNumeroLinea(1);
        det1.setProducto(p1);
        det1.setCantidad(2.0);
        det1.setPrecioUnitario(p1.getPrecioVenta());
        det1.setDescuento(5.0); // Descuento aplicado

        DetalleCotizacion det2 = new DetalleCotizacion();
        det2.setNumeroLinea(2);
        det2.setProducto(p3);
        det2.setCantidad(10.0);
        det2.setPrecioUnitario(p3.getPrecioVenta());
        det2.setDescuento(0.0);

        DetalleCotizacion det3 = new DetalleCotizacion();
        det3.setNumeroLinea(3);
        det3.setProducto(p5);
        det3.setCantidad(1.0);
        det3.setPrecioUnitario(p5.getPrecioVenta());
        det3.setDescuento(0.0);

        // Agregar los detalles a la cotización
        cotizacion.getDetalles().add(det1);
        cotizacion.getDetalles().add(det2);
        cotizacion.getDetalles().add(det3);

        // 4. Calcular Matemática (Importes, Subtotal, IGV, Total)
        double subTotal = 0.0;
        for (DetalleCotizacion detalle : cotizacion.getDetalles()) {
            double totalLinea = (detalle.getCantidad() * detalle.getPrecioUnitario()) - detalle.getDescuento();
            detalle.setImporte(totalLinea);
            subTotal += totalLinea;
        }

        double igv = subTotal * 0.18;
        double total = subTotal + igv;

        cotizacion.setSubTotal(subTotal);
        cotizacion.setIgv(igv);
        cotizacion.setTotal(total);

        // 5. Estado ACEPTADA
        cotizacion.setEstado(EstadoCotizacion.ACEPTADA);

        // Pequeña impresión de confirmación en consola
        System.out.println("> Cotización " + cotizacion.getNumero() + " generada exitosamente por S/ " + String.format("%.2f", cotizacion.getTotal()));

        // 6. Retornar el objeto tal cual pide tu molde
        return new DatosCatalogo(productos, cotizacion);
    }
}