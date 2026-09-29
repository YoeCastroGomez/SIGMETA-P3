package pe.edu.pucp.sigmeta.boimpl.producto;

import pe.edu.pucp.sigmeta.bo.producto.CategoriaBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.producto.CategoriaDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.daoimpl.producto.CategoriaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriaBOImpl implements CategoriaBO {

    private final CategoriaDAO categoriaDAO;
    private final ProductoDAO productoDAO;
    private final UsuarioBO usuarioBO;

    public CategoriaBOImpl() {
        this.categoriaDAO = new CategoriaDAOImpl();
        this.productoDAO = new ProductoDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF006
    @Override
    public Categoria registrar(Categoria categoria, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validarDatos(categoria);
        try {
            validarNombreUnico(categoria);
            categoriaDAO.save(categoria);
            transactionContext.commit();
            categoria.setEstado(true);
            return categoria;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF006
    @Override
    public Categoria modificar(Categoria categoria, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validarDatos(categoria);
        try {
            Categoria actual = obtenerExistente(categoria.getId());
            if (!actual.isEstado()) {
                throw new IllegalStateException("No se puede modificar una categoria desactivada");
            }
            validarNombreUnico(categoria);
            categoriaDAO.update(categoria);
            transactionContext.commit();
            categoria.setEstado(actual.isEstado());
            return categoria;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF006, RNF002: baja logica
    @Override
    public void desactivar(int idCategoria, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        try {
            Categoria categoria = obtenerExistente(idCategoria);
            // RF006: no puede desactivarse una categoria con productos activos asociados
            boolean tieneProductosActivos = productoDAO.listarTodos().stream()
                    .anyMatch(p -> p.getCategoria().getId() == idCategoria);
            if (tieneProductosActivos) {
                throw new IllegalStateException("La categoria tiene productos activos; reasignelos o desactivelos primero");
            }
            categoriaDAO.remove(categoria);
            transactionContext.commit();
            categoria.setEstado(false);
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF006
    @Override
    public Categoria obtener(int idCategoria) throws SQLException {
        try {
            return categoriaDAO.load(idCategoria);
        } finally {
            transactionContext.close();
        }
    }

    // RF006: solo categorias activas
    @Override
    public List<Categoria> listarTodos() throws SQLException {
        try {
            return categoriaDAO.listarTodos();
        } finally {
            transactionContext.close();
        }
    }

    // RF006: busqueda por coincidencia parcial entre las categorias activas y desactivadas
    @Override
    public List<Categoria> buscarPorNombre(String nombre) throws SQLException {
        String texto = Validador.textoObligatorio(nombre, "nombre", 100).toLowerCase();
        try {
            List<Categoria> categorias = new ArrayList<>(categoriaDAO.listarPorEstado(true));
            categorias.addAll(categoriaDAO.listarPorEstado(false));
            return categorias.stream()
                    .filter(c -> c.getNombre().toLowerCase().contains(texto))
                    .toList();
        } finally {
            transactionContext.close();
        }
    }

    // RF006
    @Override
    public List<Categoria> listarPorEstado(boolean estado) throws SQLException {
        try {
            return categoriaDAO.listarPorEstado(estado);
        } finally {
            transactionContext.close();
        }
    }

    // RF006: las categorias las mantiene el Administrador
    private void verificarPermiso(int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.ADMINISTRADOR);
    }

    private Categoria obtenerExistente(int idCategoria) throws SQLException {
        Categoria categoria = categoriaDAO.load(idCategoria);
        if (categoria == null) {
            throw new IllegalArgumentException("No existe la categoria con id " + idCategoria);
        }
        return categoria;
    }

    private void validarDatos(Categoria categoria) {
        Validador.obligatorio(categoria, "categoria");
        categoria.setNombre(Validador.textoObligatorio(categoria.getNombre(), "nombre", 100));
        categoria.setDescripcion(Validador.textoOpcional(categoria.getDescripcion(), "descripcion", 255));
    }

    // la tabla no tiene UNIQUE en nombre; se controla entre las categorias activas
    private void validarNombreUnico(Categoria categoria) throws SQLException {
        for (Categoria otra : categoriaDAO.listarTodos()) {
            if (otra.getNombre().equalsIgnoreCase(categoria.getNombre()) && otra.getId() != categoria.getId()) {
                throw new IllegalArgumentException("Ya existe la categoria " + categoria.getNombre());
            }
        }
    }
}
