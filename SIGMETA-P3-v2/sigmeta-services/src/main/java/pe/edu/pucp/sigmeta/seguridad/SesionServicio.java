package pe.edu.pucp.sigmeta.seguridad;

import java.sql.SQLException;
import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

/** Identidad autenticada del lado servidor. Nunca deserializarla desde una peticion.
 * Los BO son internos; un adaptador web debe conservar esta instancia en su sesion.
 */
public final class SesionServicio {
    private final int idUsuario;
    // RF001: la sesion vence tras 30 minutos SIN USO; cada pedido la renueva
    // (mientras el usuario siga trabajando no se cierra)
    public static final long INACTIVIDAD_MAXIMA_MS = 30L * 60 * 1000;
    private volatile long expira;
    private volatile boolean cerrada;
    private static final ThreadLocal<Integer> ACTUAL = new ThreadLocal<>();
    private SesionServicio(int id) {
        idUsuario = id;
        renovar();
    }
    public static SesionServicio iniciar(String nombre, String clave) throws SQLException {
        Usuario usuario = new UsuarioBOImpl().iniciarSesion(nombre, clave);
        return new SesionServicio(usuario.getId());
    }
    /** Para el adaptador web: vuelve a abrir la sesion de un usuario cuyo ticket FIRMADO por el
     * servidor sigue vigente (por ejemplo despues de reiniciar o redesplegar GlassFish).
     * Solo se debe llamar despues de verificar la firma del ticket. En cada pedido se vuelve a
     * revisar que el usuario y su rol sigan activos. */
    public static SesionServicio reanudar(int idUsuario) {
        return new SesionServicio(idUsuario);
    }
    public int getIdUsuario() { return idUsuario; }
    @FunctionalInterface
    public interface Accion<T> { T ejecutar(int idAutenticado) throws SQLException; }
    public <T> T ejecutar(Accion<T> accion) throws SQLException {
        if (cerrada) {
            throw new SecurityException("Su sesion fue cerrada. Vuelva a iniciar sesion");
        }
        if (System.currentTimeMillis() >= expira) {
            throw new SecurityException("Su sesion vencio tras 30 minutos sin uso. Vuelva a iniciar sesion");
        }
        renovar();
        if (ACTUAL.get() != null) throw new SecurityException("No se permite sustituir una sesion activa");
        ACTUAL.set(idUsuario);
        try {
            return Transaccion.ejecutar(() -> {
                Usuario usuario = new UsuarioBOImpl().obtener(idUsuario);
                if (usuario == null || !usuario.isEstado() || !usuario.getRol().isEstado()) {
                    // RF001/RF002: si el Administrador desactivo al usuario (o a su rol) mientras estaba
                    // conectado, se cierra su sesion: el adaptador web responde 401 y Blazor lo saca al login
                    cerrar();
                    throw new SecurityException("Su usuario o su rol fue desactivado. Consulte con el Administrador");
                }
                return accion.ejecutar(idUsuario);
            });
        } finally { ACTUAL.remove(); }
    }
    private void renovar() { expira = System.currentTimeMillis() + INACTIVIDAD_MAXIMA_MS; }
    public void cerrar() { cerrada = true; }
    // para el adaptador web (sigmeta-ws): saber si el ticket sigue sirviendo
    public boolean estaVigente() { return !cerrada && System.currentTimeMillis() < expira; }
    public static void verificarResponsable(int id) {
        Integer autenticado = ACTUAL.get();
        if (autenticado != null && autenticado != id) {
            throw new SecurityException("El responsable no coincide con la sesion autenticada");
        }
    }
}
