-- Solo desde la primera version corregida (que ya tiene tipo_persona y transaccion_control).
-- No borra datos. Ejecutar sin usuarios escribiendo, primero en una copia de pruebas.
USE sigmeta;
DELIMITER $$
DROP PROCEDURE IF EXISTS sp_migrar_v1_v2$$
CREATE PROCEDURE sp_migrar_v1_v2()
BEGIN
    IF EXISTS (SELECT 1 FROM cuenta_por_cobrar GROUP BY id_venta HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hay varias cuentas por venta: revisar antes de migrar';
    END IF;
    IF EXISTS (SELECT 1 FROM cliente WHERE calificacion_crediticia IS NOT NULL
        AND UPPER(TRIM(calificacion_crediticia)) NOT IN ('A','B','C','SIN_EVALUAR','SIN LINEA','')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hay calificaciones desconocidas: clasificar antes de migrar';
    END IF;
    IF EXISTS (SELECT 1 FROM cuenta_por_cobrar c JOIN venta v ON v.id=c.id_venta
        WHERE c.id_cliente<>v.id_cliente OR v.condicion_pago<>'CREDITO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hay cuentas de cliente/venta incoherentes: revisar antes de migrar';
    END IF;
    UPDATE cliente SET calificacion_crediticia = CASE
        WHEN calificacion_crediticia IS NULL OR TRIM(calificacion_crediticia)='' OR UPPER(TRIM(calificacion_crediticia))='SIN LINEA'
        THEN 'SIN_EVALUAR' ELSE UPPER(TRIM(calificacion_crediticia)) END;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_schema=DATABASE() AND table_name='cliente' AND constraint_name='ck_cliente_calificacion') THEN
        ALTER TABLE cliente ADD CONSTRAINT ck_cliente_calificacion
            CHECK (calificacion_crediticia IS NULL OR calificacion_crediticia IN ('A','B','C','SIN_EVALUAR'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_schema=DATABASE() AND table_name='cuenta_por_cobrar' AND constraint_name='uk_cxc_venta') THEN
        ALTER TABLE cuenta_por_cobrar ADD CONSTRAINT uk_cxc_venta UNIQUE(id_venta);
    END IF;
END$$
CALL sp_migrar_v1_v2()$$
DROP PROCEDURE sp_migrar_v1_v2$$
DELIMITER ;
-- Despues: recargar SP_yoe, SP_claudia, SP_daniel, SP_sebastian, SP_adriano, SP_integridad
-- usando antes 00_drop_sps.sql. Reiniciar la aplicacion con Java v2.
