package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
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
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoDocumentoIdentidad;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private final pe.edu.pucp.sigmeta.dao.ConsultaDAO consultaDAO = new pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl();
    // partes de este BO (mismo paquete): notas de credito, anulacion y representacion impresa
    private final NotaCreditoEmision notas;
    private final ComprobanteAnulacion anulacion;
    private final ComprobanteImpreso impreso;

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
        this.notas = new NotaCreditoEmision(comprobanteDAO, detalleNotaCreditoDAO, ventaDAO, detalleVentaDAO, clienteDAO,
                productoDAO, stockDAO, movimientoDAO, usuarioBO, compensacionCaja, autorizacionBO);
        this.anulacion = new ComprobanteAnulacion(comprobanteDAO, detalleNotaCreditoDAO, ventaDAO, stockDAO, movimientoDAO,
                usuarioBO, compensacionCaja, despachoDAO);
        this.impreso = new ComprobanteImpreso(comprobanteDAO, ventaDAO, detalleVentaDAO, clienteDAO, productoDAO, usuarioBO, consultaDAO);
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
            // RF008: foto de lo que se imprime (cliente y lineas con codigo y nombre del producto). La
            // representacion impresa sale siempre de esta foto, aunque despues cambien el cliente o el catalogo
            consultaDAO.ejecutar("sp_comprobante_congelar", comprobante.getId());
            cargarFotoImpresa(comprobante);
            return comprobante;
        });
    }

    // RF008: completa el comprobante con el cliente tal como se imprimio (ComprobanteImpreso)
    private void cargarFotoImpresa(Comprobante c) throws SQLException {
        impreso.cargarFotoImpresa(c);
    }

    // RF013: emision de nota de credito autorizada por el Administrador (NotaCreditoEmision)
    @Override
    public Comprobante registrarNotaCredito(Comprobante notaCredito, int idResponsable) throws SQLException {
        return notas.registrarNotaCredito(notaCredito, idResponsable);
    }

    // RF013/RF014: el Cajero pide la autorizacion de ESA nota (NotaCreditoEmision)
    @Override
    public SolicitudAutorizacion solicitarNotaCredito(Comprobante notaCredito, int idResponsable) throws SQLException {
        return notas.solicitarNotaCredito(notaCredito, idResponsable);
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
        guardarConNumeroUnico(comprobanteDAO, c);
    }

    static void guardarConNumeroUnico(ComprobanteDAO dao, Comprobante c) throws SQLException {
        try {
            dao.save(c);
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uk_comprobante_serie_numero")) {
                throw new IllegalStateException("El número " + c.getSerie() + "-" + c.getNumero()
                        + " ya fue usado por otro comprobante. Vuelva a intentarlo para recibir el siguiente número");
            }
            throw e;
        }
    }

    // RF008/RF013: anula una factura, boleta o nota de credito (ComprobanteAnulacion)
    @Override
    public void anular(int idComprobante, String motivo, int idResponsable) throws SQLException {
        anulacion.anular(idComprobante, motivo, idResponsable);
    }

    @Override
    public Comprobante obtener(int id) throws SQLException {
        return Transaccion.ejecutar(() -> {
            Comprobante c = comprobanteDAO.load(id);
            cargarFotoImpresa(c);
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

    // RF008: representacion impresa en PDF (ComprobanteImpreso)
    @Override
    public byte[] pdf(int idComprobante, int idResponsable) throws SQLException {
        return impreso.pdf(idComprobante, idResponsable);
    }
    // RF008: registro del envio del comprobante electronico (ComprobanteImpreso)
    @Override
    public Comprobante registrarEnvio(int idComprobante, String medioEnvio, int idResponsable) throws SQLException {
        return impreso.registrarEnvio(idComprobante, medioEnvio, idResponsable);
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
