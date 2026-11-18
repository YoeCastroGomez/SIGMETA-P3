package pe.edu.pucp.sigmeta.boimpl.ventas;

import java.util.EnumSet;
import java.util.Set;
import pe.edu.pucp.sigmeta.model.enums.UnidadMedida;
import pe.edu.pucp.sigmeta.model.producto.Producto;

// RF007/RF008: un producto que se vende por unidad (unidad, caja, juego, millar, galon) solo se cotiza y se vende
// en cantidades enteras. Los que se venden por kilogramo, metro o litro admiten decimales (hasta 3, como la tabla).
final class UnidadesVenta {

    private static final Set<UnidadMedida> FRACCIONABLES =
            EnumSet.of(UnidadMedida.KILOGRAMO, UnidadMedida.METRO, UnidadMedida.LITRO);

    private UnidadesVenta() {
    }

    static boolean admiteDecimales(UnidadMedida unidad) {
        return unidad == null || FRACCIONABLES.contains(unidad);
    }

    static void validarCantidad(Producto producto, double cantidad, int numeroLinea) {
        if (!admiteDecimales(producto.getUnidadVenta()) && Math.abs(cantidad - Math.rint(cantidad)) > 1e-9) {
            throw new IllegalArgumentException("La línea " + numeroLinea + " (" + producto.getNombre() + ") se vende por "
                    + producto.getUnidadVenta().name().toLowerCase() + ": la cantidad debe ser un número entero");
        }
    }
}
