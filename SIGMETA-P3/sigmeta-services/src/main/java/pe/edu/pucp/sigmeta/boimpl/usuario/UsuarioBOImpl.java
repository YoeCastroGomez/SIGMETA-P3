package pe.edu.pucp.sigmeta.boimpl.seguridad;

import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.seguridad.RolDAO;
import pe.edu.pucp.sigmeta.dao.seguridad.UsuarioDAO;
import pe.edu.pucp.sigmeta.daoimpl.seguridad.RolDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.seguridad.UsuarioDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.usuario.Rol;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class UsuarioBOImpl implements UsuarioBO {

    private static final int LONGITUD_MINIMA_CLAVE = 8;
    // mismo mensaje si el usuario no existe o la clave es incorrecta, para no revelar que usuarios existen
    private static final String CREDENCIALES_INVALIDAS = "Usuario o clave incorrectos";

    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;

    public UsuarioBOImpl() {
        this.usuarioDAO = new UsuarioDAOImpl();
        this.rolDAO = new RolDAOImpl();
    }



    // RF001
    @Override
    public Usuario iniciarSesion(String nombreUsuario, String clave) throws SQLException {
        if(nombreUsuario == null || nombreUsuario.isBlank() || clave == null || clave.isEmpty()){
            throw new IllegalArgumentException(CREDENCIALES_INVALIDAS);
        }
        Usuario usuario = usuarioDAO.buscarPorNombreUsuario(nombreUsuario.trim());
        if(usuario == null || !GeneradorHash.verificar(clave, usuario.getSalt(), usuario.getClaveHash())){
            throw new IllegalArgumentException(CREDENCIALES_INVALIDAS);
        }
        if(!usuario.isEstado()){
            throw new IllegalStateException("El usuario se encuentra desactivado");
        }
        if(!usuario.getRol().isEstado()){
            throw new IllegalStateException("El rol del usuario se encuentra desactivado");
        }
        return usuario;
    }

    // RF002
    @Override
    public Usuario registrar(Usuario usuario, String clave, int idAdministrador) throws SQLException {
        obtenerAdministradorActivo(idAdministrador);
        validarDatos(usuario);
        validarNombreUsuarioUnico(usuario);
        validarClave(clave);
        usuario.setSalt(GeneradorHash.generarSalt());
        usuario.setClaveHash(GeneradorHash.calcularHash(clave, usuario.getSalt()));
        usuario.setFechaRegistro(LocalDateTime.now());
        return usuarioDAO.save(usuario);
    }

    // RF002
    @Override
    public Usuario modificar(Usuario usuario, int idAdministrador) throws SQLException {
        obtenerAdministradorActivo(idAdministrador);
        Validador.obligatorio(usuario, "usuario");
        Usuario actual = obtenerExistente(usuario.getId());
        validarDatos(usuario);
        validarNombreUsuarioUnico(usuario);
        boolean sigueSiendoAdministrador = usuario.isEstado()
                && usuario.getRol().getTipo() == TipoRol.ADMINISTRADOR;
        if(!sigueSiendoAdministrador){
            validarQueQuedeOtroAdministrador(actual);
        }
        // la clave no se modifica aqui, solo con cambiarClave
        usuario.setClaveHash(actual.getClaveHash());
        usuario.setSalt(actual.getSalt());
        usuario.setFechaRegistro(actual.getFechaRegistro());
        return usuarioDAO.update(usuario);
    }

    // el propio usuario cambia su clave: debe confirmar la actual
    // RF002, RNF001
    @Override
    public void cambiarClave(int idUsuario, String claveActual, String claveNueva) throws SQLException {
        Usuario usuario = obtenerExistente(idUsuario);
        if(claveActual == null || !GeneradorHash.verificar(claveActual, usuario.getSalt(), usuario.getClaveHash())){
            throw new IllegalArgumentException("La clave actual es incorrecta");
        }
        if(claveActual.equals(claveNueva)){
            throw new IllegalArgumentException("La clave nueva debe ser distinta de la actual");
        }
        guardarClave(usuario, claveNueva);
    }

    // el Administrador asigna una clave nueva a un usuario que olvido la suya (RF002)
    // RF002, RNF001
    @Override
    public void restablecerClave(int idUsuario, String claveNueva, int idAdministrador) throws SQLException {
        obtenerAdministradorActivo(idAdministrador);
        guardarClave(obtenerExistente(idUsuario), claveNueva);
    }

    // RF002, RNF002
    @Override
    public void desactivar(int idUsuario, int idAdministrador) throws SQLException {
        obtenerAdministradorActivo(idAdministrador);
        Usuario usuario = obtenerExistente(idUsuario);
        validarQueQuedeOtroAdministrador(usuario);
        usuarioDAO.remove(usuario);
    }

    // RF002
    @Override
    public Usuario obtener(int idUsuario) throws SQLException {
        return usuarioDAO.load(idUsuario);
    }

    // RF002
    @Override
    public List<Usuario> listarTodos() throws SQLException {
        return usuarioDAO.listarTodos();
    }

    // RF002
    @Override
    public List<Usuario> listarPorEstado(boolean estado) throws SQLException {
        return usuarioDAO.listarPorEstado(estado);
    }

    // RF001: el control por rol se hace aqui y no solo en la interfaz, porque los
    // servicios web se pueden llamar directamente sin pasar por los menus
    @Override
    public Usuario verificarRol(int idUsuario, TipoRol... rolesPermitidos) throws SQLException {
        Usuario usuario = usuarioDAO.load(idUsuario);
        if(usuario == null || !usuario.isEstado() || !usuario.getRol().isEstado()){
            throw new IllegalStateException("La operacion requiere un usuario activo");
        }
        for(TipoRol rol : rolesPermitidos){
            if(usuario.getRol().getTipo() == rol){
                return usuario;
            }
        }
        throw new IllegalStateException("El rol " + usuario.getRol().getTipo()
                + " no tiene permiso para realizar esta operacion");
    }

    // RF001
    @Override
    public Usuario obtenerAdministradorActivo(int idUsuario) throws SQLException {
        return verificarRol(idUsuario, TipoRol.ADMINISTRADOR);
    }

    private Usuario obtenerExistente(int idUsuario) throws SQLException {
        Usuario usuario = usuarioDAO.load(idUsuario);
        if(usuario == null){
            throw new IllegalArgumentException("No existe el usuario con id " + idUsuario);
        }
        return usuario;
    }

    private void guardarClave(Usuario usuario, String claveNueva) throws SQLException {
        validarClave(claveNueva);
        usuario.setSalt(GeneradorHash.generarSalt());
        usuario.setClaveHash(GeneradorHash.calcularHash(claveNueva, usuario.getSalt()));
        usuarioDAO.update(usuario);
    }

    private void validarDatos(Usuario usuario) throws SQLException {
        Validador.obligatorio(usuario, "usuario");
        String nombreUsuario = Validador.textoObligatorio(usuario.getNombreUsuario(), "nombre de usuario", 50);
        if(!nombreUsuario.matches("^[A-Za-z0-9._-]+$")){
            throw new IllegalArgumentException("El nombre de usuario solo puede contener letras, digitos, '.', '_' y '-'");
        }
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setNombres(Validador.textoObligatorio(usuario.getNombres(), "nombres", 100));
        usuario.setApellidos(Validador.textoObligatorio(usuario.getApellidos(), "apellidos", 100));
        usuario.setCorreo(Validador.correo(usuario.getCorreo()));

        // RF002: un unico rol, que debe existir y estar activo
        Validador.obligatorio(usuario.getRol(), "rol");
        Rol rol = rolDAO.load(usuario.getRol().getId());
        if(rol == null || !rol.isEstado()){
            throw new IllegalArgumentException("El rol seleccionado no existe o esta desactivado");
        }
        usuario.setRol(rol);
    }

    private void validarNombreUsuarioUnico(Usuario usuario) throws SQLException {
        Usuario existente = usuarioDAO.buscarPorNombreUsuario(usuario.getNombreUsuario());
        if(existente != null && existente.getId() != usuario.getId()){
            throw new IllegalArgumentException("El nombre de usuario ya esta registrado");
        }
    }

    // RNF001: minimo 8 caracteres con al menos una letra y un numero
    private void validarClave(String clave) {
        if(clave == null || clave.length() < LONGITUD_MINIMA_CLAVE){
            throw new IllegalArgumentException("La clave debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres");
        }
        if(!clave.matches(".*[A-Za-z].*") || !clave.matches(".*\\d.*")){
            throw new IllegalArgumentException("La clave debe incluir al menos una letra y un numero");
        }
    }

    // evita que el sistema quede sin ningun Administrador activo
    private void validarQueQuedeOtroAdministrador(Usuario actual) throws SQLException {
        if(!actual.isEstado() || actual.getRol().getTipo() != TipoRol.ADMINISTRADOR){
            return;
        }
        long administradoresActivos = usuarioDAO.listarPorEstado(true).stream()
                .filter(u -> u.getRol().getTipo() == TipoRol.ADMINISTRADOR)
                .count();
        if(administradoresActivos <= 1){
            throw new IllegalStateException("No se puede quitar al unico Administrador activo del sistema");
        }
    }
}
