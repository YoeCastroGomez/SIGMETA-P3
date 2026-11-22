-- RF013: anulacion de notas de credito como reversion rastreable en Kardex.
-- Ejecutar sobre copia de pruebas despues de respaldo, antes de desplegar Java actualizado.
-- No altera movimientos historicos ni elimina filas.
USE sigmeta;

DELIMITER $$
DROP PROCEDURE IF EXISTS tmp_rf013_tipo$$
CREATE PROCEDURE tmp_rf013_tipo()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'movimiento_inventario'
          AND COLUMN_NAME = 'tipo'
          AND COLUMN_TYPE LIKE '%SALIDA_REVERSION_DEVOLUCION%'
    ) THEN
        ALTER TABLE movimiento_inventario
          MODIFY COLUMN tipo ENUM(
            'INGRESO_COMPRA','SALIDA_DESPACHO','INGRESO_PRODUCCION',
            'SALIDA_PRODUCCION','INGRESO_DEVOLUCION','SALIDA_REVERSION_DEVOLUCION',
            'INGRESO_REVERSION_DESPACHO','SALIDA_REVERSION_COMPRA',
            'REVERSION_AJUSTE_INGRESO','REVERSION_AJUSTE_SALIDA',
            'AJUSTE_INGRESO','AJUSTE_SALIDA'
          ) NOT NULL;
    END IF;
END$$
DELIMITER ;
CALL tmp_rf013_tipo();
DROP PROCEDURE IF EXISTS tmp_rf013_tipo;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_movimiento_listar_comprobante$$
CREATE PROCEDURE sp_movimiento_listar_comprobante(IN p_comprobante INT)
BEGIN
    SELECT * FROM movimiento_inventario
    WHERE id_comprobante = p_comprobante
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_reversion_nota_credito$$
CREATE PROCEDURE sp_movimiento_reversion_nota_credito(
    IN p_producto INT, IN p_tipo VARCHAR(40),
    IN p_cantidad DECIMAL(12,3), IN p_stock DECIMAL(12,3),
    IN p_comprobante INT, IN p_usuario INT, IN p_motivo VARCHAR(255),
    IN p_original INT, OUT p_id INT
)
BEGIN
    IF p_tipo <> 'SALIDA_REVERSION_DEVOLUCION'
       OR p_cantidad <= 0 OR p_stock < 0
       OR NOT EXISTS (
          SELECT 1 FROM movimiento_inventario
          WHERE id = p_original AND id_producto = p_producto
            AND id_comprobante = p_comprobante AND tipo = 'INGRESO_DEVOLUCION'
       ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La reversion no coincide con el ingreso de devolucion';
    END IF;

    INSERT INTO movimiento_inventario (
        id_producto, tipo, fecha_movimiento, cantidad, stock_resultante,
        id_comprobante, id_usuario_registro, motivo, id_movimiento_revertido
    ) VALUES (
        p_producto, p_tipo, NOW(), p_cantidad, p_stock,
        p_comprobante, p_usuario, p_motivo, p_original
    );
    SET p_id = LAST_INSERT_ID();
END$$
DELIMITER ;
