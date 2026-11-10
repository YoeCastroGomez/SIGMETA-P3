package pe.edu.pucp.sigmeta.ws.cobranza;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.cobranza.CobroBO;
import pe.edu.pucp.sigmeta.bo.cobranza.CuentaPorCobrarBO;
import pe.edu.pucp.sigmeta.boimpl.cobranza.CobroBOImpl;
import pe.edu.pucp.sigmeta.boimpl.cobranza.CuentaPorCobrarBOImpl;
import pe.edu.pucp.sigmeta.model.cobranza.Cobro;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;
import pe.edu.pucp.sigmeta.ws.caja.Utilidades;

// RF009: cobros sobre las cuentas por cobrar.
//
//   GET    /api/cobros                 todos los cobros (Cajero o Administrador)
//   GET    /api/cobros?cuenta=2        cobros de una cuenta
//   GET    /api/cobros?cliente=3       cobros de un cliente
//   GET    /api/cobros?desde=2026-11-01&hasta=2026-11-09   cobros por rango de fechas de cobro
//   GET    /api/cobros/{id}            un cobro
//   POST   /api/cobros                 registrar (Cajero con su caja abierta): baja el saldo y entra el ingreso a la caja
//   PUT    /api/cobros/{id}            modificar fecha y referencia (Cajero o Administrador)
//   DELETE /api/cobros/{id}            anular (Cajero o Administrador): el saldo vuelve a la cuenta y la caja registra un egreso
@Path("cobros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CobroRS extends VentanillaBase {

    private CobroBO cobroBO = new CobroBOImpl();
    private CuentaPorCobrarBO cuentaBO = new CuentaPorCobrarBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket,
                           @QueryParam("cuenta") Integer idCuenta,
                           @QueryParam("cliente") Integer idCliente,
                           @QueryParam("desde") String desde,
                           @QueryParam("hasta") String hasta) {
        return ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            List<Cobro> cobros;
            if (idCuenta != null) {
                cobros = cobroBO.listarPorCuentaPorCobrar(idCuenta);
            } else if (idCliente != null) {
                cobros = cobroBO.listarPorCliente(idCliente);
            } else if ((desde != null && !desde.isBlank()) || (hasta != null && !hasta.isBlank())) {
                cobros = cobroBO.buscarPorRangoFechas(Utilidades.leerFecha(desde, "fecha desde"),
                        Utilidades.leerFecha(hasta, "fecha hasta"));
            } else {
                cobros = cobroBO.listarTodos();
            }
            Map<Integer, CuentaDatos> cuentas = cuentas();
            List<CobroDatos> lista = new ArrayList<>();
            for (Cobro c : cobros) {
                lista.add(convertir(c, c.getCuentaPorCobrar() != null ? cuentas.get(c.getCuentaPorCobrar().getId()) : null));
            }
            return lista;
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idCobro) {
        Response respuesta = ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            Cobro cobro = cobroBO.obtenerPorId(idCobro);
            if (cobro == null) {
                return null;
            }
            CuentaPorCobrar cxc = cobro.getCuentaPorCobrar() != null
                    ? cuentaBO.obtenerPorId(cobro.getCuentaPorCobrar().getId()) : null;
            return convertir(cobro, cxc != null ? CuentaPorCobrarRS.convertir(cxc) : null);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe el cobro con id " + idCobro);
        }
        return respuesta;
    }

    @POST
    public Response registrar(@HeaderParam("ticket") String ticket, CobroDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del cobro");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            Cobro cobro = new Cobro();
            CuentaPorCobrar cxc = new CuentaPorCobrar();
            cxc.setId(datos.idCuentaPorCobrar);
            cobro.setCuentaPorCobrar(cxc);
            cobro.setMonto(datos.monto);
            cobro.setMedioPago(Utilidades.leerEnum(MedioPago.class, datos.medioPago, "medio de pago"));
            cobro.setFechaCobro(Utilidades.leerFecha(datos.fechaCobro, "fecha del cobro"));
            cobro.setReferencia(datos.referencia);
            return convertir(cobroBO.registrarCobro(cobro, id), null);
        });
    }

    @PUT
    @Path("{id}")
    public Response modificar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCobro, CobroDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del cobro");
        }
        return ejecutar(ticket, id -> {
            Cobro actual = cobroBO.obtenerPorId(idCobro);
            if (actual == null) {
                throw new IllegalArgumentException("No existe el cobro con id " + idCobro);
            }
            // solo cambian la fecha y la referencia; el monto y el medio de pago se mandan como estan
            Cobro cobro = new Cobro();
            cobro.setId(idCobro);
            cobro.setCuentaPorCobrar(actual.getCuentaPorCobrar());
            cobro.setMonto(actual.getMonto());
            cobro.setMedioPago(actual.getMedioPago());
            cobro.setFechaCobro(Utilidades.leerFecha(datos.fechaCobro, "fecha del cobro"));
            cobro.setReferencia(datos.referencia);
            return convertir(cobroBO.modificar(cobro, id), null);
        });
    }

    @DELETE
    @Path("{id}")
    public Response anular(@HeaderParam("ticket") String ticket, @PathParam("id") int idCobro) {
        return ejecutar(ticket, id -> {
            Cobro cobro = new Cobro();
            cobro.setId(idCobro);
            cobroBO.eliminar(cobro, id);
            return null;
        });
    }

    private Map<Integer, CuentaDatos> cuentas() throws SQLException {
        Map<Integer, CuentaDatos> mapa = new HashMap<>();
        for (CuentaPorCobrar cxc : cuentaBO.listarTodasConDetalle()) {
            mapa.put(cxc.getId(), CuentaPorCobrarRS.convertir(cxc));
        }
        return mapa;
    }

    static CobroDatos convertir(Cobro c, CuentaDatos cuenta) {
        CobroDatos d = new CobroDatos();
        d.id = c.getId();
        d.idCuentaPorCobrar = c.getCuentaPorCobrar() != null ? c.getCuentaPorCobrar().getId() : 0;
        d.fechaCobro = Utilidades.fecha(c.getFechaCobro());
        d.medioPago = c.getMedioPago() != null ? c.getMedioPago().name() : null;
        d.monto = c.getMonto();
        d.referencia = c.getReferencia();
        d.usuarioRegistro = Utilidades.usuario(c.getUsuarioRegistro());
        d.fechaRegistro = Utilidades.fechaHora(c.getFechaRegistro());
        d.anulado = c.isAnulado();
        d.fechaAnulacion = Utilidades.fechaHora(c.getFechaAnulacion());
        if (cuenta != null) {
            d.venta = cuenta.venta;
            d.comprobante = cuenta.comprobante;
            d.cliente = cuenta.cliente;
        }
        return d;
    }
}
