namespace SigmetaWeb.Components.Shared;

// Tipo de la ventana de mensaje (MensajeModal): define el ícono, el título por defecto y el color.
public enum TipoMensaje
{
    Exito,   // la acción se completó
    Error,   // la acción no se pudo completar (mensaje del BO o del servidor)
    Aviso    // información que el usuario debe leer
}
