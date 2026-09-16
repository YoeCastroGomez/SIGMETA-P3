package pe.edu.pucp.ejecucion;

import java.time.LocalDateTime;
import pe.edu.pucp.model.enums.CondicionPago;
import pe.edu.pucp.model.enums.EstadoAutorizacion;
import pe.edu.pucp.model.enums.TipoDocumentoIdentidad;
import pe.edu.pucp.model.enums.TipoRol;
import pe.edu.pucp.model.seguridad.SolicitudAutorizacion;
import pe.edu.pucp.model.socio.Cliente;
import pe.edu.pucp.model.socio.Proveedor;
import pe.edu.pucp.model.usuario.Rol;
import pe.edu.pucp.model.usuario.Usuario;

public class PruebaSeguridad {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 9, 1, 8, 0);

    public static DatosMaestros ejecutar() {
        System.out.println("== PRUEBA 1: seguridad y socios ==");

        Rol rolVendedor = crearRol(1, TipoRol.VENDEDOR, "Atencion al cliente y ciclo comercial");
        Rol rolCajero = crearRol(2, TipoRol.CAJERO, "Cobros y manejo de efectivo");
        Rol rolAlmacenero = crearRol(3, TipoRol.ALMACENERO, "Recepcion y despacho de mercaderia");
        Rol rolAdmin = crearRol(4, TipoRol.ADMINISTRADOR, "Supervision y configuracion");

        Usuario vendedor = crearUsuario(1, "lrojas", "Lucia", "Rojas Vega",
                "lrojas@grupometa.pe", rolVendedor);
        crearUsuario(2, "mchavez", "Marco", "Chavez Rios",
                "mchavez@grupometa.pe", rolCajero);
        crearUsuario(3, "jtorres", "Jorge", "Torres Pena",
                "jtorres@grupometa.pe", rolAlmacenero);
        Usuario administrador = crearUsuario(4, "aquispe", "Ana", "Quispe Mendoza",
                "aquispe@grupometa.pe", rolAdmin);

        System.out.println("Roles y usuarios registrados:");
        for (Rol rol : new Rol[]{rolVendedor, rolCajero, rolAlmacenero, rolAdmin}) {
            for (Usuario u : rol.getUsuarios()) {
                System.out.println("  " + u.getNombreUsuario() + " (" + u.getNombres() + " "
                        + u.getApellidos() + ") con rol " + u.getRol().getTipo());
            }
        }

        Cliente clienteContado = new Cliente();
        clienteContado.setId(1);
        clienteContado.setRazonSocial("Pedro Alberto Ramos Diaz");
        clienteContado.setDireccion("Av. Los Angeles 240, Lurigancho-Chosica");
        clienteContado.setTelefono("987654321");
        clienteContado.setCorreo("pramos@gmail.com");
        clienteContado.setEstado(true);
        clienteContado.setTipoDocumento(TipoDocumentoIdentidad.DNI);
        clienteContado.setNumeroDocumento("45781290");
        clienteContado.setContactoNombre("Pedro Ramos");
        clienteContado.setCondicionPago(CondicionPago.CONTADO);
        clienteContado.setPlazoCreditoDias(0);
        clienteContado.setLimiteCredito(0.0);
        clienteContado.setCalificacionCrediticia("SIN LINEA");

        Cliente clienteCredito = new Cliente();
        clienteCredito.setId(2);
        clienteCredito.setRazonSocial("Constructora Andina del Sur S.A.C.");
        clienteCredito.setDireccion("Jr. Huaraz 1180, Cercado de Lima");
        clienteCredito.setTelefono("014785522");
        clienteCredito.setCorreo("compras@andinadelsur.com.pe");
        clienteCredito.setEstado(true);
        clienteCredito.setTipoDocumento(TipoDocumentoIdentidad.RUC);
        clienteCredito.setNumeroDocumento("20548963217");
        clienteCredito.setContactoNombre("Rosa Linares");
        clienteCredito.setCondicionPago(CondicionPago.CREDITO);
        clienteCredito.setPlazoCreditoDias(30);
        clienteCredito.setLimiteCredito(25000.00);
        clienteCredito.setCalificacionCrediticia("A");

        System.out.println("Cliente al contado: " + clienteContado.getRazonSocial()
                + " (" + clienteContado.getTipoDocumento() + " "
                + clienteContado.getNumeroDocumento() + ")");

        System.out.println("Cliente al credito: " + clienteCredito.getRazonSocial()
                + " (" + clienteCredito.getTipoDocumento() + " "
                + clienteCredito.getNumeroDocumento() + "), limite S/ "
                + clienteCredito.getLimiteCredito() + " a "
                + clienteCredito.getPlazoCreditoDias() + " dias");

        Proveedor proveedorPrincipal = new Proveedor();
        proveedorPrincipal.setId(1);
        proveedorPrincipal.setRazonSocial("Importaciones Electricas Delta S.A.C.");
        proveedorPrincipal.setDireccion("Av. Argentina 3450, Callao");
        proveedorPrincipal.setTelefono("014520011");
        proveedorPrincipal.setCorreo("ventas@delta.com.pe");
        proveedorPrincipal.setEstado(true);
        proveedorPrincipal.setRuc("20431298765");
        proveedorPrincipal.setRubro("Material electrico industrial");
        proveedorPrincipal.setContactoNombre("Cesar Bravo");
        proveedorPrincipal.setPlazoEntregaDias(5);
        proveedorPrincipal.setCondicionPago(CondicionPago.CREDITO);

        Proveedor proveedorSecundario = new Proveedor();
        proveedorSecundario.setId(2);
        proveedorSecundario.setRazonSocial("Ferreteria Industrial Huachipa E.I.R.L.");
        proveedorSecundario.setDireccion("Carretera Central km 9, Lurigancho-Chosica");
        proveedorSecundario.setTelefono("013710044");
        proveedorSecundario.setCorreo("contacto@fihuachipa.pe");
        proveedorSecundario.setEstado(true);
        proveedorSecundario.setRuc("20512346789");
        proveedorSecundario.setRubro("Fijaciones y abrasivos");
        proveedorSecundario.setContactoNombre("Nelly Fuentes");
        proveedorSecundario.setPlazoEntregaDias(2);
        proveedorSecundario.setCondicionPago(CondicionPago.CONTADO);

        System.out.println("Proveedores: " + proveedorPrincipal.getRazonSocial()
                + " (entrega en " + proveedorPrincipal.getPlazoEntregaDias() + " dias) y "
                + proveedorSecundario.getRazonSocial()
                + " (entrega en " + proveedorSecundario.getPlazoEntregaDias() + " dias)");

        SolicitudAutorizacion solicitud = new SolicitudAutorizacion();
        solicitud.setId(1);
        solicitud.setSolicitante(vendedor);
        solicitud.setAdministrador(administrador);
        solicitud.setOperacionRestringida("VENTA_SOBRE_LIMITE_CREDITO");
        solicitud.setMotivo("El cliente supera su linea de credito en la orden del mes");
        solicitud.setFechaSolicitud(INICIO.plusHours(1));
        solicitud.setEstado(EstadoAutorizacion.APROBADA);
        solicitud.setFechaResolucion(INICIO.plusHours(1).plusMinutes(12));
        solicitud.setVigenciaMinutos(60);
        solicitud.setFechaVencimiento(INICIO.plusHours(1).plusMinutes(72));

        System.out.println("Solicitud " + solicitud.getOperacionRestringida()
                + " pedida por " + solicitud.getSolicitante().getNombreUsuario()
                + ", resuelta por " + solicitud.getAdministrador().getNombreUsuario()
                + " como " + solicitud.getEstado());

        System.out.println("  vigencia de " + solicitud.getVigenciaMinutos()
                + " minutos, vence el " + solicitud.getFechaVencimiento());

        boolean vigente = solicitud.getEstado() == EstadoAutorizacion.APROBADA
                && solicitud.getFechaVencimiento().isAfter(solicitud.getFechaResolucion());

        System.out.println("  autorizacion utilizable: " + vigente);

        System.out.println();

        return new DatosMaestros(administrador, clienteCredito, proveedorPrincipal);
    }

    private static Rol crearRol(int id, TipoRol tipo, String descripcion) {
        Rol rol = new Rol();
        rol.setId(id);
        rol.setTipo(tipo);
        rol.setDescripcion(descripcion);
        rol.setEstado(true);
        return rol;
    }

    private static Usuario crearUsuario(int id, String nombreUsuario, String nombres,
                                        String apellidos, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setClaveHash("hash_" + nombreUsuario);
        usuario.setSalt("salt_" + nombreUsuario);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreo(correo);
        usuario.setEstado(true);
        usuario.setFechaRegistro(INICIO);
        usuario.setRol(rol);
        rol.getUsuarios().add(usuario);
        return usuario;
    }
}
