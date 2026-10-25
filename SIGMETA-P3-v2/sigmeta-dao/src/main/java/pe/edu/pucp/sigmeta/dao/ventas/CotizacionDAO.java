package pe.edu.pucp.sigmeta.dao.ventas;

import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;

import java.sql.SQLException;
import java.util.List;

public interface CotizacionDAO extends BaseDAO<Cotizacion, Integer> {
    List<Cotizacion> listarTodos() throws SQLException;
}
