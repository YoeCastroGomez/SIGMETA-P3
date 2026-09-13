package pe.edu.pucp.ejecucion;

import pe.edu.pucp.model.socio.Cliente;
import pe.edu.pucp.model.socio.Proveedor;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Contenedor simple para pasar los maestros de una prueba a otra.
 * No pertenece al dominio, solo sirve para encadenar las pruebas.
 */
public class DatosMaestros {

    private Usuario administrador;
    private Cliente cliente;
    private Proveedor proveedor;

    public DatosMaestros(Usuario administrador, Cliente cliente, Proveedor proveedor) {
        this.administrador = administrador;
        this.cliente = cliente;
        this.proveedor = proveedor;
    }

    public Usuario getAdministrador() {
        return administrador;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }
}
