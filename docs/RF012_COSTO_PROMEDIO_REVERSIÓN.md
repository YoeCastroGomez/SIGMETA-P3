# RF012 — Reversión segura del costo promedio (2026-11-22)

## Diagnóstico
Antes, RecepcionCompraBOImpl recalculaba costo_unitario en cada INGRESO_COMPRA
pero MovimientoInventarioBOImpl.revertirRecepcionCompra solo descontaba stock.
Revertir dejaba el promedio del ingreso anulado. El promedio de una recepción no
se puede invertir exactamente a partir del promedio redondeado posterior.

## Solución implementada en main
- Nueva tabla costo_recepcion_historico ligada por FK/PK al movimiento INGRESO_COMPRA;
  guarda costo_anterior y costo_posterior (DECIMAL(12,2)).
- Se guardan ambos costos inmediatamente después de registrar el movimiento,
  dentro de Transaccion.ejecutar; cualquier error revierte recepción, stock y costo.
- La reversión procesa ingresos del mismo producto en orden inverso; verifica
  que no haya movimientos posteriores al último ingreso, que el stock coincida
  con el histórico y que el costo actual sea el costo posterior registrado.
  Restaura costo anterior y registra SALIDA_REVERSION_COMPRA en la misma transacción.
- Recepciones anteriores sin snapshot NO se pueden revertir automáticamente.
  Requieren conciliación manual de valorización histórica (no inventar costos).

## Instalación requerida (no se ha ejecutado en su base)
1. Respaldar la BD y probar primero en una copia de MySQL.
2. Si se adopta auditoria-diseno, integrar previamente sus cambios de
   estructura/migraciones 12 y 13. No mezclar procedimientos de versiones distintas.
3. Ejecutar sql/3_migraciones/14_costo_historico_recepcion.sql.
4. Desplegar nuevamente el WAR Java desde main o la rama que incluya esta corrección.
   No desplegar el Java nuevo antes del SQL: registrar recepciones fallaría y se
   revertiría la transacción.
5. Desde SIGMETA-P3-v2, ejecutar mvn -pl sigmeta-ejecucion -am test.
   Verificar CostoPromedioRecepcionTest y las pruebas de integración en base de pruebas.

## Escenarios de aceptación
- Stock 10 / costo 20; ingresar 10 a 30 -> stock 20 / costo 25;
  revertir inmediatamente -> stock 10 / costo 20.
- Ingreso con stock 0 -> al revertir devuelve el costo anterior registrado.
- Recepción de varias líneas del mismo producto -> restauración en orden inverso.
- Compra parcialmente recibida -> recuperación de cantidades y estado de compra.
- Movimiento posterior (despacho, ajuste, otra recepción, nota de crédito):
  rechazar reversión automáticamente y mantener TODOS los valores sin cambios.
- Recepción antigua sin snapshot: rechazar con mensaje explícito, sin cambios.
- Reversión duplicada: rechazar sin cambios.
- Costo actual alterado fuera del historial: rechazar sin cambios.

## Límites
La solución es conservadora: no recalcula un kardex valorizado histórico completo.
Tampoco rellena artificialmente costos anteriores para ingresos viejos. El costeo
histórico tras movimientos posteriores requiere diseñar valorización por evento
y reconciliar salidas, ajustes, notas de crédito y documentos dependientes.
No se han compilado proyectos ni ejecutado procedimientos de MySQL en esta sesión.
