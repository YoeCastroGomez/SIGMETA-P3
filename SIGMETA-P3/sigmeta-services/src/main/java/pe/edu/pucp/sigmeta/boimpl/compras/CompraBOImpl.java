package pe.edu.pucp.sigmeta.boimpl.compras;

import pe.edu.pucp.sigmeta.bo.compras.CompraBO;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.dao.compras.CompraDAO;
import pe.edu.pucp.sigmeta.dao.compras.DetalleCompraDAO;
import pe.edu.pucp.sigmeta.daoimpl.compras.CompraDAOImpl;
import pe.edu.pucp.sigmeta.daoimpl.compras.DetalleCompraDAOImpl;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import pe.edu.pucp.sigmeta.model.enums.EstadoCompra;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompraBOImpl implements CompraBO {

    private final CompraDAO compraDAO;
    private final DetalleCompraDAO detalleCompraDAO;

    public CompraBOImpl() {
        this.compraDAO = new CompraDAOImpl();
        this.detalleCompraDAO = new DetalleCompraDAOImpl();
    }

    @Override
    public Compra registrar(Compra compra) throws SQLException {
        validarCompra(compra);
        if (compra.getEstado() != EstadoCompra.REGISTRADA || compra.isAnulado()) {
            throw new IllegalArgumentException("Una compra nueva debe estar REGISTRADA y no anulada");
        }
        validarDetalles(compra.getDetalles());
        verificarSubtotal(compra, compra.getDetalles());

        try {
            compraDAO.save(compra);
            for (DetalleCompra detalle : compra.getDetalles()) {
                detalle.setCompra(compra);
                detalleCompraDAO.save(detalle);
            }
            transactionContext.commit();
            return compra;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Compra modificar(Compra compra) throws SQLException {
        validarCompra(compra);
        validarId(compra.getId());
        if (compra.getEstado() != EstadoCompra.REGISTRADA || compra.isAnulado()) {
            throw new IllegalArgumentException("Solo se pueden editar compras registradas y no anuladas");
        }

        try {
            Connection conn = transactionContext.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM compra WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, compra.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("No existe la compra con ID " + compra.getId());
                    }
                }
            }
            Compra existente = compraDAO.load(compra.getId());
            if (existente == null) {
                throw new IllegalArgumentException("No existe la compra con ID " + compra.getId());
            }
            if (existente.isAnulado() || existente.getEstado() != EstadoCompra.REGISTRADA) {
                throw new IllegalArgumentException("La compra ya no se puede modificar");
            }
            // Una modificacion de cabecera no puede desajustar el importe de las lineas guardadas.
            List<DetalleCompra> lineas = new java.util.ArrayList<>();
            for (DetalleCompra linea : detalleCompraDAO.listAll()) {
                if (linea.getCompra().getId() == compra.getId()) {
                    lineas.add(linea);
                }
            }
            if (lineas.isEmpty()) {
                throw new IllegalArgumentException("La compra no tiene detalles registrados");
            }
            verificarSubtotal(compra, lineas);
            compraDAO.update(compra);
            transactionContext.commit();
            return compra;
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public void anular(int idCompra) throws SQLException {
        validarId(idCompra);
        try {
            Compra compra = compraDAO.load(idCompra);
            if (compra == null) {
                throw new IllegalArgumentException("No existe la compra con ID " + idCompra);
            }
            if (compra.isAnulado()) {
                throw new IllegalArgumentException("La compra ya esta anulada");
            }
            compraDAO.remove(compra);
            transactionContext.commit();
        } catch (SQLException | RuntimeException e) {
            transactionContext.rollback();
            throw e;
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public Compra obtener(int idCompra) throws SQLException {
        validarId(idCompra);
        try {
            return compraDAO.load(idCompra);
        } finally {
            transactionContext.close();
        }
    }

    @Override
    public List<Compra> listarTodos() throws SQLException {
        try {
            return compraDAO.listAll();
        } finally {
            transactionContext.close();
        }
    }

    private void validarCompra(Compra compra) {
        Validador.obligatorio(compra, "compra");
        Validador.obligatorio(compra.getProveedor(), "proveedor");
        Validador.obligatorio(compra.getUsuarioRegistro(), "usuario de registro");
        Validador.obligatorio(compra.getFechaEmision(), "fecha de emision");
        Validador.obligatorio(compra.getMoneda(), "moneda");
        Validador.obligatorio(compra.getEstado(), "estado");

        if (compra.getProveedor().getId() <= 0 || compra.getUsuarioRegistro().getId() <= 0) {
            throw new IllegalArgumentException("El proveedor y el usuario deben tener un ID valido");
        }

        compra.setNumero(Validador.textoObligatorio(compra.getNumero(), "numero", 50));
        compra.setObservaciones(Validador.textoOpcional(compra.getObservaciones(), "observaciones", 255));

        if (!Double.isFinite(compra.getSubTotal())
                || !Double.isFinite(compra.getIgv())
                || !Double.isFinite(compra.getTotal())
                || compra.getSubTotal() < 0
                || compra.getIgv() < 0
                || compra.getTotal() < 0) {
            throw new IllegalArgumentException("Los importes deben ser validos y no negativos");
        }
        if (Math.abs(compra.getSubTotal() + compra.getIgv() - compra.getTotal()) > 0.011) {
            throw new IllegalArgumentException("El total debe ser igual al subtotal mas el IGV");
        }
    }

    private void verificarSubtotal(Compra compra, List<DetalleCompra> detalles) {
        BigDecimal suma = BigDecimal.ZERO;
        for (DetalleCompra detalle : detalles) {
            suma = suma.add(BigDecimal.valueOf(detalle.getImporte()));
        }
        suma = suma.setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = BigDecimal.valueOf(compra.getSubTotal()).setScale(2, RoundingMode.HALF_UP);
        if (suma.compareTo(subtotal) != 0) {
            throw new IllegalArgumentException("El subtotal no coincide con la suma de importes de los detalles");
        }
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
                throw new IllegalArgumentException("Los numeros de linea deben ser positivos y unicos");
            }
            if (detalle.getProducto().getId() <= 0) {
                throw new IllegalArgumentException("El producto debe tener un ID valido");
            }
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
                throw new IllegalArgumentException("Las cantidades e importes del detalle deben ser validos");
            }
        }
    }

    private void validarId(int idCompra) {
        if (idCompra <= 0) {
            throw new IllegalArgumentException("El ID de la compra debe ser positivo");
        }
    }
}
