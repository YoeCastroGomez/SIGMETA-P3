-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: CATEGORIA
-- ==========================================
DELIMITER $$
CREATE PROCEDURE sp_categoria_insertar(
    OUT p_id_categoria INT,
    IN p_nombre VARCHAR(100),
    IN p_descripcion VARCHAR(255)
)
BEGIN
    INSERT INTO categoria (nombre, descripcion, estado) 
    VALUES (p_nombre, p_descripcion, TRUE);
    SET p_id_categoria = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_categoria_modificar(
    IN p_id_categoria INT,
    IN p_nombre VARCHAR(100),
    IN p_descripcion VARCHAR(255)
)
BEGIN
    UPDATE categoria 
    SET nombre = p_nombre, descripcion = p_descripcion
    WHERE id = p_id_categoria;
END$$

CREATE PROCEDURE sp_categoria_eliminar(IN p_id_categoria INT)
BEGIN
    UPDATE categoria SET estado = FALSE WHERE id = p_id_categoria;
END$$

CREATE PROCEDURE sp_categoria_obtener(IN p_id_categoria INT)
BEGIN
    SELECT * FROM categoria WHERE id = p_id_categoria;
END$$

CREATE PROCEDURE sp_categoria_listar()
BEGIN
    SELECT * FROM categoria WHERE estado = TRUE;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: PRODUCTO
-- ==========================================
CREATE PROCEDURE sp_producto_insertar(
    OUT p_id_producto INT,
    IN p_codigo_interno VARCHAR(50),
    IN p_codigo_fabricante VARCHAR(50),
    IN p_codigo_proveedor VARCHAR(50),
    IN p_nombre VARCHAR(150),
    IN p_descripcion VARCHAR(255),
    IN p_id_categoria INT,
    IN p_unidad_compra ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO'),
    IN p_unidad_venta ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO'),
    IN p_factor_conversion DECIMAL(12,3),
    IN p_precio_venta DECIMAL(12,2),
    IN p_costo_unitario DECIMAL(12,2),
    IN p_stock_actual DECIMAL(12,3),
    IN p_stock_minimo DECIMAL(12,3),
    IN p_imagen VARCHAR(255),
    IN p_precio_referencial DECIMAL(12,2)
)
BEGIN
    INSERT INTO producto (
        codigo_interno, codigo_fabricante, codigo_proveedor, nombre, descripcion, id_categoria, 
        unidad_compra, unidad_venta, factor_conversion, precio_venta, costo_unitario, 
        stock_actual, stock_minimo, imagen, estado, precio_referencial
    ) VALUES (
        p_codigo_interno, p_codigo_fabricante, p_codigo_proveedor, p_nombre, p_descripcion, p_id_categoria, 
        p_unidad_compra, p_unidad_venta, p_factor_conversion, p_precio_venta, p_costo_unitario, 
        p_stock_actual, p_stock_minimo, p_imagen, TRUE, p_precio_referencial
    );
    SET p_id_producto = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_producto_modificar(
    IN p_id_producto INT,
    IN p_codigo_interno VARCHAR(50),
    IN p_codigo_fabricante VARCHAR(50),
    IN p_codigo_proveedor VARCHAR(50),
    IN p_nombre VARCHAR(150),
    IN p_descripcion VARCHAR(255),
    IN p_id_categoria INT,
    IN p_unidad_compra ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO'),
    IN p_unidad_venta ENUM('UNIDAD', 'CAJA', 'KILOGRAMO', 'METRO', 'LITRO', 'MILLAR', 'GALON', 'JUEGO'),
    IN p_factor_conversion DECIMAL(12,3),
    IN p_precio_venta DECIMAL(12,2),
    IN p_costo_unitario DECIMAL(12,2),
    IN p_stock_actual DECIMAL(12,3),
    IN p_stock_minimo DECIMAL(12,3),
    IN p_imagen VARCHAR(255),
    IN p_precio_referencial DECIMAL(12,2)
)
BEGIN
    UPDATE producto SET 
        codigo_interno = p_codigo_interno, codigo_fabricante = p_codigo_fabricante, codigo_proveedor = p_codigo_proveedor, 
        nombre = p_nombre, descripcion = p_descripcion, id_categoria = p_id_categoria, unidad_compra = p_unidad_compra, 
        unidad_venta = p_unidad_venta, factor_conversion = p_factor_conversion, precio_venta = p_precio_venta, 
        costo_unitario = p_costo_unitario, stock_actual = p_stock_actual, stock_minimo = p_stock_minimo, 
        imagen = p_imagen, precio_referencial = p_precio_referencial
    WHERE id = p_id_producto;
END$$

CREATE PROCEDURE sp_producto_eliminar(IN p_id_producto INT)
BEGIN
    UPDATE producto SET estado = FALSE WHERE id = p_id_producto;
END$$

CREATE PROCEDURE sp_producto_obtener(IN p_id_producto INT)
BEGIN
    SELECT * FROM producto WHERE id = p_id_producto;
END$$

CREATE PROCEDURE sp_producto_listar()
BEGIN
    SELECT * FROM producto WHERE estado = TRUE;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: COTIZACION
-- ==========================================
CREATE PROCEDURE sp_cotizacion_insertar(
    OUT p_id_cotizacion INT,
    IN p_numero VARCHAR(20),
    IN p_fecha_emision DATE,
    IN p_moneda ENUM('SOLES', 'DOLARES'),
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(255),
    IN p_id_usuario_registro INT,
    IN p_id_cliente INT,
    IN p_fecha_vigencia DATE,
    IN p_estado ENUM('REGISTRADA', 'ENVIADA', 'ACEPTADA', 'RECHAZADA', 'VENCIDA', 'ANULADA')
)
BEGIN
    INSERT INTO cotizacion (
        numero, fecha_emision, moneda, sub_total, igv, total, observaciones, fecha_registro, 
        id_usuario_registro, anulado, id_cliente, fecha_vigencia, estado
    ) VALUES (
        p_numero, p_fecha_emision, p_moneda, p_sub_total, p_igv, p_total, p_observaciones, NOW(), 
        p_id_usuario_registro, FALSE, p_id_cliente, p_fecha_vigencia, p_estado
    );
    SET p_id_cotizacion = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_cotizacion_modificar(
    IN p_id_cotizacion INT,
    IN p_numero VARCHAR(20),
    IN p_fecha_emision DATE,
    IN p_moneda ENUM('SOLES', 'DOLARES'),
    IN p_sub_total DECIMAL(12,2),
    IN p_igv DECIMAL(12,2),
    IN p_total DECIMAL(12,2),
    IN p_observaciones VARCHAR(255),
    IN p_id_cliente INT,
    IN p_fecha_vigencia DATE,
    IN p_estado ENUM('REGISTRADA', 'ENVIADA', 'ACEPTADA', 'RECHAZADA', 'VENCIDA', 'ANULADA')
)
BEGIN
    UPDATE cotizacion SET 
        numero = p_numero, fecha_emision = p_fecha_emision, moneda = p_moneda, sub_total = p_sub_total, 
        igv = p_igv, total = p_total, observaciones = p_observaciones, id_cliente = p_id_cliente, 
        fecha_vigencia = p_fecha_vigencia, estado = p_estado
    WHERE id = p_id_cotizacion;
END$$

CREATE PROCEDURE sp_cotizacion_eliminar(
    IN p_id_cotizacion INT, 
    IN p_motivo_anulacion VARCHAR(255)
)
BEGIN
    UPDATE cotizacion 
    SET anulado = TRUE, motivo_anulacion = p_motivo_anulacion, fecha_anulacion = NOW(), estado = 'ANULADA'
    WHERE id = p_id_cotizacion;
END$$

CREATE PROCEDURE sp_cotizacion_obtener(IN p_id_cotizacion INT)
BEGIN
    SELECT * FROM cotizacion WHERE id = p_id_cotizacion;
END$$

CREATE PROCEDURE sp_cotizacion_listar()
BEGIN
    SELECT * FROM cotizacion WHERE anulado = FALSE;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: DETALLE_COTIZACION
-- ==========================================
CREATE PROCEDURE sp_detalle_cotizacion_insertar(
    OUT p_id_detalle_cotizacion INT,
    IN p_id_cotizacion INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2)
)
BEGIN
    INSERT INTO detalle_cotizacion (
        id_cotizacion, numero_linea, id_producto, cantidad, precio_unitario, descuento, importe
    ) VALUES (
        p_id_cotizacion, p_numero_linea, p_id_producto, p_cantidad, p_precio_unitario, p_descuento, p_importe
    );
    SET p_id_detalle_cotizacion = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_detalle_cotizacion_modificar(
    IN p_id_detalle_cotizacion INT,
    IN p_numero_linea INT,
    IN p_id_producto INT,
    IN p_cantidad DECIMAL(12,3),
    IN p_precio_unitario DECIMAL(12,2),
    IN p_descuento DECIMAL(12,2),
    IN p_importe DECIMAL(12,2)
)
BEGIN
    UPDATE detalle_cotizacion SET 
        numero_linea = p_numero_linea, id_producto = p_id_producto, cantidad = p_cantidad, 
        precio_unitario = p_precio_unitario, descuento = p_descuento, importe = p_importe
    WHERE id = p_id_detalle_cotizacion;
END$$

CREATE PROCEDURE sp_detalle_cotizacion_eliminar(IN p_id_detalle_cotizacion INT)
BEGIN
    -- Como la tabla detalle_cotizacion no tiene campo 'estado' o 'anulado', 
    -- y los detalles se manejan desde la cabecera, aquí aplicamos borrado físico si se elimina una línea.
    DELETE FROM detalle_cotizacion WHERE id = p_id_detalle_cotizacion;
END$$

CREATE PROCEDURE sp_detalle_cotizacion_obtener(IN p_id_detalle_cotizacion INT)
BEGIN
    SELECT * FROM detalle_cotizacion WHERE id = p_id_detalle_cotizacion;
END$$

CREATE PROCEDURE sp_detalle_cotizacion_listar(IN p_id_cotizacion INT)
BEGIN
    SELECT * FROM detalle_cotizacion WHERE id_cotizacion = p_id_cotizacion;
END$$

-- RF006: busqueda de categorias por estado (activas o desactivadas)
CREATE PROCEDURE sp_categoria_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT * FROM categoria WHERE estado = p_estado;
END$$

-- RF005: busqueda de productos por estado (activos o desactivados)
CREATE PROCEDURE sp_producto_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT * FROM producto WHERE estado = p_estado;
END$$

DELIMITER ;