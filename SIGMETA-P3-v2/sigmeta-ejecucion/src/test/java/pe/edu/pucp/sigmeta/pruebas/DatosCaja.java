package pe.edu.pucp.sigmeta.pruebas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import pe.edu.pucp.sigmeta.dbmanager.DBManager;

/**
 * H14: datos comunes de las pruebas de integracion de caja y cobranza (Adriano).
 * Los usuarios se buscan por rol y no por id, asi sirven con cualquier carga de datos;
 * el cajero de la prueba es uno que NO tenga su caja abierta (en los datos demo, rsanchez tiene una).
 */
final class DatosCaja {

    private DatosCaja() {
    }

    // primer usuario activo del rol; 0 si no hay
    static int usuarioActivo(String rol) throws SQLException {
        return entero("SELECT u.id FROM usuario u JOIN rol r ON r.id = u.id_rol "
                + "WHERE r.tipo = ? AND u.estado = TRUE AND r.estado = TRUE ORDER BY u.id LIMIT 1", rol);
    }

    // cajero activo que no tenga una caja abierta (para poder abrir una en la prueba); 0 si no hay
    static int cajeroSinCajaAbierta() throws SQLException {
        return entero("SELECT u.id FROM usuario u JOIN rol r ON r.id = u.id_rol "
                + "WHERE r.tipo = 'CAJERO' AND u.estado = TRUE AND r.estado = TRUE "
                + "AND NOT EXISTS (SELECT 1 FROM caja c WHERE c.id_usuario_apertura = u.id AND c.abierta = TRUE) "
                + "ORDER BY u.id LIMIT 1");
    }

    // otro cajero activo distinto al indicado; 0 si no hay
    static int otroCajero(int idCajero) throws SQLException {
        return entero("SELECT u.id FROM usuario u JOIN rol r ON r.id = u.id_rol "
                + "WHERE r.tipo = 'CAJERO' AND u.estado = TRUE AND r.estado = TRUE AND u.id <> ? ORDER BY u.id LIMIT 1", idCajero);
    }

    // cuenta por cobrar con al menos ese saldo pendiente; 0 si no hay
    static int cuentaConSaldo(double minimo) throws SQLException {
        return entero("SELECT x.id FROM cuenta_por_cobrar x JOIN venta v ON v.id = x.id_venta "
                + "WHERE x.saldo_pendiente >= ? AND v.anulado = FALSE ORDER BY x.saldo_pendiente DESC, x.id LIMIT 1", minimo);
    }

    // id de la caja abierta del usuario; 0 si no tiene
    static int cajaAbiertaDe(int idUsuario) throws SQLException {
        return entero("SELECT id FROM caja WHERE id_usuario_apertura = ? AND abierta = TRUE LIMIT 1", idUsuario);
    }

    static int contar(String sql, Object... parametros) throws SQLException {
        return entero(sql, parametros);
    }

    private static int entero(String sql, Object... parametros) throws SQLException {
        try (Connection con = DBManager.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                int valor = rs.next() ? rs.getInt(1) : 0;
                con.rollback();
                return valor;
            }
        }
    }
}
