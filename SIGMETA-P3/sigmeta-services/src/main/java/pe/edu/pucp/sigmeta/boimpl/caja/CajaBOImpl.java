package pe.edu.pucp.sigmeta.boimpl.caja;

import java.util.List;
import pe.edu.pucp.sigmeta.bo.caja.CajaBO;
import pe.edu.pucp.sigmeta.dao.caja.CajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.CierreCajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.MovimientoCajaDAO;
import pe.edu.pucp.sigmeta.daoimpl.caja.CajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.CierreCajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.MovimientoCajaDAOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CajaBOImpl implements CajaBO {

    private final CajaDAO cajaDAO;
    private final MovimientoCajaDAO movimientoCajaDAO;
    private final CierreCajaDAO cierreCajaDAO;

    public CajaBOImpl() {
        this.cajaDAO = new CajaDAOImpl();
        this.movimientoCajaDAO = new MovimientoCajaDAOImpl();
        this.cierreCajaDAO = new CierreCajaDAOImpl();
    }

    @Override
    public Caja abrirCaja(Caja caja) throws Exception {
        try {
            caja.setAbierta(true);
            Caja resultado = cajaDAO.save(caja);
            transactionContext.commit();
            return resultado;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Caja modificar(Caja caja) throws Exception {
        try {
            Caja resultado = cajaDAO.update(caja);
            transactionContext.commit();
            return resultado;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public void eliminar(Caja caja) throws Exception {
        try {
            cajaDAO.remove(caja);
            transactionContext.commit();
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Caja obtenerPorId(int id) throws Exception {
        try {
            Caja resultado = cajaDAO.load(id);
            transactionContext.commit();
            return resultado;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Caja obtenerCajaAbiertaPorUsuario(int idUsuario) throws Exception {
        try {
            Caja resultado = cajaDAO.obtenerCajaAbiertaPorUsuario(idUsuario);
            transactionContext.commit();
            return resultado;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<Caja> listarTodasConUsuario() throws Exception {
        try {
            List<Caja> resultado = cajaDAO.listarTodasConUsuario();
            transactionContext.commit();
            return resultado;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public MovimientoCaja registrarMovimiento(MovimientoCaja movimiento) throws Exception {
        try {
            MovimientoCaja resultado = movimientoCajaDAO.save(movimiento);
            transactionContext.commit();
            return resultado;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<MovimientoCaja> listarMovimientosPorCaja(int idCaja) throws Exception {
        try {
            List<MovimientoCaja> resultado = movimientoCajaDAO.listarPorCaja(idCaja);
            transactionContext.commit();
            return resultado;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public double calcularMontoCalculado(int idCaja) throws Exception {
        Caja caja = cajaDAO.load(idCaja);
        if (caja == null) return 0.0;

        double montoTotal = caja.getMontoInicial();
        List<MovimientoCaja> movimientos = movimientoCajaDAO.listarPorCaja(idCaja);

        for (MovimientoCaja mov : movimientos) {
            if (mov.getTipo() == TipoMovimientoCaja.COBRO_CONTADO ||
                    mov.getTipo() == TipoMovimientoCaja.COBRO_CREDITO ||
                    mov.getTipo() == TipoMovimientoCaja.INGRESO_MANUAL) {
                montoTotal += mov.getMonto();
            } else if (mov.getTipo() == TipoMovimientoCaja.EGRESO_MANUAL) {
                montoTotal -= mov.getMonto();
            }
        }
        return montoTotal;
    }

    @Override
    public CierreCaja cerrarCaja(CierreCaja cierreCaja) throws Exception {
        try {
            // 1. Calcular monto del sistema sumando el inicial + movimientos
            double montoCalculado = calcularMontoCalculado(cierreCaja.getCaja().getId());
            cierreCaja.setMontoCalculado(montoCalculado);

            // 2. Calcular diferencia (montoDeclarado - montoCalculado)
            double diferencia = cierreCaja.getMontoDeclarado() - montoCalculado;
            cierreCaja.setDiferencia(diferencia);

            // 3. Registrar el Cierre
            CierreCaja resultado = cierreCajaDAO.save(cierreCaja);

            // 4. Marcar la caja como cerrada y actualizarla
            Caja caja = cajaDAO.load(cierreCaja.getCaja().getId());
            if (caja != null) {
                caja.setAbierta(false);
                cajaDAO.update(caja);
            }

            transactionContext.commit();
            return resultado;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public CierreCaja obtenerCierrePorCaja(int idCaja) throws Exception {
        try {
            CierreCaja resultado = cierreCajaDAO.obtenerPorCaja(idCaja);
            transactionContext.commit();
            return resultado;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }
}
