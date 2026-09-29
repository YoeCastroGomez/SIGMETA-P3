package pe.edu.pucp.sigmeta.model.enums;

/**
 * Tipos de movimiento que afectan la existencia de un producto.
 */
public enum TipoMovimientoInventario {
    INGRESO_COMPRA,
    SALIDA_DESPACHO,
    INGRESO_PRODUCCION,
    SALIDA_PRODUCCION,
    INGRESO_DEVOLUCION,
    INGRESO_REVERSION_DESPACHO,
    SALIDA_REVERSION_COMPRA,
    REVERSION_AJUSTE_INGRESO,
    REVERSION_AJUSTE_SALIDA,
    AJUSTE_INGRESO,
    AJUSTE_SALIDA
}
