package pe.edu.pucp.sigmeta.boimpl.reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
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

    private static final Locale PERU = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
        parametros.put("PERIODO", fecha(texto(datos.get("desde"))) + " al " + fecha(texto(datos.get("hasta"))));
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
            String periodo = texto(parametros.get("PERIODO"));
            return exportar(impreso, "SIGMETA - " + nombre + " - " + periodo,
                    nombre + " del periodo " + periodo + " (RF015)", generadoPor);
        } catch (JRException e) {
            throw new RuntimeException("No se pudo generar el " + nombre.toLowerCase(PERU) + " en PDF: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ cada reporte

    private static void comercial(Map<String, Object> datos, Map<String, Object> p) {
        Map<String, Object> t = mapa(datos.get("totales"));
        double contado = numero(t.get("montoContado"));
        double credito = numero(t.get("montoCredito"));
        double notas = numero(t.get("montoNotasCredito"));
        indicador(p, 1, "VENTAS AL CONTADO", soles(contado), entero(numero(t.get("ventasContado"))) + " ventas");
        indicador(p, 2, "VENTAS AL CRÉDITO", soles(credito), entero(numero(t.get("ventasCredito"))) + " ventas");
        indicador(p, 3, "VENTA NETA DEL PERIODO", soles(contado + credito - notas),
                "Notas de crédito: " + soles(notas));
        indicador(p, 4, "POR COBRAR A LA FECHA", soles(numero(t.get("saldoPorCobrar"))),
                soles(numero(t.get("saldoVencido"))) + " vencido");

        tabla(p, "VENTAS_DIA", datos.get("porDia"), f -> f.put("fechaTexto", fecha(texto(f.get("fecha")))));
        tabla(p, "VENTAS_VENDEDOR", datos.get("porVendedor"), f -> { });
        tabla(p, "VENTAS_CLIENTE", datos.get("porCliente"), f -> { });
        tabla(p, "CREDITO", datos.get("credito"), f -> {
            String calificacion = texto(f.get("calificacionCrediticia"));
            f.put("calificacionTexto", calificacion.isEmpty() || calificacion.equals("SIN_EVALUAR") ? "—" : calificacion);
            double dias = numero(f.get("diasAtraso"));
            f.put("diasAtrasoTexto", dias > 0 ? entero(dias) + " d" : "—");
        });
    }

    private static void abastecimiento(Map<String, Object> datos, Map<String, Object> p) {
        List<Map<String, Object>> compras = filas(datos.get("compras"));
        List<Map<String, Object>> inventario = filas(datos.get("inventario"));
        indicador(p, 1, "COMPRADO EN EL PERIODO", soles(suma(compras, "total")), entero(suma(compras, "compras")) + " compras");
        indicador(p, 2, "COMPRAS POR RECIBIR", entero(suma(compras, "porRecibir")), "registradas o recibidas en parte");
        indicador(p, 3, "INVENTARIO AL COSTO", soles(suma(inventario, "valorCosto")),
                "a precio de venta " + soles(suma(inventario, "valorVenta")));
        indicador(p, 4, "PRODUCTOS EN SU MÍNIMO", entero(suma(inventario, "enMinimo")), "requieren reposición");

        tabla(p, "COMPRAS", compras, f -> { });
        tabla(p, "MAYOR_MOVIMIENTO", datos.get("mayorMovimiento"),
                f -> f.put("salieronTexto", cantidad(numero(f.get("unidades"))) + " " + unidad(texto(f.get("unidadVenta")))));
        tabla(p, "SIN_MOVIMIENTO", datos.get("sinMovimiento"), f -> {
            String ultima = texto(f.get("ultimaSalida"));
            f.put("ultimaSalidaTexto", ultima.isEmpty() ? "Nunca" : fecha(ultima));
        });
        tabla(p, "INVENTARIO", inventario, f -> { });
    }

    private static void caja(Map<String, Object> datos, Map<String, Object> p) {
        List<Map<String, Object>> cierres = filas(datos.get("cierres"));
        List<Map<String, Object>> vigentes = cierres.stream().filter(c -> "VIGENTE".equals(texto(c.get("estado")))).toList();
        double diferencia = suma(vigentes, "diferencia");
        indicador(p, 1, "CIERRES VIGENTES", entero(vigentes.size()), "en el periodo");
        indicador(p, 2, "CIERRES REVERTIDOS", entero(cierres.size() - vigentes.size()), "reabiertos por el Administrador");
        indicador(p, 3, "DIFERENCIA NETA", soles(diferencia), diferencia < -0.005 ? "faltó dinero" : "negativa = faltó dinero");
        indicador(p, 4, "INGRESO NETO EN CAJAS", soles(suma(filas(datos.get("porMedioPago")), "neto")), "todos los medios de pago");

        tabla(p, "CIERRES", cierres, f -> {
            f.put("cajaTexto", "N.º " + entero(numero(f.get("idCaja"))));
            f.put("fechaCierreTexto", fechaHora(texto(f.get("fechaCierre"))));
            String estado = legible(texto(f.get("estado")));
            String reabrio = texto(f.get("usuarioReapertura"));
            f.put("estadoTexto", reabrio.isEmpty() ? estado
                    : estado + " (reabrió " + reabrio + ": " + texto(f.get("motivoReapertura")) + ")");
        });
        tabla(p, "MEDIO_PAGO", datos.get("porMedioPago"), f -> f.put("medioTexto", legible(texto(f.get("medioPago")))));
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
        for (Map<String, Object> fila : filas(filas)) {
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

    // ------------------------------------------------------------------ formato de presentacion

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> filas(Object valor) {
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object fila : lista) {
            if (fila instanceof Map<?, ?> m) {
                resultado.add((Map<String, Object>) m);
            }
        }
        return resultado;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor) {
        return valor instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static double numero(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : 0;
    }

    private static double suma(List<Map<String, Object>> filas, String campo) {
        return filas.stream().mapToDouble(f -> numero(f.get(campo))).sum();
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private static DecimalFormat formato(String patron) {
        return new DecimalFormat(patron, DecimalFormatSymbols.getInstance(PERU));
    }

    /** 1234.5 -> "S/ 1,234.50"; -20 -> "-S/ 20.00" (el signo va antes del simbolo de moneda). */
    private static String soles(double monto) {
        String texto = "S/ " + formato("#,##0.00").format(Math.abs(monto));
        return monto < -0.005 ? "-" + texto : texto;
    }

    private static String entero(double valor) {
        return formato("#,##0").format(valor);
    }

    private static String cantidad(double valor) {
        return formato("#,##0.###").format(valor);
    }

    /** "2026-11-05" -> "05/11/2026"; si no es una fecha, se deja como vino. */
    private static String fecha(String iso) {
        if (iso.length() < 10) {
            return iso;
        }
        try {
            return LocalDate.parse(iso.substring(0, 10)).format(FECHA);
        } catch (RuntimeException e) {
            return iso;
        }
    }

    /** "2026-11-05T18:30:00" -> "05/11/2026 18:30". */
    private static String fechaHora(String iso) {
        try {
            return LocalDateTime.parse(iso.length() > 19 ? iso.substring(0, 19) : iso).format(FECHA_HORA);
        } catch (RuntimeException e) {
            return fecha(iso);
        }
    }

    /** "TARJETA_CREDITO" -> "Tarjeta crédito" (mismo criterio que la pantalla, con tildes de los valores conocidos). */
    private static String legible(String codigo) {
        if (codigo.isEmpty()) {
            return "—";
        }
        String texto = codigo.replace('_', ' ').toLowerCase(PERU)
                .replace("credito", "crédito").replace("debito", "débito");
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String unidad(String codigo) {
        return switch (codigo) {
            case "UNIDAD" -> "und";
            case "CAJA" -> "cajas";
            case "KILOGRAMO" -> "kg";
            case "METRO" -> "m";
            case "LITRO" -> "L";
            case "MILLAR" -> "millares";
            case "GALON" -> "gal";
            case "JUEGO" -> "juegos";
            default -> codigo.toLowerCase(PERU);
        };
    }
}
