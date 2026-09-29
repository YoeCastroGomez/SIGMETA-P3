package pe.edu.pucp.sigmeta.daoimpl.producto;

import pe.edu.pucp.sigmeta.dao.producto.CategoriaDAO;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAOImpl implements CategoriaDAO {
    @Override
    public Categoria load(Integer id) throws SQLException {
        String sql = "{CALL sp_categoria_obtener(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) return mapearCategoria(rs);
            }
        }
        return null;
    }


    private Categoria mapearCategoria(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setDescripcion(rs.getString("descripcion"));
        return c;
    }


    @Override
    public Categoria save(Categoria categoria) throws SQLException {
        String sql = "{CALL sp_categoria_insertar(?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.setString(2, categoria.getNombre());
            cs.setString(3, categoria.getDescripcion());
            cs.execute();
            categoria.setId(cs.getInt(1));
            return categoria;
        }

    }

    @Override
    public Categoria update(Categoria categoria) throws SQLException {
        String sql = "{CALL sp_categoria_modificar(?,?,?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, categoria.getId());
            cs.setString(2, categoria.getNombre());
            cs.setString(3, categoria.getDescripcion());
            cs.executeUpdate();
            return categoria;
        }


    }

    @Override
    public void remove(Categoria categoria) throws SQLException {
        String sql = "{CALL sp_categoria_eliminar(?)}";
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1,categoria.getId());
            cs.execute();
        }
    }


    @Override
    public List<Categoria> listarTodos() throws SQLException {
        String sql = "{CALL sp_categoria_listar()}";
        List<Categoria> categorias = new ArrayList<>();
        Connection conn = transactionContext.getConnection();
        try (CallableStatement cs = conn.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) categorias.add(mapearCategoria(rs));
        }
        return categorias;
    }
}
