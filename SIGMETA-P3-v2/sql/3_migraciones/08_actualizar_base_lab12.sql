USE sigmeta;
SET NAMES utf8mb4;

-- =========================================================================
-- MIGRACION 08 - ACTUALIZAR UNA BASE QUE YA EXISTE CON LOS CAMBIOS DEL LAB 12
-- Junta SOLO lo nuevo, para no recargar todos los scripts. Cada bloque esta copiado tal cual de su
-- archivo de origen (que sigue siendo la referencia para una base nueva).
--
--   1. UNIQUE(numero) en cotizacion .................. de 3_migraciones/07_migracion_unicidad_cotizacion.sql
--   2. Correlativos, cotizaciones vencidas y anuladas, ventas cobradas al contado (Sebastian)
--      y UNIQUE en comprobante y orden de compra de cliente ... de 2_procedimientos/04_SP_sebastian.sql (secciones 9 y 10)
--   3. Reactivar productos y clientes ................ de 2_procedimientos/07_SP_reforma.sql
--
-- Es re-ejecutable: los procedimientos se borran y se vuelven a crear, y los UNIQUE solo se agregan si faltan.
-- Si algun bloque muestra una columna "aviso", hay numeros repetidos y ese UNIQUE NO se agrego: corregirlos
-- y volver a ejecutar el archivo.
-- DESPUES DE EJECUTARLO: reiniciar GlassFish (guarda en cache la forma de los procedimientos).
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. NUMERO DE COTIZACION UNICO (H09, catalogo)
-- -------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE IF EXISTS tmp_unicidad_cotizacion$$
CREATE PROCEDURE tmp_unicidad_cotizacion()
BEGIN
    IF EXISTS (SELECT 1 FROM cotizacion GROUP BY numero HAVING COUNT(*) > 1) THEN
        SELECT 'cotizacion: hay numeros repetidos, no se agrego UNIQUE' AS aviso, numero, COUNT(*) AS veces
        FROM cotizacion GROUP BY numero HAVING COUNT(*) > 1;
    ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cotizacion'
                         AND CONSTRAINT_NAME = 'uk_cotizacion_numero') THEN
        ALTER TABLE cotizacion ADD CONSTRAINT uk_cotizacion_numero UNIQUE (numero);
        SELECT 'cotizacion: se agrego UNIQUE(numero)' AS resultado;
    ELSE
        SELECT 'cotizacion: UNIQUE(numero) ya existia, no se hizo nada' AS resultado;
    END IF;
END$$

DELIMITER ;

CALL tmp_unicidad_cotizacion();
DROP PROCEDURE IF EXISTS tmp_unicidad_cotizacion;

-- -------------------------------------------------------------------------
-- 2. VENTAS: CORRELATIVOS Y CONSULTAS NUEVAS, Y NUMERACION UNICA (H09, Sebastian)
-- -------------------------------------------------------------------------

DELIMITER $$

-- ============================================================
-- 9. CORRELATIVOS (H09)
-- ============================================================

-- Numero que recibiria el proximo documento de un tipo y anio, SIN reservarlo (solo para mostrarlo en el
-- formulario). El numero real lo reserva sp_correlativo_siguiente al guardar (06_SP_integridad.sql).
DROP PROCEDURE IF EXISTS sp_correlativo_proximo$$
CREATE PROCEDURE sp_correlativo_proximo(
    IN p_tipo VARCHAR(20),
    IN p_anio INT
)
BEGIN
    SELECT COALESCE((SELECT ultimo FROM correlativo_documento
                     WHERE tipo = p_tipo AND anio = p_anio), 0) + 1 AS numero;
END$$

-- RF007: pasa a VENCIDA las cotizaciones registradas o enviadas cuya vigencia ya paso. Un solo UPDATE
-- (atomico): solo cambia el estado, asi no pisa una cotizacion que otro usuario este editando a la vez.
DROP PROCEDURE IF EXISTS sp_cotizacion_marcar_vencidas$$
CREATE PROCEDURE sp_cotizacion_marcar_vencidas(OUT p_cantidad INT)
BEGIN
    UPDATE cotizacion
    SET estado = 'VENCIDA'
    WHERE anulado = FALSE
      AND estado IN ('REGISTRADA', 'ENVIADA')
      AND fecha_vigencia < CURDATE();
    SET p_cantidad = ROW_COUNT();
END$$

-- RF007: cotizaciones anuladas (baja logica), para el filtro por estado "Anulada" de la lista.
-- sp_cotizacion_listar (02_SP_claudia.sql) sigue trayendo solo las vigentes y no se toca.
DROP PROCEDURE IF EXISTS sp_cotizacion_listar_anuladas$$
CREATE PROCEDURE sp_cotizacion_listar_anuladas()
BEGIN
    SELECT *
    FROM cotizacion
    WHERE anulado = TRUE
    ORDER BY id;
END$$

-- RF008/RF010: ventas al contado que ya tienen su cobro en caja (no se modifican). Una sola consulta para
-- toda la lista de ventas, con el mismo criterio que sp_venta_contar_cobros_contado (07_SP_reforma.sql).
DROP PROCEDURE IF EXISTS sp_venta_ids_cobradas_contado$$
CREATE PROCEDURE sp_venta_ids_cobradas_contado()
BEGIN
    SELECT DISTINCT id_documento_origen AS id_venta
    FROM movimiento_caja
    WHERE documento_origen = 'VENTA' AND tipo = 'COBRO_CONTADO';
END$$

DELIMITER ;

-- ============================================================
-- 10. NUMERACION UNICA (H09) - ajuste de tablas para bases que ya existen
-- ============================================================
-- Se ejecuta cada vez que se recarga este archivo y no hace nada si ya se aplico:
--   - comprobante: UNIQUE(serie, numero)
--   - orden_compra_cliente: UNIQUE(numero)
--   - correlativo_documento: fila 'OCC' por anio con el mayor numero ya usado (nunca retrocede).
--     Desde ahora el numero de la orden lo reserva sp_correlativo_siguiente('OCC', anio), como en ventas.
-- Si hay numeros repetidos no agrega la restriccion: muestra cuales son (corregirlos y recargar el archivo).
-- Una base nueva ya trae todo esto desde 1_base/sigmeta.sql.

DELIMITER $$

DROP PROCEDURE IF EXISTS tmp_unicidad_h09$$
CREATE PROCEDURE tmp_unicidad_h09()
BEGIN
    IF EXISTS (SELECT 1 FROM comprobante GROUP BY serie, numero HAVING COUNT(*) > 1) THEN
        SELECT 'comprobante: hay serie y numero repetidos, no se agrego UNIQUE' AS aviso, serie, numero, COUNT(*) AS veces
        FROM comprobante GROUP BY serie, numero HAVING COUNT(*) > 1;
    ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comprobante'
                         AND CONSTRAINT_NAME = 'uk_comprobante_serie_numero') THEN
        ALTER TABLE comprobante ADD CONSTRAINT uk_comprobante_serie_numero UNIQUE (serie, numero);
    END IF;

    IF EXISTS (SELECT 1 FROM orden_compra_cliente GROUP BY numero HAVING COUNT(*) > 1) THEN
        SELECT 'orden_compra_cliente: hay numeros repetidos, no se agrego UNIQUE' AS aviso, numero, COUNT(*) AS veces
        FROM orden_compra_cliente GROUP BY numero HAVING COUNT(*) > 1;
    ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orden_compra_cliente'
                         AND CONSTRAINT_NAME = 'uk_orden_compra_cliente_numero') THEN
        ALTER TABLE orden_compra_cliente ADD CONSTRAINT uk_orden_compra_cliente_numero UNIQUE (numero);
    END IF;
END$$

DELIMITER ;

CALL tmp_unicidad_h09();
DROP PROCEDURE IF EXISTS tmp_unicidad_h09;

INSERT INTO correlativo_documento (tipo, anio, ultimo)
SELECT * FROM (
    SELECT 'OCC' AS tipo,
           CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(numero, '-', 2), '-', -1) AS UNSIGNED) AS anio,
           MAX(CAST(SUBSTRING_INDEX(numero, '-', -1) AS UNSIGNED)) AS ultimo
    FROM orden_compra_cliente
    WHERE numero REGEXP '^OCC-[0-9]{4}-[0-9]+$'
    GROUP BY anio
) AS existentes
ON DUPLICATE KEY UPDATE ultimo = GREATEST(correlativo_documento.ultimo, existentes.ultimo);

-- -------------------------------------------------------------------------
-- 3. REACTIVAR PRODUCTOS Y CLIENTES (RF005, RF003)
-- -------------------------------------------------------------------------

DELIMITER $$

-- RF005: vuelve a activar un producto desactivado (baja logica reversible)
DROP PROCEDURE IF EXISTS sp_producto_activar$$
CREATE PROCEDURE sp_producto_activar(IN p_id_producto INT)
BEGIN
    UPDATE producto SET estado = TRUE WHERE id = p_id_producto;
END$$

-- RF003: vuelve a activar un cliente desactivado (baja logica reversible)
DROP PROCEDURE IF EXISTS sp_cliente_activar$$
CREATE PROCEDURE sp_cliente_activar(IN p_id_cliente INT)
BEGIN
    UPDATE cliente SET estado = TRUE WHERE id = p_id_cliente;
END$$

DELIMITER ;

-- -------------------------------------------------------------------------
-- 4. COMPROBACION (deben salir 6 procedimientos y 3 restricciones)
-- -------------------------------------------------------------------------

SELECT ROUTINE_NAME AS procedimiento FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'sigmeta' AND ROUTINE_NAME IN ('sp_producto_activar', 'sp_cliente_activar',
      'sp_venta_ids_cobradas_contado', 'sp_cotizacion_marcar_vencidas', 'sp_cotizacion_listar_anuladas', 'sp_correlativo_proximo');

SELECT TABLE_NAME AS tabla, CONSTRAINT_NAME AS restriccion FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'sigmeta' AND CONSTRAINT_NAME IN ('uk_cotizacion_numero', 'uk_comprobante_serie_numero', 'uk_orden_compra_cliente_numero');
