package pe.edu.pucp.dbmanager;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
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
        host = db.getString("host");
        port = Integer.parseInt(db.getString("port"));
        username = db.getString("username");
        password = db.getString("password");
        database = db.getString("database");
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
