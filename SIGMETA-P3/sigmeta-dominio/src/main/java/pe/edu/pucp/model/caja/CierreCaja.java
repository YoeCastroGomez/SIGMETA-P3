package pe.edu.pucp.model.caja;

import java.time.LocalDateTime;
import pe.edu.pucp.model.usuario.Usuario;

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

    public CierreCaja() {
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
}
