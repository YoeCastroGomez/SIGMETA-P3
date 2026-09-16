package pe.edu.pucp.model.almacen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.ventas.Venta;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Despacho de una venta con su guia de remision (RF011).
 */
public class Despacho {

    private int id;
    private Venta venta;
    private String serieGuia;
    private String numeroGuia;
    private LocalDate fechaDespacho;
    private String direccionEntrega;
    private String transportista;
    private boolean anulado;
    private Usuario usuarioRegistro;
    private LocalDateTime fechaRegistro;
    private List<DetalleDespacho> detalles;

    public Despacho() {
        this.detalles = new ArrayList<>();
    }

    public Despacho(int id, Venta venta, String serieGuia, String numeroGuia, LocalDate fechaDespacho, String direccionEntrega, String transportista, boolean anulado, Usuario usuarioRegistro, LocalDateTime fechaRegistro, List<DetalleDespacho> detalles) {
        this.id = id;
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

    public List<DetalleDespacho> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleDespacho> detalles) {
        this.detalles = detalles;
    }

}
