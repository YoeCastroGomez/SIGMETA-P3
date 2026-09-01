package pe.edu.pucp.model.comun;

import java.time.LocalDateTime;

/**
 * Contrato de los documentos que admiten anulacion logica (RNF002).
 */
public interface Anulable {

    boolean isAnulado();
    void setAnulado(boolean anulado);
    String getMotivoAnulacion();
    void setMotivoAnulacion(String motivoAnulacion);
    LocalDateTime getFechaAnulacion();
    void setFechaAnulacion(LocalDateTime fechaAnulacion);
}
