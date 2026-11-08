package pe.edu.pucp.sigmeta.ws.seguridad;

// Lo que el Java responde al iniciar sesion. No lleva la clave ni el id del usuario:
// {"ticket": "...", "nombreUsuario": "mchavez", "nombres": "...", "apellidos": "...", "rol": "CAJERO"}
public class DatosSesion {

    private String ticket;
    private String nombreUsuario;
    private String nombres;
    private String apellidos;
    private String rol;

    public DatosSesion() {
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
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

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
