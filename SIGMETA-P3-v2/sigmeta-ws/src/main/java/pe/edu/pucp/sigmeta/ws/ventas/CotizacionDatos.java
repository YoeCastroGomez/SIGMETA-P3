package pe.edu.pucp.sigmeta.ws.ventas;

import java.util.ArrayList;
import java.util.List;

// RF007: cotizacion tal como viaja en JSON (lo que responde /api/cotizaciones y lo que Blazor manda).
//
// Lo que Blazor manda al registrar o modificar:
//   {"numero": "COT-2026-0010", "idCliente": 3, "fechaEmision": "2026-11-10", "fechaVigencia": "2026-11-24",
//    "observaciones": "...", "lineas": [ {...}, {...} ]}
// El estado, los totales y los datos de auditoria no se mandan: los pone el BO.
// Las fechas viajan como texto AAAA-MM-DD; el cliente y el usuario, como texto para mostrar.
public class CotizacionDatos {

    public int id;
    public String numero;
    public int idCliente;
    public String cliente;
    public String documentoCliente;
    public String fechaEmision;
    public String fechaVigencia;
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

    public CotizacionDatos() {
    }
}
