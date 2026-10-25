package pe.edu.pucp.sigmeta.boimpl.socio;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.socio.ProveedorBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.socio.ProveedorDAO;
import pe.edu.pucp.sigmeta.daoimpl.socio.ProveedorDAOImpl;
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
        return Transaccion.ejecutar(() -> {
            usuarioBO.obtenerAdministradorActivo(idAdministrador);
            validarDatos(proveedor);
            validarRucUnico(proveedor);
            validarRazonSocialUnica(proveedor);
            return proveedorDAO.save(proveedor);
        });
    }

    // RF004
    @Override
    public Proveedor modificar(Proveedor proveedor, int idAdministrador) throws SQLException {
        return Transaccion.ejecutar(() -> {
            usuarioBO.obtenerAdministradorActivo(idAdministrador);
            Validador.obligatorio(proveedor, "proveedor");
            obtenerExistente(proveedor.getId());
            validarDatos(proveedor);
            validarRucUnico(proveedor);
            validarRazonSocialUnica(proveedor);
            return proveedorDAO.update(proveedor);
        });
    }

    // baja logica: conserva su historial de compras y deja de aparecer en listarTodos
    // RF004, RNF002
    @Override
    public void desactivar(int idProveedor, int idAdministrador) throws SQLException {
        Transaccion.ejecutar(() -> {
            usuarioBO.obtenerAdministradorActivo(idAdministrador);
            proveedorDAO.remove(obtenerExistente(idProveedor));
            return null;
        });
    }

    // RF004
    @Override
    public Proveedor obtener(int idProveedor) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return proveedorDAO.load(idProveedor);
        });
    }

    // solo activos: es la lista que se usa para elegir el proveedor de una nueva orden de compra
    // RF004
    @Override
    public List<Proveedor> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return proveedorDAO.listarTodos();
        });
    }

    // RF004
    @Override
    public Proveedor buscarPorRuc(String ruc) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return proveedorDAO.buscarPorRuc(Validador.ruc(ruc));
        });
    }

    // RF004
    @Override
    public List<Proveedor> buscarPorRazonSocial(String razonSocial) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return proveedorDAO.buscarPorRazonSocial(
                    Validador.textoObligatorio(razonSocial, "razon social", 150));
        });
    }

    // RF004
    @Override
    public List<Proveedor> listarPorEstado(boolean estado) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return proveedorDAO.listarPorEstado(estado);
        });
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
        Validador.obligatorio(proveedor.getContacto(), "datos de contacto");
        proveedor.getContacto().setDireccion(Validador.textoOpcional(proveedor.getContacto().getDireccion(), "direccion", 200));
        proveedor.getContacto().setTelefono(Validador.telefono(proveedor.getContacto().getTelefono()));
        proveedor.getContacto().setCorreo(Validador.correo(proveedor.getContacto().getCorreo()));
        proveedor.setRubro(Validador.textoOpcional(proveedor.getRubro(), "rubro", 100));
        proveedor.getContacto().setNombreContacto(Validador.textoOpcional(proveedor.getContacto().getNombreContacto(), "contacto", 100));
        Validador.obligatorio(proveedor.getCondicionPago(), "condicion de pago");
        if(proveedor.getPlazoEntregaDias() < 0){
            throw new IllegalArgumentException("El plazo de entrega no puede ser negativo");
        }
    }

    // la razon social es UNIQUE en la tabla; aqui se da un mensaje claro antes de llegar a la BD
    private void validarRazonSocialUnica(Proveedor proveedor) throws SQLException {
        for (Proveedor otro : proveedorDAO.buscarPorRazonSocial(proveedor.getRazonSocial())) {
            if (otro.getId() != proveedor.getId() && proveedor.getRazonSocial().equalsIgnoreCase(otro.getRazonSocial())) {
                throw new IllegalArgumentException("Ya existe un proveedor con la razon social " + proveedor.getRazonSocial());
            }
        }
    }

    private void validarRucUnico(Proveedor proveedor) throws SQLException {
        Proveedor existente = proveedorDAO.buscarPorRuc(proveedor.getRuc());
        if(existente != null && existente.getId() != proveedor.getId()){
            throw new IllegalArgumentException("Ya existe un proveedor con el RUC " + proveedor.getRuc());
        }
    }
}
