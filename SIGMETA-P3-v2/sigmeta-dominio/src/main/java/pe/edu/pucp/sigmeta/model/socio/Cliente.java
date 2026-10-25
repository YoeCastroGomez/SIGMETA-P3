package pe.edu.pucp.sigmeta.model.socio;

import pe.edu.pucp.sigmeta.model.comun.DatosContacto;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import pe.edu.pucp.sigmeta.model.enums.TipoPersona;
import pe.edu.pucp.sigmeta.model.enums.TipoDocumentoIdentidad;

/** Cliente: tipo de persona independiente del documento de identidad. */
public class Cliente {

    private TipoPersona tipoPersona;
    public TipoPersona getTipoPersona() { return tipoPersona; }
    public void setTipoPersona(TipoPersona tipoPersona) { this.tipoPersona = tipoPersona; }

    private int id;
    private String razonSocial;
    private String nombres;
    private String apellidos;
    private DatosContacto contacto;
    private boolean activo;
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    private CondicionPago condicionPago;
    private int plazoCreditoDias;
    private java.math.BigDecimal limiteCredito = java.math.BigDecimal.ZERO;
    private pe.edu.pucp.sigmeta.model.enums.CalificacionCrediticia calificacionCrediticia = pe.edu.pucp.sigmeta.model.enums.CalificacionCrediticia.SIN_EVALUAR;

    public Cliente() {
        this.contacto = new DatosContacto();
    }

    public Cliente(int id, TipoPersona tipoPersona, String razonSocial, String nombres, String apellidos, DatosContacto contacto,
                   boolean estado, TipoDocumentoIdentidad tipoDocumento, String numeroDocumento,
                   String contactoNombre, CondicionPago condicionPago, int plazoCreditoDias,
                   java.math.BigDecimal limiteCredito, String calificacionCrediticia) {
        this();
        this.id = id;
        this.tipoPersona = tipoPersona;
        this.razonSocial = razonSocial;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.contacto = contacto;
        this.activo = estado;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.contacto.setNombreContacto(contactoNombre);
        this.condicionPago = condicionPago;
        this.plazoCreditoDias = plazoCreditoDias;
        this.limiteCredito = java.util.Objects.requireNonNull(limiteCredito).setScale(2, java.math.RoundingMode.HALF_UP);
        this.calificacionCrediticia = pe.edu.pucp.sigmeta.model.enums.CalificacionCrediticia.desdeTexto(calificacionCrediticia);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public DatosContacto getContacto() {
        return contacto;
    }

    public void setContacto(DatosContacto contacto) {
        this.contacto = contacto;
    }

    public boolean isEstado() {
        return activo;
    }

    public void setEstado(boolean estado) {
        this.activo = estado;
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

    @Deprecated
    public String getContactoNombre() {
        return contacto.getNombreContacto();
    }

    @Deprecated
    public void setContactoNombre(String contactoNombre) {
        this.contacto.setNombreContacto(contactoNombre);
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

    public java.math.BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(java.math.BigDecimal limiteCredito) {
        this.limiteCredito = java.util.Objects.requireNonNull(limiteCredito).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public String getCalificacionCrediticia() {
        return calificacionCrediticia.name();
    }

    public void setCalificacionCrediticia(String calificacionCrediticia) {
        this.calificacionCrediticia = pe.edu.pucp.sigmeta.model.enums.CalificacionCrediticia.desdeTexto(calificacionCrediticia);
    }

    // Persona juridica; una persona natural tambien puede tener RUC.
    public boolean esEmpresa() {
        return tipoPersona == TipoPersona.JURIDICA;
    }

    // nombre para mostrar en documentos: razon social o nombres y apellidos
    public String getNombreMostrado() {
        return esEmpresa() ? razonSocial : nombres + " " + apellidos;
    }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public pe.edu.pucp.sigmeta.model.enums.CalificacionCrediticia getCalificacion() { return calificacionCrediticia; }

}
