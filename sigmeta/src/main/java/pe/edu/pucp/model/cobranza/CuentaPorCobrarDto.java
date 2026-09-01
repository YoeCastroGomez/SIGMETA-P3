package pe.edu.pucp.model.cobranza;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comercial.VentaDto;
import pe.edu.pucp.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.ClienteDto;

/**
 * Cuenta por cobrar generada por una venta al credito (RF009).
 */
public class CuentaPorCobrarDto {

    private int idCuentaPorCobrar;
    private VentaDto venta;
    private ClienteDto cliente;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private Moneda moneda;
    private double montoOriginal;
    private double montoPagado;
    private double saldoPendiente;
    private EstadoCuentaPorCobrar estado;
    private List<CobroDto> cobros;

    public CuentaPorCobrarDto() {
        this.cobros = new ArrayList<>();
    }

    public CuentaPorCobrarDto(int idCuentaPorCobrar, VentaDto venta, ClienteDto cliente, LocalDate fechaEmision, LocalDate fechaVencimiento, Moneda moneda, double montoOriginal, double montoPagado, double saldoPendiente, EstadoCuentaPorCobrar estado, List<CobroDto> cobros) {
        this.idCuentaPorCobrar = idCuentaPorCobrar;
        this.venta = venta;
        this.cliente = cliente;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.moneda = moneda;
        this.montoOriginal = montoOriginal;
        this.montoPagado = montoPagado;
        this.saldoPendiente = saldoPendiente;
        this.estado = estado;
        this.cobros = cobros;
    }

    public int getIdCuentaPorCobrar() {
        return idCuentaPorCobrar;
    }

    public void setIdCuentaPorCobrar(int idCuentaPorCobrar) {
        this.idCuentaPorCobrar = idCuentaPorCobrar;
    }

    public VentaDto getVenta() {
        return venta;
    }

    public void setVenta(VentaDto venta) {
        this.venta = venta;
    }

    public ClienteDto getCliente() {
        return cliente;
    }

    public void setCliente(ClienteDto cliente) {
        this.cliente = cliente;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Moneda getMoneda() {
        return moneda;
    }

    public void setMoneda(Moneda moneda) {
        this.moneda = moneda;
    }

    public double getMontoOriginal() {
        return montoOriginal;
    }

    public void setMontoOriginal(double montoOriginal) {
        this.montoOriginal = montoOriginal;
    }

    public double getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(double montoPagado) {
        this.montoPagado = montoPagado;
    }

    public double getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(double saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public EstadoCuentaPorCobrar getEstado() {
        return estado;
    }

    public void setEstado(EstadoCuentaPorCobrar estado) {
        this.estado = estado;
    }

    public List<CobroDto> getCobros() {
        return cobros;
    }

    public void setCobros(List<CobroDto> cobros) {
        this.cobros = cobros;
    }

}
