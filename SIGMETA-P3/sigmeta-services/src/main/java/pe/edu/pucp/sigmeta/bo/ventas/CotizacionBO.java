package pe.edu.pucp.sigmeta.bo.ventas;

import pe.edu.pucp.sigmeta.model.enums.EstadoCotizacion;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;

import java.sql.SQLException;
import java.util.List;

// RF007: la cotizacion se gestiona junto con sus detalles
public interface CotizacionBO {
    // idResponsable: usuario en sesion; debe ser Vendedor (anular tambien el Administrador)
    Cotizacion registrar(Cotizacion cotizacion, int idResponsable) throws SQLException;
    // reemplaza la cabecera y todas las lineas; solo en estado REGISTRADA
    Cotizacion modificar(Cotizacion cotizacion, int idResponsable) throws SQLException;
    void anular(int idCotizacion, String motivo, int idResponsable) throws SQLException;
    // incluye los detalles
    Cotizacion obtener(int idCotizacion) throws SQLException;
    List<Cotizacion> listarTodos() throws SQLException;
    List<Cotizacion> listarPorCliente(int idCliente) throws SQLException;
    List<Cotizacion> listarPorEstado(EstadoCotizacion estado) throws SQLException;

    // ciclo de vida: REGISTRADA -> ENVIADA -> ACEPTADA | RECHAZADA
    Cotizacion enviar(int idCotizacion, int idResponsable) throws SQLException;
    Cotizacion aceptar(int idCotizacion, int idResponsable) throws SQLException;
    Cotizacion rechazar(int idCotizacion, int idResponsable) throws SQLException;
    // pasa a VENCIDA las cotizaciones REGISTRADAS o ENVIADAS cuya vigencia ya paso; devuelve cuantas
    int marcarVencidas() throws SQLException;
}
