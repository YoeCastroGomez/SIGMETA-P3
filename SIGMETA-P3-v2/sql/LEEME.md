# Scripts SQL de SIGMETA

Todos los scripts de la base `sigmeta`, ordenados por para qué sirven. Las carpetas van numeradas en el orden en que normalmente se usan.

```
sql/
├── 1_base/                  Crea la base desde cero (tablas + datos minimos)
│   ├── sigmeta.sql          script integrado: el que se usa normalmente
│   └── por_modulo/          el mismo contenido partido por integrante (00 a 06)
├── 2_procedimientos/        Procedimientos almacenados (SP) de cada integrante
├── 3_migraciones/           Cambios para una base que YA existe (por ejemplo la de AWS)
├── 4_datos_demo/            Datos de demostracion: tres meses de operacion simulada
└── 5_pruebas/               Consultas para probar a mano los SP de cada integrante
```

## ¿Qué ejecuto?

### Caso 1: base nueva (desde cero)
**Ojo: `sigmeta.sql` borra la base `sigmeta` y la crea de nuevo.**

1. `1_base/sigmeta.sql`
2. `2_procedimientos/`, en orden de número: `00_drop_sps.sql`, `01_SP_yoe.sql`, `02_SP_claudia.sql`,
   `03_SP_daniel.sql`, `04_SP_sebastian.sql`, `05_SP_adriano.sql`, `06_SP_integridad.sql` y, siempre al final, `07_SP_reforma.sql`.
   Antes de `01` y `02`, seleccionar `sigmeta` como schema por defecto: esos dos archivos no tienen `USE sigmeta;`.
3. Opcional: `4_datos_demo/datos_demo.sql` para tener datos realistas en todas las pantallas.

En vez de `sigmeta.sql` se pueden correr los de `1_base/por_modulo/` (00 a 06) en orden. **Una opción o la otra, no las dos.**

No hace falta ejecutar `3_migraciones/`: `sigmeta.sql` ya trae todos esos cambios.

En MySQL 8, quitar antes `ONLY_FULL_GROUP_BY` del `sql_mode` (ver el `README.md` del proyecto): la última sentencia de `sigmeta.sql` falla con ese modo.

### Caso 2: base que ya existe (por ejemplo la compartida de AWS)
**No reinstalar.**

1. Ejecutar solo las migraciones de `3_migraciones/` que todavía no se aplicaron, en orden de número.
2. Volver a cargar todos los `2_procedimientos/` en orden (de `00` a `07`).

| Migración | Qué cambia |
|---|---|
| `01_migracion_reversion_despacho.sql` | Permite revertir despachos en el kardex |
| `02_migracion_reversion_recepciones_ajustes.sql` | Permite revertir recepciones y ajustes de inventario |
| `03_migracion_v1_v2.sql` | Ordena calificaciones de crédito y deja una sola cuenta por cobrar por venta |
| `04_migracion_lab9_sebastian.sql` | Agrega el administrador que autoriza la nota de crédito |
| `05_migracion_nombres_catalogo.sql` | Junta las categorías repetidas y deja los códigos en MAYÚSCULAS (también pasaba los nombres a MAYÚSCULAS sin tildes; eso lo corrige la 06) |
| `06_migracion_textos_legibles.sql` | Nombres y descripciones de categorías y productos en mayúscula normal y con tildes ("Látex y esmaltes"); tildes de proveedores, clientes y usuarios de los datos demo. Va **después** de la 05 |
| `07_migracion_unicidad_cotizacion.sql` | Agrega `UNIQUE(numero)` en cotizaciones (H09). Re-ejecutable: si hay números repetidos, los muestra y no cambia nada |

Nota H09: `04_SP_sebastian.sql` termina con un bloque que agrega, si todavía no están, `UNIQUE(serie, numero)` en comprobantes, `UNIQUE(numero)` en órdenes de compra de clientes y el correlativo `OCC`. No hace falta una migración aparte: basta con recargar los procedimientos como siempre.

### Caso 3: limpiar y cargar datos de demostración
`4_datos_demo/datos_demo.sql` **borra todos los datos** (no las tablas ni los procedimientos) y carga datos coherentes del 2026-08-03 al 2026-11-10: compras, ventas, caja, cobranza y kardex que cuadran entre sí.
La clave de todos los usuarios es `Meta2026`.
Los nombres ya vienen en mayúscula normal y con tildes: el archivo está en UTF-8 (trae `SET NAMES utf8mb4`).
**La base de AWS es compartida: avisar al grupo y sacar un respaldo antes.**

### Caso 4: probar un procedimiento a mano
Abrir el archivo del integrante en `5_pruebas/` y ejecutar las consultas que se necesiten.

## Criterio de textos

Los nombres se **guardan como los escribe el usuario** (sin espacios de más) y se **comparan con una clave** en mayúsculas y sin tildes (`TextoNormalizado.clave` en los BO), que nunca se guarda. Solo los códigos de producto se guardan en MAYÚSCULAS y sin espacios. Los scripts de datos siguen ese criterio.

## `07_SP_reforma.sql`

Procedimientos de las pantallas de almacén (RF012), notas de crédito (RF013) y reportes (RF015), y de las reglas de stock comprometido, costo promedio y reactivación de categorías. **Sin este archivo esas pantallas responden error**, y registrar una venta o una recepción también, porque usan `sp_stock_comprometido` y `sp_producto_actualizar_costo`.

Es re-ejecutable (cada procedimiento se borra y se vuelve a crear) y no cambia ninguna tabla. Se carga después del `06`.
