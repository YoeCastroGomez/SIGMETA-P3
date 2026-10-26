# Correcciones del Laboratorio 4 — SIGMETA (Grupo 5)

Fichas de las correcciones pedidas por el JP en la evaluación del Laboratorio 4.
Cada integrante completa su sección. Formato de cada ficha:

- **Qué estaba mal:** lo que dijo el JP.
- **Qué se cambió:** tabla, clase, DAO, SP y BO que se tocaron.
- **Cómo se prueba:** qué se corre y qué tiene que salir.

Proyecto: `SIGMETA-P3-v2`. Las pruebas se corren con `sigmeta-ejecucion → Main` (clase `PruebaCrud`).

---

## Estudiante 1 - Yoe

_(pendiente)_

---

## Estudiante 2 - Claudia

_(pendiente)_

---

## Estudiante 3 - Luis Daniel

_(pendiente)_

---

## Estudiante 4 - Sebastian

### 1. DetalleVenta apunta a la línea de la orden de compra del cliente

- **Qué estaba mal:** el JP pidió relacionar el despacho y la devolución con la línea de venta concreta, y también vincular DetalleVenta con la línea de orden atendida (como ya lo hacían DetalleRecepcionCompra y DetalleCompra).
- **Qué se cambió:**
  - Tabla `detalle_venta`: columna `id_detalle_orden_compra_cliente` (NULL en una venta directa) con FK a `detalle_orden_compra_cliente`.
  - Clase `DetalleVenta`: atributo `detalleOrdenCompraCliente`.
  - SP `sp_detalle_venta_insertar` y `sp_detalle_venta_modificar` reciben la línea de orden; `DetalleVentaDAOImpl` la guarda y la lee.
  - `VentaBOImpl.enlazarLineasOrden`: al registrar o modificar una venta desde una orden, une cada línea de venta con su línea de orden.
- **Cómo se prueba:** en Workbench, `detalle_venta` de la venta 1 (datos de ejemplo) muestra `id_detalle_orden_compra_cliente` = 1, 2 y 3; las ventas directas creadas por el Main tienen NULL.

### 2. DetalleNotaCredito apunta a DetalleVenta

- **Qué estaba mal:** DetalleNotaCredito referenciaba un Producto y no la línea de venta que se devuelve; con dos líneas del mismo producto en una venta no se sabía cuál se devolvía.
- **Qué se cambió:**
  - Tabla `detalle_nota_credito`: sin `id_producto`; con `id_detalle_venta` (FK a `detalle_venta`) y `UNIQUE (id_comprobante, id_detalle_venta)`.
  - Clase `DetalleNotaCredito`: atributo `detalleVenta` (el producto se obtiene de la línea de venta).
  - SP `sp_detalle_nota_credito_*` y `DetalleNotaCreditoDAOImpl` usan `id_detalle_venta`.
  - `ComprobanteBOImpl.registrarNotaCredito`: valida que cada línea pertenezca a la venta del comprobante de origen y que no se repita en la misma nota.
- **Cómo se prueba:** prueba 10 del Main. En Workbench, insertar dos veces la misma línea de venta en una misma nota da `Error 1062 Duplicate entry`.

### 3. Nota de crédito con descuento

- **Qué estaba mal:** la devolución en la nota de crédito no contemplaba los descuentos aplicados en la venta.
- **Qué se cambió:**
  - `ComprobanteBOImpl.registrarNotaCredito`: el importe devuelto es `importe de la línea de venta (ya con descuento) × cantidad devuelta ÷ cantidad vendida`; el descuento de la nota es la parte proporcional (`bruto − importe`).
  - No permite devolver más de lo vendido en la línea menos lo ya acreditado en notas vigentes.
  - `DetalleVenta.getPrecioEfectivo()`: precio efectivamente cobrado por unidad.
- **Cómo se prueba:** prueba 10 del Main. Línea de 2 unidades a S/ 45.00 con S/ 2.00 de descuento: devolver 1 unidad da importe S/ 44.00 y descuento S/ 1.00; intentar devolver 2 más se rechaza.

### 4. Sin moneda en Venta y Comprobante

- **Qué estaba mal:** el manejo de monedas forzaba todo a soles y se perdía trazabilidad. El JP sugirió usar una sola moneda.
- **Qué se cambió:** se eliminó el atributo moneda de `Venta` y `Comprobante`, la columna de sus tablas y de sus SP. El sistema trabaja solo en soles (RF007, RF008).
- **Cómo se prueba:** consulta a `information_schema.columns` sobre `venta`, `detalle_venta`, `comprobante` y `detalle_nota_credito` filtrando `column_name LIKE '%moneda%'`: no devuelve filas. Prueba 5 del Main.

### Ajustes adicionales del Lab 9 (verificación contra el catálogo RF008 y RF013)

Al comparar el código con el catálogo de requerimientos se encontraron diferencias y se corrigieron en tabla, model, DAO, SP y BO:

| Cambio | RF | Qué se hizo | Prueba |
|---|---|---|---|
| Quitar `NOTA_DEBITO` | — | Ningún RF la pide. Se quitó del enum `TipoComprobante` y del ENUM de `comprobante.tipo`. | — |
| Administrador que autoriza la NC | RF013, RNF001 | Columna `comprobante.id_administrador_autoriza` (FK a `usuario`), atributo `Comprobante.administradorAutoriza`, `sp_comprobante_insertar` y `ComprobanteDAOImpl` lo guardan; `registrarNotaCredito` registra al administrador validado. | Prueba 10 |
| Registro del envío del comprobante | RF008 | `ComprobanteBO.registrarEnvio(idComprobante, medio, idResponsable)`: solo FACTURA o BOLETA no anuladas, rol VENDEDOR o CAJERO, medio CORREO, MENSAJERIA o IMPRESO; guarda `medio_envio` y `fecha_envio` (`sp_comprobante_registrar_envio`). | Prueba 12 |
| Búsqueda de notas de crédito | RF013 | `ComprobanteBO.buscarNotasCredito(fechaDesde, fechaHasta, idCliente, serieNumeroOrigen)` con filtros opcionales por fecha, cliente y comprobante de origen (`sp_comprobante_buscar_nc`); valida que la fecha inicial no sea posterior a la final. | Prueba 13 |

- **Script para una BD existente (AWS):** `sql/migracion_lab9_sebastian.sql`, y después recargar todos los SP en orden: `00_drop_sps.sql`, `SP_yoe`, `SP_claudia`, `SP_daniel`, `SP_sebastian`, `SP_adriano`, `SP_integridad`. Las notas de crédito anteriores quedan con `id_administrador_autoriza` en NULL (no se registró en su momento).
- **BD nueva:** `sql/sigmeta.sql` y luego los mismos SP en ese orden.
- **Resultado esperado del Main:** `RESUMEN: 13 PRUEBAS OK | 0 CON ERROR (de 13)`.

---

## Estudiante 5 - Adriano

_(pendiente)_
