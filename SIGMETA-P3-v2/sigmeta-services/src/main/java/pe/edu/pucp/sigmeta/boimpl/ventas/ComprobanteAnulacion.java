package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.almacen.DespachoDAO;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.dao.ventas.ComprobanteDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleNotaCreditoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.VentaDAO;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RF008/RF013: anulacion de facturas, boletas y notas de credito (revierte stock, deuda y caja).
// Parte de ComprobanteBOImpl: la clase publica conserva sus metodos y delega aqui (mismo paquete, no publica).
final class ComprobanteAnulacion {

    private final ComprobanteDAO comprobanteDAO;
    private final DetalleNotaCreditoDAO detalleNotaCreditoDAO;
    private final VentaDAO ventaDAO;
    private final StockInventarioDAO stockDAO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final UsuarioBO usuarioBO;
    private final CompensacionCaja compensacionCaja;
    private final DespachoDAO despachoDAO;

    ComprobanteAnulacion(ComprobanteDAO comprobanteDAO, DetalleNotaCreditoDAO detalleNotaCreditoDAO, VentaDAO ventaDAO, StockInventarioDAO stockDAO, MovimientoInventarioDAO movimientoDAO, UsuarioBO usuarioBO, CompensacionCaja compensacionCaja, DespachoDAO despachoDAO) {
        this.comprobanteDAO = comprobanteDAO;
        this.detalleNotaCreditoDAO = detalleNotaCreditoDAO;
        this.ventaDAO = ventaDAO;
        this.stockDAO = stockDAO;
        this.movimientoDAO = movimientoDAO;
        this.usuarioBO = usuarioBO;
        this.compensacionCaja = compensacionCaja;
        this.despachoDAO = despachoDAO;
    }

    // RF008: anula una factura o boleta (Vendedor, Cajero o Administrador) para que la venta pueda corregirse o
    // anularse. Solo si la mercaderia NO se entrego: con despachos vigentes la devolucion se hace con nota de credito.
    // RF013: anula una nota de credito (Cajero o Administrador) revirtiendo stock, deuda y caja.
    public void anular(int idComprobante, String motivo, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.ADMINISTRADOR, TipoRol.CAJERO, TipoRol.VENDEDOR);
        Validador.textoObligatorio(motivo, "motivo de anulación", 200);

        Transaccion.ejecutar(() -> {
            Comprobante comprobante = comprobanteDAO.load(idComprobante);
            if (comprobante == null) {
                throw new IllegalArgumentException("No existe el comprobante con id " + idComprobante);
            }
            if (comprobante.getEstado() == EstadoComprobante.ANULADO) {
                throw new IllegalStateException("El comprobante ya se encuentra anulado");
            }

            if (comprobante.getTipo() == TipoComprobante.NOTA_CREDITO) {
                if (responsable.getRol().getTipo() == TipoRol.VENDEDOR) {
                    throw new IllegalStateException("El rol VENDEDOR no anula notas de crédito; lo hace el Cajero o el Administrador");
                }
                // RF013: localizar cada ingreso original del comprobante para revertirlo
                // con id_movimiento_revertido; no confundir una devolucion anulada con merma.
                List<DetalleNotaCredito> detalles = detalleNotaCreditoDAO.listarPorComprobante(idComprobante);
                Map<Integer, Double> esperadoPorProducto = new HashMap<>();
                for (DetalleNotaCredito d : detalles) {
                    int idProducto = d.getDetalleVenta().getProducto().getId();
                    if (!Double.isFinite(d.getCantidad()) || d.getCantidad() <= 0) {
                        throw new IllegalStateException("La nota tiene cantidades historicas invalidas");
                    }
                    esperadoPorProducto.merge(idProducto, d.getCantidad(), Double::sum);
                }
                List<MovimientoInventario> ingresos = movimientoDAO.listarPorComprobante(idComprobante)
                        .stream().filter(m -> m.getTipo() == TipoMovimientoInventario.INGRESO_DEVOLUCION)
                        .sorted(java.util.Comparator.comparingInt(MovimientoInventario::getId).reversed())
                        .toList();
                Map<Integer, Double> ingresadoPorProducto = new HashMap<>();
                for (MovimientoInventario original : ingresos) {
                    if (!Double.isFinite(original.getCantidad()) || original.getCantidad() <= 0
                            || movimientoDAO.existeReversion(original.getId())) {
                        throw new IllegalStateException("El ingreso de la devolucion no es reversible");
                    }
                    ingresadoPorProducto.merge(original.getProducto().getId(), original.getCantidad(), Double::sum);
                }
                if (ingresos.isEmpty() || esperadoPorProducto.size() != ingresadoPorProducto.size()) {
                    throw new IllegalStateException("Los ingresos del Kardex no coinciden con la nota de credito");
                }
                for (Map.Entry<Integer, Double> item : esperadoPorProducto.entrySet()) {
                    if (Math.abs(item.getValue() - ingresadoPorProducto.getOrDefault(item.getKey(), -1.0)) > 0.0001) {
                        throw new IllegalStateException("Los ingresos del Kardex no coinciden con la nota de credito");
                    }
                }
                for (MovimientoInventario original : ingresos) {
                    MovimientoInventario bloqueado = movimientoDAO.bloquearMovimiento(original.getId());
                    if (bloqueado == null || bloqueado.getTipo() != TipoMovimientoInventario.INGRESO_DEVOLUCION
                            || bloqueado.getComprobante() == null
                            || bloqueado.getComprobante().getId() != idComprobante
                            || movimientoDAO.existeReversion(original.getId())) {
                        throw new IllegalStateException("El movimiento de devolucion no se puede revertir");
                    }
                    int idProd = original.getProducto().getId();
                    double stockActual = stockDAO.obtenerStockParaActualizar(idProd);
                    double nuevoStock = stockActual - original.getCantidad();
                    if (!Double.isFinite(nuevoStock) || nuevoStock < -0.000001) {
                        throw new IllegalStateException("Stock insuficiente para revertir la nota de credito en el producto " + idProd);
                    }
                    nuevoStock = Math.max(0, nuevoStock);

                    MovimientoInventario inverso = new MovimientoInventario();
                    inverso.setProducto(original.getProducto());
                    inverso.setTipo(TipoMovimientoInventario.SALIDA_REVERSION_DEVOLUCION);
                    inverso.setCantidad(original.getCantidad());
                    inverso.setStockResultante(nuevoStock);
                    inverso.setComprobante(comprobante);
                    inverso.setUsuarioRegistro(responsable);
                    inverso.setMotivo("Reversion de nota de credito " + comprobante.getSerie()
                            + "-" + comprobante.getNumero());
                    inverso.setIdMovimientoRevertido(original.getId());
                    stockDAO.actualizarStock(idProd, nuevoStock);
                    movimientoDAO.registrarReversionNotaCredito(inverso);
                }

                // Revertir el efecto en la cuenta por cobrar: la deuda vuelve a su monto anterior (RF013)
                Venta venta = ventaDAO.load(comprobante.getVenta().getId());
                // RF013 + RNF001: queda constancia de quien anulo la nota, cuando y por que (historial de la
                // cuenta por cobrar y reporte de caja)
                Integer idCuenta = null;
                if (venta != null && venta.getCondicionPago() == pe.edu.pucp.sigmeta.model.enums.CondicionPago.CREDITO) {
                    pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar cxc =
                            new pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl().obtenerPorVenta(venta.getId());
                    idCuenta = cxc == null ? null : cxc.getId();
                }
                pe.edu.pucp.sigmeta.boimpl.cobranza.RegistroAnulacion.registrar(
                        pe.edu.pucp.sigmeta.boimpl.cobranza.RegistroAnulacion.NOTA_CREDITO, idComprobante,
                        comprobante.getSerie() + "-" + comprobante.getNumero(), idCuenta, comprobante.getTotal(),
                        motivo, responsable.getId());
                if (venta != null && venta.getCondicionPago() == pe.edu.pucp.sigmeta.model.enums.CondicionPago.CREDITO) {
                    // la deuda vuelve solo por la parte que la nota rebajo; lo que se devolvio en efectivo
                    // se revierte abajo, con su ingreso compensatorio en la caja
                    BigDecimal devueltoEnCaja = BigDecimal.ZERO;
                    for (MovimientoCaja devolucion : compensacionCaja.buscar(CompensacionCaja.DOCUMENTO_NOTA_CREDITO, idComprobante)) {
                        devueltoEnCaja = devueltoEnCaja.add(BigDecimal.valueOf(devolucion.getMonto()));
                    }
                    BigDecimal aLaDeuda = BigDecimal.valueOf(comprobante.getTotal()).subtract(devueltoEnCaja)
                            .setScale(2, RoundingMode.HALF_UP);
                    if (aLaDeuda.signum() > 0) {
                        comprobanteDAO.restaurarSaldoCuentaPorCobrar(venta.getId(), aLaDeuda.doubleValue());
                    }
                }
                // Revertir la devolucion de dinero en caja de una venta al contado (RF010)
                for (MovimientoCaja devolucion : compensacionCaja.buscar(CompensacionCaja.DOCUMENTO_NOTA_CREDITO, idComprobante)) {
                    compensacionCaja.compensar(devolucion, "Anulación de la NC " + comprobante.getSerie() + "-" + comprobante.getNumero(),
                            CompensacionCaja.DOCUMENTO_ANULACION_NOTA_CREDITO, idComprobante, responsable);
                }
            } else {
                // Validar que no tenga notas de credito activas
                List<Comprobante> ncs = comprobanteDAO.listarNotasCreditoPorComprobante(idComprobante);
                if (!ncs.isEmpty()) {
                    throw new IllegalStateException("No se puede anular un comprobante que tiene notas de crédito activas");
                }
                // RF008/RF012: si la mercaderia ya salio del almacen, el comprobante no se anula: se emite nota de credito
                if (comprobante.getVenta() != null && despachoDAO.listarPorVenta(comprobante.getVenta().getId())
                        .stream().anyMatch(d -> !d.isAnulado())) {
                    throw new IllegalStateException("La venta ya tiene mercadería despachada: para devolverla emita una nota de crédito. "
                            + "Si el despacho fue un error, anúlelo primero en almacén");
                }
            }

            comprobanteDAO.actualizarEstado(idComprobante, EstadoComprobante.ANULADO);
            return null;
        });
    }
}
