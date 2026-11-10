package pe.edu.pucp.sigmeta.ws.ventas;

import java.util.ArrayList;
import java.util.List;

// RF008: comprobante electronico (boleta o factura) tal como viaja en JSON.
//
// Lo que Blazor manda:
//   emitir:            {"idVenta": 2, "tipo": "BOLETA"}        (o "FACTURA"; la serie y el numero los pone el BO)
//   registrar envio:   {"medioEnvio": "CORREO"}                (CORREO, MENSAJERIA o IMPRESO; la fecha la pone el BO)
// Con el comprobante viajan los datos del cliente y las lineas de la venta, para mostrarlo completo en la pantalla.
public class ComprobanteDatos {

    public int id;
    public int idVenta;
    public String numeroVenta;
    public String condicionPago;
    public String tipo;
    public String serie;
    public String numero;
    public String comprobante;
    public String fechaEmision;
    public String estado;
    public double subTotal;
    public double igv;
    public double total;
    public String medioEnvio;
    public String fechaEnvio;
    public String fechaRegistro;

    public int idCliente;
    public String cliente;
    public String tipoDocumentoCliente;
    public String documentoCliente;
    public String direccionCliente;
    public String correoCliente;

    public List<LineaDatos> lineas = new ArrayList<>();

    public ComprobanteDatos() {
    }
}
