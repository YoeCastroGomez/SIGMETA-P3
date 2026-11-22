package pe.edu.pucp.sigmeta.boimpl.reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.SimplePdfExporterConfiguration;

/**
 * RF015: genera en PDF los tres reportes del Administrador con JasperReports (sesion 25 del curso).
 *
 * Ciclo de JasperReports:
 *   1. Diseno:      plantillas .jrxml en src/main/resources/reports (una principal por reporte y un subreporte por tabla).
 *   2. Compilacion: en el BUILD (CompilarPlantillas genera los .jasper); aqui se cargan con JRLoader y quedan en cache.
 *   3. Llenado:     JasperFillManager con parametros (membrete e indicadores) y un JRMapCollectionDataSource por tabla.
 *   4. Exportacion: JRPdfExporter a un byte[] con los metadatos del PDF (titulo, autor, asunto).
 *
 * Los datos ya vienen agregados de la base (ReporteBO, procedimientos sp_reporte_*): aqui solo se les da formato
 * de presentacion (fechas dd/MM/aaaa, textos legibles). La plantilla no tiene logica de negocio.
 */
public final class ReportePdf {

    public static final String COMERCIAL = "comercial";
    public static final String ABASTECIMIENTO = "abastecimiento";
    public static final String CAJA = "caja";

    static final Locale PERU = Locale.forLanguageTag("es-PE");
    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Plantillas ya compiladas: compilar un .jrxml es lento y basta hacerlo una vez (buena practica de la sesion 25). */
    private static final Map<String, JasperReport> COMPILADAS = new ConcurrentHashMap<>();

    private ReportePdf() {
    }

    /** Nombre que se muestra en el PDF y en el archivo. */
    public static String nombre(String tipo) {
        return switch (tipo) {
            case COMERCIAL -> "Reporte comercial";
            case ABASTECIMIENTO -> "Reporte de abastecimiento e inventario";
            case CAJA -> "Reporte de caja";
            default -> throw new IllegalArgumentException(
                    "Reporte desconocido: " + tipo + ". Los reportes son: comercial, abastecimiento y caja");
        };
    }

    /**
     * Genera el PDF de un reporte.
     *
     * @param tipo        comercial, abastecimiento o caja
     * @param datos       lo que devuelve ReporteBO para ese tipo (desde, hasta y sus tablas)
     * @param generadoPor nombre del Administrador que lo pide
     * @param emitido     fecha y hora de emision
     */
    public static byte[] generar(String tipo, Map<String, Object> datos, String generadoPor, LocalDateTime emitido) {
        String nombre = nombre(tipo);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put(JRParameter.REPORT_LOCALE, PERU);
        parametros.put("NOMBRE_REPORTE", nombre);
        parametros.put("PERIODO", ReporteFormato.fecha(ReporteFormato.texto(datos.get("desde"))) + " al " + ReporteFormato.fecha(ReporteFormato.texto(datos.get("hasta"))));
        parametros.put("EMITIDO", emitido.format(FECHA_HORA));
        parametros.put("GENERADO_POR", generadoPor == null || generadoPor.isBlank() ? "—" : generadoPor);

        switch (tipo) {
            case COMERCIAL -> comercial(datos, parametros);
            case ABASTECIMIENTO -> abastecimiento(datos, parametros);
            default -> caja(datos, parametros);
        }

        try {
            // la plantilla principal tiene una sola "fila": las tablas son subreportes con su propio data source
            JasperPrint impreso = JasperFillManager.fillReport(plantilla("reporte_" + tipo), parametros, new JREmptyDataSource(1));
            String periodo = ReporteFormato.texto(parametros.get("PERIODO"));
            return exportar(impreso, "SIGMETA - " + nombre + " - " + periodo,
                    nombre + " del periodo " + periodo + " (RF015)", generadoPor);
        } catch (JRException e) {
            throw new RuntimeException("No se pudo generar el " + nombre.toLowerCase(PERU) + " en PDF: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ cada reporte

    private static void comercial(Map<String, Object> datos, Map<String, Object> p) {
        Map<String, Object> t = ReporteFormato.mapa(datos.get("totales"));
        double contado = ReporteFormato.numero(t.get("montoContado"));
        double credito = ReporteFormato.numero(t.get("montoCredito"));
        double notas = ReporteFormato.numero(t.get("montoNotasCredito"));
        indicador(p, 1, "VENTAS AL CONTADO", ReporteFormato.soles(contado), ReporteFormato.entero(ReporteFormato.numero(t.get("ventasContado"))) + " ventas");
        indicador(p, 2, "VENTAS AL CRÉDITO", ReporteFormato.soles(credito), ReporteFormato.entero(ReporteFormato.numero(t.get("ventasCredito"))) + " ventas");
        indicador(p, 3, "VENTA NETA DEL PERIODO", ReporteFormato.soles(contado + credito - notas),
                "Notas de crédito: " + ReporteFormato.soles(notas));
        indicador(p, 4, "POR COBRAR A LA FECHA", ReporteFormato.soles(ReporteFormato.numero(t.get("saldoPorCobrar"))),
                ReporteFormato.soles(ReporteFormato.numero(t.get("saldoVencido"))) + " vencido");

        tabla(p, "VENTAS_DIA", datos.get("porDia"), f -> f.put("fechaTexto", ReporteFormato.fecha(ReporteFormato.texto(f.get("fecha")))));
        tabla(p, "VENTAS_VENDEDOR", datos.get("porVendedor"), f -> { });
        tabla(p, "VENTAS_CLIENTE", datos.get("porCliente"), f -> { });
        tabla(p, "CREDITO", datos.get("credito"), f -> {
            String calificacion = ReporteFormato.texto(f.get("calificacionCrediticia"));
            f.put("calificacionTexto", calificacion.isEmpty() || calificacion.equals("SIN_EVALUAR") ? "—" : calificacion);
            double dias = ReporteFormato.numero(f.get("diasAtraso"));
            f.put("diasAtrasoTexto", dias > 0 ? ReporteFormato.entero(dias) + " d" : "—");
        });
    }

    private static void abastecimiento(Map<String, Object> datos, Map<String, Object> p) {
        List<Map<String, Object>> compras = ReporteFormato.filas(datos.get("compras"));
        List<Map<String, Object>> inventario = ReporteFormato.filas(datos.get("inventario"));
        indicador(p, 1, "COMPRADO EN EL PERIODO", ReporteFormato.soles(ReporteFormato.suma(compras, "total")), ReporteFormato.entero(ReporteFormato.suma(compras, "compras")) + " compras");
        indicador(p, 2, "COMPRAS POR RECIBIR", ReporteFormato.entero(ReporteFormato.suma(compras, "porRecibir")), "registradas o recibidas en parte");
        indicador(p, 3, "INVENTARIO AL COSTO", ReporteFormato.soles(ReporteFormato.suma(inventario, "valorCosto")),
                "a precio de venta " + ReporteFormato.soles(ReporteFormato.suma(inventario, "valorVenta")));
        indicador(p, 4, "PRODUCTOS EN SU MÍNIMO", ReporteFormato.entero(ReporteFormato.suma(inventario, "enMinimo")), "requieren reposición");

        tabla(p, "COMPRAS", compras, f -> { });
        tabla(p, "MAYOR_MOVIMIENTO", datos.get("mayorMovimiento"),
                f -> f.put("salieronTexto", ReporteFormato.cantidad(ReporteFormato.numero(f.get("unidades"))) + " " + ReporteFormato.unidad(ReporteFormato.texto(f.get("unidadVenta")))));
        tabla(p, "SIN_MOVIMIENTO", datos.get("sinMovimiento"), f -> {
            String ultima = ReporteFormato.texto(f.get("ultimaSalida"));
            f.put("ultimaSalidaTexto", ultima.isEmpty() ? "Nunca" : ReporteFormato.fecha(ultima));
        });
        tabla(p, "INVENTARIO", inventario, f -> { });
    }

    private static void caja(Map<String, Object> datos, Map<String, Object> p) {
        List<Map<String, Object>> cierres = ReporteFormato.filas(datos.get("cierres"));
        List<Map<String, Object>> vigentes = cierres.stream().filter(c -> "VIGENTE".equals(ReporteFormato.texto(c.get("estado")))).toList();
        double diferencia = ReporteFormato.suma(vigentes, "diferencia");
        indicador(p, 1, "CIERRES VIGENTES", ReporteFormato.entero(vigentes.size()), "en el periodo");
        indicador(p, 2, "CIERRES REVERTIDOS", ReporteFormato.entero(cierres.size() - vigentes.size()), "reabiertos por el Administrador");
        indicador(p, 3, "DIFERENCIA NETA", ReporteFormato.soles(diferencia), diferencia < -0.005 ? "faltó dinero" : "negativa = faltó dinero");
        indicador(p, 4, "INGRESO NETO EN CAJAS", ReporteFormato.soles(ReporteFormato.suma(ReporteFormato.filas(datos.get("porMedioPago")), "neto")), "todos los medios de pago");

        tabla(p, "CIERRES", cierres, f -> {
            f.put("cajaTexto", "N.º " + ReporteFormato.entero(ReporteFormato.numero(f.get("idCaja"))));
            f.put("fechaCierreTexto", ReporteFormato.fechaHora(ReporteFormato.texto(f.get("fechaCierre"))));
            String estado = ReporteFormato.legible(ReporteFormato.texto(f.get("estado")));
            String reabrio = ReporteFormato.texto(f.get("usuarioReapertura"));
            f.put("estadoTexto", reabrio.isEmpty() ? estado
                    : estado + " (reabrió " + reabrio + ": " + ReporteFormato.texto(f.get("motivoReapertura")) + ")");
        });
        tabla(p, "MEDIO_PAGO", datos.get("porMedioPago"), f -> f.put("medioTexto", ReporteFormato.legible(ReporteFormato.texto(f.get("medioPago")))));
        tabla(p, "CAJERO", datos.get("porCajero"), f -> { });
    }

    // ------------------------------------------------------------------ JasperReports

    /**
     * Carga la plantilla ya compilada (.jasper, generada en el build por CompilarPlantillas) y la guarda en cache.
     * Si el .jasper no existe (por ejemplo al probar desde el IDE sin pasar por Maven) compila el .jrxml; eso
     * funciona fuera del servidor, pero dentro de GlassFish solo se usan los .jasper.
     */
    static JasperReport plantilla(String nombre) {
        return COMPILADAS.computeIfAbsent(nombre, n -> {
            try (InputStream jasper = ReportePdf.class.getResourceAsStream("/reports/" + n + ".jasper")) {
                if (jasper != null) {
                    return (JasperReport) JRLoader.loadObject(jasper);
                }
            } catch (JRException | IOException e) {
                throw new RuntimeException("No se pudo cargar la plantilla " + n + ".jasper: " + e.getMessage(), e);
            }
            try (InputStream jrxml = ReportePdf.class.getResourceAsStream("/reports/" + n + ".jrxml")) {
                if (jrxml == null) {
                    throw new IllegalStateException("No se encontró la plantilla /reports/" + n);
                }
                prepararCompilador();
                return JasperCompileManager.compileReport(jrxml);
            } catch (JRException | IOException e) {
                throw new RuntimeException("Falta la plantilla compilada " + n + ".jasper. Ejecute 'mvn clean install' "
                        + "con GlassFish detenido y vuelva a desplegar. Detalle: " + e.getMessage(), e);
            }
        });
    }

    private static volatile boolean compiladorListo;

    /**
     * Respaldo por si falta un .jasper: el compilador de Java que usa JasperReports solo ve el classpath del
     * servidor (java.class.path), no las librerias del .war, y falla con "cannot find symbol class JRFillField".
     * Se le pasan los .jar de la carpeta donde esta JasperReports (WEB-INF/lib dentro de GlassFish).
     */
    private static void prepararCompilador() {
        if (compiladorListo) {
            return;
        }
        try {
            java.io.File jar = new java.io.File(JasperReport.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            java.io.File carpeta = jar.isFile() ? jar.getParentFile() : jar;
            StringBuilder classpath = new StringBuilder(System.getProperty("java.class.path", ""));
            java.io.File[] jars = carpeta.listFiles((d, nombreArchivo) -> nombreArchivo.endsWith(".jar"));
            if (jars != null) {
                for (java.io.File f : jars) {
                    classpath.append(java.io.File.pathSeparator).append(f.getAbsolutePath());
                }
            }
            net.sf.jasperreports.engine.DefaultJasperReportsContext.getInstance()
                    .setProperty("net.sf.jasperreports.compiler.classpath", classpath.toString());
        } catch (Exception e) {
            // sin la ubicacion de las librerias se intenta con el classpath por defecto
        }
        compiladorListo = true;
    }

    /** Agrega el subreporte compilado y su data source (una copia de cada fila con los textos de presentacion). */
    private static void tabla(Map<String, Object> p, String clave, Object filas, Consumer<Map<String, Object>> formato) {
        List<Map<String, ?>> preparadas = new ArrayList<>();
        for (Map<String, Object> fila : ReporteFormato.filas(filas)) {
            Map<String, Object> copia = new LinkedHashMap<>(fila);
            formato.accept(copia);
            preparadas.add(copia);
        }
        p.put("SUB_" + clave, plantilla("sub_" + clave.toLowerCase(Locale.ROOT)));
        p.put("DS_" + clave, new JRMapCollectionDataSource(preparadas));
    }

    private static void indicador(Map<String, Object> p, int n, String titulo, String valor, String detalle) {
        p.put("IND" + n + "_TITULO", titulo);
        p.put("IND" + n + "_VALOR", valor);
        p.put("IND" + n + "_DETALLE", detalle);
    }

    /** Exporta a PDF (byte[]) con sus metadatos. Lo usan los reportes (RF015) y el comprobante (RF008). */
    static byte[] exportar(JasperPrint impreso, String titulo, String asunto, String autor) throws JRException {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        JRPdfExporter exportador = new JRPdfExporter();
        exportador.setExporterInput(new SimpleExporterInput(impreso));
        exportador.setExporterOutput(new SimpleOutputStreamExporterOutput(pdf));
        SimplePdfExporterConfiguration configuracion = new SimplePdfExporterConfiguration();
        configuracion.setMetadataTitle(titulo);
        configuracion.setMetadataAuthor(autor == null || autor.isBlank() ? "SIGMETA" : autor);
        configuracion.setMetadataSubject(asunto);
        configuracion.setMetadataCreator("SIGMETA · Grupo Meta");
        configuracion.setMetadataKeywords("SIGMETA, Grupo Meta, " + asunto);
        exportador.setConfiguration(configuracion);
        exportador.exportReport();
        return pdf.toByteArray();
    }

}
