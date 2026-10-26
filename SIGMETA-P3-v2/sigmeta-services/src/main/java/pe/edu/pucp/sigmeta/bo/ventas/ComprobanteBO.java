package pe.edu.pucp.sigmeta.bo.ventas;

import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface ComprobanteBO {
    Comprobante emitir(int idVenta, TipoComprobante tipo, int idResponsable) throws SQLException;
    Comprobante registrarNotaCredito(Comprobante notaCredito, int idAdministradorAutoriza, int idResponsable) throws SQLException;
    Comprobante modificarNotaCredito(Comprobante nc, int idResponsable) throws SQLException;
    void anular(int idComprobante, String motivo, int idResponsable) throws SQLException;
    Comprobante obtener(int id) throws SQLException;
    List<Comprobante> listarTodos() throws SQLException;
    Comprobante registrarEnvio(int idComprobante, String medioEnvio, int idResponsable) throws SQLException;
    List<Comprobante> buscarNotasCredito(LocalDate fechaDesde, LocalDate fechaHasta, Integer idCliente, String serieNumeroOrigen) throws SQLException;
}
