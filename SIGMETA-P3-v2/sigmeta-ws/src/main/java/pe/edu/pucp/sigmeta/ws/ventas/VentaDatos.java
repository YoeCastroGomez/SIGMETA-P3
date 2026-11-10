package pe.edu.pucp.sigmeta.ws.ventas;

import java.util.ArrayList;
import java.util.List;

// RF008: venta tal como viaja en JSON (lo que responde /api/ventas y lo que Blazor manda).
//
// Lo que Blazor manda al registrar:
//   venta directa:      {"idCliente": 3, "fechaEmision": "2026-11-10", "condicionPago": "CONTADO",
//                        "lineas": [{"idProducto": 5, "cantidad": 2, "precioUnitario": 45, "descuento": 0}]}
//   desde una orden:    lo mismo con "idOrden": 2 y en cada linea "idDetalleOrden" (la linea de la orden que atiende)
//   al credito:         "condicionPago": "CREDITO" y opcional "plazoCreditoDias" (si va en 0, el BO usa el del cliente)
// El numero, el estado y los totales los pone el BO. Al modificar no cambian la condicion de pago ni la orden.
public class VentaDatos {

    public int id;
    public String numero;
    public int idCliente;
    public String cliente;
    public String documentoCliente;
    public int idOrden;
    public String numeroOrden;
    public String fechaEmision;
    public String condicionPago;
    public int plazoCreditoDias;
    public String estado;
    public String observaciones;
    public double subTotal;
    public double igv;
    public double total;
    public String usuarioRegistro;
    public String fechaRegistro;
    public boolean anulado;
    public String motivoAnulacion;
    public String fechaAnulacion;

    // comprobante vigente (boleta o factura) de la venta; 0 y null si aun no se emite
    public int idComprobante;
    public String tipoComprobante;
    public String comprobante;

    public List<LineaDatos> lineas = new ArrayList<>();

    public VentaDatos() {
    }
}
