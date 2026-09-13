# SIGMETA

Tarea Academica de Programacion 3. Grupo 5.

## Estructura

    pom.xml                 padre, no tiene codigo
    sigmeta-dominio/        clases del modelo
    sigmeta-ejecucion/      Main y las cinco pruebas
    sql/                    un archivo por estudiante

## Como abrirlo

En IntelliJ: File > Open, y seleccionar el pom.xml de la raiz (el archivo,
no la carpeta). Deberian aparecer los tres modulos en el panel de Maven.

Para verificar que compila, desde la raiz:

    mvn clean install

Tienen que salir tres BUILD SUCCESS.

## Como correr las pruebas

La clase Main esta en sigmeta-ejecucion, paquete pe.edu.pucp.ejecucion.
Cada uno completa el metodo ejecutar de su clase Prueba. Las cinco estan
encadenadas: cada una recibe lo que produjo la anterior.

Para probar tu parte sin esperar a los demas, comenta las llamadas de las
otras en el Main y pasale objetos inventados a la tuya.

## SQL

Cada uno escribe su archivo en sql/. El numero indica el orden en que se
concatenan, que es el orden de las llaves foraneas.

Para armar el script completo:

    cat sql/0*.sql > sigmeta.sql

Y despues se corre sobre una base vacia. El diagrama sale de ahi con
Database > Reverse Engineer de MySQL Workbench.
