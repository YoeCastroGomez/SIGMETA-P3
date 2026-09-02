package pe.edu.pucp.model.socio;

import pe.edu.pucp.model.comun.EntidadComercial;
import pe.edu.pucp.model.enums.CondicionPago;
import pe.edu.pucp.model.enums.TipoDocumentoIdentidad;

/**
 * Cliente de la empresa (RF003 y RF009).
 */
public class ClienteDto extends EntidadComercial {

    private int idCliente;
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    private String contactoNombre;
    private CondicionPago condicionPago;
    private int plazoCreditoDias;
    private double limiteCredito;

    public ClienteDto() {
        super();
    }

    public ClienteDto(String razonSocial, String direccion, String telefono, String correo,
                      boolean estado, int idCliente, TipoDocumentoIdentidad tipoDocumento,
                      String numeroDocumento, String contactoNombre, CondicionPago condicionPago,
                      int plazoCreditoDias, double limiteCredito) {
        super(razonSocial, direccion, telefono, correo, estado);
        this.idCliente = idCliente;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.contactoNombre = contactoNombre;
        this.condicionPago = condicionPago;
        this.plazoCreditoDias = plazoCreditoDias;
        this.limiteCredito = limiteCredito;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public TipoDocumentoIdentidad getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumentoIdentidad tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getContactoNombre() {
        return contactoNombre;
    }

    public void setContactoNombre(String contactoNombre) {
        this.contactoNombre = contactoNombre;
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

    public double getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(double limiteCredito) {
        this.limiteCredito = limiteCredito;
    }
}
