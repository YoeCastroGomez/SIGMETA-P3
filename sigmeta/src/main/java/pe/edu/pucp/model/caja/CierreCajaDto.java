package pe.edu.pucp.model.caja;

import java.time.LocalDateTime;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Cierre y cuadre de una jornada de caja (RF010).
 */
public class CierreCajaDto {
    private int idCierreCaja;
    private CajaDto caja;
    private LocalDateTime fechaCierre;
    private double montoCalculado;
    private double montoDeclarado;
    private double diferencia;
    private UsuarioDto usuarioCierre;

    public CierreCajaDto() {
    }

    public CierreCajaDto(int idCierreCaja, CajaDto caja, LocalDateTime fechaCierre,
                         double montoCalculado, double montoDeclarado, double diferencia,
                         UsuarioDto usuarioCierre) {
        this.idCierreCaja = idCierreCaja;
        this.caja = caja;
        this.fechaCierre = fechaCierre;
        this.montoCalculado = montoCalculado;
        this.montoDeclarado = montoDeclarado;
        this.diferencia = diferencia;
        this.usuarioCierre = usuarioCierre;
    }

    public int getIdCierreCaja() {
        return idCierreCaja;
    }

    public void setIdCierreCaja(int idCierreCaja) {
        this.idCierreCaja = idCierreCaja;
    }

    public CajaDto getCaja() {
        return caja;
    }

    public void setCaja(CajaDto caja) {
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

    public UsuarioDto getUsuarioCierre() {
        return usuarioCierre;
    }

    public void setUsuarioCierre(UsuarioDto usuarioCierre) {
        this.usuarioCierre = usuarioCierre;
    }
}
