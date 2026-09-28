package pe.edu.pucp.sigmeta.boimpl.compras;

import pe.edu.pucp.sigmeta.bo.compras.DetalleCompraBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.compras.CompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.DetalleCompraDAO;
import pe.edu.pucp.sigmeta.daoimpl.compras.CompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.compras.DetalleCompraDAOImpl;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;
import java.util.List;

public class DetalleCompraBOImpl implements DetalleCompraBO {

    private final DetalleCompraDAO detalleCompraDAO;
    private final CompraDAO compraDAO;

    public DetalleCompraBOImpl() {
        this.detalleCompraDAO = new DetalleCompraDAOImpl();
        this.compraDAO = new CompraDAOImpl();
    }

    @Override
    public DetalleCompra registrar(DetalleCompra detalle) throws SQLException {
        validarDetalle(detalle);
        if (detalle.getCantidadRecibida() != 0) {
            throw new IllegalArgumentException("Un detalle nuevo no puede tener cantidades recibidas");
        }

        try {
            obtenerCompraEditable(detalle.getCompra().getId());
            verificarNumeroLinea(detalle, 0);
            detalleCompraDAO.save(detalle);
            transactionContext.commit();
            return detalle;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public DetalleCompra modificar(DetalleCompra detalle) throws SQLException {
        validarDetalle(detalle);
        validarId(detalle.getId());

        try {
            DetalleCompra existente = obtenerExistente(detalle.getId());
            if (existente.getCompra().getId() != detalle.getCompra().getId()) {
                throw new IllegalArgumentException("No se puede trasladar el detalle a otra compra");
            }
            obtenerCompraEditable(detalle.getCompra().getId());
            if (existente.getCantidadRecibida() > 0 || detalle.getCantidadRecibida() != 0) {
                throw new IllegalArgumentException("No se puede editar un detalle que ya tiene cantidades recibidas");
            }
            verificarNumeroLinea(detalle, detalle.getId());

            detalleCompraDAO.update(detalle);
            transactionContext.commit();
            return detalle;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public void eliminar(int idDetalleCompra) throws SQLException {
        validarId(idDetalleCompra);
        try {
            DetalleCompra detalle = obtenerExistente(idDetalleCompra);
            obtenerCompraEditable(detalle.getCompra().getId());
            if (detalle.getCantidadRecibida() > 0) {
                throw new IllegalArgumentException("No se puede eliminar un detalle con cantidades recibidas");
            }
            detalleCompraDAO.remove(detalle);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public DetalleCompra obtener(int idDetalleCompra) throws SQLException {
        validarId(idDetalleCompra);
        try {
            return detalleCompraDAO.load(idDetalleCompra);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<DetalleCompra> listarTodos() throws SQLException {
        try {
            return detalleCompraDAO.listAll();
        } finally {
            transactionContext.close();
        }
    }

    private Compra obtenerCompraEditable(int idCompra) throws SQLException {
        Compra compra = compraDAO.load(idCompra);
        if (compra == null) {
            throw new IllegalArgumentException("No existe la compra con ID " + idCompra);
        }
        if (compra.isAnulado() || compra.getEstado() != EstadoCompra.REGISTRADA) {
            throw new IllegalArgumentException("Solo se pueden editar detalles de compras registradas y no anuladas");
        }
        return compra;
    }

    private DetalleCompra obtenerExistente(int idDetalleCompra) throws SQLException {
        DetalleCompra detalle = detalleCompraDAO.load(idDetalleCompra);
        if (detalle == null) {
            throw new IllegalArgumentException("No existe el detalle de compra con ID " + idDetalleCompra);
        }
        return detalle;
    }

    private void verificarNumeroLinea(DetalleCompra detalle, int idExcluido) throws SQLException {
        for (DetalleCompra otro : detalleCompraDAO.listAll()) {
            if (otro.getCompra().getId() == detalle.getCompra().getId()
                    && otro.getNumeroLinea() == detalle.getNumeroLinea()
                    && otro.getId() != idExcluido) {
                throw new IllegalArgumentException("Ya existe ese numero de linea en la compra");
            }
        }
    }

    private void validarDetalle(DetalleCompra detalle) {
        Validador.obligatorio(detalle, "detalle de compra");
        Validador.obligatorio(detalle.getCompra(), "compra");
        Validador.obligatorio(detalle.getProducto(), "producto");
        Validador.obligatorio(detalle.getUnidadCompra(), "unidad de compra");

        if (detalle.getCompra().getId() <= 0 || detalle.getProducto().getId() <= 0
                || detalle.getNumeroLinea() <= 0) {
            throw new IllegalArgumentException("Los identificadores y el numero de linea deben ser positivos");
        }
        if (!Double.isFinite(detalle.getCantidad())
                || !Double.isFinite(detalle.getPrecioUnitario())
                || !Double.isFinite(detalle.getDescuento())
                || !Double.isFinite(detalle.getImporte())
                || !Double.isFinite(detalle.getFactorConversion())
                || !Double.isFinite(detalle.getCantidadRecibida())
                || detalle.getCantidad() <= 0
                || detalle.getPrecioUnitario() < 0
                || detalle.getDescuento() < 0
                || detalle.getImporte() < 0
                || detalle.getFactorConversion() <= 0
                || detalle.getCantidadRecibida() < 0
                || detalle.getCantidadRecibida() > detalle.getCantidad()) {
            throw new IllegalArgumentException("Las cantidades e importes del detalle deben ser validos");
        }
    }

    private void validarId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo");
        }
    }
}
