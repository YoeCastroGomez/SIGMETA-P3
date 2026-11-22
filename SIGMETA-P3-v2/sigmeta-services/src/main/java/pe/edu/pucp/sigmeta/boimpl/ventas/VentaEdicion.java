package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.dao.socio.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.almacen.DespachoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import java.sql.SQLException;

// RF008/RF009: modificar una venta registrada (lineas, precios, stock y cuenta por cobrar).
// Parte de VentaBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class VentaEdicion {

    private final VentaDAO ventaDAO;
    private final OrdenCompraClienteDAO ordenCompraClienteDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final DespachoDAO despachoDAO;
    private final ClienteDAO clienteDAO;
    private final UsuarioBO usuarioBO;
    private final pe.edu.pucp.sigmeta.dao.ConsultaDAO consultaDAO;
    private final pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO autorizacionBO;
    private final VentaPrecios controlPrecios;
    private final VentaLineas calculoLineas;
    private final VentaCobranza cobranzaVenta;

    VentaEdicion(VentaDAO ventaDAO, OrdenCompraClienteDAO ordenCompraClienteDAO, ComprobanteDAO comprobanteDAO, DespachoDAO despachoDAO, ClienteDAO clienteDAO, UsuarioBO usuarioBO, pe.edu.pucp.sigmeta.dao.ConsultaDAO consultaDAO, pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO autorizacionBO, VentaPrecios controlPrecios, VentaLineas calculoLineas, VentaCobranza cobranzaVenta) {
        this.ventaDAO = ventaDAO;
        this.ordenCompraClienteDAO = ordenCompraClienteDAO;
        this.comprobanteDAO = comprobanteDAO;
        this.despachoDAO = despachoDAO;
        this.clienteDAO = clienteDAO;
        this.usuarioBO = usuarioBO;
        this.consultaDAO = consultaDAO;
        this.autorizacionBO = autorizacionBO;
        this.controlPrecios = controlPrecios;
        this.calculoLineas = calculoLineas;
        this.cobranzaVenta = cobranzaVenta;
    }

    public Venta modificar(Venta venta, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR);
        VentaBOImpl.validarCabecera(venta);
        calculoLineas.validarDetalles(venta.getDetalles());

        return Transaccion.ejecutar(() -> {
            Venta actual = ventaDAO.load(venta.getId());
            if (actual == null) {
                throw new IllegalArgumentException("No existe la venta con id " + venta.getId());
            }
            if (actual.isAnulado() || actual.getEstado() != EstadoVenta.REGISTRADA) {
                throw new IllegalStateException("Solo se puede modificar una venta en estado REGISTRADA");
            }

            // Verificar que no tenga comprobantes ni despachos VIGENTES: si se anulo la factura (por ejemplo para
            // corregir un error) o el despacho, la venta vuelve a poder corregirse antes de facturarla de nuevo
            if (comprobanteDAO.listarPorVenta(venta.getId()).stream()
                    .anyMatch(c -> c.getEstado() != pe.edu.pucp.sigmeta.model.enums.EstadoComprobante.ANULADO)) {
                throw new IllegalStateException("No se puede modificar una venta con comprobante vigente; anule primero el comprobante");
            }
            if (despachoDAO.listarPorVenta(venta.getId()).stream().anyMatch(d -> !d.isAnulado())) {
                throw new IllegalStateException("No se puede modificar una venta que ya tiene despachos registrados");
            }
            // RF010: una venta al contado ya cobrada en caja no cambia su total (el cobro quedaria descuadrado)
            if (actual.getCondicionPago() == CondicionPago.CONTADO
                    && consultaDAO.numero("sp_venta_contar_cobros_contado", venta.getId()).signum() > 0) {
                throw new IllegalStateException("La venta ya se cobró en caja y no se puede modificar; "
                        + "si hay un error, anule la venta (el cobro se devuelve en caja) y registre otra");
            }

            Cliente cliente = clienteDAO.load(venta.getCliente().getId());
            if (cliente == null || !cliente.isEstado()) {
                throw new IllegalArgumentException("El cliente está inactivo o no existe");
            }

            // la condicion de pago y la orden de origen no se cambian al modificar
            venta.setCondicionPago(actual.getCondicionPago());
            venta.setPlazoCreditoDias(actual.getPlazoCreditoDias());
            if (actual.getCondicionPago() == CondicionPago.CREDITO && !venta.getFechaEmision().equals(actual.getFechaEmision())) {
                throw new IllegalArgumentException("No se cambia la fecha de emisión de una venta al crédito; refinancie su cuenta");
            }
            if (!venta.getFechaEmision().equals(actual.getFechaEmision())) {
                VentaBOImpl.validarFechaEmision(venta.getFechaEmision());
            }
            venta.setOrdenCompraCliente(actual.getOrdenCompraCliente());
            if (venta.getOrdenCompraCliente() != null) {
                OrdenCompraCliente oc = ordenCompraClienteDAO.load(venta.getOrdenCompraCliente().getId());
                if (oc == null || oc.isAnulado() || oc.getCliente().getId() != cliente.getId()) {
                    throw new IllegalArgumentException("Orden anulada, inexistente o de otro cliente");
                }
                venta.setOrdenCompraCliente(oc);
            }
            if (actual.getCondicionPago() == CondicionPago.CREDITO && cliente.getId() != actual.getCliente().getId()) {
                throw new IllegalStateException("No se puede cambiar el cliente de una venta al crédito con cuenta por cobrar generada");
            }
            calculoLineas.enlazarLineasOrden(venta);
            calculoLineas.fijarPreciosYCantidades(venta.getDetalles());

            calculoLineas.verificarProductosYStock(venta.getDetalles(), venta.getId());
            calculoLineas.calcularTotales(venta);
            Integer idAutorizacionPrecio = controlPrecios.verificarPrecios(venta, idResponsable);
            if (venta.getCondicionPago() == CondicionPago.CREDITO) {
                cobranzaVenta.actualizarCuentaPorCobrar(venta, cliente);
            }

            venta.setEstado(actual.getEstado());
            venta.setAnulado(false);
            venta.setUsuarioRegistro(actual.getUsuarioRegistro());
            venta.setFechaRegistro(actual.getFechaRegistro());

            ventaDAO.update(venta);
            calculoLineas.reemplazarDetalles(venta);
            if (idAutorizacionPrecio != null) {
                autorizacionBO.consumir(idAutorizacionPrecio);   // un solo uso (RF014)
            }

            return venta;
        });
    }
}
