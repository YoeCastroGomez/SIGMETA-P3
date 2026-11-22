package pe.edu.pucp.sigmeta.boimpl.compras;

import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.dao.socio.ProveedorDAO;
import pe.edu.pucp.sigmeta.daoimpl.socio.ProveedorDAOImpl;
import pe.edu.pucp.sigmeta.bo.compras.CompraBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.boimpl.Validador;
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
import java.util.Locale;
import java.util.List;

public class CompraBOImpl implements CompraBO {

    private final CompraDAO compraDAO;
    private final DetalleCompraDAO detalleCompraDAO;
    private final UsuarioBO usuarioBO;
    private final ProveedorDAO proveedorDAO;
    // partes de esta clase (mismo paquete)
    private final CompraValidacion validacionCompra;

    public CompraBOImpl() {
        this.compraDAO = new CompraDAOImpl();
        this.detalleCompraDAO = new DetalleCompraDAOImpl();
        this.usuarioBO = new UsuarioBOImpl();
        this.proveedorDAO = new ProveedorDAOImpl();
        this.validacionCompra = new CompraValidacion(compraDAO, proveedorDAO);
    }

    @Override
    public Compra registrar(Compra compra) throws SQLException {
        validacionCompra.validarCompra(compra);
        if (compra.getEstado() != EstadoCompra.REGISTRADA || compra.isAnulado()) {
            throw new IllegalArgumentException("Una compra nueva debe estar REGISTRADA y no anulada");
        }
        validacionCompra.validarDetalles(compra.getDetalles());
        CalculoCompra.totales(compra, compra.getDetalles());
        usuarioBO.verificarRol(compra.getUsuarioRegistro().getId(), TipoRol.ADMINISTRADOR);

        return Transaccion.ejecutar(() -> {
            validacionCompra.verificarProveedorActivo(compra.getProveedor().getId());
            compra.setNumero(siguienteNumero(compra.getFechaEmision()));
            validacionCompra.validarNumeroUnico(compra.getNumero(), 0);
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
        validacionCompra.validarCompra(compra);
        validacionCompra.validarId(compra.getId());
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
                validacionCompra.verificarProveedorActivo(compra.getProveedor().getId());
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
                validacionCompra.validarDetalles(nuevas);
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
        validacionCompra.validarId(idCompra);
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
        validacionCompra.validarId(idCompra);
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

    @Override
    public String proponerNumero(LocalDate fechaEmision, int idAdministrador) throws SQLException {
        Validador.obligatorio(fechaEmision, "fecha de emisión");
        return Transaccion.consultar(() -> {
            usuarioBO.verificarRol(idAdministrador, TipoRol.ADMINISTRADOR);
            return siguienteNumero(fechaEmision);
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

}
