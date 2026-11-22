package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.RecepcionCompraDAO;
import pe.edu.pucp.sigmeta.dao.almacen.DetalleRecepcionCompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.CompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.DetalleCompraDAO;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import static pe.edu.pucp.sigmeta.boimpl.almacen.MovimientoInventarioBOImpl.EPSILON;

// RF011/RF012: revertir una recepcion de compra (stock, costo promedio y estado de la compra).
// Parte de MovimientoInventarioBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class ReversionRecepcion {

    private final MovimientoInventarioDAO movimientoDAO;
    private final StockInventarioDAO stockDAO;
    private final UsuarioBO usuarioBO;
    private final RecepcionCompraDAO recepcionDAO;
    private final DetalleRecepcionCompraDAO detalleRecepcionDAO;
    private final CompraDAO compraDAO;
    private final DetalleCompraDAO detalleCompraDAO;

    ReversionRecepcion(MovimientoInventarioDAO movimientoDAO, StockInventarioDAO stockDAO, UsuarioBO usuarioBO, RecepcionCompraDAO recepcionDAO, DetalleRecepcionCompraDAO detalleRecepcionDAO, CompraDAO compraDAO, DetalleCompraDAO detalleCompraDAO) {
        this.movimientoDAO = movimientoDAO;
        this.stockDAO = stockDAO;
        this.usuarioBO = usuarioBO;
        this.recepcionDAO = recepcionDAO;
        this.detalleRecepcionDAO = detalleRecepcionDAO;
        this.compraDAO = compraDAO;
        this.detalleCompraDAO = detalleCompraDAO;
    }

    public List<MovimientoInventario> revertirRecepcionCompra(int idRecepcion, int idAlmacenero,
                                                               String motivo) throws SQLException {
        MovimientoInventarioBOImpl.validarId(idRecepcion);
        MovimientoInventarioBOImpl.validarId(idAlmacenero);
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
                MovimientoInventario inverso = MovimientoInventarioBOImpl.nuevaCompensacion(m, idAlmacenero, razon);
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
}
