package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.bo.almacen.RecepcionCompraBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.almacen.DetalleRecepcionCompraDAO;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.RecepcionCompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.CompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.DetalleCompraDAO;
import pe.edu.pucp.sigmeta.daoimpl.almacen.DetalleRecepcionCompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.MovimientoInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.RecepcionCompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.compras.CompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.compras.DetalleCompraDAOImpl;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecepcionCompraBOImpl implements RecepcionCompraBO {

    private static final double EPSILON = 0.000001;

    private final RecepcionCompraDAO recepcionDAO;
    private final DetalleRecepcionCompraDAO detalleRecepcionDAO;
    private final DetalleCompraDAO detalleCompraDAO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final CompraDAO compraDAO;

    public RecepcionCompraBOImpl() {
        this.recepcionDAO = new RecepcionCompraDAOImpl();
        this.detalleRecepcionDAO = new DetalleRecepcionCompraDAOImpl();
        this.detalleCompraDAO = new DetalleCompraDAOImpl();
        this.movimientoDAO = new MovimientoInventarioDAOImpl();
        this.compraDAO = new CompraDAOImpl();
    }

    @Override
    public RecepcionCompra registrar(RecepcionCompra recepcion) throws SQLException {
        validarRecepcion(recepcion);
        List<DetalleRecepcionCompra> detalles = recepcion.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La recepcion debe tener al menos un detalle");
        }

        try {
            Connection conn = transactionContext.getConnection();
            int idCompra = recepcion.getCompra().getId();

            // Serializa las recepciones de la misma compra y evita recibir cantidades en exceso.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM compra WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, idCompra);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("No existe la compra con ID " + idCompra);
                    }
                }
            }

            Compra compra = compraDAO.load(idCompra);
            if (compra.isAnulado() || (compra.getEstado() != EstadoCompra.REGISTRADA
                    && compra.getEstado() != EstadoCompra.RECIBIDA_PARCIAL)) {
                throw new IllegalArgumentException("La compra no admite nuevas recepciones");
            }

            Map<Integer, DetalleCompra> lineas = new HashMap<>();
            for (DetalleCompra linea : detalleCompraDAO.listAll()) {
                if (linea.getCompra().getId() == idCompra) {
                    lineas.put(linea.getId(), linea);
                }
            }
            if (lineas.isEmpty()) {
                throw new IllegalArgumentException("La compra no tiene detalles");
            }

            Set<Integer> idsRecibidos = new HashSet<>();
            for (DetalleRecepcionCompra detalle : detalles) {
                Validador.obligatorio(detalle, "detalle de recepcion");
                Validador.obligatorio(detalle.getDetalleCompra(), "detalle de compra");
                int idLinea = detalle.getDetalleCompra().getId();
                if (!idsRecibidos.add(idLinea)) {
                    throw new IllegalArgumentException("Un detalle de compra no puede repetirse en una recepcion");
                }
                DetalleCompra linea = lineas.get(idLinea);
                if (linea == null) {
                    throw new IllegalArgumentException("El detalle no pertenece a la compra");
                }
                double cantidad = detalle.getCantidadRecibida();
                if (!Double.isFinite(cantidad) || cantidad <= 0) {
                    throw new IllegalArgumentException("La cantidad recibida debe ser positiva");
                }
                if (cantidad + linea.getCantidadRecibida() > linea.getCantidad() + EPSILON) {
                    throw new IllegalArgumentException("La recepcion excede la cantidad pendiente de la linea " + linea.getNumeroLinea());
                }
                if (!Double.isFinite(linea.getFactorConversion()) || linea.getFactorConversion() <= 0) {
                    throw new IllegalArgumentException("El factor de conversion debe ser positivo");
                }
            }

            recepcionDAO.save(recepcion);

            for (DetalleRecepcionCompra detalle : detalles) {
                DetalleCompra linea = lineas.get(detalle.getDetalleCompra().getId());
                detalle.setRecepcionCompra(recepcion);
                detalleRecepcionDAO.save(detalle);

                linea.setCantidadRecibida(linea.getCantidadRecibida() + detalle.getCantidadRecibida());
                detalleCompraDAO.update(linea);

                // La cantidad recibida esta en unidad de compra; el stock se lleva en unidad de venta.
                double cantidadStock = detalle.getCantidadRecibida() * linea.getFactorConversion();
                if (!Double.isFinite(cantidadStock) || cantidadStock <= 0) {
                    throw new IllegalArgumentException("La cantidad convertida no es valida");
                }
                int idProducto = linea.getProducto().getId();
                double stockAnterior;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT stock_actual FROM producto WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, idProducto);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("No existe el producto con ID " + idProducto);
                        }
                        stockAnterior = rs.getDouble("stock_actual");
                    }
                }

                double nuevoStock = stockAnterior + cantidadStock;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE producto SET stock_actual = ? WHERE id = ?")) {
                    ps.setDouble(1, nuevoStock);
                    ps.setInt(2, idProducto);
                    ps.executeUpdate();
                }

                MovimientoInventario movimiento = new MovimientoInventario();
                Producto producto = new Producto();
                producto.setId(idProducto);
                movimiento.setProducto(producto);
                movimiento.setTipo(TipoMovimientoInventario.INGRESO_COMPRA);
                movimiento.setCantidad(cantidadStock);
                movimiento.setStockResultante(nuevoStock);
                movimiento.setRecepcionCompra(recepcion);
                movimiento.setUsuarioRegistro(recepcion.getUsuarioRegistro());
                movimiento.setMotivo("Ingreso por recepcion de compra");
                movimientoDAO.save(movimiento);
            }

            boolean completa = true;
            for (DetalleCompra linea : lineas.values()) {
                if (linea.getCantidadRecibida() + EPSILON < linea.getCantidad()) {
                    completa = false;
                    break;
                }
            }
            compraDAO.actualizarEstado(idCompra,
                    completa ? EstadoCompra.RECIBIDA : EstadoCompra.RECIBIDA_PARCIAL);

            transactionContext.commit();
            return recepcion;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public RecepcionCompra modificar(RecepcionCompra recepcion) throws SQLException {
        validarRecepcion(recepcion);
        validarId(recepcion.getId());
        try {
            RecepcionCompra existente = recepcionDAO.load(recepcion.getId());
            if (existente == null) {
                throw new IllegalArgumentException("No existe la recepcion con ID " + recepcion.getId());
            }
            if (existente.getCompra().getId() != recepcion.getCompra().getId()) {
                throw new IllegalArgumentException("No se puede cambiar la compra de una recepcion");
            }
            // Solo actualiza fecha y observaciones, nunca cantidades ni movimientos historicos.
            recepcionDAO.update(recepcion);
            transactionContext.commit();
            return recepcion;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public RecepcionCompra obtener(int idRecepcion) throws SQLException {
        validarId(idRecepcion);
        try {
            return recepcionDAO.load(idRecepcion);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<RecepcionCompra> listarTodos() throws SQLException {
        try {
            return recepcionDAO.listAll();
        } finally {
            transactionContext.close();
        }
    }

    private void validarRecepcion(RecepcionCompra recepcion) {
        Validador.obligatorio(recepcion, "recepcion");
        Validador.obligatorio(recepcion.getCompra(), "compra");
        Validador.obligatorio(recepcion.getUsuarioRegistro(), "usuario de registro");
        Validador.obligatorio(recepcion.getFechaRecepcion(), "fecha de recepcion");
        validarId(recepcion.getCompra().getId());
        validarId(recepcion.getUsuarioRegistro().getId());
        recepcion.setObservaciones(
                Validador.textoOpcional(recepcion.getObservaciones(), "observaciones", 255));
    }

    private void validarId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo");
        }
    }
}
