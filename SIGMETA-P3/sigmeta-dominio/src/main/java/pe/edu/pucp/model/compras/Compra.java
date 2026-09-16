package pe.edu.pucp.model.compras;

import pe.edu.pucp.model.almacen.RecepcionCompra;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comun.DocumentoComercial;
import pe.edu.pucp.model.enums.EstadoCompra;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.Proveedor;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Compra realizada a un proveedor (RF010).
 */
public class Compra extends DocumentoComercial {

    private int id;
    private Proveedor proveedor;
    private EstadoCompra estado;
    private LocalDate fechaRecepcionEstimada;
    private List<DetalleCompra> detalles;
    private List<RecepcionCompra> recepciones;

    public Compra() {
        super();
        this.detalles = new ArrayList<>();
        this.recepciones = new ArrayList<>();
    }

    public Compra(String numero, LocalDate fechaEmision, Moneda moneda, double subTotal, double igv, double total, String observaciones, LocalDateTime fechaRegistro, Usuario usuarioRegistro, boolean anulado, String motivoAnulacion, LocalDateTime fechaAnulacion, int id, Proveedor proveedor, EstadoCompra estado, LocalDate fechaRecepcionEstimada, List<DetalleCompra> detalles) {
        super(numero, fechaEmision, moneda, subTotal, igv, total, observaciones, fechaRegistro, usuarioRegistro, anulado, motivoAnulacion, fechaAnulacion);
        this.id = id;
        this.proveedor = proveedor;
        this.estado = estado;
        this.fechaRecepcionEstimada = fechaRecepcionEstimada;
        this.detalles = detalles;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public EstadoCompra getEstado() {
        return estado;
    }

    public void setEstado(EstadoCompra estado) {
        this.estado = estado;
    }

    public LocalDate getFechaRecepcionEstimada() {
        return fechaRecepcionEstimada;
    }

    public void setFechaRecepcionEstimada(LocalDate fechaRecepcionEstimada) {
        this.fechaRecepcionEstimada = fechaRecepcionEstimada;
    }

    public List<DetalleCompra> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompra> detalles) {
        this.detalles = detalles;
    }


    public List<RecepcionCompra> getRecepciones() {
        return recepciones;
    }

    public void setRecepciones(List<RecepcionCompra> recepciones) {
        this.recepciones = recepciones;
    }
}
