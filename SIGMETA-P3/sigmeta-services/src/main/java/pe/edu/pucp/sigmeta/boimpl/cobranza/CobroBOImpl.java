package pe.edu.pucp.sigmeta.boimpl.cobranza;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.cobranza.CobroBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.caja.CajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.MovimientoCajaDAO;
import pe.edu.pucp.sigmeta.dao.cobranza.CobroDAO;
import pe.edu.pucp.sigmeta.dao.cobranza.CuentaPorCobrarDAO;
import pe.edu.pucp.sigmeta.daoimpl.caja.CajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.MovimientoCajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CobroDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.cobranza.Cobro;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CobroBOImpl implements CobroBO {

    // documento_origen con el que el movimiento de caja apunta a su cobro
    public static final String DOCUMENTO_COBRO = "COBRO";

    private final CobroDAO cobroDAO;
    private final CuentaPorCobrarDAO cuentaPorCobrarDAO;
    private final CajaDAO cajaDAO;
    private final MovimientoCajaDAO movimientoCajaDAO;
    private final UsuarioBO usuarioBO;

    public CobroBOImpl() {
        this.cobroDAO = new CobroDAOImpl();
        this.cuentaPorCobrarDAO = new CuentaPorCobrarDAOImpl();
        this.cajaDAO = new CajaDAOImpl();
        this.movimientoCajaDAO = new MovimientoCajaDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF009, RF010, RNF002: cobro, saldo de la cuenta e ingreso en caja en una sola transaccion
    @Override
    public Cobro registrarCobro(Cobro cobro, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        validarDatos(cobro);
        try {
            CuentaPorCobrar cxc = cuentaPorCobrarDAO.load(cobro.getCuentaPorCobrar().getId());
            if (cxc == null) {
                throw new IllegalArgumentException("No existe la cuenta por cobrar con id " + cobro.getCuentaPorCobrar().getId());
            }
            if (cxc.getEstado() == EstadoCuentaPorCobrar.PAGADA) {
                throw new IllegalStateException("La cuenta por cobrar ya esta pagada");
            }
            if (cobro.getMonto() > cxc.getSaldoPendiente() + 0.005) {
                throw new IllegalArgumentException(String.format(
                        "El cobro de S/ %.2f supera el saldo pendiente de S/ %.2f", cobro.getMonto(), cxc.getSaldoPendiente()));
            }
            Caja caja = cajaDAO.obtenerCajaAbiertaPorUsuario(idResponsable);
            if (caja == null) {
                throw new IllegalStateException("El cajero debe abrir su caja antes de registrar cobros (RF010)");
            }

            cobro.setCuentaPorCobrar(cxc);
            cobro.setUsuarioRegistro(responsable);
            cobroDAO.save(cobro);

            aplicarPago(cxc, cobro.getMonto());
            cuentaPorCobrarDAO.update(cxc);

            MovimientoCaja ingreso = new MovimientoCaja();
            ingreso.setCaja(caja);
            ingreso.setTipo(TipoMovimientoCaja.COBRO_CREDITO);
            ingreso.setMedioPago(cobro.getMedioPago());
            ingreso.setMonto(cobro.getMonto());
            ingreso.setConcepto("Cobro de la cuenta por cobrar " + cxc.getId()
                    + (cxc.getVenta() != null && cxc.getVenta().getNumero() != null ? " (venta " + cxc.getVenta().getNumero() + ")" : ""));
            ingreso.setDocumentoOrigen(DOCUMENTO_COBRO);
            ingreso.setIdDocumentoOrigen(cobro.getId());
            ingreso.setUsuarioRegistro(responsable);
            movimientoCajaDAO.save(ingreso);

            transactionContext.commit();
            return cobro;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF009: monto y medio de pago ya impactaron la cuenta y la caja; para cambiarlos se anula y se registra otro cobro
    @Override
    public Cobro modificar(Cobro cobro, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Validador.obligatorio(cobro, "cobro");
        try {
            Cobro actual = obtenerExistente(cobro.getId());
            if (Math.abs(cobro.getMonto() - actual.getMonto()) > 0.005
                    || (cobro.getMedioPago() != null && cobro.getMedioPago() != actual.getMedioPago())) {
                throw new IllegalArgumentException("Para cambiar el monto o el medio de pago anule el cobro y registre uno nuevo");
            }
            if (cobro.getFechaCobro() != null) {
                validarFecha(cobro.getFechaCobro());
                actual.setFechaCobro(cobro.getFechaCobro());
            }
            actual.setReferencia(Validador.textoOpcional(cobro.getReferencia(), "referencia", 50));
            cobroDAO.update(actual);
            transactionContext.commit();
            return actual;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF009: anular el cobro devuelve el saldo a la cuenta y retira su ingreso de la caja,
    // siempre que esa caja siga abierta (una caja cerrada la reabre el Administrador)
    @Override
    public void eliminar(Cobro cobro, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Validador.obligatorio(cobro, "cobro");
        try {
            Cobro actual = obtenerExistente(cobro.getId());
            MovimientoCaja ingreso = movimientoCajaDAO.listarTodosConDetalle().stream()
                    .filter(m -> DOCUMENTO_COBRO.equals(m.getDocumentoOrigen()) && m.getIdDocumentoOrigen() == actual.getId())
                    .findFirst().orElse(null);
            if (ingreso != null) {
                Caja caja = cajaDAO.load(ingreso.getCaja().getId());
                if (caja == null || !caja.isAbierta()) {
                    throw new IllegalStateException("La caja donde se registro el cobro ya esta cerrada; "
                            + "el Administrador debe reabrirla para anularlo");
                }
                movimientoCajaDAO.remove(ingreso);
            }

            CuentaPorCobrar cxc = cuentaPorCobrarDAO.load(actual.getCuentaPorCobrar().getId());
            aplicarPago(cxc, -actual.getMonto());
            cuentaPorCobrarDAO.update(cxc);

            cobroDAO.remove(actual);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Cobro obtenerPorId(int id) throws SQLException {
        try {
            return cobroDAO.load(id);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<Cobro> listarTodos() throws SQLException {
        try {
            return cobroDAO.listarTodos();
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<Cobro> listarPorCuentaPorCobrar(int idCuentaPorCobrar) throws SQLException {
        try {
            return cobroDAO.listarPorCuentaPorCobrar(idCuentaPorCobrar);
        } finally {
            transactionContext.close();
        }
    }

    // RF009: busqueda por cliente
    @Override
    public List<Cobro> listarPorCliente(int idCliente) throws SQLException {
        try {
            return cobroDAO.listarPorCliente(idCliente);
        } finally {
            transactionContext.close();
        }
    }

    // RF009: el saldo se actualiza con cada cobro (monto positivo) o anulacion (monto negativo).
    // Criterio unico con las notas de credito: la NC rebaja monto_original (sp_comprobante_revertir_saldo_cpc)
    // y el saldo siempre es monto_original - monto_pagado.
    private void aplicarPago(CuentaPorCobrar cxc, double monto) {
        double pagado = CuentaPorCobrarBOImpl.redondear(cxc.getMontoPagado() + monto);
        cxc.setMontoPagado(pagado);
        cxc.setSaldoPendiente(CuentaPorCobrarBOImpl.redondear(Math.max(0, cxc.getMontoOriginal() - pagado)));
        cxc.setEstado(CuentaPorCobrarBOImpl.calcularEstado(cxc));
    }

    private Cobro obtenerExistente(int idCobro) throws SQLException {
        Cobro cobro = cobroDAO.load(idCobro);
        if (cobro == null) {
            throw new IllegalArgumentException("No existe el cobro con id " + idCobro);
        }
        return cobro;
    }

    private void validarDatos(Cobro cobro) {
        Validador.obligatorio(cobro, "cobro");
        Validador.obligatorio(cobro.getCuentaPorCobrar(), "cuenta por cobrar");
        Validador.obligatorio(cobro.getMedioPago(), "medio de pago");
        if (!Double.isFinite(cobro.getMonto()) || cobro.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto del cobro debe ser mayor a 0");
        }
        cobro.setMonto(CuentaPorCobrarBOImpl.redondear(cobro.getMonto()));
        if (cobro.getFechaCobro() == null) {
            cobro.setFechaCobro(LocalDate.now());
        }
        validarFecha(cobro.getFechaCobro());
        cobro.setReferencia(Validador.textoOpcional(cobro.getReferencia(), "referencia", 50));
    }

    private void validarFecha(LocalDate fechaCobro) {
        if (fechaCobro.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del cobro no puede ser futura");
        }
    }
}
