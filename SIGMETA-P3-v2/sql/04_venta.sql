-- 04_venta.sql | Estudiante 4 | SIGMETA
-- Comercial: orden de compra del cliente, venta, comprobante, nota de credito y despacho
-- Generado a partir de sql/sigmeta.sql, que es el script oficial: ejecutar 00_base.sql y luego
-- 01 a 05 en orden sobre una base nueva equivale a ejecutar sigmeta.sql. No ejecutar ambos.

USE sigmeta;

-- 04_venta.sql | Estudiante 4 | SIGMETA
-- Bloque Comercial: ordenes de compra de clientes, ventas, comprobantes (facturas y notas de credito) y despachos
-- ============================================================
-- CREACION DE TABLAS
-- ============================================================

-- Tabla: orden_compra_cliente
-- Clase Java: OrdenCompraCliente extends DocumentoComercial
CREATE TABLE orden_compra_cliente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente              INT NOT NULL,
    id_cotizacion           INT NULL,
    numero_orden_cliente    VARCHAR(30) NULL,
    estado                  ENUM('PENDIENTE', 'ATENDIDA_PARCIAL', 'ATENDIDA', 'ANULADA') NOT NULL,
    numero                  VARCHAR(20) NOT NULL,
    fecha_emision           DATE NOT NULL,
    sub_total               DECIMAL(12,2) NOT NULL,
    igv                     DECIMAL(12,2) NOT NULL,
    total                   DECIMAL(12,2) NOT NULL,
    observaciones           VARCHAR(300) NULL,
    fecha_registro          DATETIME NOT NULL,
    id_usuario_registro     INT NOT NULL,
    anulado                 BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion        VARCHAR(200) NULL,
    fecha_anulacion         DATETIME NULL,
    CONSTRAINT fk_orden_compra_cliente_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente(id),
    CONSTRAINT fk_orden_compra_cliente_cotizacion
        FOREIGN KEY (id_cotizacion) REFERENCES cotizacion(id),
    CONSTRAINT fk_orden_compra_cliente_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: detalle_orden_compra_cliente
-- Clase Java: DetalleOrdenCompraCliente extends LineaDocumento
CREATE TABLE detalle_orden_compra_cliente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_orden_compra_cliente         INT NOT NULL,
    numero_linea                    INT NOT NULL,
    id_producto                     INT NOT NULL,
    cantidad                        DECIMAL(12,3) NOT NULL,
    precio_unitario                 DECIMAL(12,2) NOT NULL,
    descuento                       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    importe                         DECIMAL(12,2) NOT NULL,
    cantidad_atendida               DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    CONSTRAINT fk_detalle_orden_compra_cliente_orden_compra_cliente
        FOREIGN KEY (id_orden_compra_cliente) REFERENCES orden_compra_cliente(id),
    CONSTRAINT fk_detalle_orden_compra_cliente_producto
        FOREIGN KEY (id_producto) REFERENCES producto(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: venta
-- Clase Java: Venta extends DocumentoComercial
CREATE TABLE venta (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente              INT NOT NULL,
    id_orden_compra_cliente INT NULL,
    condicion_pago          ENUM('CONTADO', 'CREDITO') NOT NULL,
    plazo_credito_dias      INT NOT NULL DEFAULT 0,
    estado                  ENUM('REGISTRADA', 'DESPACHADA_PARCIAL', 'DESPACHADA', 'ANULADA') NOT NULL,
    numero                  VARCHAR(20) NOT NULL,
    fecha_emision           DATE NOT NULL,
    sub_total               DECIMAL(12,2) NOT NULL,
    igv                     DECIMAL(12,2) NOT NULL,
    total                   DECIMAL(12,2) NOT NULL,
    observaciones           VARCHAR(300) NULL,
    fecha_registro          DATETIME NOT NULL,
    id_usuario_registro     INT NOT NULL,
    anulado                 BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion        VARCHAR(200) NULL,
    fecha_anulacion         DATETIME NULL,
    CONSTRAINT fk_venta_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente(id),
    CONSTRAINT fk_venta_orden_compra_cliente
        FOREIGN KEY (id_orden_compra_cliente) REFERENCES orden_compra_cliente(id),
    CONSTRAINT fk_venta_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: detalle_venta
-- Clase Java: DetalleVenta extends LineaDocumento
CREATE TABLE detalle_venta (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    id_venta            INT NOT NULL,
    numero_linea        INT NOT NULL,
    id_producto         INT NOT NULL,
    cantidad            DECIMAL(12,3) NOT NULL,
    precio_unitario     DECIMAL(12,2) NOT NULL,
    descuento           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    importe             DECIMAL(12,2) NOT NULL,
    cantidad_despachada DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    -- linea de la orden de compra del cliente que atiende (NULL en una venta directa)
    id_detalle_orden_compra_cliente INT NULL,
    CONSTRAINT fk_detalle_venta_venta
        FOREIGN KEY (id_venta) REFERENCES venta(id),
    CONSTRAINT fk_detalle_venta_producto
        FOREIGN KEY (id_producto) REFERENCES producto(id),
    CONSTRAINT fk_detalle_venta_detalle_orden
        FOREIGN KEY (id_detalle_orden_compra_cliente) REFERENCES detalle_orden_compra_cliente(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: comprobante
-- Clase Java: Comprobante (independiente; incluye autorreferencia y columna adicional motivo)
CREATE TABLE comprobante (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    id_venta                   INT NOT NULL,
    tipo                       ENUM('FACTURA', 'BOLETA', 'NOTA_CREDITO', 'NOTA_DEBITO') NOT NULL,
    serie                      VARCHAR(10) NOT NULL,
    numero                     VARCHAR(20) NOT NULL,
    fecha_emision              DATE NOT NULL,
    sub_total                  DECIMAL(12,2) NOT NULL,
    igv                        DECIMAL(12,2) NOT NULL,
    total                      DECIMAL(12,2) NOT NULL,
    estado                     ENUM('EMITIDO', 'ACEPTADO', 'RECHAZADO', 'ANULADO') NOT NULL,
    id_comprobante_relacionado INT NULL,
    motivo                     VARCHAR(200) NULL,
    medio_envio                VARCHAR(30) NULL,
    fecha_envio                DATETIME NULL,
    fecha_registro             DATETIME NOT NULL,
    CONSTRAINT fk_comprobante_venta
        FOREIGN KEY (id_venta) REFERENCES venta(id),
    CONSTRAINT fk_comprobante_comprobante
        FOREIGN KEY (id_comprobante_relacionado) REFERENCES comprobante(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: detalle_nota_credito
-- Clase Java: DetalleNotaCredito extends LineaDocumento (tabla nueva por decision de equipo)
CREATE TABLE detalle_nota_credito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_comprobante          INT NOT NULL,
    numero_linea            INT NOT NULL,
    -- linea de venta que se devuelve (el producto se obtiene de ella)
    id_detalle_venta         INT NOT NULL,
    cantidad                DECIMAL(12,3) NOT NULL,
    precio_unitario         DECIMAL(12,2) NOT NULL,
    descuento               DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    importe                 DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_detalle_nota_credito_comprobante
        FOREIGN KEY (id_comprobante) REFERENCES comprobante(id),
    CONSTRAINT fk_detalle_nota_credito_detalle_venta
        FOREIGN KEY (id_detalle_venta) REFERENCES detalle_venta(id),
    UNIQUE KEY uq_detalle_nota_credito_linea (id_comprobante, id_detalle_venta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: despacho
-- Clase Java: Despacho
CREATE TABLE despacho (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    id_venta            INT NOT NULL,
    serie_guia          VARCHAR(10) NOT NULL,
    numero_guia         VARCHAR(20) NOT NULL,
    fecha_despacho      DATE NOT NULL,
    direccion_entrega   VARCHAR(200) NULL,
    transportista       VARCHAR(100) NULL,
    anulado             BOOLEAN NOT NULL DEFAULT FALSE,
    id_usuario_registro INT NOT NULL,
    fecha_registro      DATETIME NOT NULL,
    CONSTRAINT fk_despacho_venta
        FOREIGN KEY (id_venta) REFERENCES venta(id),
    CONSTRAINT fk_despacho_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: detalle_despacho
-- Clase Java: DetalleDespacho
CREATE TABLE detalle_despacho (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_despacho         INT NOT NULL,
    -- linea de venta que se despacha (el producto se obtiene de ella)
    id_detalle_venta     INT NOT NULL,
    cantidad_despachada DECIMAL(12,3) NOT NULL,
    CONSTRAINT fk_detalle_despacho_despacho
        FOREIGN KEY (id_despacho) REFERENCES despacho(id),
    CONSTRAINT fk_detalle_despacho_detalle_venta
        FOREIGN KEY (id_detalle_venta) REFERENCES detalle_venta(id),
    UNIQUE KEY uq_detalle_despacho_linea (id_despacho, id_detalle_venta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- DATOS DE PRUEBA
-- ============================================================

-- 1. Orden de compra del cliente (generada a partir de Cotizacion 1 aceptada)
INSERT INTO orden_compra_cliente (
    id, id_cliente, id_cotizacion, numero_orden_cliente,
    estado, numero, fecha_emision, sub_total, igv, total,
    observaciones, fecha_registro, id_usuario_registro, anulado, motivo_anulacion, fecha_anulacion
) VALUES (
    1, 2, 1, 'OC-ANDINA-4521',
    'ATENDIDA', 'OCC-2026-0001', '2026-09-02', 1300.00, 234.00, 1534.00,
    'Orden de compra generada a partir de Cotizacion COT-2026-0001', '2026-09-02 09:30:00', 1, FALSE, NULL, NULL
);

-- 2. Detalle de orden de compra del cliente (3 lineas copiadas de la cotizacion)
INSERT INTO detalle_orden_compra_cliente (
    id, id_orden_compra_cliente, numero_linea,
    id_producto, cantidad, precio_unitario, descuento, importe, cantidad_atendida
) VALUES
    (1, 1, 1, 1, 20.000, 45.00, 0.00, 900.00, 20.000),
    (2, 1, 2, 2, 10.000, 35.00, 50.00, 300.00, 10.000),
    (3, 1, 3, 3, 40.000, 2.50, 0.00, 100.00, 40.000);

-- 3. Venta registrada al credito (asociada a la orden de compra 1)
INSERT INTO venta (
    id, id_cliente, id_orden_compra_cliente, condicion_pago,
    plazo_credito_dias, estado, numero, fecha_emision,
    sub_total, igv, total, observaciones, fecha_registro,
    id_usuario_registro, anulado, motivo_anulacion, fecha_anulacion
) VALUES (
    1, 2, 1, 'CREDITO',
    30, 'DESPACHADA_PARCIAL', 'VTA-2026-0001', '2026-09-02',
    1300.00, 234.00, 1534.00, 'Venta al credito a 30 dias segun OC-ANDINA-4521', '2026-09-02 10:15:00',
    1, FALSE, NULL, NULL
);

-- 4. Detalle de venta (mismas 3 lineas, con despacho parcial registrado)
INSERT INTO detalle_venta (
    id, id_venta, numero_linea, id_producto,
    cantidad, precio_unitario, descuento, importe, cantidad_despachada, id_detalle_orden_compra_cliente
) VALUES
    (1, 1, 1, 1, 20.000, 45.00, 0.00, 900.00, 12.000, 1),
    (2, 1, 2, 2, 10.000, 35.00, 50.00, 300.00, 6.000, 2),
    (3, 1, 3, 3, 40.000, 2.50, 0.00, 100.00, 25.000, 3);

-- 5. Comprobantes: Factura correlativa inicial y Nota de Credito que la referencia
INSERT INTO comprobante (
    id, id_venta, tipo, serie, numero, fecha_emision,
    sub_total, igv, total, estado,
    id_comprobante_relacionado, motivo, medio_envio, fecha_envio, fecha_registro
) VALUES
    (1, 1, 'FACTURA', 'F001', '00000123', '2026-09-02',
     1300.00, 234.00, 1534.00, 'EMITIDO',
     NULL, NULL, 'OSE_NUBE', '2026-09-02 10:20:00', '2026-09-02 10:18:00'),
    (2, 1, 'NOTA_CREDITO', 'FC01', '00000012', '2026-09-04',
     90.00, 16.20, 106.20, 'EMITIDO',
     1, 'Devolucion de producto por defecto de fabrica', 'OSE_NUBE', '2026-09-04 11:30:00', '2026-09-04 11:25:00');

-- 6. Detalle de nota de credito (linea de producto devuelto asociada al comprobante 2)
INSERT INTO detalle_nota_credito (
    id, id_comprobante, numero_linea, id_detalle_venta,
    cantidad, precio_unitario, descuento, importe
) VALUES (
    1, 2, 1, 1, 2.000, 45.00, 0.00, 90.00
);

-- 7. Despacho parcial con guia de remision asociada a la venta 1
INSERT INTO despacho (
    id, id_venta, serie_guia, numero_guia, fecha_despacho,
    direccion_entrega, transportista, anulado, id_usuario_registro, fecha_registro
) VALUES (
    1, 1, 'T001', '00000045', '2026-09-03',
    'Jr. Huaraz 1180, Cercado de Lima', 'Transportes Rapidos del Centro S.A.C.',
    FALSE, 3, '2026-09-03 08:30:00'
);

-- 8. Detalle de despacho (cantidades parciales efectivamente entregadas)
INSERT INTO detalle_despacho (
    id, id_despacho, id_detalle_venta, cantidad_despachada
) VALUES
    (1, 1, 1, 12.000),
    (2, 1, 2, 6.000),
    (3, 1, 3, 25.000);

-- ============ AJUSTE DE ORDEN DE FK ============
ALTER TABLE movimiento_inventario
    ADD CONSTRAINT fk_movimiento_inventario_despacho
        FOREIGN KEY (id_despacho) REFERENCES despacho(id);

ALTER TABLE movimiento_inventario
    ADD CONSTRAINT fk_movimiento_inventario_comprobante
        FOREIGN KEY (id_comprobante) REFERENCES comprobante(id);

-- ============ MOVIMIENTOS DE INVENTARIO DEL FLUJO DE VENTA ============
-- Se registran aqui porque referencian despacho y comprobante (bloque 4).
-- Despacho parcial T001-00000045 (almacenero, id_usuario = 3)
INSERT INTO movimiento_inventario (
    id, id_producto, tipo, fecha_movimiento,
    cantidad, stock_resultante, id_recepcion_compra, id_despacho, id_comprobante,
    id_usuario_registro, motivo, cantidad_contada
) VALUES
    (2, 1, 'SALIDA_DESPACHO',    '2026-09-03 08:30:00', 12.000,  88.000, NULL, 1, NULL, 3, NULL, NULL),
    (3, 2, 'SALIDA_DESPACHO',    '2026-09-03 08:30:00',  6.000,  44.000, NULL, 1, NULL, 3, NULL, NULL),
    (4, 3, 'SALIDA_DESPACHO',    '2026-09-03 08:30:00', 25.000, 475.000, NULL, 1, NULL, 3, NULL, NULL),
-- Reingreso de 2 martillos por la nota de credito FC01-00000012 (RF013)
    (5, 1, 'INGRESO_DEVOLUCION', '2026-09-04 11:25:00',  2.000,  90.000, NULL, NULL, 2, 3,
     'Devolucion por defecto de fabrica', NULL);

-- Stock final de los productos, igual al ultimo stock_resultante
UPDATE producto SET stock_actual =  90.000 WHERE id = 1;
UPDATE producto SET stock_actual =  44.000 WHERE id = 2;
UPDATE producto SET stock_actual = 475.000 WHERE id = 3;
