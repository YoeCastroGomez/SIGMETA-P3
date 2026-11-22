package pe.edu.pucp.sigmeta.model.enums;

/**
 * Estado de una solicitud de autorizacion temporal (RF014).
 */
public enum EstadoAutorizacion {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    // paso su vigencia o el Administrador la revoco sin usarse
    VENCIDA,
    // se consumio en la operacion que autorizaba (un solo uso)
    USADA
}
