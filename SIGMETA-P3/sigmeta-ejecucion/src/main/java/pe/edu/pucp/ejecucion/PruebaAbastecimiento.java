package pe.edu.pucp.ejecucion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import pe.edu.pucp.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.model.almacen.MovimientoInventario;
import pe.edu.pucp.model.almacen.RecepcionCompra;
import pe.edu.pucp.model.compras.Compra;
import pe.edu.pucp.model.compras.DetalleCompra;
import pe.edu.pucp.model.enums.EstadoCompra;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.model.producto.Producto;
import pe.edu.pucp.model.socio.Proveedor;
import pe.edu.pucp.model.usuario.Usuario;

public class PruebaAbastecimiento {

    public static void ejecutar(Proveedor proveedor, List<Producto> productos, Usuario registro) {
        System.out.println("== PRUEBA 3: compras e inventario ==");

        if (productos == null || productos.size() < 2) {
            System.out.println("Se necesitan al menos 2 productos.");
            return;
        }

        Producto producto1 = productos.get(0);
        Producto producto2 = productos.get(1);

        Compra compra = new Compra();
        compra.setIdCompra(1);
        compra.setProveedor(proveedor);
        compra.setEstado(EstadoCompra.REGISTRADA);
        compra.setFechaRecepcionEstimada(LocalDate.now().plusDays(2));
        compra.setNumero("COMP-001");
        compra.setFechaEmision(LocalDate.now());
        compra.setMoneda(Moneda.SOLES);
        compra.setSubTotal(500.00);
        compra.setIgv(90.00);
        compra.setTotal(590.00);
        compra.setObservaciones("Compra de prueba");
        compra.setFechaRegistro(LocalDateTime.now());
        compra.setUsuarioRegistro(registro);

        DetalleCompra detalle1 = new DetalleCompra();
        detalle1.setIdDetalleCompra(1);
        detalle1.setCompra(compra);
        detalle1.setNumeroLinea(1);
        detalle1.setProducto(producto1);
        detalle1.setCantidad(10);
        detalle1.setPrecioUnitario(30);
        detalle1.setDescuento(0);
        detalle1.setImporte(300);
        detalle1.setUnidadCompra(producto1.getUnidadCompra());
        detalle1.setFactorConversion(producto1.getFactorConversion());
        detalle1.setCantidadRecibida(0);

        DetalleCompra detalle2 = new DetalleCompra();
        detalle2.setIdDetalleCompra(2);
        detalle2.setCompra(compra);
        detalle2.setNumeroLinea(2);
        detalle2.setProducto(producto2);
        detalle2.setCantidad(5);
        detalle2.setPrecioUnitario(40);
        detalle2.setDescuento(0);
        detalle2.setImporte(200);
        detalle2.setUnidadCompra(producto2.getUnidadCompra());
        detalle2.setFactorConversion(producto2.getFactorConversion());
        detalle2.setCantidadRecibida(0);

        compra.getDetalles().add(detalle1);
        compra.getDetalles().add(detalle2);

        RecepcionCompra recepcion = new RecepcionCompra();
        recepcion.setIdRecepcionCompra(1);
        recepcion.setCompra(compra);
        recepcion.setFechaRecepcion(LocalDate.now());
        recepcion.setObservaciones("Primera recepcion parcial");
        recepcion.setUsuarioRegistro(registro);
        recepcion.setFechaRegistro(LocalDateTime.now());

        compra.getRecepciones().add(recepcion);

        double cantidadRecibida1 = 4;
        double cantidadRecibida2 = 2;

        DetalleRecepcionCompra recepcionDetalle1 = new DetalleRecepcionCompra();
        recepcionDetalle1.setIdDetalleRecepcionCompra(1);
        recepcionDetalle1.setRecepcionCompra(recepcion);
        recepcionDetalle1.setDetalleCompra(detalle1);
        recepcionDetalle1.setCantidadRecibida(cantidadRecibida1);

        DetalleRecepcionCompra recepcionDetalle2 = new DetalleRecepcionCompra();
        recepcionDetalle2.setIdDetalleRecepcionCompra(2);
        recepcionDetalle2.setRecepcionCompra(recepcion);
        recepcionDetalle2.setDetalleCompra(detalle2);
        recepcionDetalle2.setCantidadRecibida(cantidadRecibida2);

        recepcion.getDetalles().add(recepcionDetalle1);
        recepcion.getDetalles().add(recepcionDetalle2);

        detalle1.setCantidadRecibida(cantidadRecibida1);
        detalle2.setCantidadRecibida(cantidadRecibida2);
        compra.setEstado(EstadoCompra.RECIBIDA_PARCIAL);

        double stockInicial1 = producto1.getStockActual();
        double stockInicial2 = producto2.getStockActual();

        double factor1 = producto1.getFactorConversion();
        double factor2 = producto2.getFactorConversion();

        if (factor1 <= 0) factor1 = 1;
        if (factor2 <= 0) factor2 = 1;

        double ingresoStock1 = cantidadRecibida1 * factor1;
        double ingresoStock2 = cantidadRecibida2 * factor2;

        producto1.setStockActual(stockInicial1 + ingresoStock1);
        producto2.setStockActual(stockInicial2 + ingresoStock2);

        MovimientoInventario movimiento1 = new MovimientoInventario(
                1, producto1, TipoMovimientoInventario.INGRESO_COMPRA,
                LocalDateTime.now(), ingresoStock1, producto1.getStockActual(),
                recepcion, null, null, registro,
                "Ingreso por recepcion parcial de compra", 0
        );

        MovimientoInventario movimiento2 = new MovimientoInventario(
                2, producto2, TipoMovimientoInventario.INGRESO_COMPRA,
                LocalDateTime.now(), ingresoStock2, producto2.getStockActual(),
                recepcion, null, null, registro,
                "Ingreso por recepcion parcial de compra", 0
        );

        producto1.getMovimientos().add(movimiento1);
        producto2.getMovimientos().add(movimiento2);

        boolean stockCorrecto1 = movimiento1.getStockResultante() == producto1.getStockActual();
        boolean stockCorrecto2 = movimiento2.getStockResultante() == producto2.getStockActual();

        System.out.println("Compra: " + compra.getNumero());
        System.out.println("Estado: " + compra.getEstado());
        System.out.println(producto1.getNombre() + " | Stock final: " + producto1.getStockActual() + " | Correcto: " + stockCorrecto1);
        System.out.println(producto2.getNombre() + " | Stock final: " + producto2.getStockActual() + " | Correcto: " + stockCorrecto2);
    }
}
