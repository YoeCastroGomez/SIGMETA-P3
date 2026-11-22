package pe.edu.pucp.sigmeta.ws.seguridad;

// RF014: solicitud de autorizacion tal como viaja en JSON (lo que responde /api/solicitudes).
//
// Lo que Blazor manda:
//   solicitar:  {"operacion": "EMITIR_NOTA_CREDITO", "motivo": "Devolucion de 2 alicates con falla"}
//   aprobar:    {"vigenciaMinutos": 30}
// El solicitante y el administrador no se mandan: son el usuario de la sesion.
public class SolicitudDatos {
    public int id;
    public int idSolicitante;
    public String solicitante;          // nombre de usuario
    public String nombreSolicitante;    // nombres y apellidos
    public String rolSolicitante;
    public String operacion;
    public String descripcionOperacion;
    public String motivo;
    public String fechaSolicitud;
    public String estado;               // PENDIENTE, APROBADA, RECHAZADA, VENCIDA, USADA
    public boolean vigente;             // aprobada y todavia dentro de su vigencia
    public String administrador;
    public String fechaResolucion;
    public int vigenciaMinutos;
    public String fechaVencimiento;
    // RF014: la operacion que se autoriza (documento o producto y lo que revisa el Administrador)
    public String referencia;
    public String detalle;
    // RF014: lo que el solicitante preparo en su pantalla (JSON) para retomar la operacion; null si no se puede
    public String datosOperacion;
    // RF014: la cancelo su solicitante ("Cambiar datos" o "Descartar"); no la rechazo un Administrador
    public boolean cancelada;

    public SolicitudDatos() {
    }
}
