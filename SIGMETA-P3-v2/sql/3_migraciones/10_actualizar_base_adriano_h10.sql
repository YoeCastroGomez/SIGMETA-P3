-- =========================================================================
-- MIGRACION 10 - ACTUALIZAR UNA BASE QUE YA EXISTE CON LO DE ADRIANO (H10)
-- Ejecutar UNA vez en la base que usa GlassFish (local o AWS), en MySQL Workbench:
-- abrir este archivo y ejecutarlo completo (rayo). Se puede volver a ejecutar sin problema.
--   1) Crea los 7 procedimientos nuevos de caja y cobranza (filtrado en la base).
--   2) Crea los indices que los hacen rapidos (si ya existen, no hace nada).
-- No borra ni cambia datos y no modifica ningun procedimiento que ya exista.
-- =========================================================================
USE sigmeta;

DELIMITER $$
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
           x.monto_original, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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
           x.monto_original, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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
DELIMITER ;

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

-- Comprobacion: debe mostrar 7 filas
SELECT routine_name AS procedimiento_creado FROM information_schema.routines
WHERE routine_schema = 'sigmeta' AND routine_name IN (
  'sp_movimiento_caja_buscar_por_documento', 'sp_caja_listar_ventas_por_cobrar_contado', 'sp_cobro_buscar_por_fechas',
  'sp_cuenta_por_cobrar_buscar_por_comprobante', 'sp_cuenta_por_cobrar_buscar_por_fechas', 'sp_cuenta_por_cobrar_resumen',
  'sp_cuenta_por_cobrar_comprobantes_vigentes');
