package pe.edu.pucp.model.caja;

import java.time.LocalDateTime;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Jornada de caja abierta por un usuario (RF010).
 */
public class CajaDto {
    private int idCaja;
    private LocalDateTime fechaApertura;
    private double montoInicial;
    private UsuarioDto usuarioApertura;
    private boolean abierta;

    public CajaDto() {
    }

    public CajaDto(int idCaja, LocalDateTime fechaApertura, double montoInicial,
                   UsuarioDto usuarioApertura, boolean abierta) {
        this.idCaja = idCaja;
        this.fechaApertura = fechaApertura;
        this.montoInicial = montoInicial;
        this.usuarioApertura = usuarioApertura;
        this.abierta = abierta;
    }

    public int getIdCaja() {
        return idCaja;
    }

    public void setIdCaja(int idCaja) {
        this.idCaja = idCaja;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public double getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(double montoInicial) {
        this.montoInicial = montoInicial;
    }

    public UsuarioDto getUsuarioApertura() {
        return usuarioApertura;
    }

    public void setUsuarioApertura(UsuarioDto usuarioApertura) {
        this.usuarioApertura = usuarioApertura;
    }

    public boolean isAbierta() {
        return abierta;
    }

    public void setAbierta(boolean abierta) {
        this.abierta = abierta;
    }
}
