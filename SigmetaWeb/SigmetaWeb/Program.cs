using Microsoft.AspNetCore.Authentication.Cookies;
using SigmetaWeb.Components;
using SigmetaWeb.Servicios;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddRazorComponents()
    .AddInteractiveServerComponents();

// Conexión con el Java (sigmeta-ws). La dirección está en appsettings.json -> ApiJava:BaseUrl
builder.Services.AddHttpClient("ApiJava", cliente =>
{
    cliente.BaseAddress = new Uri(builder.Configuration["ApiJava:BaseUrl"]!);
});
builder.Services.AddScoped<ApiJava>();

// RF001: autenticación con cookie. La cookie guarda el usuario, su rol y el ticket que dio el Java.
// Dura 8 horas y se renueva mientras se use (SlidingExpiration), igual que la sesión del Java.
builder.Services.AddAuthentication(CookieAuthenticationDefaults.AuthenticationScheme)
    .AddCookie(opciones =>
    {
        opciones.LoginPath = "/login";
        opciones.ExpireTimeSpan = TimeSpan.FromHours(8);
        opciones.SlidingExpiration = true;
    });
builder.Services.AddAuthorization();
builder.Services.AddCascadingAuthenticationState();
builder.Services.AddHttpContextAccessor();
builder.Services.AddScoped<IServicioSesionAuth, ServicioSesionAuth>();

var app = builder.Build();

// Fechas y montos en formato peruano (S/ 1,234.50 y 08/11/2026)
string[] supportedCultures = ["es-PE"];
var localizationOptions = new RequestLocalizationOptions()
    .SetDefaultCulture(supportedCultures[0])
    .AddSupportedCultures(supportedCultures)
    .AddSupportedUICultures(supportedCultures);
app.UseRequestLocalization(localizationOptions);

// Configure the HTTP request pipeline.
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error", createScopeForErrors: true);
    // The default HSTS value is 30 days. You may want to change this for production scenarios, see https://aka.ms/aspnetcore-hsts.
    app.UseHsts();
}
app.UseStatusCodePagesWithReExecute("/not-found", createScopeForStatusCodePages: true);
app.UseHttpsRedirection();

app.UseAuthentication();
app.UseAuthorization();

app.UseAntiforgery();

app.MapStaticAssets();
app.MapAuthEndpoints();   // /auth/login y /auth/logout
app.MapRazorComponents<App>()
    .AddInteractiveServerRenderMode();

app.Run();
