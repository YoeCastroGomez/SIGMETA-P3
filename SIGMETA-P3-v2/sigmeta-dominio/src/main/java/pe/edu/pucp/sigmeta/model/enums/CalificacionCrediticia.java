package pe.edu.pucp.sigmeta.model.enums;

/** Escala interna: A riesgo bajo, B medio, C alto. No equivale a una calificacion externa. */
public enum CalificacionCrediticia {
    SIN_EVALUAR, A, B, C;
    public static CalificacionCrediticia desdeTexto(String texto) {
        if (texto == null || texto.isBlank() || texto.trim().equalsIgnoreCase("SIN LINEA")) return SIN_EVALUAR;
        try { return valueOf(texto.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Calificacion permitida: A, B, C o SIN_EVALUAR"); }
    }
}
