package pe.edu.pucp.sigmeta.ws.base;

import pe.edu.pucp.sigmeta.seguridad.SesionServicio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// RF001: aqui se guardan las sesiones de los usuarios conectados.
// Al iniciar sesion se crea un ticket y se guarda junto con su sesion.
// Blazor manda ese ticket en cada pedido y con el se encuentra la sesion: asi el Java sabe
// quien esta conectado sin que Blazor mande el id del usuario.
//
// El ticket va FIRMADO por el servidor: "idUsuario.emitido.azar.firma". Si GlassFish se reinicia o se
// redespliega (por ejemplo al guardar un archivo en NetBeans), el mapa en memoria se pierde, pero el
// ticket sigue sirviendo porque la firma prueba que lo dio este servidor. Asi el usuario no es
// expulsado al login cada vez que se redespliega. Nadie puede inventar un ticket sin la clave secreta.
public class Sesiones {

    // un ticket firmado solo se puede reanudar (tras un reinicio) hasta 12 horas despues de emitido
    private static final long VIDA_MAXIMA_TICKET_MS = 12L * 60 * 60 * 1000;

    private static final Map<String, SesionServicio> sesiones = new HashMap<>();
    private static final Map<String, Long> cerrados = new HashMap<>();
    private static final byte[] clave = cargarClave();

    public static synchronized String guardar(SesionServicio sesion) {
        String cuerpo = sesion.getIdUsuario() + "." + System.currentTimeMillis() + "."
                + UUID.randomUUID().toString().replace("-", "");
        String ticket = cuerpo + "." + firmar(cuerpo);
        sesiones.put(ticket, sesion);
        return ticket;
    }

    // devuelve null si no hay ticket, si el ticket no es valido o si la sesion ya vencio
    public static synchronized SesionServicio buscar(String ticket) {
        if (ticket == null) {
            return null;
        }
        SesionServicio sesion = sesiones.get(ticket);
        if (sesion == null) {
            sesion = reanudar(ticket);
            if (sesion == null) {
                return null;
            }
            sesiones.put(ticket, sesion);
        }
        if (!sesion.estaVigente()) {
            sesiones.remove(ticket);
            return null;
        }
        return sesion;
    }

    public static synchronized void cerrar(String ticket) {
        if (ticket == null) {
            return;
        }
        SesionServicio sesion = sesiones.remove(ticket);
        if (sesion != null) {
            sesion.cerrar();
        }
        cerrados.put(ticket, System.currentTimeMillis());
        cerrados.values().removeIf(t -> System.currentTimeMillis() - t > VIDA_MAXIMA_TICKET_MS);
    }

    // ticket que este servidor firmo pero que ya no esta en memoria (GlassFish se reinicio)
    private static SesionServicio reanudar(String ticket) {
        if (cerrados.containsKey(ticket)) {
            return null;
        }
        String[] partes = ticket.split("\\.");
        if (partes.length != 4) {
            return null;
        }
        String cuerpo = partes[0] + "." + partes[1] + "." + partes[2];
        if (!MessageDigest.isEqual(firmar(cuerpo).getBytes(StandardCharsets.UTF_8),
                partes[3].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        try {
            int idUsuario = Integer.parseInt(partes[0]);
            long emitido = Long.parseLong(partes[1]);
            if (System.currentTimeMillis() - emitido > VIDA_MAXIMA_TICKET_MS) {
                return null;
            }
            return SesionServicio.reanudar(idUsuario);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firmar(String cuerpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            byte[] firma = mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(firma);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el ticket", e);
        }
    }

    // La clave secreta se guarda en ~/.sigmeta/clave-ticket para que sobreviva a los reinicios.
    // Si no se puede leer ni escribir, se usa una clave solo en memoria (los tickets duran hasta el reinicio).
    private static byte[] cargarClave() {
        Path archivo = Paths.get(System.getProperty("user.home"), ".sigmeta", "clave-ticket");
        try {
            if (Files.exists(archivo)) {
                byte[] guardada = Base64.getDecoder().decode(Files.readString(archivo).trim());
                if (guardada.length >= 32) {
                    return guardada;
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            // se genera una nueva
        }
        byte[] nueva = new byte[32];
        new SecureRandom().nextBytes(nueva);
        try {
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, Base64.getEncoder().encodeToString(nueva));
        } catch (IOException e) {
            System.err.println("SIGMETA: no se pudo guardar la clave de tickets en " + archivo + ": " + e.getMessage());
        }
        return nueva;
    }
}
