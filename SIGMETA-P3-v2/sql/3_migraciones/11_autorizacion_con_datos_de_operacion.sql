-- =========================================================================
-- MIGRACION 11 - RF014 OPCION B (Sebastian): LA AUTORIZACION LLEVA LA OPERACION
-- Ejecutar UNA vez en la base que usa GlassFish (local o AWS), en MySQL Workbench (rayo).
-- Agrega referencia, detalle y huella a solicitud_autorizacion y crea 2 procedimientos.
-- No borra ni cambia datos. Se puede volver a ejecutar sin problema.
-- (Recargar 2_procedimientos/04_SP_sebastian.sql hace lo mismo.)
-- =========================================================================
USE sigmeta;

-- ============================================================
-- RF014 (opcion B): la solicitud de autorizacion lleva LA OPERACION que se quiere hacer.
--   referencia: el documento o producto ("F001-00000049", "Venta nueva - Cliente", "INT-0026")
--   detalle:    lo que el Administrador ve antes de decidir (productos, cantidades, montos)
--   huella:     SHA-256 de los datos de la operacion. La operacion solo se ejecuta con la autorizacion
--               aprobada de ESA huella: si cambian los datos, hay que volver a pedirla.
-- Agrega las columnas si faltan (base ya instalada); una base nueva ya las trae desde 1_base.
-- Se puede ejecutar mas de una vez.
-- ============================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS tmp_solicitud_datos_operacion$$
CREATE PROCEDURE tmp_solicitud_datos_operacion()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'solicitud_autorizacion'
                     AND COLUMN_NAME = 'huella') THEN
        ALTER TABLE solicitud_autorizacion
            ADD COLUMN referencia VARCHAR(80) NULL,
            ADD COLUMN detalle VARCHAR(1000) NULL,
            ADD COLUMN huella CHAR(64) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'solicitud_autorizacion'
                     AND INDEX_NAME = 'idx_solicitud_huella') THEN
        CREATE INDEX idx_solicitud_huella ON solicitud_autorizacion (id_solicitante, operacion_restringida, huella);
    END IF;
END$$

DELIMITER ;

CALL tmp_solicitud_datos_operacion();
DROP PROCEDURE IF EXISTS tmp_solicitud_datos_operacion;

DELIMITER $$

-- RF014: guarda los datos de la operacion en la solicitud recien registrada (sp_solicitud_autorizacion_insertar)
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_datos_operacion$$
CREATE PROCEDURE sp_solicitud_autorizacion_datos_operacion(
    IN p_id_solicitud_autorizacion INT,
    IN p_referencia VARCHAR(80),
    IN p_detalle VARCHAR(1000),
    IN p_huella CHAR(64)
)
BEGIN
    UPDATE solicitud_autorizacion
    SET referencia = p_referencia, detalle = p_detalle, huella = p_huella
    WHERE id = p_id_solicitud_autorizacion;
END$$

-- RF014: la solicitud de UNA operacion concreta (su huella) en un estado.
-- PENDIENTE: la que espera al Administrador. APROBADA: solo si todavia no vencio.
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_buscar_por_huella$$
CREATE PROCEDURE sp_solicitud_autorizacion_buscar_por_huella(
    IN p_id_solicitante INT,
    IN p_operacion_restringida VARCHAR(60),
    IN p_huella CHAR(64),
    IN p_estado ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA', 'VENCIDA')
)
BEGIN
    SELECT * FROM solicitud_autorizacion
    WHERE id_solicitante = p_id_solicitante
      AND operacion_restringida = p_operacion_restringida
      AND huella = p_huella
      AND estado = p_estado
      AND (p_estado <> 'APROBADA' OR fecha_vencimiento > NOW())
    ORDER BY id DESC
    LIMIT 1;
END$$

DELIMITER ;

SELECT ROUTINE_NAME AS procedimiento_creado FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_NAME LIKE 'sp_solicitud_autorizacion_%'
ORDER BY ROUTINE_NAME;
