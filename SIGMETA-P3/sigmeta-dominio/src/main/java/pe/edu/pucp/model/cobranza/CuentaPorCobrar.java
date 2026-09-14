package pe.edu.pucp.model.cobranza;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.ventas.Venta;
import pe.edu.pucp.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.Cliente;

/**
 * Cuenta por cobrar generada por una venta al credito (RF009).
 */
public class CuentaPorCobrar {

    private int idCuentaPorCobrar;
    private Venta venta;
    private Cliente cliente;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private Moneda moneda;
    private double montoOriginal;
    private double montoPagado;
    private double saldoPendiente;
    private EstadoCuentaPorCobrar estado;
    private List<Cobro> cobros;

    public CuentaPorCobrar() {
        this.cobros = new ArrayList<>();
    }

    public CuentaPorCobrar(int idCuentaPorCobrar, Venta venta, Cliente cliente, LocalDate fechaEmision, LocalDate fechaVencimiento, Moneda moneda, double montoOriginal, double montoPagado, double saldoPendiente, EstadoCuentaPorCobrar estado, List<Cobro> cobros) {
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

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
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

    public List<Cobro> getCobros() {
        return cobros;
    }

    public void setCobros(List<Cobro> cobros) {
        this.cobros = cobros;
    }

}
