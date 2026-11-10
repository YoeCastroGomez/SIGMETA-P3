package pe.edu.pucp.sigmeta.boimpl.cobranza;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import pe.edu.pucp.sigmeta.bo.cobranza.CuentaPorCobrarBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.cobranza.CobroDAO;
import pe.edu.pucp.sigmeta.dao.cobranza.CuentaPorCobrarDAO;
import pe.edu.pucp.sigmeta.dao.ventas.ComprobanteDAO;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CobroDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.ComprobanteDAOImpl;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.cobranza.ResumenCuentasPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;

public class CuentaPorCobrarBOImpl implements CuentaPorCobrarBO {

    private final CuentaPorCobrarDAO cuentaPorCobrarDAO;
    private final CobroDAO cobroDAO;
    private final ComprobanteDAO comprobanteDAO;
    private final UsuarioBO usuarioBO;

    public CuentaPorCobrarBOImpl() {
        this.cuentaPorCobrarDAO = new CuentaPorCobrarDAOImpl();
        this.cobroDAO = new CobroDAOImpl();
        this.comprobanteDAO = new ComprobanteDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF009: normalmente la genera la venta al credito; el registro manual es del Administrador
    @Override
    public CuentaPorCobrar insertar(CuentaPorCobrar cxc, int idResponsable) throws SQLException {
        return Transaccion.ejecutar(() -> {
            usuarioBO.obtenerAdministradorActivo(idResponsable);
            Validador.obligatorio(cxc, "cuenta por cobrar");
            Validador.obligatorio(cxc.getVenta(), "venta");
            Validador.obligatorio(cxc.getCliente(), "cliente");
            pe.edu.pucp.sigmeta.model.ventas.Venta venta = new pe.edu.pucp.sigmeta.daoimpl.ventas.VentaDAOImpl().load(cxc.getVenta().getId());
            if (venta == null || venta.isAnulado() || venta.getCondicionPago() != pe.edu.pucp.sigmeta.model.enums.CondicionPago.CREDITO) {
                throw new IllegalArgumentException("La cuenta exige una venta al credito vigente");
            }
            if (cxc.getCliente().getId() != venta.getCliente().getId()) throw new IllegalArgumentException("El cliente no corresponde a la venta");
            if (cuentaPorCobrarDAO.obtenerPorVenta(venta.getId()) != null) throw new IllegalStateException("La venta ya tiene cuenta por cobrar");
            if (venta.getTotal() <= 0) throw new IllegalArgumentException("La venta no genera deuda positiva");
            cxc.setVenta(venta);
            cxc.setCliente(venta.getCliente());
            cxc.setFechaEmision(venta.getFechaEmision());
            cxc.setFechaVencimiento(venta.getFechaEmision().plusDays(venta.getPlazoCreditoDias()));
            cxc.setMontoOriginal(redondear(venta.getTotal()));
            cxc.setMontoPagado(0);cxc.setSaldoPendiente(cxc.getMontoOriginal());
            cxc.setEstado(calcularEstado(cxc));
            cuentaPorCobrarDAO.save(cxc);
            return cxc;
        });
    }

    // RF009: el Administrador puede refinanciar (cambiar el vencimiento); los montos solo los mueven los cobros
    @Override
    public CuentaPorCobrar modificar(CuentaPorCobrar cxc, int idResponsable) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idResponsable);
        Validador.obligatorio(cxc, "cuenta por cobrar");
        return Transaccion.ejecutar(() -> {
            CuentaPorCobrar actual = obtenerExistente(cxc.getId());
            actual.setFechaVencimiento(cxc.getFechaVencimiento());
            validarVencimiento(actual);
            actual.setEstado(calcularEstado(actual));
            cuentaPorCobrarDAO.update(actual);
            return actual;
        });
    }

    // RNF002: solo una cuenta sin cobros (generada por error); con cobros se conserva
    @Override
    public void eliminar(CuentaPorCobrar cxc, int idResponsable) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idResponsable);
        Validador.obligatorio(cxc, "cuenta por cobrar");
        Transaccion.ejecutar(() -> {
            CuentaPorCobrar actual = obtenerExistente(cxc.getId());
            pe.edu.pucp.sigmeta.model.ventas.Venta venta = new pe.edu.pucp.sigmeta.daoimpl.ventas.VentaDAOImpl().load(actual.getVenta().getId());
            if (venta == null || !venta.isAnulado()) throw new IllegalStateException("No se elimina la deuda de una venta vigente");
            if (!cobroDAO.listarPorCuentaPorCobrar(actual.getId()).isEmpty()) {
                throw new IllegalStateException("No se puede eliminar una cuenta por cobrar con cobros registrados");
            }
            cuentaPorCobrarDAO.remove(actual);
            return null;
        });
    }

    @Override
    public CuentaPorCobrar obtenerPorId(int id) throws SQLException {
        return Transaccion.ejecutar(() -> {
            CuentaPorCobrar cxc = cuentaPorCobrarDAO.load(id);
            if (cxc != null) {
                completarComprobantes(List.of(cxc));
            }
            return cxc;
        });
    }

    @Override
    public CuentaPorCobrar obtenerPorVenta(int idVenta) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return cuentaPorCobrarDAO.obtenerPorVenta(idVenta);
        });
    }

    // RF009: busqueda por cliente
    @Override
    public List<CuentaPorCobrar> listarPorCliente(int idCliente) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return completarComprobantes(cuentaPorCobrarDAO.listarPorCliente(idCliente));
        });
    }

    @Override
    public List<CuentaPorCobrar> listarPorEstado(EstadoCuentaPorCobrar estado) throws SQLException {
        Validador.obligatorio(estado, "estado");
        return Transaccion.ejecutar(() -> {
            return completarComprobantes(cuentaPorCobrarDAO.listarPorEstado(estado));
        });
    }

    // RF009: el Administrador consulta las cuentas vencidas
    @Override
    public List<CuentaPorCobrar> listarVencidas() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return completarComprobantes(cuentaPorCobrarDAO.listarVencidas());
        });
    }

    @Override
    public List<CuentaPorCobrar> listarTodasConDetalle() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return completarComprobantes(cuentaPorCobrarDAO.listarTodasConDetalle());
        });
    }

    // RF009: busqueda por comprobante. El comprobante (FACTURA o BOLETA vigente) se busca por su serie-numero
    // y con su venta se encuentra la cuenta. Se filtra aqui en el BO (no hay SP para esta busqueda).
    @Override
    public List<CuentaPorCobrar> buscarPorComprobante(String comprobante) throws SQLException {
        String texto = Validador.textoObligatorio(comprobante, "comprobante", 20).toUpperCase();
        return Transaccion.ejecutar(() -> {
            Set<Integer> ventas = comprobanteDAO.listarTodos().stream()
                    .filter(this::esComprobanteVigente)
                    .filter(c -> numeroComprobante(c).contains(texto))
                    .map(c -> c.getVenta().getId())
                    .collect(Collectors.toSet());
            List<CuentaPorCobrar> todas = completarComprobantes(cuentaPorCobrarDAO.listarTodasConDetalle());
            return todas.stream()
                    .filter(cxc -> cxc.getVenta() != null && ventas.contains(cxc.getVenta().getId()))
                    .collect(Collectors.toList());
        });
    }

    // RF009: busqueda por rango de fechas (de emision o de vencimiento). Se filtra aqui en el BO.
    @Override
    public List<CuentaPorCobrar> buscarPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin, boolean porVencimiento) throws SQLException {
        Validador.obligatorio(fechaInicio, "fecha de inicio");
        Validador.obligatorio(fechaFin, "fecha de fin");
        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        return Transaccion.ejecutar(() -> {
            List<CuentaPorCobrar> lista = new ArrayList<>();
            for (CuentaPorCobrar cxc : cuentaPorCobrarDAO.listarTodasConDetalle()) {
                LocalDate fecha = porVencimiento ? cxc.getFechaVencimiento() : cxc.getFechaEmision();
                if (fecha != null && !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin)) {
                    lista.add(cxc);
                }
            }
            return completarComprobantes(lista);
        });
    }

    // RF009: el Administrador ve el total de saldos pendientes y de cuentas vencidas
    @Override
    public ResumenCuentasPorCobrar obtenerResumen(int idResponsable) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idResponsable);
        return Transaccion.ejecutar(() -> {
            ResumenCuentasPorCobrar resumen = new ResumenCuentasPorCobrar();
            double pendiente = 0;
            double vencido = 0;
            int conSaldo = 0;
            int vencidas = 0;
            for (CuentaPorCobrar cxc : cuentaPorCobrarDAO.listarTodasConDetalle()) {
                if (cxc.getSaldoPendiente() <= 0.005) {
                    continue;
                }
                pendiente += cxc.getSaldoPendiente();
                conSaldo++;
                if (cxc.isVencida()) {
                    vencido += cxc.getSaldoPendiente();
                    vencidas++;
                }
            }
            resumen.setTotalSaldoPendiente(redondear(pendiente));
            resumen.setTotalSaldoVencido(redondear(vencido));
            resumen.setCuentasConSaldo(conSaldo);
            resumen.setCuentasVencidas(vencidas);
            return resumen;
        });
    }

    // RF009: a cada cuenta le pone el comprobante vigente de su venta (para mostrarlo y buscar por el)
    private List<CuentaPorCobrar> completarComprobantes(List<CuentaPorCobrar> cuentas) throws SQLException {
        if (cuentas.isEmpty()) {
            return cuentas;
        }
        Map<Integer, List<Comprobante>> porVenta = new HashMap<>();
        for (Comprobante c : comprobanteDAO.listarTodos()) {
            if (esComprobanteVigente(c)) {
                porVenta.computeIfAbsent(c.getVenta().getId(), k -> new ArrayList<>()).add(c);
            }
        }
        for (CuentaPorCobrar cxc : cuentas) {
            if (cxc.getVenta() != null) {
                cxc.getVenta().setComprobantes(porVenta.getOrDefault(cxc.getVenta().getId(), new ArrayList<>()));
            }
        }
        return cuentas;
    }

    private boolean esComprobanteVigente(Comprobante c) {
        return c.getVenta() != null
                && (c.getTipo() == TipoComprobante.FACTURA || c.getTipo() == TipoComprobante.BOLETA)
                && c.getEstado() != EstadoComprobante.ANULADO;
    }

    // serie-numero del comprobante, por ejemplo F001-00000123
    public static String numeroComprobante(Comprobante c) {
        return ((c.getSerie() != null ? c.getSerie() : "") + "-" + (c.getNumero() != null ? c.getNumero() : "")).toUpperCase();
    }

    private CuentaPorCobrar obtenerExistente(int idCuenta) throws SQLException {
        CuentaPorCobrar cxc = cuentaPorCobrarDAO.load(idCuenta);
        if (cxc == null) {
            throw new IllegalArgumentException("No existe la cuenta por cobrar con id " + idCuenta);
        }
        return cxc;
    }

    private void validarVencimiento(CuentaPorCobrar cxc) {
        Validador.obligatorio(cxc.getFechaVencimiento(), "fecha de vencimiento");
        if (cxc.getFechaVencimiento().isBefore(cxc.getFechaEmision())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de emision");
        }
    }

    // RF009: el estado se deriva del saldo y del vencimiento
    public static EstadoCuentaPorCobrar calcularEstado(CuentaPorCobrar cxc) {
        if (cxc.getSaldoPendiente() <= 0.005) {
            return EstadoCuentaPorCobrar.PAGADA;
        }
        if (cxc.getFechaVencimiento().isBefore(LocalDate.now())) {
            return EstadoCuentaPorCobrar.VENCIDA;
        }
        return cxc.getMontoPagado() > 0 ? EstadoCuentaPorCobrar.PAGADA_PARCIAL : EstadoCuentaPorCobrar.PENDIENTE;
    }

    public static double redondear(double monto) {
        return BigDecimal.valueOf(monto).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
