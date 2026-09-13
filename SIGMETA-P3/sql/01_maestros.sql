-- ============================================================
-- bloque del estudiante 1
-- Tablas: rol, usuario, cliente, proveedor, solicitud_autorizacion
-- Los datos son los mismos que crea PruebaSeguridad.java
-- ============================================================

-- Mientras se prueba solamente , descomentar..
-- SET FOREIGN_KEY_CHECKS = 0;

-- ======================== TABLAS ========================

CREATE TABLE rol (
                     id_rol      INT AUTO_INCREMENT PRIMARY KEY,
                     tipo        ENUM('VENDEDOR','CAJERO','ALMACENERO','ADMINISTRADOR') NOT NULL,
                     descripcion VARCHAR(150),
                     estado      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE usuario (
                         id_usuario     INT AUTO_INCREMENT PRIMARY KEY,
                         nombre_usuario VARCHAR(50)  NOT NULL UNIQUE,
                         clave_hash     VARCHAR(255) NOT NULL,
                         salt           VARCHAR(64)  NOT NULL,
                         nombres        VARCHAR(100) NOT NULL,
                         apellidos      VARCHAR(100) NOT NULL,
                         correo         VARCHAR(120),
                         id_rol         INT      NOT NULL,
                         estado         BOOLEAN  NOT NULL DEFAULT TRUE,
                         fecha_registro DATETIME NOT NULL,
                         CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);

CREATE TABLE cliente (
                         id_cliente               INT AUTO_INCREMENT PRIMARY KEY,
                         razon_social             VARCHAR(150) NOT NULL,
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
                         calificacion_crediticia  VARCHAR(20)
);

CREATE TABLE proveedor (
                           id_proveedor       INT AUTO_INCREMENT PRIMARY KEY,
                           razon_social       VARCHAR(150) NOT NULL,
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
                                        id_solicitud_autorizacion INT AUTO_INCREMENT PRIMARY KEY,
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
                                            FOREIGN KEY (id_solicitante)   REFERENCES usuario(id_usuario),
                                        CONSTRAINT fk_solicitud_autorizacion_administrador
                                            FOREIGN KEY (id_administrador) REFERENCES usuario(id_usuario)
);

-- ==================== DATOS DE PRUEBA ====================

INSERT INTO rol (id_rol, tipo, descripcion, estado) VALUES
                                                        (1,'VENDEDOR','Atencion al cliente y ciclo comercial',TRUE),
                                                        (2,'CAJERO','Cobros y manejo de efectivo',TRUE),
                                                        (3,'ALMACENERO','Recepcion y despacho de mercaderia',TRUE),
                                                        (4,'ADMINISTRADOR','Supervision y configuracion',TRUE);

INSERT INTO usuario (id_usuario, nombre_usuario, clave_hash, salt, nombres, apellidos, correo, id_rol, estado, fecha_registro) VALUES
                                                                                                                                   (1,'lrojas','hash_lrojas','salt_lrojas','Lucia','Rojas Vega','lrojas@grupometa.pe',1,TRUE,'2026-09-01 08:00:00'),
                                                                                                                                   (2,'mchavez','hash_mchavez','salt_mchavez','Marco','Chavez Rios','mchavez@grupometa.pe',2,TRUE,'2026-09-01 08:00:00'),
                                                                                                                                   (3,'jtorres','hash_jtorres','salt_jtorres','Jorge','Torres Pena','jtorres@grupometa.pe',3,TRUE,'2026-09-01 08:00:00'),
                                                                                                                                   (4,'aquispe','hash_aquispe','salt_aquispe','Ana','Quispe Mendoza','aquispe@grupometa.pe',4,TRUE,'2026-09-01 08:00:00');

INSERT INTO cliente (id_cliente, razon_social, direccion, telefono, correo, estado, tipo_documento, numero_documento, contacto_nombre, condicion_pago, plazo_credito_dias, limite_credito, calificacion_crediticia) VALUES
                                                                                                                                                                                                                        (1,'Pedro Alberto Ramos Diaz','Av. Los Angeles 240, Lurigancho-Chosica','987654321','pramos@gmail.com',TRUE,'DNI','45781290','Pedro Ramos','CONTADO',0,0.00,'SIN LINEA'),
                                                                                                                                                                                                                        (2,'Constructora Andina del Sur S.A.C.','Jr. Huaraz 1180, Cercado de Lima','014785522','compras@andinadelsur.com.pe',TRUE,'RUC','20548963217','Rosa Linares','CREDITO',30,25000.00,'A');

INSERT INTO proveedor (id_proveedor, razon_social, direccion, telefono, correo, estado, ruc, rubro, contacto_nombre, plazo_entrega_dias, condicion_pago) VALUES
                                                                                                                                                             (1,'Importaciones Electricas Delta S.A.C.','Av. Argentina 3450, Callao','014520011','ventas@delta.com.pe',TRUE,'20431298765','Material electrico industrial','Cesar Bravo',5,'CREDITO'),
                                                                                                                                                             (2,'Ferreteria Industrial Huachipa E.I.R.L.','Carretera Central km 9, Lurigancho-Chosica','013710044','contacto@fihuachipa.pe',TRUE,'20512346789','Fijaciones y abrasivos','Nelly Fuentes',2,'CONTADO');

INSERT INTO solicitud_autorizacion (id_solicitud_autorizacion, id_solicitante, id_administrador, operacion_restringida, motivo, fecha_solicitud, estado, fecha_resolucion, vigencia_minutos, fecha_vencimiento) VALUES
    (1,1,4,'VENTA_SOBRE_LIMITE_CREDITO','El cliente supera su linea de credito en la orden del mes','2026-09-01 09:00:00','APROBADA','2026-09-01 09:12:00',60,'2026-09-01 10:12:00');

-- SET FOREIGN_KEY_CHECKS = 1;