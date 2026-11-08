package pe.edu.pucp.sigmeta.ws.seguridad;

// Lo que Blazor manda al iniciar sesion: {"nombreUsuario": "mchavez", "clave": "Meta2026"}
public class Credenciales {

    private String nombreUsuario;
    private String clave;

    public Credenciales() {
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}
