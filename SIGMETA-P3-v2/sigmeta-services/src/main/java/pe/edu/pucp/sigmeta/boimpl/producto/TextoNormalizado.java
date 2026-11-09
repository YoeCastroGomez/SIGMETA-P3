package pe.edu.pucp.sigmeta.boimpl.producto;

import java.text.Normalizer;
import java.util.Locale;

// RF005 y RF006: preprocesado de los nombres de categorias y productos.
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
}
