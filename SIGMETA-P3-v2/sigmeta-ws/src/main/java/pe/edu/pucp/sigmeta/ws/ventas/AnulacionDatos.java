package pe.edu.pucp.sigmeta.ws.ventas;

// RF007 y RF008: motivo de anulacion. Blazor manda {"motivo": "El cliente desistio de la compra"}.
// Se usa POST .../anular (y no DELETE) porque el BO exige el motivo y un DELETE no lleva cuerpo.
public class AnulacionDatos {

    public String motivo;

    public AnulacionDatos() {
    }
}
