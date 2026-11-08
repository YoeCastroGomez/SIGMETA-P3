package pe.edu.pucp.sigmeta.ws.base;

// Lo que responde el servicio cuando hay un error. En JSON sale asi:
// {"mensaje": "Ya existe un cliente con el documento 20548963217"}
// Blazor muestra ese texto en el aviso rojo.
public class Mensaje {

    private String mensaje;

    public Mensaje() {
    }

    public Mensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
