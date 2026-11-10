package pe.edu.pucp.sigmeta.dbmanager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;

// Conexiones a la base de datos.
//
// Tiempo de respuesta: la base esta en AWS (us-east-1) y cada ida y vuelta tarda ~200 ms. Abrir una conexion
// SSL nueva a MySQL lleva 8-10 idas y vueltas (~2 s), y antes se abria una por cada pedido. Ahora hay un POOL
// (HikariCP): las conexiones se abren una vez y se reutilizan. Para quien la usa no cambia nada:
// getConnection() entrega una conexion y close() la devuelve al pool (no la cierra de verdad).
public class DBManager {
    private static final HikariDataSource pool;
    private static final DBManager instance = new DBManager();

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // carga el driver de MySQL (necesario dentro de GlassFish)
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se encontro el driver de MySQL", e);
        }
        ResourceBundle db = ResourceBundle.getBundle("db");
        String host = System.getProperty("sigmeta.db.host", db.getString("db.host")).trim();
        int port = Integer.parseInt(System.getProperty("sigmeta.db.port", db.getString("db.port")).trim());
        String username = System.getProperty("sigmeta.db.user", db.getString("db.user")).trim();
        String password = System.getProperty("sigmeta.db.password", db.getString("db.password"));
        String database = System.getProperty("sigmeta.db.database", db.getString("db.database")).trim();

        HikariConfig config = new HikariConfig();
        config.setPoolName("sigmeta");
        // fallbackToSystemKeyStore=false: dentro de GlassFish no usar su almacen de certificados (la conexion
        // sigue cifrada). Lo demas evita idas y vueltas que no hacen falta:
        //   useLocalSessionState / useLocalTransactionState: no reenviar autocommit, aislamiento, commit o
        //     rollback cuando no cambiaron o no hay nada pendiente
        //   cacheServerConfiguration: leer la configuracion del servidor una sola vez
        //   cachePrepStmts / cacheCallableStmts: reutilizar las llamadas a los SP ya preparadas
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?fallbackToSystemKeyStore=false"
                + "&useLocalSessionState=true&useLocalTransactionState=true&cacheServerConfiguration=true"
                + "&cachePrepStmts=true&cacheCallableStmts=true");
        config.setUsername(username);
        config.setPassword(password);
        // las transacciones (Transaccion / transactionContext) trabajan asi; el pool deja cada conexion igual
        config.setAutoCommit(false);
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20_000);   // espera maxima por una conexion libre
        config.setKeepaliveTime(120_000);      // mantiene vivas las conexiones quietas (la red las corta)
        config.setMaxLifetime(600_000);        // renueva cada conexion a los 10 minutos
        // si la base no responde al arrancar, no se cae la aplicacion: se reintenta en el primer pedido
        config.setInitializationFailTimeout(-1);
        pool = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    public static DBManager getInstance() {
        return instance;
    }
}
