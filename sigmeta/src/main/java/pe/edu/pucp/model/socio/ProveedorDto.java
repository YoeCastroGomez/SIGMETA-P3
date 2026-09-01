package pe.edu.pucp.model.socio;

import pe.edu.pucp.model.comun.EntidadComercial;
import pe.edu.pucp.model.enums.CondicionPago;

/**
 * Proveedor al que se realizan las compras (RF004).
 */
public class ProveedorDto extends EntidadComercial {

    private int idProveedor;
    private String ruc;
    private String rubro;
    private String contactoNombre;
    private int plazoEntregaDias;
    private CondicionPago condicionPago;

    public ProveedorDto() {
        super();
    }

    public ProveedorDto(String razonSocial, String direccion, String telefono, String correo, boolean estado, int idProveedor, String ruc, String rubro, String contactoNombre, int plazoEntregaDias, CondicionPago condicionPago) {
        super(razonSocial, direccion, telefono, correo, estado);
        this.idProveedor = idProveedor;
        this.ruc = ruc;
        this.rubro = rubro;
        this.contactoNombre = contactoNombre;
        this.plazoEntregaDias = plazoEntregaDias;
        this.condicionPago = condicionPago;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public String getContactoNombre() {
        return contactoNombre;
    }

    public void setContactoNombre(String contactoNombre) {
        this.contactoNombre = contactoNombre;
    }

    public int getPlazoEntregaDias() {
        return plazoEntregaDias;
    }

    public void setPlazoEntregaDias(int plazoEntregaDias) {
        this.plazoEntregaDias = plazoEntregaDias;
    }

    public CondicionPago getCondicionPago() {
        return condicionPago;
    }

    public void setCondicionPago(CondicionPago condicionPago) {
        this.condicionPago = condicionPago;
    }

}
