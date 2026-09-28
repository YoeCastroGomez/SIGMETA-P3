
package pe.edu.pucp.sigmeta.daoimpl.compras;

import pe.edu.pucp.sigmeta.dao.compras.CompraDAO;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CompraDAOImpl implements CompraDAO {

    @Override
    public Compra save(Compra compra) throws SQLException {
        String sql = "{CALL sp_compra_insertar(?,?,?,?,?,?,?,?,?,?,?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, compra.getProveedor().getId());
            cs.setString(2, compra.getNumero());
            cs.setDate(3, Date.valueOf(compra.getFechaEmision()));
            cs.setString(4, compra.getMoneda().name());
            cs.setDouble(5, compra.getSubTotal());
            cs.setDouble(6, compra.getIgv());
            cs.setDouble(7, compra.getTotal());
            cs.setString(8, compra.getObservaciones());
            cs.setInt(9, compra.getUsuarioRegistro().getId());
            cs.setString(10, compra.getEstado().name());

            if (compra.getFechaRecepcionEstimada() != null) {
                cs.setDate(11, Date.valueOf(compra.getFechaRecepcionEstimada()));
            } else {
                cs.setNull(11, Types.DATE);
            }

            cs.registerOutParameter(12, Types.INTEGER);
            cs.execute();

            compra.setId(cs.getInt(12));
            return compra;
        }
    }

    @Override
    public Compra load(Integer id) throws SQLException {
        String sql = "{CALL sp_compra_obtener(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapearCompra(rs);
                }
            }
        }

        return null;
    }

    @Override
    public Compra update(Compra compra) throws SQLException {
        String sql = "{CALL sp_compra_modificar(?,?,?,?,?,?,?,?,?,?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, compra.getId());
            cs.setInt(2, compra.getProveedor().getId());
            cs.setString(3, compra.getNumero());
            cs.setDate(4, Date.valueOf(compra.getFechaEmision()));
            cs.setString(5, compra.getMoneda().name());
            cs.setDouble(6, compra.getSubTotal());
            cs.setDouble(7, compra.getIgv());
            cs.setDouble(8, compra.getTotal());
            cs.setString(9, compra.getObservaciones());
            cs.setString(10, compra.getEstado().name());

            if (compra.getFechaRecepcionEstimada() != null) {
                cs.setDate(11, Date.valueOf(compra.getFechaRecepcionEstimada()));
            } else {
                cs.setNull(11, Types.DATE);
            }

            cs.executeUpdate();
            return compra;
        }
    }

    @Override
    public void remove(Compra compra) throws SQLException {
        String sql = "{CALL sp_compra_eliminar(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, compra.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public List<Compra> listAll() throws SQLException {
        String sql = "{CALL sp_compra_listar()}";

        List<Compra> compras = new ArrayList<>();
        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {

            while (rs.next()) {
                compras.add(mapearCompra(rs));
            }
        }

        return compras;
    }

    private Compra mapearCompra(ResultSet rs) throws SQLException {
        Compra compra = new Compra();

        compra.setId(rs.getInt("id"));
        compra.setNumero(rs.getString("numero"));

        Date fechaEmision = rs.getDate("fecha_emision");
        if (fechaEmision != null) {
            compra.setFechaEmision(fechaEmision.toLocalDate());
        }

        compra.setMoneda(Moneda.valueOf(rs.getString("moneda")));
        compra.setSubTotal(rs.getDouble("sub_total"));
        compra.setIgv(rs.getDouble("igv"));
        compra.setTotal(rs.getDouble("total"));
        compra.setObservaciones(rs.getString("observaciones"));

        Timestamp fechaRegistro = rs.getTimestamp("fecha_registro");
        if (fechaRegistro != null) {
            compra.setFechaRegistro(fechaRegistro.toLocalDateTime());
        }

        compra.setAnulado(rs.getBoolean("anulado"));
        compra.setMotivoAnulacion(rs.getString("motivo_anulacion"));

        Timestamp fechaAnulacion = rs.getTimestamp("fecha_anulacion");
        if (fechaAnulacion != null) {
            compra.setFechaAnulacion(fechaAnulacion.toLocalDateTime());
        }

        compra.setEstado(EstadoCompra.valueOf(rs.getString("estado")));

        Date fechaEstimada = rs.getDate("fecha_recepcion_estimada");
        if (fechaEstimada != null) {
            compra.setFechaRecepcionEstimada(fechaEstimada.toLocalDate());
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setId(rs.getInt("id_proveedor"));
        compra.setProveedor(proveedor);

        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id_usuario_registro"));
        compra.setUsuarioRegistro(usuario);

        return compra;
    }
}
