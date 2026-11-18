using SigmetaWeb.Components.Shared;

namespace SigmetaWeb.Components.Pages.Ventas;

// PATRÓN UX · Reglas del flujo comercial vistas desde la pantalla: en qué situación está cada documento,
// qué le falta y QUIÉN lo hace. Usa solo datos que ya trae el Java; las reglas reales siguen en los BO
// (VentaBOImpl, ComprobanteBOImpl, DespachoBOImpl). Si una regla del BO cambia, se ajusta aquí también.
//
// Orden del flujo de una venta (H16: el despacho exige comprobante y, al contado, el cobro en caja):
//   Venta -> Comprobante (Vendedor o Cajero) -> Cobro en caja si es al contado (Cajero) -> Despacho (Almacenero)
public static class FlujoVentas
{
    // ---------------- Venta ----------------

    public const string SinComprobante = "SIN_COMPROBANTE";
    public const string PorCobrar = "POR_COBRAR";
    public const string PorDespachar = "POR_DESPACHAR";
    public const string Completa = "COMPLETA";
    public const string Anulada = "ANULADA";

    public static bool EstaAnulada(VentaDatos v) => v.Anulado || v.Estado == "ANULADA";

    // Lo primero que le falta a la venta
    public static string Situacion(VentaDatos v) =>
        EstaAnulada(v) ? Anulada
        : v.Comprobante is null ? SinComprobante
        : v.CondicionPago == "CONTADO" && !v.CobradaContado ? PorCobrar
        : v.Estado != "DESPACHADA" ? PorDespachar
        : Completa;

    public static string TextoSituacion(string situacion) => situacion switch
    {
        SinComprobante => "Sin comprobante",
        PorCobrar => "Por cobrar",
        PorDespachar => "Por despachar",
        Completa => "Completa",
        Anulada => "Anulada",
        _ => situacion
    };

    // Tono del chip de filtro (mismo estándar de colores que <Estado>)
    public static string TonoSituacion(string situacion) => situacion switch
    {
        SinComprobante => "ambar",
        PorCobrar => "ambar",
        PorDespachar => "azul",
        Completa => "verde",
        Anulada => "rojo",
        _ => ""
    };

    // Qué sigue y quién lo hace (columna "Siguiente paso" de la lista)
    public static (string Que, string? Quien) Siguiente(VentaDatos v) => Situacion(v) switch
    {
        SinComprobante => ("Emitir comprobante", "Vendedor o Cajero"),
        PorCobrar => ("Cobrar en caja", "Cajero"),
        PorDespachar => (v.Estado == "DESPACHADA_PARCIAL" ? "Completar el despacho" : "Despachar", "Almacenero"),
        Completa => ("Nada pendiente", null),
        _ => ("Anulada", null)
    };

    // Una venta ya tiene mercadería entregada (entonces su comprobante no se anula: va nota de crédito)
    public static bool TieneDespacho(VentaDatos v) =>
        v.Estado is "DESPACHADA" or "DESPACHADA_PARCIAL" || v.Lineas.Any(l => l.CantidadDespachada > 0);

    // Motivo por el que el Vendedor no puede modificar la venta (null = sí puede). Mismas reglas que VentaBOImpl.modificar.
    public static string? MotivoNoEditable(VentaDatos v) =>
        EstaAnulada(v) ? "La venta está anulada."
        : v.Comprobante is not null ? $"Tiene el comprobante {v.Comprobante}: anúlelo primero para corregir la venta."
        : TieneDespacho(v) ? "Ya salió mercadería del almacén."
        : v.CobradaContado ? "Ya se cobró en caja: si hay un error, anúlela y registre otra."
        : null;

    // Motivo por el que la venta no se puede anular (null = sí). Mismas reglas que VentaBOImpl.anular.
    public static string? MotivoNoAnulable(VentaDatos v) =>
        EstaAnulada(v) ? "Ya está anulada."
        : v.Comprobante is not null ? $"Tiene el comprobante {v.Comprobante}: anúlelo primero."
        : TieneDespacho(v) ? "Tiene despachos vigentes."
        : null;

    // Recorrido de la venta. "orden" es opcional: si se tiene, se muestra también su cotización.
    public static List<PasoDocumento> PasosVenta(VentaDatos v, OrdenDatos? orden = null)
    {
        var pasos = new List<PasoDocumento>();
        if (orden is not null)
        {
            pasos.Add(new("Cotización", EstadoPaso.Hecho, orden.NumeroCotizacion,
                Ruta: orden.IdCotizacion > 0 ? $"/cotizaciones/editar/{orden.IdCotizacion}" : null));
            pasos.Add(new("Orden del cliente", EstadoPaso.Hecho, orden.Numero, Ruta: $"/ordenes/editar/{orden.Id}"));
        }
        else if (v.IdOrden > 0)
        {
            pasos.Add(new("Orden del cliente", EstadoPaso.Hecho, v.NumeroOrden, Ruta: $"/ordenes/editar/{v.IdOrden}"));
        }
        else
        {
            pasos.Add(new("Cotización y orden", EstadoPaso.Omitido, "Venta directa"));
        }

        if (EstaAnulada(v))
        {
            pasos.Add(new("Venta", EstadoPaso.Anulado, $"{v.Numero} · anulada"));
            return pasos;
        }
        pasos.Add(new("Venta", EstadoPaso.Hecho, v.Numero, Ruta: $"/ventas/editar/{v.Id}"));

        var conComprobante = v.Comprobante is not null;
        pasos.Add(new("Comprobante", conComprobante ? EstadoPaso.Hecho : EstadoPaso.Actual,
            conComprobante ? v.Comprobante : null, "Vendedor o Cajero", $"/ventas/{v.Id}/comprobante"));

        bool cobroListo;
        if (v.CondicionPago == "CREDITO")
        {
            // al crédito el cobro no frena el despacho: se cobra después en Cuentas por cobrar
            pasos.Add(new("Cobro", EstadoPaso.Omitido, "Al crédito"));
            cobroListo = true;
        }
        else
        {
            cobroListo = v.CobradaContado;
            pasos.Add(new("Cobro en caja",
                v.CobradaContado ? EstadoPaso.Hecho : conComprobante ? EstadoPaso.Actual : EstadoPaso.Pendiente,
                null, "Cajero"));
        }

        var despachada = v.Estado == "DESPACHADA";
        var parcial = v.Estado == "DESPACHADA_PARCIAL";
        var puedeDespacharse = conComprobante && cobroListo;
        pasos.Add(new("Despacho",
            despachada ? EstadoPaso.Hecho : puedeDespacharse ? EstadoPaso.Actual : EstadoPaso.Pendiente,
            parcial ? "Parcial" : null, "Almacenero"));
        return pasos;
    }

    // ---------------- Orden de compra del cliente ----------------

    public static List<PasoDocumento> PasosOrden(OrdenDatos o)
    {
        var pasos = new List<PasoDocumento>
        {
            new("Cotización", EstadoPaso.Hecho, o.NumeroCotizacion,
                Ruta: o.IdCotizacion > 0 ? $"/cotizaciones/editar/{o.IdCotizacion}" : null)
        };
        if (o.Anulado || o.Estado == "ANULADA")
        {
            pasos.Add(new("Orden del cliente", EstadoPaso.Anulado, $"{o.Numero} · anulada"));
            return pasos;
        }
        pasos.Add(new("Orden del cliente", EstadoPaso.Hecho, o.Numero));
        pasos.Add(new("Venta", o.Estado == "PENDIENTE" ? EstadoPaso.Actual : EstadoPaso.Hecho, null, "Vendedor"));
        pasos.Add(new("Entrega al cliente",
            o.Estado == "ATENDIDA" ? EstadoPaso.Hecho : o.Estado == "ATENDIDA_PARCIAL" ? EstadoPaso.Actual : EstadoPaso.Pendiente,
            o.Estado == "ATENDIDA_PARCIAL" ? "Parcial" : null, "Almacenero"));
        return pasos;
    }

    // ---------------- Cotización ----------------

    // "orden": la orden que salió de la cotización, si ya existe
    public static List<PasoDocumento> PasosCotizacion(CotizacionDatos c, OrdenDatos? orden = null)
    {
        var estado = c.Anulado ? "ANULADA" : c.Estado ?? "REGISTRADA";
        // Títulos cortos: el paso hecho dice lo que pasó ("Enviada") y el que toca, lo que hay que hacer ("Enviar")
        var pasos = new List<PasoDocumento>
        {
            new("Registrada", EstadoPaso.Hecho)
        };
        if (estado == "ANULADA")
        {
            pasos.Add(new("Anulada", EstadoPaso.Anulado));
            return pasos;
        }
        if (estado == "VENCIDA")
        {
            // el Java no guarda si venció estando registrada o enviada: solo se dice que venció
            pasos.Add(new("Vencida", EstadoPaso.Anulado, TextosVentas.Fecha(c.FechaVigencia)));
            return pasos;
        }
        var enviada = estado is "ENVIADA" or "ACEPTADA" or "RECHAZADA";
        pasos.Add(new(enviada ? "Enviada" : "Enviar", enviada ? EstadoPaso.Hecho : EstadoPaso.Actual, Responsable: "Vendedor"));
        pasos.Add(new(estado switch { "ACEPTADA" => "Aceptada", "RECHAZADA" => "Rechazada", _ => "Respuesta del cliente" },
            estado == "ACEPTADA" ? EstadoPaso.Hecho
            : estado == "RECHAZADA" ? EstadoPaso.Anulado
            : estado == "ENVIADA" ? EstadoPaso.Actual : EstadoPaso.Pendiente,
            Responsable: "Vendedor"));
        if (estado == "RECHAZADA")
        {
            return pasos;
        }
        pasos.Add(new(orden is not null ? "Orden" : "Generar orden",
            orden is not null ? EstadoPaso.Hecho : estado == "ACEPTADA" ? EstadoPaso.Actual : EstadoPaso.Pendiente,
            orden?.Numero, "Vendedor",
            orden is not null ? $"/ordenes/editar/{orden.Id}" : null));
        return pasos;
    }
}
