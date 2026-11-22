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
        estado, numero, fecha_emision,
        sub_total, igv, total, observaciones,
        fecha_registro, id_usuario_registro
    ) VALUES (
        p_id_cliente, p_id_cotizacion, p_numero_orden_cliente,
        p_estado, p_numero, p_fecha_emision,
        p_sub_total, p_igv, p_total, p_observaciones,
        NOW(), p_id_usuario_registro
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
    SET estado = 'ANULADA',
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
        sub_total, igv, total, observaciones,
        fecha_registro, id_usuario_registro
    ) VALUES (
        p_id_cliente, p_id_orden_compra_cliente, p_condicion_pago,
        p_plazo_credito_dias, p_estado, p_numero, p_fecha_emision,
        p_sub_total, p_igv, p_total, p_observaciones,
        NOW(), p_id_usuario_registro
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
    SET estado = 'ANULADA',
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
          AND saldo_pendiente > 0;
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
    IN p_monto_original DECIMAL(12,2)
)
BEGIN
    DECLARE v_id INT;
    CALL sp_cuenta_por_cobrar_insertar(v_id, p_id_venta, p_id_cliente,
        p_fecha_emision, p_fecha_vencimiento, p_monto_original, 0, p_monto_original, 'PENDIENTE');
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
    IN p_id_detalle_orden_compra_cliente INT,
    OUT p_id INT
)
BEGIN
    INSERT INTO detalle_venta (
        id_venta, numero_linea, id_producto,
        cantidad, precio_unitario, descuento, importe, cantidad_despachada,
        id_detalle_orden_compra_cliente
    ) VALUES (
        p_id_venta, p_numero_linea, p_id_producto,
        p_cantidad, p_precio_unitario, p_descuento, p_importe, p_cantidad_despachada,
        p_id_detalle_orden_compra_cliente
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
    IN p_cantidad_despachada DECIMAL(12,3),
    IN p_id_detalle_orden_compra_cliente INT
)
BEGIN
    UPDATE detalle_venta
    SET numero_linea = p_numero_linea,
        id_producto = p_id_producto,
        cantidad = p_cantidad,
        precio_unitario = p_precio_unitario,
        descuento = p_descuento,
        importe = p_importe,
        cantidad_despachada = p_cantidad_despachada,
        id_detalle_orden_compra_cliente = p_id_detalle_orden_compra_cliente
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
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_estado VARCHAR(20),
    IN p_id_comprobante_relacionado INT,
    IN p_motivo VARCHAR(200),
    IN p_medio_envio VARCHAR(30),
    IN p_fecha_envio DATETIME,
    IN p_id_administrador_autoriza INT,
    OUT p_id INT
)
BEGIN
    INSERT INTO comprobante (
        id_venta, tipo, serie, numero, fecha_emision,
        sub_total, igv, total, estado,
        id_comprobante_relacionado, motivo, medio_envio,
        fecha_envio, id_administrador_autoriza, fecha_registro
    ) VALUES (
        p_id_venta, p_tipo, p_serie, p_numero, p_fecha_emision,
        p_sub_total, p_igv, p_total, p_estado,
        p_id_comprobante_relacionado, p_motivo, p_medio_envio,
        p_fecha_envio, p_id_administrador_autoriza, NOW()
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

-- RF013: la nota de credito rebaja la deuda. monto_original (el total de la venta) NO cambia: lo acreditado
-- se acumula en monto_acreditado, sin pasar del saldo que quedaba. El saldo siempre es
-- monto_original - monto_acreditado - monto_pagado, el mismo criterio que usan los cobros (CobroBOImpl).
-- MySQL asigna de izquierda a derecha: saldo_pendiente y estado ya ven el monto_acreditado nuevo.
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
        SET monto_acreditado = monto_acreditado
                + LEAST(p_monto_reversion, GREATEST(monto_original - monto_acreditado - monto_pagado, 0)),
            saldo_pendiente = monto_original - monto_acreditado - monto_pagado,
            estado = CASE
                        WHEN saldo_pendiente <= 0.00 THEN 'PAGADA'
                        WHEN fecha_vencimiento < CURDATE() THEN 'VENCIDA'
                        WHEN monto_pagado > 0.00 THEN 'PAGADA_PARCIAL'
                        ELSE 'PENDIENTE'
                     END
        WHERE id_venta = p_id_venta;
    END IF;
END$$

-- RF008: foto de lo que se imprime en una factura o boleta, tomada al emitirla. El cliente (nombre, documento y
-- direccion) y cada linea (codigo, descripcion y unidad del producto, cantidad y montos) quedan copiados en el
-- comprobante: si despues se renombra el producto o el cliente cambia de direccion, la reimpresion sale igual.
-- Solo copia lo que falta (se puede llamar de nuevo sin duplicar). La usa ComprobanteBOImpl.emitir.
DROP PROCEDURE IF EXISTS sp_comprobante_congelar$$
CREATE PROCEDURE sp_comprobante_congelar(IN p_id INT)
BEGIN
    UPDATE comprobante c
    INNER JOIN venta v ON v.id = c.id_venta
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    SET c.cliente_nombre = CASE WHEN cl.tipo_persona = 'JURIDICA' THEN cl.razon_social
                                ELSE CONCAT(cl.nombres, ' ', cl.apellidos) END,
        c.cliente_tipo_documento = cl.tipo_documento,
        c.cliente_numero_documento = cl.numero_documento,
        c.cliente_direccion = cl.direccion
    WHERE c.id = p_id AND c.tipo IN ('FACTURA', 'BOLETA') AND c.cliente_nombre IS NULL;

    INSERT INTO detalle_comprobante (
        id_comprobante, numero_linea, codigo, descripcion, unidad,
        cantidad, precio_unitario, descuento, importe
    )
    SELECT c.id, d.numero_linea, p.codigo_interno, p.nombre, p.unidad_venta,
           d.cantidad, d.precio_unitario, d.descuento, d.importe
    FROM comprobante c
    INNER JOIN detalle_venta d ON d.id_venta = c.id_venta
    INNER JOIN producto p ON p.id = d.id_producto
    WHERE c.id = p_id AND c.tipo IN ('FACTURA', 'BOLETA')
      AND NOT EXISTS (SELECT 1 FROM detalle_comprobante x WHERE x.id_comprobante = c.id)
    ORDER BY d.numero_linea;
END$$

-- RF008: cliente tal como se imprimio (columnas NULL si el comprobante no tiene foto)
DROP PROCEDURE IF EXISTS sp_comprobante_impreso$$
CREATE PROCEDURE sp_comprobante_impreso(IN p_id INT)
BEGIN
    SELECT cliente_nombre, cliente_tipo_documento, cliente_numero_documento, cliente_direccion
    FROM comprobante
    WHERE id = p_id;
END$$

-- RF008: lineas tal como se imprimieron
DROP PROCEDURE IF EXISTS sp_comprobante_impreso_lineas$$
CREATE PROCEDURE sp_comprobante_impreso_lineas(IN p_id INT)
BEGIN
    SELECT numero_linea, codigo, descripcion, unidad, cantidad, precio_unitario, descuento, importe
    FROM detalle_comprobante
    WHERE id_comprobante = p_id
    ORDER BY numero_linea;
END$$

-- RF013: al anular una nota de credito la deuda vuelve a su monto anterior: se resta de lo acreditado
DROP PROCEDURE IF EXISTS sp_comprobante_restaurar_saldo_cpc$$
CREATE PROCEDURE sp_comprobante_restaurar_saldo_cpc(
    IN p_id_venta INT,
    IN p_monto DECIMAL(12,2)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'cuenta_por_cobrar'
    ) THEN
        UPDATE cuenta_por_cobrar
        SET monto_acreditado = GREATEST(monto_acreditado - p_monto, 0),
            saldo_pendiente = monto_original - monto_acreditado - monto_pagado,
            estado = CASE
                        WHEN saldo_pendiente <= 0.00 THEN 'PAGADA'
                        WHEN fecha_vencimiento < CURDATE() THEN 'VENCIDA'
                        WHEN monto_pagado > 0.00 THEN 'PAGADA_PARCIAL'
                        ELSE 'PENDIENTE'
                     END
        WHERE id_venta = p_id_venta;
    END IF;
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_registrar_envio$$
CREATE PROCEDURE sp_comprobante_registrar_envio(
    IN p_id INT,
    IN p_medio_envio VARCHAR(30),
    IN p_fecha_envio DATETIME
)
BEGIN
    UPDATE comprobante
    SET medio_envio = p_medio_envio,
        fecha_envio = p_fecha_envio
    WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_comprobante_buscar_nc$$
CREATE PROCEDURE sp_comprobante_buscar_nc(
    IN p_fecha_desde DATE,
    IN p_fecha_hasta DATE,
    IN p_id_cliente INT,
    IN p_serie_numero_origen VARCHAR(30)
)
BEGIN
    SELECT c.*
    FROM comprobante c
    JOIN venta v ON c.id_venta = v.id
    LEFT JOIN comprobante o ON o.id = c.id_comprobante_relacionado
    WHERE c.tipo = 'NOTA_CREDITO'
      AND (p_fecha_desde IS NULL OR c.fecha_emision >= p_fecha_desde)
      AND (p_fecha_hasta IS NULL OR c.fecha_emision <= p_fecha_hasta)
      AND (p_id_cliente IS NULL OR v.id_cliente = p_id_cliente)
      AND (p_serie_numero_origen IS NULL OR CONCAT(o.serie, '-', o.numero) LIKE CONCAT('%', p_serie_numero_origen, '%')
                                         OR o.numero LIKE CONCAT('%', p_serie_numero_origen, '%')
                                         OR o.serie LIKE CONCAT('%', p_serie_numero_origen, '%'))
    ORDER BY c.fecha_emision DESC, c.id DESC;
END$$


-- ============================================================
-- 6. DETALLE NOTA CREDITO
-- ============================================================

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_insertar$$
CREATE PROCEDURE sp_detalle_nota_credito_insertar(
    IN p_id_comprobante INT,
    IN p_numero_linea INT,
    IN p_id_detalle_venta INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2),
    OUT p_id INT
)
BEGIN
    INSERT INTO detalle_nota_credito (
        id_comprobante, numero_linea, id_detalle_venta,
        cantidad, precio_unitario, descuento, importe
    ) VALUES (
        p_id_comprobante, p_numero_linea, p_id_detalle_venta,
        p_cantidad, p_precio_unitario, p_descuento, p_importe
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_modificar$$
CREATE PROCEDURE sp_detalle_nota_credito_modificar(
    IN p_id INT,
    IN p_numero_linea INT,
    IN p_id_detalle_venta INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2)
)
BEGIN
    UPDATE detalle_nota_credito
    SET numero_linea = p_numero_linea,
        id_detalle_venta = p_id_detalle_venta,
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
    SELECT d.*, dv.id_producto
    FROM detalle_nota_credito d
    INNER JOIN detalle_venta dv ON dv.id = d.id_detalle_venta
    WHERE d.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_listar$$
CREATE PROCEDURE sp_detalle_nota_credito_listar()
BEGIN
    SELECT d.*, dv.id_producto
    FROM detalle_nota_credito d
    INNER JOIN detalle_venta dv ON dv.id = d.id_detalle_venta
    ORDER BY d.id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_listar_por_comprobante$$
CREATE PROCEDURE sp_detalle_nota_credito_listar_por_comprobante(
    IN p_id_comprobante INT
)
BEGIN
    SELECT d.*, dv.id_producto
    FROM detalle_nota_credito d
    INNER JOIN detalle_venta dv ON dv.id = d.id_detalle_venta
    WHERE d.id_comprobante = p_id_comprobante
    ORDER BY d.numero_linea;
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
    IN p_id_detalle_venta INT,
    IN p_cantidad_despachada DECIMAL(12,3),
    OUT p_id INT
)
BEGIN
    INSERT INTO detalle_despacho (
        id_despacho, id_detalle_venta, cantidad_despachada
    ) VALUES (
        p_id_despacho, p_id_detalle_venta, p_cantidad_despachada
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_detalle_despacho_modificar$$
CREATE PROCEDURE sp_detalle_despacho_modificar(
    IN p_id INT,
    IN p_id_detalle_venta INT,
    IN p_cantidad_despachada DECIMAL(12,3)
)
BEGIN
    UPDATE detalle_despacho
    SET id_detalle_venta = p_id_detalle_venta,
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
    SELECT d.*, dv.id_producto
    FROM detalle_despacho d
    INNER JOIN detalle_venta dv ON dv.id = d.id_detalle_venta
    WHERE d.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_despacho_listar$$
CREATE PROCEDURE sp_detalle_despacho_listar()
BEGIN
    SELECT d.*, dv.id_producto
    FROM detalle_despacho d
    INNER JOIN detalle_venta dv ON dv.id = d.id_detalle_venta
    ORDER BY d.id;
END$$

DROP PROCEDURE IF EXISTS sp_detalle_despacho_listar_por_despacho$$
CREATE PROCEDURE sp_detalle_despacho_listar_por_despacho(
    IN p_id_despacho INT
)
BEGIN
    SELECT d.*, dv.id_producto
    FROM detalle_despacho d
    INNER JOIN detalle_venta dv ON dv.id = d.id_detalle_venta
    WHERE d.id_despacho = p_id_despacho
    ORDER BY d.id;
END$$


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

-- ============================================================
-- RF014: la solicitud se puede RETOMAR y distingue USADA de VENCIDA.
--   estado USADA:    la autorizacion se consumio en la operacion que autorizaba (antes quedaba VENCIDA,
--                    igual que una que paso su vigencia o que el Administrador revoco).
--   datos_operacion: lo que el usuario preparo en la pantalla (JSON que manda Blazor). Sirve para volver a
--                    abrir la operacion con la solicitud en curso (pendiente, aprobada o rechazada) sin pedirla
--                    otra vez. NULL = descartada (ya no se muestra para continuar). No reemplaza a la huella:
--                    la operacion se sigue validando con sus datos reales al registrarla.
-- Agrega lo que falta en una base ya instalada; una base nueva ya lo trae desde 1_base.
-- Se puede ejecutar mas de una vez.
-- ============================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS tmp_solicitud_retomar$$
CREATE PROCEDURE tmp_solicitud_retomar()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'solicitud_autorizacion'
                     AND COLUMN_NAME = 'estado' AND COLUMN_TYPE LIKE '%USADA%') THEN
        ALTER TABLE solicitud_autorizacion
            MODIFY COLUMN estado ENUM('PENDIENTE','APROBADA','RECHAZADA','VENCIDA','USADA') NOT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'solicitud_autorizacion'
                     AND COLUMN_NAME = 'datos_operacion') THEN
        ALTER TABLE solicitud_autorizacion ADD COLUMN datos_operacion TEXT NULL;
    END IF;
END$$

DELIMITER ;

CALL tmp_solicitud_retomar();
DROP PROCEDURE IF EXISTS tmp_solicitud_retomar;

DELIMITER $$

-- RF014: la autorizacion aprobada se consumio en su operacion (un solo uso). La llama
-- SolicitudAutorizacionBOImpl.consumir dentro de la transaccion de la operacion: si esta falla, rollback.
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_usar$$
CREATE PROCEDURE sp_solicitud_autorizacion_usar(IN p_id_solicitud_autorizacion INT)
BEGIN
    UPDATE solicitud_autorizacion
    SET estado = 'USADA'
    WHERE id = p_id_solicitud_autorizacion AND estado = 'APROBADA';
END$$

-- RF014: guarda (o borra con NULL) los datos de la pantalla para retomar la operacion
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_guardar_datos$$
CREATE PROCEDURE sp_solicitud_autorizacion_guardar_datos(
    IN p_id_solicitud_autorizacion INT,
    IN p_datos_operacion TEXT
)
BEGIN
    UPDATE solicitud_autorizacion
    SET datos_operacion = p_datos_operacion
    WHERE id = p_id_solicitud_autorizacion;
END$$

DELIMITER ;
