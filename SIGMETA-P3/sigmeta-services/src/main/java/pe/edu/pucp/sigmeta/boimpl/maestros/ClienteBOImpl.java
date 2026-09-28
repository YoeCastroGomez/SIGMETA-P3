package pe.edu.pucp.sigmeta.boimpl.maestros;

import pe.edu.pucp.sigmeta.bo.maestros.ClienteBO;
import pe.edu.pucp.sigmeta.bo.seguridad.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.seguridad.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.maestros.ClienteDAO;
import pe.edu.pucp.sigmeta.daoimpl.maestros.ClienteDAOImpl;
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
        verificarPermiso(idResponsable);
        validarDatos(cliente);
        validarDocumentoUnico(cliente);
        // todo cliente nuevo empieza al contado; la linea de credito la asigna el Administrador
        cliente.setCondicionPago(CondicionPago.CONTADO);
        cliente.setPlazoCreditoDias(0);
        cliente.setLimiteCredito(0);
        cliente.setCalificacionCrediticia(null);
        return clienteDAO.save(cliente);
    }

    // RF003
    @Override
    public Cliente modificar(Cliente cliente, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        Validador.obligatorio(cliente, "cliente");
        Cliente actual = obtenerExistente(cliente.getId());
        validarDatos(cliente);
        validarDocumentoUnico(cliente);
        // los datos de credito no se cambian aqui, solo con asignarLineaCredito / retirarLineaCredito
        copiarDatosCredito(actual, cliente);
        return clienteDAO.update(cliente);
    }

    // RF003, RNF002
    @Override
    public void desactivar(int idCliente, int idResponsable) throws SQLException {
        verificarPermiso(idResponsable);
        clienteDAO.remove(obtenerExistente(idCliente));
    }

    // RF003
    @Override
    public Cliente obtener(int idCliente) throws SQLException {
        return clienteDAO.load(idCliente);
    }

    // RF003
    @Override
    public List<Cliente> listarTodos() throws SQLException {
        return clienteDAO.listarTodos();
    }

    // RF003
    @Override
    public Cliente buscarPorDocumento(String numeroDocumento) throws SQLException {
        return clienteDAO.buscarPorDocumento(
                Validador.textoObligatorio(numeroDocumento, "numero de documento", 20));
    }

    // RF003
    @Override
    public List<Cliente> buscarPorRazonSocial(String razonSocial) throws SQLException {
        return clienteDAO.buscarPorRazonSocial(
                Validador.textoObligatorio(razonSocial, "razon social", 150));
    }

    // RF003
    @Override
    public List<Cliente> listarPorEstado(boolean estado) throws SQLException {
        return clienteDAO.listarPorEstado(estado);
    }

    // RF003
    @Override
    public Cliente asignarLineaCredito(int idCliente, double limiteCredito, int plazoCreditoDias,
                                       String calificacionCrediticia, int idAdministrador) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idAdministrador);
        Cliente cliente = obtenerExistente(idCliente);
        if(!cliente.isEstado()){
            throw new IllegalStateException("No se puede asignar credito a un cliente desactivado");
        }
        if(limiteCredito <= 0){
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
    }

    // RF003
    @Override
    public Cliente retirarLineaCredito(int idCliente, int idAdministrador) throws SQLException {
        usuarioBO.obtenerAdministradorActivo(idAdministrador);
        Cliente cliente = obtenerExistente(idCliente);
        // se conserva la calificacion crediticia como antecedente
        cliente.setCondicionPago(CondicionPago.CONTADO);
        cliente.setLimiteCredito(0);
        cliente.setPlazoCreditoDias(0);
        return clienteDAO.update(cliente);
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
        cliente.setRazonSocial(Validador.textoObligatorio(cliente.getRazonSocial(), "razon social", 150));
        cliente.setDireccion(Validador.textoOpcional(cliente.getDireccion(), "direccion", 200));
        cliente.setTelefono(Validador.telefono(cliente.getTelefono()));
        cliente.setCorreo(Validador.correo(cliente.getCorreo()));
        // RF003: el tipo de documento distingue persona natural (DNI, CE, pasaporte) de empresa (RUC)
        cliente.setNumeroDocumento(Validador.numeroDocumento(cliente.getTipoDocumento(), cliente.getNumeroDocumento()));
        cliente.setContactoNombre(Validador.textoOpcional(cliente.getContactoNombre(), "contacto", 100));
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
