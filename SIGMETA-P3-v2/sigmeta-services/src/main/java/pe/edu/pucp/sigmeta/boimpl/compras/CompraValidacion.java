package pe.edu.pucp.sigmeta.boimpl.compras;

import pe.edu.pucp.sigmeta.dao.socio.ProveedorDAO;
import pe.edu.pucp.sigmeta.model.socio.Proveedor;
import pe.edu.pucp.sigmeta.boimpl.Validador;
import pe.edu.pucp.sigmeta.boimpl.producto.TextoNormalizado;
import pe.edu.pucp.sigmeta.dao.compras.CompraDAO;
import pe.edu.pucp.sigmeta.model.compras.Compra;
import pe.edu.pucp.sigmeta.model.compras.DetalleCompra;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// RF011: validaciones de la compra (numero unico, cabecera, detalle y proveedor activo).
// Parte de CompraBOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class CompraValidacion {

    private final CompraDAO compraDAO;
    private final ProveedorDAO proveedorDAO;

    CompraValidacion(CompraDAO compraDAO, ProveedorDAO proveedorDAO) {
        this.compraDAO = compraDAO;
        this.proveedorDAO = proveedorDAO;
    }

    void validarNumeroUnico(String numero, int idCompraActual) throws SQLException {
        boolean repetido = compraDAO.listAll().stream()
                .anyMatch(c -> c.getId() != idCompraActual
                        && c.getNumero() != null
                        && c.getNumero().trim().equalsIgnoreCase(numero.trim()));
        if (repetido) {
            throw new IllegalArgumentException(
                    "Ya existe una compra con el número " + numero + ". Ingrese un número diferente.");
        }
    }

    void validarCompra(Compra compra) {
        Validador.obligatorio(compra, "compra");
        Validador.obligatorio(compra.getProveedor(), "proveedor");
        Validador.obligatorio(compra.getUsuarioRegistro(), "usuario de registro");
        Validador.obligatorio(compra.getFechaEmision(), "fecha de emisión");
        Validador.obligatorio(compra.getEstado(), "estado");
        if (compra.getFechaRecepcionEstimada() != null
                && compra.getFechaRecepcionEstimada().isBefore(compra.getFechaEmision())) {
            throw new IllegalArgumentException(
                    "La recepción estimada no puede ser anterior a la emisión");
        }

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

    void validarDetalles(List<DetalleCompra> detalles) {
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

    void validarId(int idCompra) {
        if (idCompra <= 0) {
            throw new IllegalArgumentException("El ID de la compra debe ser positivo");
        }
    }

    // RF004: un proveedor desactivado conserva su historial, pero no se usa en nuevas ordenes
    void verificarProveedorActivo(int idProveedor) throws SQLException {
        Proveedor proveedor = proveedorDAO.load(idProveedor);
        if (proveedor == null || !proveedor.isEstado()) {
            throw new IllegalArgumentException("El proveedor " + idProveedor + " no existe o está desactivado");
        }
    }
}
