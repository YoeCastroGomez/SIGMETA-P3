package pe.edu.pucp.sigmeta.bo.almacen;

import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;

import java.sql.SQLException;
import java.util.List;

public interface RecepcionCompraBO {

    RecepcionCompra registrar(RecepcionCompra recepcion) throws SQLException;

    RecepcionCompra modificar(RecepcionCompra recepcion) throws SQLException;

    RecepcionCompra obtener(int idRecepcion) throws SQLException;

    List<RecepcionCompra> listarTodos() throws SQLException;
}
