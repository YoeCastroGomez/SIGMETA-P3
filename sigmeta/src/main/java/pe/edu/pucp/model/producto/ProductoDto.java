package pe.edu.pucp.model.producto;

import pe.edu.pucp.model.enums.UnidadMedida;

/**
 * Producto del catalogo de comercializacion (RF005 y RF006).
 */
public class ProductoDto {
    private int idProducto;
    private String codigoInterno;
    private String codigoFabricante;
    private String codigoProveedor;
    private String nombre;
    private String descripcion;
    private CategoriaDto categoria;
    private UnidadMedida unidadCompra;
    private UnidadMedida unidadVenta;
    private double factorConversion;
    private double precioVenta;
    private double costoUnitario;
    private double stockActual;
    private double stockMinimo;
    private String imagen;
    private boolean estado;

    public ProductoDto() {
    }

    public ProductoDto(int idProducto, String codigoInterno, String codigoFabricante,
                       String codigoProveedor, String nombre, String descripcion,
                       CategoriaDto categoria, UnidadMedida unidadCompra,
                       UnidadMedida unidadVenta, double factorConversion,
                       double precioVenta, double costoUnitario, double stockActual,
                       double stockMinimo, String imagen, boolean estado) {
        this.idProducto = idProducto;
        this.codigoInterno = codigoInterno;
        this.codigoFabricante = codigoFabricante;
        this.codigoProveedor = codigoProveedor;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.unidadCompra = unidadCompra;
        this.unidadVenta = unidadVenta;
        this.factorConversion = factorConversion;
        this.precioVenta = precioVenta;
        this.costoUnitario = costoUnitario;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.imagen = imagen;
        this.estado = estado;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigoInterno() {
        return codigoInterno;
    }

    public void setCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
    }

    public String getCodigoFabricante() {
        return codigoFabricante;
    }

    public void setCodigoFabricante(String codigoFabricante) {
        this.codigoFabricante = codigoFabricante;
    }

    public String getCodigoProveedor() {
        return codigoProveedor;
    }

    public void setCodigoProveedor(String codigoProveedor) {
        this.codigoProveedor = codigoProveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public CategoriaDto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaDto categoria) {
        this.categoria = categoria;
    }

    public UnidadMedida getUnidadCompra() {
        return unidadCompra;
    }

    public void setUnidadCompra(UnidadMedida unidadCompra) {
        this.unidadCompra = unidadCompra;
    }

    public UnidadMedida getUnidadVenta() {
        return unidadVenta;
    }

    public void setUnidadVenta(UnidadMedida unidadVenta) {
        this.unidadVenta = unidadVenta;
    }

    public double getFactorConversion() {
        return factorConversion;
    }

    public void setFactorConversion(double factorConversion) {
        this.factorConversion = factorConversion;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public double getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(double costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public double getStockActual() {
        return stockActual;
    }

    public void setStockActual(double stockActual) {
        this.stockActual = stockActual;
    }

    public double getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(double stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
