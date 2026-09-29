-- Ejecutar una sola vez sobre una base SIGMETA existente.
-- Si se crea la base desde cero con sql/sigmeta.sql, NO ejecutar esta migracion.
ALTER TABLE movimiento_inventario
    MODIFY COLUMN tipo ENUM(
        'INGRESO_COMPRA',
        'SALIDA_DESPACHO',
        'INGRESO_PRODUCCION',
        'SALIDA_PRODUCCION',
        'INGRESO_DEVOLUCION',
        'INGRESO_REVERSION_DESPACHO',
        'AJUSTE_INGRESO',
        'AJUSTE_SALIDA'
    ) NOT NULL,
    ADD COLUMN id_movimiento_revertido INT NULL,
    ADD UNIQUE KEY uq_movimiento_revertido (id_movimiento_revertido),
    ADD CONSTRAINT fk_movimiento_inventario_reversion
        FOREIGN KEY (id_movimiento_revertido) REFERENCES movimiento_inventario(id);
