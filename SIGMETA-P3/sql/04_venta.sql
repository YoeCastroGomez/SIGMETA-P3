-- 04_venta.sql | Estudiante 4 | SIGMETA
-- Bloque Comercial: ordenes de compra de clientes, ventas, comprobantes (facturas y notas de credito) y despachos

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- ELIMINACION DE TABLAS (DROP IF EXISTS)
-- ============================================================

DROP TABLE IF EXISTS detalle_despacho;
DROP TABLE IF EXISTS despacho;
DROP TABLE IF EXISTS detalle_nota_credito;
DROP TABLE IF EXISTS comprobante;
DROP TABLE IF EXISTS detalle_venta;
DROP TABLE IF EXISTS venta;
DROP TABLE IF EXISTS detalle_orden_compra_cliente;
DROP TABLE IF EXISTS orden_compra_cliente;

-- ============================================================
-- CREACION DE TABLAS
-- ============================================================

-- Tabla: orden_compra_cliente
-- Clase Java: OrdenCompraCliente extends DocumentoComercial
CREATE TABLE orden_compra_cliente (
    id_orden_compra_cliente INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente              INT NOT NULL,
    id_cotizacion           INT NULL,
    numero_orden_cliente    VARCHAR(30) NULL,
    estado                  ENUM('PENDIENTE', 'ATENDIDA_PARCIAL', 'ATENDIDA', 'ANULADA') NOT NULL,
    numero                  VARCHAR(20) NOT NULL,
    fecha_emision           DATE NOT NULL,
    moneda                  ENUM('SOLES', 'DOLARES') NOT NULL,
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
        FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
    CONSTRAINT fk_orden_compra_cliente_cotizacion
        FOREIGN KEY (id_cotizacion) REFERENCES cotizacion(id_cotizacion),
    CONSTRAINT fk_orden_compra_cliente_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: detalle_orden_compra_cliente
-- Clase Java: DetalleOrdenCompraCliente extends LineaDocumento
CREATE TABLE detalle_orden_compra_cliente (
    id_detalle_orden_compra_cliente INT AUTO_INCREMENT PRIMARY KEY,
    id_orden_compra_cliente         INT NOT NULL,
    numero_linea                    INT NOT NULL,
    id_producto                     INT NOT NULL,
    cantidad                        DECIMAL(12,3) NOT NULL,
    precio_unitario                 DECIMAL(12,2) NOT NULL,
    descuento                       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    importe                         DECIMAL(12,2) NOT NULL,
    cantidad_atendida               DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    CONSTRAINT fk_detalle_orden_compra_cliente_orden_compra_cliente
        FOREIGN KEY (id_orden_compra_cliente) REFERENCES orden_compra_cliente(id_orden_compra_cliente),
    CONSTRAINT fk_detalle_orden_compra_cliente_producto
        FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: venta
-- Clase Java: Venta extends DocumentoComercial
CREATE TABLE venta (
    id_venta                INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente              INT NOT NULL,
    id_orden_compra_cliente INT NULL,
    condicion_pago          ENUM('CONTADO', 'CREDITO') NOT NULL,
    plazo_credito_dias      INT NOT NULL DEFAULT 0,
    estado                  ENUM('REGISTRADA', 'DESPACHADA_PARCIAL', 'DESPACHADA', 'ANULADA') NOT NULL,
    numero                  VARCHAR(20) NOT NULL,
    fecha_emision           DATE NOT NULL,
    moneda                  ENUM('SOLES', 'DOLARES') NOT NULL,
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
        FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
    CONSTRAINT fk_venta_orden_compra_cliente
        FOREIGN KEY (id_orden_compra_cliente) REFERENCES orden_compra_cliente(id_orden_compra_cliente),
    CONSTRAINT fk_venta_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: detalle_venta
-- Clase Java: DetalleVenta extends LineaDocumento
CREATE TABLE detalle_venta (
    id_detalle_venta    INT AUTO_INCREMENT PRIMARY KEY,
    id_venta            INT NOT NULL,
    numero_linea        INT NOT NULL,
    id_producto         INT NOT NULL,
    cantidad            DECIMAL(12,3) NOT NULL,
    precio_unitario     DECIMAL(12,2) NOT NULL,
    descuento           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    importe             DECIMAL(12,2) NOT NULL,
    cantidad_despachada DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    CONSTRAINT fk_detalle_venta_venta
        FOREIGN KEY (id_venta) REFERENCES venta(id_venta),
    CONSTRAINT fk_detalle_venta_producto
        FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: comprobante
-- Clase Java: Comprobante (independiente; incluye autorreferencia y columna adicional motivo)
CREATE TABLE comprobante (
    id_comprobante             INT AUTO_INCREMENT PRIMARY KEY,
    id_venta                   INT NOT NULL,
    tipo                       ENUM('FACTURA', 'BOLETA', 'NOTA_CREDITO', 'NOTA_DEBITO') NOT NULL,
    serie                      VARCHAR(10) NOT NULL,
    numero                     VARCHAR(20) NOT NULL,
    fecha_emision              DATE NOT NULL,
    moneda                     ENUM('SOLES', 'DOLARES') NOT NULL,
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
        FOREIGN KEY (id_venta) REFERENCES venta(id_venta),
    CONSTRAINT fk_comprobante_comprobante
        FOREIGN KEY (id_comprobante_relacionado) REFERENCES comprobante(id_comprobante)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: detalle_nota_credito
-- Clase Java: DetalleNotaCredito extends LineaDocumento (tabla nueva por decision de equipo)
CREATE TABLE detalle_nota_credito (
    id_detalle_nota_credito INT AUTO_INCREMENT PRIMARY KEY,
    id_comprobante          INT NOT NULL,
    numero_linea            INT NOT NULL,
    id_producto             INT NOT NULL,
    cantidad                DECIMAL(12,3) NOT NULL,
    precio_unitario         DECIMAL(12,2) NOT NULL,
    descuento               DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    importe                 DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_detalle_nota_credito_comprobante
        FOREIGN KEY (id_comprobante) REFERENCES comprobante(id_comprobante),
    CONSTRAINT fk_detalle_nota_credito_producto
        FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: despacho
-- Clase Java: Despacho
CREATE TABLE despacho (
    id_despacho         INT AUTO_INCREMENT PRIMARY KEY,
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
        FOREIGN KEY (id_venta) REFERENCES venta(id_venta),
    CONSTRAINT fk_despacho_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: detalle_despacho
-- Clase Java: DetalleDespacho
CREATE TABLE detalle_despacho (
    id_detalle_despacho INT AUTO_INCREMENT PRIMARY KEY,
    id_despacho         INT NOT NULL,
    id_producto         INT NOT NULL,
    cantidad_despachada DECIMAL(12,3) NOT NULL,
    CONSTRAINT fk_detalle_despacho_despacho
        FOREIGN KEY (id_despacho) REFERENCES despacho(id_despacho),
    CONSTRAINT fk_detalle_despacho_producto
        FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- DATOS DE PRUEBA
-- ============================================================

-- 1. Orden de compra del cliente (generada a partir de Cotizacion 1 aceptada)
INSERT INTO orden_compra_cliente (
    id_orden_compra_cliente, id_cliente, id_cotizacion, numero_orden_cliente,
    estado, numero, fecha_emision, moneda, sub_total, igv, total,
    observaciones, fecha_registro, id_usuario_registro, anulado, motivo_anulacion, fecha_anulacion
) VALUES (
    1, 2, 1, 'OC-ANDINA-4521',
    'ATENDIDA', 'OCC-2026-0001', '2026-09-02', 'SOLES', 5000.00, 900.00, 5900.00,
    'Orden de compra generada a partir de Cotizacion COT-2026-0001', '2026-09-02 09:30:00', 1, FALSE, NULL, NULL
);

-- 2. Detalle de orden de compra del cliente (3 lineas copiadas de la cotizacion)
INSERT INTO detalle_orden_compra_cliente (
    id_detalle_orden_compra_cliente, id_orden_compra_cliente, numero_linea,
    id_producto, cantidad, precio_unitario, descuento, importe, cantidad_atendida
) VALUES
    (1, 1, 1, 1, 20.000, 150.00, 0.00, 3000.00, 20.000),
    (2, 1, 2, 2, 10.000, 85.00, 50.00, 800.00, 10.000),
    (3, 1, 3, 3, 40.000, 30.00, 0.00, 1200.00, 40.000);

-- 3. Venta registrada al credito (asociada a la orden de compra 1)
INSERT INTO venta (
    id_venta, id_cliente, id_orden_compra_cliente, condicion_pago,
    plazo_credito_dias, estado, numero, fecha_emision, moneda,
    sub_total, igv, total, observaciones, fecha_registro,
    id_usuario_registro, anulado, motivo_anulacion, fecha_anulacion
) VALUES (
    1, 2, 1, 'CREDITO',
    30, 'DESPACHADA_PARCIAL', 'VTA-2026-0001', '2026-09-02', 'SOLES',
    5000.00, 900.00, 5900.00, 'Venta al credito a 30 dias segun OC-ANDINA-4521', '2026-09-02 10:15:00',
    1, FALSE, NULL, NULL
);

-- 4. Detalle de venta (mismas 3 lineas, con despacho parcial registrado)
INSERT INTO detalle_venta (
    id_detalle_venta, id_venta, numero_linea, id_producto,
    cantidad, precio_unitario, descuento, importe, cantidad_despachada
) VALUES
    (1, 1, 1, 1, 20.000, 150.00, 0.00, 3000.00, 12.000),
    (2, 1, 2, 2, 10.000, 85.00, 50.00, 800.00, 6.000),
    (3, 1, 3, 3, 40.000, 30.00, 0.00, 1200.00, 25.000);

-- 5. Comprobantes: Factura correlativa inicial y Nota de Credito que la referencia
INSERT INTO comprobante (
    id_comprobante, id_venta, tipo, serie, numero, fecha_emision,
    moneda, sub_total, igv, total, estado,
    id_comprobante_relacionado, motivo, medio_envio, fecha_envio, fecha_registro
) VALUES
    (1, 1, 'FACTURA', 'F001', '00000123', '2026-09-02',
     'SOLES', 5000.00, 900.00, 5900.00, 'EMITIDO',
     NULL, NULL, 'OSE_NUBE', '2026-09-02 10:20:00', '2026-09-02 10:18:00'),
    (2, 1, 'NOTA_CREDITO', 'FC01', '00000012', '2026-09-04',
     'SOLES', 300.00, 54.00, 354.00, 'EMITIDO',
     1, 'Devolucion de producto por defecto de fabrica', 'OSE_NUBE', '2026-09-04 11:30:00', '2026-09-04 11:25:00');

-- 6. Detalle de nota de credito (linea de producto devuelto asociada al comprobante 2)
INSERT INTO detalle_nota_credito (
    id_detalle_nota_credito, id_comprobante, numero_linea, id_producto,
    cantidad, precio_unitario, descuento, importe
) VALUES (
    1, 2, 1, 1, 2.000, 150.00, 0.00, 300.00
);

-- 7. Despacho parcial con guia de remision asociada a la venta 1
INSERT INTO despacho (
    id_despacho, id_venta, serie_guia, numero_guia, fecha_despacho,
    direccion_entrega, transportista, anulado, id_usuario_registro, fecha_registro
) VALUES (
    1, 1, 'T001', '00000045', '2026-09-03',
    'Jr. Huaraz 1180, Cercado de Lima', 'Transportes Rapidos del Centro S.A.C.',
    FALSE, 2, '2026-09-03 08:30:00'
);

-- 8. Detalle de despacho (cantidades parciales efectivamente entregadas)
INSERT INTO detalle_despacho (
    id_detalle_despacho, id_despacho, id_producto, cantidad_despachada
) VALUES
    (1, 1, 1, 12.000),
    (2, 1, 2, 6.000),
    (3, 1, 3, 25.000);

SET FOREIGN_KEY_CHECKS = 1;
