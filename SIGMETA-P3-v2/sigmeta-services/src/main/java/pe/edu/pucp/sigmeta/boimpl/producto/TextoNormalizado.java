package pe.edu.pucp.sigmeta.boimpl.producto;

import java.text.Normalizer;
import java.util.Locale;

// Nombres y descripciones comerciales en mayúsculas, con tildes y Ñ.
// La clave de búsqueda ignora las tildes; los códigos eliminan espacios.
public final class TextoNormalizado {

    private TextoNormalizado() {
    }

    // Para guardar: sin espacios sobrantes y en mayúsculas
    public static String nombre(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFC).trim()
                .replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    // Para texto opcional; vacío -> null
    public static String opcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return nombre(texto);
    }

    // Para comparar y buscar: MAYÚSCULAS, sin tildes y con un solo espacio. La Ñ se respeta.
    public static String clave(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = nombre(texto);
        limpio = limpio.replace('Ñ', '\u0001');   // se protege la Ñ para que no pierda su virgulilla
        limpio = Normalizer.normalize(limpio, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return limpio.replace('\u0001', 'Ñ');
    }

    // Códigos (interno, de fabricante, de proveedor): MAYÚSCULAS y sin espacios.
    //   " int-001 " -> "INT-001",  "fab 101" -> "FAB101". Vacío -> null.
    // Así el código que escribe el lector de barras siempre coincide con el guardado.
    public static String codigo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
