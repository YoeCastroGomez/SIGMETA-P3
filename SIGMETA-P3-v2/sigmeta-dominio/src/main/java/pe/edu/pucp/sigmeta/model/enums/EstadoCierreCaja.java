package pe.edu.pucp.sigmeta.model.enums;

/**
 * Estado de un cierre de caja. El cierre nunca se elimina: al reabrir la caja queda REVERTIDO (RF010).
 */
public enum EstadoCierreCaja {
    VIGENTE,
    REVERTIDO
}
