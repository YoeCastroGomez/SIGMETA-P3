# Actualización de una base existente

Este paquete incluye un instalador para una base nueva de pruebas, no una migración automática de datos reales.

Para conservar una base existente:

1. Haz una copia y trabaja primero sobre una restauración de prueba. Detén las escrituras durante el cambio.
2. Agrega a `cliente` la columna `tipo_persona` permitiendo NULL temporalmente.
3. Clasifica cada registro como NATURAL o JURIDICA usando sus datos reales. RUC por sí solo no decide la clasificación. Completa nombres/apellidos o razón social cuando corresponda.
4. Sustituye `ck_cliente_identidad` por la definición incluida en el nuevo `sql/01_maestros.sql`; después exige `tipo_persona NOT NULL`.
5. Revisa duplicados de `venta.numero` antes de imponer unicidad. No renumeres documentos ya usados sin revisar sus referencias externas.
6. Aplica `sql/06_integridad.sql` una sola vez, después de resolver duplicados. Crea el control transaccional, los contadores y el índice único. Inicializa el contador con los números existentes que cumplen VTA-AAAA-NNNN.
7. Revisa órdenes cuyas ventas vigentes ya excedan lo pedido y cuentas cuyos saldos no coincidan con sus cobros. Las nuevas validaciones evitan nuevos casos, pero no reparan históricos de forma arbitraria.
8. Recarga todos los procedimientos mediante `00_drop_sps.sql`, los cinco archivos `SP_nombre.sql` y `SP_integridad.sql`. Los scripts `sql_prueba_*.sql` insertan ejemplos: no forman parte de la migración.
9. Despliega el Java actualizado junto con los nuevos procedimientos: cambió la firma de insertar/modificar cliente.
10. Ejecuta pruebas sobre la copia restaurada antes de reanudar operaciones.

No ejecutes `sql/sigmeta.sql` ni `00_base.sql` sobre la base que quieres preservar: ambos recrean la base.

## Desde la primera versión corregida

Usa `sql/migracion_v1_v2.sql` en una copia, con escrituras detenidas; después recarga los seis archivos SP mediante el drop de procedimientos. Este script no borra la base. Consulta CAMBIOS_V2.md.
