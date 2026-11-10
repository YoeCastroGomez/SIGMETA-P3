package pe.edu.pucp.sigmeta.boimpl.producto;

import java.text.Normalizer;
import java.util.Locale;

// RF005 y RF006: preprocesado de los textos de categorias y productos.
// Todos los campos de texto se guardan en MAYUSCULAS (menos la imagen, que es el nombre de un archivo).
// El nombre queda en MAYUSCULAS, sin tildes y con un solo espacio entre palabras:
//   "martillo", "Martíllo" y "  MARTILLO " -> "MARTILLO"
//   "Látex y esmaltes" y "latex  y esmaltes" -> "LATEX Y ESMALTES"
// La Ñ se respeta ("Diseño" -> "DISEÑO"): solo se quitan las tildes de las vocales.
// Los BO lo usan al guardar (asi el nombre queda escrito siempre igual en la base)
// y al comparar (para no aceptar repetidos y para buscar sin importar tildes ni mayusculas).
final class TextoNormalizado {

    private TextoNormalizado() {
    }

    static String nombre(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = Normalizer.normalize(texto, Normalizer.Form.NFC).trim()
                .replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        limpio = limpio.replace('Ñ', '\u0001');   // se protege la Ñ para que no pierda su tilde
        limpio = Normalizer.normalize(limpio, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return limpio.replace('\u0001', 'Ñ');
    }

    // Texto opcional (descripcion): igual que el nombre; vacio -> null
    static String opcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return nombre(texto);
    }

    // Codigos (interno, de fabricante, de proveedor): MAYUSCULAS y sin espacios.
    //   " int-001 " -> "INT-001",  "fab 101" -> "FAB101". Vacio -> null.
    // Asi el codigo que escribe el lector de barras siempre coincide con el guardado.
    static String codigo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
