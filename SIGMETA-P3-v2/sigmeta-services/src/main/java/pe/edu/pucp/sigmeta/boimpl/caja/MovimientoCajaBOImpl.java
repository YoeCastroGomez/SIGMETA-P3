package pe.edu.pucp.sigmeta.boimpl.caja;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.caja.MovimientoCajaBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.caja.CajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.MovimientoCajaDAO;
import pe.edu.pucp.sigmeta.daoimpl.caja.CajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.MovimientoCajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.VentaDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

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
        return Transaccion.ejecutar(() -> {
            Caja caja = obtenerCajaAbiertaDelCajero(movimiento.getCaja().getId(), idResponsable);
            if (movimiento.getTipo() == TipoMovimientoCaja.COBRO_CONTADO) {
                validarCobroContado(movimiento);
            } else {
                // RF010: un ingreso o egreso manual no apunta a ningun documento; las devoluciones
                // (venta anulada, nota de credito, cobro anulado) solo las registra el sistema
                movimiento.setDocumentoOrigen(null);
                movimiento.setIdDocumentoOrigen(0);
            }
            if (esEgresoEnEfectivo(movimiento)) {
                verificarEfectivoDisponible(caja, movimiento.getMonto(), 0);
            }
            movimiento.setCaja(caja);
            movimiento.setUsuarioRegistro(responsable);
            movimientoCajaDAO.save(movimiento);
            return movimiento;
        });
    }

    // RF010
    @Override
    public MovimientoCaja modificar(MovimientoCaja movimiento, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        validarDatos(movimiento);
        return Transaccion.ejecutar(() -> {
            MovimientoCaja actual = obtenerExistente(movimiento.getId());
            if (!esManual(actual) || !esManual(movimiento.getTipo())) {
                throw new IllegalStateException("Solo se modifican ingresos y egresos manuales; los cobros se anulan desde su origen");
            }
            Caja caja = obtenerCajaAbiertaDelCajero(actual.getCaja().getId(), idResponsable);
            if (esEgresoEnEfectivo(movimiento)) {
                double liberado = esEgresoEnEfectivo(actual) ? actual.getMonto() : 0;
                verificarEfectivoDisponible(caja, movimiento.getMonto(), liberado);
            }
            movimiento.setCaja(caja);
            movimiento.setDocumentoOrigen(null);
            movimiento.setIdDocumentoOrigen(0);
            movimiento.setUsuarioRegistro(actual.getUsuarioRegistro());
            movimientoCajaDAO.update(movimiento);
            return movimiento;
        });
    }

    // RF010: solo ingresos y egresos manuales, por el cajero que abrio la caja y con la caja abierta.
    // Los cobros no se eliminan: el de credito se anula desde el cobro (RF009) y el de contado anulando la venta.
    @Override
    public void eliminar(MovimientoCaja movimiento, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        Validador.obligatorio(movimiento, "movimiento de caja");
        Transaccion.ejecutar(() -> {
            MovimientoCaja actual = obtenerExistente(movimiento.getId());
            if (actual.getTipo() == TipoMovimientoCaja.COBRO_CREDITO) {
                throw new IllegalStateException("El ingreso de un cobro de credito se retira anulando el cobro");
            }
            if (!esManual(actual)) {
                throw new IllegalStateException("Solo se eliminan ingresos y egresos manuales; los cobros se anulan desde su origen");
            }
            obtenerCajaAbiertaDelCajero(actual.getCaja().getId(), idResponsable);
            movimientoCajaDAO.remove(actual);
            return null;
        });
    }

    @Override
    public MovimientoCaja obtenerPorId(int id) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return movimientoCajaDAO.load(id);
        });
    }

    @Override
    public List<MovimientoCaja> listarPorCaja(int idCaja) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return movimientoCajaDAO.listarPorCaja(idCaja);
        });
    }

    @Override
    public Map<MedioPago, Double> obtenerTotalesPorMedioPago(int idCaja) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return movimientoCajaDAO.obtenerTotalesPorMedioPago(idCaja);
        });
    }

    @Override
    public List<MovimientoCaja> buscarPorRangoFechas(int idCaja, LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        Validador.obligatorio(inicio, "fecha de inicio");
        Validador.obligatorio(fin, "fecha de fin");
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        return Transaccion.ejecutar(() -> {
            return movimientoCajaDAO.buscarPorRangoFechas(idCaja, inicio, fin);
        });
    }

    @Override
    public List<MovimientoCaja> listarTodosConDetalle() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return movimientoCajaDAO.listarTodosConDetalle();
        });
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

    // RF010: cobro al contado de una venta. La venta debe existir, ser al contado, estar vigente y no tener
    // ya su cobro. El monto es el total de la venta (no lo digita el cajero).
    private void validarCobroContado(MovimientoCaja movimiento) throws SQLException {
        int idVenta = movimiento.getIdDocumentoOrigen();
        Venta venta = new VentaDAOImpl().load(idVenta);
        if (venta == null) {
            throw new IllegalArgumentException("No existe la venta con id " + idVenta);
        }
        if (venta.isAnulado()) {
            throw new IllegalStateException("La venta " + venta.getNumero() + " esta anulada; no se puede cobrar");
        }
        if (venta.getCondicionPago() != CondicionPago.CONTADO) {
            throw new IllegalStateException("La venta " + venta.getNumero()
                    + " es al credito; se cobra desde su cuenta por cobrar (RF009)");
        }
        if (!new CompensacionCaja().buscar(CompensacionCaja.DOCUMENTO_VENTA, idVenta).isEmpty()) {
            throw new IllegalStateException("La venta " + venta.getNumero() + " ya tiene su cobro al contado registrado");
        }
        movimiento.setMonto(CierreCajaBOImpl.redondear(venta.getTotal()));
    }

    // manual = ingreso o egreso que registro el cajero (no apunta a una venta, nota de credito ni cobro)
    private boolean esManual(MovimientoCaja movimiento) {
        return esManual(movimiento.getTipo()) && movimiento.getDocumentoOrigen() == null;
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
        // en el cobro al contado el monto lo pone el BO con el total de la venta
        if (movimiento.getTipo() != TipoMovimientoCaja.COBRO_CONTADO
                && (!Double.isFinite(movimiento.getMonto()) || movimiento.getMonto() <= 0)) {
            throw new IllegalArgumentException("El monto del movimiento debe ser mayor a 0");
        }
        movimiento.setConcepto(Validador.textoObligatorio(movimiento.getConcepto(), "concepto", 150));
        movimiento.setDocumentoOrigen(Validador.textoOpcional(movimiento.getDocumentoOrigen(), "documento de origen", 50));
        // el cobro al contado apunta a su venta: asi, si la venta se anula, su ingreso se compensa (RF010)
        if (movimiento.getTipo() == TipoMovimientoCaja.COBRO_CONTADO
                && (!CompensacionCaja.DOCUMENTO_VENTA.equals(movimiento.getDocumentoOrigen()) || movimiento.getIdDocumentoOrigen() <= 0)) {
            throw new IllegalArgumentException("Un cobro al contado debe indicar la venta de origen (documento VENTA y su id)");
        }
    }
}
