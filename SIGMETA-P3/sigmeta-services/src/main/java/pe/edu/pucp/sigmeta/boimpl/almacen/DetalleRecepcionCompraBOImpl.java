package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.bo.almacen.DetalleRecepcionCompraBO;
import pe.edu.pucp.sigmeta.dao.almacen.DetalleRecepcionCompraDAO;
import pe.edu.pucp.sigmeta.daoimpl.almacen.DetalleRecepcionCompraDAOImpl;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta historica. El alta de detalles la coordina RecepcionCompraBOImpl
 * en una unica transaccion con la actualizacion del stock y de la compra.
 */
public class DetalleRecepcionCompraBOImpl implements DetalleRecepcionCompraBO {

    private final DetalleRecepcionCompraDAO detalleDAO;

    public DetalleRecepcionCompraBOImpl() {
        this.detalleDAO = new DetalleRecepcionCompraDAOImpl();
    }

    @Override
    public DetalleRecepcionCompra obtener(int idDetalleRecepcion) throws SQLException {
        validarId(idDetalleRecepcion);
        try {
            return detalleDAO.load(idDetalleRecepcion);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<DetalleRecepcionCompra> listarTodos() throws SQLException {
        try {
            return detalleDAO.listAll();
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<DetalleRecepcionCompra> listarPorRecepcion(int idRecepcion) throws SQLException {
        validarId(idRecepcion);
        try {
            List<DetalleRecepcionCompra> encontrados = new ArrayList<>();
            for (DetalleRecepcionCompra detalle : detalleDAO.listAll()) {
                if (detalle.getRecepcionCompra() != null
                        && detalle.getRecepcionCompra().getId() == idRecepcion) {
                    encontrados.add(detalle);
                }
            }
            return encontrados;
        } finally {
            transactionContext.close();
        }
    }

    private void validarId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo");
        }
    }
}
