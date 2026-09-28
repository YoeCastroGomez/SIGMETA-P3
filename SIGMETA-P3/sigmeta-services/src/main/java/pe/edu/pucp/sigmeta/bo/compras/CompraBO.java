package pe.edu.pucp.sigmeta.bo.compras;

import pe.edu.pucp.sigmeta.model.compras.Compra;
import java.sql.SQLException;
import java.util.List;

public interface CompraBO {
    Compra registrar(Compra compra) throws SQLException;
    Compra modificar(Compra compra) throws SQLException;
    void anular(int idCompra) throws SQLException;
    Compra obtener(int idCompra) throws SQLException;
    List<Compra> listarTodos() throws SQLException;
}
