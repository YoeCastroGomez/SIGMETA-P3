package pe.edu.pucp.model.comercial;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comun.DocumentoComercial;
import pe.edu.pucp.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.ClienteDto;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Orden de compra recibida del cliente (RF007).
 */
public class OrdenCompraClienteDto extends DocumentoComercial {

    private int idOrdenCompraCliente;
    private ClienteDto cliente;
    private CotizacionDto cotizacion;
    private String numeroOrdenCliente;
    private EstadoOrdenCompraCliente estado;
    private List<DetalleOrdenCompraClienteDto> detalles;

    public OrdenCompraClienteDto() {
        super();
        this.detalles = new ArrayList<>();
    }

    public OrdenCompraClienteDto(String numero, LocalDate fechaEmision, Moneda moneda, double subTotal, double igv, double total, String observaciones, LocalDateTime fechaRegistro, UsuarioDto usuarioRegistro, boolean anulado, String motivoAnulacion, LocalDateTime fechaAnulacion, int idOrdenCompraCliente, ClienteDto cliente, CotizacionDto cotizacion, String numeroOrdenCliente, EstadoOrdenCompraCliente estado, List<DetalleOrdenCompraClienteDto> detalles) {
        super(numero, fechaEmision, moneda, subTotal, igv, total, observaciones, fechaRegistro, usuarioRegistro, anulado, motivoAnulacion, fechaAnulacion);
        this.idOrdenCompraCliente = idOrdenCompraCliente;
        this.cliente = cliente;
        this.cotizacion = cotizacion;
        this.numeroOrdenCliente = numeroOrdenCliente;
        this.estado = estado;
        this.detalles = detalles;
    }

    public int getIdOrdenCompraCliente() {
        return idOrdenCompraCliente;
    }

    public void setIdOrdenCompraCliente(int idOrdenCompraCliente) {
        this.idOrdenCompraCliente = idOrdenCompraCliente;
    }

    public ClienteDto getCliente() {
        return cliente;
    }

    public void setCliente(ClienteDto cliente) {
        this.cliente = cliente;
    }

    public CotizacionDto getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(CotizacionDto cotizacion) {
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

    public List<DetalleOrdenCompraClienteDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleOrdenCompraClienteDto> detalles) {
        this.detalles = detalles;
    }

}
