-- CORRERLO SOLO UNA VEZ
DROP DATABASE IF EXISTS sigmeta;
CREATE DATABASE sigmeta DEFAULT CHARACTER SET utf8mb4;
USE sigmeta;
-- ============================================================
-- bloque del estudiante 1
-- Tablas: rol, usuario, cliente, proveedor, solicitud_autorizacion
-- Los datos son los mismos que crea PruebaSeguridad.java
-- ============================================================

-- Mientras se prueba solamente , descomentar..
-- SET FOREIGN_KEY_CHECKS = 0;

-- ======================== TABLAS ========================

CREATE TABLE rol (
                     id_rol      INT AUTO_INCREMENT PRIMARY KEY,
                     tipo        ENUM('VENDEDOR','CAJERO','ALMACENERO','ADMINISTRADOR') NOT NULL,
                     descripcion VARCHAR(150),
                     estado      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE usuario (
                         id_usuario     INT AUTO_INCREMENT PRIMARY KEY,
                         nombre_usuario VARCHAR(50)  NOT NULL UNIQUE,
                         clave_hash     VARCHAR(255) NOT NULL,
                         salt           VARCHAR(64)  NOT NULL,
                         nombres        VARCHAR(100) NOT NULL,
                         apellidos      VARCHAR(100) NOT NULL,
                         correo         VARCHAR(120),
                         id_rol         INT      NOT NULL,
                         estado         BOOLEAN  NOT NULL DEFAULT TRUE,
                         fecha_registro DATETIME NOT NULL,
                         CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);

CREATE TABLE cliente (
                         id_cliente               INT AUTO_INCREMENT PRIMARY KEY,
                         razon_social             VARCHAR(150) NOT NULL,
                         direccion                VARCHAR(200),
                         telefono                 VARCHAR(20),
                         correo                   VARCHAR(120),
                         estado                   BOOLEAN NOT NULL DEFAULT TRUE,
                         tipo_documento           ENUM('RUC','DNI','CARNET_EXTRANJERIA','PASAPORTE') NOT NULL,
                         numero_documento         VARCHAR(20) NOT NULL UNIQUE,
                         contacto_nombre          VARCHAR(100),
                         condicion_pago           ENUM('CONTADO','CREDITO') NOT NULL,
                         plazo_credito_dias       INT NOT NULL DEFAULT 0,
                         limite_credito           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                         calificacion_crediticia  VARCHAR(20)
);

CREATE TABLE proveedor (
                           id_proveedor       INT AUTO_INCREMENT PRIMARY KEY,
                           razon_social       VARCHAR(150) NOT NULL,
                           direccion          VARCHAR(200),
                           telefono           VARCHAR(20),
                           correo             VARCHAR(120),
                           estado             BOOLEAN NOT NULL DEFAULT TRUE,
                           ruc                VARCHAR(11) NOT NULL UNIQUE,
                           rubro              VARCHAR(100),
                           contacto_nombre    VARCHAR(100),
                           plazo_entrega_dias INT NOT NULL DEFAULT 0,
                           condicion_pago     ENUM('CONTADO','CREDITO') NOT NULL
);

CREATE TABLE solicitud_autorizacion (
                                        id_solicitud_autorizacion INT AUTO_INCREMENT PRIMARY KEY,
                                        id_solicitante            INT NOT NULL,
                                        id_administrador          INT,
                                        operacion_restringida     VARCHAR(60)  NOT NULL,
                                        motivo                    VARCHAR(200),
                                        fecha_solicitud           DATETIME NOT NULL,
                                        estado                    ENUM('PENDIENTE','APROBADA','RECHAZADA','VENCIDA') NOT NULL,
                                        fecha_resolucion          DATETIME,
                                        vigencia_minutos          INT NOT NULL DEFAULT 0,
                                        fecha_vencimiento         DATETIME,
                                        CONSTRAINT fk_solicitud_autorizacion_solicitante
                                            FOREIGN KEY (id_solicitante)   REFERENCES usuario(id_usuario),
                                        CONSTRAINT fk_solicitud_autorizacion_administrador
                                            FOREIGN KEY (id_administrador) REFERENCES usuario(id_usuario)
);

-- ==================== DATOS DE PRUEBA ====================

INSERT INTO rol (id_rol, tipo, descripcion, estado) VALUES
                                                        (1,'VENDEDOR','Atencion al cliente y ciclo comercial',TRUE),
                                                        (2,'CAJERO','Cobros y manejo de efectivo',TRUE),
                                                        (3,'ALMACENERO','Recepcion y despacho de mercaderia',TRUE),
                                                        (4,'ADMINISTRADOR','Supervision y configuracion',TRUE);

INSERT INTO usuario (id_usuario, nombre_usuario, clave_hash, salt, nombres, apellidos, correo, id_rol, estado, fecha_registro) VALUES
                                                                                                                                   (1,'lrojas','hash_lrojas','salt_lrojas','Lucia','Rojas Vega','lrojas@grupometa.pe',1,TRUE,'2026-09-01 08:00:00'),
                                                                                                                                   (2,'mchavez','hash_mchavez','salt_mchavez','Marco','Chavez Rios','mchavez@grupometa.pe',2,TRUE,'2026-09-01 08:00:00'),
                                                                                                                                   (3,'jtorres','hash_jtorres','salt_jtorres','Jorge','Torres Pena','jtorres@grupometa.pe',3,TRUE,'2026-09-01 08:00:00'),
                                                                                                                                   (4,'aquispe','hash_aquispe','salt_aquispe','Ana','Quispe Mendoza','aquispe@grupometa.pe',4,TRUE,'2026-09-01 08:00:00');

INSERT INTO cliente (id_cliente, razon_social, direccion, telefono, correo, estado, tipo_documento, numero_documento, contacto_nombre, condicion_pago, plazo_credito_dias, limite_credito, calificacion_crediticia) VALUES
                                                                                                                                                                                                                        (1,'Pedro Alberto Ramos Diaz','Av. Los Angeles 240, Lurigancho-Chosica','987654321','pramos@gmail.com',TRUE,'DNI','45781290','Pedro Ramos','CONTADO',0,0.00,'SIN LINEA'),
                                                                                                                                                                                                                        (2,'Constructora Andina del Sur S.A.C.','Jr. Huaraz 1180, Cercado de Lima','014785522','compras@andinadelsur.com.pe',TRUE,'RUC','20548963217','Rosa Linares','CREDITO',30,25000.00,'A');

INSERT INTO proveedor (id_proveedor, razon_social, direccion, telefono, correo, estado, ruc, rubro, contacto_nombre, plazo_entrega_dias, condicion_pago) VALUES
                                                                                                                                                             (1,'Importaciones Electricas Delta S.A.C.','Av. Argentina 3450, Callao','014520011','ventas@delta.com.pe',TRUE,'20431298765','Material electrico industrial','Cesar Bravo',5,'CREDITO'),
                                                                                                                                                             (2,'Ferreteria Industrial Huachipa E.I.R.L.','Carretera Central km 9, Lurigancho-Chosica','013710044','contacto@fihuachipa.pe',TRUE,'20512346789','Fijaciones y abrasivos','Nelly Fuentes',2,'CONTADO');

INSERT INTO solicitud_autorizacion (id_solicitud_autorizacion, id_solicitante, id_administrador, operacion_restringida, motivo, fecha_solicitud, estado, fecha_resolucion, vigencia_minutos, fecha_vencimiento) VALUES
    (1,1,4,'VENTA_SOBRE_LIMITE_CREDITO','El cliente supera su linea de credito en la orden del mes','2026-09-01 09:00:00','APROBADA','2026-09-01 09:12:00',60,'2026-09-01 10:12:00');

-- SET FOREIGN_KEY_CHECKS = 1;-- bloque del estudiante 2
-- Orden: primero todos los CREATE TABLE, despues todos los INSERT.
-- Mientras pruebas solo este archivo, descomenta la linea de abajo para
-- que MySQL no reclame por las FK que apuntan a tablas de otros bloques.



SET FOREIGN_KEY_CHECKS = 0;

-- ============ TABLAS ============


-- ==========================================
-- 1. TABLA: categoria
-- ==========================================
CREATE TABLE categoria (
  id_categoria INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  descripcion VARCHAR(255),
  estado BOOLEAN NOT NULL DEFAULT TRUE
);

-- ==========================================
-- 2. TABLA: producto
-- ==========================================
-- Atributos exactos según Producto.java
CREATE TABLE producto (
  id_producto INT AUTO_INCREMENT PRIMARY KEY,
  codigo_interno VARCHAR(50) UNIQUE NOT NULL,
  codigo_fabricante VARCHAR(50),
  codigo_proveedor VARCHAR(50),
  nombre VARCHAR(150) NOT NULL,
  descripcion VARCHAR(255),
  id_categoria INT NOT NULL,
  unidad_compra ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO') NOT NULL,
  unidad_venta ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO') NOT NULL,
  factor_conversion DECIMAL(12,3) NOT NULL,
  precio_venta DECIMAL(12,2) NOT NULL,
  costo_unitario DECIMAL(12,2) NOT NULL,
  stock_actual DECIMAL(12,3) NOT NULL,
  stock_minimo DECIMAL(12,3) NOT NULL,
  imagen VARCHAR(255),
  estado BOOLEAN NOT NULL DEFAULT TRUE,
  precio_referencial DECIMAL(12,2),
  CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
);

-- ==========================================
-- 3. TABLA: cotizacion
-- ==========================================
-- Incluye columnas heredadas de DocumentoComercial
CREATE TABLE cotizacion (
  id_cotizacion INT AUTO_INCREMENT PRIMARY KEY,
  numero VARCHAR(20) NOT NULL,
  fecha_emision DATE NOT NULL,
  moneda ENUM('SOLES', 'DOLARES') NOT NULL,
  sub_total DECIMAL(12,2) NOT NULL,
  igv DECIMAL(12,2) NOT NULL,
  total DECIMAL(12,2) NOT NULL,
  observaciones VARCHAR(255),
  fecha_registro DATETIME NOT NULL,
  id_usuario_registro INT,
  anulado BOOLEAN NOT NULL DEFAULT FALSE,
  motivo_anulacion VARCHAR(255),
  fecha_anulacion DATETIME,
  id_cliente INT NOT NULL,
  fecha_vigencia DATE,
  estado ENUM('PENDIENTE', 'ACEPTADA', 'RECHAZADA') NOT NULL,
  CONSTRAINT fk_cotizacion_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  CONSTRAINT fk_cotizacion_usuario FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id_usuario)
);

-- ==========================================
-- 4. TABLA: detalle_cotizacion
-- ==========================================
-- Incluye columnas heredadas de LineaDocumento
CREATE TABLE detalle_cotizacion (
  id_detalle_cotizacion INT AUTO_INCREMENT PRIMARY KEY,
  id_cotizacion INT NOT NULL,
  numero_linea INT NOT NULL,
  id_producto INT NOT NULL,
  cantidad DECIMAL(12,3) NOT NULL,
  precio_unitario DECIMAL(12,2) NOT NULL,
  descuento DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  importe DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_detalle_cotizacion_cotizacion FOREIGN KEY (id_cotizacion) REFERENCES cotizacion(id_cotizacion),
  CONSTRAINT fk_detalle_cotizacion_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);


-- ============ DATOS DE PRUEBA ============


-- 3 Categorías
INSERT INTO categoria (nombre, descripcion, estado) VALUES
('Herramientas Manuales', 'Martillos, alicates, destornilladores', TRUE),
('Materiales Eléctricos', 'Cables, enchufes industriales', TRUE),
('Construcción', 'Cemento, fierro, agregados', TRUE);

-- 6 Productos (con sus tres códigos, precio, costo, stock y stock mínimo)
INSERT INTO producto (codigo_interno, codigo_fabricante, codigo_proveedor, nombre, id_categoria, unidad_compra, unidad_venta, factor_conversion, precio_venta, costo_unitario, stock_actual, stock_minimo, estado) VALUES
('INT-001', 'FAB-101', 'PROV-11', 'Martillo de Acero', 1, 'UNIDAD', 'UNIDAD', 1.000, 45.00, 25.00, 100.000, 10.000, TRUE),
('INT-002', 'FAB-102', 'PROV-22', 'Alicate de Presión', 1, 'UNIDAD', 'UNIDAD', 1.000, 35.00, 18.00, 50.000, 5.000, TRUE),
('INT-003', 'FAB-103', 'PROV-33', 'Cable Mellizo 2x14', 2, 'METRO', 'METRO', 1.000, 2.50, 1.20, 500.000, 100.000, TRUE),
('INT-004', 'FAB-104', 'PROV-44', 'Enchufe Mennekes', 2, 'UNIDAD', 'UNIDAD', 1.000, 85.00, 50.00, 30.000, 5.000, TRUE),
('INT-005', 'FAB-105', 'PROV-55', 'Bolsa Cemento Sol', 3, 'UNIDAD', 'UNIDAD', 1.000, 28.50, 24.00, 200.000, 50.000, TRUE),
('INT-006', 'FAB-106', 'PROV-66', 'Fierro Corrugado', 3, 'UNIDAD', 'UNIDAD', 1.000, 32.00, 26.00, 150.000, 30.000, TRUE);

-- 1 Cotización
INSERT INTO cotizacion (numero, fecha_emision, moneda, sub_total, igv, total, fecha_registro, anulado, id_cliente, estado) VALUES
('COT-2026-0001', '2026-09-01', 'SOLES', 1300.00, 234.00, 1534.00, '2026-09-01 15:00:00', FALSE, 2, 'ACEPTADA');

-- 3 Líneas de Detalle
INSERT INTO detalle_cotizacion (id_cotizacion, numero_linea, id_producto, cantidad, precio_unitario, descuento, importe) VALUES
(1, 1, 1, 20.000, 45.00, 0.00, 900.00), 
(1, 2, 2, 10.000, 35.00, 50.00, 300.00), 
(1, 3, 3, 40.000, 2.50, 0.00, 100.00);


 SET FOREIGN_KEY_CHECKS = 1;
-- Orden: primero todos los CREATE TABLE, despues todos los INSERT.
-- Mientras pruebas solo este archivo, descomenta la linea de abajo.
-- SET FOREIGN_KEY_CHECKS = 0;

-- ============ TABLAS ============

CREATE TABLE compra (
    id_compra INT AUTO_INCREMENT PRIMARY KEY,

    id_proveedor INT NOT NULL,

    numero VARCHAR(50) NOT NULL,
    fecha_emision DATE NOT NULL,
    moneda ENUM('SOLES', 'DOLARES') NOT NULL,

    sub_total DECIMAL(12,2) NOT NULL,
    igv DECIMAL(12,2) NOT NULL,
    total DECIMAL(12,2) NOT NULL,

    observaciones VARCHAR(255),
    fecha_registro DATETIME NOT NULL,
    id_usuario_registro INT NOT NULL,

    anulado BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion VARCHAR(200),
    fecha_anulacion DATETIME,

    estado ENUM(
        'REGISTRADA',
        'RECIBIDA_PARCIAL',
        'RECIBIDA',
        'ANULADA'
    ) NOT NULL,

    fecha_recepcion_estimada DATE,

    CONSTRAINT fk_compra_proveedor
        FOREIGN KEY (id_proveedor)
        REFERENCES proveedor(id_proveedor),

    CONSTRAINT fk_compra_usuario
        FOREIGN KEY (id_usuario_registro)
        REFERENCES usuario(id_usuario)
);

CREATE TABLE detalle_compra (
    id_detalle_compra INT AUTO_INCREMENT PRIMARY KEY,

    id_compra INT NOT NULL,
    numero_linea INT NOT NULL,
    id_producto INT NOT NULL,

    cantidad DECIMAL(12,3) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    descuento DECIMAL(12,2) NOT NULL DEFAULT 0,
    importe DECIMAL(12,2) NOT NULL,

    unidad_compra ENUM(
        'UNIDAD',
        'CAJA',
        'KILOGRAMO',
        'METRO',
        'LITRO',
        'MILLAR',
        'GALON',
        'JUEGO'
    ) NOT NULL,

    factor_conversion DECIMAL(12,3) NOT NULL DEFAULT 1,
    cantidad_recibida DECIMAL(12,3) NOT NULL DEFAULT 0,

    CONSTRAINT fk_detalle_compra_compra
        FOREIGN KEY (id_compra)
        REFERENCES compra(id_compra),

    CONSTRAINT fk_detalle_compra_producto
        FOREIGN KEY (id_producto)
        REFERENCES producto(id_producto)
);

CREATE TABLE recepcion_compra (
    id_recepcion_compra INT AUTO_INCREMENT PRIMARY KEY,

    id_compra INT NOT NULL,
    fecha_recepcion DATE NOT NULL,
    observaciones VARCHAR(255),
    id_usuario_registro INT NOT NULL,
    fecha_registro DATETIME NOT NULL,

    CONSTRAINT fk_recepcion_compra_compra
        FOREIGN KEY (id_compra)
        REFERENCES compra(id_compra),

    CONSTRAINT fk_recepcion_compra_usuario
        FOREIGN KEY (id_usuario_registro)
        REFERENCES usuario(id_usuario)
);

CREATE TABLE detalle_recepcion_compra (
    id_detalle_recepcion_compra INT AUTO_INCREMENT PRIMARY KEY,

    id_recepcion_compra INT NOT NULL,
    id_detalle_compra INT NOT NULL,
    cantidad_recibida DECIMAL(12,3) NOT NULL,

    CONSTRAINT fk_detalle_recepcion_recepcion
        FOREIGN KEY (id_recepcion_compra)
        REFERENCES recepcion_compra(id_recepcion_compra),

    CONSTRAINT fk_detalle_recepcion_detalle_compra
        FOREIGN KEY (id_detalle_compra)
        REFERENCES detalle_compra(id_detalle_compra)
);

CREATE TABLE movimiento_inventario (
    id_movimiento_inventario INT AUTO_INCREMENT PRIMARY KEY,

    id_producto INT NOT NULL,

    tipo ENUM(
        'INGRESO_COMPRA',
        'SALIDA_DESPACHO',
        'INGRESO_PRODUCCION',
        'SALIDA_PRODUCCION',
        'INGRESO_DEVOLUCION',
        'AJUSTE_INGRESO',
        'AJUSTE_SALIDA'
    ) NOT NULL,

    fecha_movimiento DATETIME NOT NULL,
    cantidad DECIMAL(12,3) NOT NULL,
    stock_resultante DECIMAL(12,3) NOT NULL,

    id_recepcion_compra INT NULL,
    id_despacho INT NULL,
    id_comprobante INT NULL,

    id_usuario_registro INT NOT NULL,

    motivo VARCHAR(255),
    cantidad_contada DECIMAL(12,3),

    CONSTRAINT fk_movimiento_inventario_producto
        FOREIGN KEY (id_producto)
        REFERENCES producto(id_producto),

    CONSTRAINT fk_movimiento_inventario_recepcion
        FOREIGN KEY (id_recepcion_compra)
        REFERENCES recepcion_compra(id_recepcion_compra),

    -- fk_movimiento_inventario_despacho y fk_movimiento_inventario_comprobante
    -- se agregan con ALTER TABLE al final del script concatenado, porque
    -- despacho y comprobante se crean despues, en 04_venta.sql

    CONSTRAINT fk_movimiento_inventario_usuario
        FOREIGN KEY (id_usuario_registro)
        REFERENCES usuario(id_usuario)
);

-- ============ DATOS DE PRUEBA ============
-- Compra al proveedor 1 (Importaciones Electricas Delta) de 50 unidades de
-- Martillo de Acero (producto 1, costo_unitario real del catalogo = 25.00).
-- El stock_actual=100.000 que ya trae producto (02_catalogo.sql) se explica
-- como: 50.000 de stock previo + esta compra de 50.000 = 100.000.

INSERT INTO compra (
    id_compra, id_proveedor, numero, fecha_emision, moneda,
    sub_total, igv, total, observaciones, fecha_registro,
    id_usuario_registro, anulado, motivo_anulacion, fecha_anulacion,
    estado, fecha_recepcion_estimada
) VALUES (
    1, 1, 'OC-DELTA-0087', '2026-08-25', 'SOLES',
    1250.00, 225.00, 1475.00, 'Reposicion de stock de martillos', '2026-08-25 10:00:00',
    1, FALSE, NULL, NULL,
    'RECIBIDA', '2026-08-28'
);

INSERT INTO detalle_compra (
    id_detalle_compra, id_compra, numero_linea, id_producto,
    cantidad, precio_unitario, descuento, importe,
    unidad_compra, factor_conversion, cantidad_recibida
) VALUES (
    1, 1, 1, 1,
    50.000, 25.00, 0.00, 1250.00,
    'UNIDAD', 1.000, 50.000
);

INSERT INTO recepcion_compra (
    id_recepcion_compra, id_compra, fecha_recepcion, observaciones,
    id_usuario_registro, fecha_registro
) VALUES (
    1, 1, '2026-08-28', 'Recepcion completa, sin observaciones',
    1, '2026-08-28 09:00:00'
);

INSERT INTO detalle_recepcion_compra (
    id_detalle_recepcion_compra, id_recepcion_compra, id_detalle_compra, cantidad_recibida
) VALUES (
    1, 1, 1, 50.000
);

INSERT INTO movimiento_inventario (
    id_movimiento_inventario, id_producto, tipo, fecha_movimiento,
    cantidad, stock_resultante, id_recepcion_compra, id_despacho, id_comprobante,
    id_usuario_registro, motivo, cantidad_contada
) VALUES (
    1, 1, 'INGRESO_COMPRA', '2026-08-28 09:00:00',
    50.000, 100.000, 1, NULL, NULL,
    1, NULL, NULL
);

-- SET FOREIGN_KEY_CHECKS = 1;
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
    'ATENDIDA', 'OCC-2026-0001', '2026-09-02', 'SOLES', 1300.00, 234.00, 1534.00,
    'Orden de compra generada a partir de Cotizacion COT-2026-0001', '2026-09-02 09:30:00', 1, FALSE, NULL, NULL
);

-- 2. Detalle de orden de compra del cliente (3 lineas copiadas de la cotizacion)
INSERT INTO detalle_orden_compra_cliente (
    id_detalle_orden_compra_cliente, id_orden_compra_cliente, numero_linea,
    id_producto, cantidad, precio_unitario, descuento, importe, cantidad_atendida
) VALUES
    (1, 1, 1, 1, 20.000, 45.00, 0.00, 900.00, 20.000),
    (2, 1, 2, 2, 10.000, 35.00, 50.00, 300.00, 10.000),
    (3, 1, 3, 3, 40.000, 2.50, 0.00, 100.00, 40.000);

-- 3. Venta registrada al credito (asociada a la orden de compra 1)
INSERT INTO venta (
    id_venta, id_cliente, id_orden_compra_cliente, condicion_pago,
    plazo_credito_dias, estado, numero, fecha_emision, moneda,
    sub_total, igv, total, observaciones, fecha_registro,
    id_usuario_registro, anulado, motivo_anulacion, fecha_anulacion
) VALUES (
    1, 2, 1, 'CREDITO',
    30, 'DESPACHADA_PARCIAL', 'VTA-2026-0001', '2026-09-02', 'SOLES',
    1300.00, 234.00, 1534.00, 'Venta al credito a 30 dias segun OC-ANDINA-4521', '2026-09-02 10:15:00',
    1, FALSE, NULL, NULL
);

-- 4. Detalle de venta (mismas 3 lineas, con despacho parcial registrado)
INSERT INTO detalle_venta (
    id_detalle_venta, id_venta, numero_linea, id_producto,
    cantidad, precio_unitario, descuento, importe, cantidad_despachada
) VALUES
    (1, 1, 1, 1, 20.000, 45.00, 0.00, 900.00, 12.000),
    (2, 1, 2, 2, 10.000, 35.00, 50.00, 300.00, 6.000),
    (3, 1, 3, 3, 40.000, 2.50, 0.00, 100.00, 25.000);

-- 5. Comprobantes: Factura correlativa inicial y Nota de Credito que la referencia
INSERT INTO comprobante (
    id_comprobante, id_venta, tipo, serie, numero, fecha_emision,
    moneda, sub_total, igv, total, estado,
    id_comprobante_relacionado, motivo, medio_envio, fecha_envio, fecha_registro
) VALUES
    (1, 1, 'FACTURA', 'F001', '00000123', '2026-09-02',
     'SOLES', 1300.00, 234.00, 1534.00, 'EMITIDO',
     NULL, NULL, 'OSE_NUBE', '2026-09-02 10:20:00', '2026-09-02 10:18:00'),
    (2, 1, 'NOTA_CREDITO', 'FC01', '00000012', '2026-09-04',
     'SOLES', 90.00, 16.20, 106.20, 'EMITIDO',
     1, 'Devolucion de producto por defecto de fabrica', 'OSE_NUBE', '2026-09-04 11:30:00', '2026-09-04 11:25:00');

-- 6. Detalle de nota de credito (linea de producto devuelto asociada al comprobante 2)
INSERT INTO detalle_nota_credito (
    id_detalle_nota_credito, id_comprobante, numero_linea, id_producto,
    cantidad, precio_unitario, descuento, importe
) VALUES (
    1, 2, 1, 1, 2.000, 45.00, 0.00, 90.00
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
-- bloque del estudiante 5
-- Orden: primero todos los CREATE TABLE, despues todos los INSERT.
-- Mientras pruebas solo este archivo, descomenta la linea de abajo para
-- que MySQL no reclame por las FK que apuntan a tablas de otros bloques.
-- SET FOREIGN_KEY_CHECKS = 0;

-- ============ TABLAS ============
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- ELIMINACION DE TABLAS (DROP IF EXISTS)
-- Orden inverso a las dependencias (FK)
-- ============================================================

DROP TABLE IF EXISTS cierre_caja;
DROP TABLE IF EXISTS movimiento_caja;
DROP TABLE IF EXISTS cobro;
DROP TABLE IF EXISTS cuenta_por_cobrar;
DROP TABLE IF EXISTS caja;

-- ============================================================
-- CREACION DE TABLAS
-- ============================================================

-- Tabla: caja
-- Clase Java: Caja (idCaja, fechaApertura, montoInicial, usuarioApertura, abierta;
--             "movimientos" es la lista inversa, representada por movimiento_caja.id_caja)
-- NOTA (decision de equipo): la relacion Caja <-> CierreCaja es unidireccional.
-- CierreCaja conoce su Caja (ver id_caja abajo), pero Caja NO guarda id_cierre_caja;
-- coincide con la clase Java, que no tiene atributo CierreCaja.
CREATE TABLE caja (
    id_caja               INT AUTO_INCREMENT PRIMARY KEY,
    fecha_apertura        DATETIME NOT NULL,
    monto_inicial         DECIMAL(12,2) NOT NULL,
    id_usuario_apertura   INT NOT NULL,
    abierta               BOOLEAN NOT NULL,
    CONSTRAINT fk_caja_usuario_apertura
        FOREIGN KEY (id_usuario_apertura) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: cuenta_por_cobrar
-- Clase Java: CuentaPorCobrar (idCuentaPorCobrar, venta, cliente, fechaEmision,
--             fechaVencimiento, moneda, montoOriginal, montoPagado, saldoPendiente,
--             estado; "cobros" es la lista inversa -> cobro.id_cuenta_por_cobrar)
-- Se genera a partir de una venta al credito (Estudiante 4).
CREATE TABLE cuenta_por_cobrar (
    id_cuenta_por_cobrar INT AUTO_INCREMENT PRIMARY KEY,
    id_venta             INT NOT NULL,
    id_cliente           INT NOT NULL,
    fecha_emision        DATE NOT NULL,
    fecha_vencimiento    DATE NOT NULL,
    moneda               ENUM('SOLES', 'DOLARES') NOT NULL,
    monto_original       DECIMAL(12,2) NOT NULL,
    monto_pagado         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    saldo_pendiente      DECIMAL(12,2) NOT NULL,
    estado               ENUM('PENDIENTE', 'PARCIAL', 'PAGADA', 'ANULADA') NOT NULL,
    CONSTRAINT fk_cuenta_por_cobrar_venta
        FOREIGN KEY (id_venta) REFERENCES venta(id_venta),
    CONSTRAINT fk_cuenta_por_cobrar_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: cobro
-- Clase Java: Cobro (idCobro, cuentaPorCobrar, fechaCobro, medioPago, monto,
--             referencia, usuarioRegistro, fechaRegistro)
-- OJO: la clase NO tiene una Caja directa. La entrada de dinero a una caja
-- concreta se registra aparte en movimiento_caja, que referencia este cobro
-- de forma generica via documento_origen/id_documento_origen (ver abajo).
CREATE TABLE cobro (
    id_cobro              INT AUTO_INCREMENT PRIMARY KEY,
    id_cuenta_por_cobrar  INT NOT NULL,
    fecha_cobro           DATE NOT NULL,
    medio_pago            ENUM('EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'DEPOSITO') NOT NULL,
    monto                 DECIMAL(12,2) NOT NULL,
    referencia            VARCHAR(50) NULL,
    id_usuario_registro   INT NOT NULL,
    fecha_registro        DATETIME NOT NULL,
    CONSTRAINT fk_cobro_cuenta_por_cobrar
        FOREIGN KEY (id_cuenta_por_cobrar) REFERENCES cuenta_por_cobrar(id_cuenta_por_cobrar),
    CONSTRAINT fk_cobro_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: movimiento_caja
-- Clase Java: MovimientoCaja (idMovimientoCaja, caja, tipo, fechaMovimiento,
--             medioPago, monto, concepto, documentoOrigen, idDocumentoOrigen,
--             usuarioRegistro)
-- documento_origen / id_documento_origen son la referencia generica (String + int)
-- que en la prueba de Java apuntara a 'COBRO' + id_cobro para el ingreso, y
-- quedara en NULL para el egreso manual. RF010: la nota de credito jamas
-- produce un movimiento_caja de tipo INGRESO, por eso no participa del cierre.
CREATE TABLE movimiento_caja (
    id_movimiento_caja  INT AUTO_INCREMENT PRIMARY KEY,
    id_caja             INT NOT NULL,
    tipo                ENUM('INGRESO', 'EGRESO') NOT NULL,
    fecha_movimiento    DATETIME NOT NULL,
    medio_pago          ENUM('EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'DEPOSITO') NOT NULL,
    monto               DECIMAL(12,2) NOT NULL,
    concepto            VARCHAR(150) NOT NULL,
    documento_origen    VARCHAR(50) NULL,
    id_documento_origen INT NULL,
    id_usuario_registro INT NOT NULL,
    CONSTRAINT fk_movimiento_caja_caja
        FOREIGN KEY (id_caja) REFERENCES caja(id_caja),
    CONSTRAINT fk_movimiento_caja_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: cierre_caja
-- Clase Java: CierreCaja (idCierreCaja, caja, fechaCierre, montoCalculado,
--             montoDeclarado, diferencia, usuarioCierre)
CREATE TABLE cierre_caja (
    id_cierre_caja      INT AUTO_INCREMENT PRIMARY KEY,
    id_caja             INT NOT NULL,
    fecha_cierre        DATETIME NOT NULL,
    monto_calculado     DECIMAL(12,2) NOT NULL,
    monto_declarado     DECIMAL(12,2) NOT NULL,
    diferencia          DECIMAL(12,2) NOT NULL,
    id_usuario_cierre   INT NOT NULL,
    CONSTRAINT fk_cierre_caja_caja
        FOREIGN KEY (id_caja) REFERENCES caja(id_caja),
    CONSTRAINT fk_cierre_caja_usuario
        FOREIGN KEY (id_usuario_cierre) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============ DATOS DE PRUEBA ============
-- ============================================================
-- DATOS DE PRUEBA
-- ============================================================
-- NOTA: id_usuario_apertura / id_usuario_cierre / id_usuario_registro asumen
-- el usuario con id_usuario = 1 (rol administrativo/cajero) creado por el
-- Estudiante 1. Ajustar el id si en su script el cajero quedo con otro id.
-- id_cliente = 2 corresponde al cliente de la venta del Estudiante 4.

-- 1. Apertura de caja con monto inicial (queda cerrada al final, ver paso 6)
INSERT INTO caja (
    id_caja, fecha_apertura, monto_inicial, id_usuario_apertura, abierta
) VALUES (
    1, '2026-09-05 08:00:00', 500.00, 1, FALSE
);

-- 2. Cuenta por cobrar generada desde la venta al credito del Estudiante 4
--    (venta id_venta = 1, total 5900.00, plazo_credito_dias = 30 -> vence 2026-10-02)
--    Tras el cobro parcial del paso 3 queda con monto_pagado = 2000.00,
--    saldo_pendiente = 3900.00 y estado PARCIAL.
INSERT INTO cuenta_por_cobrar (
    id_cuenta_por_cobrar, id_venta, id_cliente, fecha_emision, fecha_vencimiento,
    moneda, monto_original, monto_pagado, saldo_pendiente, estado
) VALUES (
    1, 1, 2, '2026-09-02', '2026-10-02',
    'SOLES', 1534.00, 800.00, 734.00, 'PARCIAL'
);

-- 3. Cobro parcial de la cuenta por cobrar (sin Caja directa en el modelo Java;
--    el vinculo con la caja se hace en el movimiento_caja del paso 4)
INSERT INTO cobro (
    id_cobro, id_cuenta_por_cobrar, fecha_cobro, medio_pago,
    monto, referencia, id_usuario_registro, fecha_registro
) VALUES (
    1, 1, '2026-09-05', 'TRANSFERENCIA',
    800.00, 'OP-458821', 1, '2026-09-05 09:30:00'
);

-- 4. Movimiento de caja: ingreso generado por el cobro anterior
--    (documento_origen/id_documento_origen apuntan de forma generica al cobro 1)
INSERT INTO movimiento_caja (
    id_movimiento_caja, id_caja, tipo, fecha_movimiento, medio_pago,
    monto, concepto, documento_origen, id_documento_origen, id_usuario_registro
) VALUES (
    1, 1, 'INGRESO', '2026-09-05 09:30:00', 'TRANSFERENCIA',
    800.00, 'Cobro parcial CxC venta VTA-2026-0001', 'COBRO', 1, 1
);

-- 5. Movimiento de caja: egreso manual (sin documento de origen)
INSERT INTO movimiento_caja (
    id_movimiento_caja, id_caja, tipo, fecha_movimiento, medio_pago,
    monto, concepto, documento_origen, id_documento_origen, id_usuario_registro
) VALUES (
    2, 1, 'EGRESO', '2026-09-05 15:00:00', 'EFECTIVO',
    150.00, 'Compra de utiles de oficina', NULL, NULL, 1
);

-- 6. Cierre de caja del dia
--    monto_calculado = monto_inicial (500.00) + ingresos (2000.00) - egresos (150.00) = 2350.00
--    monto_declarado difiere en 5.00 (faltante) para ejercitar la comparacion en la prueba de Java.
--    RF010: el ingreso del paso 4 viene de un COBRO, nunca de una NOTA_CREDITO,
--    por eso el monto_calculado no se ve afectado por la nota de credito del Estudiante 4.
INSERT INTO cierre_caja (
    id_cierre_caja, id_caja, fecha_cierre, monto_calculado,
    monto_declarado, diferencia, id_usuario_cierre
) VALUES (
    1, 1, '2026-09-05 20:00:00', 1150.00,
    1145.00, -5.00, 1
);

SET FOREIGN_KEY_CHECKS = 1;

SELECT * FROM caja;
SELECT * FROM cuenta_por_cobrar;
SELECT * FROM cobro;
SELECT * FROM movimiento_caja;
SELECT * FROM cierre_caja;

-- SET FOREIGN_KEY_CHECKS = 1;

-- ============ AJUSTE DE ORDEN DE FK ============
ALTER TABLE movimiento_inventario
    ADD CONSTRAINT fk_movimiento_inventario_despacho
        FOREIGN KEY (id_despacho) REFERENCES despacho(id_despacho);

ALTER TABLE movimiento_inventario
    ADD CONSTRAINT fk_movimiento_inventario_comprobante
        FOREIGN KEY (id_comprobante) REFERENCES comprobante(id_comprobante);
