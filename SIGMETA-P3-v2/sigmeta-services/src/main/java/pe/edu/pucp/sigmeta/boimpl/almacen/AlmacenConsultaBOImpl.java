package pe.edu.pucp.sigmeta.boimpl.almacen;

import pe.edu.pucp.sigmeta.bo.almacen.AlmacenConsultaBO;
import pe.edu.pucp.sigmeta.bo.usuario.UsuarioBO;
import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.usuario.UsuarioBOImpl;
import pe.edu.pucp.sigmeta.dao.ConsultaDAO;
import pe.edu.pucp.sigmeta.daoimpl.ConsultaDAOImpl;
import pe.edu.pucp.sigmeta.model.enums.TipoMovimientoInventario;
import pe.edu.pucp.sigmeta.model.enums.TipoRol;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AlmacenConsultaBOImpl implements AlmacenConsultaBO {

    private final ConsultaDAO consultaDAO = new ConsultaDAOImpl();
    private final UsuarioBO usuarioBO = new UsuarioBOImpl();

    // RF012: recepciones y despachos los trabaja el Almacenero y los consulta el Administrador
    private void almacen(int idUsuario) throws SQLException {
        usuarioBO.verificarRol(idUsuario, TipoRol.ALMACENERO, TipoRol.ADMINISTRADOR);
    }

    // RF012: el kardex y las existencias los consultan los cuatro roles
    private void cualquiera(int idUsuario) throws SQLException {
        usuarioBO.verificarRol(idUsuario, TipoRol.values());
    }

    // RF013: las notas de credito las emite el Cajero y las consulta el Administrador
    private void caja(int idUsuario) throws SQLException {
        usuarioBO.verificarRol(idUsuario, TipoRol.CAJERO, TipoRol.ADMINISTRADOR);
    }

    @Override
    public List<Map<String, Object>> comprasPorRecibir(int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            almacen(idUsuario);
            return consultaDAO.consultar("sp_compra_listar_por_recibir");
        });
    }

    @Override
    public List<Map<String, Object>> lineasDeCompra(int idCompra, int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            almacen(idUsuario);
            return consultaDAO.consultar("sp_compra_lineas_por_recibir", idCompra);
        });
    }

    @Override
    public List<Map<String, Object>> recepciones(int idCompra, int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            almacen(idUsuario);
            return consultaDAO.consultar("sp_recepcion_buscar", Math.max(idCompra, 0));
        });
    }

    @Override
    public List<Map<String, Object>> detalleDeRecepcion(int idRecepcion, int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            almacen(idUsuario);
            return consultaDAO.consultar("sp_recepcion_detalle", idRecepcion);
        });
    }

    @Override
    public List<Map<String, Object>> ventasPorDespachar(int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            almacen(idUsuario);
            return consultaDAO.consultar("sp_venta_listar_por_despachar");
        });
    }

    @Override
    public List<Map<String, Object>> lineasDeVenta(int idVenta, int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            almacen(idUsuario);
            return consultaDAO.consultar("sp_venta_lineas_por_despachar", idVenta);
        });
    }

    @Override
    public List<Map<String, Object>> despachos(int idVenta, int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            almacen(idUsuario);
            return consultaDAO.consultar("sp_despacho_buscar", Math.max(idVenta, 0));
        });
    }

    @Override
    public List<Map<String, Object>> detalleDeDespacho(int idDespacho, int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            almacen(idUsuario);
            return consultaDAO.consultar("sp_despacho_detalle", idDespacho);
        });
    }

    @Override
    public List<Map<String, Object>> kardex(int idProducto, LocalDate desde, LocalDate hasta, String tipo,
                                            int idUsuario) throws SQLException {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        String tipoValidado = "";
        if (tipo != null && !tipo.isBlank()) {
            try {
                tipoValidado = TipoMovimientoInventario.valueOf(tipo.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("El tipo de movimiento no es válido");
            }
        }
        final String tipoFinal = tipoValidado;
        return Transaccion.consultar(() -> {
            cualquiera(idUsuario);
            return consultaDAO.consultar("sp_movimiento_buscar", Math.max(idProducto, 0), desde, hasta, tipoFinal);
        });
    }

    @Override
    public List<Map<String, Object>> existencias(int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            cualquiera(idUsuario);
            return consultaDAO.consultar("sp_stock_listar_productos");
        });
    }

    @Override
    public List<Map<String, Object>> comprobantesParaNota(int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            caja(idUsuario);
            return consultaDAO.consultar("sp_comprobante_listar_para_nota");
        });
    }

    @Override
    public List<Map<String, Object>> lineasDevolvibles(int idComprobante, int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            caja(idUsuario);
            return consultaDAO.consultar("sp_comprobante_lineas_devolvibles", idComprobante);
        });
    }

    @Override
    public List<Map<String, Object>> notasCredito(LocalDate desde, LocalDate hasta, String texto, int idUsuario)
            throws SQLException {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        String buscado = texto == null ? "" : texto.trim();
        return Transaccion.consultar(() -> {
            caja(idUsuario);
            return consultaDAO.consultar("sp_nota_credito_buscar", desde, hasta, buscado);
        });
    }

    @Override
    public List<Map<String, Object>> detalleDeNota(int idNota, int idUsuario) throws SQLException {
        return Transaccion.consultar(() -> {
            caja(idUsuario);
            return consultaDAO.consultar("sp_nota_credito_detalle", idNota);
        });
    }
}
