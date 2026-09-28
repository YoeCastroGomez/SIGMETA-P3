package pe.edu.pucp.sigmeta.boimpl.maestros;

import pe.edu.pucp.sigmeta.bo.maestros.ProveedorBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.maestros.ProveedorDAO;
import pe.edu.pucp.sigmeta.daoimpl.maestros.ProveedorDAOImpl;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;

import java.sql.SQLException;
import java.util.List;

public class ProveedorBOImpl implements ProveedorBO {

    private final ProveedorDAO proveedorDAO;
    private final UsuarioBO usuarioBO;

    public ProveedorBOImpl() {
        this.proveedorDAO = new ProveedorDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF004: la gestion de proveedores es exclusiva del Administrador
    @Override
    public Proveedor registrar(Proveedor proveedor, int idAdministrador) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idAdministrador);
        validarDatos(proveedor);
        validarRucUnico(proveedor);
        return proveedorDAO.save(proveedor);
    }

    // RF004
    @Override
    public Proveedor modificar(Proveedor proveedor, int idAdministrador) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idAdministrador);
        Validador.obligatorio(proveedor, "proveedor");
        obtenerExistente(proveedor.getId());
        validarDatos(proveedor);
        validarRucUnico(proveedor);
        return proveedorDAO.update(proveedor);
    }

    // baja logica: conserva su historial de compras y deja de aparecer en listarTodos
    // RF004, RNF002
    @Override
    public void desactivar(int idProveedor, int idAdministrador) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idAdministrador);
        proveedorDAO.remove(obtenerExistente(idProveedor));
    }

    // RF004
    @Override
    public Proveedor obtener(int idProveedor) throws SQLException {
        return proveedorDAO.load(idProveedor);
    }

    // solo activos: es la lista que se usa para elegir el proveedor de una nueva orden de compra
    // RF004
    @Override
    public List<Proveedor> listarTodos() throws SQLException {
        return proveedorDAO.listarTodos();
    }

    // RF004
    @Override
    public Proveedor buscarPorRuc(String ruc) throws SQLException {
        return proveedorDAO.buscarPorRuc(Validador.ruc(ruc));
    }

    // RF004
    @Override
    public List<Proveedor> buscarPorRazonSocial(String razonSocial) throws SQLException {
        return proveedorDAO.buscarPorRazonSocial(
                Validador.textoObligatorio(razonSocial, "razon social", 150));
    }

    // RF004
    @Override
    public List<Proveedor> listarPorEstado(boolean estado) throws SQLException {
        return proveedorDAO.listarPorEstado(estado);
    }

    private Proveedor obtenerExistente(int idProveedor) throws SQLException {
        Proveedor proveedor = proveedorDAO.load(idProveedor);
        if(proveedor == null){
            throw new IllegalArgumentException("No existe el proveedor con id " + idProveedor);
        }
        return proveedor;
    }

    private void validarDatos(Proveedor proveedor) {
        Validador.obligatorio(proveedor, "proveedor");
        proveedor.setRuc(Validador.ruc(proveedor.getRuc()));
        proveedor.setRazonSocial(Validador.textoObligatorio(proveedor.getRazonSocial(), "razon social", 150));
        proveedor.setDireccion(Validador.textoOpcional(proveedor.getDireccion(), "direccion", 200));
        proveedor.setTelefono(Validador.telefono(proveedor.getTelefono()));
        proveedor.setCorreo(Validador.correo(proveedor.getCorreo()));
        proveedor.setRubro(Validador.textoOpcional(proveedor.getRubro(), "rubro", 100));
        proveedor.setContactoNombre(Validador.textoOpcional(proveedor.getContactoNombre(), "contacto", 100));
        Validador.obligatorio(proveedor.getCondicionPago(), "condicion de pago");
        if(proveedor.getPlazoEntregaDias() < 0){
            throw new IllegalArgumentException("El plazo de entrega no puede ser negativo");
        }
    }

    private void validarRucUnico(Proveedor proveedor) throws SQLException {
        Proveedor existente = proveedorDAO.buscarPorRuc(proveedor.getRuc());
        if(existente != null && existente.getId() != proveedor.getId()){
            throw new IllegalArgumentException("Ya existe un proveedor con el RUC " + proveedor.getRuc());
        }
    }
}
