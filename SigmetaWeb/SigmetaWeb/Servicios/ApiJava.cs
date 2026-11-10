using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.AspNetCore.Components;
using Microsoft.AspNetCore.Components.Authorization;

namespace SigmetaWeb.Servicios;

// Servicio común para hablar con el Java (sigmeta-ws). Se registra en Program.cs y se inyecta:
//
//     @inject ApiJava Api
//     clientes = await Api.Get<List<Cliente>>("clientes");                 // GET
//     var nuevo = await Api.Post<Cliente>("clientes", cliente);            // POST
//     var editado = await Api.Put<Cliente>($"clientes/{Id}", cliente);     // PUT
//     await Api.Delete($"clientes/{id}");                                  // DELETE
//
// - Manda el ticket del usuario conectado en el encabezado "ticket" (sale de la cookie de sesión).
// - Si el Java responde un error, lanza ErrorApi con el mensaje del BO (por ejemplo
//   "Ya existe un cliente con el documento ..."). La pantalla lo atrapa y lo muestra con <AvisoError>.
// - Si la sesión del Java venció (401), cierra la sesión y vuelve al login.
public class ApiJava
{
    private readonly HttpClient http;
    private readonly AuthenticationStateProvider estadoAuth;
    private readonly NavigationManager nav;

    public ApiJava(IHttpClientFactory fabrica, AuthenticationStateProvider estadoAuth, NavigationManager nav)
    {
        http = fabrica.CreateClient("ApiJava");
        this.estadoAuth = estadoAuth;
        this.nav = nav;
    }

    public async Task<T?> Get<T>(string ruta)
    {
        var respuesta = await Enviar(HttpMethod.Get, ruta, null);
        return await Leer<T>(respuesta);
    }

    public async Task<T?> Post<T>(string ruta, object? cuerpo)
    {
        var respuesta = await Enviar(HttpMethod.Post, ruta, cuerpo);
        return await Leer<T>(respuesta);
    }

    public async Task<T?> Put<T>(string ruta, object? cuerpo)
    {
        var respuesta = await Enviar(HttpMethod.Put, ruta, cuerpo);
        return await Leer<T>(respuesta);
    }

    public async Task<T?> Delete<T>(string ruta)
    {
        var respuesta = await Enviar(HttpMethod.Delete, ruta, null);
        return await Leer<T>(respuesta);
    }

    public async Task Delete(string ruta)
    {
        await Enviar(HttpMethod.Delete, ruta, null);
    }

    private async Task<HttpResponseMessage> Enviar(HttpMethod metodo, string ruta, object? cuerpo)
    {
        var pedido = new HttpRequestMessage(metodo, ruta);

        // el ticket guardado al iniciar sesión (claim "ticket" de la cookie)
        var estado = await estadoAuth.GetAuthenticationStateAsync();
        var ticket = estado.User.FindFirst(ServicioSesionAuth.ClaimTicket)?.Value;
        if (ticket is not null)
        {
            pedido.Headers.Add("ticket", ticket);
        }
        if (cuerpo is not null)
        {
            pedido.Content = JsonContent.Create(cuerpo);
        }

        HttpResponseMessage respuesta;
        try
        {
            respuesta = await http.SendAsync(pedido);
        }
        catch (HttpRequestException)
        {
            throw new ErrorApi("No se pudo conectar con el servidor Java. Revise que GlassFish esté encendido y sigmeta-ws desplegado.", 0);
        }

        if (respuesta.IsSuccessStatusCode)
        {
            return respuesta;
        }

        var mensaje = await LeerMensaje(respuesta);
        if (respuesta.StatusCode == HttpStatusCode.Unauthorized)
        {
            // el ticket ya no sirve: se cierra la sesión y se vuelve al login mostrando el motivo
            nav.NavigateTo("/auth/logout?motivo=" + Uri.EscapeDataString(mensaje), forceLoad: true);
        }
        throw new ErrorApi(mensaje, (int)respuesta.StatusCode);
    }

    private static async Task<T?> Leer<T>(HttpResponseMessage respuesta)
    {
        if (respuesta.StatusCode == HttpStatusCode.NoContent)
        {
            return default;
        }
        return await respuesta.Content.ReadFromJsonAsync<T>();
    }

    // el Java manda los errores como {"mensaje": "..."} (clase Mensaje del Java)
    public static async Task<string> LeerMensaje(HttpResponseMessage respuesta)
    {
        try
        {
            var error = await respuesta.Content.ReadFromJsonAsync<RespuestaError>();
            if (!string.IsNullOrWhiteSpace(error?.Mensaje))
            {
                return error.Mensaje;
            }
        }
        catch (JsonException)
        {
            // la respuesta no era JSON (por ejemplo una página de error de GlassFish)
        }
        return $"El servidor respondió con error {(int)respuesta.StatusCode}";
    }

    private class RespuestaError
    {
        public string? Mensaje { get; set; }
    }
}

// Error que devuelve el Java. Message es el texto del BO y Estado el código HTTP (400, 401, 403, 409...).
public class ErrorApi : Exception
{
    public int Estado { get; }

    public ErrorApi(string mensaje, int estado) : base(mensaje)
    {
        Estado = estado;
    }
}
