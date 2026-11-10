package pe.edu.pucp.sigmeta.ws.caja;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.caja.CierreCajaBO;
import pe.edu.pucp.sigmeta.boimpl.caja.CierreCajaBOImpl;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;

// RF010: historial de cierres y lo que solo hace el Administrador (corregir y reabrir).
//
//   GET  /api/cierres                                  todos los cierres (vigentes y revertidos)
//   GET  /api/cierres?desde=2026-11-01&hasta=2026-11-09  busqueda por rango de fechas (buscarPorRangoFechas)
//   GET  /api/cierres/{id}                             un cierre
//   PUT  /api/cierres/{id}                             corregir el monto declarado: {"montoDeclarado": 1240} (Administrador)
//   POST /api/cierres/{id}/reabrir                     reabrir la caja: {"motivo": "..."} (Administrador)
//
// El cierre de la caja (lo hace el Cajero) esta en POST /api/cajas/{id}/cerrar.
@Path("cierres")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CierreRS extends VentanillaBase {

    private CierreCajaBO cierreCajaBO = new CierreCajaBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("desde") String desde,
                           @QueryParam("hasta") String hasta) {
        return ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            LocalDate inicio = Utilidades.leerFecha(desde, "fecha desde");
            LocalDate fin = Utilidades.leerFecha(hasta, "fecha hasta");
            List<CierreCaja> cierres;
            if (inicio != null || fin != null) {
                if (inicio == null || fin == null) {
                    throw new IllegalArgumentException("Indique las dos fechas del rango (desde y hasta)");
                }
                // todo el dia: desde las 00:00 del inicio hasta las 23:59:59 del fin
                cierres = cierreCajaBO.buscarPorRangoFechas(inicio.atStartOfDay(), fin.atTime(23, 59, 59));
            } else {
                cierres = cierreCajaBO.listarTodosConDetalle();
            }
            List<CierreDatos> lista = new ArrayList<>();
            for (CierreCaja c : cierres) {
                lista.add(convertir(c));
            }
            return lista;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idCierre) {
        Response respuesta = ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            CierreCaja cierre = cierreCajaBO.obtenerPorId(idCierre);
            return cierre == null ? null : convertir(cierre);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe el cierre con id " + idCierre);
        }
        return respuesta;
    }

    @PUT
    @Path("{id}")
    public Response corregir(@HeaderParam("ticket") String ticket, @PathParam("id") int idCierre, CierreDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Falta el monto declarado");
        }
        return ejecutar(ticket, id -> {
            CierreCaja cierre = new CierreCaja();
            cierre.setId(idCierre);
            cierre.setMontoDeclarado(datos.montoDeclarado);
            return convertir(cierreCajaBO.modificar(cierre, id));
        });
    }

    @POST
    @Path("{id}/reabrir")
    public Response reabrir(@HeaderParam("ticket") String ticket, @PathParam("id") int idCierre, CierreDatos datos) {
        String motivo = datos != null ? datos.motivo : null;
        return ejecutar(ticket, id -> convertir(cierreCajaBO.reabrir(idCierre, motivo, id)));
    }

    static CierreDatos convertir(CierreCaja c) {
        CierreDatos d = new CierreDatos();
        d.id = c.getId();
        d.idCaja = c.getCaja() != null ? c.getCaja().getId() : 0;
        d.fechaCierre = Utilidades.fechaHora(c.getFechaCierre());
        d.montoCalculado = c.getMontoCalculado();
        d.montoDeclarado = c.getMontoDeclarado();
        d.diferencia = c.getDiferencia();
        d.usuarioCierre = Utilidades.usuario(c.getUsuarioCierre());
        d.estado = c.getEstado() != null ? c.getEstado().name() : null;
        d.fechaReapertura = Utilidades.fechaHora(c.getFechaReapertura());
        d.usuarioReapertura = Utilidades.usuario(c.getUsuarioReapertura());
        d.motivoReapertura = c.getMotivoReapertura();
        return d;
    }
}
