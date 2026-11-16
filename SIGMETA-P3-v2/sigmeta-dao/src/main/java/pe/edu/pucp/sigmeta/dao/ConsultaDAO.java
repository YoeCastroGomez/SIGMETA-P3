package pe.edu.pucp.sigmeta.dao;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// Consultas de solo lectura que devuelven filas ya armadas por un procedimiento almacenado
// (listados con nombres y reportes agregados: RF012, RF013, RF015). Cada fila es un mapa
// columna -> valor, en el orden del SELECT del procedimiento. Las fechas llegan como texto
// (AAAA-MM-DD o AAAA-MM-DDTHH:MM:SS) para que viajen igual que en el resto del sistema.
// Aqui no hay reglas de negocio: quien puede consultar lo decide el BO.
public interface ConsultaDAO {

    // Ejecuta "{call procedimiento(?, ?, ...)}" y devuelve todas sus filas
    List<Map<String, Object>> consultar(String procedimiento, Object... parametros) throws SQLException;

    // Primera columna de la primera fila como numero (0 si no hay filas o es NULL)
    BigDecimal numero(String procedimiento, Object... parametros) throws SQLException;

    // Procedimiento que modifica y no devuelve filas
    void ejecutar(String procedimiento, Object... parametros) throws SQLException;
}
