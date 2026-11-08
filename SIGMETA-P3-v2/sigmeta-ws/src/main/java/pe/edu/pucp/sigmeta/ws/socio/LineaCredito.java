package pe.edu.pucp.sigmeta.ws.socio;

// Lo que Blazor manda para asignar una linea de credito:
// {"limiteCredito": 25000, "plazoCreditoDias": 30, "calificacionCrediticia": "A"}
public class LineaCredito {

    private double limiteCredito;
    private int plazoCreditoDias;
    private String calificacionCrediticia;

    public LineaCredito() {
    }

    public double getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(double limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public int getPlazoCreditoDias() {
        return plazoCreditoDias;
    }

    public void setPlazoCreditoDias(int plazoCreditoDias) {
        this.plazoCreditoDias = plazoCreditoDias;
    }

    public String getCalificacionCrediticia() {
        return calificacionCrediticia;
    }

    public void setCalificacionCrediticia(String calificacionCrediticia) {
        this.calificacionCrediticia = calificacionCrediticia;
    }
}
