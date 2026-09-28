
-- Bloque del estudiante 2

-- ============ TABLAS ============

CREATE TABLE categoria (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE producto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo_interno VARCHAR(50) UNIQUE NOT NULL,
    codigo_fabricante VARCHAR(50),
    codigo_proveedor VARCHAR(50),
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    id_categoria INT NOT NULL,
    unidad_compra ENUM('UNIDAD', 'METRO', 'KILOGRAMO', 'CAJA') NOT NULL,
    unidad_venta ENUM('UNIDAD', 'METRO', 'KILOGRAMO', 'CAJA') NOT NULL,
    factor_conversion DECIMAL(12,3) NOT NULL,
    precio_venta DECIMAL(12,2) NOT NULL,
    costo_unitario DECIMAL(12,2) NOT NULL,
    stock_actual DECIMAL(12,3) NOT NULL,
    stock_minimo DECIMAL(12,3) NOT NULL,
    imagen VARCHAR(255),
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    precio_referencial DECIMAL(12,2),

    CONSTRAINT fk_producto_categoria
        FOREIGN KEY (id_categoria) REFERENCES categoria(id)
);

CREATE TABLE cotizacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
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

    CONSTRAINT fk_cotizacion_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente(id),

    CONSTRAINT fk_cotizacion_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id)
);

CREATE TABLE detalle_cotizacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_cotizacion INT NOT NULL,
    numero_linea INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad DECIMAL(12,3) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    descuento DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    importe DECIMAL(12,2) NOT NULL,

    CONSTRAINT fk_detalle_cotizacion_cotizacion
        FOREIGN KEY (id_cotizacion) REFERENCES cotizacion(id),

    CONSTRAINT fk_detalle_cotizacion_producto
        FOREIGN KEY (id_producto) REFERENCES producto(id)
);

-- ============ DATOS DE PRUEBA ============

INSERT INTO categoria (nombre, descripcion, estado) VALUES
('Herramientas Manuales', 'Martillos, alicates, destornilladores', TRUE),
('Materiales Eléctricos', 'Cables, enchufes industriales', TRUE),
('Construcción', 'Cemento, fierro, agregados', TRUE);

INSERT INTO producto (
    codigo_interno, codigo_fabricante, codigo_proveedor, nombre,
    id_categoria, unidad_compra, unidad_venta, factor_conversion,
    precio_venta, costo_unitario, stock_actual, stock_minimo, estado
) VALUES
('INT-001', 'FAB-101', 'PROV-11', 'Martillo de Acero', 1, 'UNIDAD', 'UNIDAD', 1.000, 45.00, 25.00, 100.000, 10.000, TRUE),
('INT-002', 'FAB-102', 'PROV-22', 'Alicate de Presión', 1, 'UNIDAD', 'UNIDAD', 1.000, 35.00, 18.00, 50.000, 5.000, TRUE),
('INT-003', 'FAB-103', 'PROV-33', 'Cable Mellizo 2x14', 2, 'METRO', 'METRO', 1.000, 2.50, 1.20, 500.000, 100.000, TRUE),
('INT-004', 'FAB-104', 'PROV-44', 'Enchufe Mennekes', 2, 'UNIDAD', 'UNIDAD', 1.000, 85.00, 50.00, 30.000, 5.000, TRUE),
('INT-005', 'FAB-105', 'PROV-55', 'Bolsa Cemento Sol', 3, 'UNIDAD', 'UNIDAD', 1.000, 28.50, 24.00, 200.000, 50.000, TRUE),
('INT-006', 'FAB-106', 'PROV-66', 'Fierro Corrugado', 3, 'UNIDAD', 'UNIDAD', 1.000, 32.00, 26.00, 150.000, 30.000, TRUE);

INSERT INTO cotizacion (
    numero, fecha_emision, moneda, sub_total, igv, total,
    fecha_registro, anulado, id_cliente, estado
) VALUES
('COT-0001', '2026-09-14', 'SOLES', 147.00, 26.46, 173.46,
 NOW(), FALSE, 1, 'ACEPTADA');

INSERT INTO detalle_cotizacion (
    id_cotizacion, numero_linea, id_producto,
    cantidad, precio_unitario, descuento, importe
) VALUES
(1, 1, 1, 2.000, 45.00, 5.00, 85.00),
(1, 2, 3, 10.000, 2.50, 0.00, 25.00),
(1, 3, 5, 1.000, 28.50, 0.00, 28.50);
