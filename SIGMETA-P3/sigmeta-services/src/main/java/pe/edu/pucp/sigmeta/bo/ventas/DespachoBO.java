package pe.edu.pucp.sigmeta.bo.ventas;

import pe.edu.pucp.sigmeta.model.almacen.Despacho;

import java.sql.SQLException;
import java.util.List;

public interface DespachoBO {
    Despacho registrar(Despacho despacho, int idAlmacenero) throws SQLException;
    Despacho modificar(Despacho despacho, int idAlmacenero) throws SQLException;
    void anular(int idDespacho, String motivo, int idAlmacenero) throws SQLException;
    Despacho obtener(int id) throws SQLException;
    List<Despacho> listarTodos() throws SQLException;
}
