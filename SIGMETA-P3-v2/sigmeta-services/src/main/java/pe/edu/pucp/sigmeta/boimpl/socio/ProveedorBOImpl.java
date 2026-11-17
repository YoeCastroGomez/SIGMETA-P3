package pe.edu.pucp.sigmeta.boimpl.socio;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.socio.ProveedorBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.producto.TextoNormalizado;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.socio.ProveedorDAO;
import pe.edu.pucp.sigmeta.daoimpl.socio.ProveedorDAOImpl;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
                    Validador.textoObligatorio(razonSocial, "razón social", 150));
        });
    }

    // RF004
    @Override
    public List<Proveedor> listarPorEstado(boolean estado) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return proveedorDAO.listarPorEstado(estado);
        });
    }

    @Override
    public List<Proveedor> buscar(String ruc, String razonSocial, Boolean estado, int idAdministrador)
            throws SQLException {
        String numero = ruc == null || ruc.isBlank() ? null : Validador.ruc(ruc.trim());
        String texto = razonSocial == null || razonSocial.isBlank() ? null
                : Validador.textoObligatorio(razonSocial.trim(), "razón social", 150);

        return Transaccion.consultar(() -> {
            usuarioBO.obtenerAdministradorActivo(idAdministrador);
            List<Proveedor> resultado;
            if (numero != null) {
                Proveedor encontrado = proveedorDAO.buscarPorRuc(numero);
                resultado = new ArrayList<>();
                if (encontrado != null) {
                    resultado.add(encontrado);
                }
            } else if (texto != null) {
                resultado = proveedorDAO.buscarPorRazonSocial(texto);
            } else if (estado != null) {
                return proveedorDAO.listarPorEstado(estado);
            } else {
                resultado = new ArrayList<>(proveedorDAO.listarPorEstado(true));
                resultado.addAll(proveedorDAO.listarPorEstado(false));
            }

            if (numero != null && texto != null) {
                String buscado = texto.toLowerCase(Locale.ROOT);
                resultado.removeIf(p -> p.getRazonSocial() == null
                        || !p.getRazonSocial().toLowerCase(Locale.ROOT).contains(buscado));
            }
            if (estado != null) {
                resultado.removeIf(p -> p.isEstado() != estado);
            }
            return resultado;
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
        proveedor.setRazonSocial(TextoNormalizado.nombre(
                Validador.textoObligatorio(proveedor.getRazonSocial(), "razón social", 150)));
        Validador.obligatorio(proveedor.getContacto(), "datos de contacto");
        proveedor.getContacto().setDireccion(TextoNormalizado.opcional(
                Validador.textoOpcional(proveedor.getContacto().getDireccion(), "dirección", 200)));
        proveedor.getContacto().setTelefono(Validador.telefono(proveedor.getContacto().getTelefono()));
        proveedor.getContacto().setCorreo(Validador.correo(proveedor.getContacto().getCorreo()));
        proveedor.setRubro(TextoNormalizado.opcional(
                Validador.textoOpcional(proveedor.getRubro(), "rubro", 100)));
        proveedor.getContacto().setNombreContacto(TextoNormalizado.opcional(
                Validador.textoOpcional(proveedor.getContacto().getNombreContacto(), "contacto", 100)));
        Validador.obligatorio(proveedor.getCondicionPago(), "condición de pago");
        if(proveedor.getPlazoEntregaDias() < 0){
            throw new IllegalArgumentException("El plazo de entrega no puede ser negativo");
        }
    }

    // la razon social es UNIQUE en la tabla; aqui se da un mensaje claro antes de llegar a la BD
    private void validarRazonSocialUnica(Proveedor proveedor) throws SQLException {
        // se compara por clave: "Ferretería Perú" y "FERRETERIA PERU" son el mismo proveedor
        String nueva = TextoNormalizado.clave(proveedor.getRazonSocial());
        for (Proveedor otro : proveedorDAO.buscarPorRazonSocial(proveedor.getRazonSocial())) {
            if (otro.getId() != proveedor.getId() && nueva.equals(TextoNormalizado.clave(otro.getRazonSocial()))) {
                throw new IllegalArgumentException("Ya existe un proveedor con la razón social " + proveedor.getRazonSocial());
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
