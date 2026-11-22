package pe.edu.pucp.sigmeta.model.enums;

/**
 * Estado de una cuenta por cobrar.
 */
public enum EstadoCuentaPorCobrar {
    PENDIENTE,
    PAGADA_PARCIAL,
    PAGADA,
    VENCIDA,
    // RF009: la venta se anulo; la cuenta se conserva (RNF002) con su monto original y saldo 0, y no se cobra
    ANULADA
}
