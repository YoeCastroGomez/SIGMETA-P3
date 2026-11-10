USE sigmeta;

-- =========================================================================
-- MIGRACION LAB 11 - CLAUDIA (RF006 CATEGORIAS Y RF005 PRODUCTOS)
-- Nombres de categorias y productos escritos siempre igual: MAYUSCULAS, sin
-- tildes y con un solo espacio entre palabras ("Látex y esmaltes" -> "LATEX Y ESMALTES").
-- Desde ahora los BO (TextoNormalizado) guardan asi los nombres;
-- este script arregla los que ya estaban en la base.
--
-- PARTE A - CATEGORIAS:
--   1. Junta las categorias repetidas (las que solo cambian en mayusculas,
--      tildes o espacios, por ejemplo "Construcción", "CONSTRUCCION" y
--      "Construccion"). Se queda una: la activa con el id mas bajo (si
--      ninguna esta activa, la de id mas bajo). Los productos de las otras
--      pasan a la que se queda y las repetidas se borran.
--   2. Escribe todos los nombres en MAYUSCULAS y sin tildes.
--
-- PARTE B - PRODUCTOS:
--   Los productos repetidos NO se juntan (tienen stock, ventas y movimientos).
--   1. Escribe todos los nombres en MAYUSCULAS y sin tildes.
--   2. Si dos productos quedan con el mismo nombre ("Martillo" y "MARTILLO"),
--      el primero (el activo de id mas bajo) se queda con el nombre y a los
--      otros se les agrega su codigo interno: "MARTILLO (INT-998)". Despues el
--      Administrador los renombra o desactiva desde la pantalla Productos.
--
-- Se puede ejecutar mas de una vez: si ya no hay repetidos no cambia nada.
-- Ejecutar en Workbench despues de tener la base con sus SP.
-- =========================================================================

SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- 1. Nombre normalizado de cada categoria (mayusculas, sin tildes, sin espacios de mas)
DROP TEMPORARY TABLE IF EXISTS tmp_cat_clave;
CREATE TEMPORARY TABLE tmp_cat_clave AS
SELECT id,
       estado,
       UPPER(TRIM(
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
           REPLACE(REPLACE(REPLACE(nombre COLLATE utf8mb4_bin, '   ', ' '), '  ', ' '), '  ', ' '),
               'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'), 'ü', 'u'),
               'à', 'a'), 'è', 'e'), 'ì', 'i'), 'ò', 'o'), 'ù', 'u'),
               'Á', 'A'), 'É', 'E'), 'Í', 'I'), 'Ó', 'O'), 'Ú', 'U'), 'Ü', 'U'),
               'À', 'A'), 'È', 'E'), 'Ì', 'I'), 'Ò', 'O'), 'Ù', 'U')
       )) COLLATE utf8mb4_bin AS clave
FROM categoria;

-- 2. Por cada categoria, cual es la que se queda (la activa de id mas bajo)
DROP TEMPORARY TABLE IF EXISTS tmp_cat_queda;
CREATE TEMPORARY TABLE tmp_cat_queda AS
SELECT clave,
       COALESCE(MIN(CASE WHEN estado THEN id END), MIN(id)) AS id_queda
FROM tmp_cat_clave
GROUP BY clave;

DROP TEMPORARY TABLE IF EXISTS tmp_cat_mapa;
CREATE TEMPORARY TABLE tmp_cat_mapa AS
SELECT c.id AS id_viejo, q.id_queda, c.clave
FROM tmp_cat_clave c
JOIN tmp_cat_queda q ON q.clave = c.clave;

-- Para revisar antes del COMMIT: categorias que se van a juntar
SELECT m.id_viejo, cv.nombre AS nombre_actual, m.id_queda, cq.nombre AS se_junta_con
FROM tmp_cat_mapa m
JOIN categoria cv ON cv.id = m.id_viejo
JOIN categoria cq ON cq.id = m.id_queda
WHERE m.id_viejo <> m.id_queda;

-- 3. La que se queda toma la descripcion de una repetida si no tenia
UPDATE categoria c
JOIN tmp_cat_mapa m ON m.id_queda = c.id AND m.id_viejo <> m.id_queda
JOIN categoria r ON r.id = m.id_viejo
SET c.descripcion = r.descripcion
WHERE (c.descripcion IS NULL OR TRIM(c.descripcion) = '')
  AND r.descripcion IS NOT NULL AND TRIM(r.descripcion) <> '';

-- 4. Los productos de las repetidas pasan a la que se queda
UPDATE producto p
JOIN tmp_cat_mapa m ON m.id_viejo = p.id_categoria
SET p.id_categoria = m.id_queda
WHERE m.id_viejo <> m.id_queda;

-- 5. Se borran las repetidas (ya no tienen productos)
DELETE c FROM categoria c
JOIN tmp_cat_mapa m ON m.id_viejo = c.id
WHERE m.id_viejo <> m.id_queda;

-- 6. Todos los nombres en MAYUSCULAS y sin tildes
UPDATE categoria c
JOIN tmp_cat_mapa m ON m.id_viejo = c.id
SET c.nombre = m.clave;

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_cat_mapa;
DROP TEMPORARY TABLE IF EXISTS tmp_cat_queda;
DROP TEMPORARY TABLE IF EXISTS tmp_cat_clave;

SET SQL_SAFE_UPDATES = 1;

-- Resultado
SELECT id, nombre, descripcion, estado FROM categoria ORDER BY id;

-- =========================================================================
-- PARTE B - PRODUCTOS
-- =========================================================================
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_prod_clave;
CREATE TEMPORARY TABLE tmp_prod_clave AS
SELECT p.id,
       p.estado,
       p.codigo_interno,
       UPPER(TRIM(
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
           REPLACE(REPLACE(REPLACE(p.nombre COLLATE utf8mb4_bin, '   ', ' '), '  ', ' '), '  ', ' '),
               'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'), 'ü', 'u'),
               'à', 'a'), 'è', 'e'), 'ì', 'i'), 'ò', 'o'), 'ù', 'u'),
               'Á', 'A'), 'É', 'E'), 'Í', 'I'), 'Ó', 'O'), 'Ú', 'U'), 'Ü', 'U'),
               'À', 'A'), 'È', 'E'), 'Ì', 'I'), 'Ò', 'O'), 'Ù', 'U')
       )) COLLATE utf8mb4_bin AS clave
FROM producto p;

-- El primero de cada nombre (el activo de id mas bajo) se queda con el nombre limpio
DROP TEMPORARY TABLE IF EXISTS tmp_prod_queda;
CREATE TEMPORARY TABLE tmp_prod_queda AS
SELECT clave,
       COALESCE(MIN(CASE WHEN estado THEN id END), MIN(id)) AS id_queda
FROM tmp_prod_clave
GROUP BY clave;

-- Para revisar: productos que quedan con el codigo en el nombre
SELECT c.id, p.nombre AS nombre_actual,
       LEFT(CONCAT(c.clave, ' (', c.codigo_interno, ')'), 150) AS nombre_nuevo
FROM tmp_prod_clave c
JOIN tmp_prod_queda q ON q.clave = c.clave
JOIN producto p ON p.id = c.id
WHERE c.id <> q.id_queda;

UPDATE producto p
JOIN tmp_prod_clave c ON c.id = p.id
JOIN tmp_prod_queda q ON q.clave = c.clave
SET p.nombre = CASE
                   WHEN c.id = q.id_queda THEN c.clave
                   ELSE LEFT(CONCAT(c.clave, ' (', c.codigo_interno, ')'), 150)
               END;

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_prod_queda;
DROP TEMPORARY TABLE IF EXISTS tmp_prod_clave;

SET SQL_SAFE_UPDATES = 1;

-- Resultado
SELECT id, codigo_interno, nombre, estado FROM producto ORDER BY id;


-- =========================================================================
-- PARTE C - DEMAS CAMPOS DE TEXTO Y PRECIO REFERENCIAL
-- Desde ahora los BO guardan TODOS los textos en MAYUSCULAS:
--   codigos (interno, fabricante, proveedor): mayusculas y sin espacios
--   descripciones de categorias y productos: mayusculas y sin tildes
--   la imagen no se toca (es el nombre de un archivo)
-- Ademas el precio referencial ahora es obligatorio: a los productos que no
-- lo tenian se les pone su precio de venta (el Administrador lo ajusta luego).
-- Se puede ejecutar mas de una vez.
-- =========================================================================
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

UPDATE categoria
SET descripcion = NULLIF(UPPER(TRIM(
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
           REPLACE(REPLACE(REPLACE(descripcion COLLATE utf8mb4_bin, '   ', ' '), '  ', ' '), '  ', ' '),
               'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'), 'ü', 'u'),
               'à', 'a'), 'è', 'e'), 'ì', 'i'), 'ò', 'o'), 'ù', 'u'),
               'Á', 'A'), 'É', 'E'), 'Í', 'I'), 'Ó', 'O'), 'Ú', 'U'), 'Ü', 'U'),
               'À', 'A'), 'È', 'E'), 'Ì', 'I'), 'Ò', 'O'), 'Ù', 'U')
       )), '');

UPDATE producto
SET codigo_interno    = UPPER(REPLACE(REPLACE(REPLACE(TRIM(codigo_interno), ' ', ''), '\t', ''), '\n', '')),
    codigo_fabricante = NULLIF(UPPER(REPLACE(REPLACE(REPLACE(TRIM(codigo_fabricante), ' ', ''), '\t', ''), '\n', '')), ''),
    codigo_proveedor  = NULLIF(UPPER(REPLACE(REPLACE(REPLACE(TRIM(codigo_proveedor), ' ', ''), '\t', ''), '\n', '')), ''),
    descripcion       = NULLIF(UPPER(TRIM(
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
           REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
           REPLACE(REPLACE(REPLACE(descripcion COLLATE utf8mb4_bin, '   ', ' '), '  ', ' '), '  ', ' '),
               'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'), 'ü', 'u'),
               'à', 'a'), 'è', 'e'), 'ì', 'i'), 'ò', 'o'), 'ù', 'u'),
               'Á', 'A'), 'É', 'E'), 'Í', 'I'), 'Ó', 'O'), 'Ú', 'U'), 'Ü', 'U'),
               'À', 'A'), 'È', 'E'), 'Ì', 'I'), 'Ò', 'O'), 'Ù', 'U')
       )), '');

UPDATE producto
SET precio_referencial = precio_venta
WHERE precio_referencial IS NULL OR precio_referencial <= 0;

COMMIT;

SET SQL_SAFE_UPDATES = 1;

-- Para revisar: productos con precio de venta menor que el costo.
-- Desde ahora el BO no deja guardarlos asi; el Administrador debe corregirlos desde la pantalla.
SELECT id, codigo_interno, nombre, precio_venta, costo_unitario
FROM producto
WHERE precio_venta < costo_unitario;

-- Para revisar: codigos que se repiten entre dos productos activos (el lector de barras no sabria cual
-- elegir). Desde ahora el BO no lo permite; el Administrador debe cambiar uno de los dos codigos.
SELECT a.id, a.codigo_interno, a.nombre,
       b.id AS otro_id, b.codigo_interno AS otro_codigo_interno, b.nombre AS otro_nombre
FROM producto a
JOIN producto b ON a.id < b.id AND a.estado = TRUE AND b.estado = TRUE
WHERE a.codigo_interno    IN (b.codigo_interno, b.codigo_fabricante, b.codigo_proveedor)
   OR a.codigo_fabricante IN (b.codigo_interno, b.codigo_fabricante, b.codigo_proveedor)
   OR a.codigo_proveedor  IN (b.codigo_interno, b.codigo_fabricante, b.codigo_proveedor);

-- Resultado
SELECT id, codigo_interno, codigo_fabricante, codigo_proveedor, nombre, descripcion, precio_referencial
FROM producto ORDER BY id;
