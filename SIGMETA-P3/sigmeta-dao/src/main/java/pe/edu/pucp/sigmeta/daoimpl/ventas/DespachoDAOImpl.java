package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.DespachoDAO;
import pe.edu.pucp.sigmeta.model.almacen.Despacho;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DespachoDAOImpl implements DespachoDAO {

    private Despacho mapear(ResultSet rs) throws SQLException {
        Despacho d = new Despacho();
        d.setId(rs.getInt("id"));

        Venta v = new Venta();
        v.setId(rs.getInt("id_venta"));
        d.setVenta(v);

        d.setSerieGuia(rs.getString("serie_guia"));
        d.setNumeroGuia(rs.getString("numero_guia"));

        Date fDespacho = rs.getDate("fecha_despacho");
        if (fDespacho != null) {
            d.setFechaDespacho(fDespacho.toLocalDate());
        }

        d.setDireccionEntrega(rs.getString("direccion_entrega"));
        d.setTransportista(rs.getString("transportista"));
        d.setAnulado(rs.getBoolean("anulado"));

        int idUser = rs.getInt("id_usuario_registro");
        if (!rs.wasNull()) {
            Usuario u = new Usuario();
            u.setId(idUser);
            d.setUsuarioRegistro(u);
        }

        Timestamp fRegistro = rs.getTimestamp("fecha_registro");
        if (fRegistro != null) {
            d.setFechaRegistro(fRegistro.toLocalDateTime());
        }

        return d;
    }

    @Override
    public Despacho load(Integer id) throws SQLException {
        String sql = "{CALL sp_despacho_obtener(?)}";
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
    public Despacho save(Despacho d) throws SQLException {
        String sql = "{CALL sp_despacho_insertar(?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getVenta().getId());
            cs.setString(2, d.getSerieGuia());
            cs.setString(3, d.getNumeroGuia());
            cs.setDate(4, Date.valueOf(d.getFechaDespacho()));
            cs.setString(5, d.getDireccionEntrega());
            cs.setString(6, d.getTransportista());

            if (d.getUsuarioRegistro() != null) {
                cs.setInt(7, d.getUsuarioRegistro().getId());
            } else {
                cs.setNull(7, Types.INTEGER);
            }

            cs.registerOutParameter(8, Types.INTEGER);
            cs.execute();
            d.setId(cs.getInt(8));
            return d;
        }
    }

    @Override
    public Despacho update(Despacho d) throws SQLException {
        String sql = "{CALL sp_despacho_modificar(?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.setString(2, d.getDireccionEntrega());
            cs.setString(3, d.getTransportista());
            cs.executeUpdate();
            return d;
        }
    }

    @Override
    public void remove(Despacho d) throws SQLException {
        String sql = "{CALL sp_despacho_eliminar(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, d.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public List<Despacho> listarTodos() throws SQLException {
        String sql = "{CALL sp_despacho_listar()}";
        List<Despacho> lista = new ArrayList<>();
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
    public List<Despacho> listarPorVenta(int idVenta) throws SQLException {
        String sql = "{CALL sp_despacho_listar_por_venta(?)}";
        List<Despacho> lista = new ArrayList<>();
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
        String sql = "{CALL sp_despacho_siguiente_correlativo(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setString(1, serie);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();
            return cs.getString(2);
        }
    }
}
