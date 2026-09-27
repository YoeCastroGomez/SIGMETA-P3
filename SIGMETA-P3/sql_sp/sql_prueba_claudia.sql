-- =========================================================================
-- SCRIPT DE PRUEBAS PARA PROCEDIMIENTOS ALMACENADOS - MÓDULO CATÁLOGO
-- =========================================================================

-- ---------------------------------------------------------
-- 1. PRUEBA: CATEGORÍA
-- ---------------------------------------------------------
-- Insertar dos categorías (usamos variables @ para capturar los IDs generados)
CALL sp_categoria_insertar(@id_cat1, 'Herramientas de Potencia', 'Taladros, sierras, amoladoras');
CALL sp_categoria_insertar(@id_cat2, 'Ferretería General', 'Clavos, tornillos, tuercas');
SELECT @id_cat1 AS 'ID Categoria 1', @id_cat2 AS 'ID Categoria 2';

-- Modificar la primera categoría
CALL sp_categoria_modificar(@id_cat1, 'Herramientas Eléctricas', 'Taladros, sierras, amoladoras, lijadoras');

-- Listar todas las categorías activas y obtener la primera por ID
CALL sp_categoria_listar();
CALL sp_categoria_obtener(@id_cat1);


-- ---------------------------------------------------------
-- 2. PRUEBA: PRODUCTO
-- ---------------------------------------------------------
-- Insertar dos productos usando el ID de la categoría que acabamos de crear (@id_cat1 y @id_cat2)
CALL sp_producto_insertar(@id_prod1, 'PROD-T01', 'FAB-001', 'PRV-001', 'Taladro Percutor 710W', 'Taladro industrial', @id_cat1, 'UNIDAD', 'UNIDAD', 1.000, 250.00, 180.00, 50.000, 5.000, 'url_imagen1.jpg', 260.00);
CALL sp_producto_insertar(@id_prod2, 'PROD-C01', 'FAB-002', 'PRV-002', 'Caja de Clavos 2 pulg', 'Clavos de acero', @id_cat2, 'CAJA', 'CAJA', 1.000, 15.00, 10.00, 100.000, 20.000, 'url_imagen2.jpg', 16.00);
SELECT @id_prod1 AS 'ID Producto 1', @id_prod2 AS 'ID Producto 2';

-- Modificar el precio y stock del primer producto
CALL sp_producto_modificar(@id_prod1, 'PROD-T01', 'FAB-001', 'PRV-001', 'Taladro Percutor 750W', 'Modelo actualizado', @id_cat1, 'UNIDAD', 'UNIDAD', 1.000, 270.00, 185.00, 48.000, 5.000, 'url_imagen1_nueva.jpg', 280.00);

-- Listar todos los productos activos y obtener el primero por ID
CALL sp_producto_listar();
CALL sp_producto_obtener(@id_prod1);


-- ---------------------------------------------------------
-- 3. PRUEBA: COTIZACIÓN
-- ---------------------------------------------------------
-- Insertar una cotización (OJO: Asumimos que id_usuario_registro=1 e id_cliente=2 ya existen en tu BD)
CALL sp_cotizacion_insertar(@id_cot1, 'COT-TEST-001', '2026-09-27', 'SOLES', 265.00, 47.70, 312.70, 'Cotización de prueba SP', 1, 2, '2026-10-05', 'REGISTRADA');
SELECT @id_cot1 AS 'ID Cotizacion Generada';

-- Modificar la cotización (cambiando su estado a ENVIADA y la fecha de vigencia)
CALL sp_cotizacion_modificar(@id_cot1, 'COT-TEST-001-REV', '2026-09-27', 'SOLES', 265.00, 47.70, 312.70, 'Cotización revisada', 2, '2026-10-10', 'ENVIADA');

-- Listar cotizaciones no anuladas y obtenerla por ID
CALL sp_cotizacion_listar();
CALL sp_cotizacion_obtener(@id_cot1);


-- ---------------------------------------------------------
-- 4. PRUEBA: DETALLE COTIZACIÓN
-- ---------------------------------------------------------
-- Insertar detalles para la cotización generada, usando los productos creados arriba
CALL sp_detalle_cotizacion_insertar(@id_det1, @id_cot1, 1, @id_prod1, 1.000, 250.00, 0.00, 250.00);
CALL sp_detalle_cotizacion_insertar(@id_det2, @id_cot1, 2, @id_prod2, 1.000, 15.00, 0.00, 15.00);
SELECT @id_det1 AS 'ID Detalle 1', @id_det2 AS 'ID Detalle 2';

-- Modificar cantidad e importe del detalle 2 (ahora son 2 cajas en vez de 1)
CALL sp_detalle_cotizacion_modificar(@id_det2, 2, @id_prod2, 2.000, 15.00, 0.00, 30.00);

-- Listar todos los detalles que pertenecen a la cotización específica
CALL sp_detalle_cotizacion_listar(@id_cot1);
CALL sp_detalle_cotizacion_obtener(@id_det1);


-- ---------------------------------------------------------
-- 5. PRUEBAS DE ELIMINACIÓN / ANULACIÓN
-- ---------------------------------------------------------
-- Eliminar un detalle (Borrado físico)
CALL sp_detalle_cotizacion_eliminar(@id_det2);

-- Eliminar/Anular la cotización (Borrado lógico: pasa a estado='ANULADA', anulado=TRUE)
CALL sp_cotizacion_eliminar(@id_cot1, 'El cliente canceló la solicitud');

-- Eliminar producto 2 (Borrado lógico: estado=FALSE)
CALL sp_producto_eliminar(@id_prod2);

-- Eliminar categoría 2 (Borrado lógico: estado=FALSE)
CALL sp_categoria_eliminar(@id_cat2);

-- ---------------------------------------------------------
-- 6. COMPROBACIÓN FINAL
-- ---------------------------------------------------------
-- Al ejecutar estos listados, ya NO deberían aparecer la cotización, el producto 2 ni la categoría 2
SELECT '--- LISTADO FINAL POST-ELIMINACIÓN ---' AS 'INFO';
CALL sp_categoria_listar();
CALL sp_producto_listar();
CALL sp_cotizacion_listar();