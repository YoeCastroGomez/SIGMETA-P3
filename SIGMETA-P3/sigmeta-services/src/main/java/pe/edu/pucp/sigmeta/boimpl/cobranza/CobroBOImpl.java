package pe.edu.pucp.sigmeta.boimpl.cobranza;

import java.util.List;
import pe.edu.pucp.sigmeta.bo.cobranza.CobroBO;
import pe.edu.pucp.sigmeta.dao.cobranza.CobroDAO;
import pe.edu.pucp.sigmeta.dao.cobranza.CuentaPorCobrarDAO;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CobroDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl;
import pe.edu.pucp.sigmeta.model.cobranza.Cobro;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CobroBOImpl implements CobroBO {

    private final CobroDAO cobroDAO;
    private final CuentaPorCobrarDAO cuentaPorCobrarDAO;

    public CobroBOImpl() {
        this.cobroDAO = new CobroDAOImpl();
        this.cuentaPorCobrarDAO = new CuentaPorCobrarDAOImpl();
    }

    @Override
    public Cobro registrarCobro(Cobro cobro) throws Exception {
        try {
            // 1. Guardar el cobro
            Cobro nuevoCobro = cobroDAO.save(cobro);

            // 2. Cargar y actualizar el saldo/estado de la Cuenta por Cobrar
            CuentaPorCobrar cxc = cuentaPorCobrarDAO.load(cobro.getCuentaPorCobrar().getId());
            if (cxc != null) {
                double nuevoMontoPagado = cxc.getMontoPagado() + cobro.getMonto();
                double nuevoSaldo = cxc.getMontoOriginal() - nuevoMontoPagado;

                cxc.setMontoPagado(nuevoMontoPagado);
                cxc.setSaldoPendiente(Math.max(0.0, nuevoSaldo));

                if (cxc.getSaldoPendiente() <= 0.01) {
                    cxc.setEstado(EstadoCuentaPorCobrar.PAGADA);
                } else {
                    cxc.setEstado(EstadoCuentaPorCobrar.PAGADA_PARCIAL);
                }

                cuentaPorCobrarDAO.update(cxc);
            }

            transactionContext.commit();
            return nuevoCobro;
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Cobro modificar(Cobro cobro) throws Exception {
        try {
            Cobro resultado = cobroDAO.update(cobro);
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
    public void eliminar(Cobro cobro) throws Exception {
        try {
            cobroDAO.remove(cobro);
            transactionContext.commit();
        } catch (Exception e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Cobro obtenerPorId(int id) throws Exception {
        try {
            Cobro resultado = cobroDAO.load(id);
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
    public List<Cobro> listarPorCuentaPorCobrar(int idCuentaPorCobrar) throws Exception {
        try {
            List<Cobro> resultado = cobroDAO.listarPorCuentaPorCobrar(idCuentaPorCobrar);
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
    public List<Cobro> listarPorCliente(int idCliente) throws Exception {
        try {
            List<Cobro> resultado = cobroDAO.listarPorCliente(idCliente);
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
