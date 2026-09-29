package pe.edu.pucp.sigmeta.boimpl.cobranza;

import java.util.List;
import pe.edu.pucp.sigmeta.bo.cobranza.CuentaPorCobrarBO;
import pe.edu.pucp.sigmeta.dao.cobranza.CuentaPorCobrarDAO;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CuentaPorCobrarBOImpl implements CuentaPorCobrarBO {

    private final CuentaPorCobrarDAO cuentaPorCobrarDAO;

    public CuentaPorCobrarBOImpl() {
        this.cuentaPorCobrarDAO = new CuentaPorCobrarDAOImpl();
    }

    @Override
    public CuentaPorCobrar insertar(CuentaPorCobrar cxc) throws Exception {
        try {
            CuentaPorCobrar resultado = cuentaPorCobrarDAO.save(cxc);
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
    public CuentaPorCobrar modificar(CuentaPorCobrar cxc) throws Exception {
        try {
            CuentaPorCobrar resultado = cuentaPorCobrarDAO.update(cxc);
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
    public void eliminar(CuentaPorCobrar cxc) throws Exception {
        try {
            cuentaPorCobrarDAO.remove(cxc);
            transactionContext.commit();
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public CuentaPorCobrar obtenerPorId(int id) throws Exception {
        try {
            CuentaPorCobrar resultado = cuentaPorCobrarDAO.load(id);
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
    public CuentaPorCobrar obtenerPorVenta(int idVenta) throws Exception {
        try {
            CuentaPorCobrar resultado = cuentaPorCobrarDAO.obtenerPorVenta(idVenta);
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
    public List<CuentaPorCobrar> listarPorCliente(int idCliente) throws Exception {
        try {
            List<CuentaPorCobrar> resultado = cuentaPorCobrarDAO.listarPorCliente(idCliente);
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
    public List<CuentaPorCobrar> listarPorEstado(EstadoCuentaPorCobrar estado) throws Exception {
        try {
            List<CuentaPorCobrar> resultado = cuentaPorCobrarDAO.listarPorEstado(estado);
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
    public List<CuentaPorCobrar> listarVencidas() throws Exception {
        try {
            List<CuentaPorCobrar> resultado = cuentaPorCobrarDAO.listarVencidas();
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
    public List<CuentaPorCobrar> listarTodasConDetalle() throws Exception {
        try {
            List<CuentaPorCobrar> resultado = cuentaPorCobrarDAO.listarTodasConDetalle();
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
