package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.dao.socio.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.ComprobanteDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleNotaCreditoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleVentaDAO;
import pe.edu.pucp.sigmeta.dao.ventas.VentaDAO;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static pe.edu.pucp.sigmeta.boimpl.ventas.ComprobanteBOImpl.OPERACION_NOTA_CREDITO;

// RF013/RF014: preparar, solicitar autorizacion y registrar notas de credito (stock, deuda y caja).
// Parte de ComprobanteBOImpl: la clase publica conserva sus metodos y delega aqui (mismo paquete, no publica).
final class NotaCreditoEmision {

    private final ComprobanteDAO comprobanteDAO;
    private final DetalleNotaCreditoDAO detalleNotaCreditoDAO;
    private final VentaDAO ventaDAO;
    private final DetalleVentaDAO detalleVentaDAO;
    private final ClienteDAO clienteDAO;
    private final ProductoDAO productoDAO;
    private final StockInventarioDAO stockDAO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final UsuarioBO usuarioBO;
    private final CompensacionCaja compensacionCaja;
    private final SolicitudAutorizacionBO autorizacionBO;
    private final NotaCreditoPreparacion preparacion;

    NotaCreditoEmision(ComprobanteDAO comprobanteDAO, DetalleNotaCreditoDAO detalleNotaCreditoDAO, VentaDAO ventaDAO, DetalleVentaDAO detalleVentaDAO, ClienteDAO clienteDAO, ProductoDAO productoDAO, StockInventarioDAO stockDAO, MovimientoInventarioDAO movimientoDAO, UsuarioBO usuarioBO, CompensacionCaja compensacionCaja, SolicitudAutorizacionBO autorizacionBO) {
        this.comprobanteDAO = comprobanteDAO;
        this.detalleNotaCreditoDAO = detalleNotaCreditoDAO;
        this.ventaDAO = ventaDAO;
        this.detalleVentaDAO = detalleVentaDAO;
        this.clienteDAO = clienteDAO;
        this.productoDAO = productoDAO;
        this.stockDAO = stockDAO;
        this.movimientoDAO = movimientoDAO;
        this.usuarioBO = usuarioBO;
        this.compensacionCaja = compensacionCaja;
        this.autorizacionBO = autorizacionBO;
        this.preparacion = new NotaCreditoPreparacion(comprobanteDAO, detalleNotaCreditoDAO, ventaDAO, detalleVentaDAO);
    }

    // RF013: Emision de nota de credito autorizada por administrador.
    // La sesion es del Cajero; la autorizacion del Administrador es la solicitud que aprobo en su propia
    // sesion (RF014). De ahi se toma quien autorizo y la autorizacion se consume (un solo uso).
    public Comprobante registrarNotaCredito(Comprobante notaCredito, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);

        Validador.obligatorio(notaCredito, "nota de crédito");
        Validador.obligatorio(notaCredito.getComprobanteRelacionado(), "comprobante origen");
        String motivoValidado = Validador.textoObligatorio(notaCredito.getMotivo(), "motivo de nota de crédito", 200);
        notaCredito.setMotivo(motivoValidado);

        List<DetalleNotaCredito> detalles = notaCredito.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La nota de crédito debe tener al menos una línea de detalle");
        }

        return Transaccion.ejecutar(() -> {
            // RF013: validaciones y montos (comun con solicitarNotaCredito: se autoriza lo mismo que se registra)
            NotaCreditoPreparacion.NotaPreparada preparada = preparacion.prepararNotaCredito(notaCredito);
            Comprobante origen = preparada.origen();
            Venta venta = preparada.venta();
            BigDecimal subTotalNC = preparada.subTotal();
            BigDecimal igvNC = preparada.igv();
            BigDecimal totalNC = preparada.total();

            // RF014: la autorizacion aprobada de ESTA nota (mismo comprobante y mismas cantidades)
            SolicitudAutorizacion autorizacion = autorizacionBO.requerirAutorizacion(idResponsable, OPERACION_NOTA_CREDITO,
                    NotaCreditoPreparacion.huellaNotaCredito(notaCredito));
            Usuario admin = autorizacion.getAdministrador();

            String serie = (origen.getTipo() == TipoComprobante.FACTURA) ? "FC01" : "BC01";
            String correlativo = comprobanteDAO.siguienteCorrelativo(serie);

            notaCredito.setVenta(venta);
            notaCredito.setTipo(TipoComprobante.NOTA_CREDITO);
            notaCredito.setSerie(serie);
            notaCredito.setNumero(correlativo);
            notaCredito.setFechaEmision(LocalDate.now());
            notaCredito.setSubTotal(subTotalNC.doubleValue());
            notaCredito.setIgv(igvNC.doubleValue());
            notaCredito.setTotal(totalNC.doubleValue());
            notaCredito.setEstado(EstadoComprobante.EMITIDO);
            notaCredito.setComprobanteRelacionado(origen);
            notaCredito.setAdministradorAutoriza(admin);
            notaCredito.setFechaRegistro(LocalDateTime.now());

            ComprobanteBOImpl.guardarConNumeroUnico(comprobanteDAO, notaCredito);

            // Guardar detalles de nota de credito
            for (DetalleNotaCredito d : detalles) {
                d.setComprobante(notaCredito);
                detalleNotaCreditoDAO.save(d);

                // Efecto 2: Reingreso de inventario (RF013); el producto es el de la linea de venta
                int idProd = d.getDetalleVenta().getProducto().getId();
                double stockActual = stockDAO.obtenerStockParaActualizar(idProd);
                double nuevoStock = stockActual + d.getCantidad();
                stockDAO.actualizarStock(idProd, nuevoStock);

                MovimientoInventario mov = new MovimientoInventario();
                Producto prod = new Producto();
                prod.setId(idProd);
                mov.setProducto(prod);
                mov.setTipo(TipoMovimientoInventario.INGRESO_DEVOLUCION);
                mov.setCantidad(d.getCantidad());
                mov.setStockResultante(nuevoStock);
                mov.setComprobante(notaCredito);
                mov.setUsuarioRegistro(responsable);
                mov.setMotivo("Devolución por NC " + serie + "-" + correlativo + ": " + motivoValidado);
                movimientoDAO.save(mov);
            }

            // Efecto 1: Reversion de saldo de cuenta por cobrar si es a credito (RF013, RF010)
            if (venta.getCondicionPago() == pe.edu.pucp.sigmeta.model.enums.CondicionPago.CREDITO) {
                // La nota rebaja primero la deuda que queda. Lo que exceda el saldo es dinero que el cliente
                // ya pago: se le devuelve en efectivo por la caja abierta del cajero (igual que al contado).
                pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar cuenta =
                        new pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl().obtenerPorVenta(venta.getId());
                BigDecimal totalNota = BigDecimal.valueOf(notaCredito.getTotal()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal saldo = cuenta == null ? BigDecimal.ZERO
                        : BigDecimal.valueOf(Math.max(cuenta.getSaldoPendiente(), 0)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal aLaDeuda = totalNota.min(saldo);
                BigDecimal aDevolver = totalNota.subtract(aLaDeuda);
                if (aLaDeuda.signum() > 0) {
                    comprobanteDAO.revertirSaldoCuentaPorCobrar(venta.getId(), aLaDeuda.doubleValue());
                }
                if (aDevolver.signum() > 0) {
                    compensacionCaja.egresoEnCajaAbierta(responsable.getId(),
                            pe.edu.pucp.sigmeta.model.enums.MedioPago.EFECTIVO, aDevolver.doubleValue(),
                            "Devolución por NC " + serie + "-" + correlativo + " (venta al crédito ya pagada)",
                            CompensacionCaja.DOCUMENTO_NOTA_CREDITO, notaCredito.getId(), responsable);
                }
            } else {
                // Efecto 3 (RF010): venta al contado ya cobrada -> la devolucion sale como egreso de la caja
                // del cajero, asi la nota de credito no queda contada como cobro valido en el cierre
                List<MovimientoCaja> cobros = compensacionCaja.buscar(CompensacionCaja.DOCUMENTO_VENTA, venta.getId());
                if (!cobros.isEmpty()) {
                    compensacionCaja.egresoEnCajaAbierta(responsable.getId(), cobros.get(0).getMedioPago(),
                            notaCredito.getTotal(), "Devolución por NC " + serie + "-" + correlativo,
                            CompensacionCaja.DOCUMENTO_NOTA_CREDITO, notaCredito.getId(), responsable);
                }
            }

            // RF014: la autorizacion es de un solo uso. Si algo falla antes, el rollback la deja APROBADA
            autorizacionBO.consumir(autorizacion.getId());

            return notaCredito;
        });
    }

    // RF013/RF014: el Cajero prepara la nota y pide la autorizacion de ESA nota. El Administrador ve el comprobante,
    // el cliente, los productos y el total; al aprobar, el Cajero la registra (registrarNotaCredito).
    public SolicitudAutorizacion solicitarNotaCredito(Comprobante notaCredito, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        Validador.obligatorio(notaCredito, "nota de crédito");
        Validador.obligatorio(notaCredito.getComprobanteRelacionado(), "comprobante origen");
        String motivo = Validador.textoObligatorio(notaCredito.getMotivo(), "motivo de nota de crédito", 200);
        if (notaCredito.getDetalles() == null || notaCredito.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La nota de crédito debe tener al menos una línea de detalle");
        }
        return Transaccion.ejecutar(() -> {
            NotaCreditoPreparacion.NotaPreparada p = preparacion.prepararNotaCredito(notaCredito);
            Cliente cliente = clienteDAO.load(p.venta().getCliente().getId());
            String comprobante = p.origen().getSerie() + "-" + p.origen().getNumero();
            StringBuilder detalle = new StringBuilder()
                    .append("Cliente: ").append(cliente == null ? "—" : cliente.getNombreMostrado()).append('\n')
                    .append("Venta: ").append(p.venta().getNumero())
                    .append(p.venta().getCondicionPago() == pe.edu.pucp.sigmeta.model.enums.CondicionPago.CREDITO ? " (crédito)" : " (contado)").append('\n')
                    .append("Devuelve:");
            for (DetalleNotaCredito d : notaCredito.getDetalles()) {
                Producto producto = productoDAO.load(d.getDetalleVenta().getProducto().getId());
                detalle.append('\n').append("- ")
                        .append(new BigDecimal(String.valueOf(d.getCantidad())).stripTrailingZeros().toPlainString()).append(' ')
                        .append(producto == null ? "producto" : producto.getNombre())
                        .append(String.format(java.util.Locale.ROOT, ": S/ %.2f", d.getImporte()));
            }
            detalle.append('\n').append(String.format(java.util.Locale.ROOT, "Total de la nota: S/ %.2f", p.total()));
            return autorizacionBO.solicitarOperacion(idResponsable, OPERACION_NOTA_CREDITO, motivo,
                    comprobante, detalle.toString(), NotaCreditoPreparacion.huellaNotaCredito(notaCredito));
        });
    }
}
