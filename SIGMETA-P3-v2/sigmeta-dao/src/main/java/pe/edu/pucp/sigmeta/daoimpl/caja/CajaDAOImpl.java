package pe.edu.pucp.sigmeta.daoimpl.caja;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import pe.edu.pucp.sigmeta.dao.caja.CajaDAO;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoDocumentoIdentidad;
import pe.edu.pucp.sigmeta.model.enums.TipoPersona;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CajaDAOImpl implements CajaDAO {

    private Caja mapearCajaConUsuario(ResultSet rs) throws SQLException {
        Caja caja = new Caja();
        caja.setId(rs.getInt("caja_id"));
        caja.setMontoInicial(rs.getDouble("monto_inicial"));
        caja.setAbierta(rs.getBoolean("abierta"));

        Timestamp tsApertura = rs.getTimestamp("fecha_apertura");
        if (tsApertura != null) {
            caja.setFechaApertura(tsApertura.toLocalDateTime());
        }

        // Mapeo del objeto Usuario usando nombreUsuario
        int idUsuario = rs.getInt("usuario_id");
        if (!rs.wasNull()) {
            Usuario usuario = new Usuario();
            usuario.setId(idUsuario);
            usuario.setNombreUsuario(rs.getString("nombre_usuario"));
            usuario.setNombres(rs.getString("nombres"));
            usuario.setApellidos(rs.getString("apellidos"));
            usuario.setCorreo(rs.getString("correo"));

            caja.setUsuarioApertura(usuario);
        }

        return caja;
    }

    @Override
    public Caja save(Caja caja) throws SQLException {
        String sql = "{call sp_caja_insertar(?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setDouble(2, caja.getMontoInicial());
            cs.setBoolean(3, caja.isAbierta());

            if (caja.getUsuarioApertura() != null && caja.getUsuarioApertura().getId() > 0) {
                cs.setInt(4, caja.getUsuarioApertura().getId());
            } else {
                cs.setNull(4, Types.INTEGER);
            }

            cs.executeUpdate();
            caja.setId(cs.getInt(1));
        }
        return caja;
    }

    @Override
    public Caja update(Caja caja) throws SQLException {
        String sql = "{call sp_caja_modificar(?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, caja.getId());
            cs.setDouble(2, caja.getMontoInicial());
            cs.setBoolean(3, caja.isAbierta());

            if (caja.getUsuarioApertura() != null && caja.getUsuarioApertura().getId() > 0) {
                cs.setInt(4, caja.getUsuarioApertura().getId());
            } else {
                cs.setNull(4, Types.INTEGER);
            }

            cs.executeUpdate();
        }
        return caja;
    }

    @Override
    public void remove(Caja caja) throws SQLException {
        String sql = "{call sp_caja_eliminar(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, caja.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public Caja load(Integer id) throws SQLException {
        Caja caja = null;
        String sql = "{call sp_caja_obtener(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    caja = mapearCajaConUsuario(rs);
                }
            }
        }
        return caja;
    }

    @Override
    public Caja obtenerCajaAbiertaPorUsuario(int idUsuario) throws SQLException {
        Caja caja = null;
        String sql = "{call sp_caja_obtener_abierta_por_usuario(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idUsuario);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    caja = mapearCajaConUsuario(rs);
                }
            }
        }
        return caja;
    }

    @Override
    public List<Caja> listarTodasConUsuario() throws SQLException {
        List<Caja> lista = new ArrayList<>();
        String sql = "{call sp_caja_listar_todas_con_usuario()}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCajaConUsuario(rs));
            }
        }
        return lista;
    }

    // RF010, H10: la base ya entrega solo las ventas pendientes de cobro, con su cliente y comprobante
    // (antes se cargaban todas las ventas, clientes, comprobantes y movimientos de caja)
    @Override
    public List<Venta> listarVentasPorCobrarAlContado() throws SQLException {
        List<Venta> lista = new ArrayList<>();
        String sql = "{call sp_caja_listar_ventas_por_cobrar_contado()}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                Venta venta = new Venta();
                venta.setId(rs.getInt("venta_id"));
                venta.setNumero(rs.getString("venta_numero"));
                java.sql.Date fecha = rs.getDate("fecha_emision");
                if (fecha != null) {
                    venta.setFechaEmision(fecha.toLocalDate());
                }
                venta.setTotal(rs.getDouble("total"));
                venta.setCondicionPago(CondicionPago.valueOf(rs.getString("condicion_pago")));

                Cliente cliente = new Cliente();
                cliente.setId(rs.getInt("cliente_id"));
                cliente.setTipoPersona(TipoPersona.valueOf(rs.getString("tipo_persona")));
                cliente.setTipoDocumento(TipoDocumentoIdentidad.valueOf(rs.getString("tipo_documento")));
                cliente.setNombres(rs.getString("nombres"));
                cliente.setApellidos(rs.getString("apellidos"));
                cliente.setRazonSocial(rs.getString("razon_social"));
                cliente.setNumeroDocumento(rs.getString("numero_documento"));
                venta.setCliente(cliente);

                List<Comprobante> comprobantes = new ArrayList<>();
                int idComprobante = rs.getInt("comprobante_id");
                if (!rs.wasNull()) {
                    Comprobante c = new Comprobante();
                    c.setId(idComprobante);
                    c.setTipo(TipoComprobante.valueOf(rs.getString("comprobante_tipo")));
                    c.setSerie(rs.getString("comprobante_serie"));
                    c.setNumero(rs.getString("comprobante_numero"));
                    c.setEstado(EstadoComprobante.valueOf(rs.getString("comprobante_estado")));
                    c.setVenta(venta);
                    comprobantes.add(c);
                }
                venta.setComprobantes(comprobantes);
                lista.add(venta);
            }
        }
        return lista;
    }
}
