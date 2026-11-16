package pe.edu.pucp.sigmeta.daoimpl;

import pe.edu.pucp.sigmeta.dao.ConsultaDAO;
import pe.edu.pucp.sigmeta.transaction.transactionContext;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConsultaDAOImpl implements ConsultaDAO {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public List<Map<String, Object>> consultar(String procedimiento, Object... parametros) throws SQLException {
        List<Map<String, Object>> filas = new ArrayList<>();
        try (CallableStatement cs = preparar(procedimiento, parametros);
             ResultSet rs = cs.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnas = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> fila = new LinkedHashMap<>();
                for (int i = 1; i <= columnas; i++) {
                    fila.put(nombreJson(meta.getColumnLabel(i)), valor(rs, i, meta.getColumnType(i)));
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    @Override
    public BigDecimal numero(String procedimiento, Object... parametros) throws SQLException {
        try (CallableStatement cs = preparar(procedimiento, parametros);
             ResultSet rs = cs.executeQuery()) {
            if (rs.next()) {
                BigDecimal valor = rs.getBigDecimal(1);
                return valor == null ? BigDecimal.ZERO : valor;
            }
        }
        return BigDecimal.ZERO;
    }

    @Override
    public void ejecutar(String procedimiento, Object... parametros) throws SQLException {
        try (CallableStatement cs = preparar(procedimiento, parametros)) {
            cs.execute();
        }
    }

    private CallableStatement preparar(String procedimiento, Object... parametros) throws SQLException {
        // el nombre nunca viene del usuario, pero se valida igual: solo procedimientos sp_...
        if (procedimiento == null || !procedimiento.matches("^sp_[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Nombre de procedimiento no permitido: " + procedimiento);
        }
        StringBuilder sql = new StringBuilder("{call ").append(procedimiento).append("(");
        for (int i = 0; i < parametros.length; i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")}");
        Connection conn = transactionContext.getConnection();
        CallableStatement cs = conn.prepareCall(sql.toString());
        for (int i = 0; i < parametros.length; i++) {
            Object p = parametros[i];
            if (p == null) {
                cs.setNull(i + 1, Types.NULL);
            } else if (p instanceof LocalDate fecha) {
                cs.setDate(i + 1, java.sql.Date.valueOf(fecha));
            } else if (p instanceof LocalDateTime fechaHora) {
                cs.setTimestamp(i + 1, java.sql.Timestamp.valueOf(fechaHora));
            } else {
                cs.setObject(i + 1, p);
            }
        }
        return cs;
    }

    // "fecha_emision" -> "fechaEmision": los nombres viajan en JSON igual que los del resto del servicio
    private static String nombreJson(String columna) {
        StringBuilder sb = new StringBuilder();
        boolean mayuscula = false;
        for (char c : columna.toCharArray()) {
            if (c == '_') {
                mayuscula = true;
            } else {
                sb.append(mayuscula ? Character.toUpperCase(c) : c);
                mayuscula = false;
            }
        }
        return sb.toString();
    }

    private static Object valor(ResultSet rs, int i, int tipo) throws SQLException {
        switch (tipo) {
            case Types.DATE: {
                java.sql.Date fecha = rs.getDate(i);
                return fecha == null ? null : fecha.toLocalDate().toString();
            }
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE: {
                java.sql.Timestamp fecha = rs.getTimestamp(i);
                return fecha == null ? null : fecha.toLocalDateTime().format(FECHA_HORA);
            }
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL: {
                BigDecimal numero = rs.getBigDecimal(i);
                return numero == null ? null : numero.doubleValue();
            }
            case Types.BIT:
            case Types.BOOLEAN:
            case Types.TINYINT: {
                // BOOLEAN de MySQL es TINYINT(1)
                boolean valor = rs.getBoolean(i);
                return rs.wasNull() ? null : valor;
            }
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.BIGINT: {
                long numero = rs.getLong(i);
                return rs.wasNull() ? null : numero;
            }
            default:
                return rs.getString(i);
        }
    }
}
