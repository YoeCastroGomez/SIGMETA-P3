USE sigmeta;

-- ============================================================
-- PRUEBAS DE HUMO / CALLS PARA PROCEDIMIENTOS DE SEBASTIAN
-- ============================================================

-- 1. Listar ordenes de compra de clientes
CALL sp_orden_compra_cliente_listar();

-- 2. Listar ventas
CALL sp_venta_listar();

-- 3. Listar comprobantes
CALL sp_comprobante_listar();

-- 4. Listar despachos
CALL sp_despacho_listar();

-- 5. Probar correlativo de comprobante
SET @correlativo_fac = '';
CALL sp_comprobante_siguiente_correlativo('F001', @correlativo_fac);
SELECT @correlativo_fac AS siguiente_factura;

-- 6. Probar correlativo de guia
SET @correlativo_guia = '';
CALL sp_despacho_siguiente_correlativo('T001', @correlativo_guia);
SELECT @correlativo_guia AS siguiente_guia;
