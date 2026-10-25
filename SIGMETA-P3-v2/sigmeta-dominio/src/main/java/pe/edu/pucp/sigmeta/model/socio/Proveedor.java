package pe.edu.pucp.sigmeta.model.socio;

import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.comun.DatosContacto;
import pe.edu.pucp.sigmeta.model.enums.CondicionPago;
import java.util.List;
import java.util.ArrayList;

/**
 * Proveedor al que se realizan las compras (RF004). Siempre es una empresa con RUC y razon social unica.
 */
public class Proveedor {

    private int id;
    private String razonSocial;
    private DatosContacto contacto;
    private boolean estado;
    private String ruc;
    private String rubro;
    private int plazoEntregaDias;
    private CondicionPago condicionPago;
    private List<Compra> compras;

    public Proveedor() {
        this.contacto = new DatosContacto();
        this.compras = new ArrayList<>();
    }

    public Proveedor(int id, String razonSocial, DatosContacto contacto, boolean estado, String ruc,
                     String rubro, String contactoNombre, int plazoEntregaDias, CondicionPago condicionPago) {
        this();
        this.id = id;
        this.razonSocial = razonSocial;
        this.contacto = contacto;
        this.estado = estado;
        this.ruc = ruc;
        this.rubro = rubro;
        this.contacto.setNombreContacto(contactoNombre);
        this.plazoEntregaDias = plazoEntregaDias;
        this.condicionPago = condicionPago;
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

    public DatosContacto getContacto() {
        return contacto;
    }

    public void setContacto(DatosContacto contacto) {
        this.contacto = contacto;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
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

    @Deprecated
    public String getContactoNombre() {
        return contacto.getNombreContacto();
    }

    @Deprecated
    public void setContactoNombre(String contactoNombre) {
        this.contacto.setNombreContacto(contactoNombre);
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

    public List<Compra> getCompras() {
        return compras;
    }

    public void setCompras(List<Compra> compras) {
        this.compras = compras;
    }
}
