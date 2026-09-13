package pe.edu.pucp.model.almacen;

import java.time.LocalDateTime;
import pe.edu.pucp.model.comercial.Comprobante;
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

    private RecepcionCompra recepcionCompra;
    private Despacho despacho;
    private Comprobante notaCredito;

    private Usuario usuarioRegistro;
    private String motivo;
    private double cantidadContada;

    public MovimientoInventario() {
    }

    public MovimientoInventario(
            int idMovimientoInventario,
            Producto producto,
            TipoMovimientoInventario tipo,
            LocalDateTime fechaMovimiento,
            double cantidad,
            double stockResultante,
            RecepcionCompra recepcionCompra,
            Despacho despacho,
            Comprobante notaCredito,
            Usuario usuarioRegistro,
            String motivo,
            double cantidadContada) {

        this.idMovimientoInventario = idMovimientoInventario;
        this.producto = producto;
        this.tipo = tipo;
        this.fechaMovimiento = fechaMovimiento;
        this.cantidad = cantidad;
        this.stockResultante = stockResultante;
        this.recepcionCompra = recepcionCompra;
        this.despacho = despacho;
        this.notaCredito = notaCredito;
        this.usuarioRegistro = usuarioRegistro;
        this.motivo = motivo;
        this.cantidadContada = cantidadContada;
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

    public RecepcionCompra getRecepcionCompra() {
        return recepcionCompra;
    }

    public void setRecepcionCompra(RecepcionCompra recepcionCompra) {
        this.recepcionCompra = recepcionCompra;
    }

    public Despacho getDespacho() {
        return despacho;
    }

    public void setDespacho(Despacho despacho) {
        this.despacho = despacho;
    }

    public Comprobante getNotaCredito() {
        return notaCredito;
    }

    public void setNotaCredito(Comprobante notaCredito) {
        this.notaCredito = notaCredito;
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
