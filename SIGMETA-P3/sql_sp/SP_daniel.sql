
USE sigmeta;

-- ============================================================
-- PROCEDIMIENTOS ALMACENADOS: COMPRA
-- ============================================================

DELIMITER $$

CREATE PROCEDURE sp_compra_insertar(
    IN p_id_proveedor INT,
    IN p_numero VARCHAR(50),
    IN p_fecha_emision DATE,
    IN p_moneda VARCHAR(10),
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(255),
    IN p_id_usuario_registro INT,
    IN p_estado VARCHAR(30),
    IN p_fecha_recepcion_estimada DATE,
    OUT p_id INT
)
BEGIN
    INSERT INTO compra (
        id_proveedor, numero, fecha_emision, moneda,
        sub_total, igv, total, observaciones,
        fecha_registro, id_usuario_registro, anulado,
        estado, fecha_recepcion_estimada
    ) VALUES (
        p_id_proveedor, p_numero, p_fecha_emision, p_moneda,
        p_sub_total, p_igv, p_total, p_observaciones,
        NOW(), p_id_usuario_registro, FALSE,
        p_estado, p_fecha_recepcion_estimada
    );

    SET p_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_compra_modificar(
    IN p_id INT,
    IN p_id_proveedor INT,
    IN p_numero VARCHAR(50),
    IN p_fecha_emision DATE,
    IN p_moneda VARCHAR(10),
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(255),
    IN p_estado VARCHAR(30),
    IN p_fecha_recepcion_estimada DATE
)
BEGIN
    UPDATE compra
    SET id_proveedor = p_id_proveedor,
        numero = p_numero,
        fecha_emision = p_fecha_emision,
        moneda = p_moneda,
        sub_total = p_sub_total,
        igv = p_igv,
        total = p_total,
        observaciones = p_observaciones,
        estado = p_estado,
        fecha_recepcion_estimada = p_fecha_recepcion_estimada
    WHERE id = p_id
      AND anulado = FALSE
      AND estado = 'REGISTRADA'
      AND NOT EXISTS (
          SELECT 1
          FROM recepcion_compra
          WHERE id_compra = p_id
      );
END$$

CREATE PROCEDURE sp_compra_eliminar(
    IN p_id INT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM recepcion_compra
        WHERE id_compra = p_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede anular una compra con recepciones registradas';
    END IF;

    UPDATE compra
    SET anulado = TRUE,
        estado = 'ANULADA',
        motivo_anulacion = 'Anulacion desde el sistema',
        fecha_anulacion = NOW()
    WHERE id = p_id AND anulado = FALSE;
END$$

CREATE PROCEDURE sp_compra_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM compra
    WHERE id = p_id;
END$$

CREATE PROCEDURE sp_compra_listar()
BEGIN
    SELECT *
    FROM compra
    ORDER BY id;
END$$


-- ============================================================
-- PROCEDIMIENTOS ALMACENADOS: DETALLE COMPRA
-- ============================================================

CREATE PROCEDURE sp_detalle_compra_insertar(
    IN p_id_compra INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2),
    IN p_unidad_compra VARCHAR(20),
    IN p_factor_conversion DECIMAL(12,3),
    OUT p_id INT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM compra
        WHERE id = p_id_compra
          AND anulado = FALSE
          AND estado = 'REGISTRADA'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La compra no permite agregar detalles';
    END IF;

    INSERT INTO detalle_compra (
        id_compra, numero_linea, id_producto,
        cantidad, precio_unitario, descuento, importe,
        unidad_compra, factor_conversion, cantidad_recibida
    ) VALUES (
        p_id_compra, p_numero_linea, p_id_producto,
        p_cantidad, p_precio_unitario, p_descuento, p_importe,
        p_unidad_compra, p_factor_conversion, 0
    );

    SET p_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_detalle_compra_modificar(
    IN p_id INT,
    IN p_id_compra INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2),
    IN p_unidad_compra VARCHAR(20),
    IN p_factor_conversion DECIMAL(12,3),
    IN p_cantidad_recibida DECIMAL(12,3)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM compra
        WHERE id = p_id_compra
          AND anulado = FALSE
          AND estado <> 'ANULADA'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La compra no esta disponible';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM detalle_compra
        WHERE id = p_id
          AND id_compra <> p_id_compra
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede trasladar el detalle a otra compra';
    END IF;

    UPDATE detalle_compra
    SET numero_linea = p_numero_linea,
        id_producto = p_id_producto,
        cantidad = p_cantidad,
        precio_unitario = p_precio_unitario,
        descuento = p_descuento,
        importe = p_importe,
        unidad_compra = p_unidad_compra,
        factor_conversion = p_factor_conversion,
        cantidad_recibida = p_cantidad_recibida
    WHERE id = p_id
      AND anulado = FALSE
      AND p_cantidad >= p_cantidad_recibida
      AND (
          p_cantidad_recibida = 0
          OR (
              id_producto = p_id_producto
              AND cantidad_recibida <= p_cantidad_recibida
          )
      );
END$$

CREATE PROCEDURE sp_detalle_compra_eliminar(
    IN p_id INT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM detalle_recepcion_compra
        WHERE id_detalle_compra = p_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede eliminar un detalle que ya tiene recepciones';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM detalle_compra dc
        INNER JOIN compra c ON c.id = dc.id_compra
        WHERE dc.id = p_id
          AND (
              c.estado <> 'REGISTRADA'
              OR c.anulado = TRUE
          )
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Solo se pueden eliminar detalles de compras registradas';
    END IF;

    UPDATE detalle_compra
    SET anulado = TRUE
    WHERE id = p_id
      AND cantidad_recibida = 0
      AND anulado = FALSE;
END$$

CREATE PROCEDURE sp_detalle_compra_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM detalle_compra
    WHERE id = p_id;
END$$

CREATE PROCEDURE sp_detalle_compra_listar()
BEGIN
    SELECT *
    FROM detalle_compra
    WHERE anulado = FALSE
    ORDER BY id;
END$


-- ============================================================
-- PROCEDIMIENTOS ALMACENADOS: RECEPCION COMPRA
-- ============================================================

CREATE PROCEDURE sp_recepcion_compra_insertar(
    IN p_id_compra INT,
    IN p_fecha_recepcion DATE,
    IN p_observaciones VARCHAR(255),
    IN p_id_usuario_registro INT,
    OUT p_id INT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM compra
        WHERE id = p_id_compra
          AND anulado = FALSE
          AND estado IN ('REGISTRADA', 'RECIBIDA_PARCIAL')
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La compra no permite registrar nuevas recepciones';
    END IF;

    INSERT INTO recepcion_compra (
        id_compra, fecha_recepcion, observaciones,
        id_usuario_registro, fecha_registro
    ) VALUES (
        p_id_compra, p_fecha_recepcion, p_observaciones,
        p_id_usuario_registro, NOW()
    );

    SET p_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_recepcion_compra_modificar(
    IN p_id INT,
    IN p_fecha_recepcion DATE,
    IN p_observaciones VARCHAR(255)
)
BEGIN
    UPDATE recepcion_compra
    SET fecha_recepcion = p_fecha_recepcion,
        observaciones = p_observaciones
    WHERE id = p_id;
END$$

CREATE PROCEDURE sp_recepcion_compra_eliminar(
    IN p_id INT
)
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'No se permite eliminar recepciones historicas';
END$$

CREATE PROCEDURE sp_recepcion_compra_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM recepcion_compra
    WHERE id = p_id;
END$$

CREATE PROCEDURE sp_recepcion_compra_listar()
BEGIN
    SELECT *
    FROM recepcion_compra
    ORDER BY id;
END$$


-- ============================================================
-- PROCEDIMIENTOS ALMACENADOS: DETALLE RECEPCION COMPRA
-- ============================================================

CREATE PROCEDURE sp_detalle_recepcion_compra_insertar(
    IN p_id_recepcion_compra INT,
    IN p_id_detalle_compra INT,
    IN p_cantidad_recibida DECIMAL(12,3),
    OUT p_id INT
)
BEGIN
    IF p_cantidad_recibida IS NULL
       OR p_cantidad_recibida <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La cantidad recibida debe ser positiva';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM recepcion_compra r
        INNER JOIN detalle_compra d
            ON d.id = p_id_detalle_compra
        WHERE r.id = p_id_recepcion_compra
          AND r.id_compra = d.id_compra
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El detalle no pertenece a la compra de esta recepcion';
    END IF;

    INSERT INTO detalle_recepcion_compra (
        id_recepcion_compra,
        id_detalle_compra,
        cantidad_recibida
    ) VALUES (
        p_id_recepcion_compra,
        p_id_detalle_compra,
        p_cantidad_recibida
    );

    SET p_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_detalle_recepcion_compra_modificar(
    IN p_id INT,
    IN p_cantidad_recibida DECIMAL(12,3)
)
BEGIN
    IF p_cantidad_recibida IS NULL
       OR p_cantidad_recibida <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La cantidad recibida debe ser positiva';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM detalle_recepcion_compra dr
        INNER JOIN movimiento_inventario m
            ON m.id_recepcion_compra = dr.id_recepcion_compra
        WHERE dr.id = p_id
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede modificar una recepcion que ya genero movimientos';
    END IF;

    UPDATE detalle_recepcion_compra
    SET cantidad_recibida = p_cantidad_recibida
    WHERE id = p_id;
END$$

CREATE PROCEDURE sp_detalle_recepcion_compra_eliminar(
    IN p_id INT
)
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'No se permite eliminar detalles de recepciones historicas';
END$$

CREATE PROCEDURE sp_detalle_recepcion_compra_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM detalle_recepcion_compra
    WHERE id = p_id;
END$$

CREATE PROCEDURE sp_detalle_recepcion_compra_listar()
BEGIN
    SELECT *
    FROM detalle_recepcion_compra
    ORDER BY id;
END$$


-- ============================================================
-- PROCEDIMIENTOS ALMACENADOS: MOVIMIENTO INVENTARIO
-- ============================================================

CREATE PROCEDURE sp_movimiento_inventario_insertar(
    IN p_id_producto INT,
    IN p_tipo VARCHAR(30),
    IN p_cantidad DECIMAL(12,3),
    IN p_stock_resultante DECIMAL(12,3),
    IN p_id_recepcion_compra INT,
    IN p_id_despacho INT,
    IN p_id_comprobante INT,
    IN p_id_usuario_registro INT,
    IN p_motivo VARCHAR(255),
    IN p_cantidad_contada DECIMAL(12,3),
    OUT p_id INT
)
BEGIN
    IF p_cantidad IS NULL OR p_cantidad <= 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La cantidad del movimiento debe ser positiva';
    END IF;

    INSERT INTO movimiento_inventario (
        id_producto, tipo, fecha_movimiento,
        cantidad, stock_resultante,
        id_recepcion_compra, id_despacho, id_comprobante,
        id_usuario_registro, motivo, cantidad_contada
    ) VALUES (
        p_id_producto, p_tipo, NOW(),
        p_cantidad, p_stock_resultante,
        p_id_recepcion_compra, p_id_despacho, p_id_comprobante,
        p_id_usuario_registro, p_motivo, p_cantidad_contada
    );

    SET p_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_movimiento_inventario_modificar(
    IN p_id INT,
    IN p_motivo VARCHAR(255),
    IN p_cantidad_contada DECIMAL(12,3)
)
BEGIN
    UPDATE movimiento_inventario
    SET motivo = p_motivo
    WHERE id = p_id;
END$$

CREATE PROCEDURE sp_movimiento_inventario_eliminar(
    IN p_id INT
)
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'No se permite eliminar movimientos historicos de inventario';
END$$

CREATE PROCEDURE sp_movimiento_inventario_obtener(
    IN p_id INT
)
BEGIN
    SELECT *
    FROM movimiento_inventario
    WHERE id = p_id;
END$$

CREATE PROCEDURE sp_movimiento_inventario_listar()
BEGIN
    SELECT *
    FROM movimiento_inventario
    ORDER BY id;
END$$


-- ============================================================
-- PROCEDIMIENTO ADICIONAL: ACTUALIZAR ESTADO DE COMPRA
-- ============================================================

CREATE PROCEDURE sp_compra_actualizar_estado(
    IN p_id INT,
    IN p_estado VARCHAR(30)
)
BEGIN
    IF p_estado IS NULL
       OR p_estado NOT IN ('RECIBIDA_PARCIAL', 'RECIBIDA') THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Estado de recepcion no valido';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM compra
        WHERE id = p_id
          AND anulado = FALSE
          AND estado IN ('REGISTRADA', 'RECIBIDA_PARCIAL')
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La compra no permite actualizar su estado de recepcion';
    END IF;

    UPDATE compra
    SET estado = p_estado
    WHERE id = p_id;
END$$

DELIMITER ;
