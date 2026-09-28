package pe.edu.pucp.sigmeta.bo.compras;

import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;

import java.sql.SQLException;
import java.util.List;

public interface DetalleCompraBO {

    DetalleCompra registrar(DetalleCompra detalle) throws SQLException;

    DetalleCompra modificar(DetalleCompra detalle) throws SQLException;

    void eliminar(int idDetalleCompra) throws SQLException;

    DetalleCompra obtener(int idDetalleCompra) throws SQLException;

    List<DetalleCompra> listarTodos() throws SQLException;
}
