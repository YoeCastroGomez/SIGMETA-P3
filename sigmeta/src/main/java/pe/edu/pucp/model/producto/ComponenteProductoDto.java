package pe.edu.pucp.model.producto;

import pe.edu.pucp.model.enums.UnidadMedida;

/**
 * Insumo y cantidad requerida para elaborar una unidad de un producto fabricado (RF005).
 */
public class ComponenteProductoDto {

    private int idComponenteProducto;
    private ProductoDto productoFabricado;
    private ProductoDto insumo;
    private double cantidadRequerida;
    private UnidadMedida unidadMedida;

    public ComponenteProductoDto() {
    }

    public ComponenteProductoDto(int idComponenteProducto, ProductoDto productoFabricado, ProductoDto insumo, double cantidadRequerida, UnidadMedida unidadMedida) {
        this.idComponenteProducto = idComponenteProducto;
        this.productoFabricado = productoFabricado;
        this.insumo = insumo;
        this.cantidadRequerida = cantidadRequerida;
        this.unidadMedida = unidadMedida;
    }

    public int getIdComponenteProducto() {
        return idComponenteProducto;
    }

    public void setIdComponenteProducto(int idComponenteProducto) {
        this.idComponenteProducto = idComponenteProducto;
    }

    public ProductoDto getProductoFabricado() {
        return productoFabricado;
    }

    public void setProductoFabricado(ProductoDto productoFabricado) {
        this.productoFabricado = productoFabricado;
    }

    public ProductoDto getInsumo() {
        return insumo;
    }

    public void setInsumo(ProductoDto insumo) {
        this.insumo = insumo;
    }

    public double getCantidadRequerida() {
        return cantidadRequerida;
    }

    public void setCantidadRequerida(double cantidadRequerida) {
        this.cantidadRequerida = cantidadRequerida;
    }

    public UnidadMedida getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(UnidadMedida unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

}
