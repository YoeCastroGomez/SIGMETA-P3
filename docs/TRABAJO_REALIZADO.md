# SIGMETA: trabajo realizado por integrante (Lab 12)

Cada integrante responde por 3 RF del catálogo oficial (RF001 a RF015): los completó, los revisó y los expone. Aquí está lo que se hizo en cada RF y cómo demostrarlo en la sustentación.

| Integrante | RF |
|---|---|
| Yoe | RF001, RF002, RF003 + trabajo transversal |
| Claudia | RF005, RF006, RF007 |
| Luis Daniel | RF004, RF011, RF012 |
| Sebastián | RF008, RF013, RF014 |
| Adriano | RF009, RF010, RF015 |

Usuarios demo (clave de todos: `Meta2026`): Administrador `aquispe` · Vendedor `lrojas`, `cflores` · Cajero `mchavez`, `rsanchez` · Almacenero `jtorres`.

---

## Yoe

### RF001 Autenticación y control de acceso
- Las 42 pantallas exigen el rol que les corresponde. Si un usuario entra por URL a una pantalla de otro rol, ve "No tienes permiso…".
- El servidor Java también verifica el rol en cada operación (`UsuarioBO.verificarRol`). Aunque se llame al REST directamente, rechaza lo que no corresponde al rol.
- La consulta de clientes quedó restringida a Vendedor, Cajero y Administrador.
- El inicio de sesión ya no se cae si el envío del formulario se corta, y no se puede enviar dos veces.

**Demostración:** entrar como `lrojas` (Vendedor) y escribir `/usuarios` en la barra: aparece el aviso de sin permiso.

### RF002 Usuarios y roles
- Registro, edición, desactivación y restablecimiento de clave de usuarios. La respuesta del servidor no expone el hash de la clave.
- El Administrador no puede desactivarse a sí mismo: no ve el botón en su propia fila y el Java lo rechaza.
- Pantalla "Mi clave" para que cada usuario cambie su contraseña.

**Demostración:** entrar como `aquispe`; en su fila no aparece "Desactivar".

### RF003 Clientes
- Registro, edición, desactivación y consulta.
- Filtros por documento, razón social y estado, por separado o combinados.
- Confirmación antes de desactivar.
- Reactivación de clientes desactivados (`sp_cliente_activar`).
- Línea de crédito del cliente (asignar y quitar).

**Demostración:** desactivar un cliente y luego activarlo con "Activar".

### Trabajo transversal (todo el sistema)
- **Mensajes en ventanas modales:** toda acción muestra una ventana de éxito ("Se registró el cliente…") o de error (el mensaje de la regla del BO). Componentes `MensajeModal`, `AvisoExito`, `AvisoError` y `SiguientePaso`.
- **Estándar de interfaz:** mismas búsquedas (campo, estado, Buscar, Limpiar), botones y textos en todas las pantallas, sin textos de guía.
- **Errores en vivo debajo de cada campo** en todos los formularios (RNF004).
- **Scripts SQL ordenados** en carpetas (base, procedimientos, migraciones, datos demo) y migraciones de actualización (08) para la base compartida en AWS.
- Integración del trabajo de los demás integrantes, README y `.gitignore`.

---

## Claudia

### RF005 Productos
- Registro, edición, desactivación y consulta de productos con sus tres códigos.
- Código interno generado automáticamente y no editable.
- Reactivación de productos desactivados (`sp_producto_activar`).
- La imagen del producto quedó **fuera del alcance** por decisión del grupo; se declara en la sustentación.

### RF006 Categorías
- Registro, edición, desactivación y consulta.
- No se puede desactivar una categoría con productos activos (regla en el BO).

### RF007 Cotizaciones y órdenes de clientes
- Cotización con líneas, subtotal, IGV y total, con vigencia.
- **Número de cotización único:** correlativo automático no editable y restricción `UNIQUE` en la base (migración 07), para que dos vendedores a la vez no obtengan el mismo número.
- No se emite una cotización con fecha pasada; las vencidas se marcan antes de listarlas.
- Estados: Registrada → Enviada → Aceptada / Rechazada / Vencida.
- La orden de cliente se genera **solo desde una cotización aceptada**, con número interno generado por el sistema. Su estado de atención avanza con los despachos.
- Tests JUnit: `CatalogoBOTest` (8 pruebas) y `CotizacionBOTest` (10 pruebas).

**Demostración:** como `lrojas`, crear una cotización, enviarla, aceptarla y pulsar "Generar orden".

---

## Luis Daniel

### RF004 Proveedores
- Registro, edición, desactivación y consulta.
- Búsqueda combinada (RUC, razón social y estado) en el BO.
- Confirmación en ventana antes de desactivar.
- Reactivación de proveedores desactivados (`sp_proveedor_activar`).

### RF011 Compras a proveedores
- Registro, edición y anulación de compras con su detalle.
- Correlativo de compra generado en el BO y mostrado bloqueado en la pantalla; no se repite el número de orden.
- Filtros opcionales por proveedor y por rango de fechas.
- Validación de fechas de recepción en el BO y de precisión de descuentos en la pantalla.
- Confirmación en ventana antes de anular.

### RF012 Movimientos de almacén
- Recepción de compras, despachos, reversión (solo una vez por movimiento) y ajuste por conteo con autorización.
- Guía de despacho automática y no editable.
- Kardex por producto y alerta de stock mínimo.

### Aporte a la interfaz
- Panel de inicio con datos reales y diferenciado por rol.
- Ventana de confirmación compartida que cubre toda la pantalla.
- Enlaces de "Volver" sin duplicados en todos los formularios.
- Datos comerciales en mayúsculas.

---

## Sebastián

### RF008 Ventas y comprobantes
- Venta desde una orden, con control de stock disponible y del precio bajo el costo.
- Serie y número del comprobante mostrados como vista previa, no editables.
- **Comprobante (factura o boleta) en PDF** con JasperReports, en descarga directa. Al descargarlo se registra el envío como IMPRESO.

### RF013 Notas de crédito
- Registro, consulta y anulación de notas de crédito sobre un comprobante.
- Requiere una autorización vigente del Administrador (RF014).

### RF014 Autorizaciones
- El Cajero o el Almacenero solicitan una autorización (nota de crédito o ajuste de inventario) con un motivo.
- El Administrador la aprueba con una vigencia en minutos, o la rechaza.
- La operación solo se permite mientras la autorización está vigente.

**Demostración:** como `mchavez` pedir una autorización; como `aquispe` aprobarla; volver como `mchavez` y emitir la nota de crédito.

---

## Adriano

### RF009 Cobros y cuentas por cobrar
- Cuentas por cobrar de las ventas al crédito, con su saldo.
- Registro, edición y anulación de cobros; el monto anulado vuelve al saldo.
- Cobro al contado desde la caja.
- Búsquedas de cobranza filtradas en la base con procedimientos almacenados e índices (H10).

### RF010 Caja y cierre diario
- Apertura de caja con monto inicial, movimientos de ingreso y egreso.
- Cierre diario con el monto calculado frente al declarado; excluye ventas anuladas y notas de crédito.
- Solo el Administrador reabre una caja cerrada.
- Tests JUnit de caja y de cobros (H14).

### RF015 Reportes
- Tres reportes: comercial, de abastecimiento y de caja, con sus secciones.
- Cada reporte se descarga en **PDF con JasperReports 7**, en estilo corporativo. Las plantillas se compilan al construir el proyecto y el botón de descarga está debajo de cada reporte.

---

## Alcance declarado (se explica en la sustentación)
- Imagen del producto (RF005): fuera del alcance.
- Sin integración con SUNAT: el comprobante se genera en el sistema y se descarga en PDF.
- El envío por correo y mensajería se registra (medio y fecha), pero no se envía de verdad.
