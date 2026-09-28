-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: ROL
-- ==========================================
DELIMITER $$
CREATE PROCEDURE sp_rol_insertar(
    OUT p_id_rol INT,
    IN p_tipo ENUM('VENDEDOR', 'CAJERO', 'ALMACENERO', 'ADMINISTRADOR'),
    IN p_descripcion VARCHAR(150)
)
BEGIN
    INSERT INTO rol (tipo, descripcion, estado)
    VALUES (p_tipo, p_descripcion, TRUE);
    SET p_id_rol = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_rol_modificar(
    IN p_id_rol INT,
    IN p_tipo ENUM('VENDEDOR', 'CAJERO', 'ALMACENERO', 'ADMINISTRADOR'),
    IN p_descripcion VARCHAR(150),
    IN p_estado BOOLEAN
)
BEGIN
    UPDATE rol
    SET tipo = p_tipo, descripcion = p_descripcion, estado = p_estado
    WHERE id_rol = p_id_rol;
END$$

CREATE PROCEDURE sp_rol_eliminar(IN p_id_rol INT)
BEGIN
    UPDATE rol SET estado = FALSE WHERE id_rol = p_id_rol;
END$$

CREATE PROCEDURE sp_rol_obtener(IN p_id_rol INT)
BEGIN
    SELECT * FROM rol WHERE id_rol = p_id_rol;
END$$

CREATE PROCEDURE sp_rol_listar()
BEGIN
    SELECT * FROM rol WHERE estado = TRUE;
END$$

-- RF002: al registrar un usuario se le asigna un rol por su tipo
CREATE PROCEDURE sp_rol_buscar_por_tipo(
    IN p_tipo ENUM('VENDEDOR', 'CAJERO', 'ALMACENERO', 'ADMINISTRADOR')
)
BEGIN
    SELECT * FROM rol WHERE tipo = p_tipo;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: USUARIO
-- ==========================================
CREATE PROCEDURE sp_usuario_insertar(
    OUT p_id_usuario INT,
    IN p_nombre_usuario VARCHAR(50),
    IN p_clave_hash VARCHAR(255),
    IN p_salt VARCHAR(64),
    IN p_nombres VARCHAR(100),
    IN p_apellidos VARCHAR(100),
    IN p_correo VARCHAR(120),
    IN p_id_rol INT
)
BEGIN
    INSERT INTO usuario (
        nombre_usuario, clave_hash, salt, nombres, apellidos, correo, id_rol, estado, fecha_registro
    ) VALUES (
        p_nombre_usuario, p_clave_hash, p_salt, p_nombres, p_apellidos, p_correo, p_id_rol, TRUE, NOW()
    );
    SET p_id_usuario = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_usuario_modificar(
    IN p_id_usuario INT,
    IN p_nombre_usuario VARCHAR(50),
    IN p_clave_hash VARCHAR(255),
    IN p_salt VARCHAR(64),
    IN p_nombres VARCHAR(100),
    IN p_apellidos VARCHAR(100),
    IN p_correo VARCHAR(120),
    IN p_id_rol INT,
    IN p_estado BOOLEAN
)
BEGIN
    UPDATE usuario SET
        nombre_usuario = p_nombre_usuario, clave_hash = p_clave_hash, salt = p_salt,
        nombres = p_nombres, apellidos = p_apellidos, correo = p_correo, id_rol = p_id_rol,
        estado = p_estado
    WHERE id_usuario = p_id_usuario;
END$$

CREATE PROCEDURE sp_usuario_eliminar(IN p_id_usuario INT)
BEGIN
    -- Baja logica: el usuario desactivado conserva las operaciones que registro (RF002).
    UPDATE usuario SET estado = FALSE WHERE id_usuario = p_id_usuario;
END$$

CREATE PROCEDURE sp_usuario_obtener(IN p_id_usuario INT)
BEGIN
    SELECT u.*, r.tipo, r.descripcion, r.estado AS estado_rol
    FROM usuario u INNER JOIN rol r ON u.id_rol = r.id_rol
    WHERE u.id_usuario = p_id_usuario;
END$$

CREATE PROCEDURE sp_usuario_listar()
BEGIN
    SELECT u.*, r.tipo, r.descripcion, r.estado AS estado_rol
    FROM usuario u INNER JOIN rol r ON u.id_rol = r.id_rol
    WHERE u.estado = TRUE;
END$$

-- RF001: inicio de sesion. No filtra por estado para que la capa de negocio
-- pueda distinguir entre "usuario inexistente" y "usuario desactivado".
CREATE PROCEDURE sp_usuario_buscar_por_nombre_usuario(IN p_nombre_usuario VARCHAR(50))
BEGIN
    SELECT u.*, r.tipo, r.descripcion, r.estado AS estado_rol
    FROM usuario u INNER JOIN rol r ON u.id_rol = r.id_rol
    WHERE u.nombre_usuario = p_nombre_usuario;
END$$

-- RF002: el Administrador consulta usuarios activos o desactivados
CREATE PROCEDURE sp_usuario_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT u.*, r.tipo, r.descripcion, r.estado AS estado_rol
    FROM usuario u INNER JOIN rol r ON u.id_rol = r.id_rol
    WHERE u.estado = p_estado;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: CLIENTE
-- ==========================================
CREATE PROCEDURE sp_cliente_insertar(
    OUT p_id_cliente INT,
    IN p_razon_social VARCHAR(150),
    IN p_direccion VARCHAR(200),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(120),
    IN p_tipo_documento ENUM('RUC', 'DNI', 'CARNET_EXTRANJERIA', 'PASAPORTE'),
    IN p_numero_documento VARCHAR(20),
    IN p_contacto_nombre VARCHAR(100),
    IN p_condicion_pago ENUM('CONTADO', 'CREDITO'),
    IN p_plazo_credito_dias INT,
    IN p_limite_credito DECIMAL(12,2),
    IN p_calificacion_crediticia VARCHAR(20)
)
BEGIN
    INSERT INTO cliente (
        razon_social, direccion, telefono, correo, estado, tipo_documento, numero_documento,
        contacto_nombre, condicion_pago, plazo_credito_dias, limite_credito, calificacion_crediticia
    ) VALUES (
        p_razon_social, p_direccion, p_telefono, p_correo, TRUE, p_tipo_documento, p_numero_documento,
        p_contacto_nombre, p_condicion_pago, p_plazo_credito_dias, p_limite_credito, p_calificacion_crediticia
    );
    SET p_id_cliente = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_cliente_modificar(
    IN p_id_cliente INT,
    IN p_razon_social VARCHAR(150),
    IN p_direccion VARCHAR(200),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(120),
    IN p_tipo_documento ENUM('RUC', 'DNI', 'CARNET_EXTRANJERIA', 'PASAPORTE'),
    IN p_numero_documento VARCHAR(20),
    IN p_contacto_nombre VARCHAR(100),
    IN p_condicion_pago ENUM('CONTADO', 'CREDITO'),
    IN p_plazo_credito_dias INT,
    IN p_limite_credito DECIMAL(12,2),
    IN p_calificacion_crediticia VARCHAR(20),
    IN p_estado BOOLEAN
)
BEGIN
    UPDATE cliente SET
        razon_social = p_razon_social, direccion = p_direccion, telefono = p_telefono, correo = p_correo,
        tipo_documento = p_tipo_documento, numero_documento = p_numero_documento,
        contacto_nombre = p_contacto_nombre, condicion_pago = p_condicion_pago,
        plazo_credito_dias = p_plazo_credito_dias, limite_credito = p_limite_credito,
        calificacion_crediticia = p_calificacion_crediticia, estado = p_estado
    WHERE id_cliente = p_id_cliente;
END$$

CREATE PROCEDURE sp_cliente_eliminar(IN p_id_cliente INT)
BEGIN
    -- Baja logica: el cliente conserva su historial comercial (RF003).
    UPDATE cliente SET estado = FALSE WHERE id_cliente = p_id_cliente;
END$$

CREATE PROCEDURE sp_cliente_obtener(IN p_id_cliente INT)
BEGIN
    SELECT * FROM cliente WHERE id_cliente = p_id_cliente;
END$$

CREATE PROCEDURE sp_cliente_listar()
BEGIN
    SELECT * FROM cliente WHERE estado = TRUE;
END$$

-- RF003: busqueda por documento
CREATE PROCEDURE sp_cliente_buscar_por_documento(IN p_numero_documento VARCHAR(20))
BEGIN
    SELECT * FROM cliente WHERE numero_documento = p_numero_documento;
END$$

-- RF003: busqueda por razon social (coincidencia parcial)
CREATE PROCEDURE sp_cliente_buscar_por_razon_social(IN p_razon_social VARCHAR(150))
BEGIN
    SELECT * FROM cliente WHERE razon_social LIKE CONCAT('%', p_razon_social, '%');
END$$

-- RF003: busqueda por estado
CREATE PROCEDURE sp_cliente_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT * FROM cliente WHERE estado = p_estado;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: PROVEEDOR
-- ==========================================
CREATE PROCEDURE sp_proveedor_insertar(
    OUT p_id_proveedor INT,
    IN p_razon_social VARCHAR(150),
    IN p_direccion VARCHAR(200),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(120),
    IN p_ruc VARCHAR(11),
    IN p_rubro VARCHAR(100),
    IN p_contacto_nombre VARCHAR(100),
    IN p_plazo_entrega_dias INT,
    IN p_condicion_pago ENUM('CONTADO', 'CREDITO')
)
BEGIN
    INSERT INTO proveedor (
        razon_social, direccion, telefono, correo, estado, ruc, rubro,
        contacto_nombre, plazo_entrega_dias, condicion_pago
    ) VALUES (
        p_razon_social, p_direccion, p_telefono, p_correo, TRUE, p_ruc, p_rubro,
        p_contacto_nombre, p_plazo_entrega_dias, p_condicion_pago
    );
    SET p_id_proveedor = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_proveedor_modificar(
    IN p_id_proveedor INT,
    IN p_razon_social VARCHAR(150),
    IN p_direccion VARCHAR(200),
    IN p_telefono VARCHAR(20),
    IN p_correo VARCHAR(120),
    IN p_ruc VARCHAR(11),
    IN p_rubro VARCHAR(100),
    IN p_contacto_nombre VARCHAR(100),
    IN p_plazo_entrega_dias INT,
    IN p_condicion_pago ENUM('CONTADO', 'CREDITO'),
    IN p_estado BOOLEAN
)
BEGIN
    UPDATE proveedor SET
        razon_social = p_razon_social, direccion = p_direccion, telefono = p_telefono, correo = p_correo,
        ruc = p_ruc, rubro = p_rubro, contacto_nombre = p_contacto_nombre,
        plazo_entrega_dias = p_plazo_entrega_dias, condicion_pago = p_condicion_pago, estado = p_estado
    WHERE id_proveedor = p_id_proveedor;
END$$

CREATE PROCEDURE sp_proveedor_eliminar(IN p_id_proveedor INT)
BEGIN
    -- Baja logica: el proveedor conserva su historial de compras (RF004).
    UPDATE proveedor SET estado = FALSE WHERE id_proveedor = p_id_proveedor;
END$$

CREATE PROCEDURE sp_proveedor_obtener(IN p_id_proveedor INT)
BEGIN
    SELECT * FROM proveedor WHERE id_proveedor = p_id_proveedor;
END$$

CREATE PROCEDURE sp_proveedor_listar()
BEGIN
    SELECT * FROM proveedor WHERE estado = TRUE;
END$$

-- RF004: busqueda por RUC
CREATE PROCEDURE sp_proveedor_buscar_por_ruc(IN p_ruc VARCHAR(11))
BEGIN
    SELECT * FROM proveedor WHERE ruc = p_ruc;
END$$

-- RF004: busqueda por razon social (coincidencia parcial)
CREATE PROCEDURE sp_proveedor_buscar_por_razon_social(IN p_razon_social VARCHAR(150))
BEGIN
    SELECT * FROM proveedor WHERE razon_social LIKE CONCAT('%', p_razon_social, '%');
END$$

-- RF004: busqueda por estado
CREATE PROCEDURE sp_proveedor_listar_por_estado(IN p_estado BOOLEAN)
BEGIN
    SELECT * FROM proveedor WHERE estado = p_estado;
END$$

-- ==========================================
-- PROCEDIMIENTOS ALMACENADOS: SOLICITUD_AUTORIZACION
-- ==========================================
CREATE PROCEDURE sp_solicitud_autorizacion_insertar(
    OUT p_id_solicitud_autorizacion INT,
    IN p_id_solicitante INT,
    IN p_id_administrador INT,
    IN p_operacion_restringida VARCHAR(60),
    IN p_motivo VARCHAR(200),
    IN p_fecha_solicitud DATETIME,
    IN p_estado ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA', 'VENCIDA'),
    IN p_fecha_resolucion DATETIME,
    IN p_vigencia_minutos INT,
    IN p_fecha_vencimiento DATETIME
)
BEGIN
    INSERT INTO solicitud_autorizacion (
        id_solicitante, id_administrador, operacion_restringida, motivo, fecha_solicitud,
        estado, fecha_resolucion, vigencia_minutos, fecha_vencimiento
    ) VALUES (
        p_id_solicitante, p_id_administrador, p_operacion_restringida, p_motivo, p_fecha_solicitud,
        p_estado, p_fecha_resolucion, p_vigencia_minutos, p_fecha_vencimiento
    );
    SET p_id_solicitud_autorizacion = LAST_INSERT_ID();
END$$

CREATE PROCEDURE sp_solicitud_autorizacion_modificar(
    IN p_id_solicitud_autorizacion INT,
    IN p_id_solicitante INT,
    IN p_id_administrador INT,
    IN p_operacion_restringida VARCHAR(60),
    IN p_motivo VARCHAR(200),
    IN p_fecha_solicitud DATETIME,
    IN p_estado ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA', 'VENCIDA'),
    IN p_fecha_resolucion DATETIME,
    IN p_vigencia_minutos INT,
    IN p_fecha_vencimiento DATETIME
)
BEGIN
    UPDATE solicitud_autorizacion SET
        id_solicitante = p_id_solicitante, id_administrador = p_id_administrador,
        operacion_restringida = p_operacion_restringida, motivo = p_motivo,
        fecha_solicitud = p_fecha_solicitud, estado = p_estado, fecha_resolucion = p_fecha_resolucion,
        vigencia_minutos = p_vigencia_minutos, fecha_vencimiento = p_fecha_vencimiento
    WHERE id_solicitud_autorizacion = p_id_solicitud_autorizacion;
END$$

CREATE PROCEDURE sp_solicitud_autorizacion_eliminar(IN p_id_solicitud_autorizacion INT)
BEGIN
    -- Baja logica: la solicitud queda registrada para la trazabilidad (RF014, RNF001).
    -- Una solicitud pendiente se rechaza y una aprobada se revoca venciendola en este momento.
    -- MySQL asigna de izquierda a derecha: fecha_vencimiento se evalua antes de cambiar el estado.
    UPDATE solicitud_autorizacion SET
        fecha_vencimiento = CASE WHEN estado = 'APROBADA' THEN NOW() ELSE fecha_vencimiento END,
        fecha_resolucion = COALESCE(fecha_resolucion, NOW()),
        estado = CASE WHEN estado = 'APROBADA' THEN 'VENCIDA' ELSE 'RECHAZADA' END
    WHERE id_solicitud_autorizacion = p_id_solicitud_autorizacion
      AND estado IN ('PENDIENTE', 'APROBADA');
END$$

CREATE PROCEDURE sp_solicitud_autorizacion_obtener(IN p_id_solicitud_autorizacion INT)
BEGIN
    SELECT * FROM solicitud_autorizacion WHERE id_solicitud_autorizacion = p_id_solicitud_autorizacion;
END$$

CREATE PROCEDURE sp_solicitud_autorizacion_listar()
BEGIN
    SELECT * FROM solicitud_autorizacion;
END$$

-- RF014: el Administrador revisa las solicitudes pendientes
CREATE PROCEDURE sp_solicitud_autorizacion_listar_por_estado(
    IN p_estado ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA', 'VENCIDA')
)
BEGIN
    SELECT * FROM solicitud_autorizacion WHERE estado = p_estado ORDER BY fecha_solicitud;
END$$

-- RF014: cada usuario consulta las solicitudes que realizo
CREATE PROCEDURE sp_solicitud_autorizacion_listar_por_solicitante(IN p_id_solicitante INT)
BEGIN
    SELECT * FROM solicitud_autorizacion WHERE id_solicitante = p_id_solicitante
    ORDER BY fecha_solicitud DESC;
END$$

-- RF014: autorizacion aprobada y aun vigente de un usuario para una operacion
CREATE PROCEDURE sp_solicitud_autorizacion_buscar_vigente(
    IN p_id_solicitante INT,
    IN p_operacion_restringida VARCHAR(60)
)
BEGIN
    SELECT * FROM solicitud_autorizacion
    WHERE id_solicitante = p_id_solicitante
      AND operacion_restringida = p_operacion_restringida
      AND estado = 'APROBADA'
      AND fecha_vencimiento > NOW()
    ORDER BY fecha_vencimiento DESC
    LIMIT 1;
END$$
DELIMITER ;
