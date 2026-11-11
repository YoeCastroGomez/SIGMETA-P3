USE sigmeta;
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_transaccion_bloquear$$
CREATE PROCEDURE sp_transaccion_bloquear()
BEGIN
    DECLARE v_id INT DEFAULT NULL;
    SELECT id INTO v_id FROM transaccion_control WHERE id = 1 FOR UPDATE;
    IF v_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Falta la fila de control transaccional';
    END IF;
END$$

DROP PROCEDURE IF EXISTS sp_correlativo_siguiente$$
CREATE PROCEDURE sp_correlativo_siguiente(IN p_tipo VARCHAR(20), IN p_anio INT, OUT p_numero BIGINT)
BEGIN
    INSERT INTO correlativo_documento(tipo, anio, ultimo) VALUES(p_tipo, p_anio, 0)
    ON DUPLICATE KEY UPDATE ultimo = ultimo;
    SELECT ultimo INTO p_numero FROM correlativo_documento
    WHERE tipo = p_tipo AND anio = p_anio FOR UPDATE;
    SET p_numero = p_numero + 1;
    UPDATE correlativo_documento SET ultimo = p_numero WHERE tipo = p_tipo AND anio = p_anio;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_orden_comprometida$$
CREATE PROCEDURE sp_detalle_orden_comprometida(IN p_linea INT, IN p_excluir INT)
BEGIN
    SELECT COALESCE(SUM(d.cantidad), 0) AS cantidad
    FROM detalle_venta d INNER JOIN venta v ON v.id = d.id_venta
    WHERE d.id_detalle_orden_compra_cliente = p_linea
      AND v.anulado = FALSE AND v.id <> p_excluir;
END$$

DROP PROCEDURE IF EXISTS sp_stock_obtener_para_actualizar$$
CREATE PROCEDURE sp_stock_obtener_para_actualizar(IN p_id INT)
BEGIN
SELECT stock_actual FROM producto WHERE id = p_id FOR UPDATE;
END$$

DROP PROCEDURE IF EXISTS sp_stock_actualizar$$
CREATE PROCEDURE sp_stock_actualizar(IN p_stock DECIMAL(12,3), IN p_id INT)
BEGIN
IF p_stock < 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock negativo'; END IF;
UPDATE producto SET stock_actual = p_stock WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_stock_listar_minimos$$
CREATE PROCEDURE sp_stock_listar_minimos()
BEGIN
SELECT id, codigo_interno, nombre, stock_actual, stock_minimo FROM producto WHERE estado = TRUE AND stock_actual <= stock_minimo ORDER BY stock_actual - stock_minimo, id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_recepcion_listar_por_recepcion$$
CREATE PROCEDURE sp_detalle_recepcion_listar_por_recepcion(IN p_id INT)
BEGIN
SELECT * FROM detalle_recepcion_compra WHERE id_recepcion_compra = p_id ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_recepcion_marcar_anulada$$
CREATE PROCEDURE sp_recepcion_marcar_anulada(IN p_id INT)
BEGIN
UPDATE recepcion_compra SET anulada = TRUE WHERE id = p_id AND anulada = FALSE;
END$$

DROP PROCEDURE IF EXISTS sp_compra_bloquear$$
CREATE PROCEDURE sp_compra_bloquear(IN p_id INT)
BEGIN
SELECT id FROM compra WHERE id = p_id FOR UPDATE;
END$$

DROP PROCEDURE IF EXISTS sp_compra_buscar_proveedor_fechas$$
CREATE PROCEDURE sp_compra_buscar_proveedor_fechas(IN p_proveedor INT, IN p_desde DATE, IN p_hasta DATE)
BEGIN
SELECT * FROM compra WHERE id_proveedor = p_proveedor AND fecha_emision BETWEEN p_desde AND p_hasta ORDER BY fecha_emision DESC, id DESC;
END$$

DROP PROCEDURE IF EXISTS sp_compra_estado_reversion$$
CREATE PROCEDURE sp_compra_estado_reversion(IN p_estado VARCHAR(30), IN p_id INT)
BEGIN
UPDATE compra SET estado = p_estado WHERE id = p_id AND anulado = FALSE;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_listar_producto$$
CREATE PROCEDURE sp_movimiento_listar_producto(IN p_id INT)
BEGIN
SELECT * FROM movimiento_inventario WHERE id_producto = p_id ORDER BY fecha_movimiento, id;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_bloquear$$
CREATE PROCEDURE sp_movimiento_bloquear(IN p_id INT)
BEGIN
SELECT * FROM movimiento_inventario WHERE id = p_id FOR UPDATE;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_existe_reversion$$
CREATE PROCEDURE sp_movimiento_existe_reversion(IN p_id INT)
BEGIN
SELECT id FROM movimiento_inventario WHERE id_movimiento_revertido = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_listar_recepcion$$
CREATE PROCEDURE sp_movimiento_listar_recepcion(IN p_id INT)
BEGIN
SELECT * FROM movimiento_inventario WHERE id_recepcion_compra = p_id ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_compra_descontar$$
CREATE PROCEDURE sp_detalle_compra_descontar(IN p_cantidad DECIMAL(12,3), IN p_id INT, IN p_minimo DECIMAL(12,3))
BEGIN
UPDATE detalle_compra SET cantidad_recibida = cantidad_recibida - p_cantidad WHERE id = p_id AND cantidad_recibida >= p_minimo;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_reversion_despacho$$
CREATE PROCEDURE sp_movimiento_reversion_despacho(IN p_producto INT, IN p_tipo VARCHAR(40), IN p_cantidad DECIMAL(12,3), IN p_stock DECIMAL(12,3), IN p_documento INT, IN p_usuario INT, IN p_motivo VARCHAR(255), IN p_original INT, OUT p_id INT)
BEGIN
INSERT INTO movimiento_inventario(id_producto, tipo, fecha_movimiento, cantidad, stock_resultante, id_despacho, id_usuario_registro, motivo, id_movimiento_revertido)
VALUES(p_producto, p_tipo, NOW(), p_cantidad, p_stock, p_documento, p_usuario, p_motivo, p_original);
SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_compensacion$$
CREATE PROCEDURE sp_movimiento_compensacion(IN p_producto INT, IN p_tipo VARCHAR(40), IN p_cantidad DECIMAL(12,3), IN p_stock DECIMAL(12,3), IN p_documento INT, IN p_usuario INT, IN p_motivo VARCHAR(255), IN p_original INT, OUT p_id INT)
BEGIN
INSERT INTO movimiento_inventario(id_producto, tipo, fecha_movimiento, cantidad, stock_resultante, id_recepcion_compra, id_usuario_registro, motivo, id_movimiento_revertido)
VALUES(p_producto, p_tipo, NOW(), p_cantidad, p_stock, p_documento, p_usuario, p_motivo, p_original);
SET p_id = LAST_INSERT_ID();
END$$

DELIMITER ;

DELIMITER $$
DROP PROCEDURE IF EXISTS sp_venta_listar_por_cliente$$
CREATE PROCEDURE sp_venta_listar_por_cliente(IN p_id_cliente INT)
BEGIN
    SELECT * FROM venta WHERE id_cliente = p_id_cliente ORDER BY fecha_emision DESC, id DESC;
END$$
DELIMITER ;
