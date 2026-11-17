package pe.edu.pucp.sigmeta.pruebas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static pe.edu.pucp.sigmeta.pruebas.Datos.*;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.daoimpl.ventas.ComprobanteDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.ventas.OrdenCompraClienteDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;

/**
 * H09: numeracion unica de ordenes de compra de clientes y de comprobantes.
 * Requiere haber recargado 2_procedimientos/04_SP_sebastian.sql (su bloque final agrega las restricciones y el correlativo OCC).
 *
 * Las pruebas que intentan guardar un duplicado lo hacen dentro de una transaccion que se revierte al final,
 * asi no dejan registros en la base. Los correlativos se piden para el anio 2099, que no usa nadie.
 *
 *   mvn test -pl sigmeta-ejecucion -am -Dsigmeta.integracion=true -Dsurefire.failIfNoSpecifiedTests=false
 */
@EnabledIfSystemProperty(named = "sigmeta.integracion", matches = "true")
@DisplayName("H09: correlativos y numeros unicos")
class CorrelativosTest {

    private static final int ANIO_DE_PRUEBA = 2099;

    /** Se lanza al final de una transaccion de prueba para que Transaccion haga rollback. */
    private static final class Revertir extends RuntimeException {
        Revertir() {
            super("rollback de prueba");
        }
    }

    @BeforeAll
    static void soloBaseLocal() {
        exigirBaseLocal();
    }

    @Test
    @DisplayName("Ocho reservas simultaneas del correlativo OCC reciben numeros distintos y seguidos")
    void reservasConcurrentesSonUnicas() throws Exception {
        long inicial = Transaccion.consultar(() -> new OrdenCompraClienteDAOImpl().proximoCorrelativo(ANIO_DE_PRUEBA));
        Set<Long> numeros = ConcurrentHashMap.newKeySet();
        ExecutorService hilos = Executors.newFixedThreadPool(8);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> tareas = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) {
                tareas.add(hilos.submit(() -> {
                    largada.await();
                    numeros.add(Transaccion.ejecutar(
                            () -> new OrdenCompraClienteDAOImpl().siguienteCorrelativo(ANIO_DE_PRUEBA)));
                    return null;
                }));
            }
            largada.countDown();
            for (Future<?> t : tareas) {
                t.get(30, TimeUnit.SECONDS);
            }
        } finally {
            hilos.shutdownNow();
        }

        assertEquals(8, numeros.size(), "ningun numero se repite");
        for (long n = inicial; n < inicial + 8; n++) {
            assertTrue(numeros.contains(n), "falta el numero " + n + ": los ocho deben ser seguidos");
        }
        long proximo = Transaccion.consultar(() -> new OrdenCompraClienteDAOImpl().proximoCorrelativo(ANIO_DE_PRUEBA));
        assertEquals(inicial + 8, proximo, "el proximo continua la serie");
    }

    @Test
    @DisplayName("La base rechaza dos ordenes de compra de cliente con el mismo numero")
    void ordenConNumeroRepetidoSeRechaza() throws Exception {
        Cliente cliente = empresaNueva();
        String numero = "OCC-" + ANIO_DE_PRUEBA + "-" + System.nanoTime() % 100000;
        OrdenCompraClienteDAOImpl dao = new OrdenCompraClienteDAOImpl();

        assertThrows(Revertir.class, () -> Transaccion.ejecutar(() -> {
            dao.save(orden(cliente, numero));
            assertThrows(SQLIntegrityConstraintViolationException.class, () -> dao.save(orden(cliente, numero)),
                    "falta UNIQUE(numero) en orden_compra_cliente: recargue 04_SP_sebastian.sql");
            throw new Revertir();
        }));
    }

    @Test
    @DisplayName("La base rechaza un comprobante con serie y numero ya usados")
    void comprobanteConSerieYNumeroRepetidosSeRechaza() throws Exception {
        ComprobanteDAOImpl dao = new ComprobanteDAOImpl();
        List<Comprobante> existentes = Transaccion.consultar(dao::listarTodos);
        assumeFalse(existentes.isEmpty(), "la base no tiene comprobantes para copiar");
        Comprobante original = existentes.get(0);

        assertThrows(Revertir.class, () -> Transaccion.ejecutar(() -> {
            Comprobante copia = dao.load(original.getId());
            copia.setId(0);
            copia.setFechaRegistro(LocalDateTime.now());
            assertThrows(SQLIntegrityConstraintViolationException.class, () -> dao.save(copia),
                    "falta UNIQUE(serie, numero) en comprobante: recargue 04_SP_sebastian.sql");
            throw new Revertir();
        }));
    }

    private static OrdenCompraCliente orden(Cliente cliente, String numero) {
        OrdenCompraCliente o = new OrdenCompraCliente();
        o.setCliente(cliente);
        o.setNumero(numero);
        o.setFechaEmision(LocalDate.now());
        o.setEstado(EstadoOrdenCompraCliente.PENDIENTE);
        o.setSubTotal(10);
        o.setIgv(1.8);
        o.setTotal(11.8);
        Usuario vendedor = new Usuario();
        vendedor.setId(VENDEDOR);
        o.setUsuarioRegistro(vendedor);
        o.setFechaRegistro(LocalDateTime.now());
        return o;
    }
}
