package pe.edu.pucp.model.socio;

import pe.edu.pucp.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.model.ventas.Venta;
import pe.edu.pucp.model.comun.EntidadComercial;
import pe.edu.pucp.model.enums.CondicionPago;
import pe.edu.pucp.model.enums.TipoDocumentoIdentidad;
import java.util.List;
import java.util.ArrayList;

/**
 * Cliente de la empresa (RF003 y RF009).
 */
public class Cliente extends EntidadComercial {

    private int idCliente;
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    private String contactoNombre;
    private CondicionPago condicionPago;
    private int plazoCreditoDias;
    private double limiteCredito;
    private String calificacionCrediticia;
    private List<Venta> ventas;
    private List<CuentaPorCobrar> cuentasPorCobrar;

    public Cliente() {
        super();
        this.ventas = new ArrayList<>();
        this.cuentasPorCobrar = new ArrayList<>();
    }

    public Cliente(String razonSocial, String direccion, String telefono, String correo,
                      boolean estado, int idCliente, TipoDocumentoIdentidad tipoDocumento,
                      String numeroDocumento, String contactoNombre, CondicionPago condicionPago,
                      int plazoCreditoDias, double limiteCredito, String calificacionCrediticia) {
        super(razonSocial, direccion, telefono, correo, estado);
        this.idCliente = idCliente;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.contactoNombre = contactoNombre;
        this.condicionPago = condicionPago;
        this.plazoCreditoDias = plazoCreditoDias;
        this.limiteCredito = limiteCredito;
        this.calificacionCrediticia = calificacionCrediticia;
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

    public List<Venta> getVentas() {
        return ventas;
    }

    public void setVentas(List<Venta> ventas) {
        this.ventas = ventas;
    }

    public List<CuentaPorCobrar> getCuentasPorCobrar() {
        return cuentasPorCobrar;
    }

    public void setCuentasPorCobrar(List<CuentaPorCobrar> cuentasPorCobrar) {
        this.cuentasPorCobrar = cuentasPorCobrar;
    }

    public String getCalificacionCrediticia() {
        return calificacionCrediticia;
    }

    public void setCalificacionCrediticia(String calificacionCrediticia) {
        this.calificacionCrediticia = calificacionCrediticia;
    }
}
