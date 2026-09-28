package pe.edu.pucp.sigmeta.ejecucion_CRUD;
import pe.edu.pucp.sigmeta.dbmanager.DBManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class PruebaInsert {

    public static void main(String[] args) throws SQLException {
        String sql = "insert into usuario (nombre_usuario, clave_hash, salt, nombres, apellidos, " +
                     "correo, id_rol, estado, fecha_registro) values (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try(    // realize connectivity
                Connection connection = DBManager.getInstance().getConnection();
                // type of statement
                PreparedStatement pstm = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);){
            pstm.setString(1, "rflores");
            pstm.setString(2, "hash_rflores");
            pstm.setString(3, "salt_rflores");
            pstm.setString(4, "Rosa");
            pstm.setString(5, "Flores Diaz");
            pstm.setString(6, "rflores@grupometa.pe");
            pstm.setInt(7, 1);
            pstm.setBoolean(8, true);
            pstm.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));

            int filas = pstm.executeUpdate();
            System.out.println("Filas insertadas: " + filas);

            try(ResultSet rs = pstm.getGeneratedKeys();){
                if(rs.next()){
                    System.out.println("Nuevo id_usuario: " + rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in database");
            e.printStackTrace();
        }

    }

}
