package pe.edu.pucp.model.comercial;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comun.DocumentoComercial;
import pe.edu.pucp.model.enums.CondicionPago;
import pe.edu.pucp.model.enums.EstadoVenta;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.ClienteDto;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Venta registrada a partir de una orden de compra de cliente o de forma directa (RF008).
 */
public class VentaDto extends DocumentoComercial {

    private int idVenta;
    private ClienteDto cliente;
    private OrdenCompraClienteDto ordenCompraCliente;
    private CondicionPago condicionPago;
    private int plazoCreditoDias;
    private EstadoVenta estado;
    private List<DetalleVentaDto> detalles;

    public VentaDto() {
        super();
        this.detalles = new ArrayList<>();
    }

    public VentaDto(String numero, LocalDate fechaEmision, Moneda moneda, double subTotal, double igv, double total, String observaciones, LocalDateTime fechaRegistro, UsuarioDto usuarioRegistro, boolean anulado, String motivoAnulacion, LocalDateTime fechaAnulacion, int idVenta, ClienteDto cliente, OrdenCompraClienteDto ordenCompraCliente, CondicionPago condicionPago, int plazoCreditoDias, EstadoVenta estado, List<DetalleVentaDto> detalles) {
        super(numero, fechaEmision, moneda, subTotal, igv, total, observaciones, fechaRegistro, usuarioRegistro, anulado, motivoAnulacion, fechaAnulacion);
        this.idVenta = idVenta;
        this.cliente = cliente;
        this.ordenCompraCliente = ordenCompraCliente;
        this.condicionPago = condicionPago;
        this.plazoCreditoDias = plazoCreditoDias;
        this.estado = estado;
        this.detalles = detalles;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public ClienteDto getCliente() {
        return cliente;
    }

    public void setCliente(ClienteDto cliente) {
        this.cliente = cliente;
    }

    public OrdenCompraClienteDto getOrdenCompraCliente() {
        return ordenCompraCliente;
    }

    public void setOrdenCompraCliente(OrdenCompraClienteDto ordenCompraCliente) {
        this.ordenCompraCliente = ordenCompraCliente;
    }

    public CondicionPago getCondicionPago() {
        return condicionPago;
    }

    public void setCondicionPago(CondicionPago condicionPago) {
        this.condicionPago = condicionPago;
    }

    public int getPlazoCreditoDias() {
        return plazoCreditoDias;
    }

    public void setPlazoCreditoDias(int plazoCreditoDias) {
        this.plazoCreditoDias = plazoCreditoDias;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoVenta estado) {
        this.estado = estado;
    }

    public List<DetalleVentaDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVentaDto> detalles) {
        this.detalles = detalles;
    }

}
