package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.ventas.DetalleCotizacion;

import java.sql.SQLException;
import java.util.List;

public interface DetalleCotizacionDAO extends BaseDAO<DetalleCotizacion, Integer> {
    List<DetalleCotizacion> listarTodos() throws SQLException;
    List<DetalleCotizacion> listar_por_cotizacion(Integer idCotizacion) throws SQLException;
}
