package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.boimpl.cobranza.CuentaPorCobrarBOImpl;
import pe.edu.pucp.sigmeta.dao.cobranza.CuentaPorCobrarDAO;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.almacen.DespachoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import java.math.BigDecimal;
import java.sql.SQLException;

// RF008/RF009: anulacion de la venta y su cuenta por cobrar.
// Parte de VentaBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class VentaCobranza {

    private final VentaDAO ventaDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final DespachoDAO despachoDAO;
    private final UsuarioBO usuarioBO;
    private final CuentaPorCobrarDAO cuentaPorCobrarDAO;
    private final CompensacionCaja compensacionCaja;

    VentaCobranza(VentaDAO ventaDAO, ComprobanteDAO comprobanteDAO, DespachoDAO despachoDAO, UsuarioBO usuarioBO, CuentaPorCobrarDAO cuentaPorCobrarDAO, CompensacionCaja compensacionCaja) {
        this.ventaDAO = ventaDAO;
        this.comprobanteDAO = comprobanteDAO;
        this.despachoDAO = despachoDAO;
        this.usuarioBO = usuarioBO;
        this.cuentaPorCobrarDAO = cuentaPorCobrarDAO;
        this.compensacionCaja = compensacionCaja;
    }

    public void anular(int idVenta, String motivo, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.ADMINISTRADOR);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo de anulación", 200);

        Transaccion.ejecutar(() -> {
            Venta venta = ventaDAO.load(idVenta);
            if (venta == null) {
                throw new IllegalArgumentException("No existe la venta con id " + idVenta);
            }
            if (venta.isAnulado()) {
                throw new IllegalStateException("La venta ya se encuentra anulada");
            }

            // Verificar si tiene comprobantes vigentes
            if (comprobanteDAO.listarPorVenta(idVenta).stream().anyMatch(c -> c.getEstado() != pe.edu.pucp.sigmeta.model.enums.EstadoComprobante.ANULADO)) {
                throw new IllegalStateException("No se puede anular una venta con comprobantes vigentes");
            }

            // Verificar si tiene despachos vigentes
            if (despachoDAO.listarPorVenta(idVenta).stream().anyMatch(d -> !d.isAnulado())) {
                throw new IllegalStateException("No se puede anular una venta con despachos vigentes");
            }

            // RF010: el cobro al contado de una venta anulada no es un cobro valido: se compensa en su caja
            for (MovimientoCaja cobro : compensacionCaja.buscar(CompensacionCaja.DOCUMENTO_VENTA, idVenta)) {
                compensacionCaja.compensar(cobro, "Devolución por anulación de la venta " + venta.getNumero(),
                        CompensacionCaja.DOCUMENTO_ANULACION_VENTA, idVenta, responsable);
            }
            // La cuenta solo se puede retirar despues de anular la venta, en la misma transaccion.
            venta.setMotivoAnulacion(motivoValidado);
            ventaDAO.remove(venta);
            if (venta.getCondicionPago() == CondicionPago.CREDITO) {
                cerrarCuentaPorCobrar(idVenta);
            }
            return null;
        });
    }

    // RF009: al modificar una venta al credito su cuenta por cobrar refleja el nuevo total
    // y se vuelve a validar el limite de credito del cliente
    void actualizarCuentaPorCobrar(Venta venta, Cliente cliente) throws SQLException {
        CuentaPorCobrar cxc = cuentaPorCobrarDAO.obtenerPorVenta(venta.getId());
        if (cxc == null) {
            throw new IllegalStateException("La venta al crédito no tiene cuenta por cobrar; repare la integridad antes de modificar");
        }
        // RF013: lo que acreditaron las notas de credito sigue descontandose del nuevo total
        double deudaNeta = venta.getTotal() - cxc.getMontoAcreditado();
        if (deudaNeta + 0.005 < cxc.getMontoPagado()) {
            throw new IllegalStateException("El nuevo total (S/ " + venta.getTotal() + ") es menor que lo ya cobrado (S/ "
                    + cxc.getMontoPagado() + ")");
        }
        double nuevoSaldo = CuentaPorCobrarBOImpl.redondear(deudaNeta - cxc.getMontoPagado());
        BigDecimal deudaCliente = ventaDAO.obtenerSaldoPendienteCliente(cliente.getId());
        if (deudaCliente.subtract(BigDecimal.valueOf(cxc.getSaldoPendiente())).add(BigDecimal.valueOf(nuevoSaldo)).compareTo(cliente.getLimiteCredito()) > 0) {
            throw new IllegalStateException("La venta modificada supera el límite de crédito del cliente (Límite: "
                    + cliente.getLimiteCredito() + ")");
        }
        cxc.setMontoOriginal(venta.getTotal());
        cxc.setSaldoPendiente(nuevoSaldo);
        // Conservar el vencimiento vigente, incluyendo refinanciaciones autorizadas.
        cxc.setEstado(CuentaPorCobrarBOImpl.calcularEstado(cxc));
        cuentaPorCobrarDAO.update(cxc);
    }

    // RF009, RNF002: una venta al credito anulada deja de generar deuda. Con cobros vigentes no se puede anular.
    // Su cuenta NO se borra (antes se borraba si no tenia cobros y, si los tenia anulados, quedaba "PAGADA" sin
    // que nadie pagara): queda ANULADA, con su monto original (lo que fue la venta) y saldo 0. Igual en los dos casos.
    void cerrarCuentaPorCobrar(int idVenta) throws SQLException {
        CuentaPorCobrar cxc = cuentaPorCobrarDAO.obtenerPorVenta(idVenta);
        if (cxc == null) {
            return;
        }
        if (cxc.getMontoPagado() > 0.005) {
            throw new IllegalStateException("La venta tiene cobros registrados; anule primero los cobros de su cuenta por cobrar");
        }
        cxc.setSaldoPendiente(0);
        cxc.setEstado(pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar.ANULADA);
        cuentaPorCobrarDAO.update(cxc);
    }
}
