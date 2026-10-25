-- 01_maestros.sql | Estudiante 1 | SIGMETA
-- Maestros y seguridad: rol, usuario, cliente, proveedor, solicitud_autorizacion
-- Generado a partir de sql/sigmeta.sql, que es el script oficial: ejecutar 00_base.sql y luego
-- 01 a 05 en orden sobre una base nueva equivale a ejecutar sigmeta.sql. No ejecutar ambos.

USE sigmeta;

-- ============================================================
-- bloque del estudiante 1
-- Tablas: rol, usuario, cliente, proveedor, solicitud_autorizacion
-- Los datos son los mismos que crea PruebaSeguridad.java
-- ============================================================

-- ======================== TABLAS ========================

CREATE TABLE rol (
                     id      INT AUTO_INCREMENT PRIMARY KEY,
                     tipo        ENUM('VENDEDOR','CAJERO','ALMACENERO','ADMINISTRADOR') NOT NULL,
                     descripcion VARCHAR(150),
                     estado      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE usuario (
                         id     INT AUTO_INCREMENT PRIMARY KEY,
                         nombre_usuario VARCHAR(50)  NOT NULL UNIQUE,
                         clave_hash     VARCHAR(255) NOT NULL,
                         salt           VARCHAR(64)  NOT NULL,
                         nombres        VARCHAR(100) NOT NULL,
                         apellidos      VARCHAR(100) NOT NULL,
                         correo         VARCHAR(120),
                         id_rol         INT      NOT NULL,
                         estado         BOOLEAN  NOT NULL DEFAULT TRUE,
                         fecha_registro DATETIME NOT NULL,
                         CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id)
);

CREATE TABLE cliente (
                         id               INT AUTO_INCREMENT PRIMARY KEY,
                         -- tipo_persona define la identidad; el RUC no implica persona juridica.
                         tipo_persona             ENUM('NATURAL','JURIDICA') NOT NULL,
                         razon_social             VARCHAR(150) NULL UNIQUE,
                         nombres                  VARCHAR(100) NULL,
                         apellidos                VARCHAR(100) NULL,
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
                         calificacion_crediticia  VARCHAR(20),
                         CONSTRAINT ck_cliente_calificacion CHECK (calificacion_crediticia IS NULL OR calificacion_crediticia IN ('A','B','C','SIN_EVALUAR')),
                         CONSTRAINT ck_cliente_identidad CHECK (
                             (tipo_persona = 'JURIDICA' AND razon_social IS NOT NULL AND CHAR_LENGTH(TRIM(razon_social)) > 0)
                             OR (tipo_persona = 'NATURAL' AND nombres IS NOT NULL AND apellidos IS NOT NULL AND CHAR_LENGTH(TRIM(nombres)) > 0 AND CHAR_LENGTH(TRIM(apellidos)) > 0))
);

CREATE TABLE proveedor (
                           id       INT AUTO_INCREMENT PRIMARY KEY,
                           razon_social       VARCHAR(150) NOT NULL UNIQUE,
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
                                        id INT AUTO_INCREMENT PRIMARY KEY,
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
                                            FOREIGN KEY (id_solicitante)   REFERENCES usuario(id),
                                        CONSTRAINT fk_solicitud_autorizacion_administrador
                                            FOREIGN KEY (id_administrador) REFERENCES usuario(id)
);

-- ==================== DATOS DE PRUEBA ====================

INSERT INTO rol (id, tipo, descripcion, estado) VALUES
                                                        (1,'VENDEDOR','Atencion al cliente y ciclo comercial',TRUE),
                                                        (2,'CAJERO','Cobros y manejo de efectivo',TRUE),
                                                        (3,'ALMACENERO','Recepcion y despacho de mercaderia',TRUE),
                                                        (4,'ADMINISTRADOR','Supervision y configuracion',TRUE);

-- clave de los cuatro usuarios de prueba: Meta2026 (hash PBKDF2 calculado con GeneradorHash)
INSERT INTO usuario (id, nombre_usuario, clave_hash, salt, nombres, apellidos, correo, id_rol, estado, fecha_registro) VALUES
                                                                                                                                   (1,'lrojas','Sh48oxgKIzNpDZFD8/N/PEeKhBntqwA8+h6iaL5x6WA=','0ZsfOSD7UGxhUcnElYBdjw==','Lucia','Rojas Vega','lrojas@grupometa.pe',1,TRUE,'2026-08-20 08:00:00'),
                                                                                                                                   (2,'mchavez','HDUs4Ia/J9L7K+l+3Xd+oSOT6Dv4pEb1OOqeENM+fQc=','9S2DxpJl7/3Oov6cVTIpXQ==','Marco','Chavez Rios','mchavez@grupometa.pe',2,TRUE,'2026-08-20 08:00:00'),
                                                                                                                                   (3,'jtorres','c9dTxegxi8GD1qVtae2lG6zYGnQdYbLoP3WAfbdPlWM=','5vSceFA3XFoLfkXgZFegrw==','Jorge','Torres Pena','jtorres@grupometa.pe',3,TRUE,'2026-08-20 08:00:00'),
                                                                                                                                   (4,'aquispe','RH+0jKCJjWeeBCgf8BFWq242DF8ZlsCOlHHiMv2wGpQ=','cm6Y45eMpIRuCjLNnqvykQ==','Ana','Quispe Mendoza','aquispe@grupometa.pe',4,TRUE,'2026-08-20 08:00:00');

INSERT INTO cliente (id, tipo_persona, razon_social, nombres, apellidos, direccion, telefono, correo, estado, tipo_documento, numero_documento, contacto_nombre, condicion_pago, plazo_credito_dias, limite_credito, calificacion_crediticia) VALUES
                                                                                                                                                                                                                        (1,'NATURAL',NULL,'Pedro Alberto','Ramos Diaz','Av. Los Angeles 240, Lurigancho-Chosica','987654321','pramos@gmail.com',TRUE,'DNI','45781290','Pedro Ramos','CONTADO',0,0.00,'SIN_EVALUAR'),
                                                                                                                                                                                                                        (2,'JURIDICA','Constructora Andina del Sur S.A.C.',NULL,NULL,'Jr. Huaraz 1180, Cercado de Lima','014785522','compras@andinadelsur.com.pe',TRUE,'RUC','20548963217','Rosa Linares','CREDITO',30,25000.00,'A');

INSERT INTO proveedor (id, razon_social, direccion, telefono, correo, estado, ruc, rubro, contacto_nombre, plazo_entrega_dias, condicion_pago) VALUES
                                                                                                                                                             (1,'Importaciones Electricas Delta S.A.C.','Av. Argentina 3450, Callao','014520011','ventas@delta.com.pe',TRUE,'20431298765','Material electrico industrial','Cesar Bravo',5,'CREDITO'),
                                                                                                                                                             (2,'Ferreteria Industrial Huachipa E.I.R.L.','Carretera Central km 9, Lurigancho-Chosica','013710044','contacto@fihuachipa.pe',TRUE,'20512346789','Fijaciones y abrasivos','Nelly Fuentes',2,'CONTADO');

INSERT INTO solicitud_autorizacion (id, id_solicitante, id_administrador, operacion_restringida, motivo, fecha_solicitud, estado, fecha_resolucion, vigencia_minutos, fecha_vencimiento) VALUES
    (1,1,4,'DESCUENTO_ESPECIAL','Descuento de S/ 50.00 en alicates para Constructora Andina del Sur','2026-09-01 14:40:00','APROBADA','2026-09-01 14:45:00',60,'2026-09-01 15:45:00'),
    (2,2,4,'EMITIR_NOTA_CREDITO','Devolucion de 2 martillos con defecto de fabrica (F001-00000123)','2026-09-04 11:00:00','APROBADA','2026-09-04 11:05:00',30,'2026-09-04 11:35:00');
