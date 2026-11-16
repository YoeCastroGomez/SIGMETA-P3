package pe.edu.pucp.sigmeta.bo.reporte;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

// RF015: reportes del Administrador sobre un periodo. Cada reporte es un mapa "bloque -> filas"; cada bloque
// sale de un procedimiento almacenado que ya agrega en SQL (RNF003).
public interface ReporteBO {

    // ventas del periodo, por dia, por vendedor, por cliente y credito de clientes
    Map<String, Object> comercial(LocalDate desde, LocalDate hasta, int idAdministrador) throws SQLException;

    // compras por proveedor, productos de mayor movimiento, sin movimiento, bajo el minimo e inventario valorizado
    Map<String, Object> abastecimiento(LocalDate desde, LocalDate hasta, int idAdministrador) throws SQLException;

    // cierres del periodo, dinero por medio de pago y diferencias por cajero
    Map<String, Object> caja(LocalDate desde, LocalDate hasta, int idAdministrador) throws SQLException;
}
