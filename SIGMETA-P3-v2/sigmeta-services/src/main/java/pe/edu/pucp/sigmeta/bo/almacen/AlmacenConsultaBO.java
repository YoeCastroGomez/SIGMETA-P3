package pe.edu.pucp.sigmeta.bo.almacen;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// RF012 y RF013: consultas de las pantallas de almacen y de notas de credito. Cada una devuelve filas ya
// armadas por un procedimiento almacenado (con nombres de producto, cliente, proveedor y usuario), para no
// traer tablas completas a memoria (RNF003). Aqui se decide QUIEN puede consultar; no modifican nada.
public interface AlmacenConsultaBO {

    // Recepcion (Almacenero; consulta tambien el Administrador)
    List<Map<String, Object>> comprasPorRecibir(int idUsuario) throws SQLException;
    List<Map<String, Object>> lineasDeCompra(int idCompra, int idUsuario) throws SQLException;
    List<Map<String, Object>> recepciones(int idCompra, int idUsuario) throws SQLException;
    List<Map<String, Object>> detalleDeRecepcion(int idRecepcion, int idUsuario) throws SQLException;

    // Despacho (Almacenero; consulta tambien el Administrador)
    List<Map<String, Object>> ventasPorDespachar(int idUsuario) throws SQLException;
    List<Map<String, Object>> lineasDeVenta(int idVenta, int idUsuario) throws SQLException;
    List<Map<String, Object>> despachos(int idVenta, int idUsuario) throws SQLException;
    List<Map<String, Object>> detalleDeDespacho(int idDespacho, int idUsuario) throws SQLException;

    // Kardex y existencias (consulta de los cuatro roles)
    List<Map<String, Object>> kardex(int idProducto, LocalDate desde, LocalDate hasta, String tipo, int idUsuario) throws SQLException;
    List<Map<String, Object>> existencias(int idUsuario) throws SQLException;

    // Notas de credito (Cajero; consulta tambien el Administrador)
    List<Map<String, Object>> comprobantesParaNota(int idUsuario) throws SQLException;
    List<Map<String, Object>> lineasDevolvibles(int idComprobante, int idUsuario) throws SQLException;
    List<Map<String, Object>> notasCredito(LocalDate desde, LocalDate hasta, String texto, int idUsuario) throws SQLException;
    List<Map<String, Object>> detalleDeNota(int idNota, int idUsuario) throws SQLException;
}
