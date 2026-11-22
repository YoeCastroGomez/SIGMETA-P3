package pe.edu.pucp.sigmeta.ws.almacen;

import java.util.ArrayList;
import java.util.List;

// Recepcion, despacho o ajuste tal como lo manda Blazor. Cada operacion usa solo sus campos.
public class DocumentoAlmacen {
    public int idCompra;
    public int idVenta;
    public int idProducto;
    public String fecha;                // AAAA-MM-DD; vacio = hoy
    public String observaciones;
    public String direccionEntrega;
    public String transportista;
    public String motivo;
    public Double cantidadContada;      // ajuste por conteo fisico
    public String datosOperacion;       // RF014: solo al solicitar el ajuste: la pantalla (JSON), para retomarla
    public List<LineaAlmacen> lineas = new ArrayList<>();

    public DocumentoAlmacen() {
    }
}
