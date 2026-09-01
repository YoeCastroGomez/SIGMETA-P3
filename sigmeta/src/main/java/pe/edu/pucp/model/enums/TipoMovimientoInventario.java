package pe.edu.pucp.model.enums;

/**
 * Tipos de movimiento que afectan la existencia de un producto.
 */
public enum TipoMovimientoInventario {
    INGRESO_COMPRA,
    SALIDA_DESPACHO,
    INGRESO_PRODUCCION,
    SALIDA_PRODUCCION,
    AJUSTE_INGRESO,
    AJUSTE_SALIDA
}
