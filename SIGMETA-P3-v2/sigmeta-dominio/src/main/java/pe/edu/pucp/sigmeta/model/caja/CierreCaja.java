package pe.edu.pucp.sigmeta.model.caja;

import java.time.LocalDateTime;
import pe.edu.pucp.sigmeta.model.enums.EstadoCierreCaja;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

/**
 * Cierre y cuadre de una jornada de caja (RF010).
 */
public class CierreCaja {
    private int id;
    private Caja caja;
    private LocalDateTime fechaCierre;
    private double montoCalculado;
    private double montoDeclarado;
    private double diferencia;
    private Usuario usuarioCierre;
    // RF010: el cierre persiste; reabrir la caja lo marca REVERTIDO con su responsable
    private EstadoCierreCaja estado;
    private LocalDateTime fechaReapertura;
    private Usuario usuarioReapertura;
    private String motivoReapertura;

    public CierreCaja() {
        this.estado = EstadoCierreCaja.VIGENTE;
    }

    public CierreCaja(int id, Caja caja, LocalDateTime fechaCierre,
                         double montoCalculado, double montoDeclarado, double diferencia,
                         Usuario usuarioCierre) {
        this.id = id;
        this.caja = caja;
        this.fechaCierre = fechaCierre;
        this.montoCalculado = montoCalculado;
        this.montoDeclarado = montoDeclarado;
        this.diferencia = diferencia;
        this.usuarioCierre = usuarioCierre;
        this.estado = EstadoCierreCaja.VIGENTE;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Caja getCaja() {
        return caja;
    }

    public void setCaja(Caja caja) {
        this.caja = caja;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public double getMontoCalculado() {
        return montoCalculado;
    }

    public void setMontoCalculado(double montoCalculado) {
        this.montoCalculado = montoCalculado;
    }

    public double getMontoDeclarado() {
        return montoDeclarado;
    }

    public void setMontoDeclarado(double montoDeclarado) {
        this.montoDeclarado = montoDeclarado;
    }

    public double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(double diferencia) {
        this.diferencia = diferencia;
    }

    public Usuario getUsuarioCierre() {
        return usuarioCierre;
    }

    public void setUsuarioCierre(Usuario usuarioCierre) {
        this.usuarioCierre = usuarioCierre;
    }

    public EstadoCierreCaja getEstado() {
        return estado;
    }

    public void setEstado(EstadoCierreCaja estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaReapertura() {
        return fechaReapertura;
    }

    public void setFechaReapertura(LocalDateTime fechaReapertura) {
        this.fechaReapertura = fechaReapertura;
    }

    public Usuario getUsuarioReapertura() {
        return usuarioReapertura;
    }

    public void setUsuarioReapertura(Usuario usuarioReapertura) {
        this.usuarioReapertura = usuarioReapertura;
    }

    public String getMotivoReapertura() {
        return motivoReapertura;
    }

    public void setMotivoReapertura(String motivoReapertura) {
        this.motivoReapertura = motivoReapertura;
    }
}
