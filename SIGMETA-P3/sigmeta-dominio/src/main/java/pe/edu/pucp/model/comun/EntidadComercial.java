package pe.edu.pucp.model.comun;

/**
 * Superclase de las entidades externas con las que la empresa opera.
 */
public abstract class EntidadComercial {

    private String razonSocial;
    private String direccion;
    private String telefono;
    private String correo;
    private boolean estado;

    public EntidadComercial() {
    }

    public EntidadComercial(String razonSocial, String direccion, String telefono, String correo, boolean estado) {
        this.razonSocial = razonSocial;
        this.direccion = direccion;
        this.telefono = telefono;
        this.correo = correo;
        this.estado = estado;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

}
