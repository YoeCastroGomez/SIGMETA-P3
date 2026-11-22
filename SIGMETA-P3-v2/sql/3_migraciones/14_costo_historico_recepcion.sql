-- RF012: guarda el costo ANTES y DESPUES de cada ingreso de compra.
-- Ejecutar sobre la base de pruebas DESPUES de las migraciones 12 y 13
-- y ANTES de desplegar la version Java que usa estas rutinas.
-- No borra ni modifica movimientos previos. Las recepciones historicas
-- sin snapshot requieren conciliacion manual antes de revertirse.
USE sigmeta;

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
DELIMITER ;
