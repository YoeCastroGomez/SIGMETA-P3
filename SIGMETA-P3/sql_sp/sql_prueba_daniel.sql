
-- =========================================================================
-- SCRIPT DE PRUEBAS PARA PROCEDIMIENTOS ALMACENADOS
-- MODULO ABASTECIMIENTO E INVENTARIO
-- =========================================================================

USE sigmeta;

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

SELECT @id_detalle_compra AS 'ID Detalle Compra Generado';

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

SELECT @id_detalle_recepcion AS 'ID Detalle Recepcion Generado';

CALL sp_detalle_recepcion_compra_obtener(@id_detalle_recepcion);

-- Modificamos la cantidad recibida de 1 a 2 unidades.

CALL sp_detalle_recepcion_compra_modificar(
    @id_detalle_recepcion,
    2.000
);

CALL sp_detalle_recepcion_compra_listar();

-- Actualizamos la cantidad recibida acumulada del detalle de compra.

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

CALL sp_detalle_compra_obtener(@id_detalle_compra);


-- ---------------------------------------------------------
-- 5. PRUEBA: MOVIMIENTO INVENTARIO
-- ---------------------------------------------------------

-- Se registra un movimiento de ingreso por las 2 unidades recibidas.
-- El stock resultante es un valor de prueba.
-- Este procedimiento no actualiza automaticamente producto.stock_actual.

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
-- 6. PRUEBA: ELIMINACION DE DETALLE Y ANULACION DE COMPRA
-- ---------------------------------------------------------

-- Creamos una segunda compra para probar la anulacion.
-- Esta compra no tendra recepciones asociadas.

CALL sp_compra_insertar(
    1,
    'COMP-TEST-ANULACION',
    '2026-09-28',
    'SOLES',
    50.00,
    9.00,
    59.00,
    'Compra creada para probar la anulacion',
    1,
    'REGISTRADA',
    '2026-10-05',
    @id_compra_anulacion
);

SELECT @id_compra_anulacion AS 'ID Compra para Anulacion';

-- Insertamos un detalle que todavia no tiene recepciones.

CALL sp_detalle_compra_insertar(
    @id_compra_anulacion,
    1,
    1,
    2.000,
    25.00,
    0.00,
    50.00,
    'UNIDAD',
    1.000,
    @id_detalle_anulacion
);

SELECT @id_detalle_anulacion AS 'ID Detalle para Eliminacion';

-- Eliminacion fisica del detalle sin recepciones.

CALL sp_detalle_compra_eliminar(@id_detalle_anulacion);

-- Comprobamos que el detalle ya no existe.

SELECT *
FROM detalle_compra
WHERE id = @id_detalle_anulacion;

-- Anulacion logica de la compra sin recepciones.

CALL sp_compra_eliminar(@id_compra_anulacion);

CALL sp_compra_obtener(@id_compra_anulacion);


-- ---------------------------------------------------------
-- 7. COMPROBACION FINAL
-- ---------------------------------------------------------

SELECT '--- COMPROBACION FINAL ---' AS INFO;

-- La compra anulada debe tener:
-- estado = ANULADA
-- anulado = 1

SELECT
    id,
    numero,
    estado,
    anulado,
    motivo_anulacion,
    fecha_anulacion
FROM compra
WHERE id = @id_compra_anulacion;

-- Debe devolver cero registros porque fue eliminado fisicamente.

SELECT *
FROM detalle_compra
WHERE id = @id_detalle_anulacion;

-- La compra original sigue disponible.

CALL sp_compra_obtener(@id_compra);

-- El detalle original conserva las 2 unidades recibidas.

CALL sp_detalle_compra_obtener(@id_detalle_compra);

-- La recepcion y su detalle permanecen registrados.

CALL sp_recepcion_compra_obtener(@id_recepcion);

CALL sp_detalle_recepcion_compra_obtener(@id_detalle_recepcion);

-- El movimiento historico tambien permanece registrado.

CALL sp_movimiento_inventario_obtener(@id_movimiento);


-- ---------------------------------------------------------
-- 8. REVERSIÓN DE LAS PRUEBAS
-- ---------------------------------------------------------

ROLLBACK;

SELECT 'PRUEBAS FINALIZADAS - ROLLBACK EJECUTADO' AS resultado;
