-- =========================================================================
-- MIGRACION 13 - AUDITORIA DE DISENO (docs/AUDITORIA_DISENO_ERP.md, prioridad 4): UN SOLO ESTADO
-- Ejecutar UNA vez en la base que usa GlassFish (local o AWS), en MySQL Workbench (rayo), DESPUES de la 12.
-- Se puede volver a ejecutar sin problema: una tabla ya convertida no se toca.
--
-- venta, cotizacion, compra y orden_compra_cliente guardaban "anulado" dos veces: la columna anulado
-- (TRUE/FALSE) y el valor ANULADA de estado. Nada obligaba a que coincidieran. Desde esta migracion
-- anulado es una columna CALCULADA de estado: anulado = (estado = 'ANULADA'). La base la mantiene sola, no
-- se puede escribir y nunca contradice al estado. Las consultas que filtran por anulado siguen igual.
--
-- DESPUES recargar los procedimientos que ya no escriben anulado (pasos 2 a 9 del README), al menos:
--   2_procedimientos/02_SP_claudia.sql, 03_SP_daniel.sql, 04_SP_sebastian.sql y 07_SP_reforma.sql (al final)
-- Con la columna calculada, la version ANTERIOR de esos procedimientos falla al registrar o anular
-- (no se puede escribir en una columna calculada): recargarlos es obligatorio.
-- =========================================================================
USE sigmeta;

-- 1. Revision: documentos donde las dos columnas se contradicen (normalmente no trae filas)
SELECT 'venta' AS tabla, id, numero, anulado, estado FROM venta
 WHERE anulado <> (estado = 'ANULADA')
UNION ALL SELECT 'cotizacion', id, numero, anulado, estado FROM cotizacion
 WHERE anulado <> (estado = 'ANULADA')
UNION ALL SELECT 'compra', id, numero, anulado, estado FROM compra
 WHERE anulado <> (estado = 'ANULADA')
UNION ALL SELECT 'orden_compra_cliente', id, numero, anulado, estado FROM orden_compra_cliente
 WHERE anulado <> (estado = 'ANULADA');

DELIMITER $$

DROP PROCEDURE IF EXISTS tmp_migracion_13$$
CREATE PROCEDURE tmp_migracion_13(IN p_tabla VARCHAR(64))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_tabla
                     AND COLUMN_NAME = 'anulado' AND EXTRA LIKE '%GENERATED%') THEN
        -- 2. Si hay contradicciones, manda la anulacion registrada (tiene motivo y fecha): el estado pasa a ANULADA
        SET @sql = CONCAT('UPDATE ', p_tabla, ' SET estado = ''ANULADA'' WHERE anulado = TRUE AND estado <> ''ANULADA''');
        PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
        -- 3. anulado pasa a ser calculada a partir de estado
        SET @sql = CONCAT('ALTER TABLE ', p_tabla, ' MODIFY COLUMN anulado BOOLEAN ',
                          'GENERATED ALWAYS AS (estado = ''ANULADA'') STORED NOT NULL');
        PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
END$$

DELIMITER ;

SET SQL_SAFE_UPDATES = 0;
CALL tmp_migracion_13('venta');
CALL tmp_migracion_13('cotizacion');
CALL tmp_migracion_13('compra');
CALL tmp_migracion_13('orden_compra_cliente');
SET SQL_SAFE_UPDATES = 1;
DROP PROCEDURE IF EXISTS tmp_migracion_13;

-- Revision: las cuatro columnas deben decir VIRTUAL o STORED GENERATED en EXTRA
SELECT TABLE_NAME, COLUMN_NAME, EXTRA, GENERATION_EXPRESSION
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'anulado'
  AND TABLE_NAME IN ('venta', 'cotizacion', 'compra', 'orden_compra_cliente');
