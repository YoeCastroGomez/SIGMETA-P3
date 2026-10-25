package pe.edu.pucp.sigmeta.boimpl;

import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;

// RNF002: ejecuta una operacion de negocio dentro de una transaccion.
// La operacion de afuera abre la transaccion, confirma (commit) o revierte (rollback) y la cierra.
// Si es una llamada anidada desde otro BO, solo ejecuta: la transaccion de afuera decide.
public final class Transaccion {

    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar() throws SQLException;
    }

    // cuantas operaciones de negocio estan en curso en el hilo. No basta con ver si hay una conexion
    // abierta: los DAO la abren al primer uso, tambien en las lecturas que un BO hace antes de entrar aqui
    private static final ThreadLocal<Integer> profundidad = ThreadLocal.withInitial(() -> 0);

    private Transaccion() {
    }

    public static <T> T ejecutar(Operacion<T> operacion) throws SQLException {
        int nivel = profundidad.get();
        boolean propia = nivel == 0;
        if (propia) {
            // una conexion que quedo de lecturas previas solo tiene lecturas: se descarta para empezar limpio
            transactionContext.rollback();
            transactionContext.close();
        }
        profundidad.set(nivel + 1);
        try {
            if (propia) {
                // Protocolo comun a TODOS los BO: una fila InnoDB, no un mutex de una JVM.
                // Evita actualizaciones perdidas entre cobros, notas, ventas, anulaciones y cajas.
                // Compromiso deliberado: menor concurrencia, sin introducir ordenes de bloqueo incompatibles.
                try (java.sql.CallableStatement cs = transactionContext.getConnection()
                        .prepareCall("{call sp_transaccion_bloquear()}")) {
                    cs.execute();
                }
            }
            T resultado = operacion.ejecutar();
            if (propia) {
                transactionContext.commit();
            }
            return resultado;
        } catch (SQLException | RuntimeException e) {
            if (propia) {
                transactionContext.rollback();
            }
            throw e;
        } finally {
            profundidad.set(nivel);
            if (propia) {
                transactionContext.close();
            }
        }
    }
}
