package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.bo.almacen.MovimientoInventarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.daoimpl.almacen.MovimientoInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.StockInventarioDAOImpl;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioBOImpl implements MovimientoInventarioBO {

    private static final double EPSILON = 0.000001;
    private final MovimientoInventarioDAO movimientoDAO;
    private final StockInventarioDAO stockDAO;

    public MovimientoInventarioBOImpl() {
        this.movimientoDAO = new MovimientoInventarioDAOImpl();
        this.stockDAO = new StockInventarioDAOImpl();
    }

    @Override
    public MovimientoInventario registrarAjuste(int idProducto, int idUsuario,
                                                  double cantidadContada, String motivo) throws SQLException {
        validarId(idProducto);
        validarId(idUsuario);
        if (!Double.isFinite(cantidadContada) || cantidadContada < 0) {
            throw new IllegalArgumentException("La cantidad contada debe ser valida y no negativa");
        }
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo", 255);

        try {
            double stockActual = stockDAO.obtenerStockParaActualizar(idProducto);

            double diferencia = cantidadContada - stockActual;
            if (Math.abs(diferencia) < EPSILON) {
                throw new IllegalArgumentException("El stock contado coincide con el actual; no hay ajuste");
            }

            MovimientoInventario movimiento = new MovimientoInventario();
            Producto producto = new Producto();
            producto.setId(idProducto);
            movimiento.setProducto(producto);
            Usuario usuario = new Usuario();
            usuario.setId(idUsuario);
            movimiento.setUsuarioRegistro(usuario);
            movimiento.setTipo(diferencia > 0
                    ? TipoMovimientoInventario.AJUSTE_INGRESO
                    : TipoMovimientoInventario.AJUSTE_SALIDA);
            movimiento.setCantidad(Math.abs(diferencia));
            movimiento.setStockResultante(cantidadContada);
            movimiento.setCantidadContada(cantidadContada);
            movimiento.setMotivo(motivoValidado);

            stockDAO.actualizarStock(idProducto, cantidadContada);
            movimientoDAO.save(movimiento);
            transactionContext.commit();
            return movimiento;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public MovimientoInventario modificarMotivo(int idMovimiento, String motivo) throws SQLException {
        validarId(idMovimiento);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo", 255);
        try {
            MovimientoInventario existente = movimientoDAO.load(idMovimiento);
            if (existente == null) {
                throw new IllegalArgumentException("No existe el movimiento con ID " + idMovimiento);
            }
            existente.setMotivo(motivoValidado);
            movimientoDAO.update(existente);
            transactionContext.commit();
            return existente;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public MovimientoInventario obtener(int idMovimiento) throws SQLException {
        validarId(idMovimiento);
        try {
            return movimientoDAO.load(idMovimiento);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<MovimientoInventario> listarTodos() throws SQLException {
        try {
            return movimientoDAO.listAll();
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<MovimientoInventario> listarPorProducto(int idProducto) throws SQLException {
        validarId(idProducto);
        try {
            List<MovimientoInventario> encontrados = new ArrayList<>();
            for (MovimientoInventario movimiento : movimientoDAO.listAll()) {
                if (movimiento.getProducto() != null
                        && movimiento.getProducto().getId() == idProducto) {
                    encontrados.add(movimiento);
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
