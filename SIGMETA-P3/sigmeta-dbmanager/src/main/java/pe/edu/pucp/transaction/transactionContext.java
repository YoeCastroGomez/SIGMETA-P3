package pe.edu.pucp.transaction;

import pe.edu.pucp.dbmanager.DBManager;

import java.sql.Connection;
import java.sql.SQLException;

public class transactionContext {
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public static Connection getConnection() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn == null){
            conn = DBManager.getInstance().getConnection();
            conn.setAutoCommit(false);
            connectionHolder.set(conn);

        }
        return conn;
    }


    public static void commit() throws SQLException{
        Connection conn = connectionHolder.get();
        if (conn != null){
            conn.commit();
        }
    }

    public static void rollback(){
        Connection conn = connectionHolder.get();
        if (conn != null){
            try{
                conn.rollback();
            } catch (SQLException e){
                e.printStackTrace();
            }

        }
    }

    public static void close    (){
        Connection conn = connectionHolder.get();
        if (conn != null){
            try{
                conn.close();
            } catch (SQLException e){
                e.printStackTrace();
            }
            connectionHolder.remove();

        }
    }
}
