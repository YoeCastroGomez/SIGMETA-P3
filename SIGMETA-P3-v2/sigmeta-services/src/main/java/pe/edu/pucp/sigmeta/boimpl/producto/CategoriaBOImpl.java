package pe.edu.pucp.sigmeta.boimpl.producto;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.producto.CategoriaBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.producto.CategoriaDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.daoimpl.producto.CategoriaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Categoria;

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
        return Transaccion.ejecutar(() -> {
            validarNombreUnico(categoria);
            categoriaDAO.save(categoria);
            categoria.setEstado(true);
            return categoria;
        });
    }

    // RF006
    @Override
    public Categoria modificar(Categoria categoria, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validarDatos(categoria);
        return Transaccion.ejecutar(() -> {
            // RF006: una categoria desactivada tambien se puede corregir (nombre y descripcion);
            // su estado no cambia aqui, solo con desactivar / activar
            Categoria actual = obtenerExistente(categoria.getId());
            validarNombreUnico(categoria);
            categoriaDAO.update(categoria);
            categoria.setEstado(actual.isEstado());
            return categoria;
        });
    }

    // RF006, RNF002: baja logica
    @Override
    public void desactivar(int idCategoria, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        Transaccion.ejecutar(() -> {
            Categoria categoria = obtenerExistente(idCategoria);
            if (!categoria.isEstado()) {
                throw new IllegalStateException("La categoría " + categoria.getNombre() + " ya está desactivada");
            }
            // RF006: no puede desactivarse una categoria con productos activos asociados
            boolean tieneProductosActivos = productoDAO.listarTodos().stream()
                    .anyMatch(p -> p.getCategoria().getId() == idCategoria);
            if (tieneProductosActivos) {
                throw new IllegalStateException("La categoría tiene productos activos; reasígnelos o desactívelos primero");
            }
            categoriaDAO.remove(categoria);
            categoria.setEstado(false);
            return null;
        });
    }

    // RF006: vuelve a activar una categoria desactivada (baja logica reversible). Solo el Administrador.
    @Override
    public Categoria activar(int idCategoria, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        return Transaccion.ejecutar(() -> {
            Categoria categoria = obtenerExistente(idCategoria);
            if (categoria.isEstado()) {
                throw new IllegalStateException("La categoría " + categoria.getNombre() + " ya está activa");
            }
            // el nombre pudo haberse registrado de nuevo mientras estaba desactivada
            validarNombreUnico(categoria);
            new pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl().ejecutar("sp_categoria_activar", idCategoria);
            categoria.setEstado(true);
            return categoria;
        });
    }

    // RF006
    @Override
    public Categoria obtener(int idCategoria) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return categoriaDAO.load(idCategoria);
        });
    }

    // RF006: solo categorias activas
    @Override
    public List<Categoria> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return categoriaDAO.listarTodos();
        });
    }

    // RF006: busqueda por coincidencia parcial entre las categorias activas y desactivadas.
    // Se compara con la clave del nombre: "construcción", "CONSTRUCCION" y "Construccion" encuentran lo mismo.
    @Override
    public List<Categoria> buscarPorNombre(String nombre) throws SQLException {
        String texto = TextoNormalizado.clave(Validador.textoObligatorio(nombre, "nombre", 100));
        return Transaccion.ejecutar(() -> {
            List<Categoria> categorias = new ArrayList<>(categoriaDAO.listarPorEstado(true));
            categorias.addAll(categoriaDAO.listarPorEstado(false));
            return categorias.stream()
                    .filter(c -> TextoNormalizado.clave(c.getNombre()).contains(texto))
                    .toList();
        });
    }

    // RF006
    @Override
    public List<Categoria> listarPorEstado(boolean estado) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return categoriaDAO.listarPorEstado(estado);
        });
    }

    // RF006: busqueda de la pantalla Categorias con los filtros combinados (nombre + estado).
    // Cada filtro es opcional: null no filtra. Ej.: "herra" + inactivas.
    @Override
    public List<Categoria> buscar(String nombre, Boolean estado) throws SQLException {
        String texto = nombre == null || nombre.isBlank() ? null
                : TextoNormalizado.clave(Validador.textoOpcional(nombre, "nombre", 100));
        return Transaccion.ejecutar(() -> {
            List<Categoria> categorias = new ArrayList<>();
            if (estado == null || estado) {
                categorias.addAll(categoriaDAO.listarPorEstado(true));
            }
            if (estado == null || !estado) {
                categorias.addAll(categoriaDAO.listarPorEstado(false));
            }
            return categorias.stream()
                    .filter(c -> texto == null || TextoNormalizado.clave(c.getNombre()).contains(texto))
                    .toList();
        });
    }

    // RF006: las categorias las mantiene el Administrador
    private void verificarPermiso(int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.ADMINISTRADOR);
    }

    private Categoria obtenerExistente(int idCategoria) throws SQLException {
        Categoria categoria = categoriaDAO.load(idCategoria);
        if (categoria == null) {
            throw new IllegalArgumentException("No existe la categoría con id " + idCategoria);
        }
        return categoria;
    }

    static String normalizarNombre(String nombre) {
        return TextoNormalizado.nombre(nombre);
    }

    private void validarDatos(Categoria categoria) {
        Validador.obligatorio(categoria, "categoría");
        categoria.setNombre(normalizarNombre(Validador.textoObligatorio(categoria.getNombre(), "nombre", 100)));
        categoria.setDescripcion(TextoNormalizado.opcional(
                Validador.textoOpcional(categoria.getDescripcion(), "descripción", 255)));
    }

    // la tabla no tiene UNIQUE en nombre; se controla entre las categorias activas y las desactivadas.
    // Se comparan las claves de los nombres (también los que ya estaban en la base escritos de otra forma):
    // "Construccion", "CONSTRUCCIÓN" y " construcción " son la misma categoria
    private void validarNombreUnico(Categoria categoria) throws SQLException {
        List<Categoria> todas = new ArrayList<>(categoriaDAO.listarPorEstado(true));
        todas.addAll(categoriaDAO.listarPorEstado(false));
        String nuevo = TextoNormalizado.clave(categoria.getNombre());
        for (Categoria otra : todas) {
            if (TextoNormalizado.clave(otra.getNombre()).equals(nuevo) && otra.getId() != categoria.getId()) {
                throw new IllegalArgumentException("Ya existe la categoría " + categoria.getNombre());
            }
        }
    }
}
