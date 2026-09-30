USE sigmeta;

-- =========================================================================
-- ELIMINA TODOS LOS PROCEDIMIENTOS ALMACENADOS DEL PROYECTO
-- Ejecutar ANTES de recargar los archivos SP_*.sql (SP_yoe, SP_claudia y SP_daniel
-- usan CREATE PROCEDURE sin DROP IF EXISTS y fallan si el SP ya existe).
-- No toca tablas ni datos: solo borra procedimientos.
-- Los sql_prueba_*.sql no se eliminan: no crean objetos en la base, solo ejecutan CALL.
--
-- Uso: 1) ejecutar este archivo  2) ejecutar SP_yoe, SP_claudia, SP_daniel, SP_sebastian, SP_adriano
-- Total: 184 procedimientos.
--      3) comprobar: SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema = 'sigmeta';
-- =========================================================================

-- ---------- SP_yoe.sql (37) ----------
DROP PROCEDURE IF EXISTS sp_rol_insertar;
DROP PROCEDURE IF EXISTS sp_rol_modificar;
DROP PROCEDURE IF EXISTS sp_rol_eliminar;
DROP PROCEDURE IF EXISTS sp_rol_obtener;
DROP PROCEDURE IF EXISTS sp_rol_listar;
DROP PROCEDURE IF EXISTS sp_rol_buscar_por_tipo;
DROP PROCEDURE IF EXISTS sp_usuario_insertar;
DROP PROCEDURE IF EXISTS sp_usuario_modificar;
DROP PROCEDURE IF EXISTS sp_usuario_eliminar;
DROP PROCEDURE IF EXISTS sp_usuario_obtener;
DROP PROCEDURE IF EXISTS sp_usuario_listar;
DROP PROCEDURE IF EXISTS sp_usuario_buscar_por_nombre_usuario;
DROP PROCEDURE IF EXISTS sp_usuario_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_cliente_insertar;
DROP PROCEDURE IF EXISTS sp_cliente_modificar;
DROP PROCEDURE IF EXISTS sp_cliente_eliminar;
DROP PROCEDURE IF EXISTS sp_cliente_obtener;
DROP PROCEDURE IF EXISTS sp_cliente_listar;
DROP PROCEDURE IF EXISTS sp_cliente_buscar_por_documento;
DROP PROCEDURE IF EXISTS sp_cliente_buscar_por_razon_social;
DROP PROCEDURE IF EXISTS sp_cliente_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_proveedor_insertar;
DROP PROCEDURE IF EXISTS sp_proveedor_modificar;
DROP PROCEDURE IF EXISTS sp_proveedor_eliminar;
DROP PROCEDURE IF EXISTS sp_proveedor_obtener;
DROP PROCEDURE IF EXISTS sp_proveedor_listar;
DROP PROCEDURE IF EXISTS sp_proveedor_buscar_por_ruc;
DROP PROCEDURE IF EXISTS sp_proveedor_buscar_por_razon_social;
DROP PROCEDURE IF EXISTS sp_proveedor_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_insertar;   -- corregido/agregado en la ultima revision
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_modificar;   -- corregido/agregado en la ultima revision
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_eliminar;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_obtener;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_listar;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_listar_por_solicitante;
DROP PROCEDURE IF EXISTS sp_solicitud_autorizacion_buscar_vigente;

-- ---------- SP_claudia.sql (23) ----------
DROP PROCEDURE IF EXISTS sp_categoria_insertar;
DROP PROCEDURE IF EXISTS sp_categoria_modificar;
DROP PROCEDURE IF EXISTS sp_categoria_eliminar;
DROP PROCEDURE IF EXISTS sp_categoria_obtener;
DROP PROCEDURE IF EXISTS sp_categoria_listar;
DROP PROCEDURE IF EXISTS sp_producto_insertar;
DROP PROCEDURE IF EXISTS sp_producto_modificar;
DROP PROCEDURE IF EXISTS sp_producto_eliminar;
DROP PROCEDURE IF EXISTS sp_producto_obtener;
DROP PROCEDURE IF EXISTS sp_producto_listar;
DROP PROCEDURE IF EXISTS sp_cotizacion_insertar;
DROP PROCEDURE IF EXISTS sp_cotizacion_modificar;
DROP PROCEDURE IF EXISTS sp_cotizacion_eliminar;
DROP PROCEDURE IF EXISTS sp_cotizacion_obtener;
DROP PROCEDURE IF EXISTS sp_cotizacion_listar;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_listar;
DROP PROCEDURE IF EXISTS sp_categoria_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_producto_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_detalle_cotizacion_listar_todos;   -- corregido/agregado en la ultima revision

-- ---------- SP_daniel.sql (26) ----------
DROP PROCEDURE IF EXISTS sp_compra_insertar;
DROP PROCEDURE IF EXISTS sp_compra_modificar;
DROP PROCEDURE IF EXISTS sp_compra_eliminar;
DROP PROCEDURE IF EXISTS sp_compra_obtener;
DROP PROCEDURE IF EXISTS sp_compra_listar;
DROP PROCEDURE IF EXISTS sp_detalle_compra_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_compra_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_compra_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_compra_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_compra_listar;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_insertar;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_modificar;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_eliminar;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_obtener;
DROP PROCEDURE IF EXISTS sp_recepcion_compra_listar;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_recepcion_compra_listar;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_insertar;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_modificar;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_eliminar;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_obtener;
DROP PROCEDURE IF EXISTS sp_movimiento_inventario_listar;
DROP PROCEDURE IF EXISTS sp_compra_actualizar_estado;

-- ---------- SP_sebastian.sql (60) ----------
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_insertar;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_modificar;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_eliminar;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_obtener;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_listar;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_actualizar_estado;
DROP PROCEDURE IF EXISTS sp_orden_compra_cliente_buscar_por_cotizacion;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_listar;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_listar_por_orden;
DROP PROCEDURE IF EXISTS sp_detalle_orden_compra_cliente_actualizar_atendida;
DROP PROCEDURE IF EXISTS sp_venta_insertar;
DROP PROCEDURE IF EXISTS sp_venta_modificar;
DROP PROCEDURE IF EXISTS sp_venta_eliminar;
DROP PROCEDURE IF EXISTS sp_venta_obtener;
DROP PROCEDURE IF EXISTS sp_venta_listar;
DROP PROCEDURE IF EXISTS sp_venta_bloquear;
DROP PROCEDURE IF EXISTS sp_venta_actualizar_estado;
DROP PROCEDURE IF EXISTS sp_venta_saldo_pendiente_cliente;
DROP PROCEDURE IF EXISTS sp_venta_generar_cuenta_por_cobrar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_venta_listar;
DROP PROCEDURE IF EXISTS sp_detalle_venta_listar_por_venta;
DROP PROCEDURE IF EXISTS sp_detalle_venta_actualizar_despachado;
DROP PROCEDURE IF EXISTS sp_comprobante_insertar;
DROP PROCEDURE IF EXISTS sp_comprobante_modificar;
DROP PROCEDURE IF EXISTS sp_comprobante_eliminar;
DROP PROCEDURE IF EXISTS sp_comprobante_obtener;
DROP PROCEDURE IF EXISTS sp_comprobante_listar;
DROP PROCEDURE IF EXISTS sp_comprobante_listar_por_venta;
DROP PROCEDURE IF EXISTS sp_comprobante_listar_nc_por_relacionado;
DROP PROCEDURE IF EXISTS sp_comprobante_actualizar_estado;
DROP PROCEDURE IF EXISTS sp_comprobante_siguiente_correlativo;
DROP PROCEDURE IF EXISTS sp_comprobante_revertir_saldo_cpc;   -- corregido/agregado en la ultima revision
DROP PROCEDURE IF EXISTS sp_comprobante_restaurar_saldo_cpc;   -- corregido/agregado en la ultima revision
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_listar;
DROP PROCEDURE IF EXISTS sp_detalle_nota_credito_listar_por_comprobante;
DROP PROCEDURE IF EXISTS sp_despacho_insertar;
DROP PROCEDURE IF EXISTS sp_despacho_modificar;
DROP PROCEDURE IF EXISTS sp_despacho_eliminar;
DROP PROCEDURE IF EXISTS sp_despacho_obtener;
DROP PROCEDURE IF EXISTS sp_despacho_listar;
DROP PROCEDURE IF EXISTS sp_despacho_listar_por_venta;
DROP PROCEDURE IF EXISTS sp_despacho_siguiente_correlativo;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_insertar;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_modificar;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_eliminar;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_obtener;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_listar;
DROP PROCEDURE IF EXISTS sp_detalle_despacho_listar_por_despacho;

-- ---------- SP_adriano.sql (38) ----------
DROP PROCEDURE IF EXISTS sp_caja_insertar;
DROP PROCEDURE IF EXISTS sp_caja_modificar;
DROP PROCEDURE IF EXISTS sp_caja_eliminar;
DROP PROCEDURE IF EXISTS sp_caja_obtener;
DROP PROCEDURE IF EXISTS sp_caja_obtener_abierta_por_usuario;
DROP PROCEDURE IF EXISTS sp_caja_listar_todas_con_usuario;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_insertar;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_modificar;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_eliminar;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_obtener;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_listar_por_caja;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_listar_con_detalle;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_buscar_por_fechas;
DROP PROCEDURE IF EXISTS sp_movimiento_caja_totales_medio_pago;
DROP PROCEDURE IF EXISTS sp_cierre_caja_insertar;
DROP PROCEDURE IF EXISTS sp_cierre_caja_modificar;
DROP PROCEDURE IF EXISTS sp_cierre_caja_eliminar;
DROP PROCEDURE IF EXISTS sp_cierre_caja_obtener;
DROP PROCEDURE IF EXISTS sp_cierre_caja_obtener_por_caja;
DROP PROCEDURE IF EXISTS sp_cierre_caja_listar_con_detalle;
DROP PROCEDURE IF EXISTS sp_cierre_caja_buscar_por_fechas;
DROP PROCEDURE IF EXISTS sp_cierre_caja_calcular_monto_esperado;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_insertar;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_modificar;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_eliminar;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_obtener;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_obtener_por_venta;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_por_cliente;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_vencidas;
DROP PROCEDURE IF EXISTS sp_cuenta_por_cobrar_listar_todas_con_detalle;
DROP PROCEDURE IF EXISTS sp_cobro_insertar;
DROP PROCEDURE IF EXISTS sp_cobro_modificar;
DROP PROCEDURE IF EXISTS sp_cobro_eliminar;
DROP PROCEDURE IF EXISTS sp_cobro_obtener;
DROP PROCEDURE IF EXISTS sp_cobro_listar;   -- corregido/agregado en la ultima revision
DROP PROCEDURE IF EXISTS sp_cobro_listar_por_cuenta_por_cobrar;
DROP PROCEDURE IF EXISTS sp_cobro_listar_por_cliente;
