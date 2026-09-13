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

    CONSTRAINT fk_movimiento_inventario_despacho
        FOREIGN KEY (id_despacho)
        REFERENCES despacho(id_despacho),

    CONSTRAINT fk_movimiento_inventario_comprobante
        FOREIGN KEY (id_comprobante)
        REFERENCES comprobante(id_comprobante),

    CONSTRAINT fk_movimiento_inventario_usuario
        FOREIGN KEY (id_usuario_registro)
        REFERENCES usuario(id_usuario)
);

-- ============ DATOS DE PRUEBA ============

-- Los INSERT van aqui.

-- SET FOREIGN_KEY_CHECKS = 1;
