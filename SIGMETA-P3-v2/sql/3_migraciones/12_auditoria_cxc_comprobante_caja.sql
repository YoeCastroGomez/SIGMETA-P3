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
