package pe.edu.pucp.model.ventas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comun.DocumentoComercial;
import pe.edu.pucp.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.Cliente;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Orden de compra recibida del cliente (RF007).
 */
public class OrdenCompraCliente extends DocumentoComercial {

    private int id;
    private Cliente cliente;
    private Cotizacion cotizacion;
    private String numeroOrdenCliente;
    private EstadoOrdenCompraCliente estado;
    private List<DetalleOrdenCompraCliente> detalles;

    public OrdenCompraCliente() {
        super();
        this.detalles = new ArrayList<>();
    }

    public OrdenCompraCliente(String numero, LocalDate fechaEmision, Moneda moneda, double subTotal, double igv, double total, String observaciones, LocalDateTime fechaRegistro, Usuario usuarioRegistro, boolean anulado, String motivoAnulacion, LocalDateTime fechaAnulacion, int id, Cliente cliente, Cotizacion cotizacion, String numeroOrdenCliente, EstadoOrdenCompraCliente estado, List<DetalleOrdenCompraCliente> detalles) {
        super(numero, fechaEmision, moneda, subTotal, igv, total, observaciones, fechaRegistro, usuarioRegistro, anulado, motivoAnulacion, fechaAnulacion);
        this.id = id;
        this.cliente = cliente;
        this.cotizacion = cotizacion;
        this.numeroOrdenCliente = numeroOrdenCliente;
        this.estado = estado;
        this.detalles = detalles;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

    public String getNumeroOrdenCliente() {
        return numeroOrdenCliente;
    }

    public void setNumeroOrdenCliente(String numeroOrdenCliente) {
        this.numeroOrdenCliente = numeroOrdenCliente;
    }

    public EstadoOrdenCompraCliente getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenCompraCliente estado) {
        this.estado = estado;
    }

    public List<DetalleOrdenCompraCliente> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleOrdenCompraCliente> detalles) {
        this.detalles = detalles;
    }

}
