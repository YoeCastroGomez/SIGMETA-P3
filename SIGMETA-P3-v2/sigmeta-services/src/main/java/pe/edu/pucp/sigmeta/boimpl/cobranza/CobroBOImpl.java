package pe.edu.pucp.sigmeta.boimpl.cobranza;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.cobranza.CobroBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
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

public class CobroBOImpl implements CobroBO {

    // documento_origen con el que el movimiento de caja apunta a su cobro
    public static final String DOCUMENTO_COBRO = "COBRO";
    // documento_origen del egreso que compensa un cobro anulado
    public static final String DOCUMENTO_ANULACION_COBRO = "ANULACION_COBRO";

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
        return Transaccion.ejecutar(() -> {
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

            return cobro;
        });
    }

    // RF009: monto y medio de pago ya impactaron la cuenta y la caja; para cambiarlos se anula y se registra otro cobro
    @Override
    public Cobro modificar(Cobro cobro, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Validador.obligatorio(cobro, "cobro");
        return Transaccion.ejecutar(() -> {
            Cobro actual = obtenerExistente(cobro.getId());
            if (actual.isAnulado()) {
                throw new IllegalStateException("No se puede modificar un cobro anulado");
            }
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
            return actual;
        });
    }

    // RF009: anular el cobro devuelve el saldo a la cuenta y retira su ingreso de la caja,
    // siempre que esa caja siga abierta (una caja cerrada la reabre el Administrador)
    @Override
    public void eliminar(Cobro cobro, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Validador.obligatorio(cobro, "cobro");
        Transaccion.ejecutar(() -> {
            Cobro actual = obtenerExistente(cobro.getId());
            if (actual.isAnulado()) {
                throw new IllegalStateException("El cobro " + actual.getId() + " ya esta anulado");
            }
            MovimientoCaja ingreso = movimientoCajaDAO.listarTodosConDetalle().stream()
                    .filter(m -> DOCUMENTO_COBRO.equals(m.getDocumentoOrigen()) && m.getIdDocumentoOrigen() == actual.getId())
                    .findFirst().orElse(null);
            if (ingreso != null) {
                Caja caja = cajaDAO.load(ingreso.getCaja().getId());
                if (caja == null || !caja.isAbierta()) {
                    throw new IllegalStateException("La caja donde se registro el cobro ya esta cerrada; "
                            + "el Administrador debe reabrirla para anularlo");
                }
                // el ingreso no se borra: se registra un egreso que lo compensa en la misma caja
                MovimientoCaja egreso = new MovimientoCaja();
                egreso.setCaja(caja);
                egreso.setTipo(TipoMovimientoCaja.EGRESO_MANUAL);
                egreso.setMedioPago(ingreso.getMedioPago());
                egreso.setMonto(ingreso.getMonto());
                egreso.setConcepto("Anulacion del cobro " + actual.getId());
                egreso.setDocumentoOrigen(DOCUMENTO_ANULACION_COBRO);
                egreso.setIdDocumentoOrigen(actual.getId());
                egreso.setUsuarioRegistro(responsable);
                movimientoCajaDAO.save(egreso);
            }

            CuentaPorCobrar cxc = cuentaPorCobrarDAO.load(actual.getCuentaPorCobrar().getId());
            aplicarPago(cxc, -actual.getMonto());
            cuentaPorCobrarDAO.update(cxc);

            // RNF002: baja logica, el cobro queda marcado como anulado
            cobroDAO.remove(actual);
            return null;
        });
    }

    @Override
    public Cobro obtenerPorId(int id) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return cobroDAO.load(id);
        });
    }

    @Override
    public List<Cobro> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return cobroDAO.listarTodos();
        });
    }

    @Override
    public List<Cobro> listarPorCuentaPorCobrar(int idCuentaPorCobrar) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return cobroDAO.listarPorCuentaPorCobrar(idCuentaPorCobrar);
        });
    }

    // RF009: busqueda por cliente
    @Override
    public List<Cobro> listarPorCliente(int idCliente) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return cobroDAO.listarPorCliente(idCliente);
        });
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
