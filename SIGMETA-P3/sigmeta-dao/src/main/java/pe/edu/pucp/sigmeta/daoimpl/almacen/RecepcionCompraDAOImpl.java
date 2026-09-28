
package pe.edu.pucp.sigmeta.daoimpl.almacen;

import pe.edu.pucp.sigmeta.dao.almacen.RecepcionCompraDAO;
import pe.edu.pucp.sigmeta.model.almacen.RecepcionCompra;
import pe.edu.pucp.sigmeta.model.compras.Compra;
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

public class RecepcionCompraDAOImpl implements RecepcionCompraDAO {

    @Override
    public RecepcionCompra save(RecepcionCompra recepcion) throws SQLException {
        String sql = "{CALL sp_recepcion_compra_insertar(?,?,?,?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, recepcion.getCompra().getId());
            cs.setDate(2, Date.valueOf(recepcion.getFechaRecepcion()));
            cs.setString(3, recepcion.getObservaciones());
            cs.setInt(4, recepcion.getUsuarioRegistro().getId());
            cs.registerOutParameter(5, Types.INTEGER);

            cs.execute();

            recepcion.setId(cs.getInt(5));
            return recepcion;
        }
    }

    @Override
    public RecepcionCompra load(Integer id) throws SQLException {
        String sql = "{CALL sp_recepcion_compra_obtener(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapearRecepcion(rs);
                }
            }
        }

        return null;
    }

    @Override
    public RecepcionCompra update(RecepcionCompra recepcion) throws SQLException {
        String sql = "{CALL sp_recepcion_compra_modificar(?,?,?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, recepcion.getId());
            cs.setDate(2, Date.valueOf(recepcion.getFechaRecepcion()));
            cs.setString(3, recepcion.getObservaciones());

            cs.execute();
            return recepcion;
        }
    }

    @Override
    public void remove(RecepcionCompra recepcion) throws SQLException {
        String sql = "{CALL sp_recepcion_compra_eliminar(?)}";

        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, recepcion.getId());
            cs.execute();
        }
    }

    @Override
    public List<RecepcionCompra> listAll() throws SQLException {
        String sql = "{CALL sp_recepcion_compra_listar()}";

        List<RecepcionCompra> recepciones = new ArrayList<>();
        Connection conn = transactionContext.getConnection();

        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {

            while (rs.next()) {
                recepciones.add(mapearRecepcion(rs));
            }
        }

        return recepciones;
    }

    private RecepcionCompra mapearRecepcion(ResultSet rs) throws SQLException {
        RecepcionCompra recepcion = new RecepcionCompra();

        recepcion.setId(rs.getInt("id"));

        Date fechaRecepcion = rs.getDate("fecha_recepcion");
        if (fechaRecepcion != null) {
            recepcion.setFechaRecepcion(fechaRecepcion.toLocalDate());
        }

        recepcion.setObservaciones(rs.getString("observaciones"));

        Timestamp fechaRegistro = rs.getTimestamp("fecha_registro");
        if (fechaRegistro != null) {
            recepcion.setFechaRegistro(fechaRegistro.toLocalDateTime());
        }

        Compra compra = new Compra();
        compra.setId(rs.getInt("id_compra"));
        recepcion.setCompra(compra);

        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("id_usuario_registro"));
        recepcion.setUsuarioRegistro(usuario);

        return recepcion;
    }
}
