-- ============================================================================
-- SIGMETA-P3 - DROP COMPLETO (BORRADO TOTAL)
-- Fuente: esquema DDL consolidado de SIGMETA-P3, rama main (22/11/2026).
-- CUIDADO: destruye la estructura y TODOS los datos del esquema `sigmeta`.
-- Incluye las 32 tablas y, al final, elimina tambien procedimientos, indices,
-- vistas, funciones y cualquier otro objeto que pertenezca a la base de datos.
-- NO ejecutar en AWS ni en una base compartida. HACER RESPALDO antes.
-- Para recrear el sistema: ejecutar DDL, DML y scripts de procedimientos.
-- ============================================================================

USE sigmeta;
SET FOREIGN_KEY_CHECKS = 0;

-- Tablas (incluida la creada por migracion RF012).
DROP TABLE IF EXISTS `registro_anulacion`;
DROP TABLE IF EXISTS `costo_recepcion_historico`;
DROP TABLE IF EXISTS `correlativo_documento`;
DROP TABLE IF EXISTS `transaccion_control`;
DROP TABLE IF EXISTS `cierre_caja`;
DROP TABLE IF EXISTS `movimiento_caja`;
DROP TABLE IF EXISTS `cobro`;
DROP TABLE IF EXISTS `cuenta_por_cobrar`;
DROP TABLE IF EXISTS `caja`;
DROP TABLE IF EXISTS `detalle_despacho`;
DROP TABLE IF EXISTS `despacho`;
DROP TABLE IF EXISTS `detalle_comprobante`;
DROP TABLE IF EXISTS `detalle_nota_credito`;
DROP TABLE IF EXISTS `comprobante`;
DROP TABLE IF EXISTS `detalle_venta`;
DROP TABLE IF EXISTS `venta`;
DROP TABLE IF EXISTS `detalle_orden_compra_cliente`;
DROP TABLE IF EXISTS `orden_compra_cliente`;
DROP TABLE IF EXISTS `movimiento_inventario`;
DROP TABLE IF EXISTS `detalle_recepcion_compra`;
DROP TABLE IF EXISTS `recepcion_compra`;
DROP TABLE IF EXISTS `detalle_compra`;
DROP TABLE IF EXISTS `compra`;
DROP TABLE IF EXISTS `detalle_cotizacion`;
DROP TABLE IF EXISTS `cotizacion`;
DROP TABLE IF EXISTS `producto`;
DROP TABLE IF EXISTS `categoria`;
DROP TABLE IF EXISTS `solicitud_autorizacion`;
DROP TABLE IF EXISTS `proveedor`;
DROP TABLE IF EXISTS `cliente`;
DROP TABLE IF EXISTS `usuario`;
DROP TABLE IF EXISTS `rol`;

SET FOREIGN_KEY_CHECKS = 1;

-- Borra tambien los procedimientos almacenados y el esquema completo.
-- Esta orden elimina cualquier objeto residual de `sigmeta`.
DROP DATABASE IF EXISTS `sigmeta`;
