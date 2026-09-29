package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.OrdenCompraClienteDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrdenCompraClienteDAOImpl implements OrdenCompraClienteDAO {

    private OrdenCompraCliente mapear(ResultSet rs) throws SQLException {
        OrdenCompraCliente oc = new OrdenCompraCliente();
        oc.setId(rs.getInt("id"));

        Cliente cli = new Cliente();
        cli.setId(rs.getInt("id_cliente"));
        oc.setCliente(cli);

        int idCot = rs.getInt("id_cotizacion");
        if (!rs.wasNull()) {
            Cotizacion cot = new Cotizacion();
            cot.setId(idCot);
            oc.setCotizacion(cot);
        }

        oc.setNumeroOrdenCliente(rs.getString("numero_orden_cliente"));
        oc.setEstado(EstadoOrdenCompraCliente.valueOf(rs.getString("estado")));
        oc.setNumero(rs.getString("numero"));

        Date fEmision = rs.getDate("fecha_emision");
        if (fEmision != null) {
            oc.setFechaEmision(fEmision.toLocalDate());
        }

        oc.setMoneda(Moneda.valueOf(rs.getString("moneda")));
        oc.setSubTotal(rs.getDouble("sub_total"));
        oc.setIgv(rs.getDouble("igv"));
        oc.setTotal(rs.getDouble("total"));
        oc.setObservaciones(rs.getString("observaciones"));

        Timestamp fRegistro = rs.getTimestamp("fecha_registro");
        if (fRegistro != null) {
            oc.setFechaRegistro(fRegistro.toLocalDateTime());
        }

        int idUser = rs.getInt("id_usuario_registro");
        if (!rs.wasNull()) {
            Usuario u = new Usuario();
            u.setId(idUser);
            oc.setUsuarioRegistro(u);
        }

        oc.setAnulado(rs.getBoolean("anulado"));
        oc.setMotivoAnulacion(rs.getString("motivo_anulacion"));

        Timestamp fAnulacion = rs.getTimestamp("fecha_anulacion");
        if (fAnulacion != null) {
            oc.setFechaAnulacion(fAnulacion.toLocalDateTime());
        }

        return oc;
    }

    @Override
    public OrdenCompraCliente load(Integer id) throws SQLException {
        String sql = "{CALL sp_orden_compra_cliente_obtener(?)}";
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
    public OrdenCompraCliente save(OrdenCompraCliente oc) throws SQLException {
        String sql = "{CALL sp_orden_compra_cliente_insertar(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, oc.getCliente().getId());
            if (oc.getCotizacion() != null && oc.getCotizacion().getId() > 0) {
                cs.setInt(2, oc.getCotizacion().getId());
            } else {
                cs.setNull(2, Types.INTEGER);
            }
            cs.setString(3, oc.getNumeroOrdenCliente());
            cs.setString(4, oc.getEstado().name());
            cs.setString(5, oc.getNumero());
            cs.setDate(6, Date.valueOf(oc.getFechaEmision()));
            cs.setString(7, oc.getMoneda().name());
            cs.setDouble(8, oc.getSubTotal());
            cs.setDouble(9, oc.getIgv());
            cs.setDouble(10, oc.getTotal());
            cs.setString(11, oc.getObservaciones());

            if (oc.getUsuarioRegistro() != null) {
                cs.setInt(12, oc.getUsuarioRegistro().getId());
            } else {
                cs.setNull(12, Types.INTEGER);
            }

            cs.registerOutParameter(13, Types.INTEGER);
            cs.execute();
            oc.setId(cs.getInt(13));
            return oc;
        }
    }

    @Override
    public OrdenCompraCliente update(OrdenCompraCliente oc) throws SQLException {
        String sql = "{CALL sp_orden_compra_cliente_modificar(?,?,?,?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, oc.getId());
            cs.setInt(2, oc.getCliente().getId());
            if (oc.getCotizacion() != null && oc.getCotizacion().getId() > 0) {
                cs.setInt(3, oc.getCotizacion().getId());
            } else {
                cs.setNull(3, Types.INTEGER);
            }
            cs.setString(4, oc.getNumeroOrdenCliente());
            cs.setString(5, oc.getEstado().name());
            cs.setString(6, oc.getNumero());
            cs.setDate(7, Date.valueOf(oc.getFechaEmision()));
            cs.setString(8, oc.getMoneda().name());
            cs.setDouble(9, oc.getSubTotal());
            cs.setDouble(10, oc.getIgv());
            cs.setDouble(11, oc.getTotal());
            cs.setString(12, oc.getObservaciones());
            cs.executeUpdate();
            return oc;
        }
    }

    @Override
    public void remove(OrdenCompraCliente oc) throws SQLException {
        String sql = "{CALL sp_orden_compra_cliente_eliminar(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, oc.getId());
            cs.setString(2, oc.getMotivoAnulacion() != null ? oc.getMotivoAnulacion() : "Anulacion desde el sistema");
            cs.executeUpdate();
        }
    }

    @Override
    public List<OrdenCompraCliente> listarTodos() throws SQLException {
        String sql = "{CALL sp_orden_compra_cliente_listar()}";
        List<OrdenCompraCliente> lista = new ArrayList<>();
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
    public void actualizarEstado(int id, EstadoOrdenCompraCliente estado) throws SQLException {
        String sql = "{CALL sp_orden_compra_cliente_actualizar_estado(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            cs.setString(2, estado.name());
            cs.executeUpdate();
        }
    }

    @Override
    public OrdenCompraCliente buscarPorCotizacion(int idCotizacion) throws SQLException {
        String sql = "{CALL sp_orden_compra_cliente_buscar_por_cotizacion(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idCotizacion);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }
}
