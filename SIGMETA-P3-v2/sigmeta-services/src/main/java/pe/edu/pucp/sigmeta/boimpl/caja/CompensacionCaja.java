package pe.edu.pucp.sigmeta.boimpl.caja;

import java.sql.SQLException;
import java.util.List;
import pe.edu.pucp.sigmeta.dao.caja.CajaDAO;
import pe.edu.pucp.sigmeta.dao.caja.MovimientoCajaDAO;
import pe.edu.pucp.sigmeta.daoimpl.caja.CajaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.caja.MovimientoCajaDAOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

// RF010, RNF002: los movimientos de caja no se borran. Cuando un cobro deja de ser valido (venta anulada,
// nota de credito, cobro anulado) se registra el movimiento contrario en la misma caja, asi el cierre
// no lo cuenta. Se usa dentro de la transaccion del BO que lo llama (no hace commit ni close).
public class CompensacionCaja {

    // documento_origen de los movimientos de caja
    public static final String DOCUMENTO_VENTA = "VENTA";
    public static final String DOCUMENTO_ANULACION_VENTA = "ANULACION_VENTA";
    public static final String DOCUMENTO_NOTA_CREDITO = "NOTA_CREDITO";
    public static final String DOCUMENTO_ANULACION_NOTA_CREDITO = "ANULACION_NOTA_CREDITO";

    private final MovimientoCajaDAO movimientoCajaDAO;
    private final CajaDAO cajaDAO;

    public CompensacionCaja() {
        this.movimientoCajaDAO = new MovimientoCajaDAOImpl();
        this.cajaDAO = new CajaDAOImpl();
    }

    // movimientos de caja registrados para un documento (por ejemplo, los cobros al contado de una venta)
    public List<MovimientoCaja> buscar(String documentoOrigen, int idDocumentoOrigen) throws SQLException {
        return movimientoCajaDAO.listarTodosConDetalle().stream()
                .filter(m -> documentoOrigen.equals(m.getDocumentoOrigen()) && m.getIdDocumentoOrigen() == idDocumentoOrigen)
                .toList();
    }

    // registra el movimiento contrario en la misma caja del original; la caja debe seguir abierta
    public MovimientoCaja compensar(MovimientoCaja original, String concepto, String documentoOrigen,
                                    int idDocumentoOrigen, Usuario responsable) throws SQLException {
        Caja caja = cajaDAO.load(original.getCaja().getId());
        if (caja == null || !caja.isAbierta()) {
            throw new IllegalStateException("La caja " + original.getCaja().getId()
                    + " ya esta cerrada; el Administrador debe reabrirla para revertir el movimiento");
        }
        TipoMovimientoCaja contrario = original.getTipo() == TipoMovimientoCaja.EGRESO_MANUAL
                ? TipoMovimientoCaja.INGRESO_MANUAL : TipoMovimientoCaja.EGRESO_MANUAL;
        return registrar(caja, contrario, original.getMedioPago(), original.getMonto(), concepto,
                documentoOrigen, idDocumentoOrigen, responsable);
    }

    // registra un egreso en la caja abierta del cajero (por ejemplo, la devolucion de una nota de credito)
    public MovimientoCaja egresoEnCajaAbierta(int idCajero, MedioPago medioPago, double monto, String concepto,
                                              String documentoOrigen, int idDocumentoOrigen, Usuario responsable) throws SQLException {
        Caja caja = cajaDAO.obtenerCajaAbiertaPorUsuario(idCajero);
        if (caja == null) {
            throw new IllegalStateException("El cajero debe tener su caja abierta para registrar la devolucion de dinero (RF010)");
        }
        return registrar(caja, TipoMovimientoCaja.EGRESO_MANUAL, medioPago, monto, concepto,
                documentoOrigen, idDocumentoOrigen, responsable);
    }

    private MovimientoCaja registrar(Caja caja, TipoMovimientoCaja tipo, MedioPago medioPago, double monto, String concepto,
                                     String documentoOrigen, int idDocumentoOrigen, Usuario responsable) throws SQLException {
        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setCaja(caja);
        movimiento.setTipo(tipo);
        movimiento.setMedioPago(medioPago);
        movimiento.setMonto(monto);
        movimiento.setConcepto(concepto);
        movimiento.setDocumentoOrigen(documentoOrigen);
        movimiento.setIdDocumentoOrigen(idDocumentoOrigen);
        movimiento.setUsuarioRegistro(responsable);
        return movimientoCajaDAO.save(movimiento);
    }
}
