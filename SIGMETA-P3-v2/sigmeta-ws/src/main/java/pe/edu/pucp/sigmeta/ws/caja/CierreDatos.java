package pe.edu.pucp.sigmeta.ws.caja;

// RF010: un cierre de caja. Para cerrar o corregir Blazor manda {"montoDeclarado": 1250.50};
// para reabrir manda {"motivo": "Falto registrar un cobro"}.
public class CierreDatos {

    public int id;
    public int idCaja;
    public String fechaCierre;
    public double montoCalculado;
    public double montoDeclarado;
    public double diferencia;
    public String usuarioCierre;
    public String estado;
    public String fechaReapertura;
    public String usuarioReapertura;
    public String motivoReapertura;

    // solo al reabrir
    public String motivo;

    public CierreDatos() {
    }
}
