namespace SigmetaWeb.Servicios;

// RF001: iniciar y cerrar sesión (igual que el IServicioSesionAuth del material del curso).
public interface IServicioSesionAuth
{
    Task IniciarSesionAsync(HttpContext context);
    Task CerrarSesionAsync(HttpContext context);
}
