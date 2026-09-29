package pe.edu.pucp.sigmeta.boimpl.caja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.caja.MovimientoCajaBO;
import pe.edu.pucp.sigmeta.dao.caja.MovimientoCajaDAO;
import pe.edu.pucp.sigmeta.daoimpl.caja.MovimientoCajaDAOImpl;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class MovimientoCajaBOImpl implements MovimientoCajaBO {

    private final MovimientoCajaDAO movimientoCajaDAO;

    public MovimientoCajaBOImpl() {
        this.movimientoCajaDAO = new MovimientoCajaDAOImpl();
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
    public MovimientoCaja modificar(MovimientoCaja movimiento) throws Exception {
        try {
            MovimientoCaja resultado = movimientoCajaDAO.update(movimiento);
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
    public void eliminar(MovimientoCaja movimiento) throws Exception {
        try {
            movimientoCajaDAO.remove(movimiento);
            transactionContext.commit();
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public MovimientoCaja obtenerPorId(int id) throws Exception {
        try {
            MovimientoCaja resultado = movimientoCajaDAO.load(id);
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
    public List<MovimientoCaja> listarPorCaja(int idCaja) throws Exception {
        try {
            List<MovimientoCaja> lista = movimientoCajaDAO.listarPorCaja(idCaja);
            transactionContext.commit();
            return lista;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Map<MedioPago, Double> obtenerTotalesPorMedioPago(int idCaja) throws Exception {
        try {
            Map<MedioPago, Double> totales = movimientoCajaDAO.obtenerTotalesPorMedioPago(idCaja);
            transactionContext.commit();
            return totales;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<MovimientoCaja> buscarPorRangoFechas(int idCaja, LocalDateTime inicio, LocalDateTime fin) throws Exception {
        try {
            List<MovimientoCaja> lista = movimientoCajaDAO.buscarPorRangoFechas(idCaja, inicio, fin);
            transactionContext.commit();
            return lista;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<MovimientoCaja> listarTodosConDetalle() throws Exception {
        try {
            List<MovimientoCaja> lista = movimientoCajaDAO.listarTodosConDetalle();
            transactionContext.commit();
            return lista;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }
}
