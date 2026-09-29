package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.bo.ventas.ComprobanteBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.almacen.MovimientoInventarioDAO;
import pe.edu.pucp.sigmeta.dao.almacen.StockInventarioDAO;
import pe.edu.pucp.sigmeta.dao.maestros.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.ComprobanteDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleNotaCreditoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleVentaDAO;
import pe.edu.pucp.sigmeta.dao.ventas.VentaDAO;
import pe.edu.pucp.sigmeta.daoimpl.almacen.MovimientoInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.almacen.StockInventarioDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.maestros.ClienteDAOImpl;
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
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComprobanteBOImpl implements ComprobanteBO {

    private static final BigDecimal TASA_IGV = new BigDecimal("0.18");

    private final ComprobanteDAO comprobanteDAO;
    private final DetalleNotaCreditoDAO detalleNotaCreditoDAO;
    private final VentaDAO ventaDAO;
    private final DetalleVentaDAO detalleVentaDAO;
    private final ClienteDAO clienteDAO;
    private final ProductoDAO productoDAO;
    private final StockInventarioDAO stockDAO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final UsuarioBO usuarioBO;

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
    }

    // RF008: Emision de factura o boleta
    @Override
    public Comprobante emitir(int idVenta, TipoComprobante tipo, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.CAJERO);
        Validador.obligatorio(tipo, "tipo de comprobante");
        if (tipo != TipoComprobante.FACTURA && tipo != TipoComprobante.BOLETA) {
            throw new IllegalArgumentException("La emision directa solo admite FACTURA o BOLETA");
        }

        try {
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
            comprobante.setMoneda(venta.getMoneda());
            comprobante.setSubTotal(venta.getSubTotal());
            comprobante.setIgv(venta.getIgv());
            comprobante.setTotal(venta.getTotal());
            comprobante.setEstado(EstadoComprobante.EMITIDO);
            comprobante.setComprobanteRelacionado(null);
            comprobante.setFechaRegistro(LocalDateTime.now());

            comprobanteDAO.save(comprobante);
            transactionContext.commit();
            return comprobante;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF013: Emision de nota de credito autorizada por administrador
    @Override
    public Comprobante registrarNotaCredito(Comprobante notaCredito, int idAdministradorAutoriza, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO);
        usuarioBO.obtenerAdministradorActivo(idAdministradorAutoriza);

        Validador.obligatorio(notaCredito, "nota de credito");
        Validador.obligatorio(notaCredito.getComprobanteRelacionado(), "comprobante origen");
        String motivoValidado = Validador.textoObligatorio(notaCredito.getMotivo(), "motivo de nota de credito", 200);
        notaCredito.setMotivo(motivoValidado);

        List<DetalleNotaCredito> detalles = notaCredito.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La nota de credito debe tener al menos una linea de detalle");
        }

        try {
            int idOrigen = notaCredito.getComprobanteRelacionado().getId();
            Comprobante origen = comprobanteDAO.load(idOrigen);
            if (origen == null) {
                throw new IllegalArgumentException("No existe el comprobante origen con id " + idOrigen);
            }
            if (origen.getTipo() != TipoComprobante.FACTURA && origen.getTipo() != TipoComprobante.BOLETA) {
                throw new IllegalArgumentException("La nota de credito solo puede emitirse sobre una FACTURA o BOLETA");
            }
            if (origen.getEstado() != EstadoComprobante.EMITIDO && origen.getEstado() != EstadoComprobante.ACEPTADO) {
                throw new IllegalStateException("El comprobante origen debe estar en estado EMITIDO o ACEPTADO");
            }

            Venta venta = ventaDAO.load(origen.getVenta().getId());
            if (venta == null || venta.isAnulado()) {
                throw new IllegalStateException("La venta asociada al comprobante origen no existe o esta anulada");
            }

            List<DetalleVenta> detallesVenta = detalleVentaDAO.listarPorVenta(venta.getId());
            Map<Integer, DetalleVenta> mapaDetallesVenta = new HashMap<>();
            for (DetalleVenta dv : detallesVenta) {
                mapaDetallesVenta.put(dv.getProducto().getId(), dv);
            }

            // Calcular cantidades ya acreditadas previamente en notas de credito vigentes
            List<Comprobante> ncsExistentes = comprobanteDAO.listarNotasCreditoPorComprobante(idOrigen);
            Map<Integer, Double> yaAcreditadoPorProd = new HashMap<>();
            for (Comprobante ncExistente : ncsExistentes) {
                List<DetalleNotaCredito> lineasExistentes = detalleNotaCreditoDAO.listarPorComprobante(ncExistente.getId());
                for (DetalleNotaCredito dnc : lineasExistentes) {
                    yaAcreditadoPorProd.merge(dnc.getProducto().getId(), dnc.getCantidad(), Double::sum);
                }
            }

            Set<Integer> productosEnNC = new HashSet<>();
            BigDecimal subTotalNC = BigDecimal.ZERO;

            for (DetalleNotaCredito d : detalles) {
                Validador.obligatorio(d.getProducto(), "producto");
                int idProd = d.getProducto().getId();
                if (!productosEnNC.add(idProd)) {
                    throw new IllegalArgumentException("No se puede repetir el mismo producto en la nota de credito");
                }
                DetalleVenta lineaVenta = mapaDetallesVenta.get(idProd);
                if (lineaVenta == null) {
                    throw new IllegalArgumentException("El producto " + idProd + " no pertenece a la venta origen");
                }
                if (!Double.isFinite(d.getCantidad()) || d.getCantidad() <= 0) {
                    throw new IllegalArgumentException("La cantidad devuelta debe ser mayor a 0");
                }

                double yaAcreditado = yaAcreditadoPorProd.getOrDefault(idProd, 0.0);
                if (yaAcreditado + d.getCantidad() > lineaVenta.getCantidad() + 0.0001) {
                    throw new IllegalArgumentException("La cantidad a devolver excede lo vendido menos lo ya acreditado para el producto "
                            + idProd + " (Vendido: " + lineaVenta.getCantidad() + ", Acreditado: " + yaAcreditado + ", Devuelto: " + d.getCantidad() + ")");
                }

                d.setPrecioUnitario(lineaVenta.getPrecioUnitario());
                d.setDescuento(0.0);
                BigDecimal importe = BigDecimal.valueOf(d.getCantidad())
                        .multiply(BigDecimal.valueOf(d.getPrecioUnitario()))
                        .setScale(2, RoundingMode.HALF_UP);
                d.setImporte(importe.doubleValue());
                subTotalNC = subTotalNC.add(importe);
            }

            BigDecimal igvNC = subTotalNC.multiply(TASA_IGV).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalNC = subTotalNC.add(igvNC);

            if (totalNC.doubleValue() > origen.getTotal() + 0.01) {
                throw new IllegalArgumentException("El monto total de la nota de credito no puede superar el total del comprobante origen");
            }

            String serie = (origen.getTipo() == TipoComprobante.FACTURA) ? "FC01" : "BC01";
            String correlativo = comprobanteDAO.siguienteCorrelativo(serie);

            notaCredito.setVenta(venta);
            notaCredito.setTipo(TipoComprobante.NOTA_CREDITO);
            notaCredito.setSerie(serie);
            notaCredito.setNumero(correlativo);
            notaCredito.setFechaEmision(LocalDate.now());
            notaCredito.setMoneda(origen.getMoneda());
            notaCredito.setSubTotal(subTotalNC.doubleValue());
            notaCredito.setIgv(igvNC.doubleValue());
            notaCredito.setTotal(totalNC.doubleValue());
            notaCredito.setEstado(EstadoComprobante.EMITIDO);
            notaCredito.setComprobanteRelacionado(origen);
            notaCredito.setFechaRegistro(LocalDateTime.now());

            comprobanteDAO.save(notaCredito);

            // Guardar detalles de nota de credito
            for (DetalleNotaCredito d : detalles) {
                d.setComprobante(notaCredito);
                detalleNotaCreditoDAO.save(d);

                // Efecto 2: Reingreso de inventario (RF013)
                int idProd = d.getProducto().getId();
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
                mov.setNotaCredito(notaCredito);
                mov.setUsuarioRegistro(responsable);
                mov.setMotivo("Devolucion por NC " + serie + "-" + correlativo + ": " + motivoValidado);
                movimientoDAO.save(mov);
            }

            // Efecto 1: Reversion de saldo de cuenta por cobrar si es a credito (RF013, RF010)
            if (venta.getCondicionPago() == pe.edu.pucp.sigmeta.model.enums.CondicionPago.CREDITO) {
                comprobanteDAO.revertirSaldoCuentaPorCobrar(venta.getId(), notaCredito.getTotal());
            }

            transactionContext.commit();
            return notaCredito;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Comprobante modificarNotaCredito(Comprobante nc, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Validador.obligatorio(nc, "nota de credito");
        String motivoValidado = Validador.textoObligatorio(nc.getMotivo(), "motivo", 200);

        try {
            Comprobante actual = comprobanteDAO.load(nc.getId());
            if (actual == null) {
                throw new IllegalArgumentException("No existe el comprobante con id " + nc.getId());
            }
            if (actual.getTipo() != TipoComprobante.NOTA_CREDITO) {
                throw new IllegalArgumentException("El comprobante no es una nota de credito");
            }
            if (actual.getEstado() == EstadoComprobante.ANULADO) {
                throw new IllegalStateException("No se puede modificar una nota de credito anulada");
            }

            actual.setMotivo(motivoValidado);
            actual.setMedioEnvio(Validador.textoOpcional(nc.getMedioEnvio(), "medio de envio", 30));
            actual.setFechaEnvio(nc.getFechaEnvio());

            comprobanteDAO.update(actual);
            transactionContext.commit();
            return actual;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public void anular(int idComprobante, String motivo, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.ADMINISTRADOR, TipoRol.CAJERO);
        Validador.textoObligatorio(motivo, "motivo de anulacion", 200);

        try {
            Comprobante comprobante = comprobanteDAO.load(idComprobante);
            if (comprobante == null) {
                throw new IllegalArgumentException("No existe el comprobante con id " + idComprobante);
            }
            if (comprobante.getEstado() == EstadoComprobante.ANULADO) {
                throw new IllegalStateException("El comprobante ya se encuentra anulado");
            }

            if (comprobante.getTipo() == TipoComprobante.NOTA_CREDITO) {
                // Revertir efectos de inventario
                List<DetalleNotaCredito> detalles = detalleNotaCreditoDAO.listarPorComprobante(idComprobante);
                for (DetalleNotaCredito d : detalles) {
                    int idProd = d.getProducto().getId();
                    double stockActual = stockDAO.obtenerStockParaActualizar(idProd);
                    if (stockActual < d.getCantidad()) {
                        throw new IllegalStateException("Stock insuficiente para revertir la nota de credito en el producto " + idProd);
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
                    mov.setMotivo("Reversion de nota de credito " + comprobante.getSerie() + "-" + comprobante.getNumero());
                    movimientoDAO.save(mov);
                }
            } else {
                // Validar que no tenga notas de credito activas
                List<Comprobante> ncs = comprobanteDAO.listarNotasCreditoPorComprobante(idComprobante);
                if (!ncs.isEmpty()) {
                    throw new IllegalStateException("No se puede anular un comprobante que tiene notas de credito activas");
                }
            }

            comprobanteDAO.actualizarEstado(idComprobante, EstadoComprobante.ANULADO);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Comprobante obtener(int id) throws SQLException {
        try {
            Comprobante c = comprobanteDAO.load(id);
            if (c != null && c.getTipo() == TipoComprobante.NOTA_CREDITO) {
                List<DetalleNotaCredito> detalles = detalleNotaCreditoDAO.listarPorComprobante(id);
                for (DetalleNotaCredito d : detalles) {
                    d.setComprobante(c);
                }
                c.setDetalles(detalles);
            }
            return c;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<Comprobante> listarTodos() throws SQLException {
        try {
            return comprobanteDAO.listarTodos();
        } finally {
            transactionContext.close();
        }
    }
}
