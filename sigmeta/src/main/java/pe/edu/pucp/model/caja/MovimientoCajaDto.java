package pe.edu.pucp.model.caja;

import java.time.LocalDateTime;
import pe.edu.pucp.model.enums.MedioPago;
import pe.edu.pucp.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Movimiento de ingreso o egreso registrado durante una jornada de caja (RF010).
 */
public class MovimientoCajaDto {
    private int idMovimientoCaja;
    private CajaDto caja;
    private TipoMovimientoCaja tipo;
    private LocalDateTime fechaMovimiento;
    private MedioPago medioPago;
    private double monto;
    private String concepto;
    private String documentoOrigen;
    private int idDocumentoOrigen;
    private UsuarioDto usuarioRegistro;

    public MovimientoCajaDto() {
    }

    public MovimientoCajaDto(int idMovimientoCaja, CajaDto caja, TipoMovimientoCaja tipo,
                             LocalDateTime fechaMovimiento, MedioPago medioPago,
                             double monto, String concepto, String documentoOrigen,
                             int idDocumentoOrigen, UsuarioDto usuarioRegistro) {
        this.idMovimientoCaja = idMovimientoCaja;
        this.caja = caja;
        this.tipo = tipo;
        this.fechaMovimiento = fechaMovimiento;
        this.medioPago = medioPago;
        this.monto = monto;
        this.concepto = concepto;
        this.documentoOrigen = documentoOrigen;
        this.idDocumentoOrigen = idDocumentoOrigen;
        this.usuarioRegistro = usuarioRegistro;
    }

    public int getIdMovimientoCaja() {
        return idMovimientoCaja;
    }

    public void setIdMovimientoCaja(int idMovimientoCaja) {
        this.idMovimientoCaja = idMovimientoCaja;
    }

    public CajaDto getCaja() {
        return caja;
    }

    public void setCaja(CajaDto caja) {
        this.caja = caja;
    }

    public TipoMovimientoCaja getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimientoCaja tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
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

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public String getDocumentoOrigen() {
        return documentoOrigen;
    }

    public void setDocumentoOrigen(String documentoOrigen) {
        this.documentoOrigen = documentoOrigen;
    }

    public int getIdDocumentoOrigen() {
        return idDocumentoOrigen;
    }

    public void setIdDocumentoOrigen(int idDocumentoOrigen) {
        this.idDocumentoOrigen = idDocumentoOrigen;
    }

    public UsuarioDto getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(UsuarioDto usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
