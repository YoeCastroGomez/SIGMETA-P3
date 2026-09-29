package pe.edu.pucp.sigmeta.boimpl.caja;

import java.time.LocalDateTime;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.caja.CierreCajaBO;
import pe.edu.pucp.sigmeta.dao.caja.CajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.CierreCajaDAO;
import pe.edu.pucp.sigmeta.daoimpl.caja.CajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.CierreCajaDAOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CierreCajaBOImpl implements CierreCajaBO {

    private final CierreCajaDAO cierreCajaDAO;
    private final CajaDAO cajaDAO;

    public CierreCajaBOImpl() {
        this.cierreCajaDAO = new CierreCajaDAOImpl();
        this.cajaDAO = new CajaDAOImpl();
    }

    @Override
    public CierreCaja registrarCierre(CierreCaja cierreCaja) throws Exception {
        try {
            int idCaja = cierreCaja.getCaja().getId();

            // 1. Obtener el monto calculado mediante el SP del DAO
            double montoCalculado = cierreCajaDAO.calcularMontoEsperado(idCaja);
            cierreCaja.setMontoCalculado(montoCalculado);

            // 2. Calcular la diferencia (montoDeclarado - montoCalculado)
            double diferencia = cierreCaja.getMontoDeclarado() - montoCalculado;
            cierreCaja.setDiferencia(diferencia);

            // 3. Insertar el registro de Cierre
            CierreCaja resultado = cierreCajaDAO.save(cierreCaja);

            // 4. Marcar la caja asociada como cerrada (abierta = false)
            Caja caja = cajaDAO.load(idCaja);
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
    public CierreCaja modificar(CierreCaja cierreCaja) throws Exception {
        try {
            // Recalcular la diferencia en caso se haya modificado el monto declarado o calculado
            double diferencia = cierreCaja.getMontoDeclarado() - cierreCaja.getMontoCalculado();
            cierreCaja.setDiferencia(diferencia);

            CierreCaja resultado = cierreCajaDAO.update(cierreCaja);
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
    public void eliminar(CierreCaja cierreCaja) throws Exception {
        try {
            cierreCajaDAO.remove(cierreCaja);
            transactionContext.commit();
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public CierreCaja obtenerPorId(int id) throws Exception {
        try {
            CierreCaja resultado = cierreCajaDAO.load(id);
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
    public CierreCaja obtenerPorCaja(int idCaja) throws Exception {
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

    @Override
    public double calcularMontoEsperado(int idCaja) throws Exception {
        try {
            double monto = cierreCajaDAO.calcularMontoEsperado(idCaja);
            transactionContext.commit();
            return monto;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<CierreCaja> buscarPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws Exception {
        try {
            List<CierreCaja> lista = cierreCajaDAO.buscarPorRangoFechas(fechaInicio, fechaFin);
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
    public List<CierreCaja> listarTodosConDetalle() throws Exception {
        try {
            List<CierreCaja> lista = cierreCajaDAO.listarTodosConDetalle();
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
