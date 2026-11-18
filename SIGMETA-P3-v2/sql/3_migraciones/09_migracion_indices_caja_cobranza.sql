USE sigmeta;

-- =========================================================================
-- MIGRACION - ADRIANO (H10, RNF003: CAJA, COBRANZA Y REPORTES)
-- Indices para que las busquedas nuevas (sp_movimiento_caja_buscar_por_documento,
-- sp_cobro_buscar_por_fechas, sp_cuenta_por_cobrar_buscar_por_fechas,
-- sp_cierre_caja_buscar_por_fechas y los reportes de RF015) no recorran toda la tabla.
-- Solo agrega indices: no cambia datos. Se puede ejecutar mas de una vez
-- (si el indice ya existe no hace nada).
-- sigmeta.sql ya los trae; esto es solo para una base que ya existe (por ejemplo la de AWS).
-- Despues de esta migracion, volver a cargar 2_procedimientos (00 a 07).
-- =========================================================================

DROP PROCEDURE IF EXISTS tmp_crear_indice;
DELIMITER $$
CREATE PROCEDURE tmp_crear_indice(IN p_tabla VARCHAR(64), IN p_indice VARCHAR(64), IN p_columnas VARCHAR(200))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = p_tabla AND index_name = p_indice) THEN
        SET @sql = CONCAT('CREATE INDEX ', p_indice, ' ON ', p_tabla, ' (', p_columnas, ')');
        PREPARE st FROM @sql;
        EXECUTE st;
        DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;

CALL tmp_crear_indice('movimiento_caja', 'idx_movimiento_caja_documento', 'documento_origen, id_documento_origen');
CALL tmp_crear_indice('cobro', 'idx_cobro_fecha', 'fecha_cobro');
CALL tmp_crear_indice('cuenta_por_cobrar', 'idx_cxc_fecha_emision', 'fecha_emision');
CALL tmp_crear_indice('cuenta_por_cobrar', 'idx_cxc_fecha_vencimiento', 'fecha_vencimiento');
CALL tmp_crear_indice('cierre_caja', 'idx_cierre_caja_fecha', 'fecha_cierre');
CALL tmp_crear_indice('venta', 'idx_venta_fecha_emision', 'fecha_emision');
CALL tmp_crear_indice('compra', 'idx_compra_fecha_emision', 'fecha_emision');
CALL tmp_crear_indice('movimiento_inventario', 'idx_movimiento_inventario_producto_fecha', 'id_producto, fecha_movimiento');

DROP PROCEDURE IF EXISTS tmp_crear_indice;
