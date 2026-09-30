package pe.edu.pucp.sigmeta.boimpl.cobranza;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.cobranza.CuentaPorCobrarBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.cobranza.CobroDAO;
import pe.edu.pucp.sigmeta.dao.cobranza.CuentaPorCobrarDAO;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CobroDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CuentaPorCobrarBOImpl implements CuentaPorCobrarBO {

    private final CuentaPorCobrarDAO cuentaPorCobrarDAO;
    private final CobroDAO cobroDAO;
    private final UsuarioBO usuarioBO;

    public CuentaPorCobrarBOImpl() {
        this.cuentaPorCobrarDAO = new CuentaPorCobrarDAOImpl();
        this.cobroDAO = new CobroDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF009: normalmente la genera la venta al credito; el registro manual es del Administrador
    @Override
    public CuentaPorCobrar insertar(CuentaPorCobrar cxc, int idResponsable) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idResponsable);
        Validador.obligatorio(cxc, "cuenta por cobrar");
        Validador.obligatorio(cxc.getVenta(), "venta");
        Validador.obligatorio(cxc.getCliente(), "cliente");
        Validador.obligatorio(cxc.getFechaEmision(), "fecha de emision");
        Validador.obligatorio(cxc.getMoneda(), "moneda");
        validarVencimiento(cxc);
        if (!Double.isFinite(cxc.getMontoOriginal()) || cxc.getMontoOriginal() <= 0) {
            throw new IllegalArgumentException("El monto original debe ser mayor a 0");
        }
        // una cuenta nueva no tiene cobros
        cxc.setMontoOriginal(redondear(cxc.getMontoOriginal()));
        cxc.setMontoPagado(0);
        cxc.setSaldoPendiente(cxc.getMontoOriginal());
        cxc.setEstado(calcularEstado(cxc));
        try {
            if (cuentaPorCobrarDAO.obtenerPorVenta(cxc.getVenta().getId()) != null) {
                throw new IllegalStateException("La venta " + cxc.getVenta().getId() + " ya tiene una cuenta por cobrar");
            }
            cuentaPorCobrarDAO.save(cxc);
            transactionContext.commit();
            return cxc;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF009: el Administrador puede refinanciar (cambiar el vencimiento); los montos solo los mueven los cobros
    @Override
    public CuentaPorCobrar modificar(CuentaPorCobrar cxc, int idResponsable) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idResponsable);
        Validador.obligatorio(cxc, "cuenta por cobrar");
        try {
            CuentaPorCobrar actual = obtenerExistente(cxc.getId());
            actual.setFechaVencimiento(cxc.getFechaVencimiento());
            validarVencimiento(actual);
            actual.setEstado(calcularEstado(actual));
            cuentaPorCobrarDAO.update(actual);
            transactionContext.commit();
            return actual;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RNF002: solo una cuenta sin cobros (generada por error); con cobros se conserva
    @Override
    public void eliminar(CuentaPorCobrar cxc, int idResponsable) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idResponsable);
        Validador.obligatorio(cxc, "cuenta por cobrar");
        try {
            CuentaPorCobrar actual = obtenerExistente(cxc.getId());
            if (!cobroDAO.listarPorCuentaPorCobrar(actual.getId()).isEmpty()) {
                throw new IllegalStateException("No se puede eliminar una cuenta por cobrar con cobros registrados");
            }
            cuentaPorCobrarDAO.remove(actual);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public CuentaPorCobrar obtenerPorId(int id) throws SQLException {
        try {
            return cuentaPorCobrarDAO.load(id);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public CuentaPorCobrar obtenerPorVenta(int idVenta) throws SQLException {
        try {
            return cuentaPorCobrarDAO.obtenerPorVenta(idVenta);
        } finally {
            transactionContext.close();
        }
    }

    // RF009: busqueda por cliente
    @Override
    public List<CuentaPorCobrar> listarPorCliente(int idCliente) throws SQLException {
        try {
            return cuentaPorCobrarDAO.listarPorCliente(idCliente);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<CuentaPorCobrar> listarPorEstado(EstadoCuentaPorCobrar estado) throws SQLException {
        Validador.obligatorio(estado, "estado");
        try {
            return cuentaPorCobrarDAO.listarPorEstado(estado);
        } finally {
            transactionContext.close();
        }
    }

    // RF009: el Administrador consulta las cuentas vencidas
    @Override
    public List<CuentaPorCobrar> listarVencidas() throws SQLException {
        try {
            return cuentaPorCobrarDAO.listarVencidas();
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<CuentaPorCobrar> listarTodasConDetalle() throws SQLException {
        try {
            return cuentaPorCobrarDAO.listarTodasConDetalle();
        } finally {
            transactionContext.close();
        }
    }

    private CuentaPorCobrar obtenerExistente(int idCuenta) throws SQLException {
        CuentaPorCobrar cxc = cuentaPorCobrarDAO.load(idCuenta);
        if (cxc == null) {
            throw new IllegalArgumentException("No existe la cuenta por cobrar con id " + idCuenta);
        }
        return cxc;
    }

    private void validarVencimiento(CuentaPorCobrar cxc) {
        Validador.obligatorio(cxc.getFechaVencimiento(), "fecha de vencimiento");
        if (cxc.getFechaVencimiento().isBefore(cxc.getFechaEmision())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de emision");
        }
    }

    // RF009: el estado se deriva del saldo y del vencimiento
    static EstadoCuentaPorCobrar calcularEstado(CuentaPorCobrar cxc) {
        if (cxc.getSaldoPendiente() <= 0.005) {
            return EstadoCuentaPorCobrar.PAGADA;
        }
        if (cxc.getFechaVencimiento().isBefore(LocalDate.now())) {
            return EstadoCuentaPorCobrar.VENCIDA;
        }
        return cxc.getMontoPagado() > 0 ? EstadoCuentaPorCobrar.PAGADA_PARCIAL : EstadoCuentaPorCobrar.PENDIENTE;
    }

    static double redondear(double monto) {
        return BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
