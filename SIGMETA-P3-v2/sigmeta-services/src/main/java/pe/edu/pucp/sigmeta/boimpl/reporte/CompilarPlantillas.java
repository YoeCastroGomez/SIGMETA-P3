package pe.edu.pucp.sigmeta.boimpl.reporte;

import java.io.File;

import net.sf.jasperreports.engine.JasperCompileManager;

/**
 * RF015: compila las plantillas .jrxml a .jasper DURANTE EL BUILD de Maven (fase process-classes, ver el pom de
 * sigmeta-services). Es la buena practica de la sesion 25: "pre-compilar .jrxml -> .jasper en build".
 *
 * Por que: compilar un .jrxml necesita el compilador de Java con las librerias de JasperReports en el classpath.
 * Dentro de GlassFish ese compilador no ve las librerias del .war y falla. Con los .jasper ya compilados, el
 * servidor solo los carga (JRLoader) y no compila nada.
 *
 * Uso: java -cp ... CompilarPlantillas <carpeta con .jrxml> <carpeta de salida para los .jasper>
 */
public final class CompilarPlantillas {

    private CompilarPlantillas() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: CompilarPlantillas <carpeta .jrxml> <carpeta .jasper>");
        }
        File origen = new File(args[0]);
        File destino = new File(args[1]);
        File[] plantillas = origen.listFiles((carpeta, nombre) -> nombre.endsWith(".jrxml"));
        if (plantillas == null || plantillas.length == 0) {
            throw new IllegalStateException("No hay plantillas .jrxml en " + origen.getAbsolutePath());
        }
        if (!destino.isDirectory() && !destino.mkdirs()) {
            throw new IllegalStateException("No se pudo crear " + destino.getAbsolutePath());
        }
        // las clases de las expresiones se generan para Java 17 aunque el build use un JDK mas nuevo
        // (si no, GlassFish con Java 17 o 21 no carga los .jasper y el PDF no se descarga)
        net.sf.jasperreports.engine.DefaultJasperReportsContext.getInstance()
                .setProperty(CompiladorPlantillasJava.PROPIEDAD, CompiladorPlantillasJava.class.getName());
        for (File jrxml : plantillas) {
            String jasper = new File(destino, jrxml.getName().replace(".jrxml", ".jasper")).getAbsolutePath();
            JasperCompileManager.compileReportToFile(jrxml.getAbsolutePath(), jasper);
            System.out.println("[JasperReports] compilada " + jrxml.getName());
        }
        System.out.println("[JasperReports] " + plantillas.length + " plantillas compiladas en " + destino.getAbsolutePath());
    }
}
