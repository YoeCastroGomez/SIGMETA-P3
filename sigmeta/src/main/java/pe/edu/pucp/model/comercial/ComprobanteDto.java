package pe.edu.pucp.model.comercial;

import java.time.LocalDate;
import java.time.LocalDateTime;
import pe.edu.pucp.model.enums.EstadoComprobante;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.enums.TipoComprobante;

/**
 * Comprobante electronico asociado a una venta (RF008).
 */
public class ComprobanteDto {
    private int idComprobante;
    private VentaDto venta;
    private TipoComprobante tipo;
    private String serie;
    private String numero;
    private LocalDate fechaEmision;
    private Moneda moneda;
    private double subTotal;
    private double igv;
    private double total;
    private EstadoComprobante estado;
    private ComprobanteDto comprobanteRelacionado;
    private String medioEnvio;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaRegistro;

    public ComprobanteDto() {
    }

    public ComprobanteDto(int idComprobante, VentaDto venta, TipoComprobante tipo,
                          String serie, String numero, LocalDate fechaEmision,
                          Moneda moneda, double subTotal, double igv, double total,
                          EstadoComprobante estado, ComprobanteDto comprobanteRelacionado,
                          String medioEnvio, LocalDateTime fechaEnvio,
                          LocalDateTime fechaRegistro) {
        this.idComprobante = idComprobante;
        this.venta = venta;
        this.tipo = tipo;
        this.serie = serie;
        this.numero = numero;
        this.fechaEmision = fechaEmision;
        this.moneda = moneda;
        this.subTotal = subTotal;
        this.igv = igv;
        this.total = total;
        this.estado = estado;
        this.comprobanteRelacionado = comprobanteRelacionado;
        this.medioEnvio = medioEnvio;
        this.fechaEnvio = fechaEnvio;
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdComprobante() {
        return idComprobante;
    }

    public void setIdComprobante(int idComprobante) {
        this.idComprobante = idComprobante;
    }

    public VentaDto getVenta() {
        return venta;
    }

    public void setVenta(VentaDto venta) {
        this.venta = venta;
    }

    public TipoComprobante getTipo() {
        return tipo;
    }

    public void setTipo(TipoComprobante tipo) {
        this.tipo = tipo;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public Moneda getMoneda() {
        return moneda;
    }

    public void setMoneda(Moneda moneda) {
        this.moneda = moneda;
    }

    public double getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(double subTotal) {
        this.subTotal = subTotal;
    }

    public double getIgv() {
        return igv;
    }

    public void setIgv(double igv) {
        this.igv = igv;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public EstadoComprobante getEstado() {
        return estado;
    }

    public void setEstado(EstadoComprobante estado) {
        this.estado = estado;
    }

    public ComprobanteDto getComprobanteRelacionado() {
        return comprobanteRelacionado;
    }

    public void setComprobanteRelacionado(ComprobanteDto comprobanteRelacionado) {
        this.comprobanteRelacionado = comprobanteRelacionado;
    }

    public String getMedioEnvio() {
        return medioEnvio;
    }

    public void setMedioEnvio(String medioEnvio) {
        this.medioEnvio = medioEnvio;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
