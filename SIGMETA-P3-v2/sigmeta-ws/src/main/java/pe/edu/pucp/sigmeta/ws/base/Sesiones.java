package pe.edu.pucp.sigmeta.ws.base;

import pe.edu.pucp.sigmeta.seguridad.SesionServicio;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// RF001: aqui se guardan las sesiones de los usuarios conectados.
// Al iniciar sesion se crea un ticket (un texto al azar) y se guarda junto con su sesion.
// Blazor manda ese ticket en cada pedido y con el se encuentra la sesion: asi el Java sabe
// quien esta conectado sin que Blazor mande el id del usuario.
public class Sesiones {

    private static final Map<String, SesionServicio> sesiones = new HashMap<>();

    public static synchronized String guardar(SesionServicio sesion) {
        String ticket = UUID.randomUUID().toString();
        sesiones.put(ticket, sesion);
        return ticket;
    }

    // devuelve null si no hay ticket, si el ticket no existe o si la sesion ya vencio
    public static synchronized SesionServicio buscar(String ticket) {
        if (ticket == null) {
            return null;
        }
        SesionServicio sesion = sesiones.get(ticket);
        if (sesion != null && !sesion.estaVigente()) {
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
    }
}
