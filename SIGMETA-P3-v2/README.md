# SIGMETA - Grupo 5

Sistema de ventas, inventario y caja para GRUPO META, una ferreteria industrial de Lurigancho-Chosica.
Tarea Academica de Programacion 3 (1INF30), PUCP, 2026-2.

Este repositorio es el **backend en Java**. El frontend (Blazor, .NET 10) es un proyecto aparte.

## Integrantes

| Estudiante | Nombre      | Parte                                   |
|------------|-------------|-----------------------------------------|
| 1          | Yoe         | Seguridad y maestros (usuarios, clientes, autorizaciones) |
| 2          | Claudia     | Catalogo y cotizaciones                  |
| 3          | Luis Daniel | Compras y almacen                        |
| 4          | Sebastian   | Ventas, comprobantes y notas de credito  |
| 5          | Adriano     | Caja, cobranza y reportes                |

## Estructura

Proyecto Maven con 5 modulos. Cada capa solo usa a la de abajo (RNF005).

| Modulo              | Que tiene                                                        |
|---------------------|------------------------------------------------------------------|
| `sigmeta-dominio`   | Clases del modelo y enums                                        |
| `sigmeta-dbmanager` | `DBManager` (conexion) y `transactionContext` (una conexion por transaccion) |
| `sigmeta-dao`       | Acceso a datos. Todo se hace con procedimientos almacenados      |
| `sigmeta-services`  | Logica de negocio (BO): validaciones, permisos por rol, commit y rollback |
| `sigmeta-ejecucion` | `Main` con las pruebas                                           |

Los paquetes de `dao` y `services` siguen los mismos subpaquetes que el modelo
(`socio`, `usuario`, `seguridad`, `producto`, `ventas`, `compras`, `almacen`, `caja`, `cobranza`).

## Requisitos

- Java 17
- Maven
- MySQL (driver MySQL Connector/J 9.5.0, ya incluido en el `pom.xml`)

## 1. Instalar la base de datos

**Ojo: esto borra la base `sigmeta` y la crea de nuevo. Sacar un respaldo antes.**

Con la consola de MySQL, desde la raiz del proyecto:

```sh
mysql -h HOST -u USUARIO -p --abort-source-on-error < instalar_nueva_bd.sql
```

Con Workbench (no acepta `SOURCE`), ejecutar uno por uno y en este orden:

1. `sql/sigmeta.sql` (tablas y datos de ejemplo)
2. `sql_sp/00_drop_sps.sql`
3. `sql_sp/SP_yoe.sql`
4. `sql_sp/SP_claudia.sql`
5. `sql_sp/SP_daniel.sql`
6. `sql_sp/SP_sebastian.sql`
7. `sql_sp/SP_adriano.sql`
8. `sql_sp/SP_integridad.sql` (siempre al final)

Antes de los pasos 3 y 4, seleccionar `sigmeta` como schema por defecto (doble clic en el panel de Schemas),
porque esos dos archivos no tienen `USE sigmeta;`.

En vez de `sql/sigmeta.sql` se pueden correr `sql/00_base.sql` a `sql/06_integridad.sql` en orden.
Una opcion o la otra, no las dos.

Para revisar que quedo bien: 29 tablas y 201 procedimientos en el schema `sigmeta`.

Los archivos `sql_sp/sql_prueba_*.sql` son consultas para probar a mano los procedimientos de cada integrante.

## 2. Configurar la conexion

El archivo `db.properties` tiene la clave de la base y **no se sube al repositorio**.

1. Copiar `sigmeta-ejecucion/src/main/resources/db.properties.example`
   como `db.properties` en la misma carpeta.
2. Completar los datos:

```properties
db.host=HOST
db.port=3306
db.user=USUARIO
db.password=CLAVE
db.database=sigmeta
```

## 3. Compilar y ejecutar

```sh
mvn clean install
```

Luego ejecutar `pe.edu.pucp.sigmeta.ejecucion.Main` desde el IDE.

El `Main` corre 11 pruebas, todas a traves de la capa de negocio. Tiene que terminar con
**11 PRUEBAS OK | 0 CON ERROR**.

| #  | Prueba                                   |
|----|------------------------------------------|
| 1  | CRUD de cliente (RF003)                  |
| 2  | CRUD de producto (RF005)                 |
| 3  | CRUD de cotizacion (RF007)               |
| 4  | CRUD de compra a proveedor (RF011)       |
| 5  | CRUD de venta (RF008)                    |
| 6  | CRUD de caja (RF010)                     |
| 7  | Rollback de una transaccion (RNF002)     |
| 8  | Razon social unica (correccion Lab 4)    |
| 9  | Despacho por linea de venta (correccion Lab 4) |
| 10 | Nota de credito con descuento (correccion Lab 4) |
| 11 | Cierre y reapertura de caja (correccion Lab 4)   |

Las pruebas insertan datos en la base. Correrlas solo en la base del curso o en una de prueba.

## Reglas del grupo

- Cada uno trabaja en su rama. Solo se mergea a `main` lo que compila.
- Si se cambia una tabla, el cambio tiene que estar tambien en la clase, el DAO, el SP y el BO.
- Antes de cualquier cambio a la base se saca un respaldo.
- `db.properties` nunca se sube al repositorio.