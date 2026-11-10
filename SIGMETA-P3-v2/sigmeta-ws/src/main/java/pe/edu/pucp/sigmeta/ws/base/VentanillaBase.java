package pe.edu.pucp.sigmeta.ws.base;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import pe.edu.pucp.sigmeta.seguridad.SesionServicio;

// Clase comun de todos los servicios REST (ClienteRS, ProductoRS, VentaRS...).
// Hace tres cosas para que cada servicio no las repita:
//   1. Con el ticket busca la sesion del usuario conectado (si no hay, responde 401).
//   2. Llama al BO como ese usuario: "id" es el usuario de la sesion, no un dato que manda Blazor.
//   3. Arma la Response: 200 con el resultado, o el error del BO con su mensaje.
//
// Uso en un servicio:
//     return ejecutar(ticket, id -> clienteBO.registrar(cliente, id));
//     return ejecutar(ticket, id -> { clienteBO.desactivar(idCliente, id); return null; });
public abstract class VentanillaBase {

    // el pedido HTTP actual: los GET solo consultan y se ejecutan sin el bloqueo global (mas rapido)
    @Context
    private Request pedido;

    protected Response ejecutar(String ticket, SesionServicio.Accion<?> accion) {
        return ejecutar(ticket, Response.Status.OK, accion);
    }

    // estadoOk: el codigo cuando todo sale bien (por ejemplo CREATED al registrar)
    protected Response ejecutar(String ticket, Response.Status estadoOk, SesionServicio.Accion<?> accion) {
        SesionServicio sesion = Sesiones.buscar(ticket);
        if (sesion == null) {
            return error(Response.Status.UNAUTHORIZED, "Su sesion no existe o ya vencio. Vuelva a iniciar sesion");
        }
        try {
            boolean soloConsulta = pedido != null && HttpMethod.GET.equals(pedido.getMethod());
            Object resultado = soloConsulta ? sesion.consultar(accion) : sesion.ejecutar(accion);
            if (resultado == null) {
                return Response.noContent().build(); // 204 sin cuerpo
            }
            return Response.status(estadoOk).entity(resultado).build();
        } catch (IllegalArgumentException e) {
            // dato invalido segun el BO
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            // el BO no permite la operacion: rol sin permiso (RF001) o estado que no corresponde
            if (e.getMessage() != null && e.getMessage().startsWith("El rol ")) {
                return error(Response.Status.FORBIDDEN, e.getMessage());
            }
            return error(Response.Status.CONFLICT, e.getMessage());
        } catch (SecurityException e) {
            // 401 solo si la sesion de verdad vencio: Blazor manda al login con 401.
            // Cualquier otro problema de seguridad (usuario inactivo, responsable distinto...) es 403
            // y se muestra como error, sin expulsar al usuario.
            if (!sesion.estaVigente()) {
                return error(Response.Status.UNAUTHORIZED, e.getMessage());
            }
            return error(Response.Status.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error en el servidor: " + e.getMessage());
        }
    }

    protected Response error(Response.Status estado, String mensaje) {
        return Response.status(estado).entity(new Mensaje(mensaje)).build();
    }
}
