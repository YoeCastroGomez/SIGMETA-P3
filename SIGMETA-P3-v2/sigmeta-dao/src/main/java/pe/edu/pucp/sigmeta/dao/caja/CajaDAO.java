package pe.edu.pucp.sigmeta.dao.caja;

import java.sql.SQLException;
import java.util.List;
import pe.edu.pucp.sigmeta.dao.BaseDAO;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.ventas.Venta;

public interface CajaDAO extends BaseDAO<Caja, Integer> {
    Caja obtenerCajaAbiertaPorUsuario(int idUsuario) throws SQLException;
    List<Caja> listarTodasConUsuario() throws SQLException;
    // RF010, H10: ventas al contado vigentes sin cobro en caja, con su cliente y su comprobante vigente
    List<Venta> listarVentasPorCobrarAlContado() throws SQLException;
}