package pe.edu.pucp.sigmeta.boimpl.reporte;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.design.JRAbstractMultiClassCompiler;

/**
 * RF015: compilador de las expresiones de las plantillas JasperReports que genera clases para Java 17.
 *
 * Por que: el compilador que trae JasperReports (JRJdk13Compiler) genera las clases para la version del JDK con
 * la que se compila. Si los .jasper se compilan con un JDK mas nuevo (por ejemplo 25) y GlassFish corre con 17 o
 * 21, el PDF falla con "UnsupportedClassVersionError ... compiled by a more recent version of the Java Runtime" y
 * el reporte no se descarga. Con --release 17 los .jasper sirven en cualquier GlassFish con Java 17 o superior
 * (la misma version que usa el resto del proyecto: maven.compiler.target = 17).
 *
 * Se activa con la propiedad net.sf.jasperreports.compiler.java (ver CompilarPlantillas y ReportePdf).
 */
public class CompiladorPlantillasJava extends JRAbstractMultiClassCompiler {

    public static final String PROPIEDAD = "net.sf.jasperreports.compiler.java";
    static final String VERSION_JAVA = "17";

    public CompiladorPlantillasJava(JasperReportsContext contexto) {
        super(contexto);
    }

    @Override
    public String compileClasses(File[] fuentes, String classpath) throws JRException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            throw new JRException("No hay compilador de Java: GlassFish debe ejecutarse con un JDK (no un JRE)");
        }
        StringWriter errores = new StringWriter();
        try (StandardJavaFileManager archivos = javac.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            List<String> opciones = List.of("--release", VERSION_JAVA, "-nowarn", "-encoding", "UTF-8",
                    "-classpath", classpath == null ? "" : classpath);
            Boolean ok = javac.getTask(errores, archivos, null, opciones, null,
                    archivos.getJavaFileObjects(fuentes)).call();
            return Boolean.TRUE.equals(ok) ? null : errores.toString();
        } catch (IOException e) {
            throw new JRException(e);
        }
    }

    /**
     * Revisa la version de las clases que van dentro del .jasper (cada clase empieza con CAFEBABE y luego trae
     * su version). Si alguna es mas nueva que este Java, el .jasper no sirve en este servidor.
     */
    static boolean compatibleConEsteJava(byte[] jasper) {
        int maxima = 44 + Runtime.version().feature();
        for (int i = 0; i + 7 < jasper.length; i++) {
            if ((jasper[i] & 0xFF) == 0xCA && (jasper[i + 1] & 0xFF) == 0xFE
                    && (jasper[i + 2] & 0xFF) == 0xBA && (jasper[i + 3] & 0xFF) == 0xBE) {
                int version = ((jasper[i + 6] & 0xFF) << 8) | (jasper[i + 7] & 0xFF);
                if (version > maxima) {
                    return false;
                }
            }
        }
        return true;
    }
}
