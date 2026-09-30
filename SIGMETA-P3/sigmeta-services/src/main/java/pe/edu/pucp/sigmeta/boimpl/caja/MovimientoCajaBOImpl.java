package pe.edu.pucp.sigmeta.boimpl.caja;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.caja.MovimientoCajaBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.caja.CajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.MovimientoCajaDAO;
import pe.edu.pucp.sigmeta.daoimpl.caja.CajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.MovimientoCajaDAOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class MovimientoCajaBOImpl implements MovimientoCajaBO {

    private final MovimientoCajaDAO movimientoCajaDAO;
    private final CajaDAO cajaDAO;
    private final UsuarioBO usuarioBO;

    public MovimientoCajaBOImpl() {
        this.movimientoCajaDAO = new MovimientoCajaDAOImpl();
        this.cajaDAO = new CajaDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF010: cobros al contado e ingresos y egresos manuales con su concepto
    @Override
    public MovimientoCaja registrarMovimiento(MovimientoCaja movimiento, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        validarDatos(movimiento);
        if (movimiento.getTipo() == TipoMovimientoCaja.COBRO_CREDITO) {
            throw new IllegalArgumentException("Los cobros de credito se registran desde el cobro de la cuenta por cobrar (RF009)");
        }
        try {
            Caja caja = obtenerCajaAbiertaDelCajero(movimiento.getCaja().getId(), idResponsable);
            if (esEgresoEnEfectivo(movimiento)) {
                verificarEfectivoDisponible(caja, movimiento.getMonto(), 0);
            }
            movimiento.setCaja(caja);
            movimiento.setUsuarioRegistro(responsable);
            movimientoCajaDAO.save(movimiento);
            transactionContext.commit();
            return movimiento;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF010
    @Override
    public MovimientoCaja modificar(MovimientoCaja movimiento, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        validarDatos(movimiento);
        try {
            MovimientoCaja actual = obtenerExistente(movimiento.getId());
            if (!esManual(actual.getTipo()) || !esManual(movimiento.getTipo())) {
                throw new IllegalStateException("Solo se modifican ingresos y egresos manuales; los cobros se anulan desde su origen");
            }
            Caja caja = obtenerCajaAbiertaDelCajero(actual.getCaja().getId(), idResponsable);
            if (esEgresoEnEfectivo(movimiento)) {
                double liberado = esEgresoEnEfectivo(actual) ? actual.getMonto() : 0;
                verificarEfectivoDisponible(caja, movimiento.getMonto(), liberado);
            }
            movimiento.setCaja(caja);
            movimiento.setUsuarioRegistro(actual.getUsuarioRegistro());
            movimientoCajaDAO.update(movimiento);
            transactionContext.commit();
            return movimiento;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF010: solo con la caja abierta; los cobros de credito se anulan desde CobroBO
    @Override
    public void eliminar(MovimientoCaja movimiento, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Validador.obligatorio(movimiento, "movimiento de caja");
        try {
            MovimientoCaja actual = obtenerExistente(movimiento.getId());
            if (actual.getTipo() == TipoMovimientoCaja.COBRO_CREDITO) {
                throw new IllegalStateException("El ingreso de un cobro de credito se retira anulando el cobro");
            }
            Caja caja = cajaDAO.load(actual.getCaja().getId());
            if (caja == null || !caja.isAbierta()) {
                throw new IllegalStateException("No se puede eliminar un movimiento de una caja cerrada");
            }
            movimientoCajaDAO.remove(actual);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public MovimientoCaja obtenerPorId(int id) throws SQLException {
        try {
            return movimientoCajaDAO.load(id);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<MovimientoCaja> listarPorCaja(int idCaja) throws SQLException {
        try {
            return movimientoCajaDAO.listarPorCaja(idCaja);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Map<MedioPago, Double> obtenerTotalesPorMedioPago(int idCaja) throws SQLException {
        try {
            return movimientoCajaDAO.obtenerTotalesPorMedioPago(idCaja);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<MovimientoCaja> buscarPorRangoFechas(int idCaja, LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        Validador.obligatorio(inicio, "fecha de inicio");
        Validador.obligatorio(fin, "fecha de fin");
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        try {
            return movimientoCajaDAO.buscarPorRangoFechas(idCaja, inicio, fin);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<MovimientoCaja> listarTodosConDetalle() throws SQLException {
        try {
            return movimientoCajaDAO.listarTodosConDetalle();
        } finally {
            transactionContext.close();
        }
    }

    // RF010: cada cajero registra en la caja que el mismo abrio
    private Caja obtenerCajaAbiertaDelCajero(int idCaja, int idCajero) throws SQLException {
        Caja caja = cajaDAO.load(idCaja);
        if (caja == null) {
            throw new IllegalArgumentException("No existe la caja con id " + idCaja);
        }
        if (!caja.isAbierta()) {
            throw new IllegalStateException("La caja " + idCaja + " ya esta cerrada");
        }
        if (caja.getUsuarioApertura() == null || caja.getUsuarioApertura().getId() != idCajero) {
            throw new IllegalStateException("Solo el cajero que abrio la caja puede registrar sus movimientos");
        }
        return caja;
    }

    private MovimientoCaja obtenerExistente(int idMovimiento) throws SQLException {
        MovimientoCaja movimiento = movimientoCajaDAO.load(idMovimiento);
        if (movimiento == null) {
            throw new IllegalArgumentException("No existe el movimiento de caja con id " + idMovimiento);
        }
        return movimiento;
    }

    // el efectivo de la caja es el monto inicial mas el neto de los movimientos en efectivo
    private void verificarEfectivoDisponible(Caja caja, double egreso, double montoLiberado) throws SQLException {
        Double neto = movimientoCajaDAO.obtenerTotalesPorMedioPago(caja.getId()).get(MedioPago.EFECTIVO);
        double disponible = caja.getMontoInicial() + (neto != null ? neto : 0) + montoLiberado;
        if (egreso > disponible + 0.005) {
            throw new IllegalStateException(String.format(
                    "El egreso de S/ %.2f supera el efectivo disponible en caja (S/ %.2f)", egreso, disponible));
        }
    }

    private boolean esManual(TipoMovimientoCaja tipo) {
        return tipo == TipoMovimientoCaja.INGRESO_MANUAL || tipo == TipoMovimientoCaja.EGRESO_MANUAL;
    }

    private boolean esEgresoEnEfectivo(MovimientoCaja movimiento) {
        return movimiento.getTipo() == TipoMovimientoCaja.EGRESO_MANUAL
                && movimiento.getMedioPago() == MedioPago.EFECTIVO;
    }

    private void validarDatos(MovimientoCaja movimiento) {
        Validador.obligatorio(movimiento, "movimiento de caja");
        Validador.obligatorio(movimiento.getCaja(), "caja");
        Validador.obligatorio(movimiento.getTipo(), "tipo de movimiento");
        Validador.obligatorio(movimiento.getMedioPago(), "medio de pago");
        if (!Double.isFinite(movimiento.getMonto()) || movimiento.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto del movimiento debe ser mayor a 0");
        }
        movimiento.setConcepto(Validador.textoObligatorio(movimiento.getConcepto(), "concepto", 150));
        movimiento.setDocumentoOrigen(Validador.textoOpcional(movimiento.getDocumentoOrigen(), "documento de origen", 50));
        if (movimiento.getTipo() == TipoMovimientoCaja.COBRO_CONTADO
                && (movimiento.getDocumentoOrigen() == null || movimiento.getIdDocumentoOrigen() <= 0)) {
            throw new IllegalArgumentException("Un cobro al contado debe indicar la venta o comprobante de origen");
        }
    }
}
