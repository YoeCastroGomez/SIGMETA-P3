package pe.edu.pucp.sigmeta.daoimpl.ventas;

import pe.edu.pucp.sigmeta.dao.ventas.CotizacionDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoCotizacion;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Cotizacion;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CotizacionDAOImpl implements CotizacionDAO {


    private Cotizacion mapearCotizacion(ResultSet rs) throws SQLException {
        Cotizacion c = new Cotizacion();
        c.setId(rs.getInt("id"));
        c.setNumero(rs.getString("numero"));
        if(rs.getDate("fecha_emision") != null) c.setFechaEmision(rs.getDate("fecha_emision").toLocalDate());
        c.setMoneda(Moneda.valueOf(rs.getString("moneda")));
        c.setSubTotal(rs.getDouble("sub_total"));
        c.setIgv(rs.getDouble("igv"));
        c.setTotal(rs.getDouble("total"));
        c.setObservaciones(rs.getString("observaciones"));

        Timestamp fRegistro = rs.getTimestamp("fecha_registro");
        if(fRegistro != null) c.setFechaRegistro(fRegistro.toLocalDateTime());

        c.setAnulado(rs.getBoolean("anulado"));
        c.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        Timestamp fAnulacion = rs.getTimestamp("fecha_anulacion");
        if(fAnulacion != null) c.setFechaAnulacion(fAnulacion.toLocalDateTime());

        if(rs.getDate("fecha_vigencia") != null) c.setFechaVigencia(rs.getDate("fecha_vigencia").toLocalDate());
        c.setEstado(EstadoCotizacion.valueOf(rs.getString("estado")));

        Cliente cli = new Cliente();
        cli.setId(rs.getInt("id_cliente"));
        c.setCliente(cli);

        int idUser = rs.getInt("id_usuario_registro");
        if(!rs.wasNull()){
            Usuario u = new Usuario();
            u.setId(idUser);
            c.setUsuarioRegistro(u);
        }

        return c;
    }

    @Override
    public Cotizacion load(Integer id) throws SQLException {
        String sql = "{CALL sp_cotizacion_obtener(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) return mapearCotizacion(rs);
            }
        }
        return null;
    }


    @Override
    public List<Cotizacion> listarTodos() throws SQLException {
        String sql = "{CALL sp_cotizacion_listar()}";
        List<Cotizacion> cotizaciones = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) cotizaciones.add(mapearCotizacion(rs));
        }
        return cotizaciones;
    }


    @Override
    public Cotizacion save(Cotizacion cot) throws SQLException {
        String sql = "{CALL sp_cotizacion_insertar(?,?,?,?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, cot.getNumero());
            cs.setDate(3, Date.valueOf(cot.getFechaEmision()));
            cs.setString(4, cot.getMoneda().name());
            cs.setDouble(5, cot.getSubTotal());
            cs.setDouble(6, cot.getIgv());
            cs.setDouble(7, cot.getTotal());
            cs.setString(8, cot.getObservaciones());

            if (cot.getUsuarioRegistro() != null) cs.setInt(9, cot.getUsuarioRegistro().getId());
            else cs.setNull(9, Types.INTEGER);

            cs.setInt(10, cot.getCliente().getId());

            if (cot.getFechaVigencia() != null) cs.setDate(11, Date.valueOf(cot.getFechaVigencia()));
            else cs.setNull(11, Types.DATE);

            cs.setString(12, cot.getEstado().name());

            cs.execute();
            cot.setId(cs.getInt(1));
            return cot;
        }
    }

    @Override
    public Cotizacion update(Cotizacion cot) throws SQLException {
        String sql = "{CALL sp_cotizacion_modificar(?,?,?,?,?,?,?,?,?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, cot.getId());
            cs.setString(2, cot.getNumero());
            cs.setDate(3, Date.valueOf(cot.getFechaEmision()));
            cs.setString(4, cot.getMoneda().name());
            cs.setDouble(5, cot.getSubTotal());
            cs.setDouble(6, cot.getIgv());
            cs.setDouble(7, cot.getTotal());
            cs.setString(8, cot.getObservaciones());
            cs.setInt(9, cot.getCliente().getId());

            if (cot.getFechaVigencia() != null) cs.setDate(10, Date.valueOf(cot.getFechaVigencia()));
            else cs.setNull(10, Types.DATE);

            cs.setString(11, cot.getEstado().name());
            cs.executeUpdate();
            return cot;
        }
    }

    @Override
    public void remove(Cotizacion cotizacion) throws SQLException {
        // En tu SP, eliminar usa el id y el motivo. Asumimos motivo genérico si usamos la firma del BaseDAO.
        String sql = "{CALL sp_cotizacion_eliminar(?, ?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, cotizacion.getId());
            cs.setString(2, cotizacion.getMotivoAnulacion() != null
                    ? cotizacion.getMotivoAnulacion() : "Anulado por sistema");
            cs.executeUpdate();
        }
    }
}
