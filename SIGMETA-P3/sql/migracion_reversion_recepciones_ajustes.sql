-- Ejecutar una sola vez SOBRE una BD existente que ya tenga la migracion
-- migracion_reversion_despacho.sql. Para BD nueva, usar sql/sigmeta.sql.
ALTER TABLE recepcion_compra
    ADD COLUMN anulada BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE movimiento_inventario
    MODIFY COLUMN tipo ENUM(
        'INGRESO_COMPRA',
        'SALIDA_DESPACHO',
        'INGRESO_PRODUCCION',
        'SALIDA_PRODUCCION',
        'INGRESO_DEVOLUCION',
        'INGRESO_REVERSION_DESPACHO',
        'SALIDA_REVERSION_COMPRA',
        'REVERSION_AJUSTE_INGRESO',
        'REVERSION_AJUSTE_SALIDA',
        'AJUSTE_INGRESO',
        'AJUSTE_SALIDA'
    ) NOT NULL;
