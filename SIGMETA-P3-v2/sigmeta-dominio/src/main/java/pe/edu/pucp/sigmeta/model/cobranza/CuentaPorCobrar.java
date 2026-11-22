package pe.edu.pucp.sigmeta.model.cobranza;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.socio.Cliente;

/**
 * Cuenta por cobrar generada por una venta al credito (RF009).
 */
public class CuentaPorCobrar {

    private int id;
    private Venta venta;
    private Cliente cliente;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    // RF009/RF013: el monto original es el total de la venta y no cambia. Las notas de credito que rebajan la
    // deuda se acumulan aparte en montoAcreditado: saldo = original - acreditado - pagado.
    private double montoOriginal;
    private double montoAcreditado;
    private double montoPagado;
    private double saldoPendiente;
    private EstadoCuentaPorCobrar estado;
    private List<Cobro> cobros;

    public CuentaPorCobrar() {
        this.cobros = new ArrayList<>();
    }

    public CuentaPorCobrar(int id, Venta venta, Cliente cliente, LocalDate fechaEmision, LocalDate fechaVencimiento, double montoOriginal, double montoPagado, double saldoPendiente, EstadoCuentaPorCobrar estado, List<Cobro> cobros) {
        this.id = id;
        this.venta = venta;
        this.cliente = cliente;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.montoOriginal = montoOriginal;
        this.montoPagado = montoPagado;
        this.saldoPendiente = saldoPendiente;
        this.estado = estado;
        this.cobros = cobros;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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


    public double getMontoOriginal() {
        return montoOriginal;
    }

    public void setMontoOriginal(double montoOriginal) {
        this.montoOriginal = montoOriginal;
    }

    public double getMontoAcreditado() {
        return montoAcreditado;
    }

    public void setMontoAcreditado(double montoAcreditado) {
        this.montoAcreditado = montoAcreditado;
    }

    // lo que el cliente debe en total: la venta menos lo que rebajaron las notas de credito
    public double getDeudaNeta() {
        return montoOriginal - montoAcreditado;
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

    public pe.edu.pucp.sigmeta.model.enums.SituacionPago getSituacionPago() {
        if (saldoPendiente <= 0.005) return pe.edu.pucp.sigmeta.model.enums.SituacionPago.PAGADA;
        return montoPagado > 0 ? pe.edu.pucp.sigmeta.model.enums.SituacionPago.PARCIAL : pe.edu.pucp.sigmeta.model.enums.SituacionPago.PENDIENTE;
    }
    public boolean isVencida() { return saldoPendiente > 0.005 && fechaVencimiento != null && fechaVencimiento.isBefore(LocalDate.now()); }

}
