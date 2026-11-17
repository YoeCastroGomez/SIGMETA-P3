package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.sigmeta.boimpl.ventas.ComprobanteBOImpl;

/**
 * H14 - RF013: tipo de nota de credito (catalogo 09 de SUNAT). Prueba unitaria: no usa la base de datos,
 * asi que corre siempre con mvn test.
 */
@DisplayName("RF013: tipo de nota de credito")
class TipoNotaCreditoTest {

    @Test
    @DisplayName("Acreditar el total del comprobante es devolucion total (06)")
    void totalDelComprobante() {
        assertEquals(ComprobanteBOImpl.NOTA_DEVOLUCION_TOTAL, ComprobanteBOImpl.tipoNotaCredito(1534.00, 1534.00));
        assertEquals(ComprobanteBOImpl.NOTA_DEVOLUCION_TOTAL, ComprobanteBOImpl.tipoNotaCredito(1533.99, 1534.00),
                "un centavo de redondeo sigue siendo devolucion total");
    }

    @Test
    @DisplayName("Acreditar solo una parte es devolucion por item (07)")
    void parteDelComprobante() {
        assertEquals(ComprobanteBOImpl.NOTA_DEVOLUCION_POR_ITEM, ComprobanteBOImpl.tipoNotaCredito(106.20, 1534.00));
        assertEquals(ComprobanteBOImpl.NOTA_DEVOLUCION_POR_ITEM, ComprobanteBOImpl.tipoNotaCredito(1533.90, 1534.00));
    }
}
