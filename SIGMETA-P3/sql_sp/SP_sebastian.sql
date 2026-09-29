USE sigmeta;

-- ============================================================
-- PROCEDIMIENTOS ALMACENADOS: ESTUDIANTE 4 (SEBASTIAN)
-- MODULO COMERCIAL / VENTA
-- Tablas:
-- 1. orden_compra_cliente
-- 2. detalle_orden_compra_cliente
-- 3. venta
-- 4. detalle_venta
-- 5. comprobante
-- 6. detalle_nota_credito
-- 7. despacho
-- 8. detalle_despacho
-- ============================================================

DELIMITER $$

-- ============================================================
-- 1. ORDEN DE COMPRA CLIENTE
-- ============================================================

DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_insertar$$
CREATE PROCEDURE sp_orden_compra_cliente_insertar(
    IN p_id_cliente INT,
    IN p_id_cotizacion INT,
    IN p_numero_orden_cliente VARCHAR(30),
    IN p_estado VARCHAR(30),
    IN p_numero VARCHAR(20),
    IN p_fecha_emision DATE,
    IN p_moneda VARCHAR(10),
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(300),
    IN p_id_usuario_registro INT,
    OUT p_id INT
)
BEGIN
    INSERT INTO orden_compra_cliente (
        id_cliente, id_cotizacion, numero_orden_cliente,
        estado, numero, fecha_emision, moneda,
        sub_total, igv, total, observaciones,
        fecha_registro, id_usuario_registro, anulado
    ) VALUES (
        p_id_cliente, p_id_cotizacion, p_numero_orden_cliente,
        p_estado, p_numero, p_fecha_emision, p_moneda,
        p_sub_total, p_igv, p_total, p_observaciones,
        NOW(), p_id_usuario_registro, FALSE
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_modificar$$
CREATE PROCEDURE sp_orden_compra_cliente_modificar(
    IN p_id INT,
    IN p_id_cliente INT,
    IN p_id_cotizacion INT,
    IN p_numero_orden_cliente VARCHAR(30),
    IN p_estado VARCHAR(30),
    IN p_numero VARCHAR(20),
    IN p_fecha_emision DATE,
    IN p_moneda VARCHAR(10),
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(300)
)
BEGIN
    UPDATE orden_compra_cliente
    SET id_cliente = p_id_cliente,
        id_cotizacion = p_id_cotizacion,
        numero_orden_cliente = p_numero_orden_cliente,
        estado = p_estado,
        numero = p_numero,
        fecha_emision = p_fecha_emision,
        moneda = p_moneda,
        sub_total = p_sub_total,
        igv = p_igv,
        total = p_total,
        observaciones = p_observaciones
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_eliminar$$
CREATE PROCEDURE sp_orden_compra_cliente_eliminar(
    IN p_id INT,
    IN p_motivo VARCHAR(200)
)
BEGIN
    UPDATE orden_compra_cliente
    SET anulado = TRUE,
        estado = 'ANULADA',
        motivo_anulacion = p_motivo,
        fecha_anulacion = NOW()
    WHERE id = p_id AND anulado = FALSE;
END$$

DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_obtener$$
CREATE PROCEDURE sp_orden_compra_cliente_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM orden_compra_cliente
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_listar$$
CREATE PROCEDURE sp_orden_compra_cliente_listar()
BEGIN
    SELECT *
    FROM orden_compra_cliente
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_actualizar_estado$$
CREATE PROCEDURE sp_orden_compra_cliente_actualizar_estado(
    IN p_id INT,
    IN p_estado VARCHAR(30)
)
BEGIN
    UPDATE orden_compra_cliente
    SET estado = p_estado
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_buscar_por_cotizacion$$
CREATE PROCEDURE sp_orden_compra_cliente_buscar_por_cotizacion(
    IN p_id_cotizacion INT
)
BEGIN
    SELECT *
    FROM orden_compra_cliente
    WHERE id_cotizacion = p_id_cotizacion
      AND anulado = FALSE
    LIMIT 1;
END$$


-- ============================================================
-- 2. DETALLE ORDEN DE COMPRA CLIENTE
-- ============================================================

DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_insertar$$
CREATE PROCEDURE sp_detalle_orden_compra_cliente_insertar(
    IN p_id_orden_compra_cliente INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2),
    IN p_cantidad_atendida DECIMAL(12,3),
    OUT p_id INT
)
BEGIN
    INSERT INTO detalle_orden_compra_cliente (
        id_orden_compra_cliente, numero_linea, id_producto,
        cantidad, precio_unitario, descuento, importe, cantidad_atendida
    ) VALUES (
        p_id_orden_compra_cliente, p_numero_linea, p_id_producto,
        p_cantidad, p_precio_unitario, p_descuento, p_importe, p_cantidad_atendida
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_modificar$$
CREATE PROCEDURE sp_detalle_orden_compra_cliente_modificar(
    IN p_id INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2),
    IN p_cantidad_atendida DECIMAL(12,3)
)
BEGIN
    UPDATE detalle_orden_compra_cliente
    SET numero_linea = p_numero_linea,
        id_producto = p_id_producto,
        cantidad = p_cantidad,
        precio_unitario = p_precio_unitario,
        descuento = p_descuento,
        importe = p_importe,
        cantidad_atendida = p_cantidad_atendida
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_eliminar$$
CREATE PROCEDURE sp_detalle_orden_compra_cliente_eliminar(
    IN p_id INT
)
BEGIN
    DELETE FROM detalle_orden_compra_cliente
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_obtener$$
CREATE PROCEDURE sp_detalle_orden_compra_cliente_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM detalle_orden_compra_cliente
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_listar$$
CREATE PROCEDURE sp_detalle_orden_compra_cliente_listar()
BEGIN
    SELECT *
    FROM detalle_orden_compra_cliente
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_listar_por_orden$$
CREATE PROCEDURE sp_detalle_orden_compra_cliente_listar_por_orden(
    IN p_id_orden INT
)
BEGIN
    SELECT *
    FROM detalle_orden_compra_cliente
    WHERE id_orden_compra_cliente = p_id_orden
    ORDER BY numero_linea;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_actualizar_atendida$$
CREATE PROCEDURE sp_detalle_orden_compra_cliente_actualizar_atendida(
    IN p_id INT,
    IN p_cantidad_atendida DECIMAL(12,3)
)
BEGIN
    UPDATE detalle_orden_compra_cliente
    SET cantidad_atendida = p_cantidad_atendida
    WHERE id = p_id;
END$$


-- ============================================================
-- 3. VENTA
-- ============================================================

DROP PROCEDURE IF EXISTS sp_venta_insertar$$
CREATE PROCEDURE sp_venta_insertar(
    IN p_id_cliente INT,
    IN p_id_orden_compra_cliente INT,
    IN p_condicion_pago VARCHAR(20),
    IN p_plazo_credito_dias INT,
    IN p_estado VARCHAR(30),
    IN p_numero VARCHAR(20),
    IN p_fecha_emision DATE,
    IN p_moneda VARCHAR(10),
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(300),
    IN p_id_usuario_registro INT,
    OUT p_id INT
)
BEGIN
    INSERT INTO venta (
        id_cliente, id_orden_compra_cliente, condicion_pago,
        plazo_credito_dias, estado, numero, fecha_emision,
        moneda, sub_total, igv, total, observaciones,
        fecha_registro, id_usuario_registro, anulado
    ) VALUES (
        p_id_cliente, p_id_orden_compra_cliente, p_condicion_pago,
        p_plazo_credito_dias, p_estado, p_numero, p_fecha_emision,
        p_moneda, p_sub_total, p_igv, p_total, p_observaciones,
        NOW(), p_id_usuario_registro, FALSE
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_venta_modificar$$
CREATE PROCEDURE sp_venta_modificar(
    IN p_id INT,
    IN p_id_cliente INT,
    IN p_id_orden_compra_cliente INT,
    IN p_condicion_pago VARCHAR(20),
    IN p_plazo_credito_dias INT,
    IN p_estado VARCHAR(30),
    IN p_numero VARCHAR(20),
    IN p_fecha_emision DATE,
    IN p_moneda VARCHAR(10),
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(300)
)
BEGIN
    UPDATE venta
    SET id_cliente = p_id_cliente,
        id_orden_compra_cliente = p_id_orden_compra_cliente,
        condicion_pago = p_condicion_pago,
        plazo_credito_dias = p_plazo_credito_dias,
        estado = p_estado,
        numero = p_numero,
        fecha_emision = p_fecha_emision,
        moneda = p_moneda,
        sub_total = p_sub_total,
        igv = p_igv,
        total = p_total,
        observaciones = p_observaciones
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_venta_eliminar$$
CREATE PROCEDURE sp_venta_eliminar(
    IN p_id INT,
    IN p_motivo VARCHAR(200)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM despacho
        WHERE id_venta = p_id AND anulado = FALSE
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede anular una venta con despachos vigentes';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM comprobante
        WHERE id_venta = p_id AND estado != 'ANULADO'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede anular una venta con comprobantes emitidos vigentes';
    END IF;

    UPDATE venta
    SET anulado = TRUE,
        estado = 'ANULADA',
        motivo_anulacion = p_motivo,
        fecha_anulacion = NOW()
    WHERE id = p_id AND anulado = FALSE;
END$$

DROP PROCEDURE IF EXISTS sp_venta_obtener$$
CREATE PROCEDURE sp_venta_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM venta
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_venta_listar$$
CREATE PROCEDURE sp_venta_listar()
BEGIN
    SELECT *
    FROM venta
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_venta_bloquear$$
CREATE PROCEDURE sp_venta_bloquear(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM venta
    WHERE id = p_id
    FOR UPDATE;
END$$

DROP PROCEDURE IF EXISTS sp_venta_actualizar_estado$$
CREATE PROCEDURE sp_venta_actualizar_estado(
    IN p_id INT,
    IN p_estado VARCHAR(30)
)
BEGIN
    UPDATE venta
    SET estado = p_estado
    WHERE id = p_id;
END$$

-- Procedimientos auxiliares para cuentas por cobrar (Est. 5)
DROP PROCEDURE IF EXISTS sp_venta_saldo_pendiente_cliente$$
CREATE PROCEDURE sp_venta_saldo_pendiente_cliente(
    IN p_id_cliente INT,
    OUT p_saldo_pendiente DECIMAL(12,2)
)
BEGIN
    -- Si existe la tabla cuenta_por_cobrar, suma el saldo pendiente
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = DATABASE() AND table_name = 'cuenta_por_cobrar'
    ) THEN
        SELECT COALESCE(SUM(saldo_pendiente), 0.00)
        INTO p_saldo_pendiente
        FROM cuenta_por_cobrar
        WHERE id_cliente = p_id_cliente
          AND estado IN ('PENDIENTE', 'PAGADA_PARCIAL');
    ELSE
        SET p_saldo_pendiente = 0.00;
    END IF;
END$$

DROP PROCEDURE IF EXISTS sp_venta_generar_cuenta_por_cobrar$$
CREATE PROCEDURE sp_venta_generar_cuenta_por_cobrar(
    IN p_id_venta INT,
    IN p_id_cliente INT,
    IN p_fecha_emision DATE,
    IN p_fecha_vencimiento DATE,
    IN p_moneda VARCHAR(10),
    IN p_monto_original DECIMAL(12,2)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = DATABASE() AND table_name = 'cuenta_por_cobrar'
    ) THEN
        INSERT INTO cuenta_por_cobrar (
            id_venta, id_cliente, fecha_emision, fecha_vencimiento,
            moneda, monto_original, monto_pagado, saldo_pendiente, estado
        ) VALUES (
            p_id_venta, p_id_cliente, p_fecha_emision, p_fecha_vencimiento,
            p_moneda, p_monto_original, 0.00, p_monto_original, 'PENDIENTE'
        );
    END IF;
END$$


-- ============================================================
-- 4. DETALLE VENTA
-- ============================================================

DROP PROCEDURE IF EXISTS sp_detalle_venta_insertar$$
CREATE PROCEDURE sp_detalle_venta_insertar(
    IN p_id_venta INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2),
    IN p_cantidad_despachada DECIMAL(12,3),
    OUT p_id INT
)
BEGIN
    INSERT INTO detalle_venta (
        id_venta, numero_linea, id_producto,
        cantidad, precio_unitario, descuento, importe, cantidad_despachada
    ) VALUES (
        p_id_venta, p_numero_linea, p_id_producto,
        p_cantidad, p_precio_unitario, p_descuento, p_importe, p_cantidad_despachada
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_detalle_venta_modificar$$
CREATE PROCEDURE sp_detalle_venta_modificar(
    IN p_id INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2),
    IN p_cantidad_despachada DECIMAL(12,3)
)
BEGIN
    UPDATE detalle_venta
    SET numero_linea = p_numero_linea,
        id_producto = p_id_producto,
        cantidad = p_cantidad,
        precio_unitario = p_precio_unitario,
        descuento = p_descuento,
        importe = p_importe,
        cantidad_despachada = p_cantidad_despachada
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_venta_eliminar$$
CREATE PROCEDURE sp_detalle_venta_eliminar(
    IN p_id INT
)
BEGIN
    DELETE FROM detalle_venta
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_venta_obtener$$
CREATE PROCEDURE sp_detalle_venta_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM detalle_venta
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_venta_listar$$
CREATE PROCEDURE sp_detalle_venta_listar()
BEGIN
    SELECT *
    FROM detalle_venta
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_venta_listar_por_venta$$
CREATE PROCEDURE sp_detalle_venta_listar_por_venta(
    IN p_id_venta INT
)
BEGIN
    SELECT *
    FROM detalle_venta
    WHERE id_venta = p_id_venta
    ORDER BY numero_linea;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_venta_actualizar_despachado$$
CREATE PROCEDURE sp_detalle_venta_actualizar_despachado(
    IN p_id INT,
    IN p_cantidad_despachada DECIMAL(12,3)
)
BEGIN
    UPDATE detalle_venta
    SET cantidad_despachada = p_cantidad_despachada
    WHERE id = p_id;
END$$


-- ============================================================
-- 5. COMPROBANTE
-- ============================================================

DROP PROCEDURE IF EXISTS sp_comprobante_insertar$$
CREATE PROCEDURE sp_comprobante_insertar(
    IN p_id_venta INT,
    IN p_tipo VARCHAR(20),
    IN p_serie VARCHAR(10),
    IN p_numero VARCHAR(20),
    IN p_fecha_emision DATE,
    IN p_moneda VARCHAR(10),
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_estado VARCHAR(20),
    IN p_id_comprobante_relacionado INT,
    IN p_motivo VARCHAR(200),
    IN p_medio_envio VARCHAR(30),
    IN p_fecha_envio DATETIME,
    OUT p_id INT
)
BEGIN
    INSERT INTO comprobante (
        id_venta, tipo, serie, numero, fecha_emision,
        moneda, sub_total, igv, total, estado,
        id_comprobante_relacionado, motivo, medio_envio,
        fecha_envio, fecha_registro
    ) VALUES (
        p_id_venta, p_tipo, p_serie, p_numero, p_fecha_emision,
        p_moneda, p_sub_total, p_igv, p_total, p_estado,
        p_id_comprobante_relacionado, p_motivo, p_medio_envio,
        p_fecha_envio, NOW()
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_modificar$$
CREATE PROCEDURE sp_comprobante_modificar(
    IN p_id INT,
    IN p_motivo VARCHAR(200),
    IN p_medio_envio VARCHAR(30),
    IN p_fecha_envio DATETIME
)
BEGIN
    UPDATE comprobante
    SET motivo = p_motivo,
        medio_envio = p_medio_envio,
        fecha_envio = p_fecha_envio
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_eliminar$$
CREATE PROCEDURE sp_comprobante_eliminar(
    IN p_id INT
)
BEGIN
    UPDATE comprobante
    SET estado = 'ANULADO'
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_obtener$$
CREATE PROCEDURE sp_comprobante_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM comprobante
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_listar$$
CREATE PROCEDURE sp_comprobante_listar()
BEGIN
    SELECT *
    FROM comprobante
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_listar_por_venta$$
CREATE PROCEDURE sp_comprobante_listar_por_venta(
    IN p_id_venta INT
)
BEGIN
    SELECT *
    FROM comprobante
    WHERE id_venta = p_id_venta
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_listar_nc_por_relacionado$$
CREATE PROCEDURE sp_comprobante_listar_nc_por_relacionado(
    IN p_id_comprobante_relacionado INT
)
BEGIN
    SELECT *
    FROM comprobante
    WHERE id_comprobante_relacionado = p_id_comprobante_relacionado
      AND estado != 'ANULADO'
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_actualizar_estado$$
CREATE PROCEDURE sp_comprobante_actualizar_estado(
    IN p_id INT,
    IN p_estado VARCHAR(20)
)
BEGIN
    UPDATE comprobante
    SET estado = p_estado
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_siguiente_correlativo$$
CREATE PROCEDURE sp_comprobante_siguiente_correlativo(
    IN p_serie VARCHAR(10),
    OUT p_correlativo VARCHAR(20)
)
BEGIN
    DECLARE v_max INT DEFAULT 0;
    
    SELECT COALESCE(MAX(CAST(numero AS UNSIGNED)), 0)
    INTO v_max
    FROM comprobante
    WHERE serie = p_serie
    FOR UPDATE;

    SET p_correlativo = LPAD(v_max + 1, 8, '0');
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_revertir_saldo_cpc$$
CREATE PROCEDURE sp_comprobante_revertir_saldo_cpc(
    IN p_id_venta INT,
    IN p_monto_reversion DECIMAL(12,2)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = DATABASE() AND table_name = 'cuenta_por_cobrar'
    ) THEN
        UPDATE cuenta_por_cobrar
        SET saldo_pendiente = GREATEST(0.00, saldo_pendiente - p_monto_reversion),
            estado = CASE WHEN (saldo_pendiente - p_monto_reversion) <= 0.00 THEN 'PAGADA' ELSE estado END
        WHERE id_venta = p_id_venta;
    END IF;
END$$


-- ============================================================
-- 6. DETALLE NOTA CREDITO
-- ============================================================

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_insertar$$
CREATE PROCEDURE sp_detalle_nota_credito_insertar(
    IN p_id_comprobante INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2),
    OUT p_id INT
)
BEGIN
    INSERT INTO detalle_nota_credito (
        id_comprobante, numero_linea, id_producto,
        cantidad, precio_unitario, descuento, importe
    ) VALUES (
        p_id_comprobante, p_numero_linea, p_id_producto,
        p_cantidad, p_precio_unitario, p_descuento, p_importe
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_modificar$$
CREATE PROCEDURE sp_detalle_nota_credito_modificar(
    IN p_id INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2)
)
BEGIN
    UPDATE detalle_nota_credito
    SET numero_linea = p_numero_linea,
        id_producto = p_id_producto,
        cantidad = p_cantidad,
        precio_unitario = p_precio_unitario,
        descuento = p_descuento,
        importe = p_importe
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_eliminar$$
CREATE PROCEDURE sp_detalle_nota_credito_eliminar(
    IN p_id INT
)
BEGIN
    DELETE FROM detalle_nota_credito
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_obtener$$
CREATE PROCEDURE sp_detalle_nota_credito_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM detalle_nota_credito
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_listar$$
CREATE PROCEDURE sp_detalle_nota_credito_listar()
BEGIN
    SELECT *
    FROM detalle_nota_credito
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_listar_por_comprobante$$
CREATE PROCEDURE sp_detalle_nota_credito_listar_por_comprobante(
    IN p_id_comprobante INT
)
BEGIN
    SELECT *
    FROM detalle_nota_credito
    WHERE id_comprobante = p_id_comprobante
    ORDER BY numero_linea;
END$$


-- ============================================================
-- 7. DESPACHO
-- ============================================================

DROP PROCEDURE IF EXISTS sp_despacho_insertar$$
CREATE PROCEDURE sp_despacho_insertar(
    IN p_id_venta INT,
    IN p_serie_guia VARCHAR(10),
    IN p_numero_guia VARCHAR(20),
    IN p_fecha_despacho DATE,
    IN p_direccion_entrega VARCHAR(200),
    IN p_transportista VARCHAR(100),
    IN p_id_usuario_registro INT,
    OUT p_id INT
)
BEGIN
    INSERT INTO despacho (
        id_venta, serie_guia, numero_guia, fecha_despacho,
        direccion_entrega, transportista, anulado,
        id_usuario_registro, fecha_registro
    ) VALUES (
        p_id_venta, p_serie_guia, p_numero_guia, p_fecha_despacho,
        p_direccion_entrega, p_transportista, FALSE,
        p_id_usuario_registro, NOW()
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_despacho_modificar$$
CREATE PROCEDURE sp_despacho_modificar(
    IN p_id INT,
    IN p_direccion_entrega VARCHAR(200),
    IN p_transportista VARCHAR(100)
)
BEGIN
    UPDATE despacho
    SET direccion_entrega = p_direccion_entrega,
        transportista = p_transportista
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_despacho_eliminar$$
CREATE PROCEDURE sp_despacho_eliminar(
    IN p_id INT
)
BEGIN
    UPDATE despacho
    SET anulado = TRUE
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_despacho_obtener$$
CREATE PROCEDURE sp_despacho_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM despacho
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_despacho_listar$$
CREATE PROCEDURE sp_despacho_listar()
BEGIN
    SELECT *
    FROM despacho
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_despacho_listar_por_venta$$
CREATE PROCEDURE sp_despacho_listar_por_venta(
    IN p_id_venta INT
)
BEGIN
    SELECT *
    FROM despacho
    WHERE id_venta = p_id_venta
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_despacho_siguiente_correlativo$$
CREATE PROCEDURE sp_despacho_siguiente_correlativo(
    IN p_serie_guia VARCHAR(10),
    OUT p_correlativo VARCHAR(20)
)
BEGIN
    DECLARE v_max INT DEFAULT 0;
    
    SELECT COALESCE(MAX(CAST(numero_guia AS UNSIGNED)), 0)
    INTO v_max
    FROM despacho
    WHERE serie_guia = p_serie_guia
    FOR UPDATE;

    SET p_correlativo = LPAD(v_max + 1, 8, '0');
END$$


-- ============================================================
-- 8. DETALLE DESPACHO
-- ============================================================

DROP PROCEDURE IF EXISTS sp_detalle_despacho_insertar$$
CREATE PROCEDURE sp_detalle_despacho_insertar(
    IN p_id_despacho INT,
    IN p_id_producto INT,
    IN p_cantidad_despachada DECIMAL(12,3),
    OUT p_id INT
)
BEGIN
    INSERT INTO detalle_despacho (
        id_despacho, id_producto, cantidad_despachada
    ) VALUES (
        p_id_despacho, p_id_producto, p_cantidad_despachada
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_detalle_despacho_modificar$$
CREATE PROCEDURE sp_detalle_despacho_modificar(
    IN p_id INT,
    IN p_id_producto INT,
    IN p_cantidad_despachada DECIMAL(12,3)
)
BEGIN
    UPDATE detalle_despacho
    SET id_producto = p_id_producto,
        cantidad_despachada = p_cantidad_despachada
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_despacho_eliminar$$
CREATE PROCEDURE sp_detalle_despacho_eliminar(
    IN p_id INT
)
BEGIN
    DELETE FROM detalle_despacho
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_despacho_obtener$$
CREATE PROCEDURE sp_detalle_despacho_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM detalle_despacho
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_despacho_listar$$
CREATE PROCEDURE sp_detalle_despacho_listar()
BEGIN
    SELECT *
    FROM detalle_despacho
    ORDER BY id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_despacho_listar_por_despacho$$
CREATE PROCEDURE sp_detalle_despacho_listar_por_despacho(
    IN p_id_despacho INT
)
BEGIN
    SELECT *
    FROM detalle_despacho
    WHERE id_despacho = p_id_despacho
    ORDER BY id;
END$$

DELIMITER ;
