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
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;

public class MovimientoInventarioBOImpl implements MovimientoInventarioBO {

    private static final double EPSILON = 0.000001;
    public static final String OPERACION_AJUSTE_INVENTARIO = SolicitudAutorizacionBOImpl.OPERACION_AJUSTE_INVENTARIO;
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
    public MovimientoInventario revertirAjuste(int idMovimientoAjuste, int idAlmacenero,
                                                 String motivo) throws SQLException {
        validarId(idMovimientoAjuste);
        validarId(idAlmacenero);
        String razon = Validador.textoObligatorio(motivo, "motivo de reversión", 255);
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);
        return Transaccion.ejecutar(() -> {
            // RF014: la autorizacion aprobada de ESTA reversion (mismo movimiento); un solo uso, se consume al final
            pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion autorizacion =
                    autorizacionBO.requerirAutorizacion(idAlmacenero, OPERACION_AJUSTE_INVENTARIO,
                            huellaReversionAjuste(idMovimientoAjuste));
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
                throw new IllegalStateException("Cantidad histórica inválida");
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
            autorizacionBO.consumir(autorizacion.getId());
            return reversion;
        });
    }

    @Override
    public List<MovimientoInventario> revertirRecepcionCompra(int idRecepcion, int idAlmacenero,
                                                               String motivo) throws SQLException {
        validarId(idRecepcion);
        validarId(idAlmacenero);
        String razon = Validador.textoObligatorio(motivo, "motivo de reversión", 255);
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);
        return Transaccion.ejecutar(() -> {
            RecepcionCompra recepcion = recepcionDAO.load(idRecepcion);
            if (recepcion == null) throw new IllegalArgumentException("No existe la recepción");
            int idCompra = recepcion.getCompra().getId();
            compraDAO.bloquearCompra(idCompra);
            recepcion = recepcionDAO.load(idRecepcion);
            if (recepcion.isAnulada()) throw new IllegalStateException("La recepción ya fue anulada");
            Compra compra = compraDAO.load(idCompra);
            if (compra.isAnulado()) throw new IllegalStateException("No se revierte una compra anulada");

            List<DetalleRecepcionCompra> detalles = detalleRecepcionDAO.listarPorRecepcion(idRecepcion);
            List<MovimientoInventario> originales = new ArrayList<>();
            for (MovimientoInventario m : movimientoDAO.listarPorRecepcion(idRecepcion)) {
                if (m.getTipo() == TipoMovimientoInventario.INGRESO_COMPRA) originales.add(m);
            }
            if (detalles.isEmpty() || originales.isEmpty()) {
                throw new IllegalStateException("La recepción no tiene detalles o ingresos originales");
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
                    throw new IllegalStateException("La recepción tiene ingresos ya revertidos");
                }
                cantidadesRegistradas.merge(m.getProducto().getId(), m.getCantidad(), Double::sum);
            }
            if (cantidadesEsperadas.size() != cantidadesRegistradas.size()) {
                throw new IllegalStateException("Los ingresos no coinciden con los detalles recibidos");
            }
            for (Map.Entry<Integer, Double> item : cantidadesEsperadas.entrySet()) {
                if (Math.abs(item.getValue() - cantidadesRegistradas.getOrDefault(item.getKey(), -1.0)) > 0.0001) {
                    throw new IllegalStateException("El inventario no coincide con los detalles históricos");
                }
            }

            // Revertir del movimiento mas reciente al mas antiguo de cada producto:
            // los costos anteriores/posteriores se encadenan con el redondeo original.
            originales.sort(Comparator.comparingInt((MovimientoInventario m) -> m.getProducto().getId())
                    .thenComparing(Comparator.comparingInt(MovimientoInventario::getId).reversed()));
            pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl consultas =
                    new pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl();
            java.util.Set<Integer> productosRevisados = new java.util.HashSet<>();
            List<MovimientoInventario> compensaciones = new ArrayList<>();
            for (MovimientoInventario m : originales) {
                MovimientoInventario bloqueado = movimientoDAO.bloquearMovimiento(m.getId());
                if (bloqueado == null) throw new IllegalStateException("No existe el ingreso original");
                int idProducto = m.getProducto().getId();
                if (productosRevisados.add(idProducto)) {
                    // Ningun despacho, ajuste, otra recepcion ni compensacion posterior:
                    // no se puede deshacer un promedio historico sobre otro movimiento.
                    int ultimoId = movimientoDAO.listarPorProducto(idProducto).stream()
                            .mapToInt(MovimientoInventario::getId).max().orElse(0);
                    if (ultimoId != m.getId()) {
                        throw new IllegalStateException("Existen movimientos posteriores del producto " + idProducto
                                + "; concilie el historial antes de revertir esta recepcion");
                    }
                }
                List<Map<String, Object>> historico = consultas.consultar("sp_costo_recepcion_obtener", m.getId());
                if (historico.size() != 1 || !(historico.get(0).get("costoAnterior") instanceof Number)
                        || !(historico.get(0).get("costoPosterior") instanceof Number)) {
                    throw new IllegalStateException("La recepcion no tiene costos historicos verificables; "
                            + "no es seguro revertirla automaticamente");
                }
                java.math.BigDecimal costoAnterior = new java.math.BigDecimal(
                        historico.get(0).get("costoAnterior").toString());
                java.math.BigDecimal costoPosterior = new java.math.BigDecimal(
                        historico.get(0).get("costoPosterior").toString());
                Producto productoActual = new pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl()
                        .load(idProducto);
                if (productoActual == null) throw new IllegalStateException("Producto no encontrado");
                CostoPromedioRecepcion.validarCostoActual(
                        java.math.BigDecimal.valueOf(productoActual.getCostoUnitario()), costoPosterior);
                double stock = stockDAO.obtenerStockParaActualizar(idProducto);
                // Si no coincide con el cierre del ingreso, hubo una alteracion de stock
                // que no se puede reconstruir con certeza: se aborta todo el rollback.
                if (!Double.isFinite(stock) || Math.abs(stock - m.getStockResultante()) > 0.0001) {
                    throw new IllegalStateException("El stock no coincide con el historial de la recepcion");
                }
                double nuevoStock = stock - m.getCantidad();
                if (!Double.isFinite(nuevoStock) || nuevoStock < -EPSILON) {
                    throw new IllegalStateException("Stock insuficiente para revertir la recepción");
                }
                if (nuevoStock < 0) nuevoStock = 0;
                MovimientoInventario inverso = nuevaCompensacion(m, idAlmacenero, razon);
                inverso.setTipo(TipoMovimientoInventario.SALIDA_REVERSION_COMPRA);
                inverso.setRecepcionCompra(recepcion);
                inverso.setStockResultante(nuevoStock);
                stockDAO.actualizarStock(idProducto, nuevoStock);
                consultas.ejecutar("sp_producto_actualizar_costo", idProducto, costoAnterior);
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
            return compensaciones;
        });
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
            throw new IllegalArgumentException("La cantidad contada debe ser válida y no negativa");
        }
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo", 255);
        usuarioBO.verificarRol(idUsuario, TipoRol.ALMACENERO);
        return Transaccion.ejecutar(() -> {
            // RF014: la autorizacion aprobada de ESTE ajuste (mismo producto y misma cantidad contada); un solo uso
            pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion autorizacion =
                    autorizacionBO.requerirAutorizacion(idUsuario, OPERACION_AJUSTE_INVENTARIO,
                            huellaAjuste(idProducto, cantidadContada));
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
            autorizacionBO.consumir(autorizacion.getId());
            return movimiento;
        });
    }

    // ================= RF014: el ajuste pide la autorizacion con sus datos =================

    private static String huellaAjuste(int idProducto, double cantidadContada) {
        return pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.huella(
                "AJUSTE|" + idProducto + "|" + String.format(java.util.Locale.ROOT, "%.3f", cantidadContada));
    }

    private static String huellaReversionAjuste(int idMovimientoAjuste) {
        return pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.huella("REVERTIR_AJUSTE|" + idMovimientoAjuste);
    }

    private static String cantidad(double valor) {
        return java.math.BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString();
    }

    // RF012/RF014: el Almacenero cuenta el producto y pide la autorizacion de ESE ajuste. No mueve el stock:
    // el Administrador ve el producto, el stock del sistema, lo contado y la diferencia; al aprobar se registra.
    @Override
    public pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarAjuste(int idProducto, int idUsuario,
            double cantidadContada, String motivo) throws SQLException {
        validarId(idProducto);
        validarId(idUsuario);
        if (!Double.isFinite(cantidadContada) || cantidadContada < 0) {
            throw new IllegalArgumentException("La cantidad contada debe ser válida y no negativa");
        }
        String motivoValidado = Validador.textoObligatorio(motivo, "motivo", 200);
        usuarioBO.verificarRol(idUsuario, TipoRol.ALMACENERO);
        return Transaccion.ejecutar(() -> {
            Producto producto = new pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl().load(idProducto);
            if (producto == null) {
                throw new IllegalArgumentException("No existe el producto con id " + idProducto);
            }
            double diferencia = cantidadContada - producto.getStockActual();
            if (Math.abs(diferencia) < EPSILON) {
                throw new IllegalArgumentException("El stock contado coincide con el actual; no hay ajuste");
            }
            String detalle = "Producto: " + producto.getNombre()
                    + "\nStock en el sistema: " + cantidad(producto.getStockActual())
                    + "\nContado: " + cantidad(cantidadContada)
                    + "\nDiferencia: " + (diferencia > 0 ? "+" : "−") + cantidad(Math.abs(diferencia))
                    + (diferencia > 0 ? " (ingreso)" : " (salida)");
            return autorizacionBO.solicitarOperacion(idUsuario, OPERACION_AJUSTE_INVENTARIO, motivoValidado,
                    producto.getCodigoInterno() + " · " + producto.getNombre(), detalle,
                    huellaAjuste(idProducto, cantidadContada));
        });
    }

    // RF012/RF014: pedir la autorizacion para revertir UN ajuste concreto
    @Override
    public pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarReversionAjuste(int idMovimientoAjuste,
            int idAlmacenero, String motivo) throws SQLException {
        validarId(idMovimientoAjuste);
        validarId(idAlmacenero);
        String razon = Validador.textoObligatorio(motivo, "motivo de reversión", 200);
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);
        return Transaccion.ejecutar(() -> {
            MovimientoInventario original = movimientoDAO.bloquearMovimiento(idMovimientoAjuste);
            if (original == null || (original.getTipo() != TipoMovimientoInventario.AJUSTE_INGRESO
                    && original.getTipo() != TipoMovimientoInventario.AJUSTE_SALIDA)) {
                throw new IllegalArgumentException("El movimiento no es un ajuste original");
            }
            if (movimientoDAO.existeReversion(idMovimientoAjuste)) {
                throw new IllegalStateException("El ajuste ya fue revertido");
            }
            Producto producto = new pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl().load(original.getProducto().getId());
            String nombre = producto == null ? "producto" : producto.getNombre();
            boolean ingreso = original.getTipo() == TipoMovimientoInventario.AJUSTE_INGRESO;
            String detalle = "Revertir el ajuste #" + idMovimientoAjuste + " de " + nombre
                    + "\nEl ajuste fue " + (ingreso ? "un ingreso" : "una salida") + " de " + cantidad(original.getCantidad())
                    + "\nAl revertirlo, el stock " + (ingreso ? "baja" : "sube") + " en " + cantidad(original.getCantidad());
            return autorizacionBO.solicitarOperacion(idAlmacenero, OPERACION_AJUSTE_INVENTARIO, razon,
                    "Reversión del ajuste #" + idMovimientoAjuste + " · " + nombre, detalle,
                    huellaReversionAjuste(idMovimientoAjuste));
        });
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

    private void validarId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo");
        }
    }
}
