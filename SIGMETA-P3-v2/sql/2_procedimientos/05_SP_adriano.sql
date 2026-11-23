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

-- RF010, RNF002: los movimientos de caja (dinero) nunca se borran. Un ingreso o egreso manual se anula
-- registrando su movimiento contrario (MovimientoCajaBOImpl.anular). Este procedimiento queda solo para
-- rechazar cualquier intento de borrado.
DROP PROCEDURE IF EXISTS sp_movimiento_caja_eliminar$$
CREATE PROCEDURE sp_movimiento_caja_eliminar(IN p_id INT)
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Los movimientos de caja no se eliminan: se anulan con su movimiento contrario';
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
    -- una caja reabierta conserva su cierre REVERTIDO y puede volver a cerrarse
    IF EXISTS (SELECT 1 FROM cierre_caja WHERE id_caja = p_id_caja AND estado = 'VIGENTE') THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'La caja ya tiene un cierre vigente';
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

-- RF010: el cierre nunca se elimina. Reabrir la caja (solo el Administrador, lo controla el BO)
-- marca el cierre como REVERTIDO con fecha, responsable y motivo, y vuelve a abrir la caja.
DROP PROCEDURE IF EXISTS sp_cierre_caja_eliminar$$
DROP PROCEDURE IF EXISTS sp_cierre_caja_reabrir$$
CREATE PROCEDURE sp_cierre_caja_reabrir(
    IN p_id INT,
    IN p_id_usuario_reapertura INT,
    IN p_motivo_reapertura VARCHAR(200)
)
BEGIN
    DECLARE v_id_caja INT;
    SELECT id_caja INTO v_id_caja FROM cierre_caja WHERE id = p_id AND estado = 'VIGENTE';
    IF v_id_caja IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El cierre no existe o ya fue revertido';
    END IF;
    UPDATE cierre_caja
    SET estado = 'REVERTIDO',
        fecha_reapertura = NOW(),
        id_usuario_reapertura = p_id_usuario_reapertura,
        motivo_reapertura = p_motivo_reapertura
    WHERE id = p_id;
    UPDATE caja SET abierta = TRUE WHERE id = v_id_caja;
END$$

DROP PROCEDURE IF EXISTS sp_cierre_caja_obtener$$
CREATE PROCEDURE sp_cierre_caja_obtener(IN p_id INT)
BEGIN
    SELECT cc.id AS cierre_id, cc.id_caja AS caja_id, cc.fecha_cierre, cc.monto_calculado,
           cc.monto_declarado, cc.diferencia, cc.estado, cc.fecha_reapertura,
           cc.id_usuario_reapertura, cc.motivo_reapertura,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM cierre_caja cc
    INNER JOIN usuario u ON u.id = cc.id_usuario_cierre
    WHERE cc.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_cierre_caja_obtener_por_caja$$
CREATE PROCEDURE sp_cierre_caja_obtener_por_caja(IN p_id_caja INT)
BEGIN
    SELECT cc.id AS cierre_id, cc.id_caja AS caja_id, cc.fecha_cierre, cc.monto_calculado,
           cc.monto_declarado, cc.diferencia, cc.estado, cc.fecha_reapertura,
           cc.id_usuario_reapertura, cc.motivo_reapertura,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM cierre_caja cc
    INNER JOIN usuario u ON u.id = cc.id_usuario_cierre
    WHERE cc.id_caja = p_id_caja AND cc.estado = 'VIGENTE'
    ORDER BY cc.fecha_cierre DESC
    LIMIT 1;
END$$

-- RF010: historial de cierres de una caja (vigente y revertidos)
DROP PROCEDURE IF EXISTS sp_cierre_caja_listar_por_caja$$
CREATE PROCEDURE sp_cierre_caja_listar_por_caja(IN p_id_caja INT)
BEGIN
    SELECT cc.id AS cierre_id, cc.id_caja AS caja_id, cc.fecha_cierre, cc.monto_calculado,
           cc.monto_declarado, cc.diferencia, cc.estado, cc.fecha_reapertura,
           cc.id_usuario_reapertura, cc.motivo_reapertura,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM cierre_caja cc
    INNER JOIN usuario u ON u.id = cc.id_usuario_cierre
    WHERE cc.id_caja = p_id_caja
    ORDER BY cc.fecha_cierre;
END$$

DROP PROCEDURE IF EXISTS sp_cierre_caja_listar_con_detalle$$
CREATE PROCEDURE sp_cierre_caja_listar_con_detalle()
BEGIN
    SELECT cc.id AS cierre_id, cc.id_caja AS caja_id, cc.fecha_cierre, cc.monto_calculado,
           cc.monto_declarado, cc.diferencia, cc.estado, cc.fecha_reapertura,
           cc.id_usuario_reapertura, cc.motivo_reapertura,
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
           cc.monto_declarado, cc.diferencia, cc.estado, cc.fecha_reapertura,
           cc.id_usuario_reapertura, cc.motivo_reapertura,
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
    IN p_monto_original DECIMAL(12,2),
    IN p_monto_pagado DECIMAL(12,2),
    IN p_saldo_pendiente DECIMAL(12,2),
    IN p_estado VARCHAR(20)
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM venta WHERE id = p_id_venta
                   AND id_cliente = p_id_cliente AND anulado = FALSE
                   AND condicion_pago = 'CREDITO' AND total > 0
                   AND total = p_monto_original AND fecha_emision = p_fecha_emision
                   AND DATE_ADD(fecha_emision, INTERVAL plazo_credito_dias DAY) = p_fecha_vencimiento)
       OR p_monto_pagado <> 0 OR p_saldo_pendiente <> p_monto_original THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La cuenta inicial no corresponde a su venta al credito';
    END IF;
    SET p_estado = CASE WHEN p_fecha_vencimiento < CURDATE() THEN 'VENCIDA' ELSE 'PENDIENTE' END;
    INSERT INTO cuenta_por_cobrar (
        id_venta, id_cliente, fecha_emision, fecha_vencimiento,
        monto_original, monto_pagado, saldo_pendiente, estado
    ) VALUES (
        p_id_venta, p_id_cliente, p_fecha_emision, p_fecha_vencimiento,
        p_monto_original, p_monto_pagado, p_saldo_pendiente, p_estado
    );
    SET p_id = LAST_INSERT_ID();
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_modificar$$
CREATE PROCEDURE sp_cuenta_por_cobrar_modificar(
    IN p_id INT,
    IN p_fecha_vencimiento DATE,
    IN p_monto_original DECIMAL(12,2),
    IN p_monto_pagado DECIMAL(12,2),
    IN p_saldo_pendiente DECIMAL(12,2),
    IN p_estado VARCHAR(20)
)
BEGIN
    UPDATE cuenta_por_cobrar
    SET fecha_vencimiento = p_fecha_vencimiento,
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
    IF EXISTS (SELECT 1 FROM cuenta_por_cobrar c INNER JOIN venta v ON v.id = c.id_venta
               WHERE c.id = p_id AND v.anulado = FALSE) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se elimina la deuda de una venta vigente';
    END IF;
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
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           x.fecha_emision, x.fecha_vencimiento,
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN v.anulado = TRUE THEN 'ANULADA' WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE x.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_obtener_por_venta$$
CREATE PROCEDURE sp_cuenta_por_cobrar_obtener_por_venta(IN p_id_venta INT)
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           x.fecha_emision, x.fecha_vencimiento,
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN v.anulado = TRUE THEN 'ANULADA' WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE x.id_venta = p_id_venta;
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_por_cliente$$
CREATE PROCEDURE sp_cuenta_por_cobrar_listar_por_cliente(IN p_id_cliente INT)
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           x.fecha_emision, x.fecha_vencimiento,
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN v.anulado = TRUE THEN 'ANULADA' WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           x.fecha_emision, x.fecha_vencimiento,
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN v.anulado = TRUE THEN 'ANULADA' WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE (CASE WHEN v.anulado = TRUE THEN 'ANULADA' WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END) = p_estado
    ORDER BY x.fecha_vencimiento;
END$$

-- RF009: cuentas vencidas = con saldo y fecha de vencimiento pasada, aunque su estado no se haya actualizado
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_vencidas$$
CREATE PROCEDURE sp_cuenta_por_cobrar_listar_vencidas()
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           x.fecha_emision, x.fecha_vencimiento,
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN v.anulado = TRUE THEN 'ANULADA' WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE x.saldo_pendiente > 0.005
      AND x.fecha_vencimiento < CURDATE()
    ORDER BY x.fecha_vencimiento;
END$$

DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_todas_con_detalle$$
CREATE PROCEDURE sp_cuenta_por_cobrar_listar_todas_con_detalle()
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           x.fecha_emision, x.fecha_vencimiento,
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN v.anulado = TRUE THEN 'ANULADA' WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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

-- RNF002: anular un cobro no lo borra, lo marca como anulado.
-- La reversion del saldo de la cuenta y el egreso compensatorio en caja los hace el BO en la misma transaccion.
DROP PROCEDURE IF EXISTS sp_cobro_eliminar$$
CREATE PROCEDURE sp_cobro_eliminar(IN p_id INT)
BEGIN
    UPDATE cobro
    SET anulado = TRUE,
        fecha_anulacion = NOW()
    WHERE id = p_id AND anulado = FALSE;
END$$

DROP PROCEDURE IF EXISTS sp_cobro_obtener$$
CREATE PROCEDURE sp_cobro_obtener(IN p_id INT)
BEGIN
    SELECT co.id AS cobro_id, co.id_cuenta_por_cobrar AS cuenta_por_cobrar_id, co.fecha_cobro,
           co.medio_pago, co.monto, co.referencia, co.fecha_registro, co.anulado, co.fecha_anulacion,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos
    FROM cobro co
    INNER JOIN usuario u ON u.id = co.id_usuario_registro
    WHERE co.id = p_id;
END$$

DROP PROCEDURE IF EXISTS sp_cobro_listar$$
CREATE PROCEDURE sp_cobro_listar()
BEGIN
    SELECT co.id AS cobro_id, co.id_cuenta_por_cobrar AS cuenta_por_cobrar_id, co.fecha_cobro,
           co.medio_pago, co.monto, co.referencia, co.fecha_registro, co.anulado, co.fecha_anulacion,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos
    FROM cobro co
    INNER JOIN usuario u ON u.id = co.id_usuario_registro
    ORDER BY co.fecha_cobro DESC, co.id DESC;
END$$

DROP PROCEDURE IF EXISTS sp_cobro_listar_por_cuenta_por_cobrar$$
CREATE PROCEDURE sp_cobro_listar_por_cuenta_por_cobrar(IN p_id_cuenta_por_cobrar INT)
BEGIN
    SELECT co.id AS cobro_id, co.id_cuenta_por_cobrar AS cuenta_por_cobrar_id, co.fecha_cobro,
           co.medio_pago, co.monto, co.referencia, co.fecha_registro, co.anulado, co.fecha_anulacion,
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
           co.medio_pago, co.monto, co.referencia, co.fecha_registro, co.anulado, co.fecha_anulacion,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos
    FROM cobro co
    INNER JOIN cuenta_por_cobrar x ON x.id = co.id_cuenta_por_cobrar
    INNER JOIN usuario u ON u.id = co.id_usuario_registro
    WHERE x.id_cliente = p_id_cliente
    ORDER BY co.fecha_cobro, co.id;
END$$

-- ============================================================
-- H10: BUSQUEDAS FILTRADAS EN LA BASE (antes se traia toda la tabla y se filtraba en Java)
-- ============================================================

-- CompensacionCaja.buscar: movimientos de caja de un documento (VENTA, COBRO, NOTA_CREDITO...)
DROP PROCEDURE IF EXISTS sp_movimiento_caja_buscar_por_documento$$
CREATE PROCEDURE sp_movimiento_caja_buscar_por_documento(
    IN p_documento_origen VARCHAR(50),
    IN p_id_documento_origen INT
)
BEGIN
    SELECT m.id AS mov_id, m.id_caja AS caja_id, m.tipo, m.fecha_movimiento, m.medio_pago,
           m.monto, m.concepto, m.documento_origen, m.id_documento_origen,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos, u.correo
    FROM movimiento_caja m
    INNER JOIN usuario u ON u.id = m.id_usuario_registro
    WHERE m.documento_origen = p_documento_origen
      AND m.id_documento_origen = p_id_documento_origen
    ORDER BY m.fecha_movimiento, m.id;
END$$

-- RF010: ventas al contado vigentes que todavia no tienen su cobro en caja, con su cliente y su
-- comprobante vigente (FACTURA o BOLETA). Reemplaza la carga de todas las ventas, clientes y comprobantes.
DROP PROCEDURE IF EXISTS sp_caja_listar_ventas_por_cobrar_contado$$
CREATE PROCEDURE sp_caja_listar_ventas_por_cobrar_contado()
BEGIN
    SELECT v.id AS venta_id, v.numero AS venta_numero, v.fecha_emision, v.total, v.condicion_pago,
           cl.id AS cliente_id, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           cl.razon_social, cl.numero_documento,
           c.id AS comprobante_id, c.tipo AS comprobante_tipo, c.serie AS comprobante_serie,
           c.numero AS comprobante_numero, c.estado AS comprobante_estado
    FROM venta v
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    LEFT JOIN comprobante c ON c.id = (
        SELECT MIN(c2.id) FROM comprobante c2
        WHERE c2.id_venta = v.id AND c2.tipo IN ('FACTURA', 'BOLETA') AND c2.estado <> 'ANULADO')
    WHERE v.condicion_pago = 'CONTADO'
      AND v.anulado = FALSE
      AND NOT EXISTS (SELECT 1 FROM movimiento_caja m
                      WHERE m.documento_origen = 'VENTA' AND m.id_documento_origen = v.id)
    ORDER BY v.fecha_emision, v.id;
END$$

-- RF009: cobros con fecha de cobro dentro del rango (incluye ambos extremos)
DROP PROCEDURE IF EXISTS sp_cobro_buscar_por_fechas$$
CREATE PROCEDURE sp_cobro_buscar_por_fechas(
    IN p_fecha_inicio DATE,
    IN p_fecha_fin DATE
)
BEGIN
    SELECT co.id AS cobro_id, co.id_cuenta_por_cobrar AS cuenta_por_cobrar_id, co.fecha_cobro,
           co.medio_pago, co.monto, co.referencia, co.fecha_registro, co.anulado, co.fecha_anulacion,
           u.id AS usuario_id, u.nombre_usuario, u.nombres, u.apellidos
    FROM cobro co
    INNER JOIN usuario u ON u.id = co.id_usuario_registro
    WHERE co.fecha_cobro BETWEEN p_fecha_inicio AND p_fecha_fin
    ORDER BY co.fecha_cobro DESC, co.id DESC;
END$$

-- RF009: cuentas cuyo comprobante vigente (FACTURA o BOLETA) contiene el texto, por ejemplo F001-00000123
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_buscar_por_comprobante$$
CREATE PROCEDURE sp_cuenta_por_cobrar_buscar_por_comprobante(IN p_texto VARCHAR(20))
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           x.fecha_emision, x.fecha_vencimiento,
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN v.anulado = TRUE THEN 'ANULADA' WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE EXISTS (SELECT 1 FROM comprobante c
                  WHERE c.id_venta = x.id_venta
                    AND c.tipo IN ('FACTURA', 'BOLETA') AND c.estado <> 'ANULADO'
                    AND UPPER(CONCAT(c.serie, '-', c.numero)) LIKE CONCAT('%', UPPER(TRIM(p_texto)), '%'))
    ORDER BY x.fecha_emision DESC;
END$$

-- RF009: cuentas por rango de fechas; p_por_vencimiento = FALSE usa la emision y TRUE el vencimiento
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_buscar_por_fechas$$
CREATE PROCEDURE sp_cuenta_por_cobrar_buscar_por_fechas(
    IN p_fecha_inicio DATE,
    IN p_fecha_fin DATE,
    IN p_por_vencimiento BOOLEAN
)
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           x.fecha_emision, x.fecha_vencimiento,
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN v.anulado = TRUE THEN 'ANULADA' WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE (p_por_vencimiento = FALSE AND x.fecha_emision BETWEEN p_fecha_inicio AND p_fecha_fin)
       OR (p_por_vencimiento = TRUE AND x.fecha_vencimiento BETWEEN p_fecha_inicio AND p_fecha_fin)
    ORDER BY x.fecha_emision DESC;
END$$

-- RF009: totales que ve el Administrador (saldo pendiente y saldo vencido), calculados en la base
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_resumen$$
CREATE PROCEDURE sp_cuenta_por_cobrar_resumen()
BEGIN
    SELECT COALESCE(SUM(x.saldo_pendiente), 0) AS total_saldo_pendiente,
           COALESCE(SUM(CASE WHEN x.fecha_vencimiento < CURDATE() THEN x.saldo_pendiente ELSE 0 END), 0) AS total_saldo_vencido,
           COUNT(*) AS cuentas_con_saldo,
           COALESCE(SUM(CASE WHEN x.fecha_vencimiento < CURDATE() THEN 1 ELSE 0 END), 0) AS cuentas_vencidas
    FROM cuenta_por_cobrar x
    WHERE x.saldo_pendiente > 0.005;
END$$

-- RF009: comprobante vigente (FACTURA o BOLETA) de las ventas de una lista de cuentas.
-- p_ids_venta: ids separados por coma ("4,8,15"); NULL = las de todas las cuentas por cobrar
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_comprobantes_vigentes$$
CREATE PROCEDURE sp_cuenta_por_cobrar_comprobantes_vigentes(IN p_ids_venta TEXT)
BEGIN
    SELECT c.id AS comprobante_id, c.id_venta AS venta_id, c.tipo, c.serie, c.numero, c.estado
    FROM comprobante c
    INNER JOIN cuenta_por_cobrar x ON x.id_venta = c.id_venta
    WHERE c.tipo IN ('FACTURA', 'BOLETA') AND c.estado <> 'ANULADO'
      AND (p_ids_venta IS NULL OR FIND_IN_SET(c.id_venta, p_ids_venta) > 0)
    ORDER BY c.id_venta, c.id;
END$$


-- ============================================================
-- TRAZABILIDAD DE ANULACIONES Y NOTAS DE CREDITO (RF009, RF013, RF015, RNF001)
-- registro_anulacion guarda quien, cuando y por que se anulo un cobro, una nota de credito o una cuenta
-- por cobrar. Lo que se anulo ANTES de existir la tabla se reconstruye con lo que ya habia en la base:
--   cobro      -> cobro.fecha_anulacion y el egreso compensatorio de la caja (quien lo registro)
--   nota       -> el movimiento de Kardex que revirtio la devolucion (quien y cuando)
--   cuenta     -> venta.fecha_anulacion y venta.motivo_anulacion
-- ============================================================

DROP PROCEDURE IF EXISTS sp_anulacion_registrar$$
CREATE PROCEDURE sp_anulacion_registrar(
    IN p_tipo_documento VARCHAR(30),
    IN p_id_documento INT,
    IN p_documento VARCHAR(40),
    IN p_id_cuenta_por_cobrar INT,
    IN p_monto DECIMAL(12,2),
    IN p_motivo VARCHAR(200),
    IN p_id_usuario INT
)
BEGIN
    INSERT INTO registro_anulacion (tipo_documento, id_documento, documento, id_cuenta_por_cobrar,
                                    monto, motivo, id_usuario, fecha)
    VALUES (p_tipo_documento, p_id_documento, p_documento, p_id_cuenta_por_cobrar,
            p_monto, p_motivo, p_id_usuario, NOW());
END$$

-- RF009 + RF013: todo lo que le paso a una cuenta por cobrar, en orden (el "kardex" de la cuenta).
-- efecto_saldo: cuanto sube (+) o baja (-) la deuda con ese evento; NULL si no cambia el saldo.
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_historial$$
CREATE PROCEDURE sp_cuenta_por_cobrar_historial(IN p_id_cuenta INT)
BEGIN
    SELECT h.fecha, h.evento, h.documento, h.detalle, h.efecto_saldo, h.usuario
    FROM (
        -- 1. la venta al credito genera la cuenta
        SELECT v.fecha_registro AS fecha, 1 AS orden, 'CUENTA_GENERADA' AS evento, v.numero AS documento,
               CONCAT('Venta al crédito a ', v.plazo_credito_dias, ' días') AS detalle,
               x.monto_original AS efecto_saldo,
               CONCAT(u.nombres, ' ', u.apellidos) AS usuario
        FROM cuenta_por_cobrar x
        INNER JOIN venta v ON v.id = x.id_venta
        INNER JOIN usuario u ON u.id = v.id_usuario_registro
        WHERE x.id = p_id_cuenta

        UNION ALL
        -- 2. cada nota de credito sobre la venta (rebaja la deuda)
        SELECT nc.fecha_registro, 2, 'NOTA_CREDITO', CONCAT(nc.serie, '-', nc.numero),
               CONCAT('Devolución: ', COALESCE(nc.motivo, '—'),
                      CASE WHEN ad.id IS NULL THEN '' ELSE CONCAT(' · autorizó ', ad.nombres, ' ', ad.apellidos) END),
               -nc.total, CASE WHEN ad.id IS NULL THEN NULL ELSE CONCAT(ad.nombres, ' ', ad.apellidos) END
        FROM cuenta_por_cobrar x
        INNER JOIN comprobante nc ON nc.id_venta = x.id_venta AND nc.tipo = 'NOTA_CREDITO'
        LEFT JOIN usuario ad ON ad.id = nc.id_administrador_autoriza
        WHERE x.id = p_id_cuenta

        UNION ALL
        -- 3. cada cobro (vigente o anulado: al registrarse si bajo la deuda)
        SELECT co.fecha_registro, 3, 'COBRO', CONCAT('Cobro N.º ', co.id),
               CONCAT(REPLACE(co.medio_pago, '_', ' '), CASE WHEN co.referencia IS NULL OR co.referencia = '' THEN ''
                      ELSE CONCAT(' · ref. ', co.referencia) END),
               -co.monto, CONCAT(u.nombres, ' ', u.apellidos)
        FROM cobro co
        INNER JOIN usuario u ON u.id = co.id_usuario_registro
        WHERE co.id_cuenta_por_cobrar = p_id_cuenta

        UNION ALL
        -- 4. cobros anulados (la deuda vuelve a subir)
        SELECT COALESCE(ra.fecha, co.fecha_anulacion), 4, 'COBRO_ANULADO', CONCAT('Cobro N.º ', co.id),
               CONCAT('Motivo: ', COALESCE(ra.motivo, 'no se registró (anulado antes de la trazabilidad)')),
               co.monto,
               COALESCE(CONCAT(ur.nombres, ' ', ur.apellidos),
                        (SELECT CONCAT(ue.nombres, ' ', ue.apellidos) FROM movimiento_caja m
                         INNER JOIN usuario ue ON ue.id = m.id_usuario_registro
                         WHERE m.documento_origen = 'ANULACION_COBRO' AND m.id_documento_origen = co.id
                         ORDER BY m.id LIMIT 1))
        FROM cobro co
        LEFT JOIN registro_anulacion ra ON ra.tipo_documento = 'COBRO' AND ra.id_documento = co.id
        LEFT JOIN usuario ur ON ur.id = ra.id_usuario
        WHERE co.id_cuenta_por_cobrar = p_id_cuenta AND co.anulado = TRUE

        UNION ALL
        -- 5. notas de credito anuladas (la deuda vuelve a subir)
        SELECT COALESCE(ra.fecha, (SELECT MIN(mi.fecha_movimiento) FROM movimiento_inventario mi
                                   WHERE mi.id_comprobante = nc.id AND mi.tipo = 'SALIDA_REVERSION_DEVOLUCION')),
               5, 'NOTA_CREDITO_ANULADA', CONCAT(nc.serie, '-', nc.numero),
               CONCAT('Motivo: ', COALESCE(ra.motivo, 'no se registró (anulada antes de la trazabilidad)')),
               nc.total,
               COALESCE(CONCAT(ur.nombres, ' ', ur.apellidos),
                        (SELECT CONCAT(ui.nombres, ' ', ui.apellidos) FROM movimiento_inventario mi
                         INNER JOIN usuario ui ON ui.id = mi.id_usuario_registro
                         WHERE mi.id_comprobante = nc.id AND mi.tipo = 'SALIDA_REVERSION_DEVOLUCION'
                         ORDER BY mi.id LIMIT 1))
        FROM cuenta_por_cobrar x
        INNER JOIN comprobante nc ON nc.id_venta = x.id_venta AND nc.tipo = 'NOTA_CREDITO' AND nc.estado = 'ANULADO'
        LEFT JOIN registro_anulacion ra ON ra.tipo_documento = 'NOTA_CREDITO' AND ra.id_documento = nc.id
        LEFT JOIN usuario ur ON ur.id = ra.id_usuario
        WHERE x.id = p_id_cuenta

        UNION ALL
        -- 6. la cuenta se anula porque se anulo su venta (el saldo queda en 0)
        SELECT COALESCE(ra.fecha, v.fecha_anulacion), 6, 'CUENTA_ANULADA', v.numero,
               CONCAT('Venta anulada. Motivo: ', COALESCE(ra.motivo, v.motivo_anulacion, '—')),
               NULL,
               CASE WHEN ur.id IS NULL THEN NULL ELSE CONCAT(ur.nombres, ' ', ur.apellidos) END
        FROM cuenta_por_cobrar x
        INNER JOIN venta v ON v.id = x.id_venta
        LEFT JOIN registro_anulacion ra ON ra.tipo_documento = 'CUENTA_POR_COBRAR' AND ra.id_documento = x.id
        LEFT JOIN usuario ur ON ur.id = ra.id_usuario
        WHERE x.id = p_id_cuenta AND (x.estado = 'ANULADA' OR v.estado = 'ANULADA')
    ) h
    ORDER BY h.fecha IS NULL, h.fecha, h.orden;
END$$

-- RF013: las notas de credito de la venta de una cuenta por cobrar, con su estado y, si se anularon,
-- quien, cuando y por que
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_notas_credito$$
CREATE PROCEDURE sp_cuenta_por_cobrar_notas_credito(IN p_id_cuenta INT)
BEGIN
    SELECT nc.id, CONCAT(nc.serie, '-', nc.numero) AS nota, nc.fecha_emision, nc.fecha_registro,
           CONCAT(o.serie, '-', o.numero) AS comprobante, nc.motivo, nc.sub_total, nc.igv, nc.total, nc.estado,
           CASE WHEN ad.id IS NULL THEN NULL ELSE CONCAT(ad.nombres, ' ', ad.apellidos) END AS autorizo,
           COALESCE(ra.fecha, (SELECT MIN(mi.fecha_movimiento) FROM movimiento_inventario mi
                               WHERE mi.id_comprobante = nc.id AND mi.tipo = 'SALIDA_REVERSION_DEVOLUCION')) AS fecha_anulacion,
           ra.motivo AS motivo_anulacion,
           CASE WHEN ur.id IS NULL THEN NULL ELSE CONCAT(ur.nombres, ' ', ur.apellidos) END AS usuario_anulacion
    FROM cuenta_por_cobrar x
    INNER JOIN comprobante nc ON nc.id_venta = x.id_venta AND nc.tipo = 'NOTA_CREDITO'
    LEFT JOIN comprobante o ON o.id = nc.id_comprobante_relacionado
    LEFT JOIN usuario ad ON ad.id = nc.id_administrador_autoriza
    LEFT JOIN registro_anulacion ra ON ra.tipo_documento = 'NOTA_CREDITO' AND ra.id_documento = nc.id
    LEFT JOIN usuario ur ON ur.id = ra.id_usuario
    WHERE x.id = p_id_cuenta
    ORDER BY nc.fecha_registro, nc.id;
END$$

-- RF015 (reporte de caja): anulaciones del periodo. Cobros anulados, notas de credito anuladas y cuentas por
-- cobrar anuladas (por la anulacion de su venta), con documento, cliente, monto, motivo, usuario y fecha.
DROP PROCEDURE IF EXISTS sp_reporte_anulaciones$$
CREATE PROCEDURE sp_reporte_anulaciones(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT a.fecha, a.tipo, a.documento, a.venta, a.cliente, a.monto, a.motivo, a.usuario
    FROM (
        SELECT COALESCE(ra.fecha, co.fecha_anulacion) AS fecha, 'COBRO' AS tipo, CONCAT('Cobro N.º ', co.id) AS documento,
               v.numero AS venta,
               CASE WHEN cl.tipo_persona = 'JURIDICA' THEN cl.razon_social ELSE CONCAT(cl.nombres, ' ', cl.apellidos) END AS cliente,
               co.monto,
               COALESCE(ra.motivo, 'No registrado') AS motivo,
               COALESCE(CONCAT(ur.nombres, ' ', ur.apellidos),
                        (SELECT CONCAT(ue.nombres, ' ', ue.apellidos) FROM movimiento_caja m
                         INNER JOIN usuario ue ON ue.id = m.id_usuario_registro
                         WHERE m.documento_origen = 'ANULACION_COBRO' AND m.id_documento_origen = co.id
                         ORDER BY m.id LIMIT 1), '—') AS usuario
        FROM cobro co
        INNER JOIN cuenta_por_cobrar x ON x.id = co.id_cuenta_por_cobrar
        INNER JOIN venta v ON v.id = x.id_venta
        INNER JOIN cliente cl ON cl.id = x.id_cliente
        LEFT JOIN registro_anulacion ra ON ra.tipo_documento = 'COBRO' AND ra.id_documento = co.id
        LEFT JOIN usuario ur ON ur.id = ra.id_usuario
        WHERE co.anulado = TRUE

        UNION ALL
        SELECT COALESCE(ra.fecha, (SELECT MIN(mi.fecha_movimiento) FROM movimiento_inventario mi
                                   WHERE mi.id_comprobante = nc.id AND mi.tipo = 'SALIDA_REVERSION_DEVOLUCION')),
               'NOTA_CREDITO', CONCAT(nc.serie, '-', nc.numero), v.numero,
               CASE WHEN cl.tipo_persona = 'JURIDICA' THEN cl.razon_social ELSE CONCAT(cl.nombres, ' ', cl.apellidos) END,
               nc.total,
               COALESCE(ra.motivo, 'No registrado'),
               COALESCE(CONCAT(ur.nombres, ' ', ur.apellidos),
                        (SELECT CONCAT(ui.nombres, ' ', ui.apellidos) FROM movimiento_inventario mi
                         INNER JOIN usuario ui ON ui.id = mi.id_usuario_registro
                         WHERE mi.id_comprobante = nc.id AND mi.tipo = 'SALIDA_REVERSION_DEVOLUCION'
                         ORDER BY mi.id LIMIT 1), '—')
        FROM comprobante nc
        INNER JOIN venta v ON v.id = nc.id_venta
        INNER JOIN cliente cl ON cl.id = v.id_cliente
        LEFT JOIN registro_anulacion ra ON ra.tipo_documento = 'NOTA_CREDITO' AND ra.id_documento = nc.id
        LEFT JOIN usuario ur ON ur.id = ra.id_usuario
        WHERE nc.tipo = 'NOTA_CREDITO' AND nc.estado = 'ANULADO'

        UNION ALL
        SELECT COALESCE(ra.fecha, v.fecha_anulacion), 'CUENTA_POR_COBRAR', CONCAT('Cuenta de ', v.numero), v.numero,
               CASE WHEN cl.tipo_persona = 'JURIDICA' THEN cl.razon_social ELSE CONCAT(cl.nombres, ' ', cl.apellidos) END,
               x.monto_original,
               COALESCE(ra.motivo, v.motivo_anulacion, 'No registrado'),
               COALESCE(CONCAT(ur.nombres, ' ', ur.apellidos), '—')
        FROM cuenta_por_cobrar x
        INNER JOIN venta v ON v.id = x.id_venta
        INNER JOIN cliente cl ON cl.id = x.id_cliente
        LEFT JOIN registro_anulacion ra ON ra.tipo_documento = 'CUENTA_POR_COBRAR' AND ra.id_documento = x.id
        LEFT JOIN usuario ur ON ur.id = ra.id_usuario
        WHERE x.estado = 'ANULADA' OR v.estado = 'ANULADA'
    ) a
    WHERE a.fecha >= p_desde AND a.fecha < DATE_ADD(p_hasta, INTERVAL 1 DAY)
    ORDER BY a.fecha DESC;
END$$

-- RF015 (reporte comercial) + RF013: notas de credito emitidas en el periodo, con el comprobante que corrigen,
-- el cliente, el motivo, el monto, su estado y quien las autorizo (seguimiento de las notas)
DROP PROCEDURE IF EXISTS sp_reporte_notas_credito$$
CREATE PROCEDURE sp_reporte_notas_credito(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT nc.id, CONCAT(nc.serie, '-', nc.numero) AS nota, nc.fecha_emision,
           CONCAT(o.serie, '-', o.numero) AS comprobante, v.numero AS venta, v.condicion_pago,
           CASE WHEN cl.tipo_persona = 'JURIDICA' THEN cl.razon_social ELSE CONCAT(cl.nombres, ' ', cl.apellidos) END AS cliente,
           nc.motivo, nc.total, nc.estado,
           CASE WHEN ad.id IS NULL THEN '—' ELSE CONCAT(ad.nombres, ' ', ad.apellidos) END AS autorizo,
           x.id AS id_cuenta_por_cobrar
    FROM comprobante nc
    INNER JOIN venta v ON v.id = nc.id_venta
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    LEFT JOIN comprobante o ON o.id = nc.id_comprobante_relacionado
    LEFT JOIN usuario ad ON ad.id = nc.id_administrador_autoriza
    LEFT JOIN cuenta_por_cobrar x ON x.id_venta = v.id
    WHERE nc.tipo = 'NOTA_CREDITO' AND nc.fecha_emision BETWEEN p_desde AND p_hasta
    ORDER BY nc.fecha_emision DESC, nc.id DESC;
END$$


DELIMITER ;
