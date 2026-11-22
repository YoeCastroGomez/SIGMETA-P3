# SIGMETA: auditoría de diseño de la lógica de negocio

Revisión del modelo de datos, la ubicación de las reglas, los estados de cada documento y lo que está fijo en el código. No es una revisión de seguridad ni una lista de errores (los errores están en `HALLAZGOS.md`). La pregunta es otra: ¿el diseño está bien armado y aguanta que el sistema crezca?

**Qué se revisó:** el esquema (`sql/1_base/sigmeta.sql` y las migraciones), los procedimientos almacenados y los BO de `sigmeta-services`: ventas, comprobantes, cotizaciones, órdenes, almacén, compras, caja, cobranza y autorizaciones. El frontend Blazor quedó fuera, porque solo muestra datos.

**Veredicto corto:** el diseño funcional es sólido para un ERP de este tamaño. La cadena de documentos se puede seguir de punta a punta, los precios quedan congelados en cada documento y casi todo se revierte con movimientos contrarios en vez de borrarse. Las debilidades están en la forma del código, no en las reglas:

- el modelo de dominio no tiene comportamiento;
- las reglas se repiten entre Java y SQL;
- los efectos secundarios están cableados a mano dentro de cada operación;
- quedan algunos montos e historiales que se sobrescriben.

---

## 1. Aciertos transversales (mantener)

| Patrón | Dónde se ve | Por qué está bien |
|---|---|---|
| Precio congelado en cada línea | `detalle_cotizacion`, `detalle_venta`, `detalle_compra` y `detalle_nota_credito` guardan su `precio_unitario`, `descuento` e `importe` | Si mañana cambia el precio del producto, las facturas de ayer no cambian. |
| Unidad y factor congelados en la compra | `detalle_compra.unidad_compra` y `factor_conversion` | Una compra por caja sigue siendo por caja aunque el producto cambie su unidad. |
| Trazabilidad línea a línea | `detalle_venta.id_detalle_orden_compra_cliente`, `detalle_despacho.id_detalle_venta`, `detalle_nota_credito.id_detalle_venta`, `detalle_recepcion_compra.id_detalle_compra` | La cadena se puede seguir por línea, no solo por documento: cotización → orden → venta → despacho / nota de crédito. Pocos ERP académicos llegan a este nivel. |
| Reversión con movimiento contrario | `movimiento_inventario.id_movimiento_revertido` (UNIQUE), los tipos `INGRESO_REVERSION_DESPACHO`, `SALIDA_REVERSION_COMPRA` y `REVERSION_AJUSTE_*`, y `CompensacionCaja` | El kardex y la caja no se reescriben: se compensan, y un movimiento no se revierte dos veces. |
| Cierre de caja que no se borra | `cierre_caja.estado` VIGENTE/REVERTIDO, con quién reabrió, cuándo y por qué | La reapertura deja rastro. |
| Autorización atada a la operación | `solicitud_autorizacion.huella` (hash de los datos), de un solo uso (`consumir`) | Lo que el Administrador aprueba es exactamente lo que se registra; si el Vendedor cambia una línea, la autorización ya no sirve. Es un diseño muy bueno. |
| Stock disponible ≠ stock físico | `sp_stock_comprometido` en `VentaBOImpl.verificarProductosYStock` | Dos ventas no prometen las mismas unidades. |
| Transacción por operación de negocio | `Transaccion.ejecutar` (anidable, con rollback completo) | Una venta con su cuenta por cobrar, o una nota de crédito con su stock y su caja, se guardan juntas o no se guardan. |
| Lenguaje del negocio | `OrdenCompraCliente`, `NotaCredito`, `Despacho` (guía), `CuentaPorCobrar`, `CierreCaja`, `MovimientoInventario` y tipos como `SALIDA_DESPACHO` | El código habla el idioma de la ferretería, no el de la base de datos. |

---

## 2. Dictamen por módulo

### 2.1 Ventas y comprobantes

**Aciertos**
- La factura copia los totales de la venta (`ComprobanteBOImpl.emitir`), y la nota de crédito calcula su importe con el precio efectivamente cobrado: importe de la línea por unidad, con su descuento proporcional.
- Solo se devuelve lo despachado, menos lo ya acreditado. Así el reingreso al stock siempre tiene una salida previa.
- Un comprobante con mercadería despachada no se anula: se emite una nota de crédito. Es la regla contable correcta.

**Oportunidades de mejora**
1. **El PDF de la factura no es una foto del momento.** `ComprobanteBOImpl.pdf` (línea ~596) arma las líneas con el nombre y el código **actuales** del producto, y con la dirección **actual** del cliente. Si se renombra un producto o el cliente se muda, una factura vieja se reimprime distinta. Además, la factura no tiene líneas propias: depende de `detalle_venta`, que se puede modificar si se anula la factura.
2. **Dos fuentes de verdad para "anulado".** `venta`, `cotizacion`, `compra` y `orden_compra_cliente` tienen el booleano `anulado` **y** el valor `ANULADA` en `estado`. Se pueden dar combinaciones imposibles (`anulado = TRUE` con `estado = 'REGISTRADA'`), y el código revisa una u otra según el lugar.
3. **Estados que nada usa.** `comprobante.estado` tiene `ACEPTADO` y `RECHAZADO` (respuesta de SUNAT), pero ninguna operación los pone. Son ruido en la máquina de estados.
4. **Series fijas en el código:** `"F001"`, `"B001"`, `"FC01"`, `"BC01"` (`ComprobanteBOImpl` líneas 137 y 189) y `"T001"` (`DespachoBOImpl` 150). Lo mismo con los prefijos `VTA-`, `OCC-`, `COT-` y `OC-`.
5. **`ComprobanteBOImpl.registrarNotaCredito` hace todo en un método:** valida, numera, guarda, mueve el stock, rebaja la deuda y devuelve dinero por caja. Llega a la cobranza con `new CuentaPorCobrarDAOImpl()` en medio de la lógica (línea 236).
6. **Al anular una nota de crédito, el stock sale como `AJUSTE_SALIDA`** (línea 489), el mismo tipo que un conteo físico, y sin `id_movimiento_revertido`. En el kardex, una devolución revertida se confunde con una merma.

**Propuesta**
- Agregar a `comprobante` una foto de lo que se imprimió: `cliente_documento`, `cliente_nombre` y `cliente_direccion`, más una tabla `detalle_comprobante` (o copiar `codigo` y `descripcion` en la línea al emitir). El PDF se arma solo con eso.
- Dejar **un solo** campo de estado. `anulado` pasa a ser derivado (`estado = 'ANULADA'`), y el motivo y la fecha de anulación se quedan como datos de auditoría.
- Sacar `ACEPTADO` y `RECHAZADO` hasta que exista el envío a SUNAT, o modelarlos aparte como `estado_sunat`. Es otro eje: el estado comercial y el tributario no son lo mismo.
- Crear una tabla `serie_documento (tipo, serie, activa)` y que `sp_correlativo_siguiente` lea de ahí.
- Agregar el tipo `SALIDA_REVERSION_DEVOLUCION`, enlazado con `id_movimiento_revertido` al `INGRESO_DEVOLUCION` original, igual que las demás reversiones.

### 2.2 Cotizaciones y órdenes de clientes

**Aciertos**
- El ciclo `REGISTRADA → ENVIADA → ACEPTADA | RECHAZADA` está centralizado en un `cambiarEstado(desde, hacia)` (`CotizacionBOImpl` ~220).
- La orden copia las líneas de la cotización aceptada, y la venta vende al precio de la orden.

**Oportunidades de mejora**
- `VENCIDA` se calcula con un proceso en lote (`sp_cotizacion_marcar_vencidas`) que corre **antes de listar**. Funciona, pero un estado que depende de la fecha queda guardado: si no se lista, no se actualiza.
- `orden_compra_cliente.estado` (`ATENDIDA_PARCIAL` / `ATENDIDA`) se recalcula desde el despacho (`DespachoBOImpl` ~375). El estado de la orden lo decide otro módulo.

**Propuesta**
- Calcular "vencida" al leer (`estado = ENVIADA` y `fecha_vigencia < hoy` → se muestra vencida) y guardar solo las transiciones que hace una persona.
- Que la orden calcule su propio estado a partir de `cantidad_atendida` de sus líneas, con un método de la orden que el despacho llame, en vez de que el despacho escriba el estado.

### 2.3 Almacén (kardex, recepciones, despachos, ajustes)

**Aciertos**
- El kardex guarda cada movimiento con `stock_resultante`, el tipo, el documento de origen y el usuario.
- Las reversiones son simétricas y no se repiten.
- El ajuste por conteo guarda la `cantidad_contada` y pide autorización.
- El costo es **promedio ponderado**, documentado en `RecepcionCompraBOImpl.actualizarCostoPromedio`.

**Oportunidades de mejora**
1. **El kardex no está valorizado.** `movimiento_inventario` no guarda el costo unitario ni el costo total, y `producto.costo_unitario` se sobrescribe en cada recepción. No se puede reconstruir el valor del inventario a una fecha, ni explicar por qué una venta del mes pasado quedó "bajo el costo".
2. **Revertir una recepción no revierte el costo promedio.** `revertirRecepcionCompra` saca el stock, pero el costo que esa compra cambió se queda como está. Si la compra se registró con un precio equivocado y se revierte, el costo queda contaminado.
3. **Dos fuentes del stock:** `producto.stock_actual` y el último `stock_resultante`. Cada BO actualiza los dos a mano (`stockDAO.actualizarStock` y `movimientoDAO.save`). Si alguien olvida uno, se descuadran en silencio.
4. **Búsquedas recorriendo la tabla entera.** `DespachoBOImpl.anular` hace `movimientoDAO.listAll()` y filtra en Java (línea 299), y lo mismo pasa con `detalleCompraDAO.listAll()` en recepciones y con `CompensacionCaja.buscar`. No es un tema de rendimiento solamente: el diseño no tiene la consulta "movimientos de este documento".
5. El despacho anulado no guarda su motivo: el motivo solo queda en los movimientos de reversión.

**Propuesta**
- Agregar `costo_unitario` y `costo_total` a `movimiento_inventario`. Cada ingreso guarda el costo con el que entró y cada salida el promedio vigente. El costo del producto se vuelve un dato derivado del kardex, y revertir una recepción recalcula el promedio.
- Un solo punto de entrada para mover el stock, por ejemplo `Inventario.registrarMovimiento(producto, tipo, cantidad, origen)`, que actualice el stock y escriba el kardex en una sola llamada. Hoy esa pareja de llamadas se repite en 8 lugares.
- Consultas por documento: `listarPorDespacho`, `listarPorRecepcion`, `buscarPorDocumento`. Esta última ya existe en caja y no se usa en `CompensacionCaja`.

### 2.4 Compras

**Aciertos**
- Los estados `REGISTRADA → RECIBIDA_PARCIAL → RECIBIDA` se derivan de lo recibido por línea.
- No se anula una compra con recepciones.

**Oportunidades de mejora**
- **Dos convenciones de IGV en el mismo sistema:**
  - En las ventas, el precio incluye el IGV y se desglosa con `/1.18` (`PreciosIgv`).
  - En las compras, el IGV se suma con `*0.18`, y está escrito **dos veces**: `CalculoCompra.TASA` y `ROUND(p_sub_total * 0.18, 2)` en `03_SP_daniel.sql`.

  La diferencia entre ventas y compras tiene sentido (el proveedor cotiza sin IGV), pero la tasa vive en tres lugares.
- Al editar una compra, `CompraBOImpl` borra las líneas quitadas con `DELETE FROM detalle_compra`. Mientras la compra no tenga recepciones es aceptable, pero conviene que sea explícito.

**Propuesta**
- Una sola clase `Impuesto` (o una tabla `parametro` con `IGV_TASA` y fecha de vigencia) que usen ventas y compras, con `conIgv()` y `desglosarIgv()`. Quitar el cálculo del procedimiento almacenado.

### 2.5 Caja

**Aciertos**
- `CompensacionCaja`: un cobro que deja de valer se compensa en la misma caja y no se borra.
- No se puede devolver más efectivo del que hay en la caja.
- El cierre compara lo calculado con lo declarado y guarda la diferencia.

**Oportunidades de mejora**
1. **Los movimientos manuales se editan y se borran físicamente** (`MovimientoCajaBOImpl.modificar` y `eliminar` → `DELETE FROM movimiento_caja`). Es lo único de dinero que se reescribe, y contradice el propio principio de `CompensacionCaja`.
2. **Una referencia genérica como texto.** `movimiento_caja.documento_origen` (VARCHAR: `'VENTA'`, `'COBRO'`, `'NOTA_CREDITO'`, `'ANULACION_…'`) e `id_documento_origen` no tienen clave foránea. La base no garantiza que el cobro 15 exista, y los nombres son constantes repetidas en dos clases (`CompensacionCaja` y `CobroBOImpl`).
3. **El estado de la caja vive en dos lugares:** `caja.abierta` (booleano) y `cierre_caja.estado`. Una caja abierta con un cierre VIGENTE es una combinación posible e inválida.
4. **El tipo no dice qué compensa.** Una compensación siempre es `EGRESO_MANUAL` o `INGRESO_MANUAL`, así que en el reporte de caja, la devolución de una nota de crédito y la compra de útiles de oficina tienen el mismo tipo.

**Propuesta**
- Los movimientos manuales también se anulan con un movimiento contrario (`id_movimiento_revertido`, como en el kardex). Sin `UPDATE` ni `DELETE` sobre dinero.
- Reemplazar la referencia genérica por claves foráneas opcionales (`id_cobro`, `id_venta`, `id_comprobante`) con un CHECK de que solo una está llena. O, como mínimo, un ENUM en vez del VARCHAR.
- Calcular el estado de la caja: está abierta si no tiene un cierre VIGENTE. Así se elimina el booleano.
- Agregar los tipos `DEVOLUCION_NOTA_CREDITO`, `ANULACION_COBRO` y `ANULACION_VENTA` para que el reporte diga qué pasó.

### 2.6 Cobranza (cuentas por cobrar y cobros)

**Aciertos**
- Hay una cuenta por cobrar por venta (`UNIQUE(id_venta)`).
- El cobro anulado se conserva y su ingreso en caja se compensa.
- La nota de crédito sobre una venta al crédito ya pagada devuelve el excedente por caja.

**Oportunidades de mejora**
1. **El monto original se sobrescribe.** La nota de crédito hace `monto_original = monto_original - nota` (`sp_comprobante_revertir_saldo_cpc`), y anularla lo vuelve a sumar. Después de dos notas y una anulación ya no se sabe cuál fue la deuda original de la venta. El dato de ejemplo lo muestra: la venta de 1534.00 tiene una cuenta con `monto_original = 1427.80`.
2. **Los saldos se guardan en vez de calcularse.** `monto_pagado` y `saldo_pendiente` se guardan y se mantienen a mano desde tres BO (`CobroBOImpl`, `VentaBOImpl` y `ComprobanteBOImpl`) y dos procedimientos.
3. **La regla del estado está escrita dos veces:** `CuentaPorCobrarBOImpl.calcularEstado` en Java y el `CASE` de `sp_comprobante_revertir_saldo_cpc` / `restaurar` en SQL. Si cambia, hay que acordarse de los dos.
4. **El estado se guarda aunque nadie lo lee.** Las consultas (`sp_cuenta_por_cobrar_*`) ya calculan el estado al leer, con la fecha de hoy, así que la pantalla muestra bien `VENCIDA`. Pero la columna `estado` se sigue escribiendo y puede quedar desactualizada en la tabla. Corregido respecto a la primera versión de este documento, que decía que la pantalla podía mostrar `PENDIENTE` después de vencer.

**Propuesta**
- Modelar la cuenta como un **libro de movimientos**: `movimiento_cuenta (cargo | abono | nota_credito | reversion, monto, documento)`. Se elimina la `cuenta_por_cobrar.monto_original` modificable, y:
  - el monto original es el cargo inicial y no cambia;
  - el saldo es la suma de los movimientos;
  - el estado se calcula al leer, con la fecha de hoy.
- Si no se quiere la tabla nueva, al menos agregar `monto_acreditado` (la suma de las notas de crédito) y dejar `monto_original` fijo.

### 2.7 Autorizaciones (RF014)

**Aciertos:** la huella de la operación, el uso único, la vigencia en minutos y la búsqueda de una solicitud pendiente igual antes de crear otra. Es el módulo mejor pensado del sistema.

**Oportunidades de mejora**
- `operacion_restringida` es un VARCHAR libre. Las operaciones son constantes en `SolicitudAutorizacionBOImpl` y otros BO las vuelven a declarar (`ComprobanteBOImpl.OPERACION_NOTA_CREDITO`).
- Qué operación necesita autorización está fijo en cada BO; no se puede configurar por rol ni por monto (por ejemplo, "un descuento bajo el costo de menos de S/ 20 no requiere autorización").

**Propuesta**
- Un enum `OperacionRestringida` en el dominio, y una tabla `politica_autorizacion (operacion, rol, monto_minimo)` si el negocio necesita umbrales.

### 2.8 Clientes y crédito

**Aciertos**
- La identidad se valida en la base con un CHECK: una persona jurídica requiere razón social y una persona natural, nombres y apellidos.
- El límite de crédito se verifica contra la deuda total del cliente.

**Oportunidad de mejora:** `condicion_pago` (CONTADO o CREDITO) y `limite_credito > 0` dicen casi lo mismo. Un cliente en CONTADO con límite de 5000 es una combinación posible y ambigua.

**Propuesta:** que "tiene crédito" sea `limite_credito > 0 AND plazo_credito_dias > 0`, y que `condicion_pago` del cliente sea solo la opción por defecto de la venta.

---

## 3. Problemas de arquitectura que cruzan los módulos

### 3.1 Modelo anémico
Las clases de `sigmeta-dominio` son POJO: getters, setters y nada más. Todas las reglas (calcular totales, validar transiciones, derivar estados) están en los `*BOImpl`, y algunas se repiten en procedimientos almacenados. El resultado:
- `VentaBOImpl.calcularTotales` y `CotizacionBOImpl.calcularTotales` son dos copias del mismo cálculo.
- Una `Venta` en memoria puede estar en cualquier estado; nada impide `venta.setEstado(DESPACHADA)` desde cualquier lugar.

**Propuesta, gradual:** pasar al dominio lo que no toca la base:
- `DocumentoComercial.recalcularTotales()`
- `LineaDocumento.importe()`
- `CuentaPorCobrar.estado(hoy)`
- `Cotizacion.enviar()`, `aceptar()` y `rechazar()`, que lanzan un error si la transición no vale

Los BO quedan como coordinadores: cargan, llaman al dominio y guardan.

### 3.2 Reglas repetidas entre Java y SQL
Ejemplos:
- `sp_venta_eliminar` vuelve a validar los despachos que `VentaBOImpl.anular` ya validó.
- El estado de la cuenta por cobrar está en Java y en SQL.
- El IGV de compras está en Java y en SQL.

Cuando la regla cambia, uno de los dos queda viejo. **Criterio sugerido:** los procedimientos almacenados solo hacen persistencia y restricciones de integridad (UNIQUE, FK y CHECK); las reglas de negocio viven en un solo lugar, el BO o el dominio.

### 3.3 Efectos secundarios cableados a mano
Emitir una nota de crédito llama directamente a stock, kardex, cuenta por cobrar, caja y autorización. Anular una venta llama a caja y cobranza. Cada BO crea sus DAO y otros BO con `new` (`VentaBOImpl` tiene 14 dependencias), y aparecen nombres completos como `new pe.edu.pucp.sigmeta.daoimpl.cobranza.CuentaPorCobrarDAOImpl()` en medio de la lógica.

Agregar un efecto más (por ejemplo, un asiento contable o un correo al cliente) obliga a tocar cada operación.

**Propuesta, sin frameworks:**
- Inyectar las dependencias por el constructor, lo que también facilita las pruebas.
- Para los efectos que cruzan módulos, un despachador de **eventos de dominio síncronos dentro de la misma transacción** (`NotaCreditoEmitida`, `VentaAnulada`, `CobroAnulado`). Inventario, cobranza y caja se suscriben. `Transaccion` ya garantiza que todo hace commit o rollback junto, así que el comportamiento no cambia; solo cambia quién conoce a quién.

### 3.4 Dinero en `double`
`DocumentoComercial`, `LineaDocumento`, `CuentaPorCobrar` y `MovimientoCaja` guardan los montos como `double`. Los BO convierten a `BigDecimal` para calcular y vuelven a `double` para guardar, con tolerancias como `+ 0.005` o `+ 0.0001` repartidas por el código.

**Propuesta:** `BigDecimal` en el dominio, o un tipo `Dinero` con escala 2 y redondeo HALF_UP definido una vez. Así se eliminan las tolerancias.

### 3.5 Bloqueo global de escrituras
`Transaccion` toma un bloqueo sobre una fila única (`sp_transaccion_bloquear`), así que todas las escrituras del sistema se ponen en fila. Es una decisión consciente y documentada que evita condiciones de carrera con pocos usuarios. Conviene dejar escrito que es la primera pieza que habría que cambiar si el sistema crece, pasando a bloquear por agregado (`SELECT … FOR UPDATE` sobre la venta, el producto o la caja involucrados).

---

## 4. Extensibilidad: lo que está fijo en el código

| Regla | Dónde | Sugerencia |
|---|---|---|
| IGV 18 % | `PreciosIgv.FACTOR`, `CalculoCompra.TASA`, `03_SP_daniel.sql` | Un parámetro con fecha de vigencia |
| Series F001, B001, FC01, BC01, T001 | `ComprobanteBOImpl` y `DespachoBOImpl` | Tabla `serie_documento` |
| Prefijos VTA-, OCC-, COT- y OC- | Cuatro BO | La misma tabla, con un formato por tipo |
| 7 días de atraso máximo en una venta | `VentaBOImpl.DIAS_ATRASO_MAXIMO` | Un parámetro |
| Medios de envío CORREO, MENSAJERIA e IMPRESO | Cadenas en `ComprobanteBOImpl.registrarEnvio` | Un enum, como `MedioPago` |
| Operaciones que requieren autorización | Cada BO | Tabla de políticas (ver 2.7) |
| Moneda | No existe: todo es S/ | Es una decisión documentada (`LOGICA_NEGOCIO.md`: una sola moneda). Si algún día se compra en dólares, hay que agregar `moneda` y `tipo_cambio` **por documento**, no por producto. |

**Unidades de medida:** el modelo es correcto para el alcance actual: compra en una unidad, venta en otra, un factor, y el factor congelado en cada compra. Su límite es que un producto tiene **una sola** unidad de venta. Vender el mismo cable por metro y por rollo requeriría una tabla `producto_unidad (unidad, factor)`.

---

## 5. Prioridades sugeridas

De mayor a menor impacto en la integridad de los datos:

1. **Fijar el monto original de la cuenta por cobrar**, con un libro de movimientos o con `monto_acreditado` (2.6).
2. **Foto del comprobante:** que el PDF no lea datos vivos del cliente ni del producto (2.1).
3. **Sin `UPDATE` ni `DELETE` sobre movimientos de caja manuales** (2.5).
4. **Un solo campo de estado por documento**, eliminando el booleano `anulado` duplicado (2.1).
5. **Costo en el kardex** y reversión del costo promedio al revertir una recepción (2.3).
6. **Un solo lugar para el IGV y las series** (2.4 y 4).
7. **Sacar las reglas de los procedimientos almacenados** y empezar a poner comportamiento en el dominio (3.1 y 3.2).
8. **Eventos de dominio e inyección por constructor**, cuando se agregue el próximo efecto secundario (3.3).

Los puntos 1 a 4 cambian datos que hoy se pierden o se pueden contradecir. Los demás mejoran la forma del código sin cambiar lo que el usuario ve.

### Estado

Los puntos 1 a 4 están resueltos en el código. Para que funcionen en una base ya instalada hay que aplicar `sql/3_migraciones/12_auditoria_cxc_comprobante_caja.sql` y `13_anulado_derivado_del_estado.sql`, y después recargar los procedimientos.

1. **Cuenta por cobrar:** `monto_original` ya no cambia. Las notas de crédito suman en la columna nueva `monto_acreditado`, y el saldo es original − acreditado − pagado.
2. **Comprobante:** al emitir se guarda la foto del cliente (en `comprobante`) y de las líneas (tabla nueva `detalle_comprobante`), y el PDF sale de esa foto.
3. **Caja:** los movimientos manuales se anulan registrando su contrario (`ANULACION_MOVIMIENTO`). Del movimiento solo se corrige el concepto, y `sp_movimiento_caja_eliminar` rechaza cualquier borrado.
4. **Un solo estado:** `anulado` es una columna calculada `GENERATED ALWAYS AS (estado = 'ANULADA')` en venta, cotización, compra y orden, y `isAnulado()` se deriva del estado.
