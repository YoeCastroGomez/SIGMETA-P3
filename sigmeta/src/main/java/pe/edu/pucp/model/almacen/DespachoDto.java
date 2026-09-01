package pe.edu.pucp.model.almacen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comercial.VentaDto;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Despacho de una venta con su guia de remision (RF011).
 */
public class DespachoDto {

    private int idDespacho;
    private VentaDto venta;
    private String serieGuia;
    private String numeroGuia;
    private LocalDate fechaDespacho;
    private String direccionEntrega;
    private String transportista;
    private boolean anulado;
    private UsuarioDto usuarioRegistro;
    private LocalDateTime fechaRegistro;
    private List<DetalleDespachoDto> detalles;

    public DespachoDto() {
        this.detalles = new ArrayList<>();
    }

    public DespachoDto(int idDespacho, VentaDto venta, String serieGuia, String numeroGuia, LocalDate fechaDespacho, String direccionEntrega, String transportista, boolean anulado, UsuarioDto usuarioRegistro, LocalDateTime fechaRegistro, List<DetalleDespachoDto> detalles) {
        this.idDespacho = idDespacho;
        this.venta = venta;
        this.serieGuia = serieGuia;
        this.numeroGuia = numeroGuia;
        this.fechaDespacho = fechaDespacho;
        this.direccionEntrega = direccionEntrega;
        this.transportista = transportista;
        this.anulado = anulado;
        this.usuarioRegistro = usuarioRegistro;
        this.fechaRegistro = fechaRegistro;
        this.detalles = detalles;
    }

    public int getIdDespacho() {
        return idDespacho;
    }

    public void setIdDespacho(int idDespacho) {
        this.idDespacho = idDespacho;
    }

    public VentaDto getVenta() {
        return venta;
    }

    public void setVenta(VentaDto venta) {
        this.venta = venta;
    }

    public String getSerieGuia() {
        return serieGuia;
    }

    public void setSerieGuia(String serieGuia) {
        this.serieGuia = serieGuia;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public LocalDate getFechaDespacho() {
        return fechaDespacho;
    }

    public void setFechaDespacho(LocalDate fechaDespacho) {
        this.fechaDespacho = fechaDespacho;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public String getTransportista() {
        return transportista;
    }

    public void setTransportista(String transportista) {
        this.transportista = transportista;
    }

    public boolean isAnulado() {
        return anulado;
    }

    public void setAnulado(boolean anulado) {
        this.anulado = anulado;
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

    public List<DetalleDespachoDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleDespachoDto> detalles) {
        this.detalles = detalles;
    }

}
