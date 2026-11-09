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
    // RF001: la sesion vence tras 8 horas SIN USO; cada pedido la renueva
    public static final long INACTIVIDAD_MAXIMA_MS = 8L * 60 * 60 * 1000;
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
        if (cerrada || System.currentTimeMillis() >= expira) {
            throw new SecurityException("Sesion cerrada o expirada");
        }
        renovar();
        if (ACTUAL.get() != null) throw new SecurityException("No se permite sustituir una sesion activa");
        ACTUAL.set(idUsuario);
        try {
            return Transaccion.ejecutar(() -> {
                Usuario usuario = new UsuarioBOImpl().obtener(idUsuario);
                if (usuario == null || !usuario.isEstado() || !usuario.getRol().isEstado()) {
                    throw new SecurityException("Usuario o rol inactivo");
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
