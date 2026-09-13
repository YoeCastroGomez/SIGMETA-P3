package pe.edu.pucp.model.seguridad;

import java.time.LocalDateTime;
import pe.edu.pucp.model.enums.EstadoAutorizacion;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Solicitud de autorizacion para una operacion restringida (RF014).
 */
public class SolicitudAutorizacion {
    private int idSolicitudAutorizacion;
    private Usuario solicitante;
    private Usuario administrador;
    private String operacionRestringida;
    private String motivo;
    private LocalDateTime fechaSolicitud;
    private EstadoAutorizacion estado;
    private LocalDateTime fechaResolucion;
    private int vigenciaMinutos;
    private LocalDateTime fechaVencimiento;

    public SolicitudAutorizacion() {
    }

    public SolicitudAutorizacion(int idSolicitudAutorizacion, Usuario solicitante,
                                    Usuario administrador, String operacionRestringida,
                                    String motivo, LocalDateTime fechaSolicitud,
                                    EstadoAutorizacion estado, LocalDateTime fechaResolucion,
                                    int vigenciaMinutos, LocalDateTime fechaVencimiento) {
        this.idSolicitudAutorizacion = idSolicitudAutorizacion;
        this.solicitante = solicitante;
        this.administrador = administrador;
        this.operacionRestringida = operacionRestringida;
        this.motivo = motivo;
        this.fechaSolicitud = fechaSolicitud;
        this.estado = estado;
        this.fechaResolucion = fechaResolucion;
        this.vigenciaMinutos = vigenciaMinutos;
        this.fechaVencimiento = fechaVencimiento;
    }

    public int getIdSolicitudAutorizacion() {
        return idSolicitudAutorizacion;
    }

    public void setIdSolicitudAutorizacion(int idSolicitudAutorizacion) {
        this.idSolicitudAutorizacion = idSolicitudAutorizacion;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
    }

    public Usuario getAdministrador() {
        return administrador;
    }

    public void setAdministrador(Usuario administrador) {
        this.administrador = administrador;
    }

    public String getOperacionRestringida() {
        return operacionRestringida;
    }

    public void setOperacionRestringida(String operacionRestringida) {
        this.operacionRestringida = operacionRestringida;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public EstadoAutorizacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoAutorizacion estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public int getVigenciaMinutos() {
        return vigenciaMinutos;
    }

    public void setVigenciaMinutos(int vigenciaMinutos) {
        this.vigenciaMinutos = vigenciaMinutos;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDateTime fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}
