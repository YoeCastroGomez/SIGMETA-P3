USE sigmeta;

-- =========================================================================
-- SCRIPT DE PRUEBAS PARA PROCEDIMIENTOS ALMACENADOS - MÓDULO CAJA Y COBRANZA
-- Tablas: caja, movimiento_caja, cierre_caja, cuenta_por_cobrar, cobro
-- Requiere los datos de sigmeta.sql: cajero id 2 (mchavez), administrador id 4,
-- cliente id 2 y venta id 1. Los cierres de caja, el cobro anulado y su cuenta por cobrar se conservan
-- como historial (RNF002, RF010): nada de eso se borra, se revierte con un cambio de estado.
-- =========================================================================

-- ---------------------------------------------------------
-- 1. PRUEBA: CAJA
-- ---------------------------------------------------------
-- El cajero (id 2) abre una caja con S/ 300.00
CALL sp_caja_insertar(@id_caja, 300.00, TRUE, 2);
SELECT @id_caja AS 'ID Caja';

-- Corregir el monto inicial
CALL sp_caja_modificar(@id_caja, 350.00, TRUE, 2);

-- Obtener por ID, la caja abierta del cajero y listar todas
CALL sp_caja_obtener(@id_caja);
CALL sp_caja_obtener_abierta_por_usuario(2);
CALL sp_caja_listar_todas_con_usuario();


-- ---------------------------------------------------------
-- 2. PRUEBA: MOVIMIENTO DE CAJA
-- ---------------------------------------------------------
-- Un ingreso manual en efectivo y un egreso manual (compra de utiles)
CALL sp_movimiento_caja_insertar(@id_mov1, @id_caja, 'INGRESO_MANUAL', 'EFECTIVO', 120.00, 'Sencillo adicional para vuelto', NULL, NULL, 2);
CALL sp_movimiento_caja_insertar(@id_mov2, @id_caja, 'EGRESO_MANUAL', 'EFECTIVO', 45.50, 'Compra de utiles de oficina', NULL, NULL, 2);
SELECT @id_mov1 AS 'ID Movimiento 1', @id_mov2 AS 'ID Movimiento 2';

-- Corregir el monto del egreso
CALL sp_movimiento_caja_modificar(@id_mov2, 'EGRESO_MANUAL', 'EFECTIVO', 40.00, 'Compra de utiles de oficina', NULL, NULL);

-- Consultas: por ID, por caja, por rango de fechas, totales por medio de pago y listado general
CALL sp_movimiento_caja_obtener(@id_mov2);
CALL sp_movimiento_caja_listar_por_caja(@id_caja);
CALL sp_movimiento_caja_buscar_por_fechas(@id_caja, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY));
CALL sp_movimiento_caja_totales_medio_pago(@id_caja);     -- EFECTIVO = 120.00 - 40.00 = 80.00
CALL sp_movimiento_caja_listar_con_detalle();


-- ---------------------------------------------------------
-- 3. PRUEBA: CUENTA POR COBRAR
-- ---------------------------------------------------------
-- Cuenta de prueba sobre la venta 1 del cliente 2 (en el sistema la genera la venta al credito)
CALL sp_cuenta_por_cobrar_insertar(@id_cxc, 1, 2, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 500.00, 0.00, 500.00, 'PENDIENTE');
SELECT @id_cxc AS 'ID Cuenta por Cobrar';

-- Refinanciar: ampliar el vencimiento a 45 dias
CALL sp_cuenta_por_cobrar_modificar(@id_cxc, DATE_ADD(CURDATE(), INTERVAL 45 DAY), 500.00, 0.00, 500.00, 'PENDIENTE');

-- Consultas: por ID, por venta, por cliente, por estado, vencidas y listado general
CALL sp_cuenta_por_cobrar_obtener(@id_cxc);
CALL sp_cuenta_por_cobrar_obtener_por_venta(1);
CALL sp_cuenta_por_cobrar_listar_por_cliente(2);
CALL sp_cuenta_por_cobrar_listar_por_estado('PENDIENTE');
CALL sp_cuenta_por_cobrar_listar_vencidas();
CALL sp_cuenta_por_cobrar_listar_todas_con_detalle();


-- ---------------------------------------------------------
-- 4. PRUEBA: COBRO
-- ---------------------------------------------------------
-- El cajero cobra S/ 200.00 por transferencia; en el sistema el BO actualiza el saldo
-- y registra el ingreso en caja en la misma transaccion (aqui se simula con los SP)
CALL sp_cobro_insertar(@id_cobro, @id_cxc, CURDATE(), 'TRANSFERENCIA', 200.00, 'OP-778812', 2);
CALL sp_cuenta_por_cobrar_modificar(@id_cxc, DATE_ADD(CURDATE(), INTERVAL 45 DAY), 500.00, 200.00, 300.00, 'PAGADA_PARCIAL');
CALL sp_movimiento_caja_insertar(@id_mov3, @id_caja, 'COBRO_CREDITO', 'TRANSFERENCIA', 200.00, 'Cobro de la cuenta por cobrar', 'COBRO', @id_cobro, 2);
SELECT @id_cobro AS 'ID Cobro', @id_mov3 AS 'ID Movimiento del cobro';

-- Corregir la referencia de la operacion
CALL sp_cobro_modificar(@id_cobro, CURDATE(), 'TRANSFERENCIA', 200.00, 'OP-778813');

-- Consultas: por ID, por cuenta y por cliente
CALL sp_cobro_obtener(@id_cobro);
CALL sp_cobro_listar_por_cuenta_por_cobrar(@id_cxc);
CALL sp_cobro_listar_por_cliente(2);
CALL sp_cobro_listar();


-- ---------------------------------------------------------
-- 5. PRUEBA: CIERRE DE CAJA
-- ---------------------------------------------------------
-- Monto esperado = 350.00 inicial + 120.00 ingreso - 40.00 egreso + 200.00 cobro = 630.00
CALL sp_cierre_caja_calcular_monto_esperado(@id_caja, @monto_esperado);
SELECT @monto_esperado AS 'Monto calculado por el sistema';

-- El cajero declara S/ 625.00: diferencia de -5.00
CALL sp_cierre_caja_insertar(@id_cierre, @id_caja, @monto_esperado, 625.00, 625.00 - @monto_esperado, 2);
CALL sp_caja_modificar(@id_caja, 350.00, FALSE, 2);
SELECT @id_cierre AS 'ID Cierre';

-- El Administrador corrige el monto declarado (encontro S/ 5.00 en otra gaveta)
CALL sp_cierre_caja_modificar(@id_cierre, @monto_esperado, 630.00, 0.00);

-- Consultas: por ID, por caja, por rango de fechas y listado general
CALL sp_cierre_caja_obtener(@id_cierre);
CALL sp_cierre_caja_obtener_por_caja(@id_caja);
CALL sp_cierre_caja_buscar_por_fechas(DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY));
CALL sp_cierre_caja_listar_con_detalle();


-- ---------------------------------------------------------
-- 6. REAPERTURA, ANULACIONES Y ELIMINACIÓN
-- ---------------------------------------------------------
-- Reabrir la caja (solo el Administrador, id 4): el cierre NO se borra, queda REVERTIDO
CALL sp_cierre_caja_reabrir(@id_cierre, 4, 'Se encontro un vale sin registrar');
CALL sp_cierre_caja_obtener(@id_cierre);                  -- estado = REVERTIDO, con fecha y usuario de reapertura
CALL sp_caja_obtener(@id_caja);                           -- abierta = 1

-- Anular el cobro (baja logica): egreso compensatorio en caja, cobro marcado como anulado
-- y el saldo vuelve a la cuenta. En el sistema lo hace CobroBOImpl.eliminar en una transaccion.
CALL sp_movimiento_caja_insertar(@id_mov4, @id_caja, 'EGRESO_MANUAL', 'TRANSFERENCIA', 200.00, 'Anulacion del cobro', 'ANULACION_COBRO', @id_cobro, 2);
CALL sp_cobro_eliminar(@id_cobro);
CALL sp_cobro_obtener(@id_cobro);                         -- anulado = 1, el cobro se conserva
CALL sp_cuenta_por_cobrar_modificar(@id_cxc, DATE_ADD(CURDATE(), INTERVAL 45 DAY), 500.00, 0.00, 500.00, 'PENDIENTE');
CALL sp_movimiento_caja_totales_medio_pago(@id_caja);     -- TRANSFERENCIA = 200.00 - 200.00 = 0.00

-- Una cuenta sin cobros (generada por error) si puede eliminarse
CALL sp_cuenta_por_cobrar_insertar(@id_cxc2, 1, 2, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 100.00, 0.00, 100.00, 'PENDIENTE');
CALL sp_cuenta_por_cobrar_eliminar(@id_cxc2);

-- La caja reabierta vuelve a cerrarse: se crea un cierre nuevo y el revertido se conserva
CALL sp_cierre_caja_calcular_monto_esperado(@id_caja, @monto_esperado2);
CALL sp_cierre_caja_insertar(@id_cierre2, @id_caja, @monto_esperado2, @monto_esperado2, 0.00, 2);
CALL sp_caja_modificar(@id_caja, 350.00, FALSE, 2);
CALL sp_cierre_caja_listar_por_caja(@id_caja);            -- 2 cierres: uno REVERTIDO y uno VIGENTE
CALL sp_cierre_caja_obtener_por_caja(@id_caja);           -- devuelve el cierre VIGENTE (@id_cierre2)


-- ---------------------------------------------------------
-- 7. COMPROBACIÓN FINAL
-- ---------------------------------------------------------
SELECT '--- COMPROBACIÓN FINAL ---' AS 'INFO';
-- No debe devolver filas: la cuenta sin cobros se elimino
CALL sp_cuenta_por_cobrar_obtener(@id_cxc2);
-- Si deben devolver filas: la caja, sus movimientos, sus dos cierres, el cobro anulado y su cuenta
CALL sp_caja_obtener(@id_caja);
CALL sp_movimiento_caja_listar_por_caja(@id_caja);
CALL sp_cierre_caja_listar_por_caja(@id_caja);
CALL sp_cobro_obtener(@id_cobro);
CALL sp_cuenta_por_cobrar_obtener(@id_cxc);
