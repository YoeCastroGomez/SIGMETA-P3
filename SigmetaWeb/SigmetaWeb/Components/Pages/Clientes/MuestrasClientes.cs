namespace SigmetaWeb.Components.Pages.Clientes;

// Datos de ejemplo fijos (Lab 9: sin BD ni servicios). Se reemplazan al integrar con Java.
// Los nombres de los campos son los mismos que en la clase Cliente del proyecto Java.
public record ClienteFila(int Id, string TipoPersona, string? RazonSocial, string? Nombres, string? Apellidos,
                          string TipoDocumento, string NumeroDocumento,
                          string? Direccion, string? Telefono, string? Correo, string? NombreContacto,
                          string CondicionPago, decimal LimiteCredito, int PlazoCreditoDias, string CalificacionCrediticia,
                          bool Activo)
{
    // Texto que se muestra en la tabla: razón social o nombres y apellidos.
    public string NombreMostrado => TipoPersona == "JURIDICA" ? RazonSocial ?? "" : $"{Nombres} {Apellidos}";
}

public static class MuestrasClientes
{
    public static readonly ClienteFila[] Clientes =
    {
        new(1, "JURIDICA", "Constructora Andina S.A.C.", null, null, "RUC", "20512345678",
            "Av. Nicolás Ayllón 4520, Ate", "01 356 7788", "compras@candina.pe", "Ing. Raúl Medina",
            "CREDITO", 25000m, 30, "A", true),
        new(2, "JURIDICA", "Metalmecánica Chosica E.I.R.L.", null, null, "RUC", "20601122334",
            "Carretera Central km 33, Chosica", "987 654 321", "ventas@metchosica.pe", "Sra. Elena Paredes",
            "CREDITO", 8000m, 15, "B", true),
        new(3, "NATURAL", null, "Pedro", "Huamaní Flores", "DNI", "45678912",
            "Jr. Los Pinos 120, Lurigancho", "945 112 233", null, null,
            "CONTADO", 0m, 0, "SIN_EVALUAR", true),
        new(4, "JURIDICA", "Instalaciones Eléctricas del Sur S.A.", null, null, "RUC", "20455566778",
            "Av. Los Héroes 980, San Juan de Miraflores", "01 450 1122", null, "Sr. Víctor Lazo",
            "CONTADO", 0m, 0, "C", false),
        new(5, "NATURAL", null, "Andrea", "Morales Pérez", "CARNET_EXTRANJERIA", "001234567",
            "Calle Las Begonias 45, Chaclacayo", "912 345 678", "amorales@correo.com", null,
            "CONTADO", 0m, 0, "SIN_EVALUAR", true),
    };

    public static string Texto(string valor) => valor switch
    {
        "JURIDICA" => "Empresa", "NATURAL" => "Persona natural",
        "CARNET_EXTRANJERIA" => "Carné de extranjería", "PASAPORTE" => "Pasaporte",
        "CONTADO" => "Contado", "CREDITO" => "Crédito",
        "SIN_EVALUAR" => "Sin evaluar", "A" => "A · riesgo bajo", "B" => "B · riesgo medio", "C" => "C · riesgo alto",
        _ => valor
    };
}
