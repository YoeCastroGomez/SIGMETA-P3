package pe.edu.pucp.sigmeta.ejecucion;
import pe.edu.pucp.dbmanager.DBManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

public class PruebaSelect{

    public static void main(String[] args)throws SQLException{
        String sql = "select * from usuario;";
       try(    // realize connectivity
               Connection connection = DBManager.getInstance().getConnection();
               // type of statement
               PreparedStatement pstm = connection.prepareStatement(sql);
               ResultSet rs = pstm.executeQuery();){
           while(rs.next()){
               System.out.println(rs.getInt("id_usuario") + "  " +rs.getString("nombre_usuario"));
           }
       } catch (SQLException e) {
           System.out.println("Error in database");
           e.printStackTrace();
       }

    }

}
