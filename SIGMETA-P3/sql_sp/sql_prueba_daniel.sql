
-- =========================================================================
-- PRUEBAS DE PROCEDIMIENTOS ALMACENADOS
-- MODULO ABASTECIMIENTO E INVENTARIO
-- =========================================================================

USE sigmeta;

-- Las pruebas se revierten al finalizar.
START TRANSACTION;

-- ---------------------------------------------------------
-- 1. PRUEBA: COMPRA
-- ---------------------------------------------------------

CALL sp_compra_insertar(
    1,
    'COMP-TEST-001',
    '2026-09-28',
    'SOLES',
    100.00,
    18.00,
    118.00,
    'Compra de prueba para procedimientos',
    1,
    'REGISTRADA',
    '2026-10-02',
    @id_compra
);

SELECT @id_compra AS 'ID Compra Generada';

CALL sp_compra_obtener(@id_compra);

CALL sp_compra_modificar(
    @id_compra,
    1,
    'COMP-TEST-001',
    '2026-09-28',
    'SOLES',
    96.00,
    17.28,
    113.28,
    'Compra de prueba modificada',
    'REGISTRADA',
    '2026-10-03'
);

CALL sp_compra_listar();


-- ---------------------------------------------------------
-- 2. PRUEBA: DETALLE COMPRA
-- ---------------------------------------------------------

CALL sp_detalle_compra_insertar(
    @id_compra,
    1,
    1,
    4.000,
    25.00,
    0.00,
    100.00,
    'UNIDAD',
    1.000,
    @id_detalle_compra
);

SELECT @id_detalle_compra AS 'ID Detalle Compra';

CALL sp_detalle_compra_obtener(@id_detalle_compra);

CALL sp_detalle_compra_modificar(
    @id_detalle_compra,
    @id_compra,
    1,
    1,
    4.000,
    24.00,
    0.00,
    96.00,
    'UNIDAD',
    1.000,
    0.000
);

CALL sp_detalle_compra_listar();


-- ---------------------------------------------------------
-- 3. PRUEBA: RECEPCION COMPRA
-- ---------------------------------------------------------

CALL sp_recepcion_compra_insertar(
    @id_compra,
    '2026-09-29',
    'Recepcion parcial de prueba',
    1,
    @id_recepcion
);

SELECT @id_recepcion AS 'ID Recepcion Generada';

CALL sp_recepcion_compra_obtener(@id_recepcion);

CALL sp_recepcion_compra_modificar(
    @id_recepcion,
    '2026-09-29',
    'Recepcion parcial verificada'
);

CALL sp_recepcion_compra_listar();


-- ---------------------------------------------------------
-- 4. PRUEBA: DETALLE RECEPCION COMPRA
-- ---------------------------------------------------------

CALL sp_detalle_recepcion_compra_insertar(
    @id_recepcion,
    @id_detalle_compra,
    1.000,
    @id_detalle_recepcion
);

SELECT @id_detalle_recepcion AS 'ID Detalle Recepcion';

CALL sp_detalle_recepcion_compra_obtener(@id_detalle_recepcion);

-- Modificamos la cantidad recibida de 1 a 2 unidades.
CALL sp_detalle_recepcion_compra_modificar(
    @id_detalle_recepcion,
    2.000
);

CALL sp_detalle_recepcion_compra_listar();

-- Actualizamos el acumulado del detalle de compra.
CALL sp_detalle_compra_modificar(
    @id_detalle_compra,
    @id_compra,
    1,
    1,
    4.000,
    24.00,
    0.00,
    96.00,
    'UNIDAD',
    1.000,
    2.000
);

-- Actualizamos el estado de la compra.
CALL sp_compra_modificar(
    @id_compra,
    1,
    'COMP-TEST-001',
    '2026-09-28',
    'SOLES',
    96.00,
    17.28,
    113.28,
    'Compra recibida parcialmente',
    'RECIBIDA_PARCIAL',
    '2026-10-03'
);


-- ---------------------------------------------------------
-- 5. PRUEBA: MOVIMIENTO INVENTARIO
-- ---------------------------------------------------------

-- Se usa un stock resultante ficticio de 102 para probar el SP.
-- Esta llamada no actualiza producto.stock_actual.

CALL sp_movimiento_inventario_insertar(
    1,
    'INGRESO_COMPRA',
    2.000,
    102.000,
    @id_recepcion,
    NULL,
    NULL,
    1,
    'Ingreso por recepcion parcial de prueba',
    NULL,
    @id_movimiento
);

SELECT @id_movimiento AS 'ID Movimiento Generado';

CALL sp_movimiento_inventario_obtener(@id_movimiento);

CALL sp_movimiento_inventario_modificar(
    @id_movimiento,
    'Ingreso de mercaderia verificado',
    NULL
);

CALL sp_movimiento_inventario_listar();


-- ---------------------------------------------------------
-- 6. PRUEBAS DE ELIMINACION LOGICA
-- ---------------------------------------------------------

CALL sp_movimiento_inventario_eliminar(@id_movimiento);

CALL sp_detalle_recepcion_compra_eliminar(@id_detalle_recepcion);

CALL sp_recepcion_compra_eliminar(@id_recepcion);

CALL sp_detalle_compra_eliminar(@id_detalle_compra);

CALL sp_compra_eliminar(@id_compra);


-- ---------------------------------------------------------
-- 7. COMPROBACION FINAL
-- ---------------------------------------------------------

SELECT
    'COMPRA' AS entidad,
    id,
    anulado
FROM compra
WHERE id = @id_compra;

SELECT
    'DETALLE COMPRA' AS entidad,
    id,
    anulado
FROM detalle_compra
WHERE id = @id_detalle_compra;

SELECT
    'RECEPCION COMPRA' AS entidad,
    id,
    anulado
FROM recepcion_compra
WHERE id = @id_recepcion;

SELECT
    'DETALLE RECEPCION' AS entidad,
    id,
    anulado
FROM detalle_recepcion_compra
WHERE id = @id_detalle_recepcion;

SELECT
    'MOVIMIENTO INVENTARIO' AS entidad,
    id,
    anulado
FROM movimiento_inventario
WHERE id = @id_movimiento;

-- Revertimos todos los registros y modificaciones de prueba.
ROLLBACK;

SELECT 'PRUEBAS FINALIZADAS - ROLLBACK EJECUTADO' AS resultado;
