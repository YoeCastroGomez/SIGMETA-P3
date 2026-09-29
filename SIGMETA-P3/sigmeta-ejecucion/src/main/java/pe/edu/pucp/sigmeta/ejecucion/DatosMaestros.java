package pe.edu.pucp.sigmeta.ejecucion;

import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;

/**
 * Contenedor simple para pasar los maestros de una prueba a otra.
 * No pertenece al dominio, solo sirve para encadenar las pruebas.
 */
public class DatosMaestros {

    private Usuario administrador;
    private Usuario vendedor;
    private Cliente cliente;
    private Proveedor proveedor;

    public DatosMaestros(Usuario administrador, Usuario vendedor, Cliente cliente, Proveedor proveedor) {
        this.administrador = administrador;
        this.vendedor = vendedor;
        this.cliente = cliente;
        this.proveedor = proveedor;
    }

    public Usuario getAdministrador() {
        return administrador;
    }

    public Usuario getVendedor() {
        return vendedor;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }
}
