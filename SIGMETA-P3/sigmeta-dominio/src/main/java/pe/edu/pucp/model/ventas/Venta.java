package pe.edu.pucp.model.ventas;

import pe.edu.pucp.model.almacen.Despacho;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comun.DocumentoComercial;
import pe.edu.pucp.model.enums.CondicionPago;
import pe.edu.pucp.model.enums.EstadoVenta;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.Cliente;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Venta registrada a partir de una orden de compra de cliente o de forma directa (RF008).
 */
public class Venta extends DocumentoComercial {

    private int id;
    private Cliente cliente;
    private OrdenCompraCliente ordenCompraCliente;
    private CondicionPago condicionPago;
    private int plazoCreditoDias;
    private EstadoVenta estado;
    private List<DetalleVenta> detalles;
    private List<Despacho> despachos;
    private List<Comprobante> comprobantes;

    public Venta() {
        super();
        this.detalles = new ArrayList<>();
        this.despachos = new ArrayList<>();
        this.comprobantes = new ArrayList<>();
    }

    public Venta(String numero, LocalDate fechaEmision, Moneda moneda, double subTotal, double igv, double total, String observaciones, LocalDateTime fechaRegistro, Usuario usuarioRegistro, boolean anulado, String motivoAnulacion, LocalDateTime fechaAnulacion, int id, Cliente cliente, OrdenCompraCliente ordenCompraCliente, CondicionPago condicionPago, int plazoCreditoDias, EstadoVenta estado, List<DetalleVenta> detalles) {
        super(numero, fechaEmision, moneda, subTotal, igv, total, observaciones, fechaRegistro, usuarioRegistro, anulado, motivoAnulacion, fechaAnulacion);
        this.id = id;
        this.cliente = cliente;
        this.ordenCompraCliente = ordenCompraCliente;
        this.condicionPago = condicionPago;
        this.plazoCreditoDias = plazoCreditoDias;
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

    public OrdenCompraCliente getOrdenCompraCliente() {
        return ordenCompraCliente;
    }

    public void setOrdenCompraCliente(OrdenCompraCliente ordenCompraCliente) {
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

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }


    public List<Despacho> getDespachos() {
        return despachos;
    }

    public void setDespachos(List<Despacho> despachos) {
        this.despachos = despachos;
    }

    public List<Comprobante> getComprobantes() {
        return comprobantes;
    }

    public void setComprobantes(List<Comprobante> comprobantes) {
        this.comprobantes = comprobantes;
    }
}
