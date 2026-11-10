package pe.edu.pucp.sigmeta.ws.cobranza;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.pucp.sigmeta.bo.cobranza.CobroBO;
import pe.edu.pucp.sigmeta.bo.cobranza.CuentaPorCobrarBO;
import pe.edu.pucp.sigmeta.boimpl.cobranza.CobroBOImpl;
import pe.edu.pucp.sigmeta.boimpl.cobranza.CuentaPorCobrarBOImpl;
import pe.edu.pucp.sigmeta.model.cobranza.Cobro;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF009: consultas de cuentas por cobrar (Cajero y Administrador). No hay POST: la cuenta la crea la venta al credito.
//
//   GET /api/cuentas-por-cobrar                                   todas, con su saldo
//   GET /api/cuentas-por-cobrar?comprobante=F001-00000123         por comprobante (buscarPorComprobante)
//   GET /api/cuentas-por-cobrar?cliente=3                         por cliente (listarPorCliente)
//   GET /api/cuentas-por-cobrar?desde=2026-10-01&hasta=2026-10-31&porVencimiento=false   por rango de fechas
//   GET /api/cuentas-por-cobrar?estado=PENDIENTE                  por estado (listarPorEstado)
//   GET /api/cuentas-por-cobrar?vencidas=true                     las vencidas (listarVencidas)
//   GET /api/cuentas-por-cobrar/resumen                           total de saldos pendientes y vencidos (Administrador)
//   GET /api/cuentas-por-cobrar/{id}                              una cuenta con sus cobros
//
// Si llegan varios filtros se usa el primero de la lista de arriba. Las busquedas las hace el BO.
@Path("cuentas-por-cobrar")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CuentaPorCobrarRS extends VentanillaBase {

    private CuentaPorCobrarBO cuentaBO = new CuentaPorCobrarBOImpl();
    private CobroBO cobroBO = new CobroBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("comprobante") String comprobante,
                           @QueryParam("cliente") Integer idCliente,
                           @QueryParam("desde") String desde,
                           @QueryParam("hasta") String hasta,
                           @QueryParam("porVencimiento") Boolean porVencimiento,
                           @QueryParam("estado") String estado,
                           @QueryParam("vencidas") Boolean vencidas) {
        return ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            List<CuentaPorCobrar> cuentas;
            if (comprobante != null && !comprobante.isBlank()) {
                cuentas = cuentaBO.buscarPorComprobante(comprobante.trim());
            } else if (idCliente != null) {
                cuentas = cuentaBO.listarPorCliente(idCliente);
            } else if ((desde != null && !desde.isBlank()) || (hasta != null && !hasta.isBlank())) {
                LocalDate inicio = Utilidades.leerFecha(desde, "fecha desde");
                LocalDate fin = Utilidades.leerFecha(hasta, "fecha hasta");
                cuentas = cuentaBO.buscarPorRangoFechas(inicio, fin, Boolean.TRUE.equals(porVencimiento));
            } else if (estado != null && !estado.isBlank()) {
                cuentas = cuentaBO.listarPorEstado(Utilidades.leerEnum(EstadoCuentaPorCobrar.class, estado, "estado"));
            } else if (Boolean.TRUE.equals(vencidas)) {
                cuentas = cuentaBO.listarVencidas();
            } else {
                cuentas = cuentaBO.listarTodasConDetalle();
            }
            List<CuentaDatos> lista = new ArrayList<>();
            for (CuentaPorCobrar cxc : cuentas) {
                lista.add(convertir(cxc));
            }
            return lista;
        });
    }

    @GET
    @Path("resumen")
    public Response resumen(@HeaderParam("ticket") String ticket) {
        // el BO verifica que sea el Administrador
        return ejecutar(ticket, id -> cuentaBO.obtenerResumen(id));
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idCuenta) {
        Response respuesta = ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            CuentaPorCobrar cxc = cuentaBO.obtenerPorId(idCuenta);
            if (cxc == null) {
                return null;
            }
            CuentaDatos datos = convertir(cxc);
            datos.cobros = new ArrayList<>();
            for (Cobro cobro : cobroBO.listarPorCuentaPorCobrar(idCuenta)) {
                datos.cobros.add(CobroRS.convertir(cobro, datos));
            }
            return datos;
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe la cuenta por cobrar con id " + idCuenta);
        }
        return respuesta;
    }

    static CuentaDatos convertir(CuentaPorCobrar cxc) {
        CuentaDatos d = new CuentaDatos();
        d.id = cxc.getId();
        if (cxc.getVenta() != null) {
            d.idVenta = cxc.getVenta().getId();
            d.venta = cxc.getVenta().getNumero();
            List<Comprobante> comprobantes = cxc.getVenta().getComprobantes();
            if (comprobantes != null && !comprobantes.isEmpty()) {
                d.comprobante = CuentaPorCobrarBOImpl.numeroComprobante(comprobantes.get(0));
            }
        }
        if (cxc.getCliente() != null) {
            d.idCliente = cxc.getCliente().getId();
            d.cliente = cxc.getCliente().getNombreMostrado();
            d.documentoCliente = cxc.getCliente().getNumeroDocumento();
        }
        d.fechaEmision = Utilidades.fecha(cxc.getFechaEmision());
        d.fechaVencimiento = Utilidades.fecha(cxc.getFechaVencimiento());
        d.montoOriginal = cxc.getMontoOriginal();
        d.montoPagado = cxc.getMontoPagado();
        d.saldoPendiente = cxc.getSaldoPendiente();
        d.estado = cxc.getEstado() != null ? cxc.getEstado().name() : null;
        d.vencida = cxc.isVencida();
        return d;
    }
}
