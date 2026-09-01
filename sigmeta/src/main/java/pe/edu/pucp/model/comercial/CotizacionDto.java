package pe.edu.pucp.model.comercial;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.model.comun.DocumentoComercial;
import pe.edu.pucp.model.enums.EstadoCotizacion;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.socio.ClienteDto;
import pe.edu.pucp.model.usuario.UsuarioDto;

/**
 * Cotizacion dirigida a un cliente, con vigencia y estado (RF006).
 */
public class CotizacionDto extends DocumentoComercial {

    private int idCotizacion;
    private ClienteDto cliente;
    private LocalDate fechaVigencia;
    private EstadoCotizacion estado;
    private List<DetalleCotizacionDto> detalles;

    public CotizacionDto() {
        super();
        this.detalles = new ArrayList<>();
    }

    public CotizacionDto(String numero, LocalDate fechaEmision, Moneda moneda, double subTotal, double igv, double total, String observaciones, LocalDateTime fechaRegistro, UsuarioDto usuarioRegistro, boolean anulado, String motivoAnulacion, LocalDateTime fechaAnulacion, int idCotizacion, ClienteDto cliente, LocalDate fechaVigencia, EstadoCotizacion estado, List<DetalleCotizacionDto> detalles) {
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

    public ClienteDto getCliente() {
        return cliente;
    }

    public void setCliente(ClienteDto cliente) {
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

    public List<DetalleCotizacionDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCotizacionDto> detalles) {
        this.detalles = detalles;
    }

}
