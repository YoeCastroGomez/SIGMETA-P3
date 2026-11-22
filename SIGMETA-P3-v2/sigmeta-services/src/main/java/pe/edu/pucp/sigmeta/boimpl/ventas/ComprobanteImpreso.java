package pe.edu.pucp.sigmeta.boimpl.ventas;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.socio.ClienteDAO;
import pe.edu.pucp.sigmeta.dao.producto.ProductoDAO;
import pe.edu.pucp.sigmeta.dao.ventas.ComprobanteDAO;
import pe.edu.pucp.sigmeta.dao.ventas.DetalleVentaDAO;
import pe.edu.pucp.sigmeta.dao.ventas.VentaDAO;
import pe.edu.pucp.sigmeta.model.enums.EstadoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoComprobante;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;
import pe.edu.pucp.sigmeta.model.producto.Producto;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.usuario.Usuario;
import pe.edu.pucp.sigmeta.model.ventas.Comprobante;
import pe.edu.pucp.sigmeta.model.ventas.DetalleVenta;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RF008: foto de lo impreso, representacion en PDF y registro del envio del comprobante.
// Parte de ComprobanteBOImpl: la clase publica conserva sus metodos y delega aqui (mismo paquete, no publica).
final class ComprobanteImpreso {

    private final ComprobanteDAO comprobanteDAO;
    private final VentaDAO ventaDAO;
    private final DetalleVentaDAO detalleVentaDAO;
    private final ClienteDAO clienteDAO;
    private final ProductoDAO productoDAO;
    private final UsuarioBO usuarioBO;
    private final pe.edu.pucp.sigmeta.dao.ConsultaDAO consultaDAO;

    ComprobanteImpreso(ComprobanteDAO comprobanteDAO, VentaDAO ventaDAO, DetalleVentaDAO detalleVentaDAO, ClienteDAO clienteDAO, ProductoDAO productoDAO, UsuarioBO usuarioBO, pe.edu.pucp.sigmeta.dao.ConsultaDAO consultaDAO) {
        this.comprobanteDAO = comprobanteDAO;
        this.ventaDAO = ventaDAO;
        this.detalleVentaDAO = detalleVentaDAO;
        this.clienteDAO = clienteDAO;
        this.productoDAO = productoDAO;
        this.usuarioBO = usuarioBO;
        this.consultaDAO = consultaDAO;
    }

    // RF008: completa el comprobante con el cliente tal como se imprimio (no hace nada si no tiene foto)
    void cargarFotoImpresa(Comprobante c) throws SQLException {
        if (c == null || (c.getTipo() != TipoComprobante.FACTURA && c.getTipo() != TipoComprobante.BOLETA)) {
            return;
        }
        List<Map<String, Object>> filas = consultaDAO.consultar("sp_comprobante_impreso", c.getId());
        if (filas.isEmpty() || filas.get(0).get("clienteNombre") == null) {
            return;
        }
        Map<String, Object> f = filas.get(0);
        c.setClienteNombre(texto(f.get("clienteNombre")));
        c.setClienteTipoDocumento(texto(f.get("clienteTipoDocumento")));
        c.setClienteNumeroDocumento(texto(f.get("clienteNumeroDocumento")));
        c.setClienteDireccion(texto(f.get("clienteDireccion")));
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    // RF008: representacion impresa del comprobante en PDF (JasperReports, plantilla reports/comprobante.jrxml).
    // La descargan el Vendedor y el Cajero (quienes entregan el comprobante) y el Administrador (consulta).
    public byte[] pdf(int idComprobante, int idResponsable) throws SQLException {
        Usuario responsable = usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
        Map<String, Object> cabecera = new HashMap<>();
        List<Map<String, ?>> lineas = new java.util.ArrayList<>();
        Transaccion.consultar(() -> {
            Comprobante c = comprobanteDAO.load(idComprobante);
            if (c == null) {
                throw new IllegalArgumentException("No existe el comprobante con id " + idComprobante);
            }
            if (c.getTipo() != TipoComprobante.FACTURA && c.getTipo() != TipoComprobante.BOLETA) {
                throw new IllegalArgumentException("Solo se genera el PDF de una FACTURA o BOLETA");
            }
            if (c.getEstado() == EstadoComprobante.ANULADO) {
                throw new IllegalStateException("El comprobante " + c.getSerie() + "-" + c.getNumero() + " está anulado");
            }
            Venta venta = ventaDAO.load(c.getVenta().getId());
            cargarFotoImpresa(c);
            cabecera.put("TITULO", c.getTipo() == TipoComprobante.FACTURA ? "FACTURA ELECTRÓNICA" : "BOLETA DE VENTA ELECTRÓNICA");
            cabecera.put("NUMERO", c.getSerie() + "-" + c.getNumero());
            cabecera.put("FECHA_EMISION", c.getFechaEmision() == null ? ""
                    : c.getFechaEmision().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            cabecera.put("SUB_TOTAL", c.getSubTotal());
            cabecera.put("IGV", c.getIgv());
            cabecera.put("TOTAL", c.getTotal());
            cabecera.put("VENTA", venta == null ? "" : venta.getNumero());
            cabecera.put("CONDICION_PAGO", venta == null || venta.getCondicionPago() == null ? ""
                    : venta.getCondicionPago() == pe.edu.pucp.sigmeta.model.enums.CondicionPago.CREDITO ? "Crédito" : "Contado");
            // RF008: el PDF sale de la foto tomada al emitir (cliente y lineas), no de los datos de hoy
            List<Map<String, Object>> foto = consultaDAO.consultar("sp_comprobante_impreso_lineas", c.getId());
            if (c.tieneFotoImpresa() && !foto.isEmpty()) {
                cabecera.put("CLIENTE", c.getClienteNombre());
                cabecera.put("TIPO_DOCUMENTO", c.getClienteTipoDocumento() == null ? "Documento" : c.getClienteTipoDocumento());
                cabecera.put("DOCUMENTO", c.getClienteNumeroDocumento());
                cabecera.put("DIRECCION", c.getClienteDireccion() == null ? "—" : c.getClienteDireccion());
                for (Map<String, Object> f : foto) {
                    Map<String, Object> linea = new HashMap<>();
                    linea.put("numeroLinea", ((Number) f.get("numeroLinea")).intValue());
                    linea.put("codigo", texto(f.get("codigo")));
                    linea.put("descripcion", texto(f.get("descripcion")));
                    linea.put("cantidad", ((Number) f.get("cantidad")).doubleValue());
                    linea.put("precioUnitario", ((Number) f.get("precioUnitario")).doubleValue());
                    linea.put("descuento", ((Number) f.get("descuento")).doubleValue());
                    linea.put("importe", ((Number) f.get("importe")).doubleValue());
                    lineas.add(linea);
                }
                return null;
            }
            // comprobante sin foto (emitido antes de la migracion 12 y no completado por ella): datos actuales
            Cliente cliente = venta == null || venta.getCliente() == null ? null : clienteDAO.load(venta.getCliente().getId());
            if (cliente != null) {
                cabecera.put("CLIENTE", cliente.getNombreMostrado());
                cabecera.put("TIPO_DOCUMENTO", cliente.getTipoDocumento() == null ? "Documento" : cliente.getTipoDocumento().name());
                cabecera.put("DOCUMENTO", cliente.getNumeroDocumento());
                cabecera.put("DIRECCION", cliente.getContacto() == null || cliente.getContacto().getDireccion() == null
                        ? "—" : cliente.getContacto().getDireccion());
            }
            if (venta != null) {
                for (DetalleVenta d : detalleVentaDAO.listarPorVenta(venta.getId())) {
                    Producto producto = d.getProducto() == null ? null : productoDAO.load(d.getProducto().getId());
                    Map<String, Object> linea = new HashMap<>();
                    linea.put("numeroLinea", d.getNumeroLinea());
                    linea.put("codigo", producto == null ? "" : producto.getCodigoInterno());
                    linea.put("descripcion", producto == null ? "" : producto.getNombre());
                    linea.put("cantidad", d.getCantidad());
                    linea.put("precioUnitario", d.getPrecioUnitario());
                    linea.put("descuento", d.getDescuento());
                    linea.put("importe", d.getImporte());
                    lineas.add(linea);
                }
            }
            return null;
        });
        String autor = (responsable.getNombres() + " " + responsable.getApellidos()).trim();
        return pe.edu.pucp.sigmeta.boimpl.reporte.ComprobantePdf.generar(cabecera, lineas, autor);
    }

    // RF008: Registro de envio de comprobante electronico
    public Comprobante registrarEnvio(int idComprobante, String medioEnvio, int idResponsable) throws SQLException {
        usuarioBO.verificarRol(idResponsable, TipoRol.VENDEDOR, TipoRol.CAJERO);
        String medio = Validador.textoObligatorio(medioEnvio, "medio de envío", 30).trim().toUpperCase();
        if (!medio.equals("CORREO") && !medio.equals("MENSAJERIA") && !medio.equals("IMPRESO")) {
            throw new IllegalArgumentException("Medio de envío no válido (solo se permite CORREO, MENSAJERIA o IMPRESO): " + medioEnvio);
        }

        return Transaccion.ejecutar(() -> {
            Comprobante comprobante = comprobanteDAO.load(idComprobante);
            if (comprobante == null) {
                throw new IllegalArgumentException("No existe el comprobante con id " + idComprobante);
            }
            if (comprobante.getTipo() != TipoComprobante.FACTURA && comprobante.getTipo() != TipoComprobante.BOLETA) {
                throw new IllegalArgumentException("Solo se puede registrar envío de FACTURA o BOLETA");
            }
            if (comprobante.getEstado() == EstadoComprobante.ANULADO) {
                throw new IllegalStateException("No se puede registrar el envío de un comprobante anulado");
            }
            // RF008: el correo y la mensajeria van a los datos de contacto registrados del cliente
            if (!medio.equals("IMPRESO")) {
                Venta venta = ventaDAO.load(comprobante.getVenta().getId());
                Cliente cliente = venta == null ? null : clienteDAO.load(venta.getCliente().getId());
                pe.edu.pucp.sigmeta.model.comun.DatosContacto contacto = cliente == null ? null : cliente.getContacto();
                String dato = contacto == null ? null
                        : medio.equals("CORREO") ? contacto.getCorreo() : contacto.getTelefono();
                if (dato == null || dato.isBlank()) {
                    throw new IllegalArgumentException(medio.equals("CORREO")
                            ? "El cliente no tiene correo registrado: regístrelo en su ficha o use otro medio de envío"
                            : "El cliente no tiene teléfono de contacto registrado: regístrelo en su ficha o use otro medio de envío");
                }
            }

            LocalDateTime ahora = LocalDateTime.now();
            comprobanteDAO.registrarEnvio(idComprobante, medio, ahora);
            comprobante.setMedioEnvio(medio);
            comprobante.setFechaEnvio(ahora);
            return comprobante;
        });
    }
}
