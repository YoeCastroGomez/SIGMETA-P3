package pe.edu.pucp.ejecucion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import pe.edu.pucp.model.enums.EstadoCotizacion;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.producto.Producto;
import pe.edu.pucp.model.socio.Cliente;
import pe.edu.pucp.model.ventas.Cotizacion;
import pe.edu.pucp.model.ventas.DetalleCotizacion;

public class PruebaCatalogo {

    public static DatosCatalogo ejecutar(Cliente cliente) {
        System.out.println("== PRUEBA 2: catalogo y cotizacion ==");

        List<Producto> productos = new ArrayList<>();
        Cotizacion cotizacion = new Cotizacion();

        Producto p1 = new Producto();
        p1.setId(1);
        p1.setNombre("Martillo de Acero");
        p1.setPrecioVenta(45.00);
        p1.setCostoUnitario(25.00);
        p1.setStockActual(100.0);

        Producto p2 = new Producto();
        p2.setId(2);
        p2.setNombre("Alicate de Presión");
        p2.setPrecioVenta(35.00);
        p2.setCostoUnitario(18.00);
        p2.setStockActual(50.0);

        Producto p3 = new Producto();
        p3.setId(3);
        p3.setNombre("Cable Mellizo 2x14");
        p3.setPrecioVenta(2.50);
        p3.setCostoUnitario(1.20);
        p3.setStockActual(500.0);

        Producto p4 = new Producto();
        p4.setId(4);
        p4.setNombre("Enchufe Mennekes");
        p4.setPrecioVenta(85.00);
        p4.setCostoUnitario(50.00);
        p4.setStockActual(30.0);

        Producto p5 = new Producto();
        p5.setId(5);
        p5.setNombre("Cemento Sol 42.5kg");
        p5.setPrecioVenta(28.50);
        p5.setCostoUnitario(24.00);
        p5.setStockActual(200.0);

        Producto p6 = new Producto();
        p6.setId(6);
        p6.setNombre("Fierro Corrugado");
        p6.setPrecioVenta(32.00);
        p6.setCostoUnitario(26.00);
        p6.setStockActual(150.0);

        productos.add(p1);
        productos.add(p2);
        productos.add(p3);
        productos.add(p4);
        productos.add(p5);
        productos.add(p6);

        cotizacion.setId(1);
        cotizacion.setNumero("COT-0001");
        cotizacion.setFechaEmision(LocalDate.now());
        cotizacion.setFechaRegistro(LocalDateTime.now());
        cotizacion.setMoneda(Moneda.SOLES);
        cotizacion.setCliente(cliente);

        DetalleCotizacion det1 = new DetalleCotizacion();
        det1.setNumeroLinea(1);
        det1.setProducto(p1);
        det1.setCantidad(2.0);
        det1.setPrecioUnitario(p1.getPrecioVenta());
        det1.setDescuento(5.0);

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

        cotizacion.getDetalles().add(det1);
        cotizacion.getDetalles().add(det2);
        cotizacion.getDetalles().add(det3);

        double subTotal = 0.0;

        for (DetalleCotizacion detalle : cotizacion.getDetalles()) {
            double totalLinea =
                    (detalle.getCantidad() * detalle.getPrecioUnitario())
                    - detalle.getDescuento();

            detalle.setImporte(totalLinea);
            subTotal += totalLinea;
        }

        double igv = subTotal * 0.18;
        double total = subTotal + igv;

        cotizacion.setSubTotal(subTotal);
        cotizacion.setIgv(igv);
        cotizacion.setTotal(total);
        cotizacion.setEstado(EstadoCotizacion.ACEPTADA);

        System.out.println(
                "> Cotizacion " + cotizacion.getNumero()
                        + " generada exitosamente por S/ "
                        + String.format("%.2f", cotizacion.getTotal())
        );

        return new DatosCatalogo(productos, cotizacion);
    }
}
