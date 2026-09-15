-- bloque del estudiante 5
-- Orden: primero todos los CREATE TABLE, despues todos los INSERT.
-- Mientras pruebas solo este archivo, descomenta la linea de abajo para
-- que MySQL no reclame por las FK que apuntan a tablas de otros bloques.
-- SET FOREIGN_KEY_CHECKS = 0;

-- ============ TABLAS ============
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- ELIMINACION DE TABLAS (DROP IF EXISTS)
-- Orden inverso a las dependencias (FK)
-- ============================================================

DROP TABLE IF EXISTS cierre_caja;
DROP TABLE IF EXISTS movimiento_caja;
DROP TABLE IF EXISTS cobro;
DROP TABLE IF EXISTS cuenta_por_cobrar;
DROP TABLE IF EXISTS caja;

-- ============================================================
-- CREACION DE TABLAS
-- ============================================================

-- Tabla: caja
-- Clase Java: Caja (idCaja, fechaApertura, montoInicial, usuarioApertura, abierta;
--             "movimientos" es la lista inversa, representada por movimiento_caja.id_caja)
-- NOTA (decision de equipo): la relacion Caja <-> CierreCaja es unidireccional.
-- CierreCaja conoce su Caja (ver id_caja abajo), pero Caja NO guarda id_cierre_caja;
-- coincide con la clase Java, que no tiene atributo CierreCaja.
CREATE TABLE caja (
    id_caja               INT AUTO_INCREMENT PRIMARY KEY,
    fecha_apertura        DATETIME NOT NULL,
    monto_inicial         DECIMAL(12,2) NOT NULL,
    id_usuario_apertura   INT NOT NULL,
    abierta               BOOLEAN NOT NULL,
    CONSTRAINT fk_caja_usuario_apertura
        FOREIGN KEY (id_usuario_apertura) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: cuenta_por_cobrar
-- Clase Java: CuentaPorCobrar (idCuentaPorCobrar, venta, cliente, fechaEmision,
--             fechaVencimiento, moneda, montoOriginal, montoPagado, saldoPendiente,
--             estado; "cobros" es la lista inversa -> cobro.id_cuenta_por_cobrar)
-- Se genera a partir de una venta al credito (Estudiante 4).
CREATE TABLE cuenta_por_cobrar (
    id_cuenta_por_cobrar INT AUTO_INCREMENT PRIMARY KEY,
    id_venta             INT NOT NULL,
    id_cliente           INT NOT NULL,
    fecha_emision        DATE NOT NULL,
    fecha_vencimiento    DATE NOT NULL,
    moneda               ENUM('SOLES', 'DOLARES') NOT NULL,
    monto_original       DECIMAL(12,2) NOT NULL,
    monto_pagado         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    saldo_pendiente      DECIMAL(12,2) NOT NULL,
    estado               ENUM('PENDIENTE', 'PARCIAL', 'PAGADA', 'ANULADA') NOT NULL,
    CONSTRAINT fk_cuenta_por_cobrar_venta
        FOREIGN KEY (id_venta) REFERENCES venta(id_venta),
    CONSTRAINT fk_cuenta_por_cobrar_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: cobro
-- Clase Java: Cobro (idCobro, cuentaPorCobrar, fechaCobro, medioPago, monto,
--             referencia, usuarioRegistro, fechaRegistro)
-- OJO: la clase NO tiene una Caja directa. La entrada de dinero a una caja
-- concreta se registra aparte en movimiento_caja, que referencia este cobro
-- de forma generica via documento_origen/id_documento_origen (ver abajo).
CREATE TABLE cobro (
    id_cobro              INT AUTO_INCREMENT PRIMARY KEY,
    id_cuenta_por_cobrar  INT NOT NULL,
    fecha_cobro           DATE NOT NULL,
    medio_pago            ENUM('EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'DEPOSITO') NOT NULL,
    monto                 DECIMAL(12,2) NOT NULL,
    referencia            VARCHAR(50) NULL,
    id_usuario_registro   INT NOT NULL,
    fecha_registro        DATETIME NOT NULL,
    CONSTRAINT fk_cobro_cuenta_por_cobrar
        FOREIGN KEY (id_cuenta_por_cobrar) REFERENCES cuenta_por_cobrar(id_cuenta_por_cobrar),
    CONSTRAINT fk_cobro_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: movimiento_caja
-- Clase Java: MovimientoCaja (idMovimientoCaja, caja, tipo, fechaMovimiento,
--             medioPago, monto, concepto, documentoOrigen, idDocumentoOrigen,
--             usuarioRegistro)
-- documento_origen / id_documento_origen son la referencia generica (String + int)
-- que en la prueba de Java apuntara a 'COBRO' + id_cobro para el ingreso, y
-- quedara en NULL para el egreso manual. RF010: la nota de credito jamas
-- produce un movimiento_caja de tipo INGRESO, por eso no participa del cierre.
CREATE TABLE movimiento_caja (
    id_movimiento_caja  INT AUTO_INCREMENT PRIMARY KEY,
    id_caja             INT NOT NULL,
    tipo                ENUM('INGRESO', 'EGRESO') NOT NULL,
    fecha_movimiento    DATETIME NOT NULL,
    medio_pago          ENUM('EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'DEPOSITO') NOT NULL,
    monto               DECIMAL(12,2) NOT NULL,
    concepto            VARCHAR(150) NOT NULL,
    documento_origen    VARCHAR(50) NULL,
    id_documento_origen INT NULL,
    id_usuario_registro INT NOT NULL,
    CONSTRAINT fk_movimiento_caja_caja
        FOREIGN KEY (id_caja) REFERENCES caja(id_caja),
    CONSTRAINT fk_movimiento_caja_usuario
        FOREIGN KEY (id_usuario_registro) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla: cierre_caja
-- Clase Java: CierreCaja (idCierreCaja, caja, fechaCierre, montoCalculado,
--             montoDeclarado, diferencia, usuarioCierre)
CREATE TABLE cierre_caja (
    id_cierre_caja      INT AUTO_INCREMENT PRIMARY KEY,
    id_caja             INT NOT NULL,
    fecha_cierre        DATETIME NOT NULL,
    monto_calculado     DECIMAL(12,2) NOT NULL,
    monto_declarado     DECIMAL(12,2) NOT NULL,
    diferencia          DECIMAL(12,2) NOT NULL,
    id_usuario_cierre   INT NOT NULL,
    CONSTRAINT fk_cierre_caja_caja
        FOREIGN KEY (id_caja) REFERENCES caja(id_caja),
    CONSTRAINT fk_cierre_caja_usuario
        FOREIGN KEY (id_usuario_cierre) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============ DATOS DE PRUEBA ============
-- ============================================================
-- DATOS DE PRUEBA
-- ============================================================
-- NOTA: id_usuario_apertura / id_usuario_cierre / id_usuario_registro asumen
-- el usuario con id_usuario = 1 (rol administrativo/cajero) creado por el
-- Estudiante 1. Ajustar el id si en su script el cajero quedo con otro id.
-- id_cliente = 2 corresponde al cliente de la venta del Estudiante 4.

-- 1. Apertura de caja con monto inicial (queda cerrada al final, ver paso 6)
INSERT INTO caja (
    id_caja, fecha_apertura, monto_inicial, id_usuario_apertura, abierta
) VALUES (
    1, '2026-09-05 08:00:00', 500.00, 1, FALSE
);

-- 2. Cuenta por cobrar generada desde la venta al credito del Estudiante 4
--    (venta id_venta = 1, total 5900.00, plazo_credito_dias = 30 -> vence 2026-10-02)
--    Tras el cobro parcial del paso 3 queda con monto_pagado = 2000.00,
--    saldo_pendiente = 3900.00 y estado PARCIAL.
INSERT INTO cuenta_por_cobrar (
    id_cuenta_por_cobrar, id_venta, id_cliente, fecha_emision, fecha_vencimiento,
    moneda, monto_original, monto_pagado, saldo_pendiente, estado
) VALUES (
    1, 1, 2, '2026-09-02', '2026-10-02',
    'SOLES', 5900.00, 2000.00, 3900.00, 'PARCIAL'
);

-- 3. Cobro parcial de la cuenta por cobrar (sin Caja directa en el modelo Java;
--    el vinculo con la caja se hace en el movimiento_caja del paso 4)
INSERT INTO cobro (
    id_cobro, id_cuenta_por_cobrar, fecha_cobro, medio_pago,
    monto, referencia, id_usuario_registro, fecha_registro
) VALUES (
    1, 1, '2026-09-05', 'TRANSFERENCIA',
    2000.00, 'OP-458821', 1, '2026-09-05 09:30:00'
);

-- 4. Movimiento de caja: ingreso generado por el cobro anterior
--    (documento_origen/id_documento_origen apuntan de forma generica al cobro 1)
INSERT INTO movimiento_caja (
    id_movimiento_caja, id_caja, tipo, fecha_movimiento, medio_pago,
    monto, concepto, documento_origen, id_documento_origen, id_usuario_registro
) VALUES (
    1, 1, 'INGRESO', '2026-09-05 09:30:00', 'TRANSFERENCIA',
    2000.00, 'Cobro parcial CxC venta VTA-2026-0001', 'COBRO', 1, 1
);

-- 5. Movimiento de caja: egreso manual (sin documento de origen)
INSERT INTO movimiento_caja (
    id_movimiento_caja, id_caja, tipo, fecha_movimiento, medio_pago,
    monto, concepto, documento_origen, id_documento_origen, id_usuario_registro
) VALUES (
    2, 1, 'EGRESO', '2026-09-05 15:00:00', 'EFECTIVO',
    150.00, 'Compra de utiles de oficina', NULL, NULL, 1
);

-- 6. Cierre de caja del dia
--    monto_calculado = monto_inicial (500.00) + ingresos (2000.00) - egresos (150.00) = 2350.00
--    monto_declarado difiere en 5.00 (faltante) para ejercitar la comparacion en la prueba de Java.
--    RF010: el ingreso del paso 4 viene de un COBRO, nunca de una NOTA_CREDITO,
--    por eso el monto_calculado no se ve afectado por la nota de credito del Estudiante 4.
INSERT INTO cierre_caja (
    id_cierre_caja, id_caja, fecha_cierre, monto_calculado,
    monto_declarado, diferencia, id_usuario_cierre
) VALUES (
    1, 1, '2026-09-05 20:00:00', 2350.00,
    2345.00, -5.00, 1
);

SET FOREIGN_KEY_CHECKS = 1;

SELECT * FROM caja;
SELECT * FROM cuenta_por_cobrar;
SELECT * FROM cobro;
SELECT * FROM movimiento_caja;
SELECT * FROM cierre_caja;

-- SET FOREIGN_KEY_CHECKS = 1;
