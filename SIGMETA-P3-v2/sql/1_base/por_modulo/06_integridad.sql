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

-- H09 (Sebastian): numeracion unica de comprobantes y de ordenes de compra de clientes.
-- El numero OCC-AAAA-NNNN lo reserva sp_correlativo_siguiente('OCC', anio), igual que las ventas.
ALTER TABLE comprobante ADD CONSTRAINT uk_comprobante_serie_numero UNIQUE (serie, numero);
ALTER TABLE orden_compra_cliente ADD CONSTRAINT uk_orden_compra_cliente_numero UNIQUE (numero);
INSERT INTO correlativo_documento (tipo, anio, ultimo)
SELECT 'OCC' AS tipo,
       CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(numero, '-', 2), '-', -1) AS UNSIGNED) AS anio,
       MAX(CAST(SUBSTRING_INDEX(numero, '-', -1) AS UNSIGNED)) AS ultimo
FROM orden_compra_cliente WHERE numero REGEXP '^OCC-[0-9]{4}-[0-9]+$'
GROUP BY anio;
