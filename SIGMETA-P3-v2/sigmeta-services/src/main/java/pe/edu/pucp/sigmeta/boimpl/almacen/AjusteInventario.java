package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import java.sql.SQLException;
import static pe.edu.pucp.sigmeta.boimpl.almacen.MovimientoInventarioBOImpl.EPSILON;
import static pe.edu.pucp.sigmeta.boimpl.almacen.MovimientoInventarioBOImpl.OPERACION_AJUSTE_INVENTARIO;

// RF012/RF014: ajustes por conteo fisico, su reversion y el pedido de autorizacion.
// Parte de MovimientoInventarioBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class AjusteInventario {

    private final MovimientoInventarioDAO movimientoDAO;
    private final StockInventarioDAO stockDAO;
    private final UsuarioBO usuarioBO;
    private final SolicitudAutorizacionBO autorizacionBO;

    AjusteInventario(MovimientoInventarioDAO movimientoDAO, StockInventarioDAO stockDAO, UsuarioBO usuarioBO, SolicitudAutorizacionBO autorizacionBO) {
        this.movimientoDAO = movimientoDAO;
        this.stockDAO = stockDAO;
        this.usuarioBO = usuarioBO;
        this.autorizacionBO = autorizacionBO;
    }

    public MovimientoInventario revertirAjuste(int idMovimientoAjuste, int idAlmacenero,
                                                 String motivo) throws SQLException {
        MovimientoInventarioBOImpl.validarId(idMovimientoAjuste);
        MovimientoInventarioBOImpl.validarId(idAlmacenero);
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
            MovimientoInventario reversion = MovimientoInventarioBOImpl.nuevaCompensacion(original, idAlmacenero, razon);
            reversion.setTipo(ingresoOriginal ? TipoMovimientoInventario.REVERSION_AJUSTE_INGRESO
                    : TipoMovimientoInventario.REVERSION_AJUSTE_SALIDA);
            reversion.setStockResultante(nuevoStock);
            stockDAO.actualizarStock(idProducto, nuevoStock);
            movimientoDAO.registrarCompensacion(reversion);
            autorizacionBO.consumir(autorizacion.getId());
            return reversion;
        });
    }

    public MovimientoInventario registrarAjuste(int idProducto, int idUsuario,
                                                  double cantidadContada, String motivo) throws SQLException {
        MovimientoInventarioBOImpl.validarId(idProducto);
        MovimientoInventarioBOImpl.validarId(idUsuario);
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

    static String huellaAjuste(int idProducto, double cantidadContada) {
        return pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.huella(
                "AJUSTE|" + idProducto + "|" + String.format(java.util.Locale.ROOT, "%.3f", cantidadContada));
    }

    static String huellaReversionAjuste(int idMovimientoAjuste) {
        return pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl.huella("REVERTIR_AJUSTE|" + idMovimientoAjuste);
    }

    static String cantidad(double valor) {
        return java.math.BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString();
    }

    // RF012/RF014: el Almacenero cuenta el producto y pide la autorizacion de ESE ajuste. No mueve el stock:
    // el Administrador ve el producto, el stock del sistema, lo contado y la diferencia; al aprobar se registra.
    public pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarAjuste(int idProducto, int idUsuario,
            double cantidadContada, String motivo) throws SQLException {
        MovimientoInventarioBOImpl.validarId(idProducto);
        MovimientoInventarioBOImpl.validarId(idUsuario);
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
    public pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion solicitarReversionAjuste(int idMovimientoAjuste,
            int idAlmacenero, String motivo) throws SQLException {
        MovimientoInventarioBOImpl.validarId(idMovimientoAjuste);
        MovimientoInventarioBOImpl.validarId(idAlmacenero);
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
}
