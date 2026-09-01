package pe.edu.pucp.model.almacen;

import pe.edu.pucp.model.producto.ProductoDto;

/**
 * Insumo de una orden de produccion. Separa lo requerido de lo realmente consumido y conserva el costo con el que se valorizo (RF013).
 */
public class DetalleOrdenProduccionDto {

    private int idDetalleOrdenProduccion;
    private OrdenProduccionDto ordenProduccion;
    private ProductoDto insumo;
    private double cantidadRequerida;
    private double cantidadConsumida;
    private double costoUnitarioInsumo;
    private double costoTotalInsumo;

    public DetalleOrdenProduccionDto() {
    }

    public DetalleOrdenProduccionDto(int idDetalleOrdenProduccion, OrdenProduccionDto ordenProduccion, ProductoDto insumo, double cantidadRequerida, double cantidadConsumida, double costoUnitarioInsumo, double costoTotalInsumo) {
        this.idDetalleOrdenProduccion = idDetalleOrdenProduccion;
        this.ordenProduccion = ordenProduccion;
        this.insumo = insumo;
        this.cantidadRequerida = cantidadRequerida;
        this.cantidadConsumida = cantidadConsumida;
        this.costoUnitarioInsumo = costoUnitarioInsumo;
        this.costoTotalInsumo = costoTotalInsumo;
    }

    public int getIdDetalleOrdenProduccion() {
        return idDetalleOrdenProduccion;
    }

    public void setIdDetalleOrdenProduccion(int idDetalleOrdenProduccion) {
        this.idDetalleOrdenProduccion = idDetalleOrdenProduccion;
    }

    public OrdenProduccionDto getOrdenProduccion() {
        return ordenProduccion;
    }

    public void setOrdenProduccion(OrdenProduccionDto ordenProduccion) {
        this.ordenProduccion = ordenProduccion;
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

    public double getCantidadConsumida() {
        return cantidadConsumida;
    }

    public void setCantidadConsumida(double cantidadConsumida) {
        this.cantidadConsumida = cantidadConsumida;
    }

    public double getCostoUnitarioInsumo() {
        return costoUnitarioInsumo;
    }

    public void setCostoUnitarioInsumo(double costoUnitarioInsumo) {
        this.costoUnitarioInsumo = costoUnitarioInsumo;
    }

    public double getCostoTotalInsumo() {
        return costoTotalInsumo;
    }

    public void setCostoTotalInsumo(double costoTotalInsumo) {
        this.costoTotalInsumo = costoTotalInsumo;
    }

}
