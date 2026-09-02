package pe.edu.pucp.model.almacen;

import java.time.LocalDateTime;
import pe.edu.pucp.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.model.producto.Producto;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Movimiento trazable de existencias con su documento de origen y usuario (RF014).
 */
public class MovimientoInventario {

    private int idMovimientoInventario;
    private Producto producto;
    private TipoMovimientoInventario tipo;
    private LocalDateTime fechaMovimiento;
    private double cantidad;
    private double stockResultante;
    private String documentoOrigen;
    private int idDocumentoOrigen;
    private Usuario usuarioRegistro;
    private String motivo;
    private double cantidadContada;

    public MovimientoInventario() {
    }

    public MovimientoInventario(int idMovimientoInventario, Producto producto, TipoMovimientoInventario tipo, LocalDateTime fechaMovimiento, double cantidad, double stockResultante, String documentoOrigen, int idDocumentoOrigen, Usuario usuarioRegistro, String motivo, double cantidadContada) {
        this.idMovimientoInventario = idMovimientoInventario;
        this.producto = producto;
        this.tipo = tipo;
        this.fechaMovimiento = fechaMovimiento;
        this.cantidad = cantidad;
        this.stockResultante = stockResultante;
        this.motivo = motivo;
        this.documentoOrigen = documentoOrigen;
        this.cantidadContada = cantidadContada;
        this.idDocumentoOrigen = idDocumentoOrigen;
        this.usuarioRegistro = usuarioRegistro;
    }

    public int getIdMovimientoInventario() {
        return idMovimientoInventario;
    }

    public void setIdMovimientoInventario(int idMovimientoInventario) {
        this.idMovimientoInventario = idMovimientoInventario;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
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

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }


    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public double getCantidadContada() {
        return cantidadContada;
    }

    public void setCantidadContada(double cantidadContada) {
        this.cantidadContada = cantidadContada;
    }
}
