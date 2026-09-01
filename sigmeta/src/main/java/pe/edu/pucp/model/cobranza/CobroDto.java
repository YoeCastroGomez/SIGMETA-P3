package pe.edu.pucp.model.cobranza;

import java.time.LocalDate;
import java.time.LocalDateTime;
import pe.edu.pucp.model.enums.MedioPago;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Cobro total o parcial aplicado a una cuenta por cobrar (RF009).
 */
public class CobroDto {

    private int idCobro;
    private CuentaPorCobrarDto cuentaPorCobrar;
    private LocalDate fechaCobro;
    private MedioPago medioPago;
    private double monto;
    private String referencia;
    private UsuarioDto usuarioRegistro;
    private LocalDateTime fechaRegistro;

    public CobroDto() {
    }

    public CobroDto(int idCobro, CuentaPorCobrarDto cuentaPorCobrar, LocalDate fechaCobro, MedioPago medioPago, double monto, String referencia, UsuarioDto usuarioRegistro, LocalDateTime fechaRegistro) {
        this.idCobro = idCobro;
        this.cuentaPorCobrar = cuentaPorCobrar;
        this.fechaCobro = fechaCobro;
        this.medioPago = medioPago;
        this.monto = monto;
        this.referencia = referencia;
        this.usuarioRegistro = usuarioRegistro;
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdCobro() {
        return idCobro;
    }

    public void setIdCobro(int idCobro) {
        this.idCobro = idCobro;
    }

    public CuentaPorCobrarDto getCuentaPorCobrar() {
        return cuentaPorCobrar;
    }

    public void setCuentaPorCobrar(CuentaPorCobrarDto cuentaPorCobrar) {
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

    public UsuarioDto getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(UsuarioDto usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

}
