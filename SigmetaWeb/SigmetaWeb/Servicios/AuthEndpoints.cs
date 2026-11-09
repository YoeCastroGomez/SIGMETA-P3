namespace SigmetaWeb.Servicios;

// Direcciones del login y el logout (igual que en el material del curso).
// El formulario de Login.razor hace POST a /auth/login; el botón Cerrar sesión va a /auth/logout.
public static class AuthEndpoints
{
    public static IEndpointRouteBuilder MapAuthEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapPost("/auth/login", (HttpContext context, IServicioSesionAuth servicioSesionAuth) =>
            servicioSesionAuth.IniciarSesionAsync(context));

        endpoints.MapGet("/auth/logout", (HttpContext context, IServicioSesionAuth servicioSesionAuth) =>
            servicioSesionAuth.CerrarSesionAsync(context));

        return endpoints;
    }
}
