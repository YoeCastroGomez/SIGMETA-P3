package pe.edu.pucp.sigmeta.daoimpl.cobranza;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import pe.edu.pucp.sigmeta.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.sigmeta.model.socio.Cliente;
import pe.edu.pucp.sigmeta.model.ventas.Venta;
import pe.edu.pucp.sigmeta.model.enums.EstadoCuentaPorCobrar;

// Lectura de una fila de cuenta_por_cobrar (ResultSet) al modelo CuentaPorCobrar.
// Parte de CuentaPorCobrarDAOImpl: la clase publica conserva su API y delega aqui (mismo paquete, no publica).
final class CuentaPorCobrarMapeo {

    static CuentaPorCobrar mapearCuentaPorCobrar(ResultSet rs) throws SQLException {
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
            cliente.setTipoPersona(pe.edu.pucp.sigmeta.model.enums.TipoPersona.valueOf(rs.getString("tipo_persona")));
            cliente.setTipoDocumento(pe.edu.pucp.sigmeta.model.enums.TipoDocumentoIdentidad.valueOf(rs.getString("tipo_documento")));
            cliente.setNombres(rs.getString("nombres"));
            cliente.setApellidos(rs.getString("apellidos"));
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

        cxc.setMontoOriginal(rs.getDouble("monto_original"));
        cxc.setMontoAcreditado(rs.getDouble("monto_acreditado"));
        cxc.setMontoPagado(rs.getDouble("monto_pagado"));
        cxc.setSaldoPendiente(rs.getDouble("saldo_pendiente"));

        String strEstado = rs.getString("estado");
        if (strEstado != null) {
            cxc.setEstado(EstadoCuentaPorCobrar.valueOf(strEstado));
        }

        return cxc;
    }
}
