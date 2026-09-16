package pe.edu.pucp.model.almacen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.compras.Compra;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Recepcion total o parcial de una orden de compra a proveedor (RF012).
 */
public class RecepcionCompra {
    private int id;
    private Compra compra;
    private LocalDate fechaRecepcion;
    private String observaciones;
    private Usuario usuarioRegistro;
    private LocalDateTime fechaRegistro;
    private List<DetalleRecepcionCompra> detalles;

    public RecepcionCompra() {
        this.detalles = new ArrayList<>();
    }

    public RecepcionCompra(int id, Compra compra, LocalDate fechaRecepcion,
                              String observaciones, Usuario usuarioRegistro,
                              LocalDateTime fechaRegistro,
                              List<DetalleRecepcionCompra> detalles) {
        this.id = id;
        this.compra = compra;
        this.fechaRecepcion = fechaRecepcion;
        this.observaciones = observaciones;
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

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public LocalDate getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(LocalDate fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
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

    public List<DetalleRecepcionCompra> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleRecepcionCompra> detalles) {
        this.detalles = detalles;
    }
}
