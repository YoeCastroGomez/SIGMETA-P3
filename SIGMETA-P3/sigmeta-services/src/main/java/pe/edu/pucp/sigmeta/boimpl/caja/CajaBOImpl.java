package pe.edu.pucp.sigmeta.boimpl.caja;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.caja.CajaBO;
import pe.edu.pucp.sigmeta.bo.caja.CierreCajaBO;
import pe.edu.pucp.sigmeta.bo.caja.MovimientoCajaBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.caja.CajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.CierreCajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.MovimientoCajaDAO;
import pe.edu.pucp.sigmeta.daoimpl.caja.CajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.CierreCajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.MovimientoCajaDAOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CajaBOImpl implements CajaBO {

    private final CajaDAO cajaDAO;
    private final MovimientoCajaDAO movimientoCajaDAO;
    private final CierreCajaDAO cierreCajaDAO;
    private final MovimientoCajaBO movimientoCajaBO;
    private final CierreCajaBO cierreCajaBO;
    private final UsuarioBO usuarioBO;

    public CajaBOImpl() {
        this.cajaDAO = new CajaDAOImpl();
        this.movimientoCajaDAO = new MovimientoCajaDAOImpl();
        this.cierreCajaDAO = new CierreCajaDAOImpl();
        this.movimientoCajaBO = new MovimientoCajaBOImpl();
        this.cierreCajaBO = new CierreCajaBOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF010: el Cajero abre su caja con un monto inicial; no puede tener dos cajas abiertas
    @Override
    public Caja abrirCaja(Caja caja, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        Validador.obligatorio(caja, "caja");
        validarMontoInicial(caja.getMontoInicial());
        try {
            if (cajaDAO.obtenerCajaAbiertaPorUsuario(idResponsable) != null) {
                throw new IllegalStateException("El cajero " + responsable.getNombreUsuario()
                        + " ya tiene una caja abierta; debe cerrarla antes de abrir otra");
            }
            caja.setUsuarioApertura(responsable);
            caja.setAbierta(true);
            caja.setFechaApertura(LocalDateTime.now());
            cajaDAO.save(caja);
            transactionContext.commit();
            return caja;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF010: se corrige el monto inicial; si la caja ya esta cerrada solo el Administrador,
    // y el cierre se recalcula con el nuevo monto
    @Override
    public Caja modificar(Caja caja, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Validador.obligatorio(caja, "caja");
        validarMontoInicial(caja.getMontoInicial());
        try {
            Caja actual = obtenerExistente(caja.getId());
            boolean esAdministrador = responsable.getRol().getTipo() == TipoRol.ADMINISTRADOR;
            if (!actual.isAbierta() && !esAdministrador) {
                throw new IllegalStateException("Unicamente el Administrador puede corregir una caja ya cerrada");
            }
            if (!esAdministrador && actual.getUsuarioApertura().getId() != idResponsable) {
                throw new IllegalStateException("Solo el cajero que abrio la caja puede modificarla");
            }
            actual.setMontoInicial(caja.getMontoInicial());
            cajaDAO.update(actual);

            CierreCaja cierre = cierreCajaDAO.obtenerPorCaja(actual.getId());
            if (cierre != null) {
                cierre.setMontoCalculado(CierreCajaBOImpl.redondear(cierreCajaDAO.calcularMontoEsperado(actual.getId())));
                cierre.setDiferencia(CierreCajaBOImpl.redondear(cierre.getMontoDeclarado() - cierre.getMontoCalculado()));
                cierreCajaDAO.update(cierre);
            }
            transactionContext.commit();
            return actual;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF010, RNF002: solo se elimina una apertura registrada por error (sin movimientos ni cierre)
    @Override
    public void eliminar(Caja caja, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Validador.obligatorio(caja, "caja");
        try {
            Caja actual = obtenerExistente(caja.getId());
            if (responsable.getRol().getTipo() == TipoRol.CAJERO && actual.getUsuarioApertura().getId() != idResponsable) {
                throw new IllegalStateException("Solo el cajero que abrio la caja puede eliminarla");
            }
            if (!movimientoCajaDAO.listarPorCaja(actual.getId()).isEmpty()
                    || cierreCajaDAO.obtenerPorCaja(actual.getId()) != null) {
                throw new IllegalStateException("No se puede eliminar una caja con movimientos o cierre registrados");
            }
            cajaDAO.remove(actual);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Caja obtenerPorId(int id) throws SQLException {
        try {
            return cajaDAO.load(id);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Caja obtenerCajaAbiertaPorUsuario(int idUsuario) throws SQLException {
        try {
            return cajaDAO.obtenerCajaAbiertaPorUsuario(idUsuario);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<Caja> listarTodasConUsuario() throws SQLException {
        try {
            return cajaDAO.listarTodasConUsuario();
        } finally {
            transactionContext.close();
        }
    }

    // RF010: las reglas del movimiento estan en MovimientoCajaBO
    @Override
    public MovimientoCaja registrarMovimiento(MovimientoCaja movimiento, int idResponsable) throws SQLException {
        return movimientoCajaBO.registrarMovimiento(movimiento, idResponsable);
    }

    @Override
    public List<MovimientoCaja> listarMovimientosPorCaja(int idCaja) throws SQLException {
        return movimientoCajaBO.listarPorCaja(idCaja);
    }

    // RF010: monto inicial + ingresos - egresos
    @Override
    public double calcularMontoCalculado(int idCaja) throws SQLException {
        return cierreCajaBO.calcularMontoEsperado(idCaja);
    }

    // RF010: las reglas del cierre estan en CierreCajaBO
    @Override
    public CierreCaja cerrarCaja(CierreCaja cierreCaja, int idResponsable) throws SQLException {
        return cierreCajaBO.registrarCierre(cierreCaja, idResponsable);
    }

    @Override
    public CierreCaja obtenerCierrePorCaja(int idCaja) throws SQLException {
        return cierreCajaBO.obtenerPorCaja(idCaja);
    }

    private Caja obtenerExistente(int idCaja) throws SQLException {
        Caja caja = cajaDAO.load(idCaja);
        if (caja == null) {
            throw new IllegalArgumentException("No existe la caja con id " + idCaja);
        }
        return caja;
    }

    private void validarMontoInicial(double montoInicial) {
        if (!Double.isFinite(montoInicial) || montoInicial < 0) {
            throw new IllegalArgumentException("El monto inicial de la caja no puede ser negativo");
        }
    }
}
