package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.VentaDAO;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.EstadoVenta;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VentaDAOImpl implements VentaDAO {

    private Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setId(rs.getInt("id"));

        Cliente cli = new Cliente();
        cli.setId(rs.getInt("id_cliente"));
        v.setCliente(cli);

        int idOc = rs.getInt("id_orden_compra_cliente");
        if (!rs.wasNull()) {
            OrdenCompraCliente oc = new OrdenCompraCliente();
            oc.setId(idOc);
            v.setOrdenCompraCliente(oc);
        }

        v.setCondicionPago(CondicionPago.valueOf(rs.getString("condicion_pago")));
        v.setPlazoCreditoDias(rs.getInt("plazo_credito_dias"));
        v.setEstado(EstadoVenta.valueOf(rs.getString("estado")));
        v.setNumero(rs.getString("numero"));

        Date fEmision = rs.getDate("fecha_emision");
        if (fEmision != null) {
            v.setFechaEmision(fEmision.toLocalDate());
        }

        v.setMoneda(Moneda.valueOf(rs.getString("moneda")));
        v.setSubTotal(rs.getDouble("sub_total"));
        v.setIgv(rs.getDouble("igv"));
        v.setTotal(rs.getDouble("total"));
        v.setObservaciones(rs.getString("observaciones"));

        Timestamp fRegistro = rs.getTimestamp("fecha_registro");
        if (fRegistro != null) {
            v.setFechaRegistro(fRegistro.toLocalDateTime());
        }

        int idUser = rs.getInt("id_usuario_registro");
        if (!rs.wasNull()) {
            Usuario u = new Usuario();
            u.setId(idUser);
            v.setUsuarioRegistro(u);
        }

        v.setAnulado(rs.getBoolean("anulado"));
        v.setMotivoAnulacion(rs.getString("motivo_anulacion"));

        Timestamp fAnulacion = rs.getTimestamp("fecha_anulacion");
        if (fAnulacion != null) {
            v.setFechaAnulacion(fAnulacion.toLocalDateTime());
        }

        return v;
    }

    @Override
    public Venta load(Integer id) throws SQLException {
        String sql = "{CALL sp_venta_obtener(?)}";
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
    public Venta save(Venta v) throws SQLException {
        String sql = "{CALL sp_venta_insertar(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, v.getCliente().getId());
            if (v.getOrdenCompraCliente() != null && v.getOrdenCompraCliente().getId() > 0) {
                cs.setInt(2, v.getOrdenCompraCliente().getId());
            } else {
                cs.setNull(2, Types.INTEGER);
            }
            cs.setString(3, v.getCondicionPago().name());
            cs.setInt(4, v.getPlazoCreditoDias());
            cs.setString(5, v.getEstado().name());
            cs.setString(6, v.getNumero());
            cs.setDate(7, Date.valueOf(v.getFechaEmision()));
            cs.setString(8, v.getMoneda().name());
            cs.setDouble(9, v.getSubTotal());
            cs.setDouble(10, v.getIgv());
            cs.setDouble(11, v.getTotal());
            cs.setString(12, v.getObservaciones());

            if (v.getUsuarioRegistro() != null) {
                cs.setInt(13, v.getUsuarioRegistro().getId());
            } else {
                cs.setNull(13, Types.INTEGER);
            }

            cs.registerOutParameter(14, Types.INTEGER);
            cs.execute();
            v.setId(cs.getInt(14));
            return v;
        }
    }

    @Override
    public Venta update(Venta v) throws SQLException {
        String sql = "{CALL sp_venta_modificar(?,?,?,?,?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, v.getId());
            cs.setInt(2, v.getCliente().getId());
            if (v.getOrdenCompraCliente() != null && v.getOrdenCompraCliente().getId() > 0) {
                cs.setInt(3, v.getOrdenCompraCliente().getId());
            } else {
                cs.setNull(3, Types.INTEGER);
            }
            cs.setString(4, v.getCondicionPago().name());
            cs.setInt(5, v.getPlazoCreditoDias());
            cs.setString(6, v.getEstado().name());
            cs.setString(7, v.getNumero());
            cs.setDate(8, Date.valueOf(v.getFechaEmision()));
            cs.setString(9, v.getMoneda().name());
            cs.setDouble(10, v.getSubTotal());
            cs.setDouble(11, v.getIgv());
            cs.setDouble(12, v.getTotal());
            cs.setString(13, v.getObservaciones());
            cs.executeUpdate();
            return v;
        }
    }

    @Override
    public void remove(Venta v) throws SQLException {
        String sql = "{CALL sp_venta_eliminar(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, v.getId());
            cs.setString(2, v.getMotivoAnulacion() != null ? v.getMotivoAnulacion() : "Anulacion desde el sistema");
            cs.executeUpdate();
        }
    }

    @Override
    public List<Venta> listarTodos() throws SQLException {
        String sql = "{CALL sp_venta_listar()}";
        List<Venta> lista = new ArrayList<>();
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
    public void bloquearVenta(int id) throws SQLException {
        String sql = "{CALL sp_venta_bloquear(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                // Bloqueo ejecutado
            }
        }
    }

    @Override
    public void actualizarEstado(int id, EstadoVenta estado) throws SQLException {
        String sql = "{CALL sp_venta_actualizar_estado(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            cs.setString(2, estado.name());
            cs.executeUpdate();
        }
    }

    @Override
    public double obtenerSaldoPendienteCliente(int idCliente) throws SQLException {
        String sql = "{CALL sp_venta_saldo_pendiente_cliente(?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idCliente);
            cs.registerOutParameter(2, Types.DECIMAL);
            cs.execute();
            return cs.getDouble(2);
        }
    }

    @Override
    public void generarCuentaPorCobrar(int idVenta, int idCliente, LocalDate fechaEmision, LocalDate fechaVencimiento, Moneda moneda, double montoOriginal) throws SQLException {
        String sql = "{CALL sp_venta_generar_cuenta_por_cobrar(?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idVenta);
            cs.setInt(2, idCliente);
            cs.setDate(3, Date.valueOf(fechaEmision));
            cs.setDate(4, Date.valueOf(fechaVencimiento));
            cs.setString(5, moneda.name());
            cs.setDouble(6, montoOriginal);
            cs.executeUpdate();
        }
    }
}
