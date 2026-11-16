package pe.edu.pucp.sigmeta.boimpl.producto;

import java.text.Normalizer;
import java.util.Locale;

// Criterio único de textos para Producto, Categoría, Proveedor y Cliente:
//   GUARDAR como lo escribe el usuario: sin espacios al inicio ni al final, con un solo espacio entre
//   palabras y en forma NFC. Se respetan las mayúsculas, las minúsculas y las tildes.
//     "  Martillo   de acero " -> "Martillo de acero"
//   COMPARAR con una clave: MAYÚSCULAS y sin tildes (la Ñ se respeta). Sirve para no aceptar repetidos
//   y para buscar sin importar tildes ni mayúsculas. La clave nunca se guarda.
//     "martillo", "Martíllo" y "  MARTILLO " -> clave "MARTILLO"
//     "Diseño" -> clave "DISEÑO"
//   Los CÓDIGOS (interno, de fabricante, de proveedor) sí se guardan en MAYÚSCULAS y sin espacios.
public final class TextoNormalizado {

    private TextoNormalizado() {
    }

    // Para guardar un texto obligatorio: trim, NFC y un solo espacio entre palabras
    public static String nombre(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ");
    }

    // Para guardar un texto opcional (descripción): igual que el nombre; vacío -> null
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
        String limpio = nombre(texto).toUpperCase(Locale.ROOT);
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
