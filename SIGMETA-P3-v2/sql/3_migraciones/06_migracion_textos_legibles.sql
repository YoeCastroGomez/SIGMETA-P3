USE sigmeta;
SET NAMES utf8mb4;

-- =========================================================================
-- MIGRACION 06 - TEXTOS LEGIBLES (observacion del JP: guardado de textos inconsistente)
-- Hasta la migracion 05 los nombres y descripciones de categorias y productos se
-- guardaban en MAYUSCULAS y sin tildes ("LATEX Y ESMALTES"), mientras que clientes,
-- proveedores y usuarios se guardaban tal cual. Criterio unico desde ahora:
--   * se GUARDA como lo escribe el usuario (sin espacios de mas): "Látex y esmaltes";
--   * se COMPARA con una clave (mayusculas, sin tildes, la Ñ se respeta) solo para
--     buscar y para no aceptar repetidos (TextoNormalizado.clave en los BO).
--   * los CODIGOS (interno, de fabricante, de proveedor) siguen en MAYUSCULAS y sin espacios.
-- Este script deja los datos ya cargados en mayuscula normal y con tildes.
--
-- Como funciona:
--   Cada UPDATE va por id y ADEMAS exige que la fila siga siendo la esperada (su nombre
--   anterior, su RUC, su documento o su nombre de usuario). La comparacion usa
--   utf8mb4_unicode_ci, que no distingue mayusculas ni tildes. Por eso:
--     - si en esa base el id es de otra fila, no la toca;
--     - se puede ejecutar mas de una vez: la segunda vez deja lo mismo.
--   PARTE A - categorias y productos de los datos de demostracion (4_datos_demo).
--   PARTE B - categorias y productos de los datos base (1_base/sigmeta.sql).
--   PARTE C - proveedores, clientes y usuarios de los datos de demostracion (tildes).
--
-- Los productos y categorias creados a mano desde la pantalla NO estan en este script:
-- el Administrador los corrige desde Productos / Categorias (la consulta del final los lista).
-- No cambia la estructura de ninguna tabla. Ejecutar en Workbench (la base de AWS es
-- compartida: avisar al grupo y sacar un respaldo antes).
-- =========================================================================

SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- -------------------------------------------------------------------------
-- PARTE A - Datos de demostracion: categorias (nombre y descripcion) y productos
-- -------------------------------------------------------------------------
UPDATE categoria SET nombre = 'Herramientas manuales', descripcion = 'Martillos, alicates, llaves y herramientas de mano' WHERE id = 1 AND nombre COLLATE utf8mb4_unicode_ci = 'HERRAMIENTAS MANUALES';
UPDATE categoria SET nombre = 'Herramientas eléctricas', descripcion = 'Taladros, amoladoras, sierras y sus accesorios' WHERE id = 2 AND nombre COLLATE utf8mb4_unicode_ci = 'HERRAMIENTAS ELECTRICAS';
UPDATE categoria SET nombre = 'Materiales eléctricos', descripcion = 'Cables, interruptores, tomacorrientes e iluminación' WHERE id = 3 AND nombre COLLATE utf8mb4_unicode_ci = 'MATERIALES ELECTRICOS';
UPDATE categoria SET nombre = 'Construcción', descripcion = 'Cemento, fierro, ladrillos y agregados' WHERE id = 4 AND nombre COLLATE utf8mb4_unicode_ci = 'CONSTRUCCION';
UPDATE categoria SET nombre = 'Fijaciones y tornillería', descripcion = 'Pernos, tornillos, tarugos y clavos' WHERE id = 5 AND nombre COLLATE utf8mb4_unicode_ci = 'FIJACIONES Y TORNILLERIA';
UPDATE categoria SET nombre = 'Pinturas y acabados', descripcion = 'Pinturas, esmaltes, diluyentes y accesorios' WHERE id = 6 AND nombre COLLATE utf8mb4_unicode_ci = 'PINTURAS Y ACABADOS';
UPDATE categoria SET nombre = 'Gasfitería', descripcion = 'Tuberías, conexiones y accesorios de PVC' WHERE id = 7 AND nombre COLLATE utf8mb4_unicode_ci = 'GASFITERIA';
UPDATE categoria SET nombre = 'Seguridad industrial', descripcion = 'Equipos de protección personal' WHERE id = 8 AND nombre COLLATE utf8mb4_unicode_ci = 'SEGURIDAD INDUSTRIAL';
UPDATE categoria SET nombre = 'Jardinería', descripcion = 'Mangueras y herramientas de jardín (línea descontinuada)' WHERE id = 9 AND nombre COLLATE utf8mb4_unicode_ci = 'JARDINERIA';

UPDATE producto SET nombre = 'Martillo de uña 16 oz' WHERE id = 1 AND nombre COLLATE utf8mb4_unicode_ci = 'MARTILLO DE UNA 16 OZ';
UPDATE producto SET nombre = 'Alicate universal 8 pulg' WHERE id = 2 AND nombre COLLATE utf8mb4_unicode_ci = 'ALICATE UNIVERSAL 8 PULG';
UPDATE producto SET nombre = 'Destornillador plano 6 pulg' WHERE id = 3 AND nombre COLLATE utf8mb4_unicode_ci = 'DESTORNILLADOR PLANO 6 PULG';
UPDATE producto SET nombre = 'Juego de destornilladores 6 piezas' WHERE id = 4 AND nombre COLLATE utf8mb4_unicode_ci = 'JUEGO DE DESTORNILLADORES 6 PIEZAS';
UPDATE producto SET nombre = 'Llave francesa 10 pulg' WHERE id = 5 AND nombre COLLATE utf8mb4_unicode_ci = 'LLAVE FRANCESA 10 PULG';
UPDATE producto SET nombre = 'Wincha metálica 5 m' WHERE id = 6 AND nombre COLLATE utf8mb4_unicode_ci = 'WINCHA METALICA 5 M';
UPDATE producto SET nombre = 'Serrucho de carpintero 20 pulg' WHERE id = 7 AND nombre COLLATE utf8mb4_unicode_ci = 'SERRUCHO DE CARPINTERO 20 PULG';
UPDATE producto SET nombre = 'Nivel de aluminio 24 pulg' WHERE id = 8 AND nombre COLLATE utf8mb4_unicode_ci = 'NIVEL DE ALUMINIO 24 PULG';
UPDATE producto SET nombre = 'Juego de llaves mixtas 12 piezas' WHERE id = 9 AND nombre COLLATE utf8mb4_unicode_ci = 'JUEGO DE LLAVES MIXTAS 12 PIEZAS';
UPDATE producto SET nombre = 'Taladro percutor 650W' WHERE id = 10 AND nombre COLLATE utf8mb4_unicode_ci = 'TALADRO PERCUTOR 650W';
UPDATE producto SET nombre = 'Amoladora angular 4 1/2 pulg 850W' WHERE id = 11 AND nombre COLLATE utf8mb4_unicode_ci = 'AMOLADORA ANGULAR 4 1/2 PULG 850W';
UPDATE producto SET nombre = 'Rotomartillo SDS Plus 800W' WHERE id = 12 AND nombre COLLATE utf8mb4_unicode_ci = 'ROTOMARTILLO SDS PLUS 800W';
UPDATE producto SET nombre = 'Sierra circular 7 1/4 pulg 1400W' WHERE id = 13 AND nombre COLLATE utf8mb4_unicode_ci = 'SIERRA CIRCULAR 7 1/4 PULG 1400W';
UPDATE producto SET nombre = 'Disco de corte metal 4 1/2 pulg' WHERE id = 14 AND nombre COLLATE utf8mb4_unicode_ci = 'DISCO DE CORTE METAL 4 1/2 PULG';
UPDATE producto SET nombre = 'Broca para concreto 3/8 pulg' WHERE id = 15 AND nombre COLLATE utf8mb4_unicode_ci = 'BROCA PARA CONCRETO 3/8 PULG';
UPDATE producto SET nombre = 'Cable THW 14 AWG' WHERE id = 16 AND nombre COLLATE utf8mb4_unicode_ci = 'CABLE THW 14 AWG';
UPDATE producto SET nombre = 'Cable mellizo 2x16 AWG' WHERE id = 17 AND nombre COLLATE utf8mb4_unicode_ci = 'CABLE MELLIZO 2X16 AWG';
UPDATE producto SET nombre = 'Interruptor simple' WHERE id = 18 AND nombre COLLATE utf8mb4_unicode_ci = 'INTERRUPTOR SIMPLE';
UPDATE producto SET nombre = 'Tomacorriente doble con línea a tierra' WHERE id = 19 AND nombre COLLATE utf8mb4_unicode_ci = 'TOMACORRIENTE DOBLE CON LINEA A TIERRA';
UPDATE producto SET nombre = 'Foco LED 12W luz blanca' WHERE id = 20 AND nombre COLLATE utf8mb4_unicode_ci = 'FOCO LED 12W LUZ BLANCA';
UPDATE producto SET nombre = 'Cinta aislante negra 20 yd' WHERE id = 21 AND nombre COLLATE utf8mb4_unicode_ci = 'CINTA AISLANTE NEGRA 20 YD';
UPDATE producto SET nombre = 'Llave termomagnética 2x20A' WHERE id = 22 AND nombre COLLATE utf8mb4_unicode_ci = 'LLAVE TERMOMAGNETICA 2X20A';
UPDATE producto SET nombre = 'Cemento Portland tipo I 42.5 kg' WHERE id = 23 AND nombre COLLATE utf8mb4_unicode_ci = 'CEMENTO PORTLAND TIPO I 42.5 KG';
UPDATE producto SET nombre = 'Fierro corrugado 1/2 pulg x 9 m' WHERE id = 24 AND nombre COLLATE utf8mb4_unicode_ci = 'FIERRO CORRUGADO 1/2 PULG X 9 M';
UPDATE producto SET nombre = 'Fierro corrugado 3/8 pulg x 9 m' WHERE id = 25 AND nombre COLLATE utf8mb4_unicode_ci = 'FIERRO CORRUGADO 3/8 PULG X 9 M';
UPDATE producto SET nombre = 'Alambre negro N 16' WHERE id = 26 AND nombre COLLATE utf8mb4_unicode_ci = 'ALAMBRE NEGRO N 16';
UPDATE producto SET nombre = 'Ladrillo King Kong 18 huecos' WHERE id = 27 AND nombre COLLATE utf8mb4_unicode_ci = 'LADRILLO KING KONG 18 HUECOS';
UPDATE producto SET nombre = 'Yeso en bolsa 25 kg' WHERE id = 28 AND nombre COLLATE utf8mb4_unicode_ci = 'YESO EN BOLSA 25 KG';
UPDATE producto SET nombre = 'Clavo para madera 3 pulg' WHERE id = 29 AND nombre COLLATE utf8mb4_unicode_ci = 'CLAVO PARA MADERA 3 PULG';
UPDATE producto SET nombre = 'Perno hexagonal 3/8 x 2 pulg' WHERE id = 30 AND nombre COLLATE utf8mb4_unicode_ci = 'PERNO HEXAGONAL 3/8 X 2 PULG';
UPDATE producto SET nombre = 'Tarugo plástico 1/4 pulg' WHERE id = 31 AND nombre COLLATE utf8mb4_unicode_ci = 'TARUGO PLASTICO 1/4 PULG';
UPDATE producto SET nombre = 'Tornillo drywall 6 x 1 pulg' WHERE id = 32 AND nombre COLLATE utf8mb4_unicode_ci = 'TORNILLO DRYWALL 6 X 1 PULG';
UPDATE producto SET nombre = 'Pintura látex blanco' WHERE id = 33 AND nombre COLLATE utf8mb4_unicode_ci = 'PINTURA LATEX BLANCO';
UPDATE producto SET nombre = 'Esmalte sintético negro' WHERE id = 34 AND nombre COLLATE utf8mb4_unicode_ci = 'ESMALTE SINTETICO NEGRO';
UPDATE producto SET nombre = 'Thinner acrílico' WHERE id = 35 AND nombre COLLATE utf8mb4_unicode_ci = 'THINNER ACRILICO';
UPDATE producto SET nombre = 'Brocha de nylon 3 pulg' WHERE id = 36 AND nombre COLLATE utf8mb4_unicode_ci = 'BROCHA DE NYLON 3 PULG';
UPDATE producto SET nombre = 'Rodillo de felpa 9 pulg' WHERE id = 37 AND nombre COLLATE utf8mb4_unicode_ci = 'RODILLO DE FELPA 9 PULG';
UPDATE producto SET nombre = 'Tubo PVC 1/2 pulg x 5 m' WHERE id = 38 AND nombre COLLATE utf8mb4_unicode_ci = 'TUBO PVC 1/2 PULG X 5 M';
UPDATE producto SET nombre = 'Codo PVC 1/2 pulg x 90' WHERE id = 39 AND nombre COLLATE utf8mb4_unicode_ci = 'CODO PVC 1/2 PULG X 90';
UPDATE producto SET nombre = 'Cinta teflón 1/2 pulg' WHERE id = 40 AND nombre COLLATE utf8mb4_unicode_ci = 'CINTA TEFLON 1/2 PULG';
UPDATE producto SET nombre = 'Pegamento PVC 1/4 galón' WHERE id = 41 AND nombre COLLATE utf8mb4_unicode_ci = 'PEGAMENTO PVC 1/4 GALON';
UPDATE producto SET nombre = 'Llave de paso 1/2 pulg' WHERE id = 42 AND nombre COLLATE utf8mb4_unicode_ci = 'LLAVE DE PASO 1/2 PULG';
UPDATE producto SET nombre = 'Casco de seguridad con ratchet' WHERE id = 43 AND nombre COLLATE utf8mb4_unicode_ci = 'CASCO DE SEGURIDAD CON RATCHET';
UPDATE producto SET nombre = 'Guantes de cuero reforzado' WHERE id = 44 AND nombre COLLATE utf8mb4_unicode_ci = 'GUANTES DE CUERO REFORZADO';
UPDATE producto SET nombre = 'Lentes de seguridad claros' WHERE id = 45 AND nombre COLLATE utf8mb4_unicode_ci = 'LENTES DE SEGURIDAD CLAROS';
UPDATE producto SET nombre = 'Botas de seguridad punta de acero' WHERE id = 46 AND nombre COLLATE utf8mb4_unicode_ci = 'BOTAS DE SEGURIDAD PUNTA DE ACERO';
UPDATE producto SET nombre = 'Martillo de bola 12 oz' WHERE id = 47 AND nombre COLLATE utf8mb4_unicode_ci = 'MARTILLO DE BOLA 12 OZ';
UPDATE producto SET nombre = 'Manguera de jardín 15 m' WHERE id = 48 AND nombre COLLATE utf8mb4_unicode_ci = 'MANGUERA DE JARDIN 15 M';

-- -------------------------------------------------------------------------
-- PARTE B - Datos base (1_base/sigmeta.sql): 3 categorias y 6 productos
-- -------------------------------------------------------------------------
UPDATE categoria SET nombre = 'Herramientas manuales' WHERE id = 1 AND nombre COLLATE utf8mb4_unicode_ci = 'HERRAMIENTAS MANUALES';
UPDATE categoria SET nombre = 'Materiales eléctricos' WHERE id = 2 AND nombre COLLATE utf8mb4_unicode_ci = 'MATERIALES ELECTRICOS';
UPDATE categoria SET nombre = 'Construcción' WHERE id = 3 AND nombre COLLATE utf8mb4_unicode_ci = 'CONSTRUCCION';
UPDATE producto SET nombre = 'Martillo de acero' WHERE id = 1 AND nombre COLLATE utf8mb4_unicode_ci = 'MARTILLO DE ACERO';
UPDATE producto SET nombre = 'Alicate de presión' WHERE id = 2 AND nombre COLLATE utf8mb4_unicode_ci = 'ALICATE DE PRESION';
UPDATE producto SET nombre = 'Cable mellizo 2x14' WHERE id = 3 AND nombre COLLATE utf8mb4_unicode_ci = 'CABLE MELLIZO 2X14';
UPDATE producto SET nombre = 'Enchufe Mennekes' WHERE id = 4 AND nombre COLLATE utf8mb4_unicode_ci = 'ENCHUFE MENNEKES';
UPDATE producto SET nombre = 'Bolsa cemento Sol' WHERE id = 5 AND nombre COLLATE utf8mb4_unicode_ci = 'BOLSA CEMENTO SOL';
UPDATE producto SET nombre = 'Fierro corrugado' WHERE id = 6 AND nombre COLLATE utf8mb4_unicode_ci = 'FIERRO CORRUGADO';

-- -------------------------------------------------------------------------
-- PARTE C - Datos de demostracion: tildes de proveedores, clientes y usuarios
-- -------------------------------------------------------------------------
UPDATE proveedor SET razon_social = 'Importaciones Eléctricas Delta S.A.C.', rubro = 'Material eléctrico industrial', contacto_nombre = 'César Bravo' WHERE id = 1 AND ruc = '20431298765';
UPDATE proveedor SET razon_social = 'Ferretería Industrial Huachipa E.I.R.L.' WHERE id = 2 AND ruc = '20512346789';
UPDATE proveedor SET direccion = 'Av. Nicolás Ayllón 5520, Ate' WHERE id = 3 AND ruc = '20601234571';
UPDATE proveedor SET razon_social = 'Aceros y Fierros del Perú S.A.', rubro = 'Fierro de construcción' WHERE id = 4 AND ruc = '20100456782';
UPDATE proveedor SET direccion = 'Av. Venezuela 1340, Breña', rubro = 'Herramientas manuales y eléctricas' WHERE id = 5 AND ruc = '20555123456';
UPDATE proveedor SET razon_social = 'Plásticos y Tuberías del Centro S.R.L.', rubro = 'Tubería y conexiones PVC' WHERE id = 7 AND ruc = '20487612345';
UPDATE proveedor SET razon_social = 'Seguridad Total Perú E.I.R.L.', direccion = 'Av. Tomás Marsano 2120, Surquillo', rubro = 'Equipos de protección personal', contacto_nombre = 'Vanessa León' WHERE id = 8 AND ruc = '20456789012';

UPDATE cliente SET razon_social = 'Metalmecánica Chosica E.I.R.L.' WHERE id = 2 AND numero_documento = '20601873345';
UPDATE cliente SET razon_social = 'Instalaciones Eléctricas del Sur S.A.C.', direccion = 'Av. Los Héroes 455, San Juan de Miraflores' WHERE id = 3 AND numero_documento = '20523987411';
UPDATE cliente SET contacto_nombre = 'Andrés Valdivia' WHERE id = 4 AND numero_documento = '20605544781';
UPDATE cliente SET direccion = 'Av. República de Panamá 3591, San Isidro', contacto_nombre = 'Patricia Núñez' WHERE id = 6 AND numero_documento = '20556677889';
UPDATE cliente SET contacto_nombre = 'Víctor Quispe' WHERE id = 7 AND numero_documento = '20511223349';
UPDATE cliente SET razon_social = 'Ferretería El Constructor E.I.R.L.', direccion = 'Av. Huarochirí 455, Santa Anita', contacto_nombre = 'Óscar Mendoza' WHERE id = 8 AND numero_documento = '20600112233';
UPDATE cliente SET razon_social = 'Colegio Particular Santa María S.A.C.' WHERE id = 9 AND numero_documento = '20522334456';
UPDATE cliente SET direccion = 'Av. Túpac Amaru 3420, Comas', contacto_nombre = 'Tomás Herrera' WHERE id = 10 AND numero_documento = '20544332211';
UPDATE cliente SET apellidos = 'Ramos Díaz', direccion = 'Av. Los Ángeles 240, Lurigancho-Chosica' WHERE id = 11 AND numero_documento = '45781290';
UPDATE cliente SET apellidos = 'Morales Pérez' WHERE id = 12 AND numero_documento = '70214589';
UPDATE cliente SET apellidos = 'Huamaní Flores' WHERE id = 13 AND numero_documento = '41236587';
UPDATE cliente SET apellidos = 'Gutiérrez Vargas' WHERE id = 15 AND numero_documento = '40987123';
UPDATE cliente SET nombres = 'María del Pilar' WHERE id = 16 AND numero_documento = '72145698';
UPDATE cliente SET nombres = 'José Carlos', contacto_nombre = 'José Mendoza' WHERE id = 17 AND numero_documento = '10412365879';
UPDATE cliente SET direccion = 'Av. Nicolás Ayllón 3320, Ate' WHERE id = 19 AND numero_documento = '46871235';
UPDATE cliente SET apellidos = 'Ríos Salazar' WHERE id = 20 AND numero_documento = '001234567';
UPDATE cliente SET apellidos = 'Chumpitaz López', direccion = 'Av. Prolongación Javier Prado 1450, Ate' WHERE id = 22 AND numero_documento = '73654128';

UPDATE usuario SET nombres = 'Lucía' WHERE id = 1 AND nombre_usuario = 'lrojas';
UPDATE usuario SET apellidos = 'Chávez Ríos' WHERE id = 2 AND nombre_usuario = 'mchavez';
UPDATE usuario SET apellidos = 'Torres Peña' WHERE id = 3 AND nombre_usuario = 'jtorres';
UPDATE usuario SET apellidos = 'Flores Huamán' WHERE id = 5 AND nombre_usuario = 'cflores';
UPDATE usuario SET nombres = 'Raúl', apellidos = 'Sánchez Cárdenas' WHERE id = 6 AND nombre_usuario = 'rsanchez';

COMMIT;

SET SQL_SAFE_UPDATES = 1;

-- Para revisar: categorias y productos que siguen escritos todo en MAYUSCULAS (creados a mano).
-- El Administrador los corrige desde la pantalla; el script no adivina sus tildes.
SELECT 'categoria' AS tabla, id, nombre FROM categoria WHERE nombre COLLATE utf8mb4_bin = UPPER(nombre) COLLATE utf8mb4_bin
UNION ALL
SELECT 'producto', id, nombre FROM producto WHERE nombre COLLATE utf8mb4_bin = UPPER(nombre) COLLATE utf8mb4_bin
ORDER BY tabla, id;
