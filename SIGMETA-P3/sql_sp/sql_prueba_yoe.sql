USE sigmeta;

-- =========================================================================
-- SCRIPT DE PRUEBAS PARA PROCEDIMIENTOS ALMACENADOS - MÓDULO SEGURIDAD Y MAESTROS
-- Tablas: rol, usuario, cliente, proveedor, solicitud_autorizacion
-- Requiere los datos de sigmeta.sql (roles 1..4 y el administrador con id = 4).
-- nombre_usuario, numero_documento y ruc son UNIQUE: para volver a correrlo cambie
-- 'rflores', 'jperez', los DNI/RUC de los clientes y los RUC de los proveedores.
-- =========================================================================

-- ---------------------------------------------------------
-- 1. PRUEBA: ROL
-- ---------------------------------------------------------
-- Los cuatro roles son un catalogo fijo (RF002), por eso no se ejecutan sp_rol_insertar ni
-- sp_rol_eliminar: un segundo rol del mismo tipo haria ambiguo sp_rol_buscar_por_tipo.
-- Se prueban las consultas y una modificacion que luego se revierte.
CALL sp_rol_listar();
CALL sp_rol_obtener(1);
CALL sp_rol_buscar_por_tipo('VENDEDOR');

CALL sp_rol_modificar(1, 'VENDEDOR', 'Atencion al cliente, cotizaciones y ventas', TRUE);
CALL sp_rol_obtener(1);
CALL sp_rol_modificar(1, 'VENDEDOR', 'Atencion al cliente y ciclo comercial', TRUE);


-- ---------------------------------------------------------
-- 2. PRUEBA: USUARIO
-- ---------------------------------------------------------
-- Insertar dos usuarios (vendedor y almacenero); la clave real la genera la capa de negocio (PBKDF2)
CALL sp_usuario_insertar(@id_usu1, 'rflores', 'hash_rflores', 'salt_rflores', 'Rosa', 'Flores Diaz', 'rflores@grupometa.pe', 1);
CALL sp_usuario_insertar(@id_usu2, 'jperez', 'hash_jperez', 'salt_jperez', 'Jose', 'Perez Luna', 'jperez@grupometa.pe', 3);
SELECT @id_usu1 AS 'ID Usuario 1', @id_usu2 AS 'ID Usuario 2';

-- Modificar el correo y apellidos del primer usuario
CALL sp_usuario_modificar(@id_usu1, 'rflores', 'hash_rflores', 'salt_rflores', 'Rosa', 'Flores Diaz de Vega', 'rosa.flores@grupometa.pe', 1, TRUE);

-- Obtener por ID, buscar por nombre de usuario (inicio de sesion, RF001) y listar
CALL sp_usuario_obtener(@id_usu1);
CALL sp_usuario_buscar_por_nombre_usuario('rflores');
CALL sp_usuario_listar();
CALL sp_usuario_listar_por_estado(TRUE);


-- ---------------------------------------------------------
-- 3. PRUEBA: CLIENTE
-- ---------------------------------------------------------
-- Insertar un cliente persona natural (DNI) y uno empresa (RUC)
CALL sp_cliente_insertar(@id_cli1, 'Carmen Rosa Huaman Torres', 'Av. Grau 450, Chosica', '987111222', 'chuaman@gmail.com', 'DNI', '41236587', 'Carmen Huaman', 'CONTADO', 0, 0.00, NULL);
CALL sp_cliente_insertar(@id_cli2, 'Metalmecanica del Centro S.A.C.', 'Av. Industrial 980, Ate', '013456789', 'compras@metalcentro.pe', 'RUC', '20601234567', 'Victor Salas', 'CONTADO', 0, 0.00, NULL);
SELECT @id_cli1 AS 'ID Cliente 1', @id_cli2 AS 'ID Cliente 2';

-- Modificar el cliente empresa: el Administrador le asigna linea de credito (RF003)
CALL sp_cliente_modificar(@id_cli2, 'Metalmecanica del Centro S.A.C.', 'Av. Industrial 980, Ate', '013456789', 'compras@metalcentro.pe', 'RUC', '20601234567', 'Victor Salas', 'CREDITO', 30, 15000.00, 'B', TRUE);

-- Obtener por ID y probar las busquedas (documento, razon social y estado)
CALL sp_cliente_obtener(@id_cli2);
CALL sp_cliente_buscar_por_documento('41236587');
CALL sp_cliente_buscar_por_razon_social('Metalmecanica');
CALL sp_cliente_listar();
CALL sp_cliente_listar_por_estado(TRUE);


-- ---------------------------------------------------------
-- 4. PRUEBA: PROVEEDOR
-- ---------------------------------------------------------
CALL sp_proveedor_insertar(@id_prov1, 'Distribuidora Ferretera Lima S.A.', 'Jr. Paruro 1020, Cercado de Lima', '014267788', 'ventas@dferreteralima.pe', '20512345678', 'Herramientas manuales', 'Ana Rios', 5, 'CONTADO');
CALL sp_proveedor_insertar(@id_prov2, 'Cables y Conductores del Peru S.A.C.', 'Av. Argentina 3050, Callao', '014518899', 'pedidos@cablesperu.pe', '20587654321', 'Material electrico', 'Raul Mendoza', 7, 'CREDITO');
SELECT @id_prov1 AS 'ID Proveedor 1', @id_prov2 AS 'ID Proveedor 2';

-- Modificar el plazo de entrega del primer proveedor
CALL sp_proveedor_modificar(@id_prov1, 'Distribuidora Ferretera Lima S.A.', 'Jr. Paruro 1020, Cercado de Lima', '014267788', 'ventas@dferreteralima.pe', '20512345678', 'Herramientas manuales', 'Ana Rios', 3, 'CONTADO', TRUE);

-- Obtener por ID y probar las busquedas (RUC, razon social y estado)
CALL sp_proveedor_obtener(@id_prov1);
CALL sp_proveedor_buscar_por_ruc('20587654321');
CALL sp_proveedor_buscar_por_razon_social('Cables');
CALL sp_proveedor_listar();
CALL sp_proveedor_listar_por_estado(TRUE);


-- ---------------------------------------------------------
-- 5. PRUEBA: SOLICITUD DE AUTORIZACION (RF014)
-- ---------------------------------------------------------
-- El almacenero creado (@id_usu2) solicita autorizacion para un ajuste de inventario.
-- Pendiente: sin administrador, sin resolucion y con vigencia 0 (vigencia_minutos es NOT NULL).
CALL sp_solicitud_autorizacion_insertar(@id_sol1, @id_usu2, NULL, 'AJUSTE_INVENTARIO', 'Diferencia en conteo fisico de clavos', NOW(), 'PENDIENTE', NULL, 0, NULL);
SELECT @id_sol1 AS 'ID Solicitud';

-- El Administrador (id 4) revisa las pendientes y la aprueba con 30 minutos de vigencia
CALL sp_solicitud_autorizacion_listar_por_estado('PENDIENTE');
CALL sp_solicitud_autorizacion_modificar(@id_sol1, @id_usu2, 4, 'AJUSTE_INVENTARIO', 'Diferencia en conteo fisico de clavos',
                                         NOW(), 'APROBADA', NOW(), 30, DATE_ADD(NOW(), INTERVAL 30 MINUTE));

-- La autorizacion aprobada y vigente debe encontrarse
CALL sp_solicitud_autorizacion_buscar_vigente(@id_usu2, 'AJUSTE_INVENTARIO');
CALL sp_solicitud_autorizacion_obtener(@id_sol1);
CALL sp_solicitud_autorizacion_listar_por_solicitante(@id_usu2);
CALL sp_solicitud_autorizacion_listar();


-- ---------------------------------------------------------
-- 6. PRUEBAS DE ELIMINACIÓN (BAJA LÓGICA)
-- ---------------------------------------------------------
-- Revocar la solicitud aprobada: pasa a VENCIDA con fecha_vencimiento = ahora
CALL sp_solicitud_autorizacion_eliminar(@id_sol1);
-- Ya no debe existir una autorizacion vigente (resultado vacio)
CALL sp_solicitud_autorizacion_buscar_vigente(@id_usu2, 'AJUSTE_INVENTARIO');

-- Desactivar usuario 2, cliente 1 y proveedor 2 (estado = FALSE, conservan su historial)
CALL sp_usuario_eliminar(@id_usu2);
CALL sp_cliente_eliminar(@id_cli1);
CALL sp_proveedor_eliminar(@id_prov2);


-- ---------------------------------------------------------
-- 7. COMPROBACIÓN FINAL
-- ---------------------------------------------------------
-- En los listados de activos ya NO deben aparecer el usuario 2, el cliente 1 ni el proveedor 2;
-- en los listados de inactivos si deben aparecer.
SELECT '--- LISTADO FINAL POST-ELIMINACIÓN ---' AS 'INFO';
CALL sp_usuario_listar();
CALL sp_usuario_listar_por_estado(FALSE);
CALL sp_cliente_listar();
CALL sp_cliente_listar_por_estado(FALSE);
CALL sp_proveedor_listar();
CALL sp_proveedor_listar_por_estado(FALSE);
SELECT id, estado, fecha_vencimiento FROM solicitud_autorizacion WHERE id = @id_sol1;
