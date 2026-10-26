package pe.edu.pucp.sigmeta.model.comun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

/**
 * Superclase de los documentos del ciclo comercial. Concentra numeracion, montos y auditoria.
 */
public abstract class DocumentoComercial implements Auditable, Anulable {

    /** Moneda fija de todos los documentos comerciales: soles peruanos (PEN). */
    public static final String MONEDA = "PEN";

    public String getMoneda() {
        return MONEDA;
    }

    private String numero;
    private LocalDate fechaEmision;
    private double subTotal;
    private double igv;
    private double total;
    private String observaciones;
    private LocalDateTime fechaRegistro;
    private Usuario usuarioRegistro;
    private boolean anulado;
    private String motivoAnulacion;
    private LocalDateTime fechaAnulacion;

    public DocumentoComercial() {
    }

    public DocumentoComercial(String numero, LocalDate fechaEmision, double subTotal, double igv, double total, String observaciones, LocalDateTime fechaRegistro, Usuario usuarioRegistro, boolean anulado, String motivoAnulacion, LocalDateTime fechaAnulacion) {
        this.numero = numero;
        this.fechaEmision = fechaEmision;
        this.subTotal = subTotal;
        this.igv = igv;
        this.total = total;
        this.observaciones = observaciones;
        this.fechaRegistro = fechaRegistro;
        this.usuarioRegistro = usuarioRegistro;
        this.anulado = anulado;
        this.motivoAnulacion = motivoAnulacion;
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }


    public double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public double getIgv() {
        return igv;
    }

    public void setIgv(double igv) {
        this.igv = igv;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public boolean isAnulado() {
        return anulado;
    }

    public void setAnulado(boolean anulado) {
        this.anulado = anulado;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

}
