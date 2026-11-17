package pe.edu.pucp.sigmeta.boimpl.almacen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglasInventarioTest {

    @Test
    void recepcionParcialYCompletaNoExcedenPedido() {
        assertDoesNotThrow(() -> ReglasInventario.validarCantidadRecepcion(4, 0, 10));
        assertDoesNotThrow(() -> ReglasInventario.validarCantidadRecepcion(6, 4, 10));
        assertThrows(IllegalArgumentException.class,
                () -> ReglasInventario.validarCantidadRecepcion(7, 4, 10));
    }

    @Test
    void rechazaRecepcionInvalida() {
        for (double cantidad : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> ReglasInventario.validarCantidadRecepcion(cantidad, 0, 10));
        }
        assertThrows(IllegalStateException.class,
                () -> ReglasInventario.validarCantidadRecepcion(1, -1, 10));
    }

    @Test
    void convierteUnidadCompraAUnidadInventario() {
        assertEquals(24, ReglasInventario.convertirAStock(2, 12), 0.000001);
        assertThrows(IllegalArgumentException.class,
                () -> ReglasInventario.convertirAStock(2, 0));
        assertThrows(IllegalArgumentException.class,
                () -> ReglasInventario.convertirAStock(Double.MAX_VALUE, 2));
    }

    @Test
    void ingresoActualizaStockYControlaDesbordamiento() {
        assertEquals(34, ReglasInventario.ingresar(10, 24), 0.000001);
        assertThrows(IllegalStateException.class,
                () -> ReglasInventario.ingresar(Double.MAX_VALUE, Double.MAX_VALUE));
        assertThrows(IllegalStateException.class,
                () -> ReglasInventario.ingresar(-1, 3));
    }

    @Test
    void reversionDescuentaExactamenteLoRecibido() {
        double inicial = 10;
        double recibido = ReglasInventario.convertirAStock(2, 12);
        double stock = ReglasInventario.ingresar(inicial, recibido);
        assertEquals(inicial, ReglasInventario.retirarPorReversion(stock, recibido), 0.000001);
    }

    @Test
    void reversionConStockInsuficienteNoSePermite() {
        assertThrows(IllegalStateException.class,
                () -> ReglasInventario.retirarPorReversion(5, 12));
        assertThrows(IllegalStateException.class,
                () -> ReglasInventario.retirarPorReversion(Double.NaN, 1));
        assertThrows(IllegalStateException.class,
                () -> ReglasInventario.retirarPorReversion(10, Double.POSITIVE_INFINITY));
    }

    @Test
    void reversionNoDejaStockNegativoPorRedondeo() {
        assertEquals(0, ReglasInventario.retirarPorReversion(9.9999999, 10), 0.000001);
    }
}
