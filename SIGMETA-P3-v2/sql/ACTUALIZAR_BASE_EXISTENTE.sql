-- =========================================================================
-- ACTUALIZAR UNA BASE SIGMETA YA INSTALADA (por ejemplo la de AWS) EN UN SOLO PASO
-- Archivo GENERADO: une, en orden, las migraciones 12 y 13 y todos los procedimientos (00 a 07).
-- No borra la base ni los datos: agrega columnas y una tabla, corrige montos y recarga los procedimientos.
-- Se puede volver a ejecutar sin problema.
--
-- Uso en MySQL Workbench: abrir este archivo y ejecutarlo completo (rayo). Al final no debe haber errores.
-- Requisito: la base ya tiene las migraciones 01 a 11 (si se instalo con el README de este repositorio, ya las tiene).
-- Para una base NUEVA no se usa este archivo: se sigue el README (1_base/sigmeta.sql y despues 2_procedimientos).
--
-- Si se cambia alguna migracion o procedimiento, hay que volver a generar este archivo.
-- =========================================================================
-- los procedimientos guardan el sql_mode con el que se crean (ver README: ONLY_FULL_GROUP_BY)
SET SESSION sql_mode = (SELECT REPLACE(@@SESSION.sql_mode, 'ONLY_FULL_GROUP_BY', ''));


-- #########################################################################
-- 3_migraciones/12_auditoria_cxc_comprobante_caja.sql
-- #########################################################################
USE sigmeta;

-- =========================================================================
-- MIGRACION 12 - AUDITORIA DE DISENO (docs/AUDITORIA_DISENO_ERP.md, prioridades 1 a 3)
-- Ejecutar UNA vez en la base que usa GlassFish (local o AWS), en MySQL Workbench (rayo).
-- Se puede volver a ejecutar sin problema: cada paso revisa si ya se hizo.
--
--   1. Cuenta por cobrar: el monto original (total de la venta) ya no se sobrescribe. Lo que rebajan las
--      notas de credito va a la nueva columna monto_acreditado. Saldo = original - acreditado - pagado.
--      Las cuentas existentes se corrigen: se recupera su monto original desde la venta.
--   2. Comprobante: foto de lo impreso. Columnas del cliente en comprobante y tabla detalle_comprobante con
--      las lineas (codigo y nombre del producto). Se completa para todas las facturas y boletas existentes.
--   3. Caja: ningun SQL nuevo de tablas; el procedimiento sp_movimiento_caja_eliminar ahora rechaza el borrado.
--
-- DESPUES de esta migracion recargar los procedimientos (pasos 2 a 9 del README), al menos:
--   2_procedimientos/04_SP_sebastian.sql  (notas de credito y foto del comprobante)
--   2_procedimientos/05_SP_adriano.sql    (consultas de cuentas por cobrar y borrado de movimientos de caja)
--   2_procedimientos/07_SP_reforma.sql    (siempre al final)
-- =========================================================================
USE sigmeta;

DELIMITER $$

DROP PROCEDURE IF EXISTS tmp_migracion_12$$
CREATE PROCEDURE tmp_migracion_12()
BEGIN
    -- 1. cuenta_por_cobrar.monto_acreditado
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cuenta_por_cobrar'
                     AND COLUMN_NAME = 'monto_acreditado') THEN
        ALTER TABLE cuenta_por_cobrar
            ADD COLUMN monto_acreditado DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER monto_original;
    END IF;

    -- 2. comprobante: foto del cliente impreso
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comprobante'
                     AND COLUMN_NAME = 'cliente_nombre') THEN
        ALTER TABLE comprobante
            ADD COLUMN cliente_nombre VARCHAR(200) NULL,
            ADD COLUMN cliente_tipo_documento VARCHAR(30) NULL,
            ADD COLUMN cliente_numero_documento VARCHAR(20) NULL,
            ADD COLUMN cliente_direccion VARCHAR(200) NULL;
    END IF;
END$$

DELIMITER ;

CALL tmp_migracion_12();
DROP PROCEDURE IF EXISTS tmp_migracion_12;

-- 2. lineas impresas de cada factura y boleta
CREATE TABLE IF NOT EXISTS detalle_comprobante (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_comprobante          INT NOT NULL,
    numero_linea            INT NOT NULL,
    codigo                  VARCHAR(50) NOT NULL,
    descripcion             VARCHAR(150) NOT NULL,
    unidad                  VARCHAR(20) NULL,
    cantidad                DECIMAL(12,3) NOT NULL,
    precio_unitario         DECIMAL(12,2) NOT NULL,
    descuento               DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    importe                 DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_detalle_comprobante_comprobante
        FOREIGN KEY (id_comprobante) REFERENCES comprobante(id),
    UNIQUE KEY uq_detalle_comprobante_linea (id_comprobante, numero_linea)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET SQL_SAFE_UPDATES = 0;

-- 1. Cuentas de ventas vigentes cuyo monto original rebajaron las notas de credito: el monto original vuelve a
--    ser el total de la venta y la diferencia pasa a monto_acreditado. El saldo no cambia.
UPDATE cuenta_por_cobrar x
INNER JOIN venta v ON v.id = x.id_venta
SET x.monto_acreditado = v.total - x.monto_original,
    x.monto_original = v.total
WHERE v.anulado = FALSE
  AND x.monto_acreditado = 0
  AND x.monto_original < v.total;

-- 1. Cuentas de ventas anuladas que quedaron con monto original 0: recuperan el total de la venta (saldo 0)
UPDATE cuenta_por_cobrar x
INNER JOIN venta v ON v.id = x.id_venta
SET x.monto_original = v.total,
    x.saldo_pendiente = 0
WHERE v.anulado = TRUE
  AND x.monto_original = 0
  AND x.monto_pagado = 0;

-- 2. Foto de lo impreso para las facturas y boletas ya emitidas. Se toma con los datos de hoy del cliente y
--    del producto (lo mas cercano que queda a lo que se imprimio); desde ahora cada emision guarda la suya.
UPDATE comprobante c
INNER JOIN venta v ON v.id = c.id_venta
INNER JOIN cliente cl ON cl.id = v.id_cliente
SET c.cliente_nombre = CASE WHEN cl.tipo_persona = 'JURIDICA' THEN cl.razon_social
                            ELSE CONCAT(cl.nombres, ' ', cl.apellidos) END,
    c.cliente_tipo_documento = cl.tipo_documento,
    c.cliente_numero_documento = cl.numero_documento,
    c.cliente_direccion = cl.direccion
WHERE c.tipo IN ('FACTURA', 'BOLETA') AND c.cliente_nombre IS NULL;

INSERT INTO detalle_comprobante (id_comprobante, numero_linea, codigo, descripcion, unidad,
                                 cantidad, precio_unitario, descuento, importe)
SELECT c.id, d.numero_linea, p.codigo_interno, p.nombre, p.unidad_venta,
       d.cantidad, d.precio_unitario, d.descuento, d.importe
FROM comprobante c
INNER JOIN detalle_venta d ON d.id_venta = c.id_venta
INNER JOIN producto p ON p.id = d.id_producto
WHERE c.tipo IN ('FACTURA', 'BOLETA')
  AND NOT EXISTS (SELECT 1 FROM detalle_comprobante x WHERE x.id_comprobante = c.id)
ORDER BY c.id, d.numero_linea;

SET SQL_SAFE_UPDATES = 1;

-- Revision: el saldo de cada cuenta debe ser original - acreditado - pagado (esta consulta no debe traer filas
-- de ventas vigentes)
SELECT x.id, v.numero, x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente
FROM cuenta_por_cobrar x
INNER JOIN venta v ON v.id = x.id_venta
WHERE v.anulado = FALSE
  AND ABS(x.monto_original - x.monto_acreditado - x.monto_pagado - x.saldo_pendiente) > 0.005;


-- #########################################################################
-- 3_migraciones/13_anulado_derivado_del_estado.sql
-- #########################################################################
USE sigmeta;

-- =========================================================================
-- MIGRACION 13 - AUDITORIA DE DISENO (docs/AUDITORIA_DISENO_ERP.md, prioridad 4): UN SOLO ESTADO
-- Ejecutar UNA vez en la base que usa GlassFish (local o AWS), en MySQL Workbench (rayo), DESPUES de la 12.
-- Se puede volver a ejecutar sin problema: una tabla ya convertida no se toca.
--
-- venta, cotizacion, compra y orden_compra_cliente guardaban "anulado" dos veces: la columna anulado
-- (TRUE/FALSE) y el valor ANULADA de estado. Nada obligaba a que coincidieran. Desde esta migracion
-- anulado es una columna CALCULADA de estado: anulado = (estado = 'ANULADA'). La base la mantiene sola, no
-- se puede escribir y nunca contradice al estado. Las consultas que filtran por anulado siguen igual.
--
-- DESPUES recargar los procedimientos que ya no escriben anulado (pasos 2 a 9 del README), al menos:
--   2_procedimientos/02_SP_claudia.sql, 03_SP_daniel.sql, 04_SP_sebastian.sql y 07_SP_reforma.sql (al final)
-- Con la columna calculada, la version ANTERIOR de esos procedimientos falla al registrar o anular
-- (no se puede escribir en una columna calculada): recargarlos es obligatorio.
-- =========================================================================
USE sigmeta;

-- 1. Revision: documentos donde las dos columnas se contradicen (normalmente no trae filas)
SELECT 'venta' AS tabla, id, numero, anulado, estado FROM venta
 WHERE anulado <> (estado = 'ANULADA')
UNION ALL SELECT 'cotizacion', id, numero, anulado, estado FROM cotizacion
 WHERE anulado <> (estado = 'ANULADA')
UNION ALL SELECT 'compra', id, numero, anulado, estado FROM compra
 WHERE anulado <> (estado = 'ANULADA')
UNION ALL SELECT 'orden_compra_cliente', id, numero, anulado, estado FROM orden_compra_cliente
 WHERE anulado <> (estado = 'ANULADA');

DELIMITER $$

DROP PROCEDURE IF EXISTS tmp_migracion_13$$
CREATE PROCEDURE tmp_migracion_13(IN p_tabla VARCHAR(64))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_tabla
                     AND COLUMN_NAME = 'anulado' AND EXTRA LIKE '%GENERATED%') THEN
        -- 2. Si hay contradicciones, manda la anulacion registrada (tiene motivo y fecha): el estado pasa a ANULADA
        SET @sql = CONCAT('UPDATE ', p_tabla, ' SET estado = ''ANULADA'' WHERE anulado = TRUE AND estado <> ''ANULADA''');
        PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
        -- 3. anulado pasa a ser calculada a partir de estado
        SET @sql = CONCAT('ALTER TABLE ', p_tabla, ' MODIFY COLUMN anulado BOOLEAN ',
                          'GENERATED ALWAYS AS (estado = ''ANULADA'') STORED NOT NULL');
        PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
END$$

DELIMITER ;

SET SQL_SAFE_UPDATES = 0;
CALL tmp_migracion_13('venta');
CALL tmp_migracion_13('cotizacion');
CALL tmp_migracion_13('compra');
CALL tmp_migracion_13('orden_compra_cliente');
SET SQL_SAFE_UPDATES = 1;
DROP PROCEDURE IF EXISTS tmp_migracion_13;

-- Revision: las cuatro columnas deben decir VIRTUAL o STORED GENERATED en EXTRA
SELECT TABLE_NAME, COLUMN_NAME, EXTRA, GENERATION_EXPRESSION
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'anulado'
  AND TABLE_NAME IN ('venta', 'cotizacion', 'compra', 'orden_compra_cliente');


-- #########################################################################
-- 2_procedimientos/00_drop_sps.sql
-- #########################################################################
USE sigmeta;

USE sigmeta;

-- =========================================================================
-- ELIMINA TODOS LOS PROCEDIMIENTOS ALMACENADOS DEL PROYECTO
-- Ejecutar ANTES de recargar los archivos SP_*.sql (SP_yoe, SP_claudia y SP_daniel
-- usan CREATE PROCEDURE sin DROP IF EXISTS y fallan si el SP ya existe).
-- No toca tablas ni datos: solo borra procedimientos.
-- Los sql/5_pruebas/sql_prueba_*.sql no se eliminan: no crean objetos en la base, solo ejecutan CALL.
--
-- Uso: 1) ejecutar este archivo  2) ejecutar SP_yoe, SP_claudia, SP_daniel, SP_sebastian, SP_adriano
-- Total actual: 182 procedimientos.
--      3) comprobar: SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema = 'sigmeta';
-- =========================================================================

-- ---------- 01_SP_yoe.sql (37) ----------
DROP PROCEDURE IF EXISTS sp_rol_insertar;
DROP PROCEDURE IF EXISTS sp_rol_modificar;
DROP PROCEDURE IF EXISTS sp_rol_eliminar;
DROP PROCEDURE IF EXISTS sp_rol_obtener;
DROP PROCEDURE IF EXISTS sp_rol_listar;
DROP PROCEDURE IF EXISTS sp_rol_buscar_por_tipo;
DROP PROCEDURE IF EXISTS sp_usuario_insertar;
DROP PROCEDURE IF EXISTS sp_usuario_modificar;
DROP PROCEDURE IF EXISTS sp_usuario_eliminar;
DROP PROCEDURE IF EXISTS sp_usuario_obtener;
DROP PROCEDURE IF EXISTS sp_usuario_listar;
DROP PROCEDURE IF EXISTS sp_usuario_buscar_por_nombre_usuario;
DROP PROCEDURE IF EXISTS sp_usuario_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_cliente_insertar;
DROP PROCEDURE IF EXISTS sp_cliente_modificar;
DROP PROCEDURE IF EXISTS sp_cliente_eliminar;
DROP PROCEDURE IF EXISTS sp_cliente_obtener;
DROP PROCEDURE IF EXISTS sp_cliente_listar;
DROP PROCEDURE IF EXISTS sp_cliente_buscar_por_documento;
DROP PROCEDURE IF EXISTS sp_cliente_buscar_por_razon_social;
DROP PROCEDURE IF EXISTS sp_cliente_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_proveedor_insertar;
DROP PROCEDURE IF EXISTS sp_proveedor_modificar;
DROP PROCEDURE IF EXISTS sp_proveedor_eliminar;
DROP PROCEDURE IF EXISTS sp_proveedor_obtener;
DROP PROCEDURE IF EXISTS sp_proveedor_listar;
DROP PROCEDURE IF EXISTS sp_proveedor_buscar_por_ruc;
DROP PROCEDURE IF EXISTS sp_proveedor_buscar_por_razon_social;
DROP PROCEDURE IF EXISTS sp_proveedor_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_insertar;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_modificar;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_eliminar;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_obtener;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_listar;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_listar_por_solicitante;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_buscar_vigente;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_datos_operacion;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_buscar_por_huella;

-- ---------- 02_SP_claudia.sql (23) ----------
DROP PROCEDURE IF EXISTS sp_categoria_insertar;
DROP PROCEDURE IF EXISTS sp_categoria_modificar;
DROP PROCEDURE IF EXISTS sp_categoria_eliminar;
DROP PROCEDURE IF EXISTS sp_categoria_obtener;
DROP PROCEDURE IF EXISTS sp_categoria_listar;
DROP PROCEDURE IF EXISTS sp_producto_insertar;
DROP PROCEDURE IF EXISTS sp_producto_modificar;
DROP PROCEDURE IF EXISTS sp_producto_eliminar;
DROP PROCEDURE IF EXISTS sp_producto_obtener;
DROP PROCEDURE IF EXISTS sp_producto_listar;
DROP PROCEDURE IF EXISTS sp_cotizacion_insertar;
DROP PROCEDURE IF EXISTS sp_cotizacion_modificar;
DROP PROCEDURE IF EXISTS sp_cotizacion_eliminar;
DROP PROCEDURE IF EXISTS sp_cotizacion_obtener;
DROP PROCEDURE IF EXISTS sp_cotizacion_listar;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_listar;
DROP PROCEDURE IF EXISTS sp_categoria_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_producto_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_listar_todos;

-- ---------- 03_SP_daniel.sql (23) ----------
DROP PROCEDURE IF EXISTS sp_compra_insertar;
DROP PROCEDURE IF EXISTS sp_compra_modificar;
DROP PROCEDURE IF EXISTS sp_compra_eliminar;
DROP PROCEDURE IF EXISTS sp_compra_obtener;
DROP PROCEDURE IF EXISTS sp_compra_listar;
DROP PROCEDURE IF EXISTS sp_detalle_compra_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_compra_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_compra_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_compra_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_compra_listar;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_insertar;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_modificar;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_obtener;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_listar;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_listar;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_insertar;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_modificar;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_obtener;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_listar;
DROP PROCEDURE IF EXISTS sp_compra_actualizar_estado;

-- ---------- 04_SP_sebastian.sql (60) ----------
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_insertar;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_modificar;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_eliminar;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_obtener;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_listar;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_actualizar_estado;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_buscar_por_cotizacion;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_listar;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_listar_por_orden;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_actualizar_atendida;
DROP PROCEDURE IF EXISTS sp_venta_insertar;
DROP PROCEDURE IF EXISTS sp_venta_modificar;
DROP PROCEDURE IF EXISTS sp_venta_eliminar;
DROP PROCEDURE IF EXISTS sp_venta_obtener;
DROP PROCEDURE IF EXISTS sp_venta_listar;
DROP PROCEDURE IF EXISTS sp_venta_bloquear;
DROP PROCEDURE IF EXISTS sp_venta_actualizar_estado;
DROP PROCEDURE IF EXISTS sp_venta_saldo_pendiente_cliente;
DROP PROCEDURE IF EXISTS sp_venta_generar_cuenta_por_cobrar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_venta_listar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_listar_por_venta;
DROP PROCEDURE IF EXISTS sp_detalle_venta_actualizar_despachado;
DROP PROCEDURE IF EXISTS sp_comprobante_insertar;
DROP PROCEDURE IF EXISTS sp_comprobante_modificar;
DROP PROCEDURE IF EXISTS sp_comprobante_eliminar;
DROP PROCEDURE IF EXISTS sp_comprobante_congelar;
DROP PROCEDURE IF EXISTS sp_comprobante_impreso;
DROP PROCEDURE IF EXISTS sp_comprobante_impreso_lineas;
DROP PROCEDURE IF EXISTS sp_comprobante_obtener;
DROP PROCEDURE IF EXISTS sp_comprobante_listar;
DROP PROCEDURE IF EXISTS sp_comprobante_listar_por_venta;
DROP PROCEDURE IF EXISTS sp_comprobante_listar_nc_por_relacionado;
DROP PROCEDURE IF EXISTS sp_comprobante_actualizar_estado;
DROP PROCEDURE IF EXISTS sp_comprobante_siguiente_correlativo;
DROP PROCEDURE IF EXISTS sp_comprobante_revertir_saldo_cpc;
DROP PROCEDURE IF EXISTS sp_comprobante_restaurar_saldo_cpc;
DROP PROCEDURE IF EXISTS sp_comprobante_registrar_envio;
DROP PROCEDURE IF EXISTS sp_comprobante_buscar_nc;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_listar;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_listar_por_comprobante;
DROP PROCEDURE IF EXISTS sp_despacho_insertar;
DROP PROCEDURE IF EXISTS sp_despacho_modificar;
DROP PROCEDURE IF EXISTS sp_despacho_eliminar;
DROP PROCEDURE IF EXISTS sp_despacho_obtener;
DROP PROCEDURE IF EXISTS sp_despacho_listar;
DROP PROCEDURE IF EXISTS sp_despacho_listar_por_venta;
DROP PROCEDURE IF EXISTS sp_despacho_siguiente_correlativo;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_listar;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_listar_por_despacho;

-- ---------- 05_SP_adriano.sql (39) ----------
DROP PROCEDURE IF EXISTS sp_caja_insertar;
DROP PROCEDURE IF EXISTS sp_caja_modificar;
DROP PROCEDURE IF EXISTS sp_caja_eliminar;
DROP PROCEDURE IF EXISTS sp_caja_obtener;
DROP PROCEDURE IF EXISTS sp_caja_obtener_abierta_por_usuario;
DROP PROCEDURE IF EXISTS sp_caja_listar_todas_con_usuario;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_insertar;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_modificar;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_eliminar;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_obtener;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_listar_por_caja;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_listar_con_detalle;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_buscar_por_fechas;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_totales_medio_pago;
DROP PROCEDURE IF EXISTS sp_cierre_caja_insertar;
DROP PROCEDURE IF EXISTS sp_cierre_caja_modificar;
DROP PROCEDURE IF EXISTS sp_cierre_caja_reabrir;
DROP PROCEDURE IF EXISTS sp_cierre_caja_obtener;
DROP PROCEDURE IF EXISTS sp_cierre_caja_obtener_por_caja;
DROP PROCEDURE IF EXISTS sp_cierre_caja_listar_por_caja;
DROP PROCEDURE IF EXISTS sp_cierre_caja_listar_con_detalle;
DROP PROCEDURE IF EXISTS sp_cierre_caja_buscar_por_fechas;
DROP PROCEDURE IF EXISTS sp_cierre_caja_calcular_monto_esperado;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_insertar;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_modificar;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_eliminar;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_obtener;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_obtener_por_venta;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_por_cliente;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_vencidas;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_todas_con_detalle;
DROP PROCEDURE IF EXISTS sp_cobro_insertar;
DROP PROCEDURE IF EXISTS sp_cobro_modificar;
DROP PROCEDURE IF EXISTS sp_cobro_eliminar;
DROP PROCEDURE IF EXISTS sp_cobro_obtener;
DROP PROCEDURE IF EXISTS sp_cobro_listar;
DROP PROCEDURE IF EXISTS sp_cobro_listar_por_cuenta_por_cobrar;
DROP PROCEDURE IF EXISTS sp_cobro_listar_por_cliente;

-- ---------- procedimientos que ya no existen (correcciones del Lab 4) ----------
-- los movimientos, recepciones y cierres de caja no se eliminan; reabrir caja usa sp_cierre_caja_reabrir
DROP PROCEDURE IF EXISTS sp_cierre_caja_eliminar;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_eliminar;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_eliminar;

DELIMITER ;
DROP PROCEDURE IF EXISTS sp_transaccion_bloquear;
DROP PROCEDURE IF EXISTS sp_correlativo_siguiente;
DROP PROCEDURE IF EXISTS sp_detalle_orden_comprometida;
DROP PROCEDURE IF EXISTS sp_stock_obtener_para_actualizar;
DROP PROCEDURE IF EXISTS sp_stock_actualizar;
DROP PROCEDURE IF EXISTS sp_stock_listar_minimos;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_listar_por_recepcion;
DROP PROCEDURE IF EXISTS sp_recepcion_marcar_anulada;
DROP PROCEDURE IF EXISTS sp_compra_bloquear;
DROP PROCEDURE IF EXISTS sp_compra_buscar_proveedor_fechas;
DROP PROCEDURE IF EXISTS sp_compra_estado_reversion;
DROP PROCEDURE IF EXISTS sp_movimiento_listar_producto;
DROP PROCEDURE IF EXISTS sp_movimiento_bloquear;
DROP PROCEDURE IF EXISTS sp_movimiento_existe_reversion;
DROP PROCEDURE IF EXISTS sp_movimiento_listar_recepcion;
DROP PROCEDURE IF EXISTS sp_detalle_compra_descontar;
DROP PROCEDURE IF EXISTS sp_movimiento_reversion_despacho;
DROP PROCEDURE IF EXISTS sp_movimiento_compensacion;

DROP PROCEDURE IF EXISTS sp_venta_listar_por_cliente;

-- Adriano: busquedas filtradas en la base (H10)
DROP PROCEDURE IF EXISTS sp_movimiento_caja_buscar_por_documento;
DROP PROCEDURE IF EXISTS sp_caja_listar_ventas_por_cobrar_contado;
DROP PROCEDURE IF EXISTS sp_cobro_buscar_por_fechas;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_buscar_por_comprobante;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_buscar_por_fechas;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_resumen;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_comprobantes_vigentes;


-- #########################################################################
-- 2_procedimientos/01_SP_yoe.sql
-- #########################################################################
USE sigmeta;

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: ROL
-- ==========================================
DELIMITER $$
CREATE PROCEDURE sp_rol_insertar(
    OUT p_id_rol INT,
    IN p_tipo ENUM('VENDEDOR', 'CAJERO', 'ALMACENERO', 'ADMINISTRADOR'),
    IN p_descripcion VARCHAR(150)
)
BEGIN
    INSERT INTO rol (tipo, descripcion, estado)
    VALUES (p_tipo, p_descripcion, TRUE);
    SET p_id_rol = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_rol_modificar(
    IN p_id_rol INT,
    IN p_tipo ENUM('VENDEDOR', 'CAJERO', 'ALMACENERO', 'ADMINISTRADOR'),
    IN p_descripcion VARCHAR(150),
    IN p_estado BOOLEAN
)
BEGIN
    UPDATE rol
    SET tipo = p_tipo, descripcion = p_descripcion, estado = p_estado
    WHERE id = p_id_rol;
END$$

CREATE PROCEDURE sp_rol_eliminar(IN p_id_rol INT)
BEGIN
    UPDATE rol SET estado = FALSE WHERE id = p_id_rol;
END$$

CREATE PROCEDURE sp_rol_obtener(IN p_id_rol INT)
BEGIN
    SELECT * FROM rol WHERE id = p_id_rol;
END$$

CREATE PROCEDURE sp_rol_listar()
BEGIN
    SELECT * FROM rol WHERE estado = TRUE;
END$$

-- RF002: al registrar un usuario se le asigna un rol por su tipo
CREATE PROCEDURE sp_rol_buscar_por_tipo(
    IN p_tipo ENUM('VENDEDOR', 'CAJERO', 'ALMACENERO', 'ADMINISTRADOR')
)
BEGIN
    SELECT * FROM rol WHERE tipo = p_tipo;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: USUARIO
-- ==========================================
CREATE PROCEDURE sp_usuario_insertar(
    OUT p_id_usuario INT,
    IN p_nombre_usuario VARCHAR(50),
    IN p_clave_hash VARCHAR(255),
    IN p_salt VARCHAR(64),
    IN p_nombres VARCHAR(100),
    IN p_apellidos VARCHAR(100),
    IN p_correo VARCHAR(120),
    IN p_id_rol INT
)
BEGIN
    INSERT INTO usuario (
        nombre_usuario, clave_hash, salt, nombres, apellidos, correo, id_rol, estado, fecha_registro
    ) VALUES (
        p_nombre_usuario, p_clave_hash, p_salt, p_nombres, p_apellidos, p_correo, p_id_rol, TRUE, NOW()
    );
    SET p_id_usuario = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_usuario_modificar(
    IN p_id_usuario INT,
    IN p_nombre_usuario VARCHAR(50),
    IN p_clave_hash VARCHAR(255),
    IN p_salt VARCHAR(64),
    IN p_nombres VARCHAR(100),
    IN p_apellidos VARCHAR(100),
    IN p_correo VARCHAR(120),
    IN p_id_rol INT,
    IN p_estado BOOLEAN
)
BEGIN
    UPDATE usuario SET
        nombre_usuario = p_nombre_usuario, clave_hash = p_clave_hash, salt = p_salt,
        nombres = p_nombres, apellidos = p_apellidos, correo = p_correo, id_rol = p_id_rol,
        estado = p_estado
    WHERE id = p_id_usuario;
END$$

CREATE PROCEDURE sp_usuario_eliminar(IN p_id_usuario INT)
BEGIN
    -- Baja logica: el usuario desactivado conserva las operaciones que registro (RF002).
    UPDATE usuario SET estado = FALSE WHERE id = p_id_usuario;
END$$

CREATE PROCEDURE sp_usuario_obtener(IN p_id_usuario INT)
BEGIN
    SELECT u.*, r.tipo, r.descripcion, r.estado AS estado_rol
    FROM usuario u INNER JOIN rol r ON u.id_rol = r.id
    WHERE u.id = p_id_usuario;
END$$

CREATE PROCEDURE sp_usuario_listar()
BEGIN
    SELECT u.*, r.tipo, r.descripcion, r.estado AS estado_rol
    FROM usuario u INNER JOIN rol r ON u.id_rol = r.id
    WHERE u.estado = TRUE;
END$$

-- RF001: inicio de sesion. No filtra por estado para que la capa de negocio
-- pueda distinguir entre "usuario inexistente" y "usuario desactivado".
CREATE PROCEDURE sp_usuario_buscar_por_nombre_usuario(IN p_nombre_usuario VARCHAR(50))
BEGIN
    SELECT u.*, r.tipo, r.descripcion, r.estado AS estado_rol
    FROM usuario u INNER JOIN rol r ON u.id_rol = r.id
    WHERE u.nombre_usuario = p_nombre_usuario;
END$$

-- RF002: el Administrador consulta usuarios activos o desactivados
CREATE PROCEDURE sp_usuario_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT u.*, r.tipo, r.descripcion, r.estado AS estado_rol
    FROM usuario u INNER JOIN rol r ON u.id_rol = r.id
    WHERE u.estado = p_estado;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: CLIENTE
-- ==========================================
CREATE PROCEDURE sp_cliente_insertar(
    OUT p_id_cliente INT,
    IN p_razon_social VARCHAR(150),
    IN p_nombres VARCHAR(100),
    IN p_apellidos VARCHAR(100),
    IN p_direccion VARCHAR(200),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(120),
    IN p_tipo_documento ENUM('RUC', 'DNI', 'CARNET_EXTRANJERIA', 'PASAPORTE'),
    IN p_numero_documento VARCHAR(20),
    IN p_contacto_nombre VARCHAR(100),
    IN p_condicion_pago ENUM('CONTADO', 'CREDITO'),
    IN p_plazo_credito_dias INT,
    IN p_limite_credito DECIMAL(12,2),
    IN p_calificacion_crediticia VARCHAR(20),
    IN p_tipo_persona VARCHAR(10)
)
BEGIN
    INSERT INTO cliente (
        tipo_persona, razon_social, nombres, apellidos, direccion, telefono, correo, estado, tipo_documento, numero_documento,
        contacto_nombre, condicion_pago, plazo_credito_dias, limite_credito, calificacion_crediticia
    ) VALUES (
        p_tipo_persona, p_razon_social, p_nombres, p_apellidos, p_direccion, p_telefono, p_correo, TRUE, p_tipo_documento, p_numero_documento,
        p_contacto_nombre, p_condicion_pago, p_plazo_credito_dias, p_limite_credito, p_calificacion_crediticia
    );
    SET p_id_cliente = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_cliente_modificar(
    IN p_id_cliente INT,
    IN p_razon_social VARCHAR(150),
    IN p_nombres VARCHAR(100),
    IN p_apellidos VARCHAR(100),
    IN p_direccion VARCHAR(200),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(120),
    IN p_tipo_documento ENUM('RUC', 'DNI', 'CARNET_EXTRANJERIA', 'PASAPORTE'),
    IN p_numero_documento VARCHAR(20),
    IN p_contacto_nombre VARCHAR(100),
    IN p_condicion_pago ENUM('CONTADO', 'CREDITO'),
    IN p_plazo_credito_dias INT,
    IN p_limite_credito DECIMAL(12,2),
    IN p_calificacion_crediticia VARCHAR(20),
    IN p_estado BOOLEAN,
    IN p_tipo_persona VARCHAR(10)
)
BEGIN
    UPDATE cliente SET tipo_persona = p_tipo_persona,
        razon_social = p_razon_social, nombres = p_nombres, apellidos = p_apellidos, direccion = p_direccion, telefono = p_telefono, correo = p_correo,
        tipo_documento = p_tipo_documento, numero_documento = p_numero_documento,
        contacto_nombre = p_contacto_nombre, condicion_pago = p_condicion_pago,
        plazo_credito_dias = p_plazo_credito_dias, limite_credito = p_limite_credito,
        calificacion_crediticia = p_calificacion_crediticia, estado = p_estado
    WHERE id = p_id_cliente;
END$$

CREATE PROCEDURE sp_cliente_eliminar(IN p_id_cliente INT)
BEGIN
    -- Baja logica: el cliente conserva su historial comercial (RF003).
    UPDATE cliente SET estado = FALSE WHERE id = p_id_cliente;
END$$

CREATE PROCEDURE sp_cliente_obtener(IN p_id_cliente INT)
BEGIN
    SELECT * FROM cliente WHERE id = p_id_cliente;
END$$

CREATE PROCEDURE sp_cliente_listar()
BEGIN
    SELECT * FROM cliente WHERE estado = TRUE;
END$$

-- RF003: busqueda por documento
CREATE PROCEDURE sp_cliente_buscar_por_documento(IN p_numero_documento VARCHAR(20))
BEGIN
    SELECT * FROM cliente WHERE numero_documento = p_numero_documento;
END$$

-- RF003: busqueda por razon social (empresas) o por nombre (personas naturales), coincidencia parcial
CREATE PROCEDURE sp_cliente_buscar_por_razon_social(IN p_razon_social VARCHAR(150))
BEGIN
    SELECT * FROM cliente
    WHERE razon_social LIKE CONCAT('%', p_razon_social, '%')
       OR CONCAT(nombres, ' ', apellidos) LIKE CONCAT('%', p_razon_social, '%');
END$$

-- RF003: busqueda por estado
CREATE PROCEDURE sp_cliente_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT * FROM cliente WHERE estado = p_estado;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: PROVEEDOR
-- ==========================================
CREATE PROCEDURE sp_proveedor_insertar(
    OUT p_id_proveedor INT,
    IN p_razon_social VARCHAR(150),
    IN p_direccion VARCHAR(200),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(120),
    IN p_ruc VARCHAR(11),
    IN p_rubro VARCHAR(100),
    IN p_contacto_nombre VARCHAR(100),
    IN p_plazo_entrega_dias INT,
    IN p_condicion_pago ENUM('CONTADO', 'CREDITO')
)
BEGIN
    INSERT INTO proveedor (
        razon_social, direccion, telefono, correo, estado, ruc, rubro,
        contacto_nombre, plazo_entrega_dias, condicion_pago
    ) VALUES (
        p_razon_social, p_direccion, p_telefono, p_correo, TRUE, p_ruc, p_rubro,
        p_contacto_nombre, p_plazo_entrega_dias, p_condicion_pago
    );
    SET p_id_proveedor = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_proveedor_modificar(
    IN p_id_proveedor INT,
    IN p_razon_social VARCHAR(150),
    IN p_direccion VARCHAR(200),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(120),
    IN p_ruc VARCHAR(11),
    IN p_rubro VARCHAR(100),
    IN p_contacto_nombre VARCHAR(100),
    IN p_plazo_entrega_dias INT,
    IN p_condicion_pago ENUM('CONTADO', 'CREDITO'),
    IN p_estado BOOLEAN
)
BEGIN
    UPDATE proveedor SET
        razon_social = p_razon_social, direccion = p_direccion, telefono = p_telefono, correo = p_correo,
        ruc = p_ruc, rubro = p_rubro, contacto_nombre = p_contacto_nombre,
        plazo_entrega_dias = p_plazo_entrega_dias, condicion_pago = p_condicion_pago, estado = p_estado
    WHERE id = p_id_proveedor;
END$$

CREATE PROCEDURE sp_proveedor_eliminar(IN p_id_proveedor INT)
BEGIN
    -- Baja logica: el proveedor conserva su historial de compras (RF004).
    UPDATE proveedor SET estado = FALSE WHERE id = p_id_proveedor;
END$$

CREATE PROCEDURE sp_proveedor_obtener(IN p_id_proveedor INT)
BEGIN
    SELECT * FROM proveedor WHERE id = p_id_proveedor;
END$$

CREATE PROCEDURE sp_proveedor_listar()
BEGIN
    SELECT * FROM proveedor WHERE estado = TRUE;
END$$

-- RF004: busqueda por RUC
CREATE PROCEDURE sp_proveedor_buscar_por_ruc(IN p_ruc VARCHAR(11))
BEGIN
    SELECT * FROM proveedor WHERE ruc = p_ruc;
END$$

-- RF004: busqueda por razon social (coincidencia parcial)
CREATE PROCEDURE sp_proveedor_buscar_por_razon_social(IN p_razon_social VARCHAR(150))
BEGIN
    SELECT * FROM proveedor WHERE razon_social LIKE CONCAT('%', p_razon_social, '%');
END$$

-- RF004: busqueda por estado
CREATE PROCEDURE sp_proveedor_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT * FROM proveedor WHERE estado = p_estado;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: SOLICITUD_AUTORIZACION
-- ==========================================
CREATE PROCEDURE sp_solicitud_autorizacion_insertar(
    OUT p_id_solicitud_autorizacion INT,
    IN p_id_solicitante INT,
    IN p_id_administrador INT,
    IN p_operacion_restringida VARCHAR(60),
    IN p_motivo VARCHAR(200),
    IN p_fecha_solicitud DATETIME,
    IN p_estado ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA', 'VENCIDA'),
    IN p_fecha_resolucion DATETIME,
    IN p_vigencia_minutos INT,
    IN p_fecha_vencimiento DATETIME
)
BEGIN
    INSERT INTO solicitud_autorizacion (
        id_solicitante, id_administrador, operacion_restringida, motivo, fecha_solicitud,
        estado, fecha_resolucion, vigencia_minutos, fecha_vencimiento
    ) VALUES (
        p_id_solicitante, p_id_administrador, p_operacion_restringida, p_motivo, p_fecha_solicitud,
        p_estado, p_fecha_resolucion, COALESCE(p_vigencia_minutos, 0), p_fecha_vencimiento
    );
    SET p_id_solicitud_autorizacion = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_solicitud_autorizacion_modificar(
    IN p_id_solicitud_autorizacion INT,
    IN p_id_solicitante INT,
    IN p_id_administrador INT,
    IN p_operacion_restringida VARCHAR(60),
    IN p_motivo VARCHAR(200),
    IN p_fecha_solicitud DATETIME,
    IN p_estado ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA', 'VENCIDA'),
    IN p_fecha_resolucion DATETIME,
    IN p_vigencia_minutos INT,
    IN p_fecha_vencimiento DATETIME
)
BEGIN
    UPDATE solicitud_autorizacion SET
        id_solicitante = p_id_solicitante, id_administrador = p_id_administrador,
        operacion_restringida = p_operacion_restringida, motivo = p_motivo,
        fecha_solicitud = p_fecha_solicitud, estado = p_estado, fecha_resolucion = p_fecha_resolucion,
        vigencia_minutos = COALESCE(p_vigencia_minutos, 0), fecha_vencimiento = p_fecha_vencimiento
    WHERE id = p_id_solicitud_autorizacion;
END$$

CREATE PROCEDURE sp_solicitud_autorizacion_eliminar(IN p_id_solicitud_autorizacion INT)
BEGIN
    -- Baja logica: la solicitud queda registrada para la trazabilidad (RF014, RNF001).
    -- Una solicitud pendiente se rechaza y una aprobada se revoca venciendola en este momento.
    -- MySQL asigna de izquierda a derecha: fecha_vencimiento se evalua antes de cambiar el estado.
    UPDATE solicitud_autorizacion SET
        fecha_vencimiento = CASE WHEN estado = 'APROBADA' THEN NOW() ELSE fecha_vencimiento END,
        fecha_resolucion = COALESCE(fecha_resolucion, NOW()),
        estado = CASE WHEN estado = 'APROBADA' THEN 'VENCIDA' ELSE 'RECHAZADA' END
    WHERE id = p_id_solicitud_autorizacion
      AND estado IN ('PENDIENTE', 'APROBADA');
END$$

CREATE PROCEDURE sp_solicitud_autorizacion_obtener(IN p_id_solicitud_autorizacion INT)
BEGIN
    SELECT * FROM solicitud_autorizacion WHERE id = p_id_solicitud_autorizacion;
END$$

CREATE PROCEDURE sp_solicitud_autorizacion_listar()
BEGIN
    SELECT * FROM solicitud_autorizacion;
END$$

-- RF014: el Administrador revisa las solicitudes pendientes
CREATE PROCEDURE sp_solicitud_autorizacion_listar_por_estado(
    IN p_estado ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA', 'VENCIDA')
)
BEGIN
    SELECT * FROM solicitud_autorizacion WHERE estado = p_estado ORDER BY fecha_solicitud;
END$$

-- RF014: cada usuario consulta las solicitudes que realizo
CREATE PROCEDURE sp_solicitud_autorizacion_listar_por_solicitante(IN p_id_solicitante INT)
BEGIN
    SELECT * FROM solicitud_autorizacion WHERE id_solicitante = p_id_solicitante
    ORDER BY fecha_solicitud DESC;
END$$

-- RF014: autorizacion aprobada y aun vigente de un usuario para una operacion
CREATE PROCEDURE sp_solicitud_autorizacion_buscar_vigente(
    IN p_id_solicitante INT,
    IN p_operacion_restringida VARCHAR(60)
)
BEGIN
    SELECT * FROM solicitud_autorizacion
    WHERE id_solicitante = p_id_solicitante
      AND operacion_restringida = p_operacion_restringida
      AND estado = 'APROBADA'
      AND fecha_vencimiento > NOW()
    ORDER BY fecha_vencimiento DESC
    LIMIT 1;
END$$
DELIMITER ;


-- #########################################################################
-- 2_procedimientos/02_SP_claudia.sql
-- #########################################################################
USE sigmeta;

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: CATEGORIA
-- ==========================================
DELIMITER $$
CREATE PROCEDURE sp_categoria_insertar(
    OUT p_id_categoria INT,
    IN p_nombre VARCHAR(100),
    IN p_descripcion VARCHAR(255)
)
BEGIN
    INSERT INTO categoria (nombre, descripcion, estado) 
    VALUES (p_nombre, p_descripcion, TRUE);
    SET p_id_categoria = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_categoria_modificar(
    IN p_id_categoria INT,
    IN p_nombre VARCHAR(100),
    IN p_descripcion VARCHAR(255)
)
BEGIN
    UPDATE categoria 
    SET nombre = p_nombre, descripcion = p_descripcion
    WHERE id = p_id_categoria;
END$$

CREATE PROCEDURE sp_categoria_eliminar(IN p_id_categoria INT)
BEGIN
    UPDATE categoria SET estado = FALSE WHERE id = p_id_categoria;
END$$

CREATE PROCEDURE sp_categoria_obtener(IN p_id_categoria INT)
BEGIN
    SELECT * FROM categoria WHERE id = p_id_categoria;
END$$

CREATE PROCEDURE sp_categoria_listar()
BEGIN
    SELECT * FROM categoria WHERE estado = TRUE;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: PRODUCTO
-- ==========================================
CREATE PROCEDURE sp_producto_insertar(
    OUT p_id_producto INT,
    IN p_codigo_interno VARCHAR(50),
    IN p_codigo_fabricante VARCHAR(50),
    IN p_codigo_proveedor VARCHAR(50),
    IN p_nombre VARCHAR(150),
    IN p_descripcion VARCHAR(255),
    IN p_id_categoria INT,
    IN p_unidad_compra ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO'),
    IN p_unidad_venta ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO'),
    IN p_factor_conversion DECIMAL(12,3),
    IN p_precio_venta DECIMAL(12,2),
    IN p_costo_unitario DECIMAL(12,2),
    IN p_stock_actual DECIMAL(12,3),
    IN p_stock_minimo DECIMAL(12,3),
    IN p_imagen VARCHAR(255),
    IN p_precio_referencial DECIMAL(12,2)
)
BEGIN
    INSERT INTO producto (
        codigo_interno, codigo_fabricante, codigo_proveedor, nombre, descripcion, id_categoria, 
        unidad_compra, unidad_venta, factor_conversion, precio_venta, costo_unitario, 
        stock_actual, stock_minimo, imagen, estado, precio_referencial
    ) VALUES (
        p_codigo_interno, p_codigo_fabricante, p_codigo_proveedor, p_nombre, p_descripcion, p_id_categoria, 
        p_unidad_compra, p_unidad_venta, p_factor_conversion, p_precio_venta, p_costo_unitario, 
        p_stock_actual, p_stock_minimo, p_imagen, TRUE, p_precio_referencial
    );
    SET p_id_producto = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_producto_modificar(
    IN p_id_producto INT,
    IN p_codigo_interno VARCHAR(50),
    IN p_codigo_fabricante VARCHAR(50),
    IN p_codigo_proveedor VARCHAR(50),
    IN p_nombre VARCHAR(150),
    IN p_descripcion VARCHAR(255),
    IN p_id_categoria INT,
    IN p_unidad_compra ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO'),
    IN p_unidad_venta ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO'),
    IN p_factor_conversion DECIMAL(12,3),
    IN p_precio_venta DECIMAL(12,2),
    IN p_costo_unitario DECIMAL(12,2),
    IN p_stock_actual DECIMAL(12,3),
    IN p_stock_minimo DECIMAL(12,3),
    IN p_imagen VARCHAR(255),
    IN p_precio_referencial DECIMAL(12,2),
    IN p_id_usuario_modificacion INT
)
BEGIN
    IF EXISTS (SELECT 1 FROM producto WHERE id = p_id_producto AND unidad_venta <> p_unidad_venta) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se cambia la unidad base del inventario';
    END IF;
    UPDATE producto SET 
        codigo_interno = p_codigo_interno, codigo_fabricante = p_codigo_fabricante, codigo_proveedor = p_codigo_proveedor, 
        nombre = p_nombre, descripcion = p_descripcion, id_categoria = p_id_categoria, unidad_compra = p_unidad_compra, 
        unidad_venta = p_unidad_venta, factor_conversion = p_factor_conversion, precio_venta = p_precio_venta, 
        costo_unitario = p_costo_unitario, stock_actual = p_stock_actual, stock_minimo = p_stock_minimo, 
        imagen = p_imagen, precio_referencial = p_precio_referencial,
        -- RNF001: quien modifico el producto (incluido el precio) y cuando
        id_usuario_modificacion = p_id_usuario_modificacion, fecha_modificacion = NOW()
    WHERE id = p_id_producto;
END$$

CREATE PROCEDURE sp_producto_eliminar(IN p_id_producto INT)
BEGIN
    UPDATE producto SET estado = FALSE WHERE id = p_id_producto;
END$$

CREATE PROCEDURE sp_producto_obtener(IN p_id_producto INT)
BEGIN
    SELECT * FROM producto WHERE id = p_id_producto;
END$$

CREATE PROCEDURE sp_producto_listar()
BEGIN
    SELECT * FROM producto WHERE estado = TRUE;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: COTIZACION
-- ==========================================
CREATE PROCEDURE sp_cotizacion_insertar(
    OUT p_id_cotizacion INT,
    IN p_numero VARCHAR(20),
    IN p_fecha_emision DATE,
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(255),
    IN p_id_usuario_registro INT,
    IN p_id_cliente INT,
    IN p_fecha_vigencia DATE,
    IN p_estado ENUM('REGISTRADA', 'ENVIADA', 'ACEPTADA', 'RECHAZADA', 'VENCIDA', 'ANULADA')
)
BEGIN
    INSERT INTO cotizacion (
        numero, fecha_emision, sub_total, igv, total, observaciones, fecha_registro, 
        id_usuario_registro, id_cliente, fecha_vigencia, estado
    ) VALUES (
        p_numero, p_fecha_emision, p_sub_total, p_igv, p_total, p_observaciones, NOW(), 
        p_id_usuario_registro, p_id_cliente, p_fecha_vigencia, p_estado
    );
    SET p_id_cotizacion = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_cotizacion_modificar(
    IN p_id_cotizacion INT,
    IN p_numero VARCHAR(20),
    IN p_fecha_emision DATE,
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(255),
    IN p_id_cliente INT,
    IN p_fecha_vigencia DATE,
    IN p_estado ENUM('REGISTRADA', 'ENVIADA', 'ACEPTADA', 'RECHAZADA', 'VENCIDA', 'ANULADA')
)
BEGIN
    UPDATE cotizacion SET 
        numero = p_numero, fecha_emision = p_fecha_emision, sub_total = p_sub_total, 
        igv = p_igv, total = p_total, observaciones = p_observaciones, id_cliente = p_id_cliente, 
        fecha_vigencia = p_fecha_vigencia, estado = p_estado
    WHERE id = p_id_cotizacion;
END$$

CREATE PROCEDURE sp_cotizacion_eliminar(
    IN p_id_cotizacion INT, 
    IN p_motivo_anulacion VARCHAR(255)
)
BEGIN
    UPDATE cotizacion 
    SET motivo_anulacion = p_motivo_anulacion, fecha_anulacion = NOW(), estado = 'ANULADA'
    WHERE id = p_id_cotizacion;
END$$

CREATE PROCEDURE sp_cotizacion_obtener(IN p_id_cotizacion INT)
BEGIN
    SELECT * FROM cotizacion WHERE id = p_id_cotizacion;
END$$

CREATE PROCEDURE sp_cotizacion_listar()
BEGIN
    SELECT * FROM cotizacion WHERE anulado = FALSE;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: DETALLE_COTIZACION
-- ==========================================
CREATE PROCEDURE sp_detalle_cotizacion_insertar(
    OUT p_id_detalle_cotizacion INT,
    IN p_id_cotizacion INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2)
)
BEGIN
    INSERT INTO detalle_cotizacion (
        id_cotizacion, numero_linea, id_producto, cantidad, precio_unitario, descuento, importe
    ) VALUES (
        p_id_cotizacion, p_numero_linea, p_id_producto, p_cantidad, p_precio_unitario, p_descuento, p_importe
    );
    SET p_id_detalle_cotizacion = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_detalle_cotizacion_modificar(
    IN p_id_detalle_cotizacion INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2)
)
BEGIN
    UPDATE detalle_cotizacion SET 
        numero_linea = p_numero_linea, id_producto = p_id_producto, cantidad = p_cantidad, 
        precio_unitario = p_precio_unitario, descuento = p_descuento, importe = p_importe
    WHERE id = p_id_detalle_cotizacion;
END$$

CREATE PROCEDURE sp_detalle_cotizacion_eliminar(IN p_id_detalle_cotizacion INT)
BEGIN
    -- Como la tabla detalle_cotizacion no tiene campo 'estado' o 'anulado', 
    -- y los detalles se manejan desde la cabecera, aquí aplicamos borrado físico si se elimina una línea.
    DELETE FROM detalle_cotizacion WHERE id = p_id_detalle_cotizacion;
END$$

CREATE PROCEDURE sp_detalle_cotizacion_obtener(IN p_id_detalle_cotizacion INT)
BEGIN
    SELECT * FROM detalle_cotizacion WHERE id = p_id_detalle_cotizacion;
END$$

CREATE PROCEDURE sp_detalle_cotizacion_listar(IN p_id_cotizacion INT)
BEGIN
    SELECT * FROM detalle_cotizacion WHERE id_cotizacion = p_id_cotizacion;
END$$

-- RF006: busqueda de categorias por estado (activas o desactivadas)
CREATE PROCEDURE sp_categoria_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT * FROM categoria WHERE estado = p_estado;
END$$

-- RF005: busqueda de productos por estado (activos o desactivados)
CREATE PROCEDURE sp_producto_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT * FROM producto WHERE estado = p_estado;
END$$

-- Listado general de las lineas de todas las cotizaciones
CREATE PROCEDURE sp_detalle_cotizacion_listar_todos()
BEGIN
    SELECT * FROM detalle_cotizacion ORDER BY id_cotizacion, numero_linea;
END$$

DELIMITER ;


-- #########################################################################
-- 2_procedimientos/03_SP_daniel.sql
-- #########################################################################
USE sigmeta;


USE sigmeta;

-- ============================================================
-- PROCEDIMIENTOS ALMACENADOS: COMPRA
-- ============================================================

DELIMITER $$

CREATE PROCEDURE sp_compra_insertar(
    IN p_id_proveedor INT,
    IN p_numero VARCHAR(50),
    IN p_fecha_emision DATE,
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
    SET p_igv = ROUND(p_sub_total * 0.18, 2);
    SET p_total = p_sub_total + p_igv;
    INSERT INTO compra (
        id_proveedor, numero, fecha_emision,
        sub_total, igv, total, observaciones,
        fecha_registro, id_usuario_registro,
        estado, fecha_recepcion_estimada
    ) VALUES (
        p_id_proveedor, p_numero, p_fecha_emision,
        p_sub_total, p_igv, p_total, p_observaciones,
        NOW(), p_id_usuario_registro,
        p_estado, p_fecha_recepcion_estimada
    );

    SET p_id = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_compra_modificar(
    IN p_id INT,
    IN p_id_proveedor INT,
    IN p_numero VARCHAR(50),
    IN p_fecha_emision DATE,
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(255),
    IN p_estado VARCHAR(30),
    IN p_fecha_recepcion_estimada DATE
)
BEGIN
    SET p_igv = ROUND(p_sub_total * 0.18, 2);
    SET p_total = p_sub_total + p_igv;
    UPDATE compra
    SET id_proveedor = p_id_proveedor,
        numero = p_numero,
        fecha_emision = p_fecha_emision,
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
    SET estado = 'ANULADA',
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
    IF p_cantidad <= 0 OR p_precio_unitario < 0 OR p_descuento < 0
       OR p_descuento > p_cantidad * p_precio_unitario THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cantidad, precio o descuento de compra invalido';
    END IF;
    SET p_importe = ROUND(p_cantidad * p_precio_unitario - p_descuento, 2);
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
    IF p_cantidad <= 0 OR p_precio_unitario < 0 OR p_descuento < 0
       OR p_descuento > p_cantidad * p_precio_unitario THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cantidad, precio o descuento de compra invalido';
    END IF;
    SET p_importe = ROUND(p_cantidad * p_precio_unitario - p_descuento, 2);
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

    DELETE FROM detalle_compra
    WHERE id = p_id
      AND cantidad_recibida = 0;
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
    ORDER BY id;
END$$


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


-- #########################################################################
-- 2_procedimientos/04_SP_sebastian.sql
-- #########################################################################
USE sigmeta;

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


-- #########################################################################
-- 2_procedimientos/05_SP_adriano.sql
-- #########################################################################
USE sigmeta;

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
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
    FROM cuenta_por_cobrar x
    LEFT JOIN venta v ON v.id = x.id_venta
    INNER JOIN cliente cl ON cl.id = x.id_cliente
    WHERE (CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END) = p_estado
    ORDER BY x.fecha_vencimiento;
END$$

-- RF009: cuentas vencidas = con saldo y fecha de vencimiento pasada, aunque su estado no se haya actualizado
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_vencidas$$
CREATE PROCEDURE sp_cuenta_por_cobrar_listar_vencidas()
BEGIN
    SELECT x.id AS cxc_id, x.id_venta AS venta_id, v.numero AS venta_numero,
           x.id_cliente AS cliente_id, cl.razon_social, cl.numero_documento, cl.tipo_persona, cl.tipo_documento, cl.nombres, cl.apellidos,
           x.fecha_emision, x.fecha_vencimiento,
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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
           x.monto_original, x.monto_acreditado, x.monto_pagado, x.saldo_pendiente, CASE WHEN x.saldo_pendiente <= 0.005 THEN 'PAGADA' WHEN x.fecha_vencimiento < CURDATE() THEN 'VENCIDA' WHEN x.monto_pagado > 0 THEN 'PAGADA_PARCIAL' ELSE 'PENDIENTE' END AS estado
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


-- #########################################################################
-- 2_procedimientos/06_SP_integridad.sql
-- #########################################################################
USE sigmeta;

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


-- #########################################################################
-- 2_procedimientos/07_SP_reforma.sql
-- #########################################################################
USE sigmeta;

USE sigmeta;
SET NAMES utf8mb4;
DELIMITER $$

-- =========================================================================
-- 07 - PROCEDIMIENTOS DE LA REFORMA (RF012, RF013, RF015 y reglas de integridad)
-- Se ejecuta AL FINAL, despues de 06_SP_integridad.sql. Es re-ejecutable: cada
-- procedimiento se borra y se vuelve a crear. No cambia ninguna tabla.
--
--   A. Integridad de inventario: stock comprometido y costo promedio ponderado.
--   B. Listados de almacen (RF012): compras por recibir, ventas por despachar,
--      recepciones, despachos, kardex y stock de productos.
--   C. Notas de credito (RF013): lineas que se pueden devolver de un comprobante.
--   D. Reportes del Administrador (RF015): comercial, abastecimiento e inventario, caja.
--      Toda la agregacion se hace aqui, en SQL (RNF003): el BO no suma listas en memoria.
-- =========================================================================

-- -------------------------------------------------------------------------
-- A. INTEGRIDAD DE INVENTARIO
-- -------------------------------------------------------------------------

-- Stock comprometido de un producto: lo vendido y todavia no despachado en ventas vigentes.
-- Stock disponible para vender = stock_actual - comprometido. p_excluir_venta = 0 si no aplica.
DROP PROCEDURE IF EXISTS sp_stock_comprometido$$
CREATE PROCEDURE sp_stock_comprometido(IN p_id_producto INT, IN p_excluir_venta INT)
BEGIN
    SELECT COALESCE(SUM(d.cantidad - d.cantidad_despachada), 0) AS cantidad
    FROM detalle_venta d
    INNER JOIN venta v ON v.id = d.id_venta
    WHERE d.id_producto = p_id_producto
      AND v.anulado = FALSE
      AND v.id <> p_excluir_venta
      AND d.cantidad > d.cantidad_despachada;
END$$

-- Costo promedio ponderado: lo actualiza la recepcion de compra (el stock no se toca aqui).
DROP PROCEDURE IF EXISTS sp_producto_actualizar_costo$$
CREATE PROCEDURE sp_producto_actualizar_costo(IN p_id INT, IN p_costo DECIMAL(12,2))
BEGIN
    IF p_costo < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El costo unitario no puede ser negativo';
    END IF;
    UPDATE producto SET costo_unitario = p_costo WHERE id = p_id;
END$$

-- RF006: vuelve a activar una categoria desactivada (baja logica reversible)
DROP PROCEDURE IF EXISTS sp_categoria_activar$$
CREATE PROCEDURE sp_categoria_activar(IN p_id_categoria INT)
BEGIN
    UPDATE categoria SET estado = TRUE WHERE id = p_id_categoria;
END$$

-- RF005: vuelve a activar un producto desactivado (baja logica reversible)
DROP PROCEDURE IF EXISTS sp_producto_activar$$
CREATE PROCEDURE sp_producto_activar(IN p_id_producto INT)
BEGIN
    UPDATE producto SET estado = TRUE WHERE id = p_id_producto;
END$$

-- RF003: vuelve a activar un cliente desactivado (baja logica reversible)
DROP PROCEDURE IF EXISTS sp_cliente_activar$$
CREATE PROCEDURE sp_cliente_activar(IN p_id_cliente INT)
BEGIN
    UPDATE cliente SET estado = TRUE WHERE id = p_id_cliente;
END$$

-- -------------------------------------------------------------------------
-- B. LISTADOS DE ALMACEN (RF012)
-- -------------------------------------------------------------------------

-- Compras que todavia esperan mercaderia (REGISTRADA o RECIBIDA_PARCIAL)
DROP PROCEDURE IF EXISTS sp_compra_listar_por_recibir$$
CREATE PROCEDURE sp_compra_listar_por_recibir()
BEGIN
    SELECT c.id, c.numero, c.fecha_emision, c.fecha_recepcion_estimada, c.estado, c.total,
           p.razon_social AS proveedor, p.ruc,
           (SELECT COUNT(*) FROM detalle_compra d WHERE d.id_compra = c.id) AS lineas,
           (SELECT COUNT(*) FROM detalle_compra d WHERE d.id_compra = c.id AND d.cantidad_recibida < d.cantidad) AS lineas_pendientes
    FROM compra c
    INNER JOIN proveedor p ON p.id = c.id_proveedor
    WHERE c.anulado = FALSE AND c.estado IN ('REGISTRADA', 'RECIBIDA_PARCIAL')
    ORDER BY c.fecha_emision, c.id;
END$$

-- Lineas de una compra con lo pedido, lo recibido y lo pendiente (para registrar la recepcion)
DROP PROCEDURE IF EXISTS sp_compra_lineas_por_recibir$$
CREATE PROCEDURE sp_compra_lineas_por_recibir(IN p_id_compra INT)
BEGIN
    SELECT d.id AS id_detalle_compra, d.numero_linea, d.id_producto, pr.codigo_interno, pr.nombre AS producto,
           d.unidad_compra, pr.unidad_venta, d.factor_conversion,
           d.cantidad, d.cantidad_recibida, (d.cantidad - d.cantidad_recibida) AS pendiente,
           pr.stock_actual
    FROM detalle_compra d
    INNER JOIN producto pr ON pr.id = d.id_producto
    WHERE d.id_compra = p_id_compra
    ORDER BY d.numero_linea;
END$$

-- Recepciones registradas (todas, o las de una compra si p_id_compra > 0)
DROP PROCEDURE IF EXISTS sp_recepcion_buscar$$
CREATE PROCEDURE sp_recepcion_buscar(IN p_id_compra INT)
BEGIN
    SELECT r.id, r.id_compra, c.numero AS compra, p.razon_social AS proveedor, r.fecha_recepcion,
           r.observaciones, r.anulada, r.fecha_registro, u.nombre_usuario AS usuario,
           (SELECT COUNT(*) FROM detalle_recepcion_compra d WHERE d.id_recepcion_compra = r.id) AS lineas
    FROM recepcion_compra r
    INNER JOIN compra c ON c.id = r.id_compra
    INNER JOIN proveedor p ON p.id = c.id_proveedor
    INNER JOIN usuario u ON u.id = r.id_usuario_registro
    WHERE p_id_compra = 0 OR r.id_compra = p_id_compra
    ORDER BY r.id DESC;
END$$

-- Detalle de una recepcion con su producto
DROP PROCEDURE IF EXISTS sp_recepcion_detalle$$
CREATE PROCEDURE sp_recepcion_detalle(IN p_id_recepcion INT)
BEGIN
    SELECT dr.id, dc.numero_linea, pr.codigo_interno, pr.nombre AS producto, dc.unidad_compra, pr.unidad_venta,
           dc.factor_conversion, dr.cantidad_recibida, (dr.cantidad_recibida * dc.factor_conversion) AS cantidad_stock
    FROM detalle_recepcion_compra dr
    INNER JOIN detalle_compra dc ON dc.id = dr.id_detalle_compra
    INNER JOIN producto pr ON pr.id = dc.id_producto
    WHERE dr.id_recepcion_compra = p_id_recepcion
    ORDER BY dc.numero_linea;
END$$

-- Ventas que todavia tienen mercaderia por entregar (REGISTRADA o DESPACHADA_PARCIAL)
DROP PROCEDURE IF EXISTS sp_venta_listar_por_despachar$$
CREATE PROCEDURE sp_venta_listar_por_despachar()
BEGIN
    SELECT v.id, v.numero, v.fecha_emision, v.estado, v.condicion_pago, v.total,
           COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento,
           cl.direccion,
           (SELECT CONCAT(c.serie, '-', c.numero) FROM comprobante c
             WHERE c.id_venta = v.id AND c.tipo IN ('FACTURA', 'BOLETA') AND c.estado <> 'ANULADO'
             ORDER BY c.id DESC LIMIT 1) AS comprobante,
           (SELECT COUNT(*) FROM movimiento_caja m
             WHERE m.documento_origen = 'VENTA' AND m.id_documento_origen = v.id AND m.tipo = 'COBRO_CONTADO') AS cobros_contado,
           (SELECT COUNT(*) FROM detalle_venta d WHERE d.id_venta = v.id AND d.cantidad_despachada < d.cantidad) AS lineas_pendientes
    FROM venta v
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    WHERE v.anulado = FALSE AND v.estado IN ('REGISTRADA', 'DESPACHADA_PARCIAL')
    ORDER BY v.fecha_emision, v.id;
END$$

-- Lineas de una venta con lo vendido, lo despachado y lo pendiente (para registrar el despacho)
DROP PROCEDURE IF EXISTS sp_venta_lineas_por_despachar$$
CREATE PROCEDURE sp_venta_lineas_por_despachar(IN p_id_venta INT)
BEGIN
    SELECT d.id AS id_detalle_venta, d.numero_linea, d.id_producto, pr.codigo_interno, pr.nombre AS producto,
           pr.unidad_venta, d.cantidad, d.cantidad_despachada, (d.cantidad - d.cantidad_despachada) AS pendiente,
           pr.stock_actual
    FROM detalle_venta d
    INNER JOIN producto pr ON pr.id = d.id_producto
    WHERE d.id_venta = p_id_venta
    ORDER BY d.numero_linea;
END$$

-- Despachos registrados (todos, o los de una venta si p_id_venta > 0)
DROP PROCEDURE IF EXISTS sp_despacho_buscar$$
CREATE PROCEDURE sp_despacho_buscar(IN p_id_venta INT)
BEGIN
    SELECT d.id, d.id_venta, v.numero AS venta, CONCAT(d.serie_guia, '-', d.numero_guia) AS guia,
           COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente,
           d.fecha_despacho, d.direccion_entrega, d.transportista, d.anulado, d.fecha_registro,
           u.nombre_usuario AS usuario,
           (SELECT COUNT(*) FROM detalle_despacho x WHERE x.id_despacho = d.id) AS lineas
    FROM despacho d
    INNER JOIN venta v ON v.id = d.id_venta
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    INNER JOIN usuario u ON u.id = d.id_usuario_registro
    WHERE p_id_venta = 0 OR d.id_venta = p_id_venta
    ORDER BY d.id DESC;
END$$

-- Detalle de un despacho con su producto
DROP PROCEDURE IF EXISTS sp_despacho_detalle$$
CREATE PROCEDURE sp_despacho_detalle(IN p_id_despacho INT)
BEGIN
    SELECT dd.id, dv.numero_linea, pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           dv.cantidad AS cantidad_vendida, dd.cantidad_despachada
    FROM detalle_despacho dd
    INNER JOIN detalle_venta dv ON dv.id = dd.id_detalle_venta
    INNER JOIN producto pr ON pr.id = dv.id_producto
    WHERE dd.id_despacho = p_id_despacho
    ORDER BY dv.numero_linea;
END$$

-- Kardex: movimientos de inventario con filtros opcionales.
-- p_id_producto = 0 (todos), p_desde / p_hasta = NULL (sin limite), p_tipo = '' (todos).
-- La columna "signo" dice si el movimiento suma (+1) o resta (-1) existencias.
DROP PROCEDURE IF EXISTS sp_movimiento_buscar$$
CREATE PROCEDURE sp_movimiento_buscar(IN p_id_producto INT, IN p_desde DATE, IN p_hasta DATE, IN p_tipo VARCHAR(40))
BEGIN
    SELECT m.id, m.fecha_movimiento, m.tipo, m.id_producto, pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           m.cantidad, m.stock_resultante, m.cantidad_contada, m.motivo, u.nombre_usuario AS usuario,
           m.id_recepcion_compra, m.id_despacho, m.id_comprobante, m.id_movimiento_revertido,
           CASE WHEN m.tipo IN ('INGRESO_COMPRA', 'INGRESO_PRODUCCION', 'INGRESO_DEVOLUCION',
                                'INGRESO_REVERSION_DESPACHO', 'AJUSTE_INGRESO', 'REVERSION_AJUSTE_SALIDA')
                THEN 1 ELSE -1 END AS signo,
           EXISTS (SELECT 1 FROM movimiento_inventario r WHERE r.id_movimiento_revertido = m.id) AS revertido
    FROM movimiento_inventario m
    INNER JOIN producto pr ON pr.id = m.id_producto
    INNER JOIN usuario u ON u.id = m.id_usuario_registro
    WHERE (p_id_producto = 0 OR m.id_producto = p_id_producto)
      AND (p_desde IS NULL OR m.fecha_movimiento >= p_desde)
      AND (p_hasta IS NULL OR m.fecha_movimiento < DATE_ADD(p_hasta, INTERVAL 1 DAY))
      AND (p_tipo IS NULL OR p_tipo = '' OR m.tipo = p_tipo)
    ORDER BY m.fecha_movimiento DESC, m.id DESC
    LIMIT 500;
END$$

-- Existencias de los productos activos: fisico, comprometido y disponible (kardex y ajuste por conteo)
DROP PROCEDURE IF EXISTS sp_stock_listar_productos$$
CREATE PROCEDURE sp_stock_listar_productos()
BEGIN
    SELECT pr.id, pr.codigo_interno, pr.nombre, c.nombre AS categoria, pr.unidad_venta,
           pr.stock_actual, pr.stock_minimo, pr.costo_unitario,
           COALESCE(k.comprometido, 0) AS comprometido,
           pr.stock_actual - COALESCE(k.comprometido, 0) AS disponible
    FROM producto pr
    INNER JOIN categoria c ON c.id = pr.id_categoria
    LEFT JOIN (SELECT d.id_producto, SUM(d.cantidad - d.cantidad_despachada) AS comprometido
               FROM detalle_venta d INNER JOIN venta v ON v.id = d.id_venta
               WHERE v.anulado = FALSE AND d.cantidad > d.cantidad_despachada
               GROUP BY d.id_producto) k ON k.id_producto = pr.id
    WHERE pr.estado = TRUE
    ORDER BY pr.nombre;
END$$

-- -------------------------------------------------------------------------
-- C. NOTAS DE CREDITO (RF013)
-- -------------------------------------------------------------------------

-- Lineas de la venta de un comprobante con lo vendido, lo entregado, lo ya acreditado en notas de
-- credito vigentes y lo que todavia se puede devolver (entregado - acreditado).
DROP PROCEDURE IF EXISTS sp_comprobante_lineas_devolvibles$$
CREATE PROCEDURE sp_comprobante_lineas_devolvibles(IN p_id_comprobante INT)
BEGIN
    SELECT dv.id AS id_detalle_venta, dv.numero_linea, pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           dv.cantidad, dv.cantidad_despachada, dv.precio_unitario, dv.descuento, dv.importe,
           COALESCE(a.acreditado, 0) AS acreditado,
           GREATEST(dv.cantidad_despachada - COALESCE(a.acreditado, 0), 0) AS devolvible
    FROM comprobante c
    INNER JOIN detalle_venta dv ON dv.id_venta = c.id_venta
    INNER JOIN producto pr ON pr.id = dv.id_producto
    LEFT JOIN (SELECT dn.id_detalle_venta, SUM(dn.cantidad) AS acreditado
               FROM detalle_nota_credito dn
               INNER JOIN comprobante nc ON nc.id = dn.id_comprobante
               WHERE nc.tipo = 'NOTA_CREDITO' AND nc.estado <> 'ANULADO'
               GROUP BY dn.id_detalle_venta) a ON a.id_detalle_venta = dv.id
    WHERE c.id = p_id_comprobante
    ORDER BY dv.numero_linea;
END$$

-- Comprobantes (factura o boleta vigentes) sobre los que se puede emitir una nota de credito
DROP PROCEDURE IF EXISTS sp_comprobante_listar_para_nota$$
CREATE PROCEDURE sp_comprobante_listar_para_nota()
BEGIN
    SELECT c.id, c.tipo, CONCAT(c.serie, '-', c.numero) AS comprobante, c.fecha_emision, c.total,
           v.id AS id_venta, v.numero AS venta, v.condicion_pago,
           COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento
    FROM comprobante c
    INNER JOIN venta v ON v.id = c.id_venta
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    WHERE c.tipo IN ('FACTURA', 'BOLETA') AND c.estado IN ('EMITIDO', 'ACEPTADO') AND v.anulado = FALSE
      AND EXISTS (SELECT 1 FROM detalle_venta dv WHERE dv.id_venta = v.id AND dv.cantidad_despachada > 0)
    ORDER BY c.id DESC
    LIMIT 300;
END$$

-- Notas de credito con su comprobante de origen, cliente y quien autorizo.
-- Filtros opcionales: p_desde / p_hasta = NULL, p_texto = '' (cliente, documento, numero de la nota o del origen).
DROP PROCEDURE IF EXISTS sp_nota_credito_buscar$$
CREATE PROCEDURE sp_nota_credito_buscar(IN p_desde DATE, IN p_hasta DATE, IN p_texto VARCHAR(150))
BEGIN
    SELECT nc.id, CONCAT(nc.serie, '-', nc.numero) AS nota, nc.fecha_emision, nc.estado,
           nc.sub_total, nc.igv, nc.total, nc.motivo,
           o.id AS id_origen, o.tipo AS tipo_origen, CONCAT(o.serie, '-', o.numero) AS origen,
           v.id AS id_venta, v.numero AS venta, v.condicion_pago,
           COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento,
           ua.nombre_usuario AS autorizo, nc.fecha_registro
    FROM comprobante nc
    INNER JOIN venta v ON v.id = nc.id_venta
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    LEFT JOIN comprobante o ON o.id = nc.id_comprobante_relacionado
    LEFT JOIN usuario ua ON ua.id = nc.id_administrador_autoriza
    WHERE nc.tipo = 'NOTA_CREDITO'
      AND (p_desde IS NULL OR nc.fecha_emision >= p_desde)
      AND (p_hasta IS NULL OR nc.fecha_emision <= p_hasta)
      AND (p_texto IS NULL OR p_texto = ''
           OR cl.razon_social LIKE CONCAT('%', p_texto, '%')
           OR CONCAT(cl.nombres, ' ', cl.apellidos) LIKE CONCAT('%', p_texto, '%')
           OR cl.numero_documento LIKE CONCAT('%', p_texto, '%')
           OR CONCAT(nc.serie, '-', nc.numero) LIKE CONCAT('%', p_texto, '%')
           OR CONCAT(o.serie, '-', o.numero) LIKE CONCAT('%', p_texto, '%'))
    ORDER BY nc.id DESC
    LIMIT 300;
END$$

-- Lineas de una nota de credito con su producto
DROP PROCEDURE IF EXISTS sp_nota_credito_detalle$$
CREATE PROCEDURE sp_nota_credito_detalle(IN p_id_nota INT)
BEGIN
    SELECT dn.numero_linea, pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           dn.cantidad, dn.precio_unitario, dn.descuento, dn.importe
    FROM detalle_nota_credito dn
    INNER JOIN detalle_venta dv ON dv.id = dn.id_detalle_venta
    INNER JOIN producto pr ON pr.id = dv.id_producto
    WHERE dn.id_comprobante = p_id_nota
    ORDER BY dn.numero_linea;
END$$

-- Cobros al contado registrados en caja para una venta (para exigir el cobro antes del despacho)
DROP PROCEDURE IF EXISTS sp_venta_contar_cobros_contado$$
CREATE PROCEDURE sp_venta_contar_cobros_contado(IN p_id_venta INT)
BEGIN
    SELECT COUNT(*) AS cobros
    FROM movimiento_caja
    WHERE documento_origen = 'VENTA' AND id_documento_origen = p_id_venta AND tipo = 'COBRO_CONTADO';
END$$

-- -------------------------------------------------------------------------
-- D. REPORTES (RF015)
-- Las ventas se cuentan por fecha de emision y excluyen las anuladas.
-- -------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS sp_reporte_ventas_totales$$
CREATE PROCEDURE sp_reporte_ventas_totales(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT
      (SELECT COUNT(*) FROM venta WHERE anulado = FALSE AND condicion_pago = 'CONTADO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS ventas_contado,
      (SELECT COALESCE(SUM(total), 0) FROM venta WHERE anulado = FALSE AND condicion_pago = 'CONTADO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS monto_contado,
      (SELECT COUNT(*) FROM venta WHERE anulado = FALSE AND condicion_pago = 'CREDITO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS ventas_credito,
      (SELECT COALESCE(SUM(total), 0) FROM venta WHERE anulado = FALSE AND condicion_pago = 'CREDITO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS monto_credito,
      (SELECT COUNT(*) FROM venta WHERE anulado = TRUE AND fecha_emision BETWEEN p_desde AND p_hasta) AS ventas_anuladas,
      (SELECT COUNT(*) FROM comprobante WHERE tipo = 'NOTA_CREDITO' AND estado <> 'ANULADO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS notas_credito,
      (SELECT COALESCE(SUM(total), 0) FROM comprobante WHERE tipo = 'NOTA_CREDITO' AND estado <> 'ANULADO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS monto_notas_credito,
      (SELECT COALESCE(SUM(saldo_pendiente), 0) FROM cuenta_por_cobrar WHERE saldo_pendiente > 0) AS saldo_por_cobrar,
      (SELECT COALESCE(SUM(saldo_pendiente), 0) FROM cuenta_por_cobrar WHERE saldo_pendiente > 0 AND fecha_vencimiento < CURDATE()) AS saldo_vencido;
END$$

DROP PROCEDURE IF EXISTS sp_reporte_ventas_por_dia$$
CREATE PROCEDURE sp_reporte_ventas_por_dia(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT v.fecha_emision AS fecha, COUNT(*) AS ventas,
           SUM(v.sub_total) AS sub_total, SUM(v.igv) AS igv, SUM(v.total) AS total
    FROM venta v
    WHERE v.anulado = FALSE AND v.fecha_emision BETWEEN p_desde AND p_hasta
    GROUP BY v.fecha_emision
    ORDER BY v.fecha_emision;
END$$

DROP PROCEDURE IF EXISTS sp_reporte_ventas_por_vendedor$$
CREATE PROCEDURE sp_reporte_ventas_por_vendedor(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT u.nombre_usuario AS usuario, CONCAT(u.nombres, ' ', u.apellidos) AS vendedor,
           COUNT(*) AS ventas, SUM(v.total) AS total
    FROM venta v
    INNER JOIN usuario u ON u.id = v.id_usuario_registro
    WHERE v.anulado = FALSE AND v.fecha_emision BETWEEN p_desde AND p_hasta
    GROUP BY u.id, u.nombre_usuario, u.nombres, u.apellidos
    ORDER BY total DESC;
END$$

DROP PROCEDURE IF EXISTS sp_reporte_ventas_por_cliente$$
CREATE PROCEDURE sp_reporte_ventas_por_cliente(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento,
           COUNT(*) AS ventas, SUM(v.total) AS total
    FROM venta v
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    WHERE v.anulado = FALSE AND v.fecha_emision BETWEEN p_desde AND p_hasta
    GROUP BY cl.id, cl.razon_social, cl.nombres, cl.apellidos, cl.numero_documento
    ORDER BY total DESC
    LIMIT 10;
END$$

-- Credito de clientes a la fecha: limite, deuda, deuda vencida y dias de atraso
DROP PROCEDURE IF EXISTS sp_reporte_credito_clientes$$
CREATE PROCEDURE sp_reporte_credito_clientes()
BEGIN
    SELECT COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento,
           cl.limite_credito, cl.plazo_credito_dias, cl.calificacion_crediticia,
           COALESCE(SUM(x.saldo_pendiente), 0) AS saldo_pendiente,
           COALESCE(SUM(CASE WHEN x.fecha_vencimiento < CURDATE() THEN x.saldo_pendiente ELSE 0 END), 0) AS saldo_vencido,
           COALESCE(MAX(CASE WHEN x.fecha_vencimiento < CURDATE() THEN DATEDIFF(CURDATE(), x.fecha_vencimiento) ELSE 0 END), 0) AS dias_atraso,
           cl.limite_credito - COALESCE(SUM(x.saldo_pendiente), 0) AS credito_disponible
    FROM cliente cl
    LEFT JOIN cuenta_por_cobrar x ON x.id_cliente = cl.id AND x.saldo_pendiente > 0
    WHERE cl.condicion_pago = 'CREDITO' OR x.id IS NOT NULL
    GROUP BY cl.id, cl.razon_social, cl.nombres, cl.apellidos, cl.numero_documento,
             cl.limite_credito, cl.plazo_credito_dias, cl.calificacion_crediticia
    ORDER BY saldo_vencido DESC, saldo_pendiente DESC;
END$$

DROP PROCEDURE IF EXISTS sp_reporte_compras_por_proveedor$$
CREATE PROCEDURE sp_reporte_compras_por_proveedor(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT p.razon_social AS proveedor, p.ruc, COUNT(*) AS compras, SUM(c.total) AS total,
           SUM(CASE WHEN c.estado IN ('REGISTRADA', 'RECIBIDA_PARCIAL') THEN 1 ELSE 0 END) AS por_recibir
    FROM compra c
    INNER JOIN proveedor p ON p.id = c.id_proveedor
    WHERE c.anulado = FALSE AND c.fecha_emision BETWEEN p_desde AND p_hasta
    GROUP BY p.id, p.razon_social, p.ruc
    ORDER BY total DESC;
END$$

-- Productos de mayor salida: despachos menos sus reversiones y menos lo devuelto por nota de credito
DROP PROCEDURE IF EXISTS sp_reporte_productos_mayor_movimiento$$
CREATE PROCEDURE sp_reporte_productos_mayor_movimiento(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           SUM(CASE WHEN m.tipo = 'SALIDA_DESPACHO' THEN m.cantidad ELSE -m.cantidad END) AS unidades,
           SUM(CASE WHEN m.tipo = 'SALIDA_DESPACHO' THEN m.cantidad ELSE -m.cantidad END) * pr.precio_venta AS importe_referencial,
           pr.stock_actual
    FROM movimiento_inventario m
    INNER JOIN producto pr ON pr.id = m.id_producto
    WHERE m.tipo IN ('SALIDA_DESPACHO', 'INGRESO_REVERSION_DESPACHO', 'INGRESO_DEVOLUCION')
      AND m.fecha_movimiento >= p_desde AND m.fecha_movimiento < DATE_ADD(p_hasta, INTERVAL 1 DAY)
    GROUP BY pr.id, pr.codigo_interno, pr.nombre, pr.unidad_venta, pr.precio_venta, pr.stock_actual
    HAVING unidades > 0
    ORDER BY unidades DESC
    LIMIT 10;
END$$

-- Productos activos con existencias que no tuvieron ninguna salida por despacho en el periodo
DROP PROCEDURE IF EXISTS sp_reporte_productos_sin_movimiento$$
CREATE PROCEDURE sp_reporte_productos_sin_movimiento(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta, pr.stock_actual,
           pr.stock_actual * pr.costo_unitario AS valor_inmovilizado,
           (SELECT MAX(x.fecha_movimiento) FROM movimiento_inventario x
             WHERE x.id_producto = pr.id AND x.tipo = 'SALIDA_DESPACHO') AS ultima_salida
    FROM producto pr
    WHERE pr.estado = TRUE AND pr.stock_actual > 0
      AND NOT EXISTS (SELECT 1 FROM movimiento_inventario m
                      WHERE m.id_producto = pr.id AND m.tipo = 'SALIDA_DESPACHO'
                        AND m.fecha_movimiento >= p_desde AND m.fecha_movimiento < DATE_ADD(p_hasta, INTERVAL 1 DAY))
    ORDER BY valor_inmovilizado DESC
    LIMIT 50;
END$$

-- Inventario valorizado al costo promedio, por categoria
DROP PROCEDURE IF EXISTS sp_reporte_inventario_valorizado$$
CREATE PROCEDURE sp_reporte_inventario_valorizado()
BEGIN
    SELECT c.nombre AS categoria, COUNT(*) AS productos,
           SUM(CASE WHEN pr.stock_actual <= pr.stock_minimo THEN 1 ELSE 0 END) AS en_minimo,
           SUM(pr.stock_actual * pr.costo_unitario) AS valor_costo,
           SUM(pr.stock_actual * pr.precio_venta) AS valor_venta
    FROM producto pr
    INNER JOIN categoria c ON c.id = pr.id_categoria
    WHERE pr.estado = TRUE
    GROUP BY c.id, c.nombre
    ORDER BY valor_costo DESC;
END$$

-- Cierres de caja del periodo con su cajero, su diferencia y quien reabrio
DROP PROCEDURE IF EXISTS sp_reporte_caja_cierres$$
CREATE PROCEDURE sp_reporte_caja_cierres(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT ci.id, ci.id_caja, ca.fecha_apertura, ci.fecha_cierre, ca.monto_inicial,
           ci.monto_calculado, ci.monto_declarado, ci.diferencia, ci.estado,
           ua.nombre_usuario AS cajero, uc.nombre_usuario AS usuario_cierre,
           ur.nombre_usuario AS usuario_reapertura, ci.fecha_reapertura, ci.motivo_reapertura
    FROM cierre_caja ci
    INNER JOIN caja ca ON ca.id = ci.id_caja
    INNER JOIN usuario ua ON ua.id = ca.id_usuario_apertura
    INNER JOIN usuario uc ON uc.id = ci.id_usuario_cierre
    LEFT JOIN usuario ur ON ur.id = ci.id_usuario_reapertura
    WHERE ci.fecha_cierre >= p_desde AND ci.fecha_cierre < DATE_ADD(p_hasta, INTERVAL 1 DAY)
    ORDER BY ci.fecha_cierre DESC;
END$$

-- Dinero que paso por las cajas en el periodo, por medio de pago
DROP PROCEDURE IF EXISTS sp_reporte_caja_por_medio_pago$$
CREATE PROCEDURE sp_reporte_caja_por_medio_pago(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT m.medio_pago,
           SUM(CASE WHEN m.tipo = 'COBRO_CONTADO' THEN m.monto ELSE 0 END) AS cobros_contado,
           SUM(CASE WHEN m.tipo = 'COBRO_CREDITO' THEN m.monto ELSE 0 END) AS cobros_credito,
           SUM(CASE WHEN m.tipo = 'INGRESO_MANUAL' THEN m.monto ELSE 0 END) AS ingresos,
           SUM(CASE WHEN m.tipo = 'EGRESO_MANUAL' THEN m.monto ELSE 0 END) AS egresos,
           SUM(CASE WHEN m.tipo = 'EGRESO_MANUAL' THEN -m.monto ELSE m.monto END) AS neto
    FROM movimiento_caja m
    WHERE m.fecha_movimiento >= p_desde AND m.fecha_movimiento < DATE_ADD(p_hasta, INTERVAL 1 DAY)
    GROUP BY m.medio_pago
    ORDER BY neto DESC;
END$$

-- Diferencias de los cierres vigentes del periodo, por cajero
DROP PROCEDURE IF EXISTS sp_reporte_caja_por_cajero$$
CREATE PROCEDURE sp_reporte_caja_por_cajero(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT ua.nombre_usuario AS cajero, COUNT(*) AS cierres,
           SUM(CASE WHEN ci.diferencia < 0 THEN 1 ELSE 0 END) AS con_faltante,
           SUM(CASE WHEN ci.diferencia > 0 THEN 1 ELSE 0 END) AS con_sobrante,
           SUM(CASE WHEN ci.diferencia < 0 THEN ci.diferencia ELSE 0 END) AS faltantes,
           SUM(CASE WHEN ci.diferencia > 0 THEN ci.diferencia ELSE 0 END) AS sobrantes,
           SUM(ci.diferencia) AS diferencia_neta
    FROM cierre_caja ci
    INNER JOIN caja ca ON ca.id = ci.id_caja
    INNER JOIN usuario ua ON ua.id = ca.id_usuario_apertura
    WHERE ci.estado = 'VIGENTE'
      AND ci.fecha_cierre >= p_desde AND ci.fecha_cierre < DATE_ADD(p_hasta, INTERVAL 1 DAY)
    GROUP BY ua.id, ua.nombre_usuario
    ORDER BY ua.nombre_usuario;
END$$

DELIMITER ;

-- #########################################################################
-- Revision final: deben salir 4 filas con STORED GENERATED y una cuenta por cobrar con su monto acreditado
-- #########################################################################
SELECT TABLE_NAME, EXTRA FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'sigmeta' AND COLUMN_NAME = 'anulado'
  AND TABLE_NAME IN ('venta', 'cotizacion', 'compra', 'orden_compra_cliente');
SELECT COUNT(*) AS procedimientos FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = 'sigmeta' AND ROUTINE_TYPE = 'PROCEDURE';
