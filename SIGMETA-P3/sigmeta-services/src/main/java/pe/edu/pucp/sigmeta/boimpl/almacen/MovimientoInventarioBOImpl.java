package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.bo.almacen.MovimientoInventarioBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.daoimpl.almacen.MovimientoInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.StockInventarioDAOImpl;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;
import java.util.List;

public class MovimientoInventarioBOImpl implements MovimientoInventarioBO {

    private static final double EPSILON = 0.000001;
    public static final String OPERACION_AJUSTE_INVENTARIO = "AJUSTE_INVENTARIO";
    private final MovimientoInventarioDAO movimientoDAO;
    private final StockInventarioDAO stockDAO;
    private final UsuarioBO usuarioBO;
    private final SolicitudAutorizacionBO autorizacionBO;

    public MovimientoInventarioBOImpl() {
        this.movimientoDAO = new MovimientoInventarioDAOImpl();
        this.stockDAO = new StockInventarioDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
        this.autorizacionBO = new SolicitudAutorizacionBOImpl();
    }

    /**
     * Operacion compuesta: DespachoBO es responsable del commit/rollback/close.
     * El bloqueo de la salida y la UNIQUE en id_movimiento_revertido impiden
     * acreditar el mismo movimiento dos veces, incluso con llamadas concurrentes.
     */
    @Override
    public MovimientoInventario revertirSalidaDespacho(int idMovimientoSalida, int idDespacho,
                                                        int idAlmacenero, String motivo) throws SQLException {
        validarId(idMovimientoSalida);
        validarId(idDespacho);
        validarId(idAlmacenero);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo de reversion", 255);
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);

        MovimientoInventario original = movimientoDAO.bloquearMovimiento(idMovimientoSalida);
        if (original == null || original.getTipo() != TipoMovimientoInventario.SALIDA_DESPACHO
                || original.getDespacho() == null || original.getDespacho().getId() != idDespacho) {
            throw new IllegalArgumentException("La salida indicada no corresponde al despacho");
        }
        if (movimientoDAO.existeReversion(idMovimientoSalida)) {
            throw new IllegalStateException("Esta salida de despacho ya fue revertida");
        }
        if (!Double.isFinite(original.getCantidad()) || original.getCantidad() <= 0) {
            throw new IllegalStateException("La cantidad historica del movimiento no es valida");
        }

        int idProducto = original.getProducto().getId();
        double stockActual = stockDAO.obtenerStockParaActualizar(idProducto);
        double nuevoStock = stockActual + original.getCantidad();
        if (!Double.isFinite(nuevoStock)) {
            throw new IllegalStateException("El stock resultante no es valido");
        }

        MovimientoInventario reversion = new MovimientoInventario();
        reversion.setProducto(original.getProducto());
        reversion.setTipo(TipoMovimientoInventario.INGRESO_REVERSION_DESPACHO);
        reversion.setCantidad(original.getCantidad());
        reversion.setStockResultante(nuevoStock);
        reversion.setDespacho(new Despacho());
        reversion.getDespacho().setId(idDespacho);
        Usuario responsable = new Usuario();
        responsable.setId(idAlmacenero);
        reversion.setUsuarioRegistro(responsable);
        reversion.setMotivo(motivoValidado);
        reversion.setIdMovimientoRevertido(idMovimientoSalida);

        stockDAO.actualizarStock(idProducto, nuevoStock);
        return movimientoDAO.registrarReversionDespacho(reversion);
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
        usuarioBO.verificarRol(idUsuario, TipoRol.ALMACENERO);
        if (!autorizacionBO.tieneAutorizacionVigente(idUsuario, OPERACION_AJUSTE_INVENTARIO)) {
            throw new IllegalStateException("El ajuste requiere autorizacion vigente del Administrador");
        }

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
    public MovimientoInventario modificarMotivo(int idMovimiento, String motivo, int idAdministrador) throws SQLException {
        validarId(idMovimiento);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo", 255);
        usuarioBO.verificarRol(idAdministrador, TipoRol.ADMINISTRADOR);
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
            return movimientoDAO.listarPorProducto(idProducto);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<Producto> listarEnStockMinimo() throws SQLException {
        try {
            return stockDAO.listarEnStockMinimo();
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
