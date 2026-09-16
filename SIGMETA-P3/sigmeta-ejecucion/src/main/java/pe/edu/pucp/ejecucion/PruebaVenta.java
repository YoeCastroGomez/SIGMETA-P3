package pe.edu.pucp.ejecucion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import pe.edu.pucp.model.almacen.Despacho;
import pe.edu.pucp.model.almacen.DetalleDespacho;
import pe.edu.pucp.model.enums.CondicionPago;
import pe.edu.pucp.model.enums.EstadoComprobante;
import pe.edu.pucp.model.enums.EstadoOrdenCompraCliente;
import pe.edu.pucp.model.enums.EstadoVenta;
import pe.edu.pucp.model.enums.Moneda;
import pe.edu.pucp.model.enums.TipoComprobante;
import pe.edu.pucp.model.producto.Producto;
import pe.edu.pucp.model.socio.Cliente;
import pe.edu.pucp.model.usuario.Usuario;
import pe.edu.pucp.model.ventas.Comprobante;
import pe.edu.pucp.model.ventas.Cotizacion;
import pe.edu.pucp.model.ventas.DetalleCotizacion;
import pe.edu.pucp.model.ventas.DetalleNotaCredito;
import pe.edu.pucp.model.ventas.DetalleOrdenCompraCliente;
import pe.edu.pucp.model.ventas.DetalleVenta;
import pe.edu.pucp.model.ventas.OrdenCompraCliente;
import pe.edu.pucp.model.ventas.Venta;

public class PruebaVenta {

    public static Venta ejecutar(Cotizacion cotizacion, Usuario usuario) {
        Cliente cliente = cotizacion != null ? cotizacion.getCliente() : null;
        return ejecutar(cotizacion, cliente, usuario);
    }

    public static Venta ejecutar(Cotizacion cotizacion, Cliente cliente, Usuario registro) {
        System.out.println("== PRUEBA 4: venta, comprobantes y despacho ==");

        if (cliente == null && cotizacion != null) {
            cliente = cotizacion.getCliente();
        }

        if (cotizacion == null) {
            cotizacion = new Cotizacion();
            cotizacion.setId(1);
            cotizacion.setNumero("COT-2026-0001");
            cotizacion.setCliente(cliente);
        }

        if (cotizacion.getDetalles() == null || cotizacion.getDetalles().isEmpty()) {
            Producto prod1 = new Producto();
            prod1.setId(1);
            prod1.setNombre("Interruptor Termomagnetico 2x32A");

            Producto prod2 = new Producto();
            prod2.setId(2);
            prod2.setNombre("Cable Vulcanizado 3x14 AWG");

            Producto prod3 = new Producto();
            prod3.setId(3);
            prod3.setNombre("Caja de Paso Galvanizada 100x100");

            DetalleCotizacion dc1 = new DetalleCotizacion(
                    1, prod1, 20.0, 150.0, 0.0, 3000.0, 1, cotizacion
            );

            DetalleCotizacion dc2 = new DetalleCotizacion(
                    2, prod2, 10.0, 85.0, 50.0, 800.0, 2, cotizacion
            );

            DetalleCotizacion dc3 = new DetalleCotizacion(
                    3, prod3, 40.0, 30.0, 0.0, 1200.0, 3, cotizacion
            );

            cotizacion.getDetalles().add(dc1);
            cotizacion.getDetalles().add(dc2);
            cotizacion.getDetalles().add(dc3);
        }

        OrdenCompraCliente orden = new OrdenCompraCliente();
        orden.setId(1);
        orden.setCliente(cliente);
        orden.setCotizacion(cotizacion);
        orden.setNumeroOrdenCliente("OC-ANDINA-4521");
        orden.setNumero("OCC-2026-0001");
        orden.setFechaEmision(LocalDate.of(2026, 9, 2));
        orden.setMoneda(Moneda.SOLES);
        orden.setSubTotal(5000.00);
        orden.setIgv(900.00);
        orden.setTotal(5900.00);
        orden.setObservaciones("Orden de compra generada a partir de Cotizacion COT-2026-0001");
        orden.setFechaRegistro(LocalDateTime.of(2026, 9, 2, 9, 30, 0));
        orden.setUsuarioRegistro(registro);
        orden.setEstado(EstadoOrdenCompraCliente.ATENDIDA);
        orden.setAnulado(false);
        orden.setMotivoAnulacion(null);
        orden.setFechaAnulacion(null);

        int idDetalleOrden = 1;

        for (DetalleCotizacion detCot : cotizacion.getDetalles()) {
            DetalleOrdenCompraCliente detOrden = new DetalleOrdenCompraCliente();

            detOrden.setId(
                    detCot.getId() > 0
                            ? detCot.getId()
                            : idDetalleOrden
            );

            detOrden.setNumeroLinea(detCot.getNumeroLinea());
            detOrden.setProducto(detCot.getProducto());
            detOrden.setCantidad(detCot.getCantidad());
            detOrden.setPrecioUnitario(detCot.getPrecioUnitario());
            detOrden.setDescuento(detCot.getDescuento());
            detOrden.setImporte(detCot.getImporte());
            detOrden.setCantidadAtendida(detCot.getCantidad());
            detOrden.setOrdenCompraCliente(orden);

            orden.getDetalles().add(detOrden);
            idDetalleOrden++;
        }

        Venta venta = new Venta();
        venta.setId(1);
        venta.setCliente(cliente);
        venta.setOrdenCompraCliente(orden);
        venta.setCondicionPago(CondicionPago.CREDITO);
        venta.setPlazoCreditoDias(30);
        venta.setEstado(EstadoVenta.DESPACHADA_PARCIAL);
        venta.setNumero("VTA-2026-0001");
        venta.setFechaEmision(LocalDate.of(2026, 9, 2));
        venta.setMoneda(Moneda.SOLES);
        venta.setSubTotal(5000.00);
        venta.setIgv(900.00);
        venta.setTotal(5900.00);
        venta.setObservaciones("Venta al credito a 30 dias segun OC-ANDINA-4521");
        venta.setFechaRegistro(LocalDateTime.of(2026, 9, 2, 10, 15, 0));
        venta.setUsuarioRegistro(registro);
        venta.setAnulado(false);
        venta.setMotivoAnulacion(null);
        venta.setFechaAnulacion(null);

        double[] cantidadesDespachadas = {12.000, 6.000, 25.000};
        int idxLineaVenta = 0;

        for (DetalleOrdenCompraCliente detOrd : orden.getDetalles()) {
            DetalleVenta detVenta = new DetalleVenta();

            detVenta.setId(detOrd.getId());
            detVenta.setNumeroLinea(detOrd.getNumeroLinea());
            detVenta.setProducto(detOrd.getProducto());
            detVenta.setCantidad(detOrd.getCantidad());
            detVenta.setPrecioUnitario(detOrd.getPrecioUnitario());
            detVenta.setDescuento(detOrd.getDescuento());
            detVenta.setImporte(detOrd.getImporte());

            double cantDesp = idxLineaVenta < cantidadesDespachadas.length
                    ? cantidadesDespachadas[idxLineaVenta]
                    : 0.0;

            detVenta.setCantidadDespachada(cantDesp);
            detVenta.setVenta(venta);

            venta.getDetalles().add(detVenta);
            idxLineaVenta++;
        }

        Comprobante factura = new Comprobante();
        factura.setId(1);
        factura.setVenta(venta);
        factura.setTipo(TipoComprobante.FACTURA);
        factura.setSerie("F001");
        factura.setNumero("00000123");
        factura.setFechaEmision(LocalDate.of(2026, 9, 2));
        factura.setMoneda(Moneda.SOLES);
        factura.setSubTotal(5000.00);
        factura.setIgv(900.00);
        factura.setTotal(5900.00);
        factura.setEstado(EstadoComprobante.EMITIDO);
        factura.setComprobanteRelacionado(null);
        factura.setMotivo(null);
        factura.setDetalles(new ArrayList<>());
        factura.setMedioEnvio("OSE_NUBE");
        factura.setFechaEnvio(LocalDateTime.of(2026, 9, 2, 10, 20, 0));
        factura.setFechaRegistro(LocalDateTime.of(2026, 9, 2, 10, 18, 0));

        venta.getComprobantes().add(factura);

        Despacho despacho = new Despacho();
        despacho.setId(1);
        despacho.setVenta(venta);
        despacho.setSerieGuia("T001");
        despacho.setNumeroGuia("00000045");
        despacho.setFechaDespacho(LocalDate.of(2026, 9, 3));
        despacho.setDireccionEntrega("Jr. Huaraz 1180, Cercado de Lima");
        despacho.setTransportista("Transportes Rapidos del Centro S.A.C.");
        despacho.setAnulado(false);
        despacho.setUsuarioRegistro(registro);
        despacho.setFechaRegistro(LocalDateTime.of(2026, 9, 3, 8, 30, 0));

        int idDetDespacho = 1;

        for (DetalleVenta detV : venta.getDetalles()) {
            DetalleDespacho detDesp = new DetalleDespacho();

            detDesp.setId(idDetDespacho++);
            detDesp.setProducto(detV.getProducto());
            detDesp.setCantidadDespachada(detV.getCantidadDespachada());
            detDesp.setDespacho(despacho);

            despacho.getDetalles().add(detDesp);
        }

        venta.getDespachos().add(despacho);

        Comprobante notaCredito = new Comprobante();
        notaCredito.setId(2);
        notaCredito.setVenta(venta);
        notaCredito.setTipo(TipoComprobante.NOTA_CREDITO);
        notaCredito.setSerie("FC01");
        notaCredito.setNumero("00000012");
        notaCredito.setFechaEmision(LocalDate.of(2026, 9, 4));
        notaCredito.setMoneda(Moneda.SOLES);
        notaCredito.setSubTotal(300.00);
        notaCredito.setIgv(54.00);
        notaCredito.setTotal(354.00);
        notaCredito.setEstado(EstadoComprobante.EMITIDO);
        notaCredito.setComprobanteRelacionado(factura);
        notaCredito.setMotivo("Devolucion de producto por defecto de fabrica");
        notaCredito.setMedioEnvio("OSE_NUBE");
        notaCredito.setFechaEnvio(LocalDateTime.of(2026, 9, 4, 11, 30, 0));
        notaCredito.setFechaRegistro(LocalDateTime.of(2026, 9, 4, 11, 25, 0));

        Producto prodDevuelto = venta.getDetalles().get(0).getProducto();

        DetalleNotaCredito detNC = new DetalleNotaCredito();
        detNC.setId(1);
        detNC.setNumeroLinea(1);
        detNC.setProducto(prodDevuelto);
        detNC.setCantidad(2.000);
        detNC.setPrecioUnitario(150.00);
        detNC.setDescuento(0.00);
        detNC.setImporte(300.00);
        detNC.setComprobante(notaCredito);

        notaCredito.getDetalles().add(detNC);
        venta.getComprobantes().add(notaCredito);

        String nomCliente = cliente != null
                ? cliente.getRazonSocial()
                : "Cliente no especificado";

        System.out.println("Cliente: " + nomCliente);

        System.out.println("Orden de compra: " + orden.getNumero()
                + " (Ref: " + orden.getNumeroOrdenCliente() + ") - Estado: "
                + orden.getEstado()
                + " - Total: S/ " + String.format("%.2f", orden.getTotal()));

        System.out.println("Lineas copiadas de la cotizacion: "
                + orden.getDetalles().size());

        for (DetalleOrdenCompraCliente doc : orden.getDetalles()) {
            String nomProd = doc.getProducto() != null
                    ? doc.getProducto().getNombre()
                    : "Item " + doc.getNumeroLinea();

            System.out.println("Linea " + doc.getNumeroLinea() + ": " + nomProd
                    + " | Cant: " + doc.getCantidad()
                    + " | Atendida: " + doc.getCantidadAtendida()
                    + " | P.Unit: S/ " + String.format("%.2f", doc.getPrecioUnitario())
                    + " | Imp: S/ " + String.format("%.2f", doc.getImporte()));
        }

        System.out.println("Venta: " + venta.getNumero()
                + " (" + venta.getCondicionPago()
                + " a " + venta.getPlazoCreditoDias()
                + " dias) - Estado: " + venta.getEstado()
                + " - Total: S/ " + String.format("%.2f", venta.getTotal()));

        for (DetalleVenta dv : venta.getDetalles()) {
            String nomProd = dv.getProducto() != null
                    ? dv.getProducto().getNombre()
                    : "Item " + dv.getNumeroLinea();

            System.out.println("Linea " + dv.getNumeroLinea() + ": "
                    + nomProd
                    + " | Cant. Vendida: " + dv.getCantidad()
                    + " | Cant. Despachada: " + dv.getCantidadDespachada());
        }

        System.out.println("Factura emitida: "
                + factura.getSerie() + "-" + factura.getNumero()
                + " | Estado: " + factura.getEstado()
                + " | Medio: " + factura.getMedioEnvio()
                + " | Total: S/ " + String.format("%.2f", factura.getTotal()));

        System.out.println("Despacho: Guia "
                + despacho.getSerieGuia() + "-" + despacho.getNumeroGuia()
                + " | Fecha: " + despacho.getFechaDespacho()
                + " | Transportista: " + despacho.getTransportista()
                + " | Destino: " + despacho.getDireccionEntrega());

        for (DetalleDespacho dd : despacho.getDetalles()) {
            String nomProd = dd.getProducto() != null
                    ? dd.getProducto().getNombre()
                    : "Item";

            System.out.println("Despachado: "
                    + nomProd
                    + " -> " + dd.getCantidadDespachada()
                    + " unidades");
        }

        System.out.println("Nota de Credito: "
                + notaCredito.getSerie() + "-" + notaCredito.getNumero()
                + " (Afecta a: "
                + notaCredito.getComprobanteRelacionado().getSerie()
                + "-"
                + notaCredito.getComprobanteRelacionado().getNumero()
                + ") | Motivo: " + notaCredito.getMotivo()
                + " | Total: S/ "
                + String.format("%.2f", notaCredito.getTotal()));

        for (DetalleNotaCredito dnc : notaCredito.getDetalles()) {
            String nomProd = dnc.getProducto() != null
                    ? dnc.getProducto().getNombre()
                    : "Item";

            System.out.println("Detalle NC: "
                    + nomProd
                    + " | Cant Devuelta: " + dnc.getCantidad()
                    + " | Monto: S/ "
                    + String.format("%.2f", dnc.getImporte()));
        }

        return venta;
    }
}
