package pe.edu.pucp.sigmeta.boimpl.compras;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.dao.socio.ProveedorDAO;
import pe.edu.pucp.sigmeta.daoimpl.socio.ProveedorDAOImpl;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;
import pe.edu.pucp.sigmeta.bo.compras.CompraBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.producto.TextoNormalizado;
import pe.edu.pucp.sigmeta.dao.compras.CompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.DetalleCompraDAO;
import pe.edu.pucp.sigmeta.daoimpl.compras.CompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.compras.DetalleCompraDAOImpl;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;

import java.sql.SQLException;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompraBOImpl implements CompraBO {

    private final CompraDAO compraDAO;
    private final DetalleCompraDAO detalleCompraDAO;
    private final UsuarioBO usuarioBO;
    private final ProveedorDAO proveedorDAO;

    public CompraBOImpl() {
        this.compraDAO = new CompraDAOImpl();
        this.detalleCompraDAO = new DetalleCompraDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
        this.proveedorDAO = new ProveedorDAOImpl();
    }

    @Override
    public Compra registrar(Compra compra) throws SQLException {
        validarCompra(compra);
        if (compra.getEstado() != EstadoCompra.REGISTRADA || compra.isAnulado()) {
            throw new IllegalArgumentException("Una compra nueva debe estar REGISTRADA y no anulada");
        }
        validarDetalles(compra.getDetalles());
        CalculoCompra.totales(compra, compra.getDetalles());
        usuarioBO.verificarRol(compra.getUsuarioRegistro().getId(), TipoRol.ADMINISTRADOR);

        return Transaccion.ejecutar(() -> {
            verificarProveedorActivo(compra.getProveedor().getId());
            compra.setNumero(siguienteNumero(compra.getFechaEmision()));
            validarNumeroUnico(compra.getNumero(), 0);
            compraDAO.save(compra);
            for (DetalleCompra detalle : compra.getDetalles()) {
                detalle.setCompra(compra);
                detalleCompraDAO.save(detalle);
            }
            return compra;
        });
    }

    @Override
    public Compra modificar(Compra compra, int idAdministrador) throws SQLException {
        validarCompra(compra);
        validarId(compra.getId());
        if (compra.getEstado() != EstadoCompra.REGISTRADA || compra.isAnulado()) {
            throw new IllegalArgumentException("Solo se pueden editar compras registradas y no anuladas");
        }

        usuarioBO.verificarRol(idAdministrador, TipoRol.ADMINISTRADOR);
        return Transaccion.ejecutar(() -> {
            compraDAO.bloquearCompra(compra.getId());
            Compra existente = compraDAO.load(compra.getId());
            if (existente == null) {
                throw new IllegalArgumentException("No existe la compra con ID " + compra.getId());
            }
            if (existente.isAnulado() || existente.getEstado() != EstadoCompra.REGISTRADA) {
                throw new IllegalArgumentException("La compra ya no se puede modificar");
            }
            if (existente.getProveedor().getId() != compra.getProveedor().getId()) {
                verificarProveedorActivo(compra.getProveedor().getId());
            }
            compra.setNumero(existente.getNumero());
            // Si se envian detalles, reemplazamos las lineas de una compra aun no recibida.
            // Si no se envian, conservamos las lineas existentes (edicion solo de cabecera).
            List<DetalleCompra> anteriores = detalleCompraDAO.listarPorCompra(compra.getId());
            if (anteriores.isEmpty()) {
                throw new IllegalArgumentException("La compra no tiene detalles registrados");
            }
            List<DetalleCompra> nuevas = compra.getDetalles();
            boolean modificarLineas = nuevas != null && !nuevas.isEmpty();
            if (modificarLineas) {
                validarDetalles(nuevas);
                for (DetalleCompra anterior : anteriores) {
                    if (anterior.getCantidadRecibida() > 0) {
                        throw new IllegalStateException("No se pueden modificar detalles con cantidades recibidas");
                    }
                }
            }
            List<DetalleCompra> lineas = modificarLineas ? nuevas : anteriores;
            CalculoCompra.totales(compra, lineas);

            if (modificarLineas) {
                // La transaccion garantiza rollback si falla cualquier detalle o la cabecera.
                // Reemplazamos solo compras REGISTRADAS y sin recepciones (restriccion del SP).
                for (DetalleCompra anterior : anteriores) {
                    detalleCompraDAO.remove(anterior);
                }
                for (DetalleCompra nueva : nuevas) {
                    nueva.setId(0);
                    nueva.setCompra(compra);
                    detalleCompraDAO.save(nueva);
                }
            }
            compraDAO.update(compra);
            compra.setDetalles(lineas);
            return compra;
        });
    }

    @Override
    public void anular(int idCompra, int idAdministrador) throws SQLException {
        validarId(idCompra);
        usuarioBO.verificarRol(idAdministrador, TipoRol.ADMINISTRADOR);
        Transaccion.ejecutar(() -> {
            compraDAO.bloquearCompra(idCompra);
            Compra compra = compraDAO.load(idCompra);
            if (compra == null) {
                throw new IllegalArgumentException("No existe la compra con ID " + idCompra);
            }
            if (compra.isAnulado()) {
                throw new IllegalArgumentException("La compra ya está anulada");
            }
            if (compra.getEstado() != EstadoCompra.REGISTRADA) {
                throw new IllegalStateException("No se puede anular una compra con recepciones registradas");
            }
            compraDAO.remove(compra);
            return null;
        });
    }

    @Override
    public Compra obtener(int idCompra) throws SQLException {
        validarId(idCompra);
        return Transaccion.ejecutar(() -> {
            Compra compra = compraDAO.load(idCompra);
            if (compra != null) {
                compra.setDetalles(detalleCompraDAO.listarPorCompra(idCompra));
            }
            return compra;
        });
    }

    @Override
    public List<Compra> listarTodos() throws SQLException {
        return Transaccion.ejecutar(() -> {
            return compraDAO.listAll();
        });
    }

    @Override
    public List<Compra> buscar(Integer idProveedor, LocalDate fechaInicio, LocalDate fechaFin)
            throws SQLException {
        if (idProveedor != null && idProveedor <= 0) {
            throw new IllegalArgumentException("El ID del proveedor debe ser positivo");
        }
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        if (idProveedor != null && fechaInicio != null && fechaFin != null) {
            return listarPorProveedorYFechas(idProveedor, fechaInicio, fechaFin);
        }

        return Transaccion.consultar(() -> compraDAO.listAll().stream()
                .filter(c -> idProveedor == null || c.getProveedor().getId() == idProveedor)
                .filter(c -> fechaInicio == null || !c.getFechaEmision().isBefore(fechaInicio))
                .filter(c -> fechaFin == null || !c.getFechaEmision().isAfter(fechaFin))
                .toList());
    }

    @Override
    public List<Compra> listarPorProveedorYFechas(int idProveedor, LocalDate fechaInicio, LocalDate fechaFin)
            throws SQLException {
        if (idProveedor <= 0) {
            throw new IllegalArgumentException("El ID del proveedor debe ser positivo");
        }
        Validador.obligatorio(fechaInicio, "fecha de inicio");
        Validador.obligatorio(fechaFin, "fecha de fin");
        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        return Transaccion.ejecutar(() -> {
            return compraDAO.listarPorProveedorYFechas(idProveedor, fechaInicio, fechaFin);
        });
    }

    private String siguienteNumero(LocalDate fechaEmision) throws SQLException {
        String prefijo = "OC-" + fechaEmision.getYear() + "-";
        long mayor = 0;
        for (Compra anterior : compraDAO.listAll()) {
            String numero = anterior.getNumero();
            if (numero == null || !numero.regionMatches(true, 0, prefijo, 0, prefijo.length())) {
                continue;
            }
            String correlativo = numero.substring(prefijo.length());
            if (correlativo.matches("[0-9]+")) {
                try {
                    mayor = Math.max(mayor, Long.parseLong(correlativo));
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("El correlativo de compras excede el límite permitido");
                }
            }
        }
        if (mayor == Long.MAX_VALUE) {
            throw new IllegalStateException("No se pueden generar más correlativos para ese año");
        }
        return prefijo + String.format(Locale.ROOT, "%04d", mayor + 1);
    }

    private void validarNumeroUnico(String numero, int idCompraActual) throws SQLException {
        boolean repetido = compraDAO.listAll().stream()
                .anyMatch(c -> c.getId() != idCompraActual
                        && c.getNumero() != null
                        && c.getNumero().trim().equalsIgnoreCase(numero.trim()));
        if (repetido) {
            throw new IllegalArgumentException(
                    "Ya existe una compra con el número " + numero + ". Ingrese un número diferente.");
        }
    }

    private void validarCompra(Compra compra) {
        Validador.obligatorio(compra, "compra");
        Validador.obligatorio(compra.getProveedor(), "proveedor");
        Validador.obligatorio(compra.getUsuarioRegistro(), "usuario de registro");
        Validador.obligatorio(compra.getFechaEmision(), "fecha de emisión");
        Validador.obligatorio(compra.getEstado(), "estado");

        if (compra.getProveedor().getId() <= 0 || compra.getUsuarioRegistro().getId() <= 0) {
            throw new IllegalArgumentException("El proveedor y el usuario deben tener un ID válido");
        }

        if (compra.getNumero() != null && !compra.getNumero().isBlank()) {
            compra.setNumero(TextoNormalizado.nombre(
                    Validador.textoObligatorio(compra.getNumero(), "número", 50)));
        }
        compra.setObservaciones(TextoNormalizado.opcional(
                Validador.textoOpcional(compra.getObservaciones(), "observaciones", 255)));

    }

    private void validarDetalles(List<DetalleCompra> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un detalle");
        }
        Set<Integer> lineas = new HashSet<>();
        for (DetalleCompra detalle : detalles) {
            Validador.obligatorio(detalle, "detalle de compra");
            Validador.obligatorio(detalle.getProducto(), "producto");
            Validador.obligatorio(detalle.getUnidadCompra(), "unidad de compra");
            if (detalle.getNumeroLinea() <= 0 || !lineas.add(detalle.getNumeroLinea())) {
                throw new IllegalArgumentException("Los números de línea deben ser positivos y únicos");
            }
            if (detalle.getProducto().getId() <= 0) {
                throw new IllegalArgumentException("El producto debe tener un ID válido");
            }
            CalculoCompra.linea(detalle);
            if (!Double.isFinite(detalle.getCantidad())
                    || !Double.isFinite(detalle.getPrecioUnitario())
                    || !Double.isFinite(detalle.getDescuento())
                    || !Double.isFinite(detalle.getImporte())
                    || !Double.isFinite(detalle.getFactorConversion())
                    || detalle.getCantidad() <= 0
                    || detalle.getPrecioUnitario() < 0
                    || detalle.getDescuento() < 0
                    || detalle.getImporte() < 0
                    || detalle.getFactorConversion() <= 0) {
                throw new IllegalArgumentException("Las cantidades e importes del detalle deben ser válidos");
            }
        }
    }

    private void validarId(int idCompra) {
        if (idCompra <= 0) {
            throw new IllegalArgumentException("El ID de la compra debe ser positivo");
        }
    }

    // RF004: un proveedor desactivado conserva su historial, pero no se usa en nuevas ordenes
    private void verificarProveedorActivo(int idProveedor) throws SQLException {
        Proveedor proveedor = proveedorDAO.load(idProveedor);
        if (proveedor == null || !proveedor.isEstado()) {
            throw new IllegalArgumentException("El proveedor " + idProveedor + " no existe o está desactivado");
        }
    }
}
