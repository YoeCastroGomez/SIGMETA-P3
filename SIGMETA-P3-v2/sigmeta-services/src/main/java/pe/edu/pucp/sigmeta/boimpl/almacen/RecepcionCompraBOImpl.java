package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.almacen.RecepcionCompraBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.almacen.DetalleRecepcionCompraDAO;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.RecepcionCompraDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.dao.compras.CompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.DetalleCompraDAO;
import pe.edu.pucp.sigmeta.daoimpl.almacen.DetalleRecepcionCompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.MovimientoInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.RecepcionCompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.StockInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.compras.CompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.compras.DetalleCompraDAOImpl;
import pe.edu.pucp.sigmeta.model.almacen.DetalleRecepcionCompra;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.producto.Producto;

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
    private final StockInventarioDAO stockDAO;
    private final UsuarioBO usuarioBO;

    public RecepcionCompraBOImpl() {
        this.recepcionDAO = new RecepcionCompraDAOImpl();
        this.detalleRecepcionDAO = new DetalleRecepcionCompraDAOImpl();
        this.detalleCompraDAO = new DetalleCompraDAOImpl();
        this.movimientoDAO = new MovimientoInventarioDAOImpl();
        this.compraDAO = new CompraDAOImpl();
        this.stockDAO = new StockInventarioDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // Metodo de costeo: PROMEDIO PONDERADO. Cada ingreso por compra recalcula el costo unitario del
    // producto (en unidad de venta, sin IGV):
    //   costo nuevo = (stock anterior x costo anterior + cantidad que ingresa x costo de la compra)
    //                 / (stock anterior + cantidad que ingresa)
    // El costo de la compra es el importe neto de la linea (ya con su descuento) entre lo comprado,
    // llevado a unidad de venta con el factor de conversion. Si no habia stock, queda el costo de la compra.
    // Revertir una recepcion devuelve las unidades pero no recalcula el promedio (criterio conservador).
    private void actualizarCostoPromedio(int idProducto, double stockAnterior, double cantidadStock,
                                         DetalleCompra linea) throws SQLException {
        if (linea.getCantidad() <= 0 || linea.getFactorConversion() <= 0) {
            return;
        }
        Producto producto = new pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl().load(idProducto);
        if (producto == null) {
            return;
        }
        java.math.BigDecimal costoCompra = java.math.BigDecimal.valueOf(linea.getImporte())
                .divide(java.math.BigDecimal.valueOf(linea.getCantidad() * linea.getFactorConversion()),
                        6, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal anterior = java.math.BigDecimal.valueOf(Math.max(stockAnterior, 0));
        java.math.BigDecimal ingreso = java.math.BigDecimal.valueOf(cantidadStock);
        java.math.BigDecimal costoNuevo = anterior.multiply(java.math.BigDecimal.valueOf(producto.getCostoUnitario()))
                .add(ingreso.multiply(costoCompra))
                .divide(anterior.add(ingreso), 2, java.math.RoundingMode.HALF_UP);
        new pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl().ejecutar("sp_producto_actualizar_costo", idProducto, costoNuevo);
    }

    @Override
    public RecepcionCompra registrar(RecepcionCompra recepcion) throws SQLException {
        validarRecepcion(recepcion);
        usuarioBO.verificarRol(recepcion.getUsuarioRegistro().getId(), TipoRol.ALMACENERO);
        List<DetalleRecepcionCompra> detalles = recepcion.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La recepción debe tener al menos un detalle");
        }

        return Transaccion.ejecutar(() -> {
            int idCompra = recepcion.getCompra().getId();
            compraDAO.bloquearCompra(idCompra);

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
                Validador.obligatorio(detalle, "detalle de recepción");
                Validador.obligatorio(detalle.getDetalleCompra(), "detalle de compra");
                int idLinea = detalle.getDetalleCompra().getId();
                if (!idsRecibidos.add(idLinea)) {
                    throw new IllegalArgumentException("Un detalle de compra no puede repetirse en una recepción");
                }
                DetalleCompra linea = lineas.get(idLinea);
                if (linea == null) {
                    throw new IllegalArgumentException("El detalle no pertenece a la compra");
                }
                double cantidad = detalle.getCantidadRecibida();
                ReglasInventario.validarCantidadRecepcion(cantidad, linea.getCantidadRecibida(), linea.getCantidad());
                if (!Double.isFinite(linea.getFactorConversion()) || linea.getFactorConversion() <= 0) {
                    throw new IllegalArgumentException("El factor de conversión debe ser positivo");
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
                double cantidadStock = ReglasInventario.convertirAStock(
                        detalle.getCantidadRecibida(), linea.getFactorConversion());
                int idProducto = linea.getProducto().getId();
                double stockAnterior = stockDAO.obtenerStockParaActualizar(idProducto);
                double nuevoStock = ReglasInventario.ingresar(stockAnterior, cantidadStock);
                actualizarCostoPromedio(idProducto, stockAnterior, cantidadStock, linea);
                stockDAO.actualizarStock(idProducto, nuevoStock);

                MovimientoInventario movimiento = new MovimientoInventario();
                Producto producto = new Producto();
                producto.setId(idProducto);
                movimiento.setProducto(producto);
                movimiento.setTipo(TipoMovimientoInventario.INGRESO_COMPRA);
                movimiento.setCantidad(cantidadStock);
                movimiento.setStockResultante(nuevoStock);
                movimiento.setRecepcionCompra(recepcion);
                movimiento.setUsuarioRegistro(recepcion.getUsuarioRegistro());
                movimiento.setMotivo("Ingreso por recepción de compra");
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

            return recepcion;
        });
    }

    @Override
    public RecepcionCompra modificar(RecepcionCompra recepcion, int idAlmacenero) throws SQLException {
        validarRecepcion(recepcion);
        validarId(recepcion.getId());
        usuarioBO.verificarRol(idAlmacenero, TipoRol.ALMACENERO);
        return Transaccion.ejecutar(() -> {
            RecepcionCompra existente = recepcionDAO.load(recepcion.getId());
            if (existente == null) {
                throw new IllegalArgumentException("No existe la recepción con ID " + recepcion.getId());
            }
            if (existente.isAnulada()) {
                throw new IllegalStateException("No se puede modificar una recepción anulada");
            }
            if (existente.getUsuarioRegistro().getId() != recepcion.getUsuarioRegistro().getId()) {
                throw new IllegalArgumentException("No se puede cambiar el usuario que registró la recepción");
            }
            if (!existente.getFechaRecepcion().equals(recepcion.getFechaRecepcion())) {
                throw new IllegalArgumentException("La fecha histórica de recepción no puede modificarse");
            }
            if (existente.getCompra().getId() != recepcion.getCompra().getId()) {
                throw new IllegalArgumentException("No se puede cambiar la compra de una recepción");
            }
            // La fecha permanece inalterada; se permite corregir solo las observaciones.
            recepcionDAO.update(recepcion);
            return recepcion;
        });
    }

    @Override
    public RecepcionCompra obtener(int idRecepcion) throws SQLException {
        validarId(idRecepcion);
        return Transaccion.ejecutar(() -> {
            return recepcionDAO.load(idRecepcion);
        });
    }

    @Override
    public List<RecepcionCompra> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return recepcionDAO.listAll();
        });
    }

    private void validarRecepcion(RecepcionCompra recepcion) {
        Validador.obligatorio(recepcion, "recepción");
        Validador.obligatorio(recepcion.getCompra(), "compra");
        Validador.obligatorio(recepcion.getUsuarioRegistro(), "usuario de registro");
        Validador.obligatorio(recepcion.getFechaRecepcion(), "fecha de recepción");
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
