package pe.edu.pucp.sigmeta.daoimpl.cobranza;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import pe.edu.pucp.sigmeta.dao.cobranza.CuentaPorCobrarDAO;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.enums.Moneda;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

public class CuentaPorCobrarDAOImpl implements CuentaPorCobrarDAO {

    private CuentaPorCobrar mapearCuentaPorCobrar(ResultSet rs) throws SQLException {
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(rs.getInt("cxc_id"));

        int idVenta = rs.getInt("venta_id");
        if (!rs.wasNull()) {
            Venta venta = new Venta();
            venta.setId(idVenta);
            venta.setNumero(rs.getString("venta_numero"));
            cxc.setVenta(venta);
        }

        int idCliente = rs.getInt("cliente_id");
        if (!rs.wasNull()) {
            Cliente cliente = new Cliente();
            cliente.setId(idCliente);
            cliente.setRazonSocial(rs.getString("razon_social"));
            cliente.setNumeroDocumento(rs.getString("numero_documento"));
            cxc.setCliente(cliente);
        }

        Date fechaEmision = rs.getDate("fecha_emision");
        if (fechaEmision != null) {
            cxc.setFechaEmision(fechaEmision.toLocalDate());
        }

        Date fechaVencimiento = rs.getDate("fecha_vencimiento");
        if (fechaVencimiento != null) {
            cxc.setFechaVencimiento(fechaVencimiento.toLocalDate());
        }

        String strMoneda = rs.getString("moneda");
        if (strMoneda != null) {
            cxc.setMoneda(Moneda.valueOf(strMoneda));
        }

        cxc.setMontoOriginal(rs.getDouble("monto_original"));
        cxc.setMontoPagado(rs.getDouble("monto_pagado"));
        cxc.setSaldoPendiente(rs.getDouble("saldo_pendiente"));

        String strEstado = rs.getString("estado");
        if (strEstado != null) {
            cxc.setEstado(EstadoCuentaPorCobrar.valueOf(strEstado));
        }

        return cxc;
    }

    @Override
    public CuentaPorCobrar save(CuentaPorCobrar cxc) throws SQLException {
        String sql = "{call sp_cuenta_por_cobrar_insertar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);

            if (cxc.getVenta() != null && cxc.getVenta().getId() > 0) {
                cs.setInt(2, cxc.getVenta().getId());
            } else {
                cs.setNull(2, Types.INTEGER);
            }

            cs.setInt(3, cxc.getCliente().getId());
            cs.setDate(4, Date.valueOf(cxc.getFechaEmision()));
            cs.setDate(5, Date.valueOf(cxc.getFechaVencimiento()));
            cs.setString(6, cxc.getMoneda().name());
            cs.setDouble(7, cxc.getMontoOriginal());
            cs.setDouble(8, cxc.getMontoPagado());
            cs.setDouble(9, cxc.getSaldoPendiente());
            cs.setString(10, cxc.getEstado().name());

            cs.executeUpdate();
            cxc.setId(cs.getInt(1));
        }
        return cxc;
    }

    @Override
    public CuentaPorCobrar update(CuentaPorCobrar cxc) throws SQLException {
        String sql = "{call sp_cuenta_por_cobrar_modificar(?, ?, ?, ?, ?, ?, ?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, cxc.getId());
            cs.setDate(2, Date.valueOf(cxc.getFechaVencimiento()));
            cs.setString(3, cxc.getMoneda().name());
            cs.setDouble(4, cxc.getMontoOriginal());
            cs.setDouble(5, cxc.getMontoPagado());
            cs.setDouble(6, cxc.getSaldoPendiente());
            cs.setString(7, cxc.getEstado().name());

            cs.executeUpdate();
        }
        return cxc;
    }

    @Override
    public void remove(CuentaPorCobrar cxc) throws SQLException {
        String sql = "{call sp_cuenta_por_cobrar_eliminar(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, cxc.getId());
            cs.executeUpdate();
        }
    }

    @Override
    public CuentaPorCobrar load(Integer id) throws SQLException {
        CuentaPorCobrar cxc = null;
        String sql = "{call sp_cuenta_por_cobrar_obtener(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    cxc = mapearCuentaPorCobrar(rs);
                }
            }
        }
        return cxc;
    }

    @Override
    public CuentaPorCobrar obtenerPorVenta(int idVenta) throws SQLException {
        CuentaPorCobrar cxc = null;
        String sql = "{call sp_cuenta_por_cobrar_obtener_por_venta(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idVenta);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    cxc = mapearCuentaPorCobrar(rs);
                }
            }
        }
        return cxc;
    }

    @Override
    public List<CuentaPorCobrar> listarPorCliente(int idCliente) throws SQLException {
        List<CuentaPorCobrar> lista = new ArrayList<>();
        String sql = "{call sp_cuenta_por_cobrar_listar_por_cliente(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setInt(1, idCliente);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCuentaPorCobrar(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public List<CuentaPorCobrar> listarPorEstado(EstadoCuentaPorCobrar estado) throws SQLException {
        List<CuentaPorCobrar> lista = new ArrayList<>();
        String sql = "{call sp_cuenta_por_cobrar_listar_por_estado(?)}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql)) {
            cs.setString(1, estado.name());
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCuentaPorCobrar(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public List<CuentaPorCobrar> listarVencidas() throws SQLException {
        List<CuentaPorCobrar> lista = new ArrayList<>();
        String sql = "{call sp_cuenta_por_cobrar_listar_vencidas()}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCuentaPorCobrar(rs));
            }
        }
        return lista;
    }

    @Override
    public List<CuentaPorCobrar> listarTodasConDetalle() throws SQLException {
        List<CuentaPorCobrar> lista = new ArrayList<>();
        String sql = "{call sp_cuenta_por_cobrar_listar_todas_con_detalle()}";
        Connection con = transactionContext.getConnection();
        try (CallableStatement cs = con.prepareCall(sql);
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearCuentaPorCobrar(rs));
            }
        }
        return lista;
    }
}
