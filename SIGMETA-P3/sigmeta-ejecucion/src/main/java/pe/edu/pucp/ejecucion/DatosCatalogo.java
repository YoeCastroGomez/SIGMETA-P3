package pe.edu.pucp.ejecucion;

import java.util.List;
import pe.edu.pucp.model.comercial.Cotizacion;
import pe.edu.pucp.model.producto.Producto;

/**
 * Contenedor simple con lo que produce la prueba de catalogo.
 */
public class DatosCatalogo {

    private List<Producto> productos;
    private Cotizacion cotizacion;

    public DatosCatalogo(List<Producto> productos, Cotizacion cotizacion) {
        this.productos = productos;
        this.cotizacion = cotizacion;
    }

    public List<Producto> getProductos() {
        return productos;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }
}
