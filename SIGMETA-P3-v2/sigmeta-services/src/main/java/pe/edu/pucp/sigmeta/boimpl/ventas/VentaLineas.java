package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.model.ventas.DetalleOrdenCompraCliente;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// RF005/RF007/RF008: validacion de lineas, precios de lista, stock disponible, totales y enlace con la orden.
// Parte de VentaBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class VentaLineas {

    private final VentaDAO ventaDAO;
    private final DetalleVentaDAO detalleVentaDAO;
    private final ProductoDAO productoDAO;
    private final DetalleOrdenCompraClienteDAO detalleOrdenCompraClienteDAO;
    private final pe.edu.pucp.sigmeta.dao.ConsultaDAO consultaDAO;

    VentaLineas(VentaDAO ventaDAO, DetalleVentaDAO detalleVentaDAO, ProductoDAO productoDAO, DetalleOrdenCompraClienteDAO detalleOrdenCompraClienteDAO, pe.edu.pucp.sigmeta.dao.ConsultaDAO consultaDAO) {
        this.ventaDAO = ventaDAO;
        this.detalleVentaDAO = detalleVentaDAO;
        this.productoDAO = productoDAO;
        this.detalleOrdenCompraClienteDAO = detalleOrdenCompraClienteDAO;
        this.consultaDAO = consultaDAO;
    }

    void validarDetalles(List<DetalleVenta> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La venta debe tener al menos un detalle");
        }
        Set<Integer> lineas = new HashSet<>();
        for (DetalleVenta d : detalles) {
            Validador.obligatorio(d, "detalle de venta");
            Validador.obligatorio(d.getProducto(), "producto");
            if (d.getNumeroLinea() <= 0 || !lineas.add(d.getNumeroLinea())) {
                throw new IllegalArgumentException("Los números de línea deben ser positivos y únicos");
            }
            if (!Double.isFinite(d.getCantidad()) || d.getCantidad() <= 0
                    || !Double.isFinite(d.getPrecioUnitario()) || d.getPrecioUnitario() <= 0
                    || !Double.isFinite(d.getDescuento()) || d.getDescuento() < 0) {
                throw new IllegalArgumentException("La línea " + d.getNumeroLinea()
                        + " debe tener cantidad y precio mayores a 0 y descuento no negativo");
            }
        }
    }

    // RF005: el precio de lista lo fija el Administrador; la pantalla no decide el precio. Una linea que atiende una
    // orden del cliente se vende al precio de esa orden (el de la cotizacion aceptada); las demas, al precio de venta
    // vigente del producto. El Vendedor solo ajusta con descuento (ver verificarPrecios). Se llama despues de
    // enlazarLineasOrden. Tambien exige cantidades enteras en los productos que se venden por unidad.
    void fijarPreciosYCantidades(List<DetalleVenta> detalles) throws SQLException {
        for (DetalleVenta d : detalles) {
            Producto producto = productoDAO.load(d.getProducto().getId());
            if (producto == null || !producto.isEstado()) {
                throw new IllegalArgumentException("El producto no existe o está inactivo: " + d.getProducto().getId());
            }
            UnidadesVenta.validarCantidad(producto, d.getCantidad(), d.getNumeroLinea());
            d.setPrecioUnitario(d.getDetalleOrdenCompraCliente() != null
                    ? d.getDetalleOrdenCompraCliente().getPrecioUnitario()
                    : producto.getPrecioVenta());
        }
    }

    // RF008: se vende contra el stock DISPONIBLE = stock fisico - stock comprometido, donde el comprometido es
    // lo ya vendido y todavia no despachado en otras ventas vigentes. Asi dos ventas no prometen las mismas
    // unidades. idVentaExcluida: la venta que se esta modificando (0 al registrar).
    void verificarProductosYStock(List<DetalleVenta> detalles, int idVentaExcluida) throws SQLException {
        Map<Integer, BigDecimal> cantidades = new HashMap<>();
        for (DetalleVenta d : detalles) {
            cantidades.merge(d.getProducto().getId(), BigDecimal.valueOf(d.getCantidad()), BigDecimal::add);
        }
        for (var entrada : cantidades.entrySet()) {
            Producto producto = productoDAO.load(entrada.getKey());
            if (producto == null || !producto.isEstado()) {
                throw new IllegalArgumentException("El producto no existe o está inactivo: " + entrada.getKey());
            }
            BigDecimal comprometido = consultaDAO.numero("sp_stock_comprometido", producto.getId(), idVentaExcluida);
            BigDecimal disponible = BigDecimal.valueOf(producto.getStockActual()).subtract(comprometido);
            if (entrada.getValue().compareTo(disponible) > 0) {
                throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre()
                        + " (en almacén: " + sinCeros(BigDecimal.valueOf(producto.getStockActual()))
                        + ", comprometido en otras ventas: " + sinCeros(comprometido)
                        + ", disponible: " + sinCeros(disponible.max(BigDecimal.ZERO))
                        + ", solicitado: " + sinCeros(entrada.getValue()) + ")");
            }
        }
    }

    static String sinCeros(BigDecimal numero) {
        return numero.stripTrailingZeros().toPlainString();
    }

    void calcularTotales(Venta venta) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (DetalleVenta d : venta.getDetalles()) {
            BigDecimal bruto = BigDecimal.valueOf(d.getCantidad())
                    .multiply(BigDecimal.valueOf(d.getPrecioUnitario()));
            BigDecimal importe = bruto.subtract(BigDecimal.valueOf(d.getDescuento()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (importe.signum() < 0) {
                throw new IllegalArgumentException("El descuento de la línea " + d.getNumeroLinea()
                        + " supera el importe de la línea");
            }
            d.setImporte(importe.doubleValue());
            subTotal = subTotal.add(importe);
        }
        // los precios incluyen IGV: el total es la suma de los importes y se desglosa en valor de venta + IGV
        BigDecimal total = subTotal;
        venta.setSubTotal(PreciosIgv.valorVenta(total).doubleValue());
        venta.setIgv(PreciosIgv.igv(total).doubleValue());
        venta.setTotal(total.doubleValue());
    }

    void guardarDetalles(Venta venta) throws SQLException {
        for (DetalleVenta d : venta.getDetalles()) {
            d.setVenta(venta);
            d.setCantidadDespachada(0.0);
            detalleVentaDAO.save(d);
        }
    }

    void reemplazarDetalles(Venta venta) throws SQLException {
        Map<Integer, DetalleVenta> anteriores = new HashMap<>();
        for (DetalleVenta anterior : detalleVentaDAO.listarPorVenta(venta.getId())) {
            anteriores.put(anterior.getNumeroLinea(), anterior);
        }
        for (DetalleVenta d : venta.getDetalles()) {
            d.setVenta(venta);
            DetalleVenta anterior = anteriores.remove(d.getNumeroLinea());
            if (anterior != null) {
                d.setId(anterior.getId());
                detalleVentaDAO.update(d);
            } else {
                detalleVentaDAO.save(d);
            }
        }
        for (DetalleVenta quitada : anteriores.values()) {
            try {
                detalleVentaDAO.remove(quitada);
            } catch (java.sql.SQLIntegrityConstraintViolationException e) {
                // la linea figura en un despacho o una nota de credito anulados (historial): no se borra
                throw new IllegalStateException("La línea " + quitada.getNumeroLinea() + " tiene despachos o notas de crédito "
                        + "anulados en su historial y no se puede quitar. Modifique su cantidad o anule la venta y registre otra");
            }
        }
    }

    // RF007/RF008: cada linea de una venta que atiende una orden apunta a la linea de orden concreta
    void enlazarLineasOrden(Venta venta) throws SQLException {
        OrdenCompraCliente oc = venta.getOrdenCompraCliente();
        if (oc == null || oc.getId() <= 0) {
            for (DetalleVenta d : venta.getDetalles()) {
                d.setDetalleOrdenCompraCliente(null);
            }
            return;
        }
        List<DetalleOrdenCompraCliente> lineasOrden = detalleOrdenCompraClienteDAO.listarPorOrden(oc.getId());
        Set<Integer> usadas = new HashSet<>();
        for (DetalleVenta d : venta.getDetalles()) {
            DetalleOrdenCompraCliente lineaOrden = null;
            if (d.getDetalleOrdenCompraCliente() != null && d.getDetalleOrdenCompraCliente().getId() > 0) {
                int idLinea = d.getDetalleOrdenCompraCliente().getId();
                lineaOrden = lineasOrden.stream().filter(l -> l.getId() == idLinea).findFirst().orElse(null);
                if (lineaOrden == null) {
                    throw new IllegalArgumentException("La línea de orden " + idLinea + " no pertenece a la orden de la venta");
                }
            } else {
                // si no se indica, se toma la primera linea de la orden con el mismo producto aun no usada
                for (DetalleOrdenCompraCliente l : lineasOrden) {
                    if (l.getProducto().getId() == d.getProducto().getId() && !usadas.contains(l.getId())) {
                        lineaOrden = l;
                        break;
                    }
                }
                if (lineaOrden == null) {
                    throw new IllegalArgumentException("El producto " + d.getProducto().getId() + " no está en la orden de compra del cliente");
                }
            }
            if (lineaOrden.getProducto().getId() != d.getProducto().getId()) {
                throw new IllegalArgumentException("La línea " + d.getNumeroLinea() + " no corresponde al producto de su línea de orden");
            }
            if (!usadas.add(lineaOrden.getId())) {
                throw new IllegalArgumentException("Dos líneas de la venta no pueden atender la misma línea de la orden");
            }
            double comprometida = ventaDAO.cantidadComprometida(lineaOrden.getId(), venta.getId());
            if (d.getCantidad() + comprometida > lineaOrden.getCantidad() + 0.0001) {
                throw new IllegalArgumentException("La línea " + d.getNumeroLinea() + " vende más de lo pedido en la orden ("
                        + lineaOrden.getCantidad() + ")");
            }
            d.setDetalleOrdenCompraCliente(lineaOrden);
        }
    }
}
