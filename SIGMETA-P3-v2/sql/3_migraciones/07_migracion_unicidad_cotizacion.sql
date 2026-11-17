USE sigmeta;
SET NAMES utf8mb4;

-- =========================================================================
-- MIGRACION 07 - NUMERO DE COTIZACION UNICO (H09, Claudia)
-- El numero COT-AAAA-NNNN lo genera CotizacionBOImpl (mayor numero del anio + 1) y no lo escribe el
-- usuario. Esta migracion agrega UNIQUE(numero) en cotizacion para que la base tampoco acepte un
-- numero repetido, igual que ya ocurre con comprobantes y ordenes de compra de clientes.
--
-- Es re-ejecutable: si la restriccion ya existe no hace nada. Si encuentra numeros repetidos NO agrega
-- la restriccion y muestra cuales son (corregirlos y volver a ejecutar este archivo).
-- Una base nueva ya la trae desde 1_base/sigmeta.sql.
-- =========================================================================

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
