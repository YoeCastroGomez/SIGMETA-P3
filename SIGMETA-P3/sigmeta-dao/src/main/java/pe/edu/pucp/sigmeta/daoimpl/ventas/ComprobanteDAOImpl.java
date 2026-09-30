package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.ComprobanteDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComprobanteDAOImpl implements ComprobanteDAO {

    private Comprobante mapear(ResultSet rs) throws SQLException {
        Comprobante c = new Comprobante();
        c.setId(rs.getInt("id"));

        Venta v = new Venta();
        v.setId(rs.getInt("id_venta"));
        c.setVenta(v);

        c.setTipo(TipoComprobante.valueOf(rs.getString("tipo")));
        c.setSerie(rs.getString("serie"));
        c.setNumero(rs.getString("numero"));

        Date fEmision = rs.getDate("fecha_emision");
        if (fEmision != null) {
            c.setFechaEmision(fEmision.toLocalDate());
        }

        c.setMoneda(Moneda.valueOf(rs.getString("moneda")));
        c.setSubTotal(rs.getDouble("sub_total"));
        c.setIgv(rs.getDouble("igv"));
        c.setTotal(rs.getDouble("total"));
        c.setEstado(EstadoComprobante.valueOf(rs.getString("estado")));

        int idRel = rs.getInt("id_comprobante_relacionado");
        if (!rs.wasNull()) {
            Comprobante rel = new Comprobante();
            rel.setId(idRel);
            c.setComprobanteRelacionado(rel);
        }

        c.setMotivo(rs.getString("motivo"));
        c.setMedioEnvio(rs.getString("medio_envio"));

        Timestamp fEnvio = rs.getTimestamp("fecha_envio");
        if (fEnvio != null) {
            c.setFechaEnvio(fEnvio.toLocalDateTime());
        }

        Timestamp fRegistro = rs.getTimestamp("fecha_registro");
        if (fRegistro != null) {
            c.setFechaRegistro(fRegistro.toLocalDateTime());
        }

        return c;
    }

    @Override
    public Comprobante load(Integer id) throws SQLException {
        String sql = "{CALL sp_comprobante_obtener(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Comprobante save(Comprobante c) throws SQLException {
        String sql = "{CALL sp_comprobante_insertar(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, c.getVenta().getId());
            cs.setString(2, c.getTipo().name());
            cs.setString(3, c.getSerie());
            cs.setString(4, c.getNumero());
            cs.setDate(5, Date.valueOf(c.getFechaEmision()));
            cs.setString(6, c.getMoneda().name());
            cs.setDouble(7, c.getSubTotal());
            cs.setDouble(8, c.getIgv());
            cs.setDouble(9, c.getTotal());
            cs.setString(10, c.getEstado().name());

            if (c.getComprobanteRelacionado() != null && c.getComprobanteRelacionado().getId() > 0) {
                cs.setInt(11, c.getComprobanteRelacionado().getId());
            } else {
                cs.setNull(11, Types.INTEGER);
            }

            cs.setString(12, c.getMotivo());
            cs.setString(13, c.getMedioEnvio());

            if (c.getFechaEnvio() != null) {
                cs.setTimestamp(14, Timestamp.valueOf(c.getFechaEnvio()));
            } else {
                cs.setNull(14, Types.TIMESTAMP);
            }

            cs.registerOutParameter(15, Types.INTEGER);
            cs.execute();
            c.setId(cs.getInt(15));
            return c;
        }
    }

    @Override
    public Comprobante update(Comprobante c) throws SQLException {
        String sql = "{CALL sp_comprobante_modificar(?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, c.getId());
            cs.setString(2, c.getMotivo());
            cs.setString(3, c.getMedioEnvio());
            if (c.getFechaEnvio() != null) {
                cs.setTimestamp(4, Timestamp.valueOf(c.getFechaEnvio()));
            } else {
                cs.setNull(4, Types.TIMESTAMP);
            }
            cs.executeUpdate();
            return c;
        }
    }

    @Override
    public void remove(Comprobante c) throws SQLException {
        String sql = "{CALL sp_comprobante_eliminar(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, c.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public List<Comprobante> listarTodos() throws SQLException {
        String sql = "{CALL sp_comprobante_listar()}";
        List<Comprobante> lista = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public List<Comprobante> listarPorVenta(int idVenta) throws SQLException {
        String sql = "{CALL sp_comprobante_listar_por_venta(?)}";
        List<Comprobante> lista = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idVenta);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public String siguienteCorrelativo(String serie) throws SQLException {
        String sql = "{CALL sp_comprobante_siguiente_correlativo(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setString(1, serie);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();
            return cs.getString(2);
        }
    }

    @Override
    public List<Comprobante> listarNotasCreditoPorComprobante(int idComprobanteRelacionado) throws SQLException {
        String sql = "{CALL sp_comprobante_listar_nc_por_relacionado(?)}";
        List<Comprobante> lista = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idComprobanteRelacionado);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public void actualizarEstado(int id, EstadoComprobante estado) throws SQLException {
        String sql = "{CALL sp_comprobante_actualizar_estado(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            cs.setString(2, estado.name());
            cs.executeUpdate();
        }
    }

    @Override
    public void revertirSaldoCuentaPorCobrar(int idVenta, double montoReversion) throws SQLException {
        String sql = "{CALL sp_comprobante_revertir_saldo_cpc(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idVenta);
            cs.setDouble(2, montoReversion);
            cs.executeUpdate();
        }
    }

    @Override
    public void restaurarSaldoCuentaPorCobrar(int idVenta, double monto) throws SQLException {
        String sql = "{CALL sp_comprobante_restaurar_saldo_cpc(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idVenta);
            cs.setDouble(2, monto);
            cs.executeUpdate();
        }
    }
}
