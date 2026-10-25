package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.almacen.DetalleRecepcionCompraBO;
import pe.edu.pucp.sigmeta.dao.almacen.DetalleRecepcionCompraDAO;
import pe.edu.pucp.sigmeta.daoimpl.almacen.DetalleRecepcionCompraDAOImpl;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;

import java.sql.SQLException;
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
        return Transaccion.ejecutar(() -> {
            return detalleDAO.load(idDetalleRecepcion);
        });
    }

    @Override
    public List<DetalleRecepcionCompra> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return detalleDAO.listAll();
        });
    }

    @Override
    public List<DetalleRecepcionCompra> listarPorRecepcion(int idRecepcion) throws SQLException {
        validarId(idRecepcion);
        return Transaccion.ejecutar(() -> {
            return detalleDAO.listarPorRecepcion(idRecepcion);
        });
    }

    private void validarId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo");
        }
    }
}
