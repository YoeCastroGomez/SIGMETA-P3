USE sigmeta;

-- ============================================================
-- PROCEDIMIENTOS ALMACENADOS: ESTUDIANTE 5 (ADRIANO)
-- MODULO CAJA Y COBRANZA (RF009, RF010)
-- Tablas: caja, movimiento_caja, cierre_caja, cuenta_por_cobrar, cobro
-- Los alias de las columnas (caja_id, mov_id, cierre_id, cobro_id, cxc_id, usuario_id, ...)
-- son los que leen los DAO de sigmeta-dao (daoimpl/caja y daoimpl/cobranza).
-- ============================================================

DELIMITER $$

-- ============================================================
-- CAJA
-- ============================================================
DROP PROCEDURE IF EXISTS sp_caja_insertar$$
CREATE PROCEDURE sp_caja_insertar(
    OUT p_id INT,
    IN p_monto_inicial DECIMAL(12,2),
    IN p_abierta BOOLEAN,
    IN p_id_usuario_apertura INT
)
BEGIN
    INSERT INTO caja (fecha_apertura, monto_inicial, id_usuario_apertura, abierta)
    VALUES (NOW(), p_monto_inicial, p_id_usuario_apertura, p_abierta);
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_caja_modificar$$
CREATE PROCEDURE sp_caja_modificar(
    IN p_id INT,
    IN p_monto_inicial DECIMAL(12,2),
    IN p_abierta BOOLEAN,
    IN p_id_usuario_apertura INT
)
BEGIN
    UPDATE caja
    SET monto_inicial = p_monto_inicial,
        abierta = p_abierta,
        id_usuario_apertura = p_id_usuario_apertura
    WHERE id = p_id;
END$$

-- La caja no tiene columna de baja logica: solo se elimina una apertura registrada por error,
-- es decir, sin movimientos ni cierre. Una caja con operaciones se conserva (RNF002).
DROP PROCEDURE IF EXISTS sp_caja_eliminar$$
CREATE PROCEDURE sp_caja_eliminar(IN p_id INT)
BEGIN
    IF EXISTS (SELECT 1 FROM movimiento_caja WHERE id_caja = p_id)
       OR EXISTS (SELECT 1 FROM cierre_caja WHERE id_caja = p_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede eliminar una caja con movimientos o cierre registrados';
    END IF;
    DELETE FROM caja WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_caja_obtener$$
CREATE PROCEDURE sp_caja_obtener(IN p_id INT)
BEGIN
    SELECT c.id AS caja_id, c.fecha_apertura, c.monto_inicial, c.abierta,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM caja c
    INNER JOIN usuario u ON u.id = c.id_usuario_apertura
    WHERE c.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_caja_obtener_abierta_por_usuario$$
CREATE PROCEDURE sp_caja_obtener_abierta_por_usuario(IN p_id_usuario INT)
BEGIN
    SELECT c.id AS caja_id, c.fecha_apertura, c.monto_inicial, c.abierta,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM caja c
    INNER JOIN usuario u ON u.id = c.id_usuario_apertura
    WHERE c.id_usuario_apertura = p_id_usuario
      AND c.abierta = TRUE
    ORDER BY c.fecha_apertura DESC
    LIMIT 1;
END$$

DROP PROCEDURE IF EXISTS sp_caja_listar_todas_con_usuario$$
CREATE PROCEDURE sp_caja_listar_todas_con_usuario()
BEGIN
    SELECT c.id AS caja_id, c.fecha_apertura, c.monto_inicial, c.abierta,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM caja c
    INNER JOIN usuario u ON u.id = c.id_usuario_apertura
    ORDER BY c.fecha_apertura DESC;
END$$


-- ============================================================
-- MOVIMIENTO DE CAJA
-- ============================================================
DROP PROCEDURE IF EXISTS sp_movimiento_caja_insertar$$
CREATE PROCEDURE sp_movimiento_caja_insertar(
    OUT p_id INT,
    IN p_id_caja INT,
    IN p_tipo VARCHAR(20),
    IN p_medio_pago VARCHAR(20),
    IN p_monto DECIMAL(12,2),
    IN p_concepto VARCHAR(150),
    IN p_documento_origen VARCHAR(50),
    IN p_id_documento_origen INT,
    IN p_id_usuario_registro INT
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM caja WHERE id = p_id_caja AND abierta = TRUE) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La caja no existe o ya esta cerrada';
    END IF;

    INSERT INTO movimiento_caja (
        id_caja, tipo, fecha_movimiento, medio_pago, monto, concepto,
        documento_origen, id_documento_origen, id_usuario_registro
    ) VALUES (
        p_id_caja, p_tipo, NOW(), p_medio_pago, p_monto, p_concepto,
        p_documento_origen, p_id_documento_origen, p_id_usuario_registro
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_caja_modificar$$
CREATE PROCEDURE sp_movimiento_caja_modificar(
    IN p_id INT,
    IN p_tipo VARCHAR(20),
    IN p_medio_pago VARCHAR(20),
    IN p_monto DECIMAL(12,2),
    IN p_concepto VARCHAR(150),
    IN p_documento_origen VARCHAR(50),
    IN p_id_documento_origen INT
)
BEGIN
    UPDATE movimiento_caja
    SET tipo = p_tipo,
        medio_pago = p_medio_pago,
        monto = p_monto,
        concepto = p_concepto,
        documento_origen = p_documento_origen,
        id_documento_origen = p_id_documento_origen
    WHERE id = p_id;
END$$

-- Solo mientras la caja esta abierta: una caja cerrada ya cuadro con sus movimientos (RF010).
DROP PROCEDURE IF EXISTS sp_movimiento_caja_eliminar$$
CREATE PROCEDURE sp_movimiento_caja_eliminar(IN p_id INT)
BEGIN
    IF EXISTS (
        SELECT 1 FROM movimiento_caja m
        INNER JOIN caja c ON c.id = m.id_caja
        WHERE m.id = p_id AND c.abierta = FALSE
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede eliminar un movimiento de una caja cerrada';
    END IF;
    DELETE FROM movimiento_caja WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_caja_obtener$$
CREATE PROCEDURE sp_movimiento_caja_obtener(IN p_id INT)
BEGIN
    SELECT m.id AS mov_id, m.id_caja AS caja_id, m.tipo, m.fecha_movimiento, m.medio_pago,
           m.monto, m.concepto, m.documento_origen, m.id_documento_origen,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM movimiento_caja m
    INNER JOIN usuario u ON u.id = m.id_usuario_registro
    WHERE m.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_caja_listar_por_caja$$
CREATE PROCEDURE sp_movimiento_caja_listar_por_caja(IN p_id_caja INT)
BEGIN
    SELECT m.id AS mov_id, m.id_caja AS caja_id, m.tipo, m.fecha_movimiento, m.medio_pago,
           m.monto, m.concepto, m.documento_origen, m.id_documento_origen,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM movimiento_caja m
    INNER JOIN usuario u ON u.id = m.id_usuario_registro
    WHERE m.id_caja = p_id_caja
    ORDER BY m.fecha_movimiento, m.id;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_caja_listar_con_detalle$$
CREATE PROCEDURE sp_movimiento_caja_listar_con_detalle()
BEGIN
    SELECT m.id AS mov_id, m.id_caja AS caja_id, m.tipo, m.fecha_movimiento, m.medio_pago,
           m.monto, m.concepto, m.documento_origen, m.id_documento_origen,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM movimiento_caja m
    INNER JOIN usuario u ON u.id = m.id_usuario_registro
    ORDER BY m.fecha_movimiento DESC, m.id DESC;
END$$

DROP PROCEDURE IF EXISTS sp_movimiento_caja_buscar_por_fechas$$
CREATE PROCEDURE sp_movimiento_caja_buscar_por_fechas(
    IN p_id_caja INT,
    IN p_fecha_inicio DATETIME,
    IN p_fecha_fin DATETIME
)
BEGIN
    SELECT m.id AS mov_id, m.id_caja AS caja_id, m.tipo, m.fecha_movimiento, m.medio_pago,
           m.monto, m.concepto, m.documento_origen, m.id_documento_origen,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM movimiento_caja m
    INNER JOIN usuario u ON u.id = m.id_usuario_registro
    WHERE m.id_caja = p_id_caja
      AND m.fecha_movimiento BETWEEN p_fecha_inicio AND p_fecha_fin
    ORDER BY m.fecha_movimiento, m.id;
END$$

-- Saldo neto por medio de pago: ingresos menos egresos
DROP PROCEDURE IF EXISTS sp_movimiento_caja_totales_medio_pago$$
CREATE PROCEDURE sp_movimiento_caja_totales_medio_pago(IN p_id_caja INT)
BEGIN
    SELECT medio_pago,
           SUM(CASE WHEN tipo = 'EGRESO_MANUAL' THEN -monto ELSE monto END) AS total_monto
    FROM movimiento_caja
    WHERE id_caja = p_id_caja
    GROUP BY medio_pago;
END$$


-- ============================================================
-- CIERRE DE CAJA
-- ============================================================
DROP PROCEDURE IF EXISTS sp_cierre_caja_insertar$$
CREATE PROCEDURE sp_cierre_caja_insertar(
    OUT p_id INT,
    IN p_id_caja INT,
    IN p_monto_calculado DECIMAL(12,2),
    IN p_monto_declarado DECIMAL(12,2),
    IN p_diferencia DECIMAL(12,2),
    IN p_id_usuario_cierre INT
)
BEGIN
    IF EXISTS (SELECT 1 FROM cierre_caja WHERE id_caja = p_id_caja) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La caja ya tiene un cierre registrado';
    END IF;

    INSERT INTO cierre_caja (
        id_caja, fecha_cierre, monto_calculado, monto_declarado, diferencia, id_usuario_cierre
    ) VALUES (
        p_id_caja, NOW(), p_monto_calculado, p_monto_declarado, p_diferencia, p_id_usuario_cierre
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_cierre_caja_modificar$$
CREATE PROCEDURE sp_cierre_caja_modificar(
    IN p_id INT,
    IN p_monto_calculado DECIMAL(12,2),
    IN p_monto_declarado DECIMAL(12,2),
    IN p_diferencia DECIMAL(12,2)
)
BEGIN
    UPDATE cierre_caja
    SET monto_calculado = p_monto_calculado,
        monto_declarado = p_monto_declarado,
        diferencia = p_diferencia
    WHERE id = p_id;
END$$

-- RF010: eliminar el cierre equivale a reabrir la caja (solo el Administrador, lo controla el BO)
DROP PROCEDURE IF EXISTS sp_cierre_caja_eliminar$$
CREATE PROCEDURE sp_cierre_caja_eliminar(IN p_id INT)
BEGIN
    DECLARE v_id_caja INT;
    SELECT id_caja INTO v_id_caja FROM cierre_caja WHERE id = p_id;
    DELETE FROM cierre_caja WHERE id = p_id;
    UPDATE caja SET abierta = TRUE WHERE id = v_id_caja;
END$$

DROP PROCEDURE IF EXISTS sp_cierre_caja_obtener$$
CREATE PROCEDURE sp_cierre_caja_obtener(IN p_id INT)
BEGIN
    SELECT cc.id AS cierre_id, cc.id_caja AS caja_id, cc.fecha_cierre, cc.monto_calculado,
           cc.monto_declarado, cc.diferencia,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM cierre_caja cc
    INNER JOIN usuario u ON u.id = cc.id_usuario_cierre
    WHERE cc.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_cierre_caja_obtener_por_caja$$
CREATE PROCEDURE sp_cierre_caja_obtener_por_caja(IN p_id_caja INT)
BEGIN
    SELECT cc.id AS cierre_id, cc.id_caja AS caja_id, cc.fecha_cierre, cc.monto_calculado,
           cc.monto_declarado, cc.diferencia,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM cierre_caja cc
    INNER JOIN usuario u ON u.id = cc.id_usuario_cierre
    WHERE cc.id_caja = p_id_caja;
END$$

DROP PROCEDURE IF EXISTS sp_cierre_caja_listar_con_detalle$$
CREATE PROCEDURE sp_cierre_caja_listar_con_detalle()
BEGIN
    SELECT cc.id AS cierre_id, cc.id_caja AS caja_id, cc.fecha_cierre, cc.monto_calculado,
           cc.monto_declarado, cc.diferencia,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM cierre_caja cc
    INNER JOIN usuario u ON u.id = cc.id_usuario_cierre
    ORDER BY cc.fecha_cierre DESC;
END$$

DROP PROCEDURE IF EXISTS sp_cierre_caja_buscar_por_fechas$$
CREATE PROCEDURE sp_cierre_caja_buscar_por_fechas(
    IN p_fecha_inicio DATETIME,
    IN p_fecha_fin DATETIME
)
BEGIN
    SELECT cc.id AS cierre_id, cc.id_caja AS caja_id, cc.fecha_cierre, cc.monto_calculado,
           cc.monto_declarado, cc.diferencia,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM cierre_caja cc
    INNER JOIN usuario u ON u.id = cc.id_usuario_cierre
    WHERE cc.fecha_cierre BETWEEN p_fecha_inicio AND p_fecha_fin
    ORDER BY cc.fecha_cierre;
END$$

-- RF010: monto que deberia haber en caja = monto inicial + ingresos - egresos.
-- Las ventas anuladas y las notas de credito no generan movimiento de caja, por eso no suman.
DROP PROCEDURE IF EXISTS sp_cierre_caja_calcular_monto_esperado$$
CREATE PROCEDURE sp_cierre_caja_calcular_monto_esperado(
    IN p_id_caja INT,
    OUT p_monto_esperado DECIMAL(12,2)
)
BEGIN
    SELECT c.monto_inicial
           + COALESCE(SUM(CASE WHEN m.tipo = 'EGRESO_MANUAL' THEN -m.monto ELSE m.monto END), 0)
    INTO p_monto_esperado
    FROM caja c
    LEFT JOIN movimiento_caja m ON m.id_caja = c.id
    WHERE c.id = p_id_caja
    GROUP BY c.id, c.monto_inicial;

    SET p_monto_esperado = COALESCE(p_monto_esperado, 0);
END$$


-- ============================================================
-- CUENTA POR COBRAR
-- ============================================================
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_insertar$$
CREATE PROCEDURE sp_cuenta_por_cobrar_insertar(
    OUT p_id INT,
    IN p_id_venta INT,
    IN p_id_cliente INT,
    IN p_fecha_emision DATE,
    IN p_fecha_vencimiento DATE,
    IN p_moneda VARCHAR(10),
    IN p_monto_original DECIMAL(12,2),
    IN p_monto_pagado DECIMAL(12,2),
    IN p_saldo_pendiente DECIMAL(12,2),
    IN p_estado VARCHAR(20)
)
BEGIN
    INSERT INTO cuenta_por_cobrar (
        id_venta, id_cliente, fecha_emision, fecha_vencimiento, moneda,
        monto_original, monto_pagado, saldo_pendiente, estado
    ) VALUES (
        p_id_venta, p_id_cliente, p_fecha_emision, p_fecha_vencimiento, p_moneda,
        p_monto_original, p_monto_pagado, p_saldo_pendiente, p_estado
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_modificar$$
CREATE PROCEDURE sp_cuenta_por_cobrar_modificar(
    IN p_id INT,
    IN p_fecha_vencimiento DATE,
    IN p_moneda VARCHAR(10),
    IN p_monto_original DECIMAL(12,2),
    IN p_monto_pagado DECIMAL(12,2),
    IN p_saldo_pendiente DECIMAL(12,2),
    IN p_estado VARCHAR(20)
)
BEGIN
    UPDATE cuenta_por_cobrar
    SET fecha_vencimiento = p_fecha_vencimiento,
        moneda = p_moneda,
        monto_original = p_monto_original,
        monto_pagado = p_monto_pagado,
        saldo_pendiente = p_saldo_pendiente,
        estado = p_estado
    WHERE id = p_id;
END$$

-- Solo se elimina una cuenta sin cobros (generada por error); con cobros se conserva (RNF002)
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_eliminar$$
CREATE PROCEDURE sp_cuenta_por_cobrar_eliminar(IN p_id INT)
BEGIN
    IF EXISTS (SELECT 1 FROM cobro WHERE id_cuenta_por_cobrar = p_id) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede eliminar una cuenta por cobrar con cobros registrados';
    END IF;
    DELETE FROM cuenta_por_cobrar WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_obtener$$
CREATE PROCEDURE sp_cuenta_por_cobrar_obtener(IN p_id INT)
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento,
           x.fecha_emision, x.fecha_vencimiento, x.moneda,
           x.monto_original, x.monto_pagado, x.saldo_pendiente, x.estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE x.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_obtener_por_venta$$
CREATE PROCEDURE sp_cuenta_por_cobrar_obtener_por_venta(IN p_id_venta INT)
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento,
           x.fecha_emision, x.fecha_vencimiento, x.moneda,
           x.monto_original, x.monto_pagado, x.saldo_pendiente, x.estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE x.id_venta = p_id_venta;
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_por_cliente$$
CREATE PROCEDURE sp_cuenta_por_cobrar_listar_por_cliente(IN p_id_cliente INT)
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento,
           x.fecha_emision, x.fecha_vencimiento, x.moneda,
           x.monto_original, x.monto_pagado, x.saldo_pendiente, x.estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE x.id_cliente = p_id_cliente
    ORDER BY x.fecha_vencimiento;
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_por_estado$$
CREATE PROCEDURE sp_cuenta_por_cobrar_listar_por_estado(IN p_estado VARCHAR(20))
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento,
           x.fecha_emision, x.fecha_vencimiento, x.moneda,
           x.monto_original, x.monto_pagado, x.saldo_pendiente, x.estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE x.estado = p_estado
    ORDER BY x.fecha_vencimiento;
END$$

-- RF009: cuentas vencidas = con saldo y fecha de vencimiento pasada, aunque su estado no se haya actualizado
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_vencidas$$
CREATE PROCEDURE sp_cuenta_por_cobrar_listar_vencidas()
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento,
           x.fecha_emision, x.fecha_vencimiento, x.moneda,
           x.monto_original, x.monto_pagado, x.saldo_pendiente, x.estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE x.saldo_pendiente > 0
      AND x.fecha_vencimiento < CURDATE()
    ORDER BY x.fecha_vencimiento;
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_todas_con_detalle$$
CREATE PROCEDURE sp_cuenta_por_cobrar_listar_todas_con_detalle()
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento,
           x.fecha_emision, x.fecha_vencimiento, x.moneda,
           x.monto_original, x.monto_pagado, x.saldo_pendiente, x.estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    ORDER BY x.fecha_emision DESC;
END$$


-- ============================================================
-- COBRO
-- ============================================================
DROP PROCEDURE IF EXISTS sp_cobro_insertar$$
CREATE PROCEDURE sp_cobro_insertar(
    OUT p_id INT,
    IN p_id_cuenta_por_cobrar INT,
    IN p_fecha_cobro DATE,
    IN p_medio_pago VARCHAR(20),
    IN p_monto DECIMAL(12,2),
    IN p_referencia VARCHAR(50),
    IN p_id_usuario_registro INT
)
BEGIN
    INSERT INTO cobro (
        id_cuenta_por_cobrar, fecha_cobro, medio_pago, monto, referencia,
        id_usuario_registro, fecha_registro
    ) VALUES (
        p_id_cuenta_por_cobrar, p_fecha_cobro, p_medio_pago, p_monto, p_referencia,
        p_id_usuario_registro, NOW()
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_cobro_modificar$$
CREATE PROCEDURE sp_cobro_modificar(
    IN p_id INT,
    IN p_fecha_cobro DATE,
    IN p_medio_pago VARCHAR(20),
    IN p_monto DECIMAL(12,2),
    IN p_referencia VARCHAR(50)
)
BEGIN
    UPDATE cobro
    SET fecha_cobro = p_fecha_cobro,
        medio_pago = p_medio_pago,
        monto = p_monto,
        referencia = p_referencia
    WHERE id = p_id;
END$$

-- La reversion del saldo de la cuenta y del movimiento de caja la hace el BO en la misma transaccion
DROP PROCEDURE IF EXISTS sp_cobro_eliminar$$
CREATE PROCEDURE sp_cobro_eliminar(IN p_id INT)
BEGIN
    DELETE FROM cobro WHERE id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_cobro_obtener$$
CREATE PROCEDURE sp_cobro_obtener(IN p_id INT)
BEGIN
    SELECT co.id AS cobro_id, co.id_cuenta_por_cobrar AS cuenta_por_cobrar_id, co.fecha_cobro,
           co.medio_pago, co.monto, co.referencia, co.fecha_registro,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos
    FROM cobro co
    INNER JOIN usuario u ON u.id = co.id_usuario_registro
    WHERE co.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_cobro_listar$$
CREATE PROCEDURE sp_cobro_listar()
BEGIN
    SELECT co.id AS cobro_id, co.id_cuenta_por_cobrar AS cuenta_por_cobrar_id, co.fecha_cobro,
           co.medio_pago, co.monto, co.referencia, co.fecha_registro,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos
    FROM cobro co
    INNER JOIN usuario u ON u.id = co.id_usuario_registro
    ORDER BY co.fecha_cobro DESC, co.id DESC;
END$$

DROP PROCEDURE IF EXISTS sp_cobro_listar_por_cuenta_por_cobrar$$
CREATE PROCEDURE sp_cobro_listar_por_cuenta_por_cobrar(IN p_id_cuenta_por_cobrar INT)
BEGIN
    SELECT co.id AS cobro_id, co.id_cuenta_por_cobrar AS cuenta_por_cobrar_id, co.fecha_cobro,
           co.medio_pago, co.monto, co.referencia, co.fecha_registro,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos
    FROM cobro co
    INNER JOIN usuario u ON u.id = co.id_usuario_registro
    WHERE co.id_cuenta_por_cobrar = p_id_cuenta_por_cobrar
    ORDER BY co.fecha_cobro, co.id;
END$$

DROP PROCEDURE IF EXISTS sp_cobro_listar_por_cliente$$
CREATE PROCEDURE sp_cobro_listar_por_cliente(IN p_id_cliente INT)
BEGIN
    SELECT co.id AS cobro_id, co.id_cuenta_por_cobrar AS cuenta_por_cobrar_id, co.fecha_cobro,
           co.medio_pago, co.monto, co.referencia, co.fecha_registro,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos
    FROM cobro co
    INNER JOIN cuenta_por_cobrar x ON x.id = co.id_cuenta_por_cobrar
    INNER JOIN usuario u ON u.id = co.id_usuario_registro
    WHERE x.id_cliente = p_id_cliente
    ORDER BY co.fecha_cobro, co.id;
END$$

DELIMITER ;
