# RF013 — Corrección de reversión de notas de crédito (2026-11-22)

## Diagnóstico
Antes de esta modificación la anulación de una nota de crédito registraba
AJUSTE_SALIDA y no vinculaba la salida al INGRESO_DEVOLUCION original.
Kardex confundía una reversión por anulación con una merma de almacén.

## Implementado en main
- Tipo de movimiento SALIDA_REVERSION_DEVOLUCION en Java y esquema nuevo.
- Procedimiento sp_movimiento_listar_comprobante para recuperar movimientos de la nota.
- Procedimiento sp_movimiento_reversion_nota_credito para generar movimiento
  compensatorio con id_comprobante e id_movimiento_revertido. La UNIQUE del
  movimiento original impide dos compensaciones del mismo ingreso.
- ComprobanteBOImpl valida el historial, las cantidades por producto y stock
  antes de actualizar. Actualiza inventario y registra la reversión en una misma
  transacción, con rollback en caso de fallo. Mantiene lógica existente de
  compensación de cuenta por cobrar y caja.
- Kardex Blazor muestra la etiqueta Reversión de nota de crédito.
- NotaCreditoBOTest comprueba el vínculo entre ingreso y reversión.

## IMPORTANTE: actualización de entorno
1. Respaldar MySQL. No ejecutar migraciones en la base compartida sin respaldo.
2. En una base de pruebas aplicar sql/3_migraciones/15_reversion_nota_credito_trazable.sql.
3. Compilar y desplegar el backend Java con los cambios de main.
4. Ejecutar pruebas de integración solo sobre base local/de pruebas:
   mvn -pl sigmeta-ejecucion -am test -Dsigmeta.integracion=true
5. Comprobar que Blazor recupera los movimientos desde /api/almacen/kardex.

## Casos de aceptación
- Registrar venta, emitir comprobante, despachar y solicitar aprobación RF014.
- Crear nota de crédito parcial por cantidad entregada.
- Verificar ingreso de devolución en Kardex y saldo/compensación caja.
- Anular la nota: salida SALIDA_REVERSION_DEVOLUCION con
  id_movimiento_revertido apuntando al INGRESO_DEVOLUCION.
- Confirmar que el stock regresa al nivel previo a la nota y que se restaura
  el saldo/compensa el efectivo de acuerdo con la condición de pago.
- Intentar una segunda anulación (rechazo).
- Intentar anular sin stock suficiente: ningún cambio en inventario,
  nota, deuda ni caja (rollback).
- Una nota con varias líneas del mismo producto debe revertir todos los
  movimientos sin duplicarlos.
- Revisar que los ajustes físicos sigan mostrados solo como AJUSTE_SALIDA.

## Límites
No se realizó compilación Java/.NET ni se ejecutó MySQL en esta sesión.
La rama auditoria-diseno contiene cambios independientes y no se fusionó;
antes de entregar una sola versión hay que integrar y probar las migraciones
y cambios del compañero. Las notas históricas anuladas antes de esta corrección
conservarán sus antiguos AJUSTE_SALIDA; no se reescriben de forma automática.
