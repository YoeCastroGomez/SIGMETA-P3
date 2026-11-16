package pe.edu.pucp.sigmeta.boimpl.seguridad;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.bo.seguridad.SolicitudAutorizacionBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.seguridad.SolicitudAutorizacionDAO;
import pe.edu.pucp.sigmeta.daoimpl.seguridad.SolicitudAutorizacionDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoAutorizacion;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class SolicitudAutorizacionBOImpl implements SolicitudAutorizacionBO {

    // RF014: operaciones restringidas del sistema. Cada una la pide un solo rol y, una vez aprobada,
    // sirve para UNA operacion: el BO que la usa la consume (queda VENCIDA) en su misma transaccion.
    public static final String OPERACION_DESCUENTO_ESPECIAL = "DESCUENTO_ESPECIAL";   // Vendedor: vender bajo el costo
    public static final String OPERACION_NOTA_CREDITO = "EMITIR_NOTA_CREDITO";         // Cajero: nota de credito
    public static final String OPERACION_AJUSTE_INVENTARIO = "AJUSTE_INVENTARIO";      // Almacenero: ajuste por conteo

    private static final java.util.Map<String, TipoRol> ROL_POR_OPERACION = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, String> DESCRIPCION_POR_OPERACION = new java.util.LinkedHashMap<>();
    static {
        ROL_POR_OPERACION.put(OPERACION_DESCUENTO_ESPECIAL, TipoRol.VENDEDOR);
        ROL_POR_OPERACION.put(OPERACION_NOTA_CREDITO, TipoRol.CAJERO);
        ROL_POR_OPERACION.put(OPERACION_AJUSTE_INVENTARIO, TipoRol.ALMACENERO);
        DESCRIPCION_POR_OPERACION.put(OPERACION_DESCUENTO_ESPECIAL,
                "Registrar una venta con una línea por debajo del costo del producto");
        DESCRIPCION_POR_OPERACION.put(OPERACION_NOTA_CREDITO,
                "Emitir una nota de crédito por devolución de mercadería");
        DESCRIPCION_POR_OPERACION.put(OPERACION_AJUSTE_INVENTARIO,
                "Registrar o revertir un ajuste de inventario por conteo físico");
    }

    // operaciones que puede pedir un rol: codigo -> descripcion
    public static java.util.Map<String, String> operacionesDe(TipoRol rol) {
        java.util.Map<String, String> resultado = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<String, TipoRol> e : ROL_POR_OPERACION.entrySet()) {
            if (rol == null || e.getValue() == rol) {
                resultado.put(e.getKey(), DESCRIPCION_POR_OPERACION.get(e.getKey()));
            }
        }
        return resultado;
    }

    public static String descripcionDe(String operacion) {
        String descripcion = DESCRIPCION_POR_OPERACION.get(operacion);
        return descripcion != null ? descripcion : operacion;
    }

    private final SolicitudAutorizacionDAO solicitudDAO;
    private final UsuarioBO usuarioBO;

    public SolicitudAutorizacionBOImpl() {
        this.solicitudDAO = new SolicitudAutorizacionDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF014
    @Override
    public SolicitudAutorizacion solicitar(int idSolicitante, String operacionRestringida, String motivo) throws SQLException {
        // RF001: se solicita siempre a nombre propio (el usuario de la sesion)
        pe.edu.pucp.sigmeta.seguridad.SesionServicio.verificarResponsable(idSolicitante);
        return Transaccion.ejecutar(() -> {
            Usuario solicitante = usuarioBO.obtener(idSolicitante);
            if(solicitante == null || !solicitante.isEstado()){
                throw new IllegalArgumentException("El solicitante no existe o está desactivado");
            }
            // RF014: solicitan el Vendedor, el Cajero y el Almacenero
            if(solicitante.getRol().getTipo() == TipoRol.ADMINISTRADOR){
                throw new IllegalStateException("El Administrador no necesita solicitar autorización");
            }
            String operacion = Validador.textoObligatorio(operacionRestringida, "operación restringida", 60)
                    .toUpperCase(java.util.Locale.ROOT);
            TipoRol rolDeLaOperacion = ROL_POR_OPERACION.get(operacion);
            if (rolDeLaOperacion == null) {
                throw new IllegalArgumentException("La operación restringida no existe. Elija una de la lista");
            }
            if (rolDeLaOperacion != solicitante.getRol().getTipo()) {
                throw new IllegalStateException("El rol " + solicitante.getRol().getTipo()
                        + " no realiza la operación " + operacion + "; no necesita esa autorización");
            }
            String motivoValidado = Validador.textoObligatorio(motivo, "motivo", 200);

            boolean yaHayPendiente = solicitudDAO.listarPorSolicitante(idSolicitante).stream()
                    .anyMatch(s -> s.getEstado() == EstadoAutorizacion.PENDIENTE
                            && s.getOperacionRestringida().equals(operacion));
            if(yaHayPendiente){
                throw new IllegalStateException("Ya existe una solicitud pendiente para esta operación");
            }

            SolicitudAutorizacion solicitud = new SolicitudAutorizacion();
            solicitud.setSolicitante(solicitante);
            solicitud.setOperacionRestringida(operacion);
            solicitud.setMotivo(motivoValidado);
            solicitud.setFechaSolicitud(LocalDateTime.now());
            solicitud.setEstado(EstadoAutorizacion.PENDIENTE);
            solicitud.setVigenciaMinutos(0);
            return solicitudDAO.save(solicitud);
        });
    }

    // RF014
    @Override
    public SolicitudAutorizacion aprobar(int idSolicitud, int idAdministrador, int vigenciaMinutos) throws SQLException {
        return Transaccion.ejecutar(() -> {
            if(vigenciaMinutos <= 0){
                throw new IllegalArgumentException("La vigencia debe ser mayor a 0 minutos");
            }
            Usuario administrador = usuarioBO.obtenerAdministradorActivo(idAdministrador);
            SolicitudAutorizacion solicitud = obtenerPendiente(idSolicitud);
            LocalDateTime ahora = LocalDateTime.now();
            solicitud.setAdministrador(administrador);
            solicitud.setEstado(EstadoAutorizacion.APROBADA);
            solicitud.setFechaResolucion(ahora);
            solicitud.setVigenciaMinutos(vigenciaMinutos);
            solicitud.setFechaVencimiento(ahora.plusMinutes(vigenciaMinutos));
            return solicitudDAO.update(solicitud);
        });
    }

    // RF014
    @Override
    public SolicitudAutorizacion rechazar(int idSolicitud, int idAdministrador) throws SQLException {
        return Transaccion.ejecutar(() -> {
            Usuario administrador = usuarioBO.obtenerAdministradorActivo(idAdministrador);
            SolicitudAutorizacion solicitud = obtenerPendiente(idSolicitud);
            solicitud.setAdministrador(administrador);
            solicitud.setEstado(EstadoAutorizacion.RECHAZADA);
            solicitud.setFechaResolucion(LocalDateTime.now());
            return solicitudDAO.update(solicitud);
        });
    }

    // una pendiente queda rechazada y una aprobada queda vencida (baja logica en sp_solicitud_autorizacion_eliminar)
    // RF014, RNF002
    @Override
    public void anular(int idSolicitud, int idResponsable) throws SQLException {
        Transaccion.ejecutar(() -> {
            SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
            // la anula quien la pidio (ya no la necesita) o un Administrador (revoca el permiso)
            Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.values());
            if(responsable.getId() != solicitud.getSolicitante().getId()
                    && responsable.getRol().getTipo() != TipoRol.ADMINISTRADOR){
                throw new IllegalStateException("Solo el solicitante o un Administrador pueden anular la solicitud");
            }
            if(solicitud.getEstado() != EstadoAutorizacion.PENDIENTE && solicitud.getEstado() != EstadoAutorizacion.APROBADA){
                throw new IllegalStateException("Solo se puede anular una solicitud pendiente o aprobada");
            }
            solicitudDAO.remove(solicitud);
            return null;
        });
    }

    // RF014
    @Override
    public SolicitudAutorizacion obtener(int idSolicitud) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return solicitudDAO.load(idSolicitud);
        });
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return solicitudDAO.listarTodos();
        });
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarPendientes() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return solicitudDAO.listarPorEstado(EstadoAutorizacion.PENDIENTE);
        });
    }

    // RF014
    @Override
    public List<SolicitudAutorizacion> listarPorSolicitante(int idSolicitante) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return solicitudDAO.listarPorSolicitante(idSolicitante);
        });
    }

    // RF014
    @Override
    public boolean tieneAutorizacionVigente(int idUsuario, String operacionRestringida) throws SQLException {
        return Transaccion.ejecutar(() -> {
            String operacion = Validador.textoObligatorio(operacionRestringida, "operación restringida", 60);
            return solicitudDAO.buscarVigente(idUsuario, operacion) != null;
        });
    }

    // RF013/RF014: la autorizacion se toma de la solicitud que el Administrador aprobo en su propia sesion,
    // asi la operacion se ejecuta en la sesion del solicitante sin que el Administrador este conectado
    @Override
    public SolicitudAutorizacion obtenerAutorizacionVigente(int idUsuario, String operacionRestringida) throws SQLException {
        return Transaccion.ejecutar(() -> {
            String operacion = Validador.textoObligatorio(operacionRestringida, "operación restringida", 60);
            SolicitudAutorizacion vigente = solicitudDAO.buscarVigente(idUsuario, operacion);
            if(vigente == null || vigente.getAdministrador() == null){
                return null;
            }
            Usuario administrador = usuarioBO.obtener(vigente.getAdministrador().getId());
            if(administrador == null || !administrador.isEstado() || !administrador.getRol().isEstado()
                    || administrador.getRol().getTipo() != TipoRol.ADMINISTRADOR){
                // el administrador que aprobo ya no esta activo: la autorizacion no sirve
                return null;
            }
            vigente.setAdministrador(administrador);
            vigente.setSolicitante(usuarioBO.obtener(idUsuario));
            return vigente;
        });
    }

    // RF014: APROBADA -> VENCIDA (sp_solicitud_autorizacion_eliminar). Corre dentro de la transaccion
    // de quien la usa: si esa operacion falla hay rollback y la autorizacion sigue APROBADA
    @Override
    public void consumir(int idSolicitud) throws SQLException {
        Transaccion.ejecutar(() -> {
            SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
            if(solicitud.getEstado() != EstadoAutorizacion.APROBADA){
                throw new IllegalStateException("Solo se puede consumir una autorización aprobada (estado: " + solicitud.getEstado() + ")");
            }
            solicitudDAO.remove(solicitud);
            return null;
        });
    }

    private SolicitudAutorizacion obtenerExistente(int idSolicitud) throws SQLException {
        SolicitudAutorizacion solicitud = solicitudDAO.load(idSolicitud);
        if(solicitud == null){
            throw new IllegalArgumentException("No existe la solicitud con id " + idSolicitud);
        }
        return solicitud;
    }

    private SolicitudAutorizacion obtenerPendiente(int idSolicitud) throws SQLException {
        SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
        if(solicitud.getEstado() != EstadoAutorizacion.PENDIENTE){
            throw new IllegalStateException("La solicitud ya fue resuelta (" + solicitud.getEstado() + ")");
        }
        return solicitud;
    }
}
