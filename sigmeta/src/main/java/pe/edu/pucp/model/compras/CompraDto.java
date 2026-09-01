package pe.edu.pucp.model.compras;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comun.DocumentoComercial;
import pe.edu.pucp.model.enums.EstadoCompra;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.ProveedorDto;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Compra realizada a un proveedor (RF010).
 */
public class CompraDto extends DocumentoComercial {

    private int idCompra;
    private ProveedorDto proveedor;
    private EstadoCompra estado;
    private LocalDate fechaRecepcionEstimada;
    private List<DetalleCompraDto> detalles;

    public CompraDto() {
        super();
        this.detalles = new ArrayList<>();
    }

    public CompraDto(String numero, LocalDate fechaEmision, Moneda moneda, double subTotal, double igv, double total, String observaciones, LocalDateTime fechaRegistro, UsuarioDto usuarioRegistro, boolean anulado, String motivoAnulacion, LocalDateTime fechaAnulacion, int idCompra, ProveedorDto proveedor, EstadoCompra estado, LocalDate fechaRecepcionEstimada, List<DetalleCompraDto> detalles) {
        super(numero, fechaEmision, moneda, subTotal, igv, total, observaciones, fechaRegistro, usuarioRegistro, anulado, motivoAnulacion, fechaAnulacion);
        this.idCompra = idCompra;
        this.proveedor = proveedor;
        this.estado = estado;
        this.fechaRecepcionEstimada = fechaRecepcionEstimada;
        this.detalles = detalles;
    }

    public int getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(int idCompra) {
        this.idCompra = idCompra;
    }

    public ProveedorDto getProveedor() {
        return proveedor;
    }

    public void setProveedor(ProveedorDto proveedor) {
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

    public List<DetalleCompraDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompraDto> detalles) {
        this.detalles = detalles;
    }

}
