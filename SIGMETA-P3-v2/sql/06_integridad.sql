USE sigmeta;

-- Control de concurrencia compartido por todas las instancias Java.
CREATE TABLE transaccion_control (
    id INT PRIMARY KEY,
    CONSTRAINT ck_transaccion_control CHECK (id = 1)
) ENGINE=InnoDB;
INSERT INTO transaccion_control(id) VALUES (1);

CREATE TABLE correlativo_documento (
    tipo VARCHAR(20) NOT NULL,
    anio INT NOT NULL,
    ultimo BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (tipo, anio)
) ENGINE=InnoDB;
ALTER TABLE venta ADD CONSTRAINT uk_venta_numero UNIQUE(numero);
INSERT INTO correlativo_documento(tipo, anio, ultimo)
SELECT 'VTA', CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(numero, '-', 2), '-', -1) AS UNSIGNED),
       MAX(CAST(SUBSTRING_INDEX(numero, '-', -1) AS UNSIGNED))
FROM venta WHERE numero REGEXP '^VTA-[0-9]{4}-[0-9]+$'
GROUP BY SUBSTRING_INDEX(SUBSTRING_INDEX(numero, '-', 2), '-', -1);
