package pe.edu.pucp.model.almacen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.enums.EstadoOrdenProduccion;
import pe.edu.pucp.model.producto.ProductoDto;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Orden de produccion de un producto fabricado. Lleva el estado del ciclo (RF012) y el costo unitario calculado al cierre a partir de los insumos consumidos (RF013).
 */
public class OrdenProduccionDto {

    private int idOrdenProduccion;
    private String numero;
    private ProductoDto productoFabricado;
    private double cantidadProducir;
    private double cantidadProducida;
    private LocalDate fechaProgramada;
    private LocalDate fechaCierre;
    private EstadoOrdenProduccion estado;
    private double costoTotalInsumos;
    private double costoUnitarioProducido;
    private UsuarioDto usuarioRegistro;
    private LocalDateTime fechaRegistro;
    private List<DetalleOrdenProduccionDto> detalles;

    public OrdenProduccionDto() {
        this.detalles = new ArrayList<>();
    }

    public OrdenProduccionDto(int idOrdenProduccion, String numero, ProductoDto productoFabricado, double cantidadProducir, double cantidadProducida, LocalDate fechaProgramada, LocalDate fechaCierre, EstadoOrdenProduccion estado, double costoTotalInsumos, double costoUnitarioProducido, UsuarioDto usuarioRegistro, LocalDateTime fechaRegistro, List<DetalleOrdenProduccionDto> detalles) {
        this.idOrdenProduccion = idOrdenProduccion;
        this.numero = numero;
        this.productoFabricado = productoFabricado;
        this.cantidadProducir = cantidadProducir;
        this.cantidadProducida = cantidadProducida;
        this.fechaProgramada = fechaProgramada;
        this.fechaCierre = fechaCierre;
        this.estado = estado;
        this.costoTotalInsumos = costoTotalInsumos;
        this.costoUnitarioProducido = costoUnitarioProducido;
        this.usuarioRegistro = usuarioRegistro;
        this.fechaRegistro = fechaRegistro;
        this.detalles = detalles;
    }

    public int getIdOrdenProduccion() {
        return idOrdenProduccion;
    }

    public void setIdOrdenProduccion(int idOrdenProduccion) {
        this.idOrdenProduccion = idOrdenProduccion;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public ProductoDto getProductoFabricado() {
        return productoFabricado;
    }

    public void setProductoFabricado(ProductoDto productoFabricado) {
        this.productoFabricado = productoFabricado;
    }

    public double getCantidadProducir() {
        return cantidadProducir;
    }

    public void setCantidadProducir(double cantidadProducir) {
        this.cantidadProducir = cantidadProducir;
    }

    public double getCantidadProducida() {
        return cantidadProducida;
    }

    public void setCantidadProducida(double cantidadProducida) {
        this.cantidadProducida = cantidadProducida;
    }

    public LocalDate getFechaProgramada() {
        return fechaProgramada;
    }

    public void setFechaProgramada(LocalDate fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDate fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public EstadoOrdenProduccion getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenProduccion estado) {
        this.estado = estado;
    }

    public double getCostoTotalInsumos() {
        return costoTotalInsumos;
    }

    public void setCostoTotalInsumos(double costoTotalInsumos) {
        this.costoTotalInsumos = costoTotalInsumos;
    }

    public double getCostoUnitarioProducido() {
        return costoUnitarioProducido;
    }

    public void setCostoUnitarioProducido(double costoUnitarioProducido) {
        this.costoUnitarioProducido = costoUnitarioProducido;
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

    public List<DetalleOrdenProduccionDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleOrdenProduccionDto> detalles) {
        this.detalles = detalles;
    }

}
