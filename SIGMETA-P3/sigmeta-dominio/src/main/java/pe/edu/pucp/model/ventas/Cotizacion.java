package pe.edu.pucp.model.ventas;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comun.DocumentoComercial;
import pe.edu.pucp.model.enums.EstadoCotizacion;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.Cliente;
import pe.edu.pucp.model.usuario.Usuario;

/**
 * Cotizacion dirigida a un cliente, con vigencia y estado (RF006).
 */
public class Cotizacion extends DocumentoComercial {

    private int idCotizacion;
    private Cliente cliente;
    private LocalDate fechaVigencia;
    private EstadoCotizacion estado;
    private List<DetalleCotizacion> detalles;

    public Cotizacion() {
        super();
        this.detalles = new ArrayList<>();
    }

    public Cotizacion(String numero, LocalDate fechaEmision, Moneda moneda, double subTotal, double igv, double total, String observaciones, LocalDateTime fechaRegistro, Usuario usuarioRegistro, boolean anulado, String motivoAnulacion, LocalDateTime fechaAnulacion, int idCotizacion, Cliente cliente, LocalDate fechaVigencia, EstadoCotizacion estado, List<DetalleCotizacion> detalles) {
        super(numero, fechaEmision, moneda, subTotal, igv, total, observaciones, fechaRegistro, usuarioRegistro, anulado, motivoAnulacion, fechaAnulacion);
        this.idCotizacion = idCotizacion;
        this.cliente = cliente;
        this.fechaVigencia = fechaVigencia;
        this.estado = estado;
        this.detalles = detalles;
    }

    public int getIdCotizacion() {
        return idCotizacion;
    }

    public void setIdCotizacion(int idCotizacion) {
        this.idCotizacion = idCotizacion;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDate getFechaVigencia() {
        return fechaVigencia;
    }

    public void setFechaVigencia(LocalDate fechaVigencia) {
        this.fechaVigencia = fechaVigencia;
    }

    public EstadoCotizacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoCotizacion estado) {
        this.estado = estado;
    }

    public List<DetalleCotizacion> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCotizacion> detalles) {
        this.detalles = detalles;
    }

}
