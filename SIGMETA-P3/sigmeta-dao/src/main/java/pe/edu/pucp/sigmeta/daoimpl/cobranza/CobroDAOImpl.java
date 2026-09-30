package pe.edu.pucp.sigmeta.daoimpl.cobranza;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import pe.edu.pucp.sigmeta.dao.cobranza.CobroDAO;
import pe.edu.pucp.sigmeta.model.cobranza.Cobro;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CobroDAOImpl implements CobroDAO {

    private Cobro mapearCobro(ResultSet rs) throws SQLException {
        Cobro cobro = new Cobro();
        cobro.setId(rs.getInt("cobro_id"));

        int idCxc = rs.getInt("cuenta_por_cobrar_id");
        if (!rs.wasNull()) {
            CuentaPorCobrar cxc = new CuentaPorCobrar();
            cxc.setId(idCxc);
            cobro.setCuentaPorCobrar(cxc);
        }

        Date fechaCobro = rs.getDate("fecha_cobro");
        if (fechaCobro != null) {
            cobro.setFechaCobro(fechaCobro.toLocalDate());
        }

        String strMedioPago = rs.getString("medio_pago");
        if (strMedioPago != null) {
            cobro.setMedioPago(MedioPago.valueOf(strMedioPago));
        }

        cobro.setMonto(rs.getDouble("monto"));
        cobro.setReferencia(rs.getString("referencia"));

        int idUsuario = rs.getInt("usuario_id");
        if (!rs.wasNull()) {
            Usuario usuario = new Usuario();
            usuario.setId(idUsuario);
            usuario.setNombreUsuario(rs.getString("nombre_usuario"));
            usuario.setNombres(rs.getString("nombres"));
            usuario.setApellidos(rs.getString("apellidos"));
            cobro.setUsuarioRegistro(usuario);
        }

        Timestamp tsRegistro = rs.getTimestamp("fecha_registro");
        if (tsRegistro != null) {
            cobro.setFechaRegistro(tsRegistro.toLocalDateTime());
        }

        return cobro;
    }

    @Override
    public Cobro save(Cobro cobro) throws SQLException {
        String sql = "{call sp_cobro_insertar(?, ?, ?, ?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setInt(2, cobro.getCuentaPorCobrar().getId());
            cs.setDate(3, Date.valueOf(cobro.getFechaCobro()));
            cs.setString(4, cobro.getMedioPago().name());
            cs.setDouble(5, cobro.getMonto());
            cs.setString(6, cobro.getReferencia());

            if (cobro.getUsuarioRegistro() != null && cobro.getUsuarioRegistro().getId() > 0) {
                cs.setInt(7, cobro.getUsuarioRegistro().getId());
            } else {
                cs.setNull(7, Types.INTEGER);
            }

            cs.executeUpdate();
            cobro.setId(cs.getInt(1));
        }
        return cobro;
    }

    @Override
    public Cobro update(Cobro cobro) throws SQLException {
        String sql = "{call sp_cobro_modificar(?, ?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, cobro.getId());
            cs.setDate(2, Date.valueOf(cobro.getFechaCobro()));
            cs.setString(3, cobro.getMedioPago().name());
            cs.setDouble(4, cobro.getMonto());
            cs.setString(5, cobro.getReferencia());

            cs.executeUpdate();
        }
        return cobro;
    }

    @Override
    public void remove(Cobro cobro) throws SQLException {
        String sql = "{call sp_cobro_eliminar(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, cobro.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public Cobro load(Integer id) throws SQLException {
        Cobro cobro = null;
        String sql = "{call sp_cobro_obtener(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    cobro = mapearCobro(rs);
                }
            }
        }
        return cobro;
    }

    @Override
    public List<Cobro> listarTodos() throws SQLException {
        List<Cobro> lista = new ArrayList<>();
        String sql = "{call sp_cobro_listar()}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCobro(rs));
            }
        }
        return lista;
    }

    @Override
    public List<Cobro> listarPorCuentaPorCobrar(int idCuentaPorCobrar) throws SQLException {
        List<Cobro> lista = new ArrayList<>();
        String sql = "{call sp_cobro_listar_por_cuenta_por_cobrar(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idCuentaPorCobrar);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCobro(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public List<Cobro> listarPorCliente(int idCliente) throws SQLException {
        List<Cobro> lista = new ArrayList<>();
        String sql = "{call sp_cobro_listar_por_cliente(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idCliente);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCobro(rs));
                }
            }
        }
        return lista;
    }
}
