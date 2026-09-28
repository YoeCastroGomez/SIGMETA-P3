package pe.edu.pucp.sigmeta.boimpl.seguridad;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// RNF001: la clave se guarda como hash PBKDF2 con un salt aleatorio por usuario
final class GeneradorHash {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 65536;
    private static final int LONGITUD_HASH_BITS = 256;
    private static final int LONGITUD_SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneradorHash() {
    }

    static String generarSalt() {
        byte[] salt = new byte[LONGITUD_SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    static String calcularHash(String clave, String salt) {
        PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), Base64.getDecoder().decode(salt),
                ITERACIONES, LONGITUD_HASH_BITS);
        try {
            byte[] hash = SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la clave", e);
        } finally {
            spec.clearPassword();
        }
    }

    // compara en tiempo constante; un salt que no es Base64 (datos de prueba) se trata como clave incorrecta
    static boolean verificar(String clave, String salt, String hashGuardado) {
        try {
            byte[] calculado = Base64.getDecoder().decode(calcularHash(clave, salt));
            byte[] guardado = Base64.getDecoder().decode(hashGuardado);
            return MessageDigest.isEqual(calculado, guardado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
