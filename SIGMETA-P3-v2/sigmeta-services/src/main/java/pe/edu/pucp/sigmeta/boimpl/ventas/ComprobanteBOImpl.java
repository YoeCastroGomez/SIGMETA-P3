package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.ComprobanteBO;
import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.boimpl.seguridad.SolicitudAutorizacionBOImpl;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.almacen.DespachoDAO;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.dao.socio.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.ComprobanteDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleNotaCreditoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleVentaDAO;
import pe.edu.pucp.sigmeta.dao.ventas.VentaDAO;
import pe.edu.pucp.sigmeta.daoimpl.almacen.DespachoDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.MovimientoInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.StockInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.socio.ClienteDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.ComprobanteDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.DetalleNotaCreditoDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.DetalleVentaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.VentaDAOImpl;
import pe.edu.pucp.sigmeta.model.almacen.MovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoDocumentoIdentidad;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComprobanteBOImpl implements ComprobanteBO {


    // RF013: tipo de la nota de credito segun el catalogo 09 de SUNAT. En SIGMETA toda nota es por devolucion de
    // mercaderia, asi que el tipo se deduce (no lo elige el usuario y no puede quedar mal):
    //   06 DEVOLUCION_TOTAL     la nota acredita el total del comprobante de origen
    //   07 DEVOLUCION_POR_ITEM  la nota devuelve solo parte de los productos o de las cantidades
    public static final String NOTA_DEVOLUCION_TOTAL = "DEVOLUCION_TOTAL";
    public static final String NOTA_DEVOLUCION_POR_ITEM = "DEVOLUCION_POR_ITEM";

    public static String tipoNotaCredito(double totalNota, double totalOrigen) {
        return Math.abs(totalNota - totalOrigen) <= 0.01 ? NOTA_DEVOLUCION_TOTAL : NOTA_DEVOLUCION_POR_ITEM;
    }
    // RF013/RF014: operacion restringida que el Administrador autoriza para emitir una nota de credito
    public static final String OPERACION_NOTA_CREDITO = SolicitudAutorizacionBOImpl.OPERACION_NOTA_CREDITO;

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
    private final DespachoDAO despachoDAO;

    public ComprobanteBOImpl() {
        this.comprobanteDAO = new ComprobanteDAOImpl();
        this.detalleNotaCreditoDAO = new DetalleNotaCreditoDAOImpl();
        this.ventaDAO = new VentaDAOImpl();
        this.detalleVentaDAO = new DetalleVentaDAOImpl();
        this.clienteDAO = new ClienteDAOImpl();
        this.productoDAO = new ProductoDAOImpl();
        this.stockDAO = new StockInventarioDAOImpl();
        this.movimientoDAO = new MovimientoInventarioDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
        this.compensacionCaja = new CompensacionCaja();
        this.autorizacionBO = new SolicitudAutorizacionBOImpl();
        this.despachoDAO = new DespachoDAOImpl();
    }

    // RF008: Emision de factura o boleta
    @Override
    public Comprobante emitir(int idVenta, TipoComprobante tipo, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.CAJERO);
        Validador.obligatorio(tipo, "tipo de comprobante");
        if (tipo != TipoComprobante.FACTURA && tipo != TipoComprobante.BOLETA) {
            throw new IllegalArgumentException("La emisión directa solo admite FACTURA o BOLETA");
        }

        return Transaccion.ejecutar(() -> {
            Venta venta = ventaDAO.load(idVenta);
            if (venta == null) {
                throw new IllegalArgumentException("No existe la venta con id " + idVenta);
            }
            if (venta.isAnulado()) {
                throw new IllegalStateException("No se puede emitir comprobante para una venta anulada");
            }

            // Validar que no tenga ya un comprobante principal vigente
            List<Comprobante> existentes = comprobanteDAO.listarPorVenta(idVenta);
            for (Comprobante c : existentes) {
                if ((c.getTipo() == TipoComprobante.FACTURA || c.getTipo() == TipoComprobante.BOLETA)
                        && c.getEstado() != EstadoComprobante.ANULADO) {
                    throw new IllegalStateException("La venta ya cuenta con un comprobante emitido vigente: "
                            + c.getSerie() + "-" + c.getNumero());
                }
            }

            Cliente cliente = clienteDAO.load(venta.getCliente().getId());
            if (tipo == TipoComprobante.FACTURA) {
                if (cliente.getTipoDocumento() != TipoDocumentoIdentidad.RUC) {
                    throw new IllegalArgumentException("La FACTURA requiere que el cliente cuente con RUC (cliente tiene: "
                            + cliente.getTipoDocumento() + ")");
                }
            }

            String serie = (tipo == TipoComprobante.FACTURA) ? "F001" : "B001";
            String correlativo = comprobanteDAO.siguienteCorrelativo(serie);

            Comprobante comprobante = new Comprobante();
            comprobante.setVenta(venta);
            comprobante.setTipo(tipo);
            comprobante.setSerie(serie);
            comprobante.setNumero(correlativo);
            comprobante.setFechaEmision(LocalDate.now());
            comprobante.setSubTotal(venta.getSubTotal());
            comprobante.setIgv(venta.getIgv());
            comprobante.setTotal(venta.getTotal());
            comprobante.setEstado(EstadoComprobante.EMITIDO);
            comprobante.setComprobanteRelacionado(null);
            comprobante.setFechaRegistro(LocalDateTime.now());

            guardarConNumeroUnico(comprobante);
            return comprobante;
        });
    }

    // RF013: Emision de nota de credito autorizada por administrador.
    // La sesion es del Cajero; la autorizacion del Administrador es la solicitud que aprobo en su propia
    // sesion (RF014). De ahi se toma quien autorizo y la autorizacion se consume (un solo uso).
    @Override
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
            NotaPreparada preparada = prepararNotaCredito(notaCredito);
            Comprobante origen = preparada.origen();
            Venta venta = preparada.venta();
            BigDecimal subTotalNC = preparada.subTotal();
            BigDecimal igvNC = preparada.igv();
            BigDecimal totalNC = preparada.total();

            // RF014: la autorizacion aprobada de ESTA nota (mismo comprobante y mismas cantidades)
            SolicitudAutorizacion autorizacion = autorizacionBO.requerirAutorizacion(idResponsable, OPERACION_NOTA_CREDITO,
                    huellaNotaCredito(notaCredito));
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

            guardarConNumeroUnico(notaCredito);

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

    // RF013: valida la nota y calcula sus montos SIN mover nada (stock, deuda, caja). La usan registrar y solicitar.
    private NotaPreparada prepararNotaCredito(Comprobante notaCredito) throws SQLException {
        List<DetalleNotaCredito> detalles = notaCredito.getDetalles();
            int idOrigen = notaCredito.getComprobanteRelacionado().getId();
            Comprobante origen = comprobanteDAO.load(idOrigen);
            if (origen == null) {
                throw new IllegalArgumentException("No existe el comprobante origen con id " + idOrigen);
            }
            if (origen.getTipo() != TipoComprobante.FACTURA && origen.getTipo() != TipoComprobante.BOLETA) {
                throw new IllegalArgumentException("La nota de crédito solo puede emitirse sobre una FACTURA o BOLETA");
            }
            if (origen.getEstado() != EstadoComprobante.EMITIDO && origen.getEstado() != EstadoComprobante.ACEPTADO) {
                throw new IllegalStateException("El comprobante origen debe estar en estado EMITIDO o ACEPTADO");
            }

            Venta venta = ventaDAO.load(origen.getVenta().getId());
            if (venta == null || venta.isAnulado()) {
                throw new IllegalStateException("La venta asociada al comprobante origen no existe o está anulada");
            }

            // RF013: cada linea de la nota apunta a la linea de venta concreta que se devuelve
            List<DetalleVenta> detallesVenta = detalleVentaDAO.listarPorVenta(venta.getId());
            Map<Integer, DetalleVenta> mapaDetallesVenta = new HashMap<>();
            for (DetalleVenta dv : detallesVenta) {
                mapaDetallesVenta.put(dv.getId(), dv);
            }

            // Cantidades ya acreditadas por linea de venta en notas de credito vigentes
            List<Comprobante> ncsExistentes = comprobanteDAO.listarNotasCreditoPorComprobante(idOrigen);
            Map<Integer, Double> yaAcreditadoPorLinea = new HashMap<>();
            for (Comprobante ncExistente : ncsExistentes) {
                List<DetalleNotaCredito> lineasExistentes = detalleNotaCreditoDAO.listarPorComprobante(ncExistente.getId());
                for (DetalleNotaCredito dnc : lineasExistentes) {
                    yaAcreditadoPorLinea.merge(dnc.getDetalleVenta().getId(), dnc.getCantidad(), Double::sum);
                }
            }

            Set<Integer> lineasEnNC = new HashSet<>();
            BigDecimal subTotalNC = BigDecimal.ZERO;

            for (DetalleNotaCredito d : detalles) {
                Validador.obligatorio(d.getDetalleVenta(), "línea de venta");
                int idLinea = d.getDetalleVenta().getId();
                if (!lineasEnNC.add(idLinea)) {
                    throw new IllegalArgumentException("No se puede repetir la misma línea de venta en la nota de crédito");
                }
                DetalleVenta lineaVenta = mapaDetallesVenta.get(idLinea);
                if (lineaVenta == null) {
                    throw new IllegalArgumentException("La línea " + idLinea + " no pertenece a la venta origen");
                }
                if (!Double.isFinite(d.getCantidad()) || d.getCantidad() <= 0) {
                    throw new IllegalArgumentException("La cantidad devuelta debe ser mayor a 0");
                }

                // solo se devuelve lo ENTREGADO al cliente (despachado), descontando lo ya acreditado:
                // asi el reingreso al stock siempre tiene una salida previa (SALIDA_DESPACHO)
                double yaAcreditado = yaAcreditadoPorLinea.getOrDefault(idLinea, 0.0);
                double entregado = lineaVenta.getCantidadDespachada();
                if (yaAcreditado + d.getCantidad() > entregado + 0.0001) {
                    throw new IllegalArgumentException("Solo se puede devolver lo entregado al cliente en la línea "
                            + lineaVenta.getNumeroLinea() + " (Entregado: " + entregado + ", Ya acreditado: "
                            + yaAcreditado + ", Devuelto: " + d.getCantidad() + ")");
                }

                // se devuelve el precio efectivamente cobrado: importe de la linea (ya con descuento) por unidad.
                // El descuento de la nota es la parte proporcional del descuento de la linea.
                BigDecimal bruto = BigDecimal.valueOf(d.getCantidad())
                        .multiply(BigDecimal.valueOf(lineaVenta.getPrecioUnitario()))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal importe = BigDecimal.valueOf(lineaVenta.getImporte())
                        .multiply(BigDecimal.valueOf(d.getCantidad()))
                        .divide(BigDecimal.valueOf(lineaVenta.getCantidad()), 2, RoundingMode.HALF_UP);
                d.setDetalleVenta(lineaVenta);
                d.setPrecioUnitario(lineaVenta.getPrecioUnitario());
                d.setDescuento(bruto.subtract(importe).doubleValue());
                d.setImporte(importe.doubleValue());
                subTotalNC = subTotalNC.add(importe);
            }

            // los importes ya incluyen IGV (precio cobrado): el total de la nota es su suma y el IGV se desglosa
            BigDecimal totalNC = subTotalNC;
            BigDecimal igvNC = PreciosIgv.igv(totalNC);
            subTotalNC = PreciosIgv.valorVenta(totalNC);

            if (totalNC.doubleValue() > origen.getTotal() + 0.01) {
                throw new IllegalArgumentException("El monto total de la nota de crédito no puede superar el total del comprobante origen");
            }
        return new NotaPreparada(origen, venta, subTotalNC, igvNC, totalNC);
    }

    private record NotaPreparada(Comprobante origen, Venta venta, BigDecimal subTotal, BigDecimal igv, BigDecimal total) {
    }

    // RF014: huella de la nota = comprobante de origen + cada linea devuelta con su cantidad
    private static String huellaNotaCredito(Comprobante notaCredito) {
        StringBuilder datos = new StringBuilder("NC|").append(notaCredito.getComprobanteRelacionado().getId());
        notaCredito.getDetalles().stream()
                .sorted(java.util.Comparator.comparingInt(d -> d.getDetalleVenta().getId()))
                .forEach(d -> datos.append('|').append(d.getDetalleVenta().getId()).append(':')
                        .append(String.format(java.util.Locale.ROOT, "%.3f", d.getCantidad())));
        return SolicitudAutorizacionBOImpl.huella(datos.toString());
    }

    // RF013/RF014: el Cajero prepara la nota y pide la autorizacion de ESA nota. El Administrador ve el comprobante,
    // el cliente, los productos y el total; al aprobar, el Cajero la registra (registrarNotaCredito).
    @Override
    public SolicitudAutorizacion solicitarNotaCredito(Comprobante notaCredito, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        Validador.obligatorio(notaCredito, "nota de crédito");
        Validador.obligatorio(notaCredito.getComprobanteRelacionado(), "comprobante origen");
        String motivo = Validador.textoObligatorio(notaCredito.getMotivo(), "motivo de nota de crédito", 200);
        if (notaCredito.getDetalles() == null || notaCredito.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La nota de crédito debe tener al menos una línea de detalle");
        }
        return Transaccion.ejecutar(() -> {
            NotaPreparada p = prepararNotaCredito(notaCredito);
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
                    comprobante, detalle.toString(), huellaNotaCredito(notaCredito));
        });
    }

    // RF013: modificar una nota de credito no anulada. Solo cambian los datos descriptivos (motivo y, si se manda,
    // el envio). El detalle y los montos no se editan: ya movieron stock, deuda y caja; para corregirlos se anula
    // la nota (revierte todo) y se emite otra con una nueva autorizacion.
    @Override
    public Comprobante modificarNotaCredito(Comprobante nc, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Validador.obligatorio(nc, "nota de crédito");
        String motivoValidado = Validador.textoObligatorio(nc.getMotivo(), "motivo", 200);

        return Transaccion.ejecutar(() -> {
            Comprobante actual = comprobanteDAO.load(nc.getId());
            if (actual == null) {
                throw new IllegalArgumentException("No existe el comprobante con id " + nc.getId());
            }
            if (actual.getTipo() != TipoComprobante.NOTA_CREDITO) {
                throw new IllegalArgumentException("El comprobante no es una nota de crédito");
            }
            if (actual.getEstado() == EstadoComprobante.ANULADO) {
                throw new IllegalStateException("No se puede modificar una nota de crédito anulada");
            }

            actual.setMotivo(motivoValidado);
            // el envio solo cambia si se manda; si no, se conserva el que ya tenia la nota
            String medio = Validador.textoOpcional(nc.getMedioEnvio(), "medio de envío", 30);
            if (medio != null) {
                actual.setMedioEnvio(medio);
                actual.setFechaEnvio(nc.getFechaEnvio() != null ? nc.getFechaEnvio() : LocalDateTime.now());
            }

            comprobanteDAO.update(actual);
            return actual;
        });
    }

    // H09: la tabla tiene UNIQUE(serie, numero). Si otra emision tomo el mismo numero, la base rechaza el
    // registro; se informa con un mensaje de negocio (409) en vez del error tecnico de MySQL.
    private void guardarConNumeroUnico(Comprobante c) throws SQLException {
        try {
            comprobanteDAO.save(c);
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uk_comprobante_serie_numero")) {
                throw new IllegalStateException("El número " + c.getSerie() + "-" + c.getNumero()
                        + " ya fue usado por otro comprobante. Vuelva a intentarlo para recibir el siguiente número");
            }
            throw e;
        }
    }

    // RF008: anula una factura o boleta (Vendedor, Cajero o Administrador) para que la venta pueda corregirse o
    // anularse. Solo si la mercaderia NO se entrego: con despachos vigentes la devolucion se hace con nota de credito.
    // RF013: anula una nota de credito (Cajero o Administrador) revirtiendo stock, deuda y caja.
    @Override
    public void anular(int idComprobante, String motivo, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.ADMINISTRADOR, TipoRol.CAJERO, TipoRol.VENDEDOR);
        Validador.textoObligatorio(motivo, "motivo de anulación", 200);

        Transaccion.ejecutar(() -> {
            Comprobante comprobante = comprobanteDAO.load(idComprobante);
            if (comprobante == null) {
                throw new IllegalArgumentException("No existe el comprobante con id " + idComprobante);
            }
            if (comprobante.getEstado() == EstadoComprobante.ANULADO) {
                throw new IllegalStateException("El comprobante ya se encuentra anulado");
            }

            if (comprobante.getTipo() == TipoComprobante.NOTA_CREDITO) {
                if (responsable.getRol().getTipo() == TipoRol.VENDEDOR) {
                    throw new IllegalStateException("El rol VENDEDOR no anula notas de crédito; lo hace el Cajero o el Administrador");
                }
                // Revertir efectos de inventario
                List<DetalleNotaCredito> detalles = detalleNotaCreditoDAO.listarPorComprobante(idComprobante);
                for (DetalleNotaCredito d : detalles) {
                    int idProd = d.getDetalleVenta().getProducto().getId();
                    double stockActual = stockDAO.obtenerStockParaActualizar(idProd);
                    if (stockActual < d.getCantidad()) {
                        throw new IllegalStateException("Stock insuficiente para revertir la nota de crédito en el producto " + idProd);
                    }
                    double nuevoStock = stockActual - d.getCantidad();
                    stockDAO.actualizarStock(idProd, nuevoStock);

                    MovimientoInventario mov = new MovimientoInventario();
                    Producto prod = new Producto();
                    prod.setId(idProd);
                    mov.setProducto(prod);
                    mov.setTipo(TipoMovimientoInventario.AJUSTE_SALIDA);
                    mov.setCantidad(d.getCantidad());
                    mov.setStockResultante(nuevoStock);
                    mov.setUsuarioRegistro(responsable);
                    mov.setMotivo("Reversión de nota de crédito " + comprobante.getSerie() + "-" + comprobante.getNumero());
                    movimientoDAO.save(mov);
                }

                // Revertir el efecto en la cuenta por cobrar: la deuda vuelve a su monto anterior (RF013)
                Venta venta = ventaDAO.load(comprobante.getVenta().getId());
                if (venta != null && venta.getCondicionPago() == pe.edu.pucp.sigmeta.model.enums.CondicionPago.CREDITO) {
                    // la deuda vuelve solo por la parte que la nota rebajo; lo que se devolvio en efectivo
                    // se revierte abajo, con su ingreso compensatorio en la caja
                    BigDecimal devueltoEnCaja = BigDecimal.ZERO;
                    for (MovimientoCaja devolucion : compensacionCaja.buscar(CompensacionCaja.DOCUMENTO_NOTA_CREDITO, idComprobante)) {
                        devueltoEnCaja = devueltoEnCaja.add(BigDecimal.valueOf(devolucion.getMonto()));
                    }
                    BigDecimal aLaDeuda = BigDecimal.valueOf(comprobante.getTotal()).subtract(devueltoEnCaja)
                            .setScale(2, RoundingMode.HALF_UP);
                    if (aLaDeuda.signum() > 0) {
                        comprobanteDAO.restaurarSaldoCuentaPorCobrar(venta.getId(), aLaDeuda.doubleValue());
                    }
                }
                // Revertir la devolucion de dinero en caja de una venta al contado (RF010)
                for (MovimientoCaja devolucion : compensacionCaja.buscar(CompensacionCaja.DOCUMENTO_NOTA_CREDITO, idComprobante)) {
                    compensacionCaja.compensar(devolucion, "Anulación de la NC " + comprobante.getSerie() + "-" + comprobante.getNumero(),
                            CompensacionCaja.DOCUMENTO_ANULACION_NOTA_CREDITO, idComprobante, responsable);
                }
            } else {
                // Validar que no tenga notas de credito activas
                List<Comprobante> ncs = comprobanteDAO.listarNotasCreditoPorComprobante(idComprobante);
                if (!ncs.isEmpty()) {
                    throw new IllegalStateException("No se puede anular un comprobante que tiene notas de crédito activas");
                }
                // RF008/RF012: si la mercaderia ya salio del almacen, el comprobante no se anula: se emite nota de credito
                if (comprobante.getVenta() != null && despachoDAO.listarPorVenta(comprobante.getVenta().getId())
                        .stream().anyMatch(d -> !d.isAnulado())) {
                    throw new IllegalStateException("La venta ya tiene mercadería despachada: para devolverla emita una nota de crédito. "
                            + "Si el despacho fue un error, anúlelo primero en almacén");
                }
            }

            comprobanteDAO.actualizarEstado(idComprobante, EstadoComprobante.ANULADO);
            return null;
        });
    }

    @Override
    public Comprobante obtener(int id) throws SQLException {
        return Transaccion.ejecutar(() -> {
            Comprobante c = comprobanteDAO.load(id);
            if (c != null && c.getTipo() == TipoComprobante.NOTA_CREDITO) {
                List<DetalleNotaCredito> detalles = detalleNotaCreditoDAO.listarPorComprobante(id);
                for (DetalleNotaCredito d : detalles) {
                    d.setComprobante(c);
                }
                c.setDetalles(detalles);
            }
            return c;
        });
    }

    @Override
    public List<Comprobante> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return comprobanteDAO.listarTodos();
        });
    }

    // RF008: representacion impresa del comprobante en PDF (JasperReports, plantilla reports/comprobante.jrxml).
    // La descargan el Vendedor y el Cajero (quienes entregan el comprobante) y el Administrador (consulta).
    @Override
    public byte[] pdf(int idComprobante, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Map<String, Object> cabecera = new HashMap<>();
        List<Map<String, ?>> lineas = new java.util.ArrayList<>();
        Transaccion.consultar(() -> {
            Comprobante c = comprobanteDAO.load(idComprobante);
            if (c == null) {
                throw new IllegalArgumentException("No existe el comprobante con id " + idComprobante);
            }
            if (c.getTipo() != TipoComprobante.FACTURA && c.getTipo() != TipoComprobante.BOLETA) {
                throw new IllegalArgumentException("Solo se genera el PDF de una FACTURA o BOLETA");
            }
            if (c.getEstado() == EstadoComprobante.ANULADO) {
                throw new IllegalStateException("El comprobante " + c.getSerie() + "-" + c.getNumero() + " está anulado");
            }
            Venta venta = ventaDAO.load(c.getVenta().getId());
            Cliente cliente = venta == null || venta.getCliente() == null ? null : clienteDAO.load(venta.getCliente().getId());
            cabecera.put("TITULO", c.getTipo() == TipoComprobante.FACTURA ? "FACTURA ELECTRÓNICA" : "BOLETA DE VENTA ELECTRÓNICA");
            cabecera.put("NUMERO", c.getSerie() + "-" + c.getNumero());
            cabecera.put("FECHA_EMISION", c.getFechaEmision() == null ? ""
                    : c.getFechaEmision().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            cabecera.put("SUB_TOTAL", c.getSubTotal());
            cabecera.put("IGV", c.getIgv());
            cabecera.put("TOTAL", c.getTotal());
            cabecera.put("VENTA", venta == null ? "" : venta.getNumero());
            cabecera.put("CONDICION_PAGO", venta == null || venta.getCondicionPago() == null ? ""
                    : venta.getCondicionPago() == pe.edu.pucp.sigmeta.model.enums.CondicionPago.CREDITO ? "Crédito" : "Contado");
            if (cliente != null) {
                cabecera.put("CLIENTE", cliente.getNombreMostrado());
                cabecera.put("TIPO_DOCUMENTO", cliente.getTipoDocumento() == null ? "Documento" : cliente.getTipoDocumento().name());
                cabecera.put("DOCUMENTO", cliente.getNumeroDocumento());
                cabecera.put("DIRECCION", cliente.getContacto() == null || cliente.getContacto().getDireccion() == null
                        ? "—" : cliente.getContacto().getDireccion());
            }
            if (venta != null) {
                for (DetalleVenta d : detalleVentaDAO.listarPorVenta(venta.getId())) {
                    Producto producto = d.getProducto() == null ? null : productoDAO.load(d.getProducto().getId());
                    Map<String, Object> linea = new HashMap<>();
                    linea.put("numeroLinea", d.getNumeroLinea());
                    linea.put("codigo", producto == null ? "" : producto.getCodigoInterno());
                    linea.put("descripcion", producto == null ? "" : producto.getNombre());
                    linea.put("cantidad", d.getCantidad());
                    linea.put("precioUnitario", d.getPrecioUnitario());
                    linea.put("descuento", d.getDescuento());
                    linea.put("importe", d.getImporte());
                    lineas.add(linea);
                }
            }
            return null;
        });
        String autor = (responsable.getNombres() + " " + responsable.getApellidos()).trim();
        return pe.edu.pucp.sigmeta.boimpl.reporte.ComprobantePdf.generar(cabecera, lineas, autor);
    }

    // RF008: Registro de envio de comprobante electronico
    @Override
    public Comprobante registrarEnvio(int idComprobante, String medioEnvio, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.CAJERO);
        String medio = Validador.textoObligatorio(medioEnvio, "medio de envío", 30).trim().toUpperCase();
        if (!medio.equals("CORREO") && !medio.equals("MENSAJERIA") && !medio.equals("IMPRESO")) {
            throw new IllegalArgumentException("Medio de envío no válido (solo se permite CORREO, MENSAJERIA o IMPRESO): " + medioEnvio);
        }

        return Transaccion.ejecutar(() -> {
            Comprobante comprobante = comprobanteDAO.load(idComprobante);
            if (comprobante == null) {
                throw new IllegalArgumentException("No existe el comprobante con id " + idComprobante);
            }
            if (comprobante.getTipo() != TipoComprobante.FACTURA && comprobante.getTipo() != TipoComprobante.BOLETA) {
                throw new IllegalArgumentException("Solo se puede registrar envío de FACTURA o BOLETA");
            }
            if (comprobante.getEstado() == EstadoComprobante.ANULADO) {
                throw new IllegalStateException("No se puede registrar el envío de un comprobante anulado");
            }
            // RF008: el correo y la mensajeria van a los datos de contacto registrados del cliente
            if (!medio.equals("IMPRESO")) {
                Venta venta = ventaDAO.load(comprobante.getVenta().getId());
                Cliente cliente = venta == null ? null : clienteDAO.load(venta.getCliente().getId());
                pe.edu.pucp.sigmeta.model.comun.DatosContacto contacto = cliente == null ? null : cliente.getContacto();
                String dato = contacto == null ? null
                        : medio.equals("CORREO") ? contacto.getCorreo() : contacto.getTelefono();
                if (dato == null || dato.isBlank()) {
                    throw new IllegalArgumentException(medio.equals("CORREO")
                            ? "El cliente no tiene correo registrado: regístrelo en su ficha o use otro medio de envío"
                            : "El cliente no tiene teléfono de contacto registrado: regístrelo en su ficha o use otro medio de envío");
                }
            }

            LocalDateTime ahora = LocalDateTime.now();
            comprobanteDAO.registrarEnvio(idComprobante, medio, ahora);
            comprobante.setMedioEnvio(medio);
            comprobante.setFechaEnvio(ahora);
            return comprobante;
        });
    }

    // RF013: Busqueda de notas de credito con filtros opcionales
    @Override
    public List<Comprobante> buscarNotasCredito(LocalDate fechaDesde, LocalDate fechaHasta, Integer idCliente, String serieNumeroOrigen) throws SQLException {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha inicial (" + fechaDesde + ") no puede ser posterior a la fecha final (" + fechaHasta + ")");
        }

        return Transaccion.ejecutar(() -> {
            List<Comprobante> lista = comprobanteDAO.buscarNotasCredito(fechaDesde, fechaHasta, idCliente, serieNumeroOrigen);
            for (Comprobante nc : lista) {
                List<DetalleNotaCredito> detalles = detalleNotaCreditoDAO.listarPorComprobante(nc.getId());
                for (DetalleNotaCredito d : detalles) {
                    d.setComprobante(nc);
                }
                nc.setDetalles(detalles);
            }
            return lista;
        });
    }
}
