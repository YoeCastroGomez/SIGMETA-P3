package pe.edu.pucp.model.socio;

import pe.edu.pucp.model.compras.Compra;
import pe.edu.pucp.model.comun.EntidadComercial;
import pe.edu.pucp.model.enums.CondicionPago;
import java.util.List;
import java.util.ArrayList;

/**
 * Proveedor al que se realizan las compras (RF004).
 */
public class Proveedor extends EntidadComercial {

    private int id;
    private String ruc;
    private String rubro;
    private String contactoNombre;
    private int plazoEntregaDias;
    private CondicionPago condicionPago;
    private List<Compra> compras;

    public Proveedor() {
        super();
        this.compras = new ArrayList<>();
    }

    public Proveedor(String razonSocial, String direccion, String telefono, String correo, boolean estado, int id, String ruc, String rubro, String contactoNombre, int plazoEntregaDias, CondicionPago condicionPago) {
        super(razonSocial, direccion, telefono, correo, estado);
        this.id = id;
        this.ruc = ruc;
        this.rubro = rubro;
        this.contactoNombre = contactoNombre;
        this.plazoEntregaDias = plazoEntregaDias;
        this.condicionPago = condicionPago;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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


    public List<Compra> getCompras() {
        return compras;
    }

    public void setCompras(List<Compra> compras) {
        this.compras = compras;
    }
}
