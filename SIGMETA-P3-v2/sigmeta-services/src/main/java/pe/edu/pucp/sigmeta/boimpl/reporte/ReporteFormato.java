package pe.edu.pucp.sigmeta.boimpl.reporte;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static pe.edu.pucp.sigmeta.boimpl.reporte.ReportePdf.CAJA;
import static pe.edu.pucp.sigmeta.boimpl.reporte.ReportePdf.PERU;
import static pe.edu.pucp.sigmeta.boimpl.reporte.ReportePdf.FECHA;
import static pe.edu.pucp.sigmeta.boimpl.reporte.ReportePdf.FECHA_HORA;

// RF015: lectura de los datos del reporte y formato de montos, cantidades, fechas y codigos.
// Parte de ReportePdf: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class ReporteFormato {

    // ------------------------------------------------------------------ formato de presentacion

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> filas(Object valor) {
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
    static Map<String, Object> mapa(Object valor) {
        return valor instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    static double numero(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : 0;
    }

    static double suma(List<Map<String, Object>> filas, String campo) {
        return filas.stream().mapToDouble(f -> numero(f.get(campo))).sum();
    }

    static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    static DecimalFormat formato(String patron) {
        return new DecimalFormat(patron, DecimalFormatSymbols.getInstance(PERU));
    }

    /** 1234.5 -> "S/ 1,234.50"; -20 -> "-S/ 20.00" (el signo va antes del simbolo de moneda). */
    static String soles(double monto) {
        String texto = "S/ " + formato("#,##0.00").format(Math.abs(monto));
        return monto < -0.005 ? "-" + texto : texto;
    }

    static String entero(double valor) {
        return formato("#,##0").format(valor);
    }

    static String cantidad(double valor) {
        return formato("#,##0.###").format(valor);
    }

    /** "2026-11-05" -> "05/11/2026"; si no es una fecha, se deja como vino. */
    static String fecha(String iso) {
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
    static String fechaHora(String iso) {
        try {
            return LocalDateTime.parse(iso.length() > 19 ? iso.substring(0, 19) : iso).format(FECHA_HORA);
        } catch (RuntimeException e) {
            return fecha(iso);
        }
    }

    /** "TARJETA_CREDITO" -> "Tarjeta crédito" (mismo criterio que la pantalla, con tildes de los valores conocidos). */
    static String legible(String codigo) {
        if (codigo.isEmpty()) {
            return "—";
        }
        String texto = codigo.replace('_', ' ').toLowerCase(PERU)
                .replace("credito", "crédito").replace("debito", "débito");
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    static String unidad(String codigo) {
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
