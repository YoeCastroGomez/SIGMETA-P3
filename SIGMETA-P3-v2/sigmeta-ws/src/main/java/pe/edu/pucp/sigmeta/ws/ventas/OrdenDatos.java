package pe.edu.pucp.sigmeta.ws.ventas;

import java.util.ArrayList;
import java.util.List;

// RF007: orden de compra del cliente tal como viaja en JSON (lo que responde /api/ordenes).
//
// Lo que Blazor manda:
//   generar desde una cotizacion ACEPTADA:  {"idCotizacion": 2, "numeroOrdenCliente": "OC-ACME-551"}
//   modificar (solo PENDIENTE):             {"numeroOrdenCliente": "OC-ACME-552", "observaciones": "..."}
// El detalle no se manda: el BO lo copia de la cotizacion. El estado de atencion
// (PENDIENTE, ATENDIDA_PARCIAL, ATENDIDA) lo actualiza el BO con cada venta.
public class OrdenDatos {

    public int id;
    public String numero;
    public String numeroOrdenCliente;
    public int idCotizacion;
    public String numeroCotizacion;
    public int idCliente;
    public String cliente;
    public String documentoCliente;
    public String fechaEmision;
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
    public List<LineaDatos> lineas = new ArrayList<>();

    public OrdenDatos() {
    }
}
