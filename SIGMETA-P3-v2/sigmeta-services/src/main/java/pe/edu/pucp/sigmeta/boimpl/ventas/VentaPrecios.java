package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.dao.socio.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// RF005/RF014: precios bajo el costo, huella de la venta y pedido de descuento especial.
// Parte de VentaBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class VentaPrecios {

    private final VentaDAO ventaDAO;
    private final OrdenCompraClienteDAO ordenCompraClienteDAO;
    private final ProductoDAO productoDAO;
    private final ClienteDAO clienteDAO;
    private final UsuarioBO usuarioBO;
    private final pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO autorizacionBO;
    private final VentaLineas calculoLineas;

    VentaPrecios(VentaDAO ventaDAO, OrdenCompraClienteDAO ordenCompraClienteDAO, ProductoDAO productoDAO, ClienteDAO clienteDAO, UsuarioBO usuarioBO, pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO autorizacionBO, VentaLineas calculoLineas) {
        this.ventaDAO = ventaDAO;
        this.ordenCompraClienteDAO = ordenCompraClienteDAO;
        this.productoDAO = productoDAO;
        this.clienteDAO = clienteDAO;
        this.usuarioBO = usuarioBO;
        this.autorizacionBO = autorizacionBO;
        this.calculoLineas = calculoLineas;
    }

    // RF005: el precio de lista lo fija el Administrador. El Vendedor puede dar descuento, pero el precio
    // efectivo de una linea (importe / cantidad, ya con su descuento) no puede quedar por debajo del costo
    // unitario del producto. RF014: con una autorizacion vigente DESCUENTO_ESPECIAL del Administrador si se
    // permite: la autorizacion aprobada de ESTA venta (misma huella); se devuelve su id para consumirla al final
    // de la transaccion (null si no hizo falta). Se llama despues de calcularTotales, cuando cada linea tiene su importe.
    Integer verificarPrecios(Venta venta, int idResponsable) throws SQLException {
        List<String> bajoCosto = lineasBajoCosto(venta.getDetalles());
        if (bajoCosto.isEmpty()) {
            return null;
        }
        try {
            return autorizacionBO.requerirAutorizacion(idResponsable,
                    pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.OPERACION_DESCUENTO_ESPECIAL,
                    huellaVenta(venta)).getId();
        } catch (IllegalStateException e) {
            // se dice que linea queda bajo el costo y lo que falta (solicitar o esperar al Administrador)
            throw new IllegalStateException(bajoCosto.get(0) + ". " + e.getMessage());
        }
    }

    // RF005: lineas cuyo precio efectivo sin IGV queda por debajo del costo (texto para el mensaje y para el Administrador)
    List<String> lineasBajoCosto(List<DetalleVenta> detalles) throws SQLException {
        List<String> lineas = new java.util.ArrayList<>();
        for (DetalleVenta d : detalles) {
            Producto producto = productoDAO.load(d.getProducto().getId());
            // el precio incluye IGV y el costo no: se comparan ambos sin IGV (PreciosIgv)
            BigDecimal efectivo = BigDecimal.valueOf(d.getImporte())
                    .divide(BigDecimal.valueOf(d.getCantidad()), 4, RoundingMode.HALF_UP)
                    .divide(PreciosIgv.FACTOR, 4, RoundingMode.HALF_UP);
            if (efectivo.compareTo(BigDecimal.valueOf(producto.getCostoUnitario())) < 0) {
                lineas.add(String.format(java.util.Locale.ROOT,
                        "La línea %d (%s) queda en S/ %.2f por unidad sin IGV, por debajo de su costo (S/ %.2f)",
                        d.getNumeroLinea(), producto.getNombre(), efectivo.doubleValue(), producto.getCostoUnitario()));
            }
        }
        return lineas;
    }

    // RF014: huella de la venta = venta (0 si es nueva), cliente, condicion de pago y cada linea
    // (producto, cantidad e importe). Si el Vendedor cambia algo despues de la aprobacion, hay que volver a pedirla.
    static String huellaVenta(Venta venta) {
        StringBuilder datos = new StringBuilder("VENTA|").append(venta.getId())
                .append('|').append(venta.getCliente().getId())
                .append('|').append(venta.getCondicionPago());
        venta.getDetalles().stream()
                .sorted(java.util.Comparator.comparingInt((DetalleVenta d) -> d.getProducto().getId())
                        .thenComparingInt(DetalleVenta::getNumeroLinea))
                .forEach(d -> datos.append('|').append(d.getProducto().getId())
                        .append(':').append(String.format(java.util.Locale.ROOT, "%.3f", d.getCantidad()))
                        .append(':').append(String.format(java.util.Locale.ROOT, "%.2f", d.getImporte())));
        return pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.huella(datos.toString());
    }

    // RF005/RF014: el Vendedor arma la venta y, si una linea queda bajo el costo, pide la autorizacion de ESA venta.
    // No registra nada: el Administrador ve el cliente, el total y las lineas bajo el costo; al aprobar, el Vendedor
    // registra (o guarda) la misma venta.
    public pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarDescuentoEspecial(Venta venta, int idResponsable)
            throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        VentaBOImpl.validarCabecera(venta);
        calculoLineas.validarDetalles(venta.getDetalles());
        return Transaccion.ejecutar(() -> {
            Cliente cliente = clienteDAO.load(venta.getCliente().getId());
            if (cliente == null || !cliente.isEstado()) {
                throw new IllegalArgumentException("El cliente no existe o está inactivo");
            }
            venta.setCliente(cliente);
            String referencia = "Venta nueva";
            if (venta.getId() > 0) {
                Venta actual = ventaDAO.load(venta.getId());
                if (actual == null) {
                    throw new IllegalArgumentException("No existe la venta con id " + venta.getId());
                }
                referencia = actual.getNumero();
                venta.setCondicionPago(actual.getCondicionPago());
                venta.setOrdenCompraCliente(actual.getOrdenCompraCliente());
            }
            if (venta.getOrdenCompraCliente() != null && venta.getOrdenCompraCliente().getId() > 0) {
                venta.setOrdenCompraCliente(ordenCompraClienteDAO.load(venta.getOrdenCompraCliente().getId()));
            } else {
                venta.setOrdenCompraCliente(null);
            }
            calculoLineas.enlazarLineasOrden(venta);
            calculoLineas.fijarPreciosYCantidades(venta.getDetalles());
            calculoLineas.calcularTotales(venta);

            List<String> bajoCosto = lineasBajoCosto(venta.getDetalles());
            if (bajoCosto.isEmpty()) {
                throw new IllegalStateException("Ninguna línea queda por debajo del costo: no hace falta autorización");
            }
            StringBuilder detalle = new StringBuilder()
                    .append("Cliente: ").append(cliente.getNombreMostrado()).append('\n')
                    .append(String.format(java.util.Locale.ROOT, "Total: S/ %.2f (%s)", venta.getTotal(),
                            venta.getCondicionPago() == CondicionPago.CREDITO ? "crédito" : "contado"));
            for (String linea : bajoCosto) {
                detalle.append('\n').append("- ").append(linea);
            }
            return autorizacionBO.solicitarOperacion(idResponsable,
                    pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.OPERACION_DESCUENTO_ESPECIAL,
                    "Venta con descuento bajo el costo para " + cliente.getNombreMostrado(),
                    referencia + " · " + cliente.getNombreMostrado(), detalle.toString(), huellaVenta(venta));
        });
    }

    // RF005/RF014: mientras el Vendedor arma la venta, que lineas quedan bajo el costo con los precios que fijara
    // el BO (lista u orden) y su descuento. Asi la pantalla pide la autorizacion ANTES de intentar registrar.
    // Si la venta todavia esta incompleta, no se informa nada: las reglas completas se validan al registrar.
    List<Map<String, Object>> revisarCosto(Venta venta, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        if (venta == null || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return Transaccion.consultar(() -> {
            List<Map<String, Object>> resultado = new java.util.ArrayList<>();
            try {
                if (venta.getId() > 0) {
                    Venta actual = ventaDAO.load(venta.getId());
                    if (actual != null) {
                        venta.setOrdenCompraCliente(actual.getOrdenCompraCliente());
                    }
                }
                if (venta.getOrdenCompraCliente() != null && venta.getOrdenCompraCliente().getId() > 0) {
                    venta.setOrdenCompraCliente(ordenCompraClienteDAO.load(venta.getOrdenCompraCliente().getId()));
                } else {
                    venta.setOrdenCompraCliente(null);
                }
                calculoLineas.enlazarLineasOrden(venta);
                calculoLineas.fijarPreciosYCantidades(venta.getDetalles());
                calculoLineas.calcularTotales(venta);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return resultado;
            }
            for (DetalleVenta d : venta.getDetalles()) {
                if (d.getCantidad() <= 0) {
                    continue;
                }
                Producto producto = productoDAO.load(d.getProducto().getId());
                BigDecimal efectivo = BigDecimal.valueOf(d.getImporte())
                        .divide(BigDecimal.valueOf(d.getCantidad()), 4, RoundingMode.HALF_UP)
                        .divide(PreciosIgv.FACTOR, 4, RoundingMode.HALF_UP);
                if (producto != null && efectivo.compareTo(BigDecimal.valueOf(producto.getCostoUnitario())) < 0) {
                    Map<String, Object> linea = new java.util.LinkedHashMap<>();
                    linea.put("numeroLinea", d.getNumeroLinea());
                    linea.put("producto", producto.getNombre());
                    linea.put("precioSinIgv", efectivo.setScale(2, RoundingMode.HALF_UP).doubleValue());
                    linea.put("costo", producto.getCostoUnitario());
                    resultado.add(linea);
                }
            }
            return resultado;
        });
    }
}
