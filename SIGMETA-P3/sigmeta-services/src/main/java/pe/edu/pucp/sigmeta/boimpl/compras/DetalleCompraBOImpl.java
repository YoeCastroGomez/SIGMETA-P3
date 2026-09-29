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
import java.math.BigDecimal;
import java.math.RoundingMode;
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
            Compra compra = bloquearCompraEditable(detalle.getCompra().getId());
            verificarNumeroLinea(detalle, 0);
            detalleCompraDAO.save(detalle);
            recalcularTotales(compra);
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
            Compra compra = bloquearCompraEditable(detalle.getCompra().getId());
            DetalleCompra existente = obtenerExistente(detalle.getId());
            if (existente.getCompra().getId() != detalle.getCompra().getId()) {
                throw new IllegalArgumentException("No se puede trasladar el detalle a otra compra");
            }
            if (existente.getCantidadRecibida() > 0 || detalle.getCantidadRecibida() != 0) {
                throw new IllegalArgumentException("No se puede editar un detalle que ya tiene cantidades recibidas");
            }
            verificarNumeroLinea(detalle, detalle.getId());

            detalleCompraDAO.update(detalle);
            recalcularTotales(compra);
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
            Compra compra = bloquearCompraEditable(detalle.getCompra().getId());
            if (detalle.getCantidadRecibida() > 0) {
                throw new IllegalArgumentException("No se puede eliminar un detalle con cantidades recibidas");
            }
            long numeroDetalles = detalleCompraDAO.listAll().stream()
                    .filter(linea -> linea.getCompra().getId() == compra.getId()).count();
            if (numeroDetalles <= 1) {
                throw new IllegalArgumentException("No se puede eliminar el ultimo detalle de la compra");
            }
            detalleCompraDAO.remove(detalle);
            recalcularTotales(compra);
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

    private Compra bloquearCompraEditable(int idCompra) throws SQLException {
        compraDAO.bloquearCompra(idCompra);
        return obtenerCompraEditable(idCompra);
    }

    private void recalcularTotales(Compra compra) throws SQLException {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleCompra linea : detalleCompraDAO.listAll()) {
            if (linea.getCompra().getId() == compra.getId()) {
                subtotal = subtotal.add(BigDecimal.valueOf(linea.getImporte()));
            }
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        compra.setSubTotal(subtotal.doubleValue());
        compra.setIgv(igv.doubleValue());
        compra.setTotal(subtotal.add(igv).doubleValue());
        compraDAO.update(compra);
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
