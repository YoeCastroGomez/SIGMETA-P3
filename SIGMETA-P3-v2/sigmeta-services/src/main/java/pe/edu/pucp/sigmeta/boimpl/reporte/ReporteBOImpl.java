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
            // RF013: seguimiento de las notas de credito emitidas en el periodo
            reporte.put("notasCredito", consultaDAO.consultar("sp_reporte_notas_credito", desde, hasta));
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
            // RF009 + RF013 + RNF001: cobros, notas de credito y cuentas anuladas en el periodo (quien, cuando, por que)
            reporte.put("anulaciones", consultaDAO.consultar("sp_reporte_anulaciones", desde, hasta));
            return reporte;
        });
    }

    // RF015: el reporte en PDF. Usa los mismos datos que la pantalla (y por eso las mismas validaciones de rol y
    // periodo) y JasperReports solo les da forma de documento (ReportePdf).
    @Override
    public byte[] pdf(String tipo, LocalDate desde, LocalDate hasta, int idAdministrador) throws SQLException {
        String cual = tipo == null ? "" : tipo.trim().toLowerCase(java.util.Locale.ROOT);
        ReportePdf.nombre(cual);   // rechaza un tipo desconocido antes de consultar la base
        Map<String, Object> datos = switch (cual) {
            case ReportePdf.COMERCIAL -> comercial(desde, hasta, idAdministrador);
            case ReportePdf.ABASTECIMIENTO -> abastecimiento(desde, hasta, idAdministrador);
            default -> caja(desde, hasta, idAdministrador);
        };
        pe.edu.pucp.sigmeta.model.usuario.Usuario administrador = usuarioBO.obtenerAdministradorActivo(idAdministrador);
        String nombre = (texto(administrador.getNombres()) + " " + texto(administrador.getApellidos())).trim();
        String generadoPor = nombre.isEmpty() ? texto(administrador.getNombreUsuario())
                : nombre + " (" + texto(administrador.getNombreUsuario()) + ")";
        return ReportePdf.generar(cual, datos, generadoPor, java.time.LocalDateTime.now());
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
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
