USE sigmeta;
SET NAMES utf8mb4;
DELIMITER $$

-- =========================================================================
-- 07 - PROCEDIMIENTOS DE LA REFORMA (RF012, RF013, RF015 y reglas de integridad)
-- Se ejecuta AL FINAL, despues de 06_SP_integridad.sql. Es re-ejecutable: cada
-- procedimiento se borra y se vuelve a crear. No cambia ninguna tabla.
--
--   A. Integridad de inventario: stock comprometido y costo promedio ponderado.
--   B. Listados de almacen (RF012): compras por recibir, ventas por despachar,
--      recepciones, despachos, kardex y stock de productos.
--   C. Notas de credito (RF013): lineas que se pueden devolver de un comprobante.
--   D. Reportes del Administrador (RF015): comercial, abastecimiento e inventario, caja.
--      Toda la agregacion se hace aqui, en SQL (RNF003): el BO no suma listas en memoria.
-- =========================================================================

-- -------------------------------------------------------------------------
-- A. INTEGRIDAD DE INVENTARIO
-- -------------------------------------------------------------------------

-- Stock comprometido de un producto: lo vendido y todavia no despachado en ventas vigentes.
-- Stock disponible para vender = stock_actual - comprometido. p_excluir_venta = 0 si no aplica.
DROP PROCEDURE IF EXISTS sp_stock_comprometido$$
CREATE PROCEDURE sp_stock_comprometido(IN p_id_producto INT, IN p_excluir_venta INT)
BEGIN
    SELECT COALESCE(SUM(d.cantidad - d.cantidad_despachada), 0) AS cantidad
    FROM detalle_venta d
    INNER JOIN venta v ON v.id = d.id_venta
    WHERE d.id_producto = p_id_producto
      AND v.anulado = FALSE
      AND v.id <> p_excluir_venta
      AND d.cantidad > d.cantidad_despachada;
END$$

-- Costo promedio ponderado: lo actualiza la recepcion de compra (el stock no se toca aqui).
DROP PROCEDURE IF EXISTS sp_producto_actualizar_costo$$
CREATE PROCEDURE sp_producto_actualizar_costo(IN p_id INT, IN p_costo DECIMAL(12,2))
BEGIN
    IF p_costo < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El costo unitario no puede ser negativo';
    END IF;
    UPDATE producto SET costo_unitario = p_costo WHERE id = p_id;
END$$

-- RF006: vuelve a activar una categoria desactivada (baja logica reversible)
DROP PROCEDURE IF EXISTS sp_categoria_activar$$
CREATE PROCEDURE sp_categoria_activar(IN p_id_categoria INT)
BEGIN
    UPDATE categoria SET estado = TRUE WHERE id = p_id_categoria;
END$$

-- RF005: vuelve a activar un producto desactivado (baja logica reversible)
DROP PROCEDURE IF EXISTS sp_producto_activar$$
CREATE PROCEDURE sp_producto_activar(IN p_id_producto INT)
BEGIN
    UPDATE producto SET estado = TRUE WHERE id = p_id_producto;
END$$

-- RF003: vuelve a activar un cliente desactivado (baja logica reversible)
DROP PROCEDURE IF EXISTS sp_cliente_activar$$
CREATE PROCEDURE sp_cliente_activar(IN p_id_cliente INT)
BEGIN
    UPDATE cliente SET estado = TRUE WHERE id = p_id_cliente;
END$$

-- -------------------------------------------------------------------------
-- B. LISTADOS DE ALMACEN (RF012)
-- -------------------------------------------------------------------------

-- Compras que todavia esperan mercaderia (REGISTRADA o RECIBIDA_PARCIAL)
DROP PROCEDURE IF EXISTS sp_compra_listar_por_recibir$$
CREATE PROCEDURE sp_compra_listar_por_recibir()
BEGIN
    SELECT c.id, c.numero, c.fecha_emision, c.fecha_recepcion_estimada, c.estado, c.total,
           p.razon_social AS proveedor, p.ruc,
           (SELECT COUNT(*) FROM detalle_compra d WHERE d.id_compra = c.id) AS lineas,
           (SELECT COUNT(*) FROM detalle_compra d WHERE d.id_compra = c.id AND d.cantidad_recibida < d.cantidad) AS lineas_pendientes
    FROM compra c
    INNER JOIN proveedor p ON p.id = c.id_proveedor
    WHERE c.anulado = FALSE AND c.estado IN ('REGISTRADA', 'RECIBIDA_PARCIAL')
    ORDER BY c.fecha_emision, c.id;
END$$

-- Lineas de una compra con lo pedido, lo recibido y lo pendiente (para registrar la recepcion)
DROP PROCEDURE IF EXISTS sp_compra_lineas_por_recibir$$
CREATE PROCEDURE sp_compra_lineas_por_recibir(IN p_id_compra INT)
BEGIN
    SELECT d.id AS id_detalle_compra, d.numero_linea, d.id_producto, pr.codigo_interno, pr.nombre AS producto,
           d.unidad_compra, pr.unidad_venta, d.factor_conversion,
           d.cantidad, d.cantidad_recibida, (d.cantidad - d.cantidad_recibida) AS pendiente,
           pr.stock_actual
    FROM detalle_compra d
    INNER JOIN producto pr ON pr.id = d.id_producto
    WHERE d.id_compra = p_id_compra
    ORDER BY d.numero_linea;
END$$

-- Recepciones registradas (todas, o las de una compra si p_id_compra > 0)
DROP PROCEDURE IF EXISTS sp_recepcion_buscar$$
CREATE PROCEDURE sp_recepcion_buscar(IN p_id_compra INT)
BEGIN
    SELECT r.id, r.id_compra, c.numero AS compra, p.razon_social AS proveedor, r.fecha_recepcion,
           r.observaciones, r.anulada, r.fecha_registro, u.nombre_usuario AS usuario,
           (SELECT COUNT(*) FROM detalle_recepcion_compra d WHERE d.id_recepcion_compra = r.id) AS lineas
    FROM recepcion_compra r
    INNER JOIN compra c ON c.id = r.id_compra
    INNER JOIN proveedor p ON p.id = c.id_proveedor
    INNER JOIN usuario u ON u.id = r.id_usuario_registro
    WHERE p_id_compra = 0 OR r.id_compra = p_id_compra
    ORDER BY r.id DESC;
END$$

-- Detalle de una recepcion con su producto
DROP PROCEDURE IF EXISTS sp_recepcion_detalle$$
CREATE PROCEDURE sp_recepcion_detalle(IN p_id_recepcion INT)
BEGIN
    SELECT dr.id, dc.numero_linea, pr.codigo_interno, pr.nombre AS producto, dc.unidad_compra, pr.unidad_venta,
           dc.factor_conversion, dr.cantidad_recibida, (dr.cantidad_recibida * dc.factor_conversion) AS cantidad_stock
    FROM detalle_recepcion_compra dr
    INNER JOIN detalle_compra dc ON dc.id = dr.id_detalle_compra
    INNER JOIN producto pr ON pr.id = dc.id_producto
    WHERE dr.id_recepcion_compra = p_id_recepcion
    ORDER BY dc.numero_linea;
END$$

-- Ventas que todavia tienen mercaderia por entregar (REGISTRADA o DESPACHADA_PARCIAL)
DROP PROCEDURE IF EXISTS sp_venta_listar_por_despachar$$
CREATE PROCEDURE sp_venta_listar_por_despachar()
BEGIN
    SELECT v.id, v.numero, v.fecha_emision, v.estado, v.condicion_pago, v.total,
           COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento,
           cl.direccion,
           (SELECT CONCAT(c.serie, '-', c.numero) FROM comprobante c
             WHERE c.id_venta = v.id AND c.tipo IN ('FACTURA', 'BOLETA') AND c.estado <> 'ANULADO'
             ORDER BY c.id DESC LIMIT 1) AS comprobante,
           (SELECT COUNT(*) FROM movimiento_caja m
             WHERE m.documento_origen = 'VENTA' AND m.id_documento_origen = v.id AND m.tipo = 'COBRO_CONTADO') AS cobros_contado,
           (SELECT COUNT(*) FROM detalle_venta d WHERE d.id_venta = v.id AND d.cantidad_despachada < d.cantidad) AS lineas_pendientes
    FROM venta v
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    WHERE v.anulado = FALSE AND v.estado IN ('REGISTRADA', 'DESPACHADA_PARCIAL')
    ORDER BY v.fecha_emision, v.id;
END$$

-- Lineas de una venta con lo vendido, lo despachado y lo pendiente (para registrar el despacho)
DROP PROCEDURE IF EXISTS sp_venta_lineas_por_despachar$$
CREATE PROCEDURE sp_venta_lineas_por_despachar(IN p_id_venta INT)
BEGIN
    SELECT d.id AS id_detalle_venta, d.numero_linea, d.id_producto, pr.codigo_interno, pr.nombre AS producto,
           pr.unidad_venta, d.cantidad, d.cantidad_despachada, (d.cantidad - d.cantidad_despachada) AS pendiente,
           pr.stock_actual
    FROM detalle_venta d
    INNER JOIN producto pr ON pr.id = d.id_producto
    WHERE d.id_venta = p_id_venta
    ORDER BY d.numero_linea;
END$$

-- Despachos registrados (todos, o los de una venta si p_id_venta > 0)
DROP PROCEDURE IF EXISTS sp_despacho_buscar$$
CREATE PROCEDURE sp_despacho_buscar(IN p_id_venta INT)
BEGIN
    SELECT d.id, d.id_venta, v.numero AS venta, CONCAT(d.serie_guia, '-', d.numero_guia) AS guia,
           COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente,
           d.fecha_despacho, d.direccion_entrega, d.transportista, d.anulado, d.fecha_registro,
           u.nombre_usuario AS usuario,
           (SELECT COUNT(*) FROM detalle_despacho x WHERE x.id_despacho = d.id) AS lineas
    FROM despacho d
    INNER JOIN venta v ON v.id = d.id_venta
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    INNER JOIN usuario u ON u.id = d.id_usuario_registro
    WHERE p_id_venta = 0 OR d.id_venta = p_id_venta
    ORDER BY d.id DESC;
END$$

-- Detalle de un despacho con su producto
DROP PROCEDURE IF EXISTS sp_despacho_detalle$$
CREATE PROCEDURE sp_despacho_detalle(IN p_id_despacho INT)
BEGIN
    SELECT dd.id, dv.numero_linea, pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           dv.cantidad AS cantidad_vendida, dd.cantidad_despachada
    FROM detalle_despacho dd
    INNER JOIN detalle_venta dv ON dv.id = dd.id_detalle_venta
    INNER JOIN producto pr ON pr.id = dv.id_producto
    WHERE dd.id_despacho = p_id_despacho
    ORDER BY dv.numero_linea;
END$$

-- Kardex: movimientos de inventario con filtros opcionales.
-- p_id_producto = 0 (todos), p_desde / p_hasta = NULL (sin limite), p_tipo = '' (todos).
-- La columna "signo" dice si el movimiento suma (+1) o resta (-1) existencias.
DROP PROCEDURE IF EXISTS sp_movimiento_buscar$$
CREATE PROCEDURE sp_movimiento_buscar(IN p_id_producto INT, IN p_desde DATE, IN p_hasta DATE, IN p_tipo VARCHAR(40))
BEGIN
    SELECT m.id, m.fecha_movimiento, m.tipo, m.id_producto, pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           m.cantidad, m.stock_resultante, m.cantidad_contada, m.motivo, u.nombre_usuario AS usuario,
           m.id_recepcion_compra, m.id_despacho, m.id_comprobante, m.id_movimiento_revertido,
           CASE WHEN m.tipo IN ('INGRESO_COMPRA', 'INGRESO_PRODUCCION', 'INGRESO_DEVOLUCION',
                                'INGRESO_REVERSION_DESPACHO', 'AJUSTE_INGRESO', 'REVERSION_AJUSTE_SALIDA')
                THEN 1 ELSE -1 END AS signo,
           EXISTS (SELECT 1 FROM movimiento_inventario r WHERE r.id_movimiento_revertido = m.id) AS revertido
    FROM movimiento_inventario m
    INNER JOIN producto pr ON pr.id = m.id_producto
    INNER JOIN usuario u ON u.id = m.id_usuario_registro
    WHERE (p_id_producto = 0 OR m.id_producto = p_id_producto)
      AND (p_desde IS NULL OR m.fecha_movimiento >= p_desde)
      AND (p_hasta IS NULL OR m.fecha_movimiento < DATE_ADD(p_hasta, INTERVAL 1 DAY))
      AND (p_tipo IS NULL OR p_tipo = '' OR m.tipo = p_tipo)
    ORDER BY m.fecha_movimiento DESC, m.id DESC
    LIMIT 500;
END$$

-- Existencias de los productos activos: fisico, comprometido y disponible (kardex y ajuste por conteo)
DROP PROCEDURE IF EXISTS sp_stock_listar_productos$$
CREATE PROCEDURE sp_stock_listar_productos()
BEGIN
    SELECT pr.id, pr.codigo_interno, pr.nombre, c.nombre AS categoria, pr.unidad_venta,
           pr.stock_actual, pr.stock_minimo, pr.costo_unitario,
           COALESCE(k.comprometido, 0) AS comprometido,
           pr.stock_actual - COALESCE(k.comprometido, 0) AS disponible
    FROM producto pr
    INNER JOIN categoria c ON c.id = pr.id_categoria
    LEFT JOIN (SELECT d.id_producto, SUM(d.cantidad - d.cantidad_despachada) AS comprometido
               FROM detalle_venta d INNER JOIN venta v ON v.id = d.id_venta
               WHERE v.anulado = FALSE AND d.cantidad > d.cantidad_despachada
               GROUP BY d.id_producto) k ON k.id_producto = pr.id
    WHERE pr.estado = TRUE
    ORDER BY pr.nombre;
END$$

-- -------------------------------------------------------------------------
-- C. NOTAS DE CREDITO (RF013)
-- -------------------------------------------------------------------------

-- Lineas de la venta de un comprobante con lo vendido, lo entregado, lo ya acreditado en notas de
-- credito vigentes y lo que todavia se puede devolver (entregado - acreditado).
DROP PROCEDURE IF EXISTS sp_comprobante_lineas_devolvibles$$
CREATE PROCEDURE sp_comprobante_lineas_devolvibles(IN p_id_comprobante INT)
BEGIN
    SELECT dv.id AS id_detalle_venta, dv.numero_linea, pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           dv.cantidad, dv.cantidad_despachada, dv.precio_unitario, dv.descuento, dv.importe,
           COALESCE(a.acreditado, 0) AS acreditado,
           GREATEST(dv.cantidad_despachada - COALESCE(a.acreditado, 0), 0) AS devolvible
    FROM comprobante c
    INNER JOIN detalle_venta dv ON dv.id_venta = c.id_venta
    INNER JOIN producto pr ON pr.id = dv.id_producto
    LEFT JOIN (SELECT dn.id_detalle_venta, SUM(dn.cantidad) AS acreditado
               FROM detalle_nota_credito dn
               INNER JOIN comprobante nc ON nc.id = dn.id_comprobante
               WHERE nc.tipo = 'NOTA_CREDITO' AND nc.estado <> 'ANULADO'
               GROUP BY dn.id_detalle_venta) a ON a.id_detalle_venta = dv.id
    WHERE c.id = p_id_comprobante
    ORDER BY dv.numero_linea;
END$$

-- Comprobantes (factura o boleta vigentes) sobre los que se puede emitir una nota de credito
DROP PROCEDURE IF EXISTS sp_comprobante_listar_para_nota$$
CREATE PROCEDURE sp_comprobante_listar_para_nota()
BEGIN
    SELECT c.id, c.tipo, CONCAT(c.serie, '-', c.numero) AS comprobante, c.fecha_emision, c.total,
           v.id AS id_venta, v.numero AS venta, v.condicion_pago,
           COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento
    FROM comprobante c
    INNER JOIN venta v ON v.id = c.id_venta
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    WHERE c.tipo IN ('FACTURA', 'BOLETA') AND c.estado IN ('EMITIDO', 'ACEPTADO') AND v.anulado = FALSE
      AND EXISTS (SELECT 1 FROM detalle_venta dv WHERE dv.id_venta = v.id AND dv.cantidad_despachada > 0)
    ORDER BY c.id DESC
    LIMIT 300;
END$$

-- Notas de credito con su comprobante de origen, cliente y quien autorizo.
-- Filtros opcionales: p_desde / p_hasta = NULL, p_texto = '' (cliente, documento, numero de la nota o del origen).
DROP PROCEDURE IF EXISTS sp_nota_credito_buscar$$
CREATE PROCEDURE sp_nota_credito_buscar(IN p_desde DATE, IN p_hasta DATE, IN p_texto VARCHAR(150))
BEGIN
    SELECT nc.id, CONCAT(nc.serie, '-', nc.numero) AS nota, nc.fecha_emision, nc.estado,
           nc.sub_total, nc.igv, nc.total, nc.motivo,
           o.id AS id_origen, o.tipo AS tipo_origen, CONCAT(o.serie, '-', o.numero) AS origen,
           v.id AS id_venta, v.numero AS venta, v.condicion_pago,
           COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento,
           ua.nombre_usuario AS autorizo, nc.fecha_registro
    FROM comprobante nc
    INNER JOIN venta v ON v.id = nc.id_venta
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    LEFT JOIN comprobante o ON o.id = nc.id_comprobante_relacionado
    LEFT JOIN usuario ua ON ua.id = nc.id_administrador_autoriza
    WHERE nc.tipo = 'NOTA_CREDITO'
      AND (p_desde IS NULL OR nc.fecha_emision >= p_desde)
      AND (p_hasta IS NULL OR nc.fecha_emision <= p_hasta)
      AND (p_texto IS NULL OR p_texto = ''
           OR cl.razon_social LIKE CONCAT('%', p_texto, '%')
           OR CONCAT(cl.nombres, ' ', cl.apellidos) LIKE CONCAT('%', p_texto, '%')
           OR cl.numero_documento LIKE CONCAT('%', p_texto, '%')
           OR CONCAT(nc.serie, '-', nc.numero) LIKE CONCAT('%', p_texto, '%')
           OR CONCAT(o.serie, '-', o.numero) LIKE CONCAT('%', p_texto, '%'))
    ORDER BY nc.id DESC
    LIMIT 300;
END$$

-- Lineas de una nota de credito con su producto
DROP PROCEDURE IF EXISTS sp_nota_credito_detalle$$
CREATE PROCEDURE sp_nota_credito_detalle(IN p_id_nota INT)
BEGIN
    SELECT dn.numero_linea, pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           dn.cantidad, dn.precio_unitario, dn.descuento, dn.importe
    FROM detalle_nota_credito dn
    INNER JOIN detalle_venta dv ON dv.id = dn.id_detalle_venta
    INNER JOIN producto pr ON pr.id = dv.id_producto
    WHERE dn.id_comprobante = p_id_nota
    ORDER BY dn.numero_linea;
END$$

-- Cobros al contado registrados en caja para una venta (para exigir el cobro antes del despacho)
DROP PROCEDURE IF EXISTS sp_venta_contar_cobros_contado$$
CREATE PROCEDURE sp_venta_contar_cobros_contado(IN p_id_venta INT)
BEGIN
    SELECT COUNT(*) AS cobros
    FROM movimiento_caja
    WHERE documento_origen = 'VENTA' AND id_documento_origen = p_id_venta AND tipo = 'COBRO_CONTADO';
END$$

-- -------------------------------------------------------------------------
-- D. REPORTES (RF015)
-- Las ventas se cuentan por fecha de emision y excluyen las anuladas.
-- -------------------------------------------------------------------------

DROP PROCEDURE IF EXISTS sp_reporte_ventas_totales$$
CREATE PROCEDURE sp_reporte_ventas_totales(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT
      (SELECT COUNT(*) FROM venta WHERE anulado = FALSE AND condicion_pago = 'CONTADO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS ventas_contado,
      (SELECT COALESCE(SUM(total), 0) FROM venta WHERE anulado = FALSE AND condicion_pago = 'CONTADO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS monto_contado,
      (SELECT COUNT(*) FROM venta WHERE anulado = FALSE AND condicion_pago = 'CREDITO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS ventas_credito,
      (SELECT COALESCE(SUM(total), 0) FROM venta WHERE anulado = FALSE AND condicion_pago = 'CREDITO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS monto_credito,
      (SELECT COUNT(*) FROM venta WHERE anulado = TRUE AND fecha_emision BETWEEN p_desde AND p_hasta) AS ventas_anuladas,
      (SELECT COUNT(*) FROM comprobante WHERE tipo = 'NOTA_CREDITO' AND estado <> 'ANULADO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS notas_credito,
      (SELECT COALESCE(SUM(total), 0) FROM comprobante WHERE tipo = 'NOTA_CREDITO' AND estado <> 'ANULADO' AND fecha_emision BETWEEN p_desde AND p_hasta) AS monto_notas_credito,
      (SELECT COALESCE(SUM(saldo_pendiente), 0) FROM cuenta_por_cobrar WHERE saldo_pendiente > 0) AS saldo_por_cobrar,
      (SELECT COALESCE(SUM(saldo_pendiente), 0) FROM cuenta_por_cobrar WHERE saldo_pendiente > 0 AND fecha_vencimiento < CURDATE()) AS saldo_vencido;
END$$

DROP PROCEDURE IF EXISTS sp_reporte_ventas_por_dia$$
CREATE PROCEDURE sp_reporte_ventas_por_dia(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT v.fecha_emision AS fecha, COUNT(*) AS ventas,
           SUM(v.sub_total) AS sub_total, SUM(v.igv) AS igv, SUM(v.total) AS total
    FROM venta v
    WHERE v.anulado = FALSE AND v.fecha_emision BETWEEN p_desde AND p_hasta
    GROUP BY v.fecha_emision
    ORDER BY v.fecha_emision;
END$$

DROP PROCEDURE IF EXISTS sp_reporte_ventas_por_vendedor$$
CREATE PROCEDURE sp_reporte_ventas_por_vendedor(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT u.nombre_usuario AS usuario, CONCAT(u.nombres, ' ', u.apellidos) AS vendedor,
           COUNT(*) AS ventas, SUM(v.total) AS total
    FROM venta v
    INNER JOIN usuario u ON u.id = v.id_usuario_registro
    WHERE v.anulado = FALSE AND v.fecha_emision BETWEEN p_desde AND p_hasta
    GROUP BY u.id, u.nombre_usuario, u.nombres, u.apellidos
    ORDER BY total DESC;
END$$

DROP PROCEDURE IF EXISTS sp_reporte_ventas_por_cliente$$
CREATE PROCEDURE sp_reporte_ventas_por_cliente(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento,
           COUNT(*) AS ventas, SUM(v.total) AS total
    FROM venta v
    INNER JOIN cliente cl ON cl.id = v.id_cliente
    WHERE v.anulado = FALSE AND v.fecha_emision BETWEEN p_desde AND p_hasta
    GROUP BY cl.id, cl.razon_social, cl.nombres, cl.apellidos, cl.numero_documento
    ORDER BY total DESC
    LIMIT 10;
END$$

-- Credito de clientes a la fecha: limite, deuda, deuda vencida y dias de atraso
DROP PROCEDURE IF EXISTS sp_reporte_credito_clientes$$
CREATE PROCEDURE sp_reporte_credito_clientes()
BEGIN
    SELECT COALESCE(cl.razon_social, CONCAT(cl.nombres, ' ', cl.apellidos)) AS cliente, cl.numero_documento,
           cl.limite_credito, cl.plazo_credito_dias, cl.calificacion_crediticia,
           COALESCE(SUM(x.saldo_pendiente), 0) AS saldo_pendiente,
           COALESCE(SUM(CASE WHEN x.fecha_vencimiento < CURDATE() THEN x.saldo_pendiente ELSE 0 END), 0) AS saldo_vencido,
           COALESCE(MAX(CASE WHEN x.fecha_vencimiento < CURDATE() THEN DATEDIFF(CURDATE(), x.fecha_vencimiento) ELSE 0 END), 0) AS dias_atraso,
           cl.limite_credito - COALESCE(SUM(x.saldo_pendiente), 0) AS credito_disponible
    FROM cliente cl
    LEFT JOIN cuenta_por_cobrar x ON x.id_cliente = cl.id AND x.saldo_pendiente > 0
    WHERE cl.condicion_pago = 'CREDITO' OR x.id IS NOT NULL
    GROUP BY cl.id, cl.razon_social, cl.nombres, cl.apellidos, cl.numero_documento,
             cl.limite_credito, cl.plazo_credito_dias, cl.calificacion_crediticia
    ORDER BY saldo_vencido DESC, saldo_pendiente DESC;
END$$

DROP PROCEDURE IF EXISTS sp_reporte_compras_por_proveedor$$
CREATE PROCEDURE sp_reporte_compras_por_proveedor(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT p.razon_social AS proveedor, p.ruc, COUNT(*) AS compras, SUM(c.total) AS total,
           SUM(CASE WHEN c.estado IN ('REGISTRADA', 'RECIBIDA_PARCIAL') THEN 1 ELSE 0 END) AS por_recibir
    FROM compra c
    INNER JOIN proveedor p ON p.id = c.id_proveedor
    WHERE c.anulado = FALSE AND c.fecha_emision BETWEEN p_desde AND p_hasta
    GROUP BY p.id, p.razon_social, p.ruc
    ORDER BY total DESC;
END$$

-- Productos de mayor salida: despachos menos sus reversiones y menos lo devuelto por nota de credito
DROP PROCEDURE IF EXISTS sp_reporte_productos_mayor_movimiento$$
CREATE PROCEDURE sp_reporte_productos_mayor_movimiento(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta,
           SUM(CASE WHEN m.tipo = 'SALIDA_DESPACHO' THEN m.cantidad ELSE -m.cantidad END) AS unidades,
           SUM(CASE WHEN m.tipo = 'SALIDA_DESPACHO' THEN m.cantidad ELSE -m.cantidad END) * pr.precio_venta AS importe_referencial,
           pr.stock_actual
    FROM movimiento_inventario m
    INNER JOIN producto pr ON pr.id = m.id_producto
    WHERE m.tipo IN ('SALIDA_DESPACHO', 'INGRESO_REVERSION_DESPACHO', 'INGRESO_DEVOLUCION')
      AND m.fecha_movimiento >= p_desde AND m.fecha_movimiento < DATE_ADD(p_hasta, INTERVAL 1 DAY)
    GROUP BY pr.id, pr.codigo_interno, pr.nombre, pr.unidad_venta, pr.precio_venta, pr.stock_actual
    HAVING unidades > 0
    ORDER BY unidades DESC
    LIMIT 10;
END$$

-- Productos activos con existencias que no tuvieron ninguna salida por despacho en el periodo
DROP PROCEDURE IF EXISTS sp_reporte_productos_sin_movimiento$$
CREATE PROCEDURE sp_reporte_productos_sin_movimiento(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT pr.codigo_interno, pr.nombre AS producto, pr.unidad_venta, pr.stock_actual,
           pr.stock_actual * pr.costo_unitario AS valor_inmovilizado,
           (SELECT MAX(x.fecha_movimiento) FROM movimiento_inventario x
             WHERE x.id_producto = pr.id AND x.tipo = 'SALIDA_DESPACHO') AS ultima_salida
    FROM producto pr
    WHERE pr.estado = TRUE AND pr.stock_actual > 0
      AND NOT EXISTS (SELECT 1 FROM movimiento_inventario m
                      WHERE m.id_producto = pr.id AND m.tipo = 'SALIDA_DESPACHO'
                        AND m.fecha_movimiento >= p_desde AND m.fecha_movimiento < DATE_ADD(p_hasta, INTERVAL 1 DAY))
    ORDER BY valor_inmovilizado DESC
    LIMIT 50;
END$$

-- Inventario valorizado al costo promedio, por categoria
DROP PROCEDURE IF EXISTS sp_reporte_inventario_valorizado$$
CREATE PROCEDURE sp_reporte_inventario_valorizado()
BEGIN
    SELECT c.nombre AS categoria, COUNT(*) AS productos,
           SUM(CASE WHEN pr.stock_actual <= pr.stock_minimo THEN 1 ELSE 0 END) AS en_minimo,
           SUM(pr.stock_actual * pr.costo_unitario) AS valor_costo,
           SUM(pr.stock_actual * pr.precio_venta) AS valor_venta
    FROM producto pr
    INNER JOIN categoria c ON c.id = pr.id_categoria
    WHERE pr.estado = TRUE
    GROUP BY c.id, c.nombre
    ORDER BY valor_costo DESC;
END$$

-- Cierres de caja del periodo con su cajero, su diferencia y quien reabrio
DROP PROCEDURE IF EXISTS sp_reporte_caja_cierres$$
CREATE PROCEDURE sp_reporte_caja_cierres(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT ci.id, ci.id_caja, ca.fecha_apertura, ci.fecha_cierre, ca.monto_inicial,
           ci.monto_calculado, ci.monto_declarado, ci.diferencia, ci.estado,
           ua.nombre_usuario AS cajero, uc.nombre_usuario AS usuario_cierre,
           ur.nombre_usuario AS usuario_reapertura, ci.fecha_reapertura, ci.motivo_reapertura
    FROM cierre_caja ci
    INNER JOIN caja ca ON ca.id = ci.id_caja
    INNER JOIN usuario ua ON ua.id = ca.id_usuario_apertura
    INNER JOIN usuario uc ON uc.id = ci.id_usuario_cierre
    LEFT JOIN usuario ur ON ur.id = ci.id_usuario_reapertura
    WHERE ci.fecha_cierre >= p_desde AND ci.fecha_cierre < DATE_ADD(p_hasta, INTERVAL 1 DAY)
    ORDER BY ci.fecha_cierre DESC;
END$$

-- Dinero que paso por las cajas en el periodo, por medio de pago
DROP PROCEDURE IF EXISTS sp_reporte_caja_por_medio_pago$$
CREATE PROCEDURE sp_reporte_caja_por_medio_pago(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT m.medio_pago,
           SUM(CASE WHEN m.tipo = 'COBRO_CONTADO' THEN m.monto ELSE 0 END) AS cobros_contado,
           SUM(CASE WHEN m.tipo = 'COBRO_CREDITO' THEN m.monto ELSE 0 END) AS cobros_credito,
           SUM(CASE WHEN m.tipo = 'INGRESO_MANUAL' THEN m.monto ELSE 0 END) AS ingresos,
           SUM(CASE WHEN m.tipo = 'EGRESO_MANUAL' THEN m.monto ELSE 0 END) AS egresos,
           SUM(CASE WHEN m.tipo = 'EGRESO_MANUAL' THEN -m.monto ELSE m.monto END) AS neto
    FROM movimiento_caja m
    WHERE m.fecha_movimiento >= p_desde AND m.fecha_movimiento < DATE_ADD(p_hasta, INTERVAL 1 DAY)
    GROUP BY m.medio_pago
    ORDER BY neto DESC;
END$$

-- Diferencias de los cierres vigentes del periodo, por cajero
DROP PROCEDURE IF EXISTS sp_reporte_caja_por_cajero$$
CREATE PROCEDURE sp_reporte_caja_por_cajero(IN p_desde DATE, IN p_hasta DATE)
BEGIN
    SELECT ua.nombre_usuario AS cajero, COUNT(*) AS cierres,
           SUM(CASE WHEN ci.diferencia < 0 THEN 1 ELSE 0 END) AS con_faltante,
           SUM(CASE WHEN ci.diferencia > 0 THEN 1 ELSE 0 END) AS con_sobrante,
           SUM(CASE WHEN ci.diferencia < 0 THEN ci.diferencia ELSE 0 END) AS faltantes,
           SUM(CASE WHEN ci.diferencia > 0 THEN ci.diferencia ELSE 0 END) AS sobrantes,
           SUM(ci.diferencia) AS diferencia_neta
    FROM cierre_caja ci
    INNER JOIN caja ca ON ca.id = ci.id_caja
    INNER JOIN usuario ua ON ua.id = ca.id_usuario_apertura
    WHERE ci.estado = 'VIGENTE'
      AND ci.fecha_cierre >= p_desde AND ci.fecha_cierre < DATE_ADD(p_hasta, INTERVAL 1 DAY)
    GROUP BY ua.id, ua.nombre_usuario
    ORDER BY ua.nombre_usuario;
END$$

DELIMITER ;
