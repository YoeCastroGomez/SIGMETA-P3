package pe.edu.pucp.sigmeta.boimpl.socio;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.socio.ClienteBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.socio.ClienteDAO;
import pe.edu.pucp.sigmeta.daoimpl.socio.ClienteDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.socio.Cliente;

import java.sql.SQLException;
import java.util.List;


public class ClienteBOImpl implements ClienteBO {

    private final ClienteDAO clienteDAO;
    private final UsuarioBO usuarioBO;

    public ClienteBOImpl() {
        this.clienteDAO = new ClienteDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
    }

    // RF003
    @Override
    public Cliente registrar(Cliente cliente, int idResponsable) throws SQLException {
        return Transaccion.ejecutar(() -> {
            verificarPermiso(idResponsable);
            validarDatos(cliente);
            validarDocumentoUnico(cliente);
            validarRazonSocialUnica(cliente);
            // todo cliente nuevo empieza al contado; la linea de credito la asigna el Administrador
            cliente.setCondicionPago(CondicionPago.CONTADO);
            cliente.setPlazoCreditoDias(0);
            cliente.setLimiteCredito(java.math.BigDecimal.ZERO);
            cliente.setCalificacionCrediticia(null);
            return clienteDAO.save(cliente);
        });
    }

    // RF003
    @Override
    public Cliente modificar(Cliente cliente, int idResponsable) throws SQLException {
        return Transaccion.ejecutar(() -> {
            verificarPermiso(idResponsable);
            Validador.obligatorio(cliente, "cliente");
            Cliente actual = obtenerExistente(cliente.getId());
            validarDatos(cliente);
            validarDocumentoUnico(cliente);
            validarRazonSocialUnica(cliente);
            // los datos de credito no se cambian aqui, solo con asignarLineaCredito / retirarLineaCredito
            copiarDatosCredito(actual, cliente);
            return clienteDAO.update(cliente);
        });
    }

    // RF003, RNF002
    @Override
    public void desactivar(int idCliente, int idResponsable) throws SQLException {
        Transaccion.ejecutar(() -> {
            verificarPermiso(idResponsable);
            clienteDAO.remove(obtenerExistente(idCliente));
            return null;
        });
    }

    // RF003
    @Override
    public Cliente obtener(int idCliente) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return clienteDAO.load(idCliente);
        });
    }

    // RF003
    @Override
    public List<Cliente> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return clienteDAO.listarTodos();
        });
    }

    // RF003
    @Override
    public Cliente buscarPorDocumento(String numeroDocumento) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return clienteDAO.buscarPorDocumento(
                    Validador.textoObligatorio(numeroDocumento, "numero de documento", 20));
        });
    }

    // RF003
    @Override
    public List<Cliente> buscarPorRazonSocial(String razonSocial) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return clienteDAO.buscarPorRazonSocial(
                    Validador.textoObligatorio(razonSocial, "razon social", 150));
        });
    }

    // RF003
    @Override
    public List<Cliente> listarPorEstado(boolean estado) throws SQLException {
        return Transaccion.ejecutar(() -> {
            return clienteDAO.listarPorEstado(estado);
        });
    }

    // RF003
    @Override
    public Cliente asignarLineaCredito(int idCliente, java.math.BigDecimal limiteCredito, int plazoCreditoDias,
                                       String calificacionCrediticia, int idAdministrador) throws SQLException {
        return Transaccion.ejecutar(() -> {
            usuarioBO.obtenerAdministradorActivo(idAdministrador);
            Cliente cliente = obtenerExistente(idCliente);
            if(!cliente.isEstado()){
                throw new IllegalStateException("No se puede asignar credito a un cliente desactivado");
            }
            if(limiteCredito == null || limiteCredito.setScale(2, java.math.RoundingMode.HALF_UP).signum() <= 0){
                throw new IllegalArgumentException("El limite de credito debe ser mayor a 0");
            }
            if(plazoCreditoDias <= 0){
                throw new IllegalArgumentException("El plazo de credito debe ser mayor a 0 dias");
            }
            cliente.setCondicionPago(CondicionPago.CREDITO);
            cliente.setLimiteCredito(limiteCredito);
            cliente.setPlazoCreditoDias(plazoCreditoDias);
            cliente.setCalificacionCrediticia(
                    Validador.textoOpcional(calificacionCrediticia, "calificacion crediticia", 20));
            return clienteDAO.update(cliente);
        });
    }

    // RF003
    @Override
    public Cliente retirarLineaCredito(int idCliente, int idAdministrador) throws SQLException {
        return Transaccion.ejecutar(() -> {
            usuarioBO.obtenerAdministradorActivo(idAdministrador);
            Cliente cliente = obtenerExistente(idCliente);
            // se conserva la calificacion crediticia como antecedente
            cliente.setCondicionPago(CondicionPago.CONTADO);
            cliente.setLimiteCredito(java.math.BigDecimal.ZERO);
            cliente.setPlazoCreditoDias(0);
            return clienteDAO.update(cliente);
        });
    }

    // RF003: registran, modifican y desactivan clientes el Vendedor y el Administrador
    private void verificarPermiso(int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.ADMINISTRADOR);
    }

    private Cliente obtenerExistente(int idCliente) throws SQLException {
        Cliente cliente = clienteDAO.load(idCliente);
        if(cliente == null){
            throw new IllegalArgumentException("No existe el cliente con id " + idCliente);
        }
        return cliente;
    }

    private void validarDatos(Cliente cliente) {
        Validador.obligatorio(cliente, "cliente");
        Validador.obligatorio(cliente.getTipoPersona(), "tipo de persona");
        // El documento no determina la naturaleza juridica.
        cliente.setNumeroDocumento(Validador.numeroDocumento(cliente.getTipoDocumento(), cliente.getNumeroDocumento()));
        if (cliente.esEmpresa()) {
            // una empresa se identifica por su razon social, que es unica
            cliente.setRazonSocial(Validador.textoObligatorio(cliente.getRazonSocial(), "razon social", 150));
            cliente.setNombres(null);
            cliente.setApellidos(null);
        } else {
            // una persona natural no tiene razon social: nombres y apellidos
            cliente.setNombres(Validador.textoObligatorio(cliente.getNombres(), "nombres", 100));
            cliente.setApellidos(Validador.textoObligatorio(cliente.getApellidos(), "apellidos", 100));
            cliente.setRazonSocial(null);
        }
        Validador.obligatorio(cliente.getContacto(), "datos de contacto");
        cliente.getContacto().setDireccion(Validador.textoOpcional(cliente.getContacto().getDireccion(), "direccion", 200));
        cliente.getContacto().setTelefono(Validador.telefono(cliente.getContacto().getTelefono()));
        cliente.getContacto().setCorreo(Validador.correo(cliente.getContacto().getCorreo()));
        cliente.getContacto().setNombreContacto(Validador.textoOpcional(cliente.getContacto().getNombreContacto(), "contacto", 100));
    }

    // la razon social es UNIQUE en la tabla; aqui se da un mensaje claro antes de llegar a la BD
    private void validarRazonSocialUnica(Cliente cliente) throws SQLException {
        if (!cliente.esEmpresa()) {
            return;
        }
        for (Cliente otro : clienteDAO.buscarPorRazonSocial(cliente.getRazonSocial())) {
            if (otro.getId() != cliente.getId() && cliente.getRazonSocial().equalsIgnoreCase(otro.getRazonSocial())) {
                throw new IllegalArgumentException("Ya existe un cliente con la razon social " + cliente.getRazonSocial());
            }
        }
    }

    private void validarDocumentoUnico(Cliente cliente) throws SQLException {
        Cliente existente = clienteDAO.buscarPorDocumento(cliente.getNumeroDocumento());
        if(existente != null && existente.getId() != cliente.getId()){
            throw new IllegalArgumentException("Ya existe un cliente con el documento " + cliente.getNumeroDocumento());
        }
    }

    private void copiarDatosCredito(Cliente origen, Cliente destino) {
        destino.setCondicionPago(origen.getCondicionPago());
        destino.setLimiteCredito(origen.getLimiteCredito());
        destino.setPlazoCreditoDias(origen.getPlazoCreditoDias());
        destino.setCalificacionCrediticia(origen.getCalificacionCrediticia());
    }
}
