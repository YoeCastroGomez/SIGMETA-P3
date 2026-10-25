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
    private final long expira;
    private volatile boolean cerrada;
    private static final ThreadLocal<Integer> ACTUAL = new ThreadLocal<>();
    private SesionServicio(int id) {
        idUsuario = id;
        expira = System.currentTimeMillis() + 30L * 60 * 1000;
    }
    public static SesionServicio iniciar(String nombre, String clave) throws SQLException {
        Usuario usuario = new UsuarioBOImpl().iniciarSesion(nombre, clave);
        return new SesionServicio(usuario.getId());
    }
    @FunctionalInterface
    public interface Accion<T> { T ejecutar(int idAutenticado) throws SQLException; }
    public <T> T ejecutar(Accion<T> accion) throws SQLException {
        if (cerrada || System.currentTimeMillis() >= expira) {
            throw new SecurityException("Sesion cerrada o expirada");
        }
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
    public void cerrar() { cerrada = true; }
    public static void verificarResponsable(int id) {
        Integer autenticado = ACTUAL.get();
        if (autenticado != null && autenticado != id) {
            throw new SecurityException("El responsable no coincide con la sesion autenticada");
        }
    }
}
