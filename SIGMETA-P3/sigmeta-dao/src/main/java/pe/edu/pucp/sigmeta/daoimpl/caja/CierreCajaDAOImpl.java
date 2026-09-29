package pe.edu.pucp.sigmeta.daoimpl.caja;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import pe.edu.pucp.sigmeta.dao.caja.CierreCajaDAO;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CierreCajaDAOImpl implements CierreCajaDAO {

    private CierreCaja mapearCierreCajaConDetalle(ResultSet rs) throws SQLException {
        CierreCaja cierre = new CierreCaja();
        cierre.setId(rs.getInt("cierre_id"));

        Caja caja = new Caja();
        caja.setId(rs.getInt("caja_id"));
        cierre.setCaja(caja);

        Timestamp ts = rs.getTimestamp("fecha_cierre");
        if (ts != null) {
            cierre.setFechaCierre(ts.toLocalDateTime());
        }

        cierre.setMontoCalculado(rs.getDouble("monto_calculado"));
        cierre.setMontoDeclarado(rs.getDouble("monto_declarado"));
        cierre.setDiferencia(rs.getDouble("diferencia"));

        // Mapeo directo a la clase Usuario
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("usuario_id"));
        usuario.setNombreUsuario(rs.getString("nombre_usuario"));
        usuario.setNombres(rs.getString("nombres"));
        usuario.setApellidos(rs.getString("apellidos"));
        usuario.setCorreo(rs.getString("correo"));

        cierre.setUsuarioCierre(usuario);

        return cierre;
    }

    @Override
    public CierreCaja save(CierreCaja cierre) throws SQLException {
        String sql = "{call sp_cierre_caja_insertar(?, ?, ?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, cierre.getCaja().getId());
            cs.setDouble(3, cierre.getMontoCalculado());
            cs.setDouble(4, cierre.getMontoDeclarado());
            cs.setDouble(5, cierre.getDiferencia());
            cs.setInt(6, cierre.getUsuarioCierre().getId());

            cs.executeUpdate();
            cierre.setId(cs.getInt(1));
        }
        return cierre;
    }

    @Override
    public CierreCaja update(CierreCaja cierre) throws SQLException {
        String sql = "{call sp_cierre_caja_modificar(?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, cierre.getId());
            cs.setDouble(2, cierre.getMontoCalculado());
            cs.setDouble(3, cierre.getMontoDeclarado());
            cs.setDouble(4, cierre.getDiferencia());
            cs.executeUpdate();
        }
        return cierre;
    }

    @Override
    public void remove(CierreCaja cierre) throws SQLException {
        String sql = "{call sp_cierre_caja_eliminar(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, cierre.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public CierreCaja load(Integer id) throws SQLException {
        CierreCaja cierre = null;
        String sql = "{call sp_cierre_caja_obtener(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    cierre = mapearCierreCajaConDetalle(rs);
                }
            }
        }
        return cierre;
    }

    @Override
    public CierreCaja obtenerPorCaja(int idCaja) throws SQLException {
        CierreCaja cierre = null;
        String sql = "{call sp_cierre_caja_obtener_por_caja(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idCaja);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    cierre = mapearCierreCajaConDetalle(rs);
                }
            }
        }
        return cierre;
    }

    @Override
    public double calcularMontoEsperado(int idCaja) throws SQLException {
        double montoEsperado = 0.0;
        String sql = "{call sp_cierre_caja_calcular_monto_esperado(?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idCaja);
            cs.registerOutParameter(2, Types.DECIMAL);
            cs.execute();
            montoEsperado = cs.getDouble(2);
        }
        return montoEsperado;
    }

    @Override
    public List<CierreCaja> buscarPorRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException {
        List<CierreCaja> lista = new ArrayList<>();
        String sql = "{call sp_cierre_caja_buscar_por_fechas(?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setTimestamp(1, Timestamp.valueOf(fechaInicio));
            cs.setTimestamp(2, Timestamp.valueOf(fechaFin));
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCierreCajaConDetalle(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public List<CierreCaja> listarTodosConDetalle() throws SQLException {
        List<CierreCaja> lista = new ArrayList<>();
        String sql = "{call sp_cierre_caja_listar_con_detalle()}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCierreCajaConDetalle(rs));
            }
        }
        return lista;
    }
}