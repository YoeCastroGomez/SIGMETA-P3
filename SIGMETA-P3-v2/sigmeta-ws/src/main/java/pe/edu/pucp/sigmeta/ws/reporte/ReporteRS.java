package pe.edu.pucp.sigmeta.ws.reporte;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.edu.pucp.sigmeta.bo.reporte.ReporteBO;
import pe.edu.pucp.sigmeta.boimpl.reporte.ReporteBOImpl;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF015: servicio REST de los reportes del Administrador. Los tres piden el periodo (AAAA-MM-DD).
//
//   GET /api/reportes/comercial?desde=2026-10-01&hasta=2026-10-31       ventas y credito
//   GET /api/reportes/abastecimiento?desde=...&hasta=...                compras e inventario
//   GET /api/reportes/caja?desde=...&hasta=...                          cierres, medios de pago y cajeros
//
// Cada respuesta es un objeto con sus bloques (totales y tablas). Las sumas vienen hechas de la base.
@Path("reportes")
@Produces(MediaType.APPLICATION_JSON)
public class ReporteRS extends VentanillaBase {

    private final ReporteBO reporteBO = new ReporteBOImpl();

    @GET
    @Path("comercial")
    public Response comercial(@HeaderParam("ticket") String ticket, @QueryParam("desde") String desde,
                              @QueryParam("hasta") String hasta) {
        return ejecutar(ticket, id -> reporteBO.comercial(Utilidades.leerFecha(desde, "fecha de inicio"),
                Utilidades.leerFecha(hasta, "fecha de fin"), id));
    }

    @GET
    @Path("abastecimiento")
    public Response abastecimiento(@HeaderParam("ticket") String ticket, @QueryParam("desde") String desde,
                                   @QueryParam("hasta") String hasta) {
        return ejecutar(ticket, id -> reporteBO.abastecimiento(Utilidades.leerFecha(desde, "fecha de inicio"),
                Utilidades.leerFecha(hasta, "fecha de fin"), id));
    }

    @GET
    @Path("caja")
    public Response caja(@HeaderParam("ticket") String ticket, @QueryParam("desde") String desde,
                         @QueryParam("hasta") String hasta) {
        return ejecutar(ticket, id -> reporteBO.caja(Utilidades.leerFecha(desde, "fecha de inicio"),
                Utilidades.leerFecha(hasta, "fecha de fin"), id));
    }

    // RF015: el reporte en PDF, generado con JasperReports (sesion 25).
    //   GET /api/reportes/comercial/pdf?desde=2026-10-01&hasta=2026-10-31
    //   GET /api/reportes/abastecimiento/pdf?desde=...&hasta=...
    //   GET /api/reportes/caja/pdf?desde=...&hasta=...
    // Responde application/pdf con Content-Disposition (el navegador lo descarga con su nombre).
    // Si algo falla responde JSON con el mensaje, como el resto de la API.
    @GET
    @Path("{tipo}/pdf")
    @Produces({"application/pdf", MediaType.APPLICATION_JSON})
    public Response pdf(@HeaderParam("ticket") String ticket, @PathParam("tipo") String tipo,
                        @QueryParam("desde") String desde, @QueryParam("hasta") String hasta) {
        Response respuesta = ejecutar(ticket, id -> reporteBO.pdf(tipo, Utilidades.leerFecha(desde, "fecha de inicio"),
                Utilidades.leerFecha(hasta, "fecha de fin"), id));
        if (respuesta.getStatus() != Response.Status.OK.getStatusCode()) {
            return Response.fromResponse(respuesta).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        // el tipo ya lo valido el BO (comercial, abastecimiento o caja) y las fechas ya se leyeron bien
        String archivo = "SIGMETA-reporte-" + tipo.trim().toLowerCase(java.util.Locale.ROOT) + "-" + desde + "-a-" + hasta + ".pdf";
        return Response.fromResponse(respuesta)
                .type("application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + archivo + "\"")
                .header("Cache-Control", "no-store")
                .build();
    }
}
