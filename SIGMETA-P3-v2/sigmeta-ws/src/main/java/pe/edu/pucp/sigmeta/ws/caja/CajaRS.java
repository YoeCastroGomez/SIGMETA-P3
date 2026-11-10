package pe.edu.pucp.sigmeta.ws.caja;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import pe.edu.pucp.sigmeta.bo.caja.CajaBO;
import pe.edu.pucp.sigmeta.bo.caja.MovimientoCajaBO;
import pe.edu.pucp.sigmeta.boimpl.caja.CajaBOImpl;
import pe.edu.pucp.sigmeta.boimpl.caja.CompensacionCaja;
import pe.edu.pucp.sigmeta.boimpl.caja.MovimientoCajaBOImpl;
import pe.edu.pucp.sigmeta.model.caja.Caja;
import pe.edu.pucp.sigmeta.model.caja.CierreCaja;
import pe.edu.pucp.sigmeta.model.caja.MovimientoCaja;
import pe.edu.pucp.sigmeta.model.enums.MedioPago;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.ws.base.VentanillaBase;

// RF010: caja del Cajero (apertura, movimientos, cobro de ventas al contado y cierre).
//
//   GET    /api/cajas                              lista de cajas (Cajero o Administrador)
//   GET    /api/cajas/abierta                      la caja abierta del usuario conectado con su resumen (204 si no tiene)
//   GET    /api/cajas/{id}                         una caja con su resumen de la jornada
//   POST   /api/cajas                              abrir caja: {"montoInicial": 500}
//   GET    /api/cajas/{id}/movimientos             movimientos de la caja
//   POST   /api/cajas/{id}/movimientos             ingreso o egreso manual
//   PUT    /api/cajas/{id}/movimientos/{idMov}     modificar un ingreso o egreso manual
//   DELETE /api/cajas/{id}/movimientos/{idMov}     eliminar un ingreso o egreso manual
//   GET    /api/cajas/ventas-por-cobrar            ventas al contado que aun no se cobran
//   POST   /api/cajas/{id}/cobros-contado          cobrar una venta al contado: {"idVenta": 4, "medioPago": "EFECTIVO"}
//   POST   /api/cajas/{id}/cerrar                  cerrar la caja: {"montoDeclarado": 1250.50}
//
// El cajero es siempre el usuario de la sesion (ticket); Blazor nunca manda su id.
// Las reglas (monto negativo, caja ya abierta, otro cajero, caja cerrada...) y sus mensajes son del BO.
@Path("cajas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CajaRS extends VentanillaBase {

    private CajaBO cajaBO = new CajaBOImpl();
    private MovimientoCajaBO movimientoCajaBO = new MovimientoCajaBOImpl();

    @GET
    public Response listar(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            List<CajaDatos> lista = new ArrayList<>();
            for (Caja caja : cajaBO.listarTodasConUsuario()) {
                lista.add(convertir(caja));
            }
            return lista;
        });
    }

    @GET
    @Path("abierta")
    public Response obtenerAbierta(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            Caja caja = cajaBO.obtenerCajaAbiertaPorUsuario(id);
            return caja == null ? null : conResumen(caja); // null = 204: el cajero no tiene caja abierta
        });
    }

    @GET
    @Path("{id}")
    public Response obtener(@HeaderParam("ticket") String ticket, @PathParam("id") int idCaja) {
        Response respuesta = ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            Caja caja = cajaBO.obtenerPorId(idCaja);
            return caja == null ? null : conResumen(caja);
        });
        if (respuesta.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return error(Response.Status.NOT_FOUND, "No existe la caja con id " + idCaja);
        }
        return respuesta;
    }

    @POST
    public Response abrir(@HeaderParam("ticket") String ticket, CajaDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Falta el monto inicial de la caja");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            Caja caja = new Caja();
            caja.setMontoInicial(datos.montoInicial);
            return conResumen(cajaBO.abrirCaja(caja, id));
        });
    }

    @GET
    @Path("{id}/movimientos")
    public Response listarMovimientos(@HeaderParam("ticket") String ticket, @PathParam("id") int idCaja) {
        return ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            List<MovimientoDatos> lista = new ArrayList<>();
            for (MovimientoCaja m : cajaBO.listarMovimientosPorCaja(idCaja)) {
                lista.add(convertir(m));
            }
            return lista;
        });
    }

    @POST
    @Path("{id}/movimientos")
    public Response registrarMovimiento(@HeaderParam("ticket") String ticket, @PathParam("id") int idCaja,
                                        MovimientoDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del movimiento");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            MovimientoCaja movimiento = aMovimiento(datos, idCaja);
            return convertir(movimientoCajaBO.registrarMovimiento(movimiento, id));
        });
    }

    @PUT
    @Path("{id}/movimientos/{idMovimiento}")
    public Response modificarMovimiento(@HeaderParam("ticket") String ticket, @PathParam("id") int idCaja,
                                        @PathParam("idMovimiento") int idMovimiento, MovimientoDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del movimiento");
        }
        return ejecutar(ticket, id -> {
            MovimientoCaja movimiento = aMovimiento(datos, idCaja);
            movimiento.setId(idMovimiento);
            return convertir(movimientoCajaBO.modificar(movimiento, id));
        });
    }

    @DELETE
    @Path("{id}/movimientos/{idMovimiento}")
    public Response eliminarMovimiento(@HeaderParam("ticket") String ticket, @PathParam("id") int idCaja,
                                       @PathParam("idMovimiento") int idMovimiento) {
        return ejecutar(ticket, id -> {
            MovimientoCaja movimiento = new MovimientoCaja();
            movimiento.setId(idMovimiento);
            movimientoCajaBO.eliminar(movimiento, id);
            return null;
        });
    }

    @GET
    @Path("ventas-por-cobrar")
    public Response listarVentasPorCobrar(@HeaderParam("ticket") String ticket) {
        return ejecutar(ticket, id -> {
            Utilidades.permitirCajeroOAdministrador(id);
            List<VentaPorCobrarDatos> lista = new ArrayList<>();
            for (Venta v : cajaBO.listarVentasPorCobrarAlContado()) {
                VentaPorCobrarDatos d = new VentaPorCobrarDatos();
                d.id = v.getId();
                d.numero = v.getNumero();
                d.fechaEmision = Utilidades.fecha(v.getFechaEmision());
                if (v.getCliente() != null) {
                    d.cliente = v.getCliente().getNombreMostrado();
                    d.documentoCliente = v.getCliente().getNumeroDocumento();
                }
                d.comprobante = comprobante(v);
                d.total = v.getTotal();
                lista.add(d);
            }
            return lista;
        });
    }

    @POST
    @Path("{id}/cobros-contado")
    public Response cobrarVentaContado(@HeaderParam("ticket") String ticket, @PathParam("id") int idCaja,
                                       CobroContadoDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Faltan los datos del cobro");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            MovimientoCaja movimiento = new MovimientoCaja();
            Caja caja = new Caja();
            caja.setId(idCaja);
            movimiento.setCaja(caja);
            movimiento.setTipo(TipoMovimientoCaja.COBRO_CONTADO);
            movimiento.setMedioPago(Utilidades.leerEnum(MedioPago.class, datos.medioPago, "medio de pago"));
            movimiento.setConcepto("Cobro al contado de la venta " + datos.idVenta);
            movimiento.setDocumentoOrigen(CompensacionCaja.DOCUMENTO_VENTA);
            movimiento.setIdDocumentoOrigen(datos.idVenta);
            // el monto lo pone el BO con el total de la venta
            return convertir(movimientoCajaBO.registrarMovimiento(movimiento, id));
        });
    }

    @POST
    @Path("{id}/cerrar")
    public Response cerrar(@HeaderParam("ticket") String ticket, @PathParam("id") int idCaja, CierreDatos datos) {
        if (datos == null) {
            return error(Response.Status.BAD_REQUEST, "Falta el monto declarado");
        }
        return ejecutar(ticket, Response.Status.CREATED, id -> {
            CierreCaja cierre = new CierreCaja();
            Caja caja = new Caja();
            caja.setId(idCaja);
            cierre.setCaja(caja);
            cierre.setMontoDeclarado(datos.montoDeclarado);
            return CierreRS.convertir(cajaBO.cerrarCaja(cierre, id));
        });
    }

    // ----- conversiones entre las clases del modelo y lo que viaja en JSON -----

    private MovimientoCaja aMovimiento(MovimientoDatos datos, int idCaja) {
        MovimientoCaja movimiento = new MovimientoCaja();
        Caja caja = new Caja();
        caja.setId(idCaja);
        movimiento.setCaja(caja);
        movimiento.setTipo(Utilidades.leerEnum(TipoMovimientoCaja.class, datos.tipo, "tipo de movimiento"));
        movimiento.setMedioPago(Utilidades.leerEnum(MedioPago.class, datos.medioPago, "medio de pago"));
        movimiento.setMonto(datos.monto);
        movimiento.setConcepto(datos.concepto);
        return movimiento;
    }

    static CajaDatos convertir(Caja caja) {
        CajaDatos d = new CajaDatos();
        d.id = caja.getId();
        d.fechaApertura = Utilidades.fechaHora(caja.getFechaApertura());
        d.montoInicial = caja.getMontoInicial();
        d.usuarioApertura = Utilidades.usuario(caja.getUsuarioApertura());
        d.abierta = caja.isAbierta();
        return d;
    }

    // RF010: resumen de la jornada con los movimientos de la caja
    private CajaDatos conResumen(Caja caja) throws SQLException {
        CajaDatos d = convertir(caja);
        Map<MedioPago, CajaDatos.TotalMedioPago> porMedio = new EnumMap<>(MedioPago.class);
        for (MovimientoCaja m : cajaBO.listarMovimientosPorCaja(caja.getId())) {
            boolean egreso = m.getTipo() == TipoMovimientoCaja.EGRESO_MANUAL;
            switch (m.getTipo()) {
                case COBRO_CONTADO -> d.totalCobrosContado += m.getMonto();
                case COBRO_CREDITO -> d.totalCobrosCredito += m.getMonto();
                case INGRESO_MANUAL -> d.totalIngresosManuales += m.getMonto();
                case EGRESO_MANUAL -> {
                    if (m.getDocumentoOrigen() == null) {
                        d.totalEgresosManuales += m.getMonto();
                    } else {
                        d.totalDevoluciones += m.getMonto();
                    }
                }
            }
            CajaDatos.TotalMedioPago t = porMedio.computeIfAbsent(m.getMedioPago(), k -> {
                CajaDatos.TotalMedioPago nuevo = new CajaDatos.TotalMedioPago();
                nuevo.medioPago = k.name();
                return nuevo;
            });
            if (egreso) {
                t.egresos += m.getMonto();
            } else {
                t.ingresos += m.getMonto();
            }
            t.neto = t.ingresos - t.egresos;
        }
        d.porMedioPago = new ArrayList<>(porMedio.values());
        d.montoEsperado = cajaBO.calcularMontoCalculado(caja.getId());
        return d;
    }

    static MovimientoDatos convertir(MovimientoCaja m) {
        MovimientoDatos d = new MovimientoDatos();
        d.id = m.getId();
        d.idCaja = m.getCaja() != null ? m.getCaja().getId() : 0;
        d.tipo = m.getTipo() != null ? m.getTipo().name() : null;
        d.fechaMovimiento = Utilidades.fechaHora(m.getFechaMovimiento());
        d.medioPago = m.getMedioPago() != null ? m.getMedioPago().name() : null;
        d.monto = m.getMonto();
        d.concepto = m.getConcepto();
        d.documentoOrigen = m.getDocumentoOrigen();
        d.idDocumentoOrigen = m.getIdDocumentoOrigen();
        d.usuarioRegistro = Utilidades.usuario(m.getUsuarioRegistro());
        d.manual = (m.getTipo() == TipoMovimientoCaja.INGRESO_MANUAL || m.getTipo() == TipoMovimientoCaja.EGRESO_MANUAL)
                && m.getDocumentoOrigen() == null;
        return d;
    }

    private static String comprobante(Venta venta) {
        if (venta.getComprobantes() == null || venta.getComprobantes().isEmpty()) {
            return null;
        }
        Comprobante c = venta.getComprobantes().get(0);
        return c.getSerie() + "-" + c.getNumero();
    }
}
