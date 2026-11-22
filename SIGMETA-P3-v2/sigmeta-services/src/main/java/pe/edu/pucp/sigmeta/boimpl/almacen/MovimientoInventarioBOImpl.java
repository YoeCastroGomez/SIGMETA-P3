package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.almacen.MovimientoInventarioBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.RecepcionCompraDAO;
import pe.edu.pucp.sigmeta.dao.almacen.DetalleRecepcionCompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.CompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.DetalleCompraDAO;
import pe.edu.pucp.sigmeta.daoimpl.almacen.MovimientoInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.StockInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.RecepcionCompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.DetalleRecepcionCompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.compras.CompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.compras.DetalleCompraDAOImpl;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.SQLException;
import java.util.List;

public class MovimientoInventarioBOImpl implements MovimientoInventarioBO {

    static final double EPSILON = 0.000001;
    public static final String OPERACION_AJUSTE_INVENTARIO = SolicitudAutorizacionBOImpl.OPERACION_AJUSTE_INVENTARIO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final StockInventarioDAO stockDAO;
    private final UsuarioBO usuarioBO;
    private final SolicitudAutorizacionBO autorizacionBO;
    private final RecepcionCompraDAO recepcionDAO;
    private final DetalleRecepcionCompraDAO detalleRecepcionDAO;
    private final CompraDAO compraDAO;
    private final DetalleCompraDAO detalleCompraDAO;
    // partes de esta clase (mismo paquete)
    private final ReversionRecepcion reversionRecepcion;
    private final AjusteInventario ajusteInventario;

    public MovimientoInventarioBOImpl() {
        this.movimientoDAO = new MovimientoInventarioDAOImpl();
        this.stockDAO = new StockInventarioDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
        this.autorizacionBO = new SolicitudAutorizacionBOImpl();
        this.recepcionDAO = new RecepcionCompraDAOImpl();
        this.detalleRecepcionDAO = new DetalleRecepcionCompraDAOImpl();
        this.compraDAO = new CompraDAOImpl();
        this.detalleCompraDAO = new DetalleCompraDAOImpl();
        this.reversionRecepcion = new ReversionRecepcion(movimientoDAO, stockDAO, usuarioBO, recepcionDAO, detalleRecepcionDAO, compraDAO, detalleCompraDAO);
        this.ajusteInventario = new AjusteInventario(movimientoDAO, stockDAO, usuarioBO, autorizacionBO);
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
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo de reversión", 255);
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO, TipoRol.ADMINISTRADOR);

        MovimientoInventario original = movimientoDAO.bloquearMovimiento(idMovimientoSalida);
        if (original == null || original.getTipo() != TipoMovimientoInventario.SALIDA_DESPACHO
                || original.getDespacho() == null || original.getDespacho().getId() != idDespacho) {
            throw new IllegalArgumentException("La salida indicada no corresponde al despacho");
        }
        if (movimientoDAO.existeReversion(idMovimientoSalida)) {
            throw new IllegalStateException("Esta salida de despacho ya fue revertida");
        }
        if (!Double.isFinite(original.getCantidad()) || original.getCantidad() <= 0) {
            throw new IllegalStateException("La cantidad histórica del movimiento no es válida");
        }

        int idProducto = original.getProducto().getId();
        double stockActual = stockDAO.obtenerStockParaActualizar(idProducto);
        double nuevoStock = stockActual + original.getCantidad();
        if (!Double.isFinite(nuevoStock)) {
            throw new IllegalStateException("El stock resultante no es válido");
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
    public MovimientoInventario revertirAjuste(int idMovimientoAjuste, int idAlmacenero, String motivo) throws SQLException {
        return ajusteInventario.revertirAjuste(idMovimientoAjuste, idAlmacenero, motivo);
    }

    @Override
    public List<MovimientoInventario> revertirRecepcionCompra(int idRecepcion, int idAlmacenero, String motivo) throws SQLException {
        return reversionRecepcion.revertirRecepcionCompra(idRecepcion, idAlmacenero, motivo);
    }

    static MovimientoInventario nuevaCompensacion(MovimientoInventario original, int idUsuario, String motivo) {
        MovimientoInventario reversion = new MovimientoInventario();
        reversion.setProducto(original.getProducto());
        reversion.setCantidad(original.getCantidad());
        reversion.setIdMovimientoRevertido(original.getId());
        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        reversion.setUsuarioRegistro(usuario);
        reversion.setMotivo(motivo);
        return reversion;
    }

    @Override
    public MovimientoInventario registrarAjuste(int idProducto, int idUsuario, double cantidadContada, String motivo) throws SQLException {
        return ajusteInventario.registrarAjuste(idProducto, idUsuario, cantidadContada, motivo);
    }

    @Override
    public pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarAjuste(int idProducto, int idUsuario, double cantidadContada, String motivo) throws SQLException {
        return ajusteInventario.solicitarAjuste(idProducto, idUsuario, cantidadContada, motivo);
    }

    @Override
    public pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarReversionAjuste(int idMovimientoAjuste, int idAlmacenero, String motivo) throws SQLException {
        return ajusteInventario.solicitarReversionAjuste(idMovimientoAjuste, idAlmacenero, motivo);
    }

    @Override
    public MovimientoInventario modificarMotivo(int idMovimiento, String motivo, int idAdministrador) throws SQLException {
        validarId(idMovimiento);
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo", 255);
        usuarioBO.verificarRol(idAdministrador, TipoRol.ADMINISTRADOR);
        return Transaccion.ejecutar(() -> {
            MovimientoInventario existente = movimientoDAO.load(idMovimiento);
            if (existente == null) {
                throw new IllegalArgumentException("No existe el movimiento con ID " + idMovimiento);
            }
            existente.setMotivo(motivoValidado);
            movimientoDAO.update(existente);
            return existente;
        });
    }

    @Override
    public MovimientoInventario obtener(int idMovimiento) throws SQLException {
        validarId(idMovimiento);
        return Transaccion.ejecutar(() -> {
            return movimientoDAO.load(idMovimiento);
        });
    }

    @Override
    public List<MovimientoInventario> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return movimientoDAO.listAll();
        });
    }

    @Override
    public List<MovimientoInventario> listarPorProducto(int idProducto) throws SQLException {
        validarId(idProducto);
        return Transaccion.ejecutar(() -> {
            return movimientoDAO.listarPorProducto(idProducto);
        });
    }

    @Override
    public List<Producto> listarEnStockMinimo() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return stockDAO.listarEnStockMinimo();
        });
    }

    static void validarId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo");
        }
    }
}
