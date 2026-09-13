package pe.edu.pucp.ejecucion;

import pe.edu.pucp.model.comercial.Venta;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * ESTUDIANTE 5.
 * Tablas: caja, movimiento_caja, cierre_caja, cuenta_por_cobrar, cobro.
 *
 * Debe crear: la apertura de caja con su monto inicial, la cuenta por
 * cobrar que nace de la venta al credito, un cobro parcial, un egreso
 * manual, y el cierre comparando monto calculado contra monto declarado.
 *
 * La nota de credito no debe contarse como cobro valido en el cierre,
 * el RF010 lo pide de forma explicita.
 */
public class PruebaCaja {

    public static void ejecutar(Venta venta, Usuario registro) {
        System.out.println("== PRUEBA 5: caja y cobranza ==");

        // TODO estudiante 5
    }
}
