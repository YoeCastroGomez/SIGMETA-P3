USE sigmeta;

-- =========================================================================
-- MIGRACION LAB 9 - SEBASTIAN (MODULO VENTAS)
-- =========================================================================
-- ORDEN DE EJECUCION DE SCRIPTS:
--
-- Para una Base de Datos EXISTENTE:
--   1. sql/3_migraciones/04_migracion_lab9_sebastian.sql
--   2. sql/2_procedimientos/00_drop_sps.sql
--   3. sql/2_procedimientos/01_SP_yoe.sql
--   4. sql/2_procedimientos/02_SP_claudia.sql
--   5. sql/2_procedimientos/03_SP_daniel.sql
--   6. sql/2_procedimientos/04_SP_sebastian.sql
--   7. sql/2_procedimientos/05_SP_adriano.sql
--   8. sql/2_procedimientos/06_SP_integridad.sql
--
-- Para una Base de Datos NUEVA:
--   1. sql/1_base/sigmeta.sql
--   2. sql/2_procedimientos/01_SP_yoe.sql
--   3. sql/2_procedimientos/02_SP_claudia.sql
--   4. sql/2_procedimientos/03_SP_daniel.sql
--   5. sql/2_procedimientos/04_SP_sebastian.sql
--   6. sql/2_procedimientos/05_SP_adriano.sql
--   7. sql/2_procedimientos/06_SP_integridad.sql
-- =========================================================================

-- 1. Quitar NOTA_DEBITO del enum de comprobante.tipo
ALTER TABLE comprobante
    MODIFY COLUMN tipo ENUM('FACTURA', 'BOLETA', 'NOTA_CREDITO') NOT NULL;

-- 2. Agregar id_administrador_autoriza con FK a usuario(id)
-- Las notas de credito existentes se mantienen en NULL (no se asume id_administrador_autoriza = 1)
ALTER TABLE comprobante
    ADD COLUMN id_administrador_autoriza INT NULL AFTER fecha_envio,
    ADD CONSTRAINT fk_comprobante_admin
        FOREIGN KEY (id_administrador_autoriza) REFERENCES usuario(id);
