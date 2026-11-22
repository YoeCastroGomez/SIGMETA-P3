package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.socio.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleCotizacionDAO;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.DetalleCotizacion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// RF007: validaciones de la cotizacion (cliente, productos, fechas), totales y guardado del detalle.
// Parte de CotizacionBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class CotizacionValidacion {

    private final DetalleCotizacionDAO detalleCotizacionDAO;
    private final ProductoDAO productoDAO;
    private final ClienteDAO clienteDAO;

    CotizacionValidacion(DetalleCotizacionDAO detalleCotizacionDAO, ProductoDAO productoDAO, ClienteDAO clienteDAO) {
        this.detalleCotizacionDAO = detalleCotizacionDAO;
        this.productoDAO = productoDAO;
        this.clienteDAO = clienteDAO;
    }

    // RF003: un cliente desactivado no puede seleccionarse en nuevos documentos
    void verificarClienteActivo(int idCliente) throws SQLException {
        Cliente cliente = clienteDAO.load(idCliente);
        if (cliente == null || !cliente.isEstado()) {
            throw new IllegalArgumentException("El cliente " + idCliente + " no existe o está desactivado");
        }
    }

    void verificarProductosActivos(List<DetalleCotizacion> detalles) throws SQLException {
        for (DetalleCotizacion detalle : detalles) {
            // RF005: un producto desactivado no puede usarse en documentos nuevos
            Producto producto = productoDAO.load(detalle.getProducto().getId());
            if (producto == null || !producto.isEstado()) {
                throw new IllegalArgumentException("El producto " + detalle.getProducto().getId()
                        + " no existe o está desactivado");
            }
            // RF005: el precio de lista lo fija el Administrador. Se cotiza siempre al precio de venta vigente del
            // producto, no al que mande la pantalla; el Vendedor solo ajusta con descuento.
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            UnidadesVenta.validarCantidad(producto, detalle.getCantidad(), detalle.getNumeroLinea());
            // RF005: no se cotiza por debajo del costo (precio efectivo = precio unitario menos el descuento
            // repartido por unidad). Una venta bajo el costo solo sale con autorizacion (ver VentaBOImpl)
            if (detalle.getCantidad() > 0) {
                double efectivo = (detalle.getCantidad() * detalle.getPrecioUnitario() - detalle.getDescuento())
                        / detalle.getCantidad();
                // el precio incluye IGV y el costo no: se comparan ambos sin IGV (PreciosIgv)
                if (PreciosIgv.sinIgv(efectivo) + 0.00005 < producto.getCostoUnitario()) {
                    throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                            "La línea %d (%s) queda en S/ %.2f por unidad sin IGV, por debajo de su costo (S/ %.2f). Reduzca el descuento",
                            detalle.getNumeroLinea(), producto.getNombre(), PreciosIgv.sinIgv(efectivo), producto.getCostoUnitario()));
                }
            }
        }
    }

    // RNF002: las lineas que siguen se actualizan en su lugar (conservan su id) y las nuevas se insertan.
    // detalle_cotizacion no tiene columna de baja logica: una linea quitada solo puede borrarse fisicamente,
    // y eso se permite unicamente mientras la cotizacion esta REGISTRADA (borrador).
    void reemplazarDetalles(Cotizacion cotizacion) throws SQLException {
        Map<Integer, DetalleCotizacion> anteriores = new HashMap<>();
        for (DetalleCotizacion anterior : detalleCotizacionDAO.listar_por_cotizacion(cotizacion.getId())) {
            anteriores.put(anterior.getNumeroLinea(), anterior);
        }
        for (DetalleCotizacion detalle : cotizacion.getDetalles()) {
            detalle.setCotizacion(cotizacion);
            DetalleCotizacion anterior = anteriores.remove(detalle.getNumeroLinea());
            if (anterior != null) {
                detalle.setId(anterior.getId());
                detalleCotizacionDAO.update(detalle);
            } else {
                detalleCotizacionDAO.save(detalle);
            }
        }
        for (DetalleCotizacion quitada : anteriores.values()) {
            detalleCotizacionDAO.remove(quitada);
        }
    }

    void guardarDetalles(Cotizacion cotizacion) throws SQLException {
        for (DetalleCotizacion detalle : cotizacion.getDetalles()) {
            detalle.setCotizacion(cotizacion);
            detalleCotizacionDAO.save(detalle);
        }
    }

    // RF007: valor de venta, IGV y total se calculan aqui (precios con IGV incluido, ver PreciosIgv);
    // no se confia en los que manda la interfaz
    void calcularTotales(Cotizacion cotizacion) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (DetalleCotizacion detalle : cotizacion.getDetalles()) {
            BigDecimal bruto = BigDecimal.valueOf(detalle.getCantidad())
                    .multiply(BigDecimal.valueOf(detalle.getPrecioUnitario()));
            BigDecimal importe = bruto.subtract(BigDecimal.valueOf(detalle.getDescuento()))
                    .setScale(2, RoundingMode.HALF_UP);
            if (importe.signum() < 0) {
                throw new IllegalArgumentException("El descuento de la línea " + detalle.getNumeroLinea()
                        + " supera el importe de la línea");
            }
            detalle.setImporte(importe.doubleValue());
            subTotal = subTotal.add(importe);
        }
        // los precios incluyen IGV: el total es la suma de los importes y se desglosa en valor de venta + IGV
        BigDecimal total = subTotal;
        cotizacion.setSubTotal(PreciosIgv.valorVenta(total).doubleValue());
        cotizacion.setIgv(PreciosIgv.igv(total).doubleValue());
        cotizacion.setTotal(total.doubleValue());
    }

    void validarCabecera(Cotizacion cotizacion) {
        Validador.obligatorio(cotizacion, "cotización");
        Validador.obligatorio(cotizacion.getCliente(), "cliente");
        Validador.obligatorio(cotizacion.getFechaEmision(), "fecha de emisión");
        Validador.obligatorio(cotizacion.getFechaVigencia(), "fecha de vigencia");
        // RF007: los importes de la cotizacion se expresan en soles
        if (cotizacion.getNumero() != null && !cotizacion.getNumero().isBlank()) {
            cotizacion.setNumero(Validador.textoObligatorio(cotizacion.getNumero(), "número", 20));
        }
        cotizacion.setObservaciones(Validador.textoOpcional(cotizacion.getObservaciones(), "observaciones", 255));
        if (cotizacion.getFechaVigencia().isBefore(cotizacion.getFechaEmision())) {
            throw new IllegalArgumentException("La fecha de vigencia no puede ser anterior a la fecha de emisión");
        }
    }

    void validarEmisionNoPasada(LocalDate fechaEmision) {
        if (fechaEmision.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de emisión no puede ser anterior a hoy");
        }
    }

    void validarDetalles(List<DetalleCotizacion> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La cotización debe tener al menos un detalle");
        }
        Set<Integer> lineas = new HashSet<>();
        for (DetalleCotizacion detalle : detalles) {
            Validador.obligatorio(detalle, "detalle de cotización");
            Validador.obligatorio(detalle.getProducto(), "producto");
            if (detalle.getNumeroLinea() <= 0 || !lineas.add(detalle.getNumeroLinea())) {
                throw new IllegalArgumentException("Los números de línea deben ser positivos y únicos");
            }
            if (!Double.isFinite(detalle.getCantidad())
                    || !Double.isFinite(detalle.getPrecioUnitario())
                    || !Double.isFinite(detalle.getDescuento())
                    || detalle.getCantidad() <= 0
                    || detalle.getPrecioUnitario() <= 0
                    || detalle.getDescuento() < 0) {
                throw new IllegalArgumentException("La línea " + detalle.getNumeroLinea()
                        + " debe tener cantidad y precio mayores a 0 y descuento no negativo");
            }
        }
    }
}
