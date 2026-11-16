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
    public String estado;               // PENDIENTE, APROBADA, RECHAZADA, VENCIDA
    public boolean vigente;             // aprobada y todavia dentro de su vigencia
    public String administrador;
    public String fechaResolucion;
    public int vigenciaMinutos;
    public String fechaVencimiento;

    public SolicitudDatos() {
    }
}
