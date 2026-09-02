package pe.edu.pucp.model.caja;

import java.time.LocalDateTime;
import pe.edu.pucp.model.usuario.Usuario;
import java.util.List;
import java.util.ArrayList;

/**
 * Jornada de caja abierta por un usuario (RF010).
 */
public class Caja {
    private int idCaja;
    private LocalDateTime fechaApertura;
    private double montoInicial;
    private Usuario usuarioApertura;
    private boolean abierta;
    private List<MovimientoCaja> movimientos;

    public Caja() {
        this.movimientos = new ArrayList<>();
    }

    public Caja(int idCaja, LocalDateTime fechaApertura, double montoInicial,
                   Usuario usuarioApertura, boolean abierta) {
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

    public Usuario getUsuarioApertura() {
        return usuarioApertura;
    }

    public void setUsuarioApertura(Usuario usuarioApertura) {
        this.usuarioApertura = usuarioApertura;
    }

    public boolean isAbierta() {
        return abierta;
    }

    public void setAbierta(boolean abierta) {
        this.abierta = abierta;
    }

    public List<MovimientoCaja> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(List<MovimientoCaja> movimientos) {
        this.movimientos = movimientos;
    }
}
