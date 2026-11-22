package pe.edu.pucp.sigmeta.ws.caja;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.caja.CajaBO;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

// Conversiones de CajaRS entre las clases del modelo y lo que viaja en JSON (y el resumen de la jornada, RF010).
// Parte de CajaRS (mismo paquete, no publica): el recurso REST conserva sus rutas y delega aqui.
final class CajaConversion {

    private CajaConversion() {
    }

    static MovimientoCaja aMovimiento(MovimientoDatos datos, int idCaja) {
        MovimientoCaja movimiento = new MovimientoCaja();
        Caja caja = new Caja();
        caja.setId(idCaja);
        movimiento.setCaja(caja);
        movimiento.setTipo(Utilidades.leerEnum(TipoMovimientoCaja.class, datos.tipo, "tipo de movimiento"));
        movimiento.setMedioPago(Utilidades.leerEnum(MedioPago.class, datos.medioPago, "medio de pago"));
        movimiento.setMonto(datos.monto);
        movimiento.setConcepto(datos.concepto);
        return movimiento;
    }

    static CajaDatos convertir(Caja caja) {
        CajaDatos d = new CajaDatos();
        d.id = caja.getId();
        d.fechaApertura = Utilidades.fechaHora(caja.getFechaApertura());
        d.montoInicial = caja.getMontoInicial();
        d.usuarioApertura = Utilidades.usuario(caja.getUsuarioApertura());
        d.abierta = caja.isAbierta();
        return d;
    }

    // RF010: resumen de la jornada con los movimientos de la caja
    static CajaDatos conResumen(CajaBO cajaBO, Caja caja) throws SQLException {
        CajaDatos d = convertir(caja);
        Map<MedioPago, CajaDatos.TotalMedioPago> porMedio = new EnumMap<>(MedioPago.class);
        for (MovimientoCaja m : cajaBO.listarMovimientosPorCaja(caja.getId())) {
            boolean egreso = m.getTipo() == TipoMovimientoCaja.EGRESO_MANUAL;
            switch (m.getTipo()) {
                case COBRO_CONTADO -> d.totalCobrosContado += m.getMonto();
                case COBRO_CREDITO -> d.totalCobrosCredito += m.getMonto();
                case INGRESO_MANUAL -> d.totalIngresosManuales += m.getMonto();
                case EGRESO_MANUAL -> {
                    if (m.getDocumentoOrigen() == null) {
                        d.totalEgresosManuales += m.getMonto();
                    } else {
                        d.totalDevoluciones += m.getMonto();
                    }
                }
            }
            CajaDatos.TotalMedioPago t = porMedio.computeIfAbsent(m.getMedioPago(), k -> {
                CajaDatos.TotalMedioPago nuevo = new CajaDatos.TotalMedioPago();
                nuevo.medioPago = k.name();
                return nuevo;
            });
            if (egreso) {
                t.egresos += m.getMonto();
            } else {
                t.ingresos += m.getMonto();
            }
            t.neto = t.ingresos - t.egresos;
        }
        d.porMedioPago = new ArrayList<>(porMedio.values());
        d.montoEsperado = cajaBO.calcularMontoCalculado(caja.getId());
        return d;
    }

    static MovimientoDatos convertir(MovimientoCaja m) {
        MovimientoDatos d = new MovimientoDatos();
        d.id = m.getId();
        d.idCaja = m.getCaja() != null ? m.getCaja().getId() : 0;
        d.tipo = m.getTipo() != null ? m.getTipo().name() : null;
        d.fechaMovimiento = Utilidades.fechaHora(m.getFechaMovimiento());
        d.medioPago = m.getMedioPago() != null ? m.getMedioPago().name() : null;
        d.monto = m.getMonto();
        d.concepto = m.getConcepto();
        d.documentoOrigen = m.getDocumentoOrigen();
        d.idDocumentoOrigen = m.getIdDocumentoOrigen();
        d.usuarioRegistro = Utilidades.usuario(m.getUsuarioRegistro());
        d.manual = (m.getTipo() == TipoMovimientoCaja.INGRESO_MANUAL || m.getTipo() == TipoMovimientoCaja.EGRESO_MANUAL)
                && m.getDocumentoOrigen() == null;
        return d;
    }

    static String comprobante(Venta venta) {
        if (venta.getComprobantes() == null || venta.getComprobantes().isEmpty()) {
            return null;
        }
        Comprobante c = venta.getComprobantes().get(0);
        return c.getSerie() + "-" + c.getNumero();
    }
}
