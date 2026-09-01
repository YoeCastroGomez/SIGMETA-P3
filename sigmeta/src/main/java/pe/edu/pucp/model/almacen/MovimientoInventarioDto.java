package pe.edu.pucp.model.almacen;

import java.time.LocalDateTime;
import pe.edu.pucp.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.model.producto.ProductoDto;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Movimiento trazable de existencias con su documento de origen y usuario (RF014).
 */
public class MovimientoInventarioDto {

    private int idMovimientoInventario;
    private ProductoDto producto;
    private TipoMovimientoInventario tipo;
    private LocalDateTime fechaMovimiento;
    private double cantidad;
    private double stockResultante;
    private String documentoOrigen;
    private int idDocumentoOrigen;
    private UsuarioDto usuarioRegistro;

    public MovimientoInventarioDto() {
    }

    public MovimientoInventarioDto(int idMovimientoInventario, ProductoDto producto, TipoMovimientoInventario tipo, LocalDateTime fechaMovimiento, double cantidad, double stockResultante, String documentoOrigen, int idDocumentoOrigen, UsuarioDto usuarioRegistro) {
        this.idMovimientoInventario = idMovimientoInventario;
        this.producto = producto;
        this.tipo = tipo;
        this.fechaMovimiento = fechaMovimiento;
        this.cantidad = cantidad;
        this.stockResultante = stockResultante;
        this.documentoOrigen = documentoOrigen;
        this.idDocumentoOrigen = idDocumentoOrigen;
        this.usuarioRegistro = usuarioRegistro;
    }

    public int getIdMovimientoInventario() {
        return idMovimientoInventario;
    }

    public void setIdMovimientoInventario(int idMovimientoInventario) {
        this.idMovimientoInventario = idMovimientoInventario;
    }

    public ProductoDto getProducto() {
        return producto;
    }

    public void setProducto(ProductoDto producto) {
        this.producto = producto;
    }

    public TipoMovimientoInventario getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimientoInventario tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public double getStockResultante() {
        return stockResultante;
    }

    public void setStockResultante(double stockResultante) {
        this.stockResultante = stockResultante;
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
