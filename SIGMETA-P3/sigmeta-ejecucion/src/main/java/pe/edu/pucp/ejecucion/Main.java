package pe.edu.pucp.ejecucion;

/**
 * Punto de entrada del modulo de ejecucion.
 *
 * Las cinco pruebas estan encadenadas a proposito: cada una recibe lo que
 * produjo la anterior, de modo que al correr el main se recorre el flujo
 * comercial completo de SIGMETA y se verifica que la capa de dominio da
 * soporte al modelo de negocio.
 */
public class Main {

    public static void main(String[] args) {

        DatosMaestros maestros = PruebaSeguridad.ejecutar();

        DatosCatalogo catalogo = PruebaCatalogo.ejecutar(maestros.getCliente());

        PruebaAbastecimiento.ejecutar(
                maestros.getProveedor(),
                catalogo.getProductos(),
                maestros.getAdministrador());

        var venta = PruebaVenta.ejecutar(
                catalogo.getCotizacion(),
                maestros.getCliente(),
                maestros.getAdministrador());

        PruebaCaja.ejecutar(venta, maestros.getAdministrador());

        System.out.println();
        System.out.println("Fin de las pruebas de la capa de dominio.");
    }
}
