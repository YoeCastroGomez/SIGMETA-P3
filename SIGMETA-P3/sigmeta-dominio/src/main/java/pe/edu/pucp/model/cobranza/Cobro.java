package pe.edu.pucp.model.cobranza;

import java.time.LocalDate;
import java.time.LocalDateTime;
import pe.edu.pucp.model.enums.MedioPago;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Cobro total o parcial aplicado a una cuenta por cobrar (RF009).
 */
public class Cobro {

    private int id;
    private CuentaPorCobrar cuentaPorCobrar;
    private LocalDate fechaCobro;
    private MedioPago medioPago;
    private double monto;
    private String referencia;
    private Usuario usuarioRegistro;
    private LocalDateTime fechaRegistro;

    public Cobro() {
    }

    public Cobro(int id, CuentaPorCobrar cuentaPorCobrar, LocalDate fechaCobro, MedioPago medioPago, double monto, String referencia, Usuario usuarioRegistro, LocalDateTime fechaRegistro) {
        this.id = id;
        this.cuentaPorCobrar = cuentaPorCobrar;
        this.fechaCobro = fechaCobro;
        this.medioPago = medioPago;
        this.monto = monto;
        this.referencia = referencia;
        this.usuarioRegistro = usuarioRegistro;
        this.fechaRegistro = fechaRegistro;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public CuentaPorCobrar getCuentaPorCobrar() {
        return cuentaPorCobrar;
    }

    public void setCuentaPorCobrar(CuentaPorCobrar cuentaPorCobrar) {
        this.cuentaPorCobrar = cuentaPorCobrar;
    }

    public LocalDate getFechaCobro() {
        return fechaCobro;
    }

    public void setFechaCobro(LocalDate fechaCobro) {
        this.fechaCobro = fechaCobro;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(MedioPago medioPago) {
        this.medioPago = medioPago;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

}
