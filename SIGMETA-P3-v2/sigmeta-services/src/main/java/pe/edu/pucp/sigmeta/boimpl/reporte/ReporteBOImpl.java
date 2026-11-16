package pe.edu.pucp.sigmeta.boimpl.reporte;

import pe.edu.pucp.sigmeta.bo.reporte.ReporteBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.ConsultaDAO;
import pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReporteBOImpl implements ReporteBO {

    // un reporte de mas de dos anios no entra en una pantalla ni cumple el RNF003
    private static final long DIAS_MAXIMOS = 731;

    private final ConsultaDAO consultaDAO = new ConsultaDAOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    @Override
    public Map<String, Object> comercial(LocalDate desde, LocalDate hasta, int idAdministrador) throws SQLException {
        validarPeriodo(desde, hasta);
        return Transaccion.consultar(() -> {
            usuarioBO.obtenerAdministradorActivo(idAdministrador);
            Map<String, Object> reporte = periodo(desde, hasta);
            reporte.put("totales", primera(consultaDAO.consultar("sp_reporte_ventas_totales", desde, hasta)));
            reporte.put("porDia", consultaDAO.consultar("sp_reporte_ventas_por_dia", desde, hasta));
            reporte.put("porVendedor", consultaDAO.consultar("sp_reporte_ventas_por_vendedor", desde, hasta));
            reporte.put("porCliente", consultaDAO.consultar("sp_reporte_ventas_por_cliente", desde, hasta));
            reporte.put("credito", consultaDAO.consultar("sp_reporte_credito_clientes"));
            return reporte;
        });
    }

    @Override
    public Map<String, Object> abastecimiento(LocalDate desde, LocalDate hasta, int idAdministrador) throws SQLException {
        validarPeriodo(desde, hasta);
        return Transaccion.consultar(() -> {
            usuarioBO.obtenerAdministradorActivo(idAdministrador);
            Map<String, Object> reporte = periodo(desde, hasta);
            reporte.put("compras", consultaDAO.consultar("sp_reporte_compras_por_proveedor", desde, hasta));
            reporte.put("mayorMovimiento", consultaDAO.consultar("sp_reporte_productos_mayor_movimiento", desde, hasta));
            reporte.put("sinMovimiento", consultaDAO.consultar("sp_reporte_productos_sin_movimiento", desde, hasta));
            reporte.put("inventario", consultaDAO.consultar("sp_reporte_inventario_valorizado"));
            return reporte;
        });
    }

    @Override
    public Map<String, Object> caja(LocalDate desde, LocalDate hasta, int idAdministrador) throws SQLException {
        validarPeriodo(desde, hasta);
        return Transaccion.consultar(() -> {
            usuarioBO.obtenerAdministradorActivo(idAdministrador);
            Map<String, Object> reporte = periodo(desde, hasta);
            reporte.put("cierres", consultaDAO.consultar("sp_reporte_caja_cierres", desde, hasta));
            reporte.put("porMedioPago", consultaDAO.consultar("sp_reporte_caja_por_medio_pago", desde, hasta));
            reporte.put("porCajero", consultaDAO.consultar("sp_reporte_caja_por_cajero", desde, hasta));
            return reporte;
        });
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        Validador.obligatorio(desde, "fecha de inicio");
        Validador.obligatorio(hasta, "fecha de fin");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > DIAS_MAXIMOS) {
            throw new IllegalArgumentException("El periodo del reporte no puede pasar de dos años");
        }
    }

    private static Map<String, Object> periodo(LocalDate desde, LocalDate hasta) {
        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("desde", desde.toString());
        reporte.put("hasta", hasta.toString());
        return reporte;
    }

    private static Map<String, Object> primera(List<Map<String, Object>> filas) {
        return filas.isEmpty() ? new LinkedHashMap<>() : filas.get(0);
    }
}
