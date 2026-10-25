package pe.edu.pucp.sigmeta.dbmanager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class DBManager {
    private static String host;
    private static String username;
    private static int port;
    private static String password;
    private static String database;
    private static DBManager instance;


    static {
        ResourceBundle db = ResourceBundle.getBundle("db");
        host = System.getProperty("sigmeta.db.host", db.getString("db.host"));
        port = Integer.parseInt(System.getProperty("sigmeta.db.port", db.getString("db.port")));
        username = System.getProperty("sigmeta.db.user", db.getString("db.user"));
        password = System.getProperty("sigmeta.db.password", db.getString("db.password"));
        database = System.getProperty("sigmeta.db.database", db.getString("db.database"));
        instance = new DBManager();
    }

    public Connection getConnection() throws SQLException{
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
        return DriverManager.getConnection(url,username,password);
    }

    public static DBManager getInstance(){
        if (instance == null)
            instance = new DBManager();
        return instance;
    }
}
