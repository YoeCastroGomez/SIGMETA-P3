package pe.edu.pucp.ejecucion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import pe.edu.pucp.model.caja.Caja;
import pe.edu.pucp.model.caja.CierreCaja;
import pe.edu.pucp.model.caja.MovimientoCaja;
import pe.edu.pucp.model.cobranza.Cobro;
import pe.edu.pucp.model.cobranza.CuentaPorCobrar;
import pe.edu.pucp.model.enums.EstadoCuentaPorCobrar;
import pe.edu.pucp.model.enums.MedioPago;
import pe.edu.pucp.model.enums.TipoMovimientoCaja;
import pe.edu.pucp.model.usuario.Usuario;
import pe.edu.pucp.model.ventas.Venta;

/**
 * ESTUDIANTE 5.
 * Tablas: caja, movimiento_caja, cierre_caja, cuenta_por_cobrar, cobro.
 *
 * Debe crear: la apertura de caja con su monto inicial, la cuenta por
 * cobrar que nace de la venta al credito, un cobro parcial, un egreso
 * manual, y el cierre comparando monto calculado contra monto declarado.
 *
 * La nota de credito no debe contarse como cobro valido en el cierre,
 * el RF010 lo pide de forma explicita (por eso el monto_calculado solo
 * se arma con los movimientos de caja, nunca con comprobantes de venta).
 */
public class PruebaCaja {

    public static void ejecutar(Venta venta, Usuario registro) {
        System.out.println("== PRUEBA 5: caja y cobranza ==");

        // ---------------------------------------------------------------------
        // 1. APERTURA DE CAJA
        // ---------------------------------------------------------------------
        Caja caja = new Caja();
        caja.setIdCaja(1);
        caja.setFechaApertura(LocalDateTime.of(2026, 9, 5, 8, 0, 0));
        caja.setMontoInicial(500.00);
        caja.setUsuarioApertura(registro);
        caja.setAbierta(true);

        // ---------------------------------------------------------------------
        // 2. CUENTA POR COBRAR (nace de la venta al credito recibida)
        // ---------------------------------------------------------------------
        LocalDate fechaEmisionCxc = venta.getFechaEmision() != null
                ? venta.getFechaEmision()
                : LocalDate.of(2026, 9, 2);
        LocalDate fechaVencimiento = fechaEmisionCxc.plusDays(venta.getPlazoCreditoDias());

        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setIdCuentaPorCobrar(1);
        cxc.setVenta(venta);
        cxc.setCliente(venta.getCliente());
        cxc.setFechaEmision(fechaEmisionCxc);
        cxc.setFechaVencimiento(fechaVencimiento);
        cxc.setMoneda(venta.getMoneda());
        cxc.setMontoOriginal(venta.getTotal());
        cxc.setMontoPagado(0.00);
        cxc.setSaldoPendiente(venta.getTotal());
        cxc.setEstado(EstadoCuentaPorCobrar.PENDIENTE);

        // ---------------------------------------------------------------------
        // 3. COBRO PARCIAL DE LA CUENTA POR COBRAR
        // ---------------------------------------------------------------------
        Cobro cobro = new Cobro();
        cobro.setIdCobro(1);
        cobro.setCuentaPorCobrar(cxc);
        cobro.setFechaCobro(LocalDate.of(2026, 9, 5));
        cobro.setMedioPago(MedioPago.TRANSFERENCIA);
        cobro.setMonto(2000.00);
        cobro.setReferencia("OP-458821");
        cobro.setUsuarioRegistro(registro);
        cobro.setFechaRegistro(LocalDateTime.of(2026, 9, 5, 9, 30, 0));

        // Relacion bidireccional
        cxc.getCobros().add(cobro);

        // Actualizacion del saldo de la cuenta por cobrar tras el cobro
        cxc.setMontoPagado(cxc.getMontoPagado() + cobro.getMonto());
        cxc.setSaldoPendiente(cxc.getMontoOriginal() - cxc.getMontoPagado());
        cxc.setEstado(cxc.getSaldoPendiente() > 0
                ? EstadoCuentaPorCobrar.PAGADA_PARCIAL
                : EstadoCuentaPorCobrar.PAGADA);

        // ---------------------------------------------------------------------
        // 4. MOVIMIENTO DE CAJA: INGRESO GENERADO POR EL COBRO
        // ---------------------------------------------------------------------
        MovimientoCaja ingreso = new MovimientoCaja();
        ingreso.setIdMovimientoCaja(1);
        ingreso.setCaja(caja);
        ingreso.setTipo(TipoMovimientoCaja.COBRO_CREDITO);
        ingreso.setFechaMovimiento(cobro.getFechaRegistro());
        ingreso.setMedioPago(cobro.getMedioPago());
        ingreso.setMonto(cobro.getMonto());
        ingreso.setConcepto("Cobro parcial CxC venta " + venta.getNumero());
        ingreso.setDocumentoOrigen("COBRO");
        ingreso.setIdDocumentoOrigen(cobro.getIdCobro());
        ingreso.setUsuarioRegistro(registro);

        caja.getMovimientos().add(ingreso);

        // ---------------------------------------------------------------------
        // 5. MOVIMIENTO DE CAJA: EGRESO MANUAL (sin documento de origen)
        // ---------------------------------------------------------------------
        MovimientoCaja egreso = new MovimientoCaja();
        egreso.setIdMovimientoCaja(2);
        egreso.setCaja(caja);
        egreso.setTipo(TipoMovimientoCaja.EGRESO_MANUAL);
        egreso.setFechaMovimiento(LocalDateTime.of(2026, 9, 5, 15, 0, 0));
        egreso.setMedioPago(MedioPago.EFECTIVO);
        egreso.setMonto(150.00);
        egreso.setConcepto("Compra de utiles de oficina");
        egreso.setDocumentoOrigen(null);
        egreso.setIdDocumentoOrigen(0);
        egreso.setUsuarioRegistro(registro);

        caja.getMovimientos().add(egreso);

        // ---------------------------------------------------------------------
        // 6. CIERRE DE CAJA DEL DIA
        //    monto_calculado = monto_inicial + ingresos - egresos, obtenido
        //    dinamicamente a partir de los movimientos de la caja (nunca de
        //    comprobantes de venta, para respetar el RF010).
        // ---------------------------------------------------------------------
        double totalIngresos = 0.0;
        double totalEgresos = 0.0;
        for (MovimientoCaja mov : caja.getMovimientos()) {
            if (mov.getTipo() == TipoMovimientoCaja.EGRESO_MANUAL) {
                totalEgresos += mov.getMonto();
            } else {
                totalIngresos += mov.getMonto();
            }
        }
        double montoCalculado = caja.getMontoInicial() + totalIngresos - totalEgresos;

        // Monto declarado por el cajero al arquear (con un faltante de 5.00
        // a proposito, para ejercitar la comparacion contra el calculado)
        double montoDeclarado = montoCalculado - 5.00;

        CierreCaja cierre = new CierreCaja();
        cierre.setIdCierreCaja(1);
        cierre.setCaja(caja);
        cierre.setFechaCierre(LocalDateTime.of(2026, 9, 5, 20, 0, 0));
        cierre.setMontoCalculado(montoCalculado);
        cierre.setMontoDeclarado(montoDeclarado);
        cierre.setDiferencia(montoDeclarado - montoCalculado);
        cierre.setUsuarioCierre(registro);

        // Al cerrar, la caja deja de estar abierta
        caja.setAbierta(false);

        // ---------------------------------------------------------------------
        // 7. IMPRESION POR CONSOLA DEL FLUJO COMPLETO
        // ---------------------------------------------------------------------
        System.out.println("Apertura de caja #" + caja.getIdCaja()
                + " el " + caja.getFechaApertura()
                + " con monto inicial S/ " + String.format("%.2f", caja.getMontoInicial()));

        System.out.println("Cuenta por cobrar de la venta " + venta.getNumero()
                + " | Monto original: S/ " + String.format("%.2f", cxc.getMontoOriginal())
                + " | Vence: " + cxc.getFechaVencimiento());

        System.out.println("Cobro registrado: S/ " + String.format("%.2f", cobro.getMonto())
                + " (" + cobro.getMedioPago() + ", ref. " + cobro.getReferencia() + ")");
        System.out.println("  CxC tras el cobro -> Pagado: S/ " + String.format("%.2f", cxc.getMontoPagado())
                + " | Saldo: S/ " + String.format("%.2f", cxc.getSaldoPendiente())
                + " | Estado: " + cxc.getEstado());

        System.out.println("Movimientos de caja:");
        for (MovimientoCaja mov : caja.getMovimientos()) {
            System.out.println("  [" + mov.getTipo() + "] S/ " + String.format("%.2f", mov.getMonto())
                    + " - " + mov.getConcepto() + " (" + mov.getMedioPago() + ")");
        }

        System.out.println("Cierre de caja del " + cierre.getFechaCierre()
                + " | Calculado: S/ " + String.format("%.2f", cierre.getMontoCalculado())
                + " | Declarado: S/ " + String.format("%.2f", cierre.getMontoDeclarado())
                + " | Diferencia: S/ " + String.format("%.2f", cierre.getDiferencia()));

        boolean cuadrada = cierre.getDiferencia() == 0.0;
        System.out.println("  Caja cuadrada: " + cuadrada
                + (cuadrada ? "" : " (hay un descuadre para revisar)"));
    }
}
