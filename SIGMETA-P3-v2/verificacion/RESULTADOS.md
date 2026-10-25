# Verificacion v2 — 25 de octubre de 2026

## Ejecutado y aprobado

- Compilacion de todas las fuentes principales y pruebas con Java 17.0.20: sin errores.
- Instalacion nueva de esquema, datos y procedimientos en MariaDB 10.11.14 local aislado: correcta.
- Driver usado: MySQL Connector/J 9.5.0.
- Main / PruebaCrud: 11 escenarios originales OK, 0 errores.
- Regression: 17 comprobaciones de la primera correccion OK.
- RegressionV2: 32 comprobaciones adicionales OK, incluida cobertura de los siete hallazgos y ajustes de Cliente.
- Migracion desde la primera version corregida a v2, recarga de SP y repeticion de Main, Regression y RegressionV2: 11 escenarios y 49 comprobaciones OK.
- Contratos: 201 procedimientos de aplicacion y 201 llamadas Java, sin discrepancias de nombre o numero de parametros ni definiciones duplicadas.
- No hay SELECT/INSERT/UPDATE/DELETE directos en las fuentes Java principales; los accesos usan procedimientos.

## Alcance

La compilacion se verifico con el compilador Java, no se afirma un BUILD SUCCESS de Maven. Maven requiere resolver los plugins/dependencias del POM en el entorno del usuario.
Las pruebas de BD se ejecutaron en MariaDB 10.11; no se ha certificado un servidor MySQL nativo. Repetir sobre la version del curso.
La migracion se probo sobre los datos de ejemplo y luego con las pruebas funcionales. No implica haber revisado datos reales del usuario ni reparado automaticamente historicos contradictorios.
No hay capa HTTP; sigue siendo necesaria la integracion de SesionServicio en cualquier controlador web futuro.
El bloqueo comun de v1 se conserva y limita el paralelismo.
