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
    // sirve para UNA operacion: el BO que la usa la consume (queda USADA) en su misma transaccion.
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

    public static final int VIGENCIA_MAXIMA_MINUTOS = 1440;

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

            // RF014: se pueden tener varias pendientes de la misma operacion (por ejemplo, notas de credito de
            // comprobantes distintos: el motivo dice cual). Solo se rechaza la MISMA solicitud repetida (doble envio).
            boolean repetida = solicitudDAO.listarPorSolicitante(idSolicitante).stream()
                    .anyMatch(s -> s.getEstado() == EstadoAutorizacion.PENDIENTE
                            && s.getOperacionRestringida().equals(operacion)
                            && motivoValidado.equalsIgnoreCase(s.getMotivo() == null ? "" : s.getMotivo().trim()));
            if(repetida){
                throw new IllegalStateException("Ya enviaste esta misma solicitud; espera la respuesta del Administrador");
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
            // RF014: el permiso es temporal; como maximo un dia (misma regla que la pantalla AprobarSolicitud)
            if(vigenciaMinutos > VIGENCIA_MAXIMA_MINUTOS){
                throw new IllegalArgumentException("La vigencia no puede pasar de " + VIGENCIA_MAXIMA_MINUTOS + " minutos (un día)");
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

    // RF014: APROBADA -> USADA (sp_solicitud_autorizacion_usar). Antes quedaba VENCIDA y no se distinguia de una
    // que paso su vigencia o que se revoco. Corre dentro de la transaccion de quien la usa: si esa operacion
    // falla hay rollback y la autorizacion sigue APROBADA (no hay que volver a pedirla)
    @Override
    public void consumir(int idSolicitud) throws SQLException {
        Transaccion.ejecutar(() -> {
            SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
            if(solicitud.getEstado() != EstadoAutorizacion.APROBADA){
                throw new IllegalStateException("Solo se puede consumir una autorización aprobada (estado: " + solicitud.getEstado() + ")");
            }
            solicitudDAO.marcarUsada(idSolicitud);
            return null;
        });
    }

    // ================= RF014: retomar la operacion con su solicitud =================

    // tope del JSON de la pantalla (la columna es TEXT: 64 KB)
    private static final int DATOS_OPERACION_MAXIMO = 60000;

    @Override
    public void guardarDatosOperacion(int idSolicitud, int idSolicitante, String datosOperacion) throws SQLException {
        if (datosOperacion == null || datosOperacion.isBlank()) {
            return; // la pantalla no mando datos: la solicitud vale igual, solo no se podra retomar
        }
        if (datosOperacion.length() > DATOS_OPERACION_MAXIMO) {
            throw new IllegalArgumentException("Los datos de la operación son demasiado grandes para guardarlos");
        }
        Transaccion.ejecutar(() -> {
            SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
            if (solicitud.getSolicitante() == null || solicitud.getSolicitante().getId() != idSolicitante) {
                throw new IllegalStateException("Solo quien pidió la autorización guarda los datos de su operación");
            }
            if (solicitud.getEstado() != EstadoAutorizacion.PENDIENTE && solicitud.getEstado() != EstadoAutorizacion.APROBADA) {
                return null; // ya resuelta: no se cambia
            }
            solicitudDAO.guardarDatosOperacion(idSolicitud, datosOperacion);
            // se volvio a pedir la MISMA operacion (misma huella) despues de un rechazo o un vencimiento: la solicitud
            // anterior ya no se retoma (si no, la operacion apareceria dos veces en la lista)
            LocalDateTime ahora = LocalDateTime.now();
            for (SolicitudAutorizacion anterior : solicitudDAO.listarPorSolicitante(idSolicitante)) {
                boolean mismaOperacion = anterior.getId() != idSolicitud && anterior.getDatosOperacion() != null
                        && solicitud.getHuella() != null && solicitud.getHuella().equals(anterior.getHuella())
                        && solicitud.getOperacionRestringida().equals(anterior.getOperacionRestringida());
                boolean yaNoSirve = anterior.getEstado() == EstadoAutorizacion.RECHAZADA
                        || anterior.getEstado() == EstadoAutorizacion.VENCIDA
                        || (anterior.getEstado() == EstadoAutorizacion.APROBADA && anterior.getFechaVencimiento() != null
                            && !anterior.getFechaVencimiento().isAfter(ahora));
                if (mismaOperacion && yaNoSirve) {
                    solicitudDAO.guardarDatosOperacion(anterior.getId(), null);
                }
            }
            return null;
        });
    }

    @Override
    public List<SolicitudAutorizacion> listarEnCurso(int idSolicitante, String operacionRestringida) throws SQLException {
        return Transaccion.consultar(() -> {
            List<SolicitudAutorizacion> todas = idSolicitante > 0
                    ? solicitudDAO.listarPorSolicitante(idSolicitante) : solicitudDAO.listarTodos();
            String operacion = operacionRestringida == null || operacionRestringida.isBlank() ? null
                    : operacionRestringida.trim().toUpperCase(java.util.Locale.ROOT);
            return todas.stream()
                    .filter(s -> s.getDatosOperacion() != null && s.getEstado() != EstadoAutorizacion.USADA)
                    .filter(s -> operacion == null || operacion.equals(s.getOperacionRestringida()))
                    .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
                    .toList();
        });
    }

    @Override
    public void descartar(int idSolicitud, int idResponsable) throws SQLException {
        Transaccion.ejecutar(() -> {
            SolicitudAutorizacion solicitud = obtenerExistente(idSolicitud);
            if (solicitud.getSolicitante() == null || solicitud.getSolicitante().getId() != idResponsable) {
                throw new IllegalStateException("Solo quien pidió la autorización puede descartarla");
            }
            if (solicitud.getEstado() == EstadoAutorizacion.USADA) {
                throw new IllegalStateException("Esta autorización ya se usó en su operación");
            }
            if (solicitud.getEstado() == EstadoAutorizacion.PENDIENTE || solicitud.getEstado() == EstadoAutorizacion.APROBADA) {
                solicitudDAO.remove(solicitud); // pendiente: queda cancelada; aprobada: ya no se puede usar
            }
            solicitudDAO.guardarDatosOperacion(idSolicitud, null);
            return null;
        });
    }

    // ================= RF014 opcion B: la solicitud lleva la operacion =================

    // Mensaje que marca un error que se resuelve pidiendo autorizacion: la pantalla lo reconoce y ofrece
    // el boton "Solicitar autorizacion" con los mismos datos de la operacion.
    public static final String MARCA_REQUIERE_AUTORIZACION = "requiere autorización del Administrador";

    // SHA-256 (hex) de los datos de una operacion. Cada BO arma el texto con los datos que importan
    // (por ejemplo, el comprobante y las cantidades devueltas): si cambian, la huella cambia.
    public static String huella(String datos) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(datos.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    @Override
    public SolicitudAutorizacion solicitarOperacion(int idSolicitante, String operacionRestringida, String motivo,
                                                    String referencia, String detalle, String huella) throws SQLException {
        Validador.obligatorio(huella, "datos de la operación");
        return Transaccion.ejecutar(() -> {
            Usuario solicitante = usuarioBO.obtener(idSolicitante);
            if (solicitante == null || !solicitante.isEstado()) {
                throw new IllegalArgumentException("El solicitante no existe o está desactivado");
            }
            String operacion = operacionRestringida.toUpperCase(java.util.Locale.ROOT);
            TipoRol rolDeLaOperacion = ROL_POR_OPERACION.get(operacion);
            if (rolDeLaOperacion == null) {
                throw new IllegalArgumentException("La operación restringida no existe");
            }
            if (rolDeLaOperacion != solicitante.getRol().getTipo()) {
                throw new IllegalStateException("El rol " + solicitante.getRol().getTipo()
                        + " no realiza la operación " + operacion);
            }
            // la misma operacion ya pedida: no se duplica (doble clic o se volvio a la pantalla)
            SolicitudAutorizacion existente = solicitudDAO.buscarPorHuella(idSolicitante, operacion, huella, EstadoAutorizacion.APROBADA);
            if (existente == null) {
                existente = solicitudDAO.buscarPorHuella(idSolicitante, operacion, huella, EstadoAutorizacion.PENDIENTE);
            }
            if (existente != null) {
                return existente;
            }
            SolicitudAutorizacion solicitud = new SolicitudAutorizacion();
            solicitud.setSolicitante(solicitante);
            solicitud.setOperacionRestringida(operacion);
            solicitud.setMotivo(Validador.textoObligatorio(motivo, "motivo", 200));
            solicitud.setFechaSolicitud(LocalDateTime.now());
            solicitud.setEstado(EstadoAutorizacion.PENDIENTE);
            solicitud.setVigenciaMinutos(0);
            solicitud.setReferencia(Validador.textoOpcional(recortar(referencia, 80), "referencia", 80));
            solicitud.setDetalle(recortar(detalle, 1000));
            solicitud.setHuella(huella);
            return solicitudDAO.save(solicitud);
        });
    }

    @Override
    public SolicitudAutorizacion requerirAutorizacion(int idUsuario, String operacionRestringida, String huella) throws SQLException {
        return Transaccion.ejecutar(() -> {
            SolicitudAutorizacion aprobada = solicitudDAO.buscarPorHuella(idUsuario, operacionRestringida, huella, EstadoAutorizacion.APROBADA);
            if (aprobada != null && aprobada.getAdministrador() != null) {
                Usuario administrador = usuarioBO.obtener(aprobada.getAdministrador().getId());
                if (administrador != null && administrador.isEstado() && administrador.getRol().isEstado()
                        && administrador.getRol().getTipo() == TipoRol.ADMINISTRADOR) {
                    aprobada.setAdministrador(administrador);
                    aprobada.setSolicitante(usuarioBO.obtener(idUsuario));
                    return aprobada;
                }
            }
            if (solicitudDAO.buscarPorHuella(idUsuario, operacionRestringida, huella, EstadoAutorizacion.PENDIENTE) != null) {
                throw new IllegalStateException("La solicitud de autorización de esta operación está en espera del Administrador");
            }
            throw new IllegalStateException("Esta operación " + MARCA_REQUIERE_AUTORIZACION
                    + ". Use «Solicitar autorización»: el Administrador verá estos mismos datos");
        });
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.length() <= maximo ? t : t.substring(0, maximo - 1) + "…";
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
