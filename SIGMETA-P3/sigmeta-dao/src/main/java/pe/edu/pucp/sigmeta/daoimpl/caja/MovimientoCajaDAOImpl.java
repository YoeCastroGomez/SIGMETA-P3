package pe.edu.pucp.sigmeta.daoimpl.caja;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import pe.edu.pucp.sigmeta.dao.caja.MovimientoCajaDAO;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class MovimientoCajaDAOImpl implements MovimientoCajaDAO {

    private MovimientoCaja mapearMovimientoConDetalle(ResultSet rs) throws SQLException {
        MovimientoCaja mov = new MovimientoCaja();
        mov.setId(rs.getInt("mov_id"));

        Caja caja = new Caja();
        caja.setId(rs.getInt("caja_id"));
        mov.setCaja(caja);

        String tipoStr = rs.getString("tipo");
        if (tipoStr != null) {
            mov.setTipo(TipoMovimientoCaja.valueOf(tipoStr));
        }

        Timestamp ts = rs.getTimestamp("fecha_movimiento");
        if (ts != null) {
            mov.setFechaMovimiento(ts.toLocalDateTime());
        }

        String medioPagoStr = rs.getString("medio_pago");
        if (medioPagoStr != null) {
            mov.setMedioPago(MedioPago.valueOf(medioPagoStr));
        }

        mov.setMonto(rs.getDouble("monto"));
        mov.setConcepto(rs.getString("concepto"));
        mov.setDocumentoOrigen(rs.getString("documento_origen"));
        mov.setIdDocumentoOrigen(rs.getInt("id_documento_origen"));

        // Mapeo directo a la clase Usuario
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("usuario_id"));
        usuario.setNombreUsuario(rs.getString("nombre_usuario"));
        usuario.setNombres(rs.getString("nombres"));
        usuario.setApellidos(rs.getString("apellidos"));
        usuario.setCorreo(rs.getString("correo"));

        mov.setUsuarioRegistro(usuario);

        return mov;
    }

    @Override
    public MovimientoCaja save(MovimientoCaja mov) throws SQLException {
        String sql = "{call sp_movimiento_caja_insertar(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, mov.getCaja().getId());
            cs.setString(3, mov.getTipo() != null ? mov.getTipo().name() : null);
            cs.setString(4, mov.getMedioPago() != null ? mov.getMedioPago().name() : null);
            cs.setDouble(5, mov.getMonto());
            cs.setString(6, mov.getConcepto());
            cs.setString(7, mov.getDocumentoOrigen());

            if (mov.getIdDocumentoOrigen() > 0) {
                cs.setInt(8, mov.getIdDocumentoOrigen());
            } else {
                cs.setNull(8, Types.INTEGER);
            }

            cs.setInt(9, mov.getUsuarioRegistro().getId());

            cs.executeUpdate();
            mov.setId(cs.getInt(1));
        }
        return mov;
    }

    @Override
    public MovimientoCaja update(MovimientoCaja mov) throws SQLException {
        String sql = "{call sp_movimiento_caja_modificar(?, ?, ?, ?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, mov.getId());
            cs.setString(2, mov.getTipo() != null ? mov.getTipo().name() : null);
            cs.setString(3, mov.getMedioPago() != null ? mov.getMedioPago().name() : null);
            cs.setDouble(4, mov.getMonto());
            cs.setString(5, mov.getConcepto());
            cs.setString(6, mov.getDocumentoOrigen());

            if (mov.getIdDocumentoOrigen() > 0) {
                cs.setInt(7, mov.getIdDocumentoOrigen());
            } else {
                cs.setNull(7, Types.INTEGER);
            }

            cs.executeUpdate();
        }
        return mov;
    }

    @Override
    public void remove(MovimientoCaja mov) throws SQLException {
        String sql = "{call sp_movimiento_caja_eliminar(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, mov.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public MovimientoCaja load(Integer id) throws SQLException {
        MovimientoCaja mov = null;
        String sql = "{call sp_movimiento_caja_obtener(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    mov = mapearMovimientoConDetalle(rs);
                }
            }
        }
        return mov;
    }

    @Override
    public List<MovimientoCaja> listarPorCaja(int idCaja) throws SQLException {
        List<MovimientoCaja> lista = new ArrayList<>();
        String sql = "{call sp_movimiento_caja_listar_por_caja(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idCaja);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMovimientoConDetalle(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public Map<MedioPago, Double> obtenerTotalesPorMedioPago(int idCaja) throws SQLException {
        Map<MedioPago, Double> resumen = new EnumMap<>(MedioPago.class);
        String sql = "{call sp_movimiento_caja_totales_medio_pago(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idCaja);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    String mpStr = rs.getString("medio_pago");
                    double total = rs.getDouble("total_monto");
                    if (mpStr != null) {
                        resumen.put(MedioPago.valueOf(mpStr), total);
                    }
                }
            }
        }
        return resumen;
    }

    @Override
    public List<MovimientoCaja> buscarPorRangoFechas(int idCaja, LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        List<MovimientoCaja> lista = new ArrayList<>();
        String sql = "{call sp_movimiento_caja_buscar_por_fechas(?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idCaja);
            cs.setTimestamp(2, Timestamp.valueOf(inicio));
            cs.setTimestamp(3, Timestamp.valueOf(fin));
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMovimientoConDetalle(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public List<MovimientoCaja> listarTodosConDetalle() throws SQLException {
        List<MovimientoCaja> lista = new ArrayList<>();
        String sql = "{call sp_movimiento_caja_listar_con_detalle()}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearMovimientoConDetalle(rs));
            }
        }
        return lista;
    }
}
