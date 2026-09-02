package pe.edu.pucp.model.almacen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.compras.CompraDto;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Recepcion total o parcial de una orden de compra a proveedor (RF012).
 */
public class RecepcionCompraDto {
    private int idRecepcionCompra;
    private CompraDto compra;
    private LocalDate fechaRecepcion;
    private String observaciones;
    private UsuarioDto usuarioRegistro;
    private LocalDateTime fechaRegistro;
    private List<DetalleRecepcionCompraDto> detalles;

    public RecepcionCompraDto() {
        this.detalles = new ArrayList<>();
    }

    public RecepcionCompraDto(int idRecepcionCompra, CompraDto compra, LocalDate fechaRecepcion,
                              String observaciones, UsuarioDto usuarioRegistro,
                              LocalDateTime fechaRegistro,
                              List<DetalleRecepcionCompraDto> detalles) {
        this.idRecepcionCompra = idRecepcionCompra;
        this.compra = compra;
        this.fechaRecepcion = fechaRecepcion;
        this.observaciones = observaciones;
        this.usuarioRegistro = usuarioRegistro;
        this.fechaRegistro = fechaRegistro;
        this.detalles = detalles;
    }

    public int getIdRecepcionCompra() {
        return idRecepcionCompra;
    }

    public void setIdRecepcionCompra(int idRecepcionCompra) {
        this.idRecepcionCompra = idRecepcionCompra;
    }

    public CompraDto getCompra() {
        return compra;
    }

    public void setCompra(CompraDto compra) {
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

    public List<DetalleRecepcionCompraDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleRecepcionCompraDto> detalles) {
        this.detalles = detalles;
    }
}
