using System.Net.Http.Json;
using System.Security.Claims;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;

namespace SigmetaWeb.Servicios;

// RF001: el login de Blazor le pregunta al Java si el usuario y la clave son correctos
// (POST /api/sesion -> iniciarSesion del BO). Si lo son, guarda en la cookie:
//   - el nombre de usuario, sus nombres y apellidos
//   - el rol (Administrador, Vendedor, Cajero o Almacenero), que usan el menú y AuthorizeView
//   - el ticket del Java, que ApiJava manda en cada pedido
// Nunca se guarda el id del usuario: el Java lo saca del ticket.
public class ServicioSesionAuth : IServicioSesionAuth
{
    public const string ClaimTicket = "ticket";

    private readonly HttpClient http;

    public ServicioSesionAuth(IHttpClientFactory fabrica)
    {
        http = fabrica.CreateClient("ApiJava");
    }

    public async Task IniciarSesionAsync(HttpContext context)
    {
        var formulario = await context.Request.ReadFormAsync();
        var credenciales = new Credenciales
        {
            NombreUsuario = formulario["usuario"].ToString(),
            Clave = formulario["clave"].ToString(),
        };

        HttpResponseMessage respuesta;
        try
        {
            respuesta = await http.PostAsJsonAsync("sesion", credenciales);
        }
        catch (HttpRequestException)
        {
            VolverAlLogin(context, "No se pudo conectar con el servidor Java. Revise que GlassFish esté encendido.");
            return;
        }

        if (!respuesta.IsSuccessStatusCode)
        {
            // "Usuario o clave incorrectos", "El usuario se encuentra desactivado"...
            VolverAlLogin(context, await ApiJava.LeerMensaje(respuesta));
            return;
        }

        var datos = await respuesta.Content.ReadFromJsonAsync<DatosSesion>();
        var claims = new List<Claim>
        {
            new(ClaimTypes.Name, datos!.NombreUsuario),
            new(ClaimTypes.GivenName, datos.Nombres),
            new(ClaimTypes.Surname, datos.Apellidos),
            new(ClaimTypes.Role, NombreRol(datos.Rol)),
            new(ClaimTicket, datos.Ticket!),
        };
        var identidad = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
        await context.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, new ClaimsPrincipal(identidad));
        context.Response.Redirect("/");
    }

    public async Task CerrarSesionAsync(HttpContext context)
    {
        var ticket = context.User.FindFirst(ClaimTicket)?.Value;
        if (ticket is not null)
        {
            try
            {
                // DELETE /api/sesion: el Java cierra su sesión y el ticket deja de servir
                var pedido = new HttpRequestMessage(HttpMethod.Delete, "sesion");
                pedido.Headers.Add("ticket", ticket);
                await http.SendAsync(pedido);
            }
            catch (HttpRequestException)
            {
                // si el Java no responde, igual se cierra la sesión en Blazor
            }
        }
        await context.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
        // si la sesión se cerró sola (el Java respondió 401), se muestra el motivo en el login
        var motivo = context.Request.Query["motivo"].ToString();
        if (string.IsNullOrWhiteSpace(motivo))
        {
            context.Response.Redirect("/login");
        }
        else
        {
            VolverAlLogin(context, motivo);
        }
    }

    private static void VolverAlLogin(HttpContext context, string mensaje)
    {
        context.Response.Redirect("/login?error=" + Uri.EscapeDataString(mensaje));
    }

    // el Java manda el rol en mayúsculas; las pantallas usan Administrador, Vendedor, Cajero, Almacenero
    private static string NombreRol(string rol) => rol switch
    {
        "ADMINISTRADOR" => "Administrador",
        "VENDEDOR" => "Vendedor",
        "CAJERO" => "Cajero",
        "ALMACENERO" => "Almacenero",
        _ => rol,
    };
}

// Lo que se manda a POST /api/sesion (clase Credenciales del Java)
public class Credenciales
{
    public string NombreUsuario { get; set; } = "";
    public string Clave { get; set; } = "";
}

// Lo que responde POST /api/sesion (clase DatosSesion del Java)
public class DatosSesion
{
    public string? Ticket { get; set; }
    public string NombreUsuario { get; set; } = "";
    public string Nombres { get; set; } = "";
    public string Apellidos { get; set; } = "";
    public string Rol { get; set; } = "";
}
