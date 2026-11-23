-- =========================================================================
-- MIGRACION 16 - OBSERVACIONES DEL JP (Adriano): seguimiento de notas de credito y trazabilidad de anulaciones
-- Ejecutar UNA vez en la base que usa GlassFish (local o AWS), en MySQL Workbench (rayo). Re-ejecutable.
-- No borra datos.
--
--   1. Tabla registro_anulacion: quien, cuando y por que se anulo un cobro, una nota de credito o una cuenta por
--      cobrar (RNF001). Lo anulado antes de esta migracion se reconstruye con lo que ya habia en la base.
--   2. Procedimientos nuevos (tambien estan en 2_procedimientos/05_SP_adriano.sql):
--        sp_anulacion_registrar, sp_cuenta_por_cobrar_historial, sp_cuenta_por_cobrar_notas_credito,
--        sp_reporte_anulaciones, sp_reporte_notas_credito
--   3. Los dos procedimientos para ANULAR una nota de credito, que solo estaban en la migracion 15 (sin ellos la
--      anulacion fallaba con "PROCEDURE sigmeta.sp_movimiento_listar_comprobante does not exist"):
--        sp_movimiento_listar_comprobante, sp_movimiento_reversion_nota_credito (ahora tambien en 07_SP_reforma.sql)
--   4. Lo que la base tampoco tenia si no se corrio la migracion 14 (recepcion de compras) y un procedimiento que
--      el Java llamaba y no existia en ningun script: tabla costo_recepcion_historico, sp_costo_recepcion_guardar,
--      sp_costo_recepcion_obtener y sp_proveedor_activar (reactivar un proveedor).
--
-- DESPUES: volver a desplegar el .war (mvn clean install) y reiniciar GlassFish.
-- =========================================================================
USE sigmeta;
SET NAMES utf8mb4;
-- los procedimientos guardan el sql_mode con el que se crean (ver README: ONLY_FULL_GROUP_BY)
SET SESSION sql_mode = (SELECT REPLACE(@@SESSION.sql_mode, 'ONLY_FULL_GROUP_BY', ''));

-- Tabla: registro_anulacion
-- RNF001 / RF009 / RF013: quien, cuando y por que se anulo un cobro, una nota de credito o una cuenta por cobrar.
-- Una fila por documento anulado (no se borra ni se modifica). id_cuenta_por_cobrar enlaza la anulacion con la
-- cuenta afectada para mostrarla en su historial; NULL si la nota de credito era de una venta al contado.
CREATE TABLE IF NOT EXISTS registro_anulacion (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento       ENUM('COBRO', 'NOTA_CREDITO', 'CUENTA_POR_COBRAR') NOT NULL,
    id_documento         INT NOT NULL,
    documento            VARCHAR(40) NOT NULL,
    id_cuenta_por_cobrar INT NULL,
    monto                DECIMAL(12,2) NOT NULL,
    motivo               VARCHAR(200) NOT NULL,
    id_usuario           INT NOT NULL,
    fecha                DATETIME NOT NULL,
    CONSTRAINT uq_registro_anulacion_documento UNIQUE (tipo_documento, id_documento),
    CONSTRAINT fk_registro_anulacion_cuenta
        FOREIGN KEY (id_cuenta_por_cobrar) REFERENCES cuenta_por_cobrar(id),
    CONSTRAINT fk_registro_anulacion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id),
    INDEX ix_registro_anulacion_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- RF012 (migracion 14)
CREATE TABLE IF NOT EXISTS costo_recepcion_historico (
    id_movimiento_ingreso INT PRIMARY KEY,
    costo_anterior DECIMAL(12,2) NOT NULL,
    costo_posterior DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_costo_recepcion_movimiento
        FOREIGN KEY (id_movimiento_ingreso) REFERENCES movimiento_inventario(id),
    CONSTRAINT chk_costo_recepcion_anterior CHECK (costo_anterior >= 0),
    CONSTRAINT chk_costo_recepcion_posterior CHECK (costo_posterior >= 0)
) ENGINE=InnoDB;


DELIMITER $$
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

DROP PROCEDURE IF EXISTS sp_costo_recepcion_guardar$$
CREATE PROCEDURE sp_costo_recepcion_guardar(
    IN p_id_movimiento INT,
    IN p_costo_anterior DECIMAL(12,2),
    IN p_costo_posterior DECIMAL(12,2)
)
BEGIN
    IF p_costo_anterior IS NULL OR p_costo_posterior IS NULL
       OR p_costo_anterior < 0 OR p_costo_posterior < 0
       OR NOT EXISTS (
           SELECT 1 FROM movimiento_inventario
           WHERE id = p_id_movimiento AND tipo = 'INGRESO_COMPRA'
       ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Snapshot de costo de recepcion invalido';
    END IF;

    INSERT INTO costo_recepcion_historico
        (id_movimiento_ingreso, costo_anterior, costo_posterior)
    VALUES (p_id_movimiento, p_costo_anterior, p_costo_posterior);
END$$

DROP PROCEDURE IF EXISTS sp_costo_recepcion_obtener$$
CREATE PROCEDURE sp_costo_recepcion_obtener(IN p_id_movimiento INT)
BEGIN
    SELECT costo_anterior, costo_posterior
      FROM costo_recepcion_historico
     WHERE id_movimiento_ingreso = p_id_movimiento;
END$$

DROP PROCEDURE IF EXISTS sp_proveedor_activar$$
CREATE PROCEDURE sp_proveedor_activar(IN p_id_proveedor INT)
BEGIN
    UPDATE proveedor SET estado = TRUE WHERE id = p_id_proveedor;
END$$

DELIMITER ;

-- Revision: deben salir los 10 procedimientos
SELECT ROUTINE_NAME AS procedimiento FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = DATABASE() AND ROUTINE_NAME IN ('sp_anulacion_registrar', 'sp_cuenta_por_cobrar_historial',
      'sp_cuenta_por_cobrar_notas_credito', 'sp_reporte_anulaciones', 'sp_reporte_notas_credito',
      'sp_movimiento_listar_comprobante', 'sp_movimiento_reversion_nota_credito',
      'sp_costo_recepcion_guardar', 'sp_costo_recepcion_obtener', 'sp_proveedor_activar')
ORDER BY ROUTINE_NAME;
SELECT COUNT(*) AS anulaciones_registradas FROM registro_anulacion;
