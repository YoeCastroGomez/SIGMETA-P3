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
   `03_SP_daniel.sql`, `04_SP_sebastian.sql`, `05_SP_adriano.sql` y `06_SP_integridad.sql` (siempre al final).
   Antes de `01` y `02`, seleccionar `sigmeta` como schema por defecto: esos dos archivos no tienen `USE sigmeta;`.
3. Opcional: `4_datos_demo/datos_demo.sql` para tener datos realistas en todas las pantallas.

En vez de `sigmeta.sql` se pueden correr los de `1_base/por_modulo/` (00 a 06) en orden. **Una opción o la otra, no las dos.**

No hace falta ejecutar `3_migraciones/`: `sigmeta.sql` ya trae todos esos cambios.

En MySQL 8, quitar antes `ONLY_FULL_GROUP_BY` del `sql_mode` (ver el `README.md` del proyecto): la última sentencia de `sigmeta.sql` falla con ese modo.

### Caso 2: base que ya existe (por ejemplo la compartida de AWS)
**No reinstalar.**

1. Ejecutar solo las migraciones de `3_migraciones/` que todavía no se aplicaron, en orden de número.
2. Volver a cargar todos los `2_procedimientos/` en orden (de `00` a `06`).

| Migración | Qué cambia |
|---|---|
| `01_migracion_reversion_despacho.sql` | Permite revertir despachos en el kardex |
| `02_migracion_reversion_recepciones_ajustes.sql` | Permite revertir recepciones y ajustes de inventario |
| `03_migracion_v1_v2.sql` | Ordena calificaciones de crédito y deja una sola cuenta por cobrar por venta |
| `04_migracion_lab9_sebastian.sql` | Agrega el administrador que autoriza la nota de crédito |
| `05_migracion_nombres_catalogo.sql` | Nombres, códigos y descripciones de categorías y productos en MAYÚSCULAS; revisa repetidos |

### Caso 3: limpiar y cargar datos de demostración
`4_datos_demo/datos_demo.sql` **borra todos los datos** (no las tablas ni los procedimientos) y carga datos coherentes del 2026-08-03 al 2026-11-10: compras, ventas, caja, cobranza y kardex que cuadran entre sí.
La clave de todos los usuarios es `Meta2026`.
**La base de AWS es compartida: avisar al grupo y sacar un respaldo antes.**

### Caso 4: probar un procedimiento a mano
Abrir el archivo del integrante en `5_pruebas/` y ejecutar las consultas que se necesiten.
