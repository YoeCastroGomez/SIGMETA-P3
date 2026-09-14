package pe.edu.pucp.ejecucion;

import java.util.List;
import pe.edu.pucp.model.producto.Producto;
import pe.edu.pucp.model.socio.Proveedor;
import pe.edu.pucp.model.usuario.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.edu.pucp.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.model.almacen.MovimientoInventario;
import pe.edu.pucp.model.almacen.RecepcionCompra;
import pe.edu.pucp.model.compras.Compra;
import pe.edu.pucp.model.compras.DetalleCompra;
import pe.edu.pucp.model.enums.EstadoCompra;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.model.enums.UnidadMedida;

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

    public static void ejecutar(
        Proveedor proveedor,
        List<Producto> productos,
        Usuario registro) {

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
}
}
