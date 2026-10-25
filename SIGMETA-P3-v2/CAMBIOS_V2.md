# Correcciones v2

## Reglas de negocio corregidas

1. **Importes de compra:** cantidad (3 decimales), precio/descuento (2) determinan el importe; se ignoran importes de entrada. Descuentos mayores al bruto se rechazan. Cálculo compartido entre CompraBO y DetalleCompraBO, con protección adicional en los SP.
2. **IGV:** el alcance actual son compras gravadas al 18 %. Alta y cambios de cabecera/detalle usan el mismo criterio. No representa compras exoneradas/inafectas; para ellas habría que incorporar una clasificación tributaria explícita.
3. **Cuentas manuales:** solo recuperan la cuenta faltante de una venta al crédito vigente. Cliente, importe y fechas deben corresponder a esa venta; se derivan de ella. No son deudas independientes. Una restricción única impide dos cuentas de una venta.
4. **Eliminación de deuda:** BO y SP rechazan borrar una cuenta de venta vigente. Anular venta y cerrar su cuenta ocurre en una transacción; si hay cobros vigentes se revierte todo. Modificar una venta al crédito sin cuenta ahora lanza error, en vez de ignorarlo.
5. **Estado de cobranza:** todas las consultas de CxC derivan el estado vigente desde saldo, pago y fecha. `listarPorEstado` y `listarVencidas` coinciden aunque el estado persistido sea antiguo. Se incorporan `getSituacionPago()` e `isVencida()` para representar pago parcial y vencimiento simultáneamente.
6. **Plazo:** el plazo del cliente es máximo autorizado y valor predeterminado. Una nueva venta no puede excederlo. Cambiarlo no altera ventas anteriores. Modificar una venta conserva las refinanciaciones de su cuenta y no permite cambiar la fecha original de emisión de la venta al crédito.
7. **Inventario:** `unidadVenta` es la unidad base estable en la que se mide stock. BO y SP impiden cambiarla incluso con stock cero, para no reinterpretar históricos. `getUnidadInventario()` hace explícito ese significado. Cambios de presentación requieren otro producto; unidad de compra y factor siguen siendo configuración de compra, y los detalles de compra conservan su factor histórico.
8. **Crédito vencido:** la suma de deuda para validar límites incluye cualquier saldo pendiente, también las cuentas VENCIDAS.

## Diseño de Cliente

- `nombreContacto` reside en `DatosContacto`, compartido por cliente y proveedor. No se almacena un segundo campo en las clases. Se conservan accesores antiguos delegados y marcados Deprecated, para facilitar transición.
- El booleano se llama `activo`; hay `isActivo/setActivo`. `isEstado/setEstado` conservan compatibilidad con el resto de la API y la columna SQL `estado`.
- `limiteCredito` y su acceso JDBC/comparaciones de crédito usan BigDecimal; la suma SQL de deuda también llega como BigDecimal. `asignarLineaCredito` tiene una API decimal y un adaptador double para llamadas antiguas.
- Calificación actual con escala interna validada A/B/C/SIN_EVALUAR (riesgo bajo/medio/alto/sin evaluación). No es una calificación oficial ni un cálculo automático. `getCalificacion()` expone el enum; los accesores String existentes validan y convierten. SIN LINEA legado se normaliza a SIN_EVALUAR.
- Se eliminan las listas vacías de ventas/CxC de Cliente. Las relaciones se consultan bajo demanda con `VentaBO.listarPorCliente` y `CuentaPorCobrarBO.listarPorCliente`, mediante SP.
- Se conserva `tipoPersona` de v1. Razón social o nombres/apellidos se validan según la naturaleza, no según tener RUC.
- Condición, límite y plazo del cliente autorizan futuras operaciones; las ventas conservan las condiciones pactadas.

## Alcance y compatibilidad

- No se añadió soporte para varios documentos o varios contactos por cliente: se conserva un documento y un contacto principal, que era el alcance del proyecto.
- No se creó historial de calificaciones ni una clase PerfilCrediticio: no son necesarios para almacenar solo la evaluación/configuración actual.
- La conversión a BigDecimal de v2 cubre el límite, la suma de deuda y los cálculos indicados. No se afirma haber cambiado todos los DTO monetarios heredados: varios mantienen double por compatibilidad; los cálculos de compras normalizan a las escalas DECIMAL antes de guardar.
- No hay interfaz HTTP. Sigue vigente el contrato de SesionServicio y BO internos documentado en README.
- Se mantiene el bloqueo transaccional común de v1: correcto para estos escenarios, pero limita el paralelismo.
- `Cliente.getLimiteCredito()` ahora retorna BigDecimal y se retiraron sus listas: adapta cualquier código externo al ZIP que use esas firmas.

## Archivos y base de datos

Todos los SELECT/INSERT/UPDATE/DELETE del código Java continúan en procedimientos almacenados.
`sigmeta.sql`, los scripts separados, SP, DAO, BO, modelos y pruebas se actualizaron conjuntamente.

Instalación nueva: `instalar_nueva_bd.sql` (borra/recrea sigmeta).
Desde v1: `sql/migracion_v1_v2.sql` y luego recargar todos los procedimientos. El script detiene la migración si hay duplicados de cuentas, calificaciones desconocidas o cuentas de otro cliente/venta al contado. No corrige históricos económicos de forma arbitraria. El DDL de MySQL realiza commits implícitos: hacer copia y detener escrituras durante la migración.
