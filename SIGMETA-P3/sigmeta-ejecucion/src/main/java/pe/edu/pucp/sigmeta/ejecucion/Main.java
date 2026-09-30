package pe.edu.pucp.sigmeta.ejecucion;

/**
 * Punto de entrada del modulo de ejecucion.
 * Evidencia el funcionamiento de las operaciones CRUD de las 6 entidades principales del proyecto
 * (Cliente, Producto, Cotizacion, Compra, Venta y Caja) a traves de la capa de negocio y la base de datos.
 */
public class Main {

    public static void main(String[] args) {
        PruebaCrud.ejecutar();
    }
}
