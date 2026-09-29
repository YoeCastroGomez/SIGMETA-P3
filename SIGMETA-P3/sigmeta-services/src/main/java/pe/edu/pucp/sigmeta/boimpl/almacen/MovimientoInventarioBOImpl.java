package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.bo.almacen.MovimientoInventarioBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
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
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;

public class MovimientoInventarioBOImpl implements MovimientoInventarioBO {

    private static final double EPSILON = 0.000001;
    public static final String OPERACION_AJUSTE_INVENTARIO = "AJUSTE_INVENTARIO";
    private final MovimientoInventarioDAO movimientoDAO;
    private final StockInventarioDAO stockDAO;
    private final UsuarioBO usuarioBO;
    private final SolicitudAutorizacionBO autorizacionBO;
    private final RecepcionCompraDAO recepcionDAO;
    private final DetalleRecepcionCompraDAO detalleRecepcionDAO;
    private final CompraDAO compraDAO;
    private final DetalleCompraDAO detalleCompraDAO;

    public MovimientoInventarioBOImpl() {
        this.movimientoDAO = new MovimientoInventarioDAOImpl();
        this.stockDAO = new StockInventarioDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
        this.autorizacionBO = new SolicitudAutorizacionBOImpl();
        this.recepcionDAO = new RecepcionCompraDAOImpl();
        this.detalleRecepcionDAO = new DetalleRecepcionCompraDAOImpl();
        this.compraDAO = new CompraDAOImpl();
        this.detalleCompraDAO = new DetalleCompraDAOImpl();
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
    public MovimientoInventario revertirAjuste(int idMovimientoAjuste, int idAlmacenero,
                                                 String motivo) throws SQLException {
        validarId(idMovimientoAjuste);
        validarId(idAlmacenero);
        String razon = Validador.textoObligatorio(motivo, "motivo de reversion", 255);
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);
        if (!autorizacionBO.tieneAutorizacionVigente(idAlmacenero, OPERACION_AJUSTE_INVENTARIO)) {
            throw new IllegalStateException("La reversion del ajuste requiere autorizacion vigente");
        }
        try {
            MovimientoInventario original = movimientoDAO.bloquearMovimiento(idMovimientoAjuste);
            if (original == null || (original.getTipo() != TipoMovimientoInventario.AJUSTE_INGRESO
                    && original.getTipo() != TipoMovimientoInventario.AJUSTE_SALIDA)) {
                throw new IllegalArgumentException("El movimiento no es un ajuste original");
            }
            if (movimientoDAO.existeReversion(idMovimientoAjuste)) {
                throw new IllegalStateException("El ajuste ya fue revertido");
            }
            double cantidad = original.getCantidad();
            if (!Double.isFinite(cantidad) || cantidad <= 0) {
                throw new IllegalStateException("Cantidad historica invalida");
            }
            boolean ingresoOriginal = original.getTipo() == TipoMovimientoInventario.AJUSTE_INGRESO;
            int idProducto = original.getProducto().getId();
            double stock = stockDAO.obtenerStockParaActualizar(idProducto);
            double nuevoStock = ingresoOriginal ? stock - cantidad : stock + cantidad;
            if (!Double.isFinite(nuevoStock) || nuevoStock < -EPSILON) {
                throw new IllegalStateException("No hay stock suficiente para revertir el ajuste");
            }
            if (nuevoStock < 0) nuevoStock = 0;
            MovimientoInventario reversion = nuevaCompensacion(original, idAlmacenero, razon);
            reversion.setTipo(ingresoOriginal ? TipoMovimientoInventario.REVERSION_AJUSTE_INGRESO
                    : TipoMovimientoInventario.REVERSION_AJUSTE_SALIDA);
            reversion.setStockResultante(nuevoStock);
            stockDAO.actualizarStock(idProducto, nuevoStock);
            movimientoDAO.registrarCompensacion(reversion);
            transactionContext.commit();
            return reversion;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<MovimientoInventario> revertirRecepcionCompra(int idRecepcion, int idAlmacenero,
                                                               String motivo) throws SQLException {
        validarId(idRecepcion);
        validarId(idAlmacenero);
        String razon = Validador.textoObligatorio(motivo, "motivo de reversion", 255);
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);
        try {
            RecepcionCompra recepcion = recepcionDAO.load(idRecepcion);
            if (recepcion == null) throw new IllegalArgumentException("No existe la recepcion");
            int idCompra = recepcion.getCompra().getId();
            compraDAO.bloquearCompra(idCompra);
            recepcion = recepcionDAO.load(idRecepcion);
            if (recepcion.isAnulada()) throw new IllegalStateException("La recepcion ya fue anulada");
            Compra compra = compraDAO.load(idCompra);
            if (compra.isAnulado()) throw new IllegalStateException("No se revierte una compra anulada");

            List<DetalleRecepcionCompra> detalles = detalleRecepcionDAO.listarPorRecepcion(idRecepcion);
            List<MovimientoInventario> originales = new ArrayList<>();
            for (MovimientoInventario m : movimientoDAO.listarPorRecepcion(idRecepcion)) {
                if (m.getTipo() == TipoMovimientoInventario.INGRESO_COMPRA) originales.add(m);
            }
            if (detalles.isEmpty() || originales.isEmpty()) {
                throw new IllegalStateException("La recepcion no tiene detalles o ingresos originales");
            }
            Map<Integer, Double> cantidadesEsperadas = new HashMap<>();
            List<DetalleCompra> lineasCompra = detalleCompraDAO.listAll();
            Map<Integer, DetalleCompra> lineas = new HashMap<>();
            for (DetalleCompra linea : lineasCompra) {
                if (linea.getCompra().getId() == idCompra) lineas.put(linea.getId(), linea);
            }
            for (DetalleRecepcionCompra d : detalles) {
                DetalleCompra linea = lineas.get(d.getDetalleCompra().getId());
                if (linea == null || linea.getCantidadRecibida() + EPSILON < d.getCantidadRecibida()) {
                    throw new IllegalStateException("Las cantidades recibidas no son consistentes");
                }
                double unidades = d.getCantidadRecibida() * linea.getFactorConversion();
                cantidadesEsperadas.merge(linea.getProducto().getId(), unidades, Double::sum);
            }
            Map<Integer, Double> cantidadesRegistradas = new HashMap<>();
            for (MovimientoInventario m : originales) {
                if (movimientoDAO.existeReversion(m.getId())) {
                    throw new IllegalStateException("La recepcion tiene ingresos ya revertidos");
                }
                cantidadesRegistradas.merge(m.getProducto().getId(), m.getCantidad(), Double::sum);
            }
            if (cantidadesEsperadas.size() != cantidadesRegistradas.size()) {
                throw new IllegalStateException("Los ingresos no coinciden con los detalles recibidos");
            }
            for (Map.Entry<Integer, Double> item : cantidadesEsperadas.entrySet()) {
                if (Math.abs(item.getValue() - cantidadesRegistradas.getOrDefault(item.getKey(), -1.0)) > 0.0001) {
                    throw new IllegalStateException("El inventario no coincide con los detalles historicos");
                }
            }

            originales.sort(Comparator.comparingInt((MovimientoInventario m) -> m.getProducto().getId())
                    .thenComparingInt(MovimientoInventario::getId));
            List<MovimientoInventario> compensaciones = new ArrayList<>();
            for (MovimientoInventario m : originales) {
                movimientoDAO.bloquearMovimiento(m.getId());
                int idProducto = m.getProducto().getId();
                double stock = stockDAO.obtenerStockParaActualizar(idProducto);
                double nuevoStock = stock - m.getCantidad();
                if (!Double.isFinite(nuevoStock) || nuevoStock < -EPSILON) {
                    throw new IllegalStateException("Stock insuficiente para revertir la recepcion");
                }
                if (nuevoStock < 0) nuevoStock = 0;
                MovimientoInventario inverso = nuevaCompensacion(m, idAlmacenero, razon);
                inverso.setTipo(TipoMovimientoInventario.SALIDA_REVERSION_COMPRA);
                inverso.setRecepcionCompra(recepcion);
                inverso.setStockResultante(nuevoStock);
                stockDAO.actualizarStock(idProducto, nuevoStock);
                compensaciones.add(movimientoDAO.registrarCompensacion(inverso));
            }
            for (DetalleRecepcionCompra d : detalles) {
                DetalleCompra linea = lineas.get(d.getDetalleCompra().getId());
                double restante = linea.getCantidadRecibida() - d.getCantidadRecibida();
                linea.setCantidadRecibida(Math.max(0, restante));
                detalleCompraDAO.descontarCantidadRecibida(linea.getId(), d.getCantidadRecibida());
            }
            boolean sinRecibir = true;
            boolean completo = true;
            for (DetalleCompra linea : lineas.values()) {
                if (linea.getCantidadRecibida() > EPSILON) sinRecibir = false;
                if (linea.getCantidadRecibida() + EPSILON < linea.getCantidad()) completo = false;
            }
            compraDAO.actualizarEstadoPorReversion(idCompra, sinRecibir ? EstadoCompra.REGISTRADA
                    : completo ? EstadoCompra.RECIBIDA : EstadoCompra.RECIBIDA_PARCIAL);
            recepcionDAO.marcarAnulada(idRecepcion);
            transactionContext.commit();
            return compensaciones;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    private MovimientoInventario nuevaCompensacion(MovimientoInventario original, int idUsuario, String motivo) {
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
