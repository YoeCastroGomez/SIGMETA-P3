USE sigmeta;

-- =========================================================================
-- MIGRACION LAB 11 - CLAUDIA (RF006 CATEGORIAS)
-- Nombres de categoria escritos siempre igual: MAYUSCULAS, sin tildes y con
-- un solo espacio entre palabras ("Látex y esmaltes" -> "LATEX Y ESMALTES").
-- Desde ahora el BO (CategoriaBOImpl.normalizarNombre) guarda asi los nombres;
-- este script arregla las categorias que ya estaban en la base.
--
-- Que hace:
--   1. Junta las categorias repetidas (las que solo cambian en mayusculas,
--      tildes o espacios, por ejemplo "Construcción", "CONSTRUCCION" y
--      "Construccion"). Se queda una: la activa con el id mas bajo (si
--      ninguna esta activa, la de id mas bajo). Los productos de las otras
--      pasan a la que se queda y las repetidas se borran.
--   2. Escribe todos los nombres en MAYUSCULAS y sin tildes.
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
