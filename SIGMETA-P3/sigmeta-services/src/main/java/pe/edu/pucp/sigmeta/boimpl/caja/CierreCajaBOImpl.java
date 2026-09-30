package pe.edu.pucp.sigmeta.boimpl.caja;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.caja.CierreCajaBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.caja.CajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.CierreCajaDAO;
import pe.edu.pucp.sigmeta.daoimpl.caja.CajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.CierreCajaDAOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CierreCajaBOImpl implements CierreCajaBO {

    private final CierreCajaDAO cierreCajaDAO;
    private final CajaDAO cajaDAO;
    private final UsuarioBO usuarioBO;

    public CierreCajaBOImpl() {
        this.cierreCajaDAO = new CierreCajaDAOImpl();
        this.cajaDAO = new CajaDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF010: compara el monto calculado por el sistema con el declarado y registra la diferencia y el responsable
    @Override
    public CierreCaja registrarCierre(CierreCaja cierreCaja, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        Validador.obligatorio(cierreCaja, "cierre de caja");
        Validador.obligatorio(cierreCaja.getCaja(), "caja");
        validarMontoDeclarado(cierreCaja.getMontoDeclarado());
        try {
            Caja caja = cajaDAO.load(cierreCaja.getCaja().getId());
            if (caja == null) {
                throw new IllegalArgumentException("No existe la caja con id " + cierreCaja.getCaja().getId());
            }
            if (!caja.isAbierta()) {
                throw new IllegalStateException("La caja " + caja.getId() + " ya esta cerrada");
            }
            if (caja.getUsuarioApertura() == null || caja.getUsuarioApertura().getId() != idResponsable) {
                throw new IllegalStateException("Solo el cajero que abrio la caja puede cerrarla");
            }
            double montoCalculado = redondear(cierreCajaDAO.calcularMontoEsperado(caja.getId()));
            cierreCaja.setCaja(caja);
            cierreCaja.setMontoCalculado(montoCalculado);
            cierreCaja.setDiferencia(redondear(cierreCaja.getMontoDeclarado() - montoCalculado));
            cierreCaja.setUsuarioCierre(responsable);
            cierreCaja.setFechaCierre(LocalDateTime.now());
            cierreCajaDAO.save(cierreCaja);

            caja.setAbierta(false);
            cajaDAO.update(caja);
            transactionContext.commit();
            return cierreCaja;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF010: unicamente el Administrador corrige una caja ya cerrada
    @Override
    public CierreCaja modificar(CierreCaja cierreCaja, int idResponsable) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idResponsable);
        Validador.obligatorio(cierreCaja, "cierre de caja");
        validarMontoDeclarado(cierreCaja.getMontoDeclarado());
        try {
            CierreCaja actual = obtenerExistente(cierreCaja.getId());
            // el monto calculado es del sistema; solo se corrige lo declarado
            actual.setMontoDeclarado(cierreCaja.getMontoDeclarado());
            actual.setDiferencia(redondear(actual.getMontoDeclarado() - actual.getMontoCalculado()));
            cierreCajaDAO.update(actual);
            transactionContext.commit();
            return actual;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF010: unicamente el Administrador reabre una caja; el SP borra el cierre y marca la caja abierta
    @Override
    public void eliminar(CierreCaja cierreCaja, int idResponsable) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idResponsable);
        Validador.obligatorio(cierreCaja, "cierre de caja");
        try {
            cierreCajaDAO.remove(obtenerExistente(cierreCaja.getId()));
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public CierreCaja obtenerPorId(int id) throws SQLException {
        try {
            return cierreCajaDAO.load(id);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public CierreCaja obtenerPorCaja(int idCaja) throws SQLException {
        try {
            return cierreCajaDAO.obtenerPorCaja(idCaja);
        } finally {
            transactionContext.close();
        }
    }

    // RF010: monto inicial + ingresos - egresos (sp_cierre_caja_calcular_monto_esperado)
    @Override
    public double calcularMontoEsperado(int idCaja) throws SQLException {
        try {
            return redondear(cierreCajaDAO.calcularMontoEsperado(idCaja));
        } finally {
            transactionContext.close();
        }
    }

    // RF015: cierres de un periodo con sus diferencias y responsables
    @Override
    public List<CierreCaja> buscarPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException {
        Validador.obligatorio(fechaInicio, "fecha de inicio");
        Validador.obligatorio(fechaFin, "fecha de fin");
        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        try {
            return cierreCajaDAO.buscarPorRangoFechas(fechaInicio, fechaFin);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<CierreCaja> listarTodosConDetalle() throws SQLException {
        try {
            return cierreCajaDAO.listarTodosConDetalle();
        } finally {
            transactionContext.close();
        }
    }

    private CierreCaja obtenerExistente(int idCierre) throws SQLException {
        CierreCaja cierre = cierreCajaDAO.load(idCierre);
        if (cierre == null) {
            throw new IllegalArgumentException("No existe el cierre de caja con id " + idCierre);
        }
        return cierre;
    }

    private void validarMontoDeclarado(double montoDeclarado) {
        if (!Double.isFinite(montoDeclarado) || montoDeclarado < 0) {
            throw new IllegalArgumentException("El monto declarado debe ser un valor no negativo");
        }
    }

    static double redondear(double monto) {
        return BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
