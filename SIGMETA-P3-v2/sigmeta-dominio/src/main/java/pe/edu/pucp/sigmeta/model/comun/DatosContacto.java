package pe.edu.pucp.sigmeta.model.comun;

/**
 * Datos de contacto de un cliente o proveedor. Se usa por composicion: un cliente persona natural
 * y una empresa comparten como contactarlos, pero no la razon social.
 */
public class DatosContacto {

    private String direccion;
    private String telefono;
    private String correo;

    public DatosContacto() {
    }

    public DatosContacto(String direccion, String telefono, String correo) {
        this.direccion = direccion;
        this.telefono = telefono;
        this.correo = correo;
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
    private String nombreContacto;
    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

}
