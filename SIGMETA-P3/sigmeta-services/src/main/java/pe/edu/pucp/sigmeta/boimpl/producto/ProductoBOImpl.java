package pe.edu.pucp.sigmeta.boimpl.producto;

import pe.edu.pucp.sigmeta.bo.producto.ProductoBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.producto.CategoriaDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.daoimpl.producto.CategoriaDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.producto.ProductoDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Categoria;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoBOImpl implements ProductoBO {

    private final ProductoDAO productoDAO;
    private final CategoriaDAO categoriaDAO;
    private final UsuarioBO usuarioBO;

    public ProductoBOImpl() {
        this.productoDAO = new ProductoDAOImpl();
        this.categoriaDAO = new CategoriaDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF005
    @Override
    public Producto registrar(Producto producto, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validarDatos(producto);
        // RF012: el stock solo entra por recepciones de compra o ajustes de inventario
        producto.setStockActual(0);
        try {
            verificarCategoriaActiva(producto.getCategoria().getId());
            validarCodigoUnico(producto);
            productoDAO.save(producto);
            transactionContext.commit();
            producto.setEstado(true);
            return producto;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF005
    @Override
    public Producto modificar(Producto producto, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        validarDatos(producto);
        try {
            Producto actual = obtenerExistente(producto.getId());
            if (!actual.isEstado()) {
                throw new IllegalStateException("No se puede modificar un producto desactivado");
            }
            verificarCategoriaActiva(producto.getCategoria().getId());
            validarCodigoUnico(producto);
            // RF012: el stock no se edita a mano, lo mueven los movimientos de almacen
            producto.setStockActual(actual.getStockActual());
            productoDAO.update(producto);
            transactionContext.commit();
            producto.setEstado(actual.isEstado());
            return producto;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF005, RNF002: baja logica
    @Override
    public void desactivar(int idProducto, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        try {
            Producto producto = obtenerExistente(idProducto);
            productoDAO.remove(producto);
            transactionContext.commit();
            producto.setEstado(false);
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    // RF005
    @Override
    public Producto obtener(int idProducto) throws SQLException {
        try {
            return productoDAO.load(idProducto);
        } finally {
            transactionContext.close();
        }
    }

    // RF005: solo productos activos
    @Override
    public List<Producto> listarTodos() throws SQLException {
        try {
            return productoDAO.listarTodos();
        } finally {
            transactionContext.close();
        }
    }

    // RF005: el codigo leido puede ser el interno, el del fabricante o el del proveedor
    @Override
    public Producto buscarPorCodigo(String codigo) throws SQLException {
        String buscado = Validador.textoObligatorio(codigo, "codigo", 50);
        try {
            return productoDAO.listarTodos().stream()
                    .filter(p -> buscado.equalsIgnoreCase(p.getCodigoInterno())
                            || buscado.equalsIgnoreCase(p.getCodigoFabricante())
                            || buscado.equalsIgnoreCase(p.getCodigoProveedor()))
                    .findFirst().orElse(null);
        } finally {
            transactionContext.close();
        }
    }

    // RF005: coincidencia parcial entre los productos activos
    @Override
    public List<Producto> buscarPorNombre(String nombre) throws SQLException {
        String texto = Validador.textoObligatorio(nombre, "nombre", 150).toLowerCase();
        try {
            return productoDAO.listarTodos().stream()
                    .filter(p -> p.getNombre().toLowerCase().contains(texto))
                    .toList();
        } finally {
            transactionContext.close();
        }
    }

    // RF005
    @Override
    public List<Producto> listarPorCategoria(int idCategoria) throws SQLException {
        try {
            return productoDAO.listarTodos().stream()
                    .filter(p -> p.getCategoria().getId() == idCategoria)
                    .toList();
        } finally {
            transactionContext.close();
        }
    }

    // RF005
    @Override
    public List<Producto> listarPorEstado(boolean estado) throws SQLException {
        try {
            return productoDAO.listarPorEstado(estado);
        } finally {
            transactionContext.close();
        }
    }

    // RF012
    @Override
    public List<Producto> listarConStockBajo() throws SQLException {
        try {
            return productoDAO.listarTodos().stream()
                    .filter(p -> p.getStockActual() <= p.getStockMinimo())
                    .toList();
        } finally {
            transactionContext.close();
        }
    }

    // RF005: registrar, modificar (incluido el precio) y desactivar es exclusivo del Administrador
    private void verificarPermiso(int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.ADMINISTRADOR);
    }

    private Producto obtenerExistente(int idProducto) throws SQLException {
        Producto producto = productoDAO.load(idProducto);
        if (producto == null) {
            throw new IllegalArgumentException("No existe el producto con id " + idProducto);
        }
        return producto;
    }

    private void verificarCategoriaActiva(int idCategoria) throws SQLException {
        Categoria categoria = categoriaDAO.load(idCategoria);
        if (categoria == null || !categoria.isEstado()) {
            throw new IllegalArgumentException("La categoria " + idCategoria + " no existe o esta desactivada");
        }
    }

    private void validarDatos(Producto producto) {
        Validador.obligatorio(producto, "producto");
        Validador.obligatorio(producto.getCategoria(), "categoria");
        Validador.obligatorio(producto.getUnidadCompra(), "unidad de compra");
        Validador.obligatorio(producto.getUnidadVenta(), "unidad de venta");
        producto.setCodigoInterno(Validador.textoObligatorio(producto.getCodigoInterno(), "codigo interno", 50));
        producto.setCodigoFabricante(Validador.textoOpcional(producto.getCodigoFabricante(), "codigo de fabricante", 50));
        producto.setCodigoProveedor(Validador.textoOpcional(producto.getCodigoProveedor(), "codigo de proveedor", 50));
        producto.setNombre(Validador.textoObligatorio(producto.getNombre(), "nombre", 150));
        producto.setDescripcion(Validador.textoOpcional(producto.getDescripcion(), "descripcion", 255));
        producto.setImagen(Validador.textoOpcional(producto.getImagen(), "imagen", 255));

        if (!Double.isFinite(producto.getFactorConversion()) || producto.getFactorConversion() <= 0) {
            throw new IllegalArgumentException("El factor de conversion debe ser mayor a 0");
        }
        // si se compra y se vende en la misma unidad no hay conversion
        if (producto.getUnidadCompra() == producto.getUnidadVenta() && producto.getFactorConversion() != 1) {
            throw new IllegalArgumentException("Si la unidad de compra y de venta son iguales, el factor debe ser 1");
        }
        if (!Double.isFinite(producto.getPrecioVenta()) || producto.getPrecioVenta() <= 0) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor a 0");
        }
        if (!Double.isFinite(producto.getCostoUnitario()) || producto.getCostoUnitario() < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo");
        }
        if (!Double.isFinite(producto.getStockMinimo()) || producto.getStockMinimo() < 0) {
            throw new IllegalArgumentException("El stock minimo no puede ser negativo");
        }
        if (!Double.isFinite(producto.getPrecioReferencial()) || producto.getPrecioReferencial() < 0) {
            throw new IllegalArgumentException("El precio referencial no puede ser negativo");
        }
    }

    // codigo_interno es UNIQUE en la tabla, tambien frente a los productos desactivados
    private void validarCodigoUnico(Producto producto) throws SQLException {
        List<Producto> productos = new ArrayList<>(productoDAO.listarPorEstado(true));
        productos.addAll(productoDAO.listarPorEstado(false));
        for (Producto otro : productos) {
            if (otro.getCodigoInterno().equalsIgnoreCase(producto.getCodigoInterno())
                    && otro.getId() != producto.getId()) {
                throw new IllegalArgumentException("Ya existe un producto con el codigo " + producto.getCodigoInterno());
            }
        }
    }
}
