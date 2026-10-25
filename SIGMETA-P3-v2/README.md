# SIGMETA — versión corregida v2

Lee **CAMBIOS_V2.md** para las nuevas reglas, cambios de API y migración desde v1.

Proyecto Java 17 con Maven: dominio, DBManager, DAO, servicios y ejecución.

## Instalación nueva de prueba

**`sql/sigmeta.sql` elimina y recrea la base `sigmeta`. No ejecutarlo en una base que quieras conservar.**

1. Extrae el ZIP y abre el `pom.xml` de la raíz en IntelliJ, NetBeans o VS Code.
2. Con el cliente MySQL, desde la raíz del proyecto, ejecuta:

   ```sh
   mysql -u TU_USUARIO -p --abort-source-on-error < instalar_nueva_bd.sql
   ```

   En Workbench, ejecuta `sql/sigmeta.sql`, luego `sql_sp/00_drop_sps.sql`, los cinco `SP_nombre.sql` y finalmente `SP_integridad.sql`.
3. Configura `sigmeta-ejecucion/src/main/resources/db.properties` para tu base local.
4. Compila desde la raíz:

   ```sh
   mvn clean install
   ```
5. Ejecuta `pe.edu.pucp.sigmeta.ejecucion.Main` desde tu IDE. Ejecuta las pruebas solo sobre datos de prueba.

También puedes cargar los scripts `sql/00_base.sql` a `sql/06_integridad.sql` en orden EN LUGAR de `sql/sigmeta.sql`. No ejecutes ambas alternativas. Los procedimientos se cargan después, en ambos casos.

El driver se fijó a MySQL Connector/J 9.5.0, utilizado en las pruebas. Se reemplazó la referencia original 26.7.0.

## Configuración sin editar credenciales

DBManager admite propiedades JVM que prevalecen sobre `db.properties`:

- `sigmeta.db.host`
- `sigmeta.db.port`
- `sigmeta.db.user`
- `sigmeta.db.password`
- `sigmeta.db.database`

## Qué cambió

- Tipo de persona explícito: una persona natural puede tener RUC y conserva sus nombres.
- SQL, procedimientos, parámetros DAO y datos de ejemplo actualizados conjuntamente.
- Cliente completo en cuentas por cobrar, incluido nombre mostrado.
- Correspondencia cliente–orden al registrar y modificar ventas.
- Cantidades comprometidas acumuladas por línea de orden, excluyendo la propia venta al modificar.
- Despacho no puede incrementar la atención por encima de la cantidad pedida.
- No se anula una orden con ventas vigentes.
- Stock agregado por producto cuando hay varias líneas del mismo producto.
- Contador transaccional para numeración automática de ventas y unicidad de `venta.numero`.
- Todos los accesos SQL escritos directamente en Java se trasladaron a procedimientos: también compras, recepciones y reversiones.
- Protección común de las operaciones de negocio mediante una fila InnoDB bloqueada hasta commit/rollback.
- Adaptador de identidad autenticada `SesionServicio`, con expiración, cierre y comprobación de responsable.

## Concurrencia: decisión deliberada

`Transaccion.ejecutar()` adquiere `sp_transaccion_bloquear()` antes de ejecutar el BO exterior. Este procedimiento bloquea la fila 1 de `transaccion_control` con `FOR UPDATE`. Las llamadas anidadas comparten conexión y bloqueo.

Se usa READ_COMMITTED. La fila común protege operaciones de cobros, ventas, notas de crédito, anulaciones y cierres que comparten datos. Evita protocolos diferentes que dejen rutas sin protección. Funciona entre procesos Java distintos que usan la misma base.

**Costo:** serializa las transacciones de los BO, incluidas lecturas. Es apropiado como solución conservadora para este proyecto académico, no una optimización para muchos usuarios. Una evolución posterior puede sustituirlo por bloqueos por agregado con un orden global bien definido y nuevas pruebas de concurrencia.

Las escrituras hechas directamente con SQL o llamando DAO fuera de `Transaccion` no participan de este protocolo. Los servicios deben entrar por BO. Los SP no hacen commits propios.

## Identidad del responsable

Los BO conservan sus firmas para compatibilidad con la demostración de consola. Son una API interna de confianza, no endpoints públicos.

Un futuro controlador web debe autenticar una vez y conservar `SesionServicio` del lado servidor:

```java
SesionServicio sesion = SesionServicio.iniciar(nombreUsuario, clave);
Venta resultado = sesion.ejecutar(idAutenticado ->
    ventaBO.registrar(venta, idAutenticado));
```

No deserialices una sesión ni recibas `idResponsable` del navegador. Dentro de ese ámbito, intentar usar el ID de otra persona se rechaza. No hay servidor HTTP/UI en este ZIP; cookies, CSRF, HTTPS y almacenamiento distribuido de sesiones pertenecen a esa integración futura. No se afirma que los BO llamados directamente autentiquen al llamador.

## Pruebas

- `Main`: 11 escenarios originales CRUD, rollback, despacho, notas y cierres.
- `sigmeta-ejecucion/src/test/java/Regression.java`: programa de integración con 17 comprobaciones de v1, incluidas ejecuciones simultáneas con conexiones independientes.
- `sigmeta-ejecucion/src/test/java/RegressionV2.java`: 32 comprobaciones nuevas de compras, cuentas, plazos, unidades y Cliente.
- `verificacion/`: resultados reales de la ejecución local y alcance.

`Regression` y `RegressionV2` son programas `main`, no una prueba JUnit descubierta automáticamente por `mvn test`. Compílalos mediante Maven (`test-compile` o `package`) y ejecútalos desde el IDE con el classpath del módulo de ejecución y sus dependencias. Reinstala una base de prueba antes de repetirlo, porque usa identificadores de documentos de prueba fijos.

## Base existente

No uses el instalador destructivo para actualizar datos existentes. Lee `MIGRACION.md`. Los clientes con RUC necesitan clasificación explícita; no hay una conversión automática fiable basada solo en el tipo de documento.

Orden de las pruebas sobre una base nueva: Main, Regression y RegressionV2. Reinstala antes de repetir el conjunto.
