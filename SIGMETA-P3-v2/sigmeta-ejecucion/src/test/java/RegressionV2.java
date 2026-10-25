import java.math.BigDecimal;
import java.time.*;
import java.sql.SQLException;
import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.socio.*;
import pe.edu.pucp.sigmeta.boimpl.producto.*;
import pe.edu.pucp.sigmeta.boimpl.compras.*;
import pe.edu.pucp.sigmeta.boimpl.cobranza.*;
import pe.edu.pucp.sigmeta.boimpl.caja.*;
import pe.edu.pucp.sigmeta.boimpl.usuario.*;
import pe.edu.pucp.sigmeta.daoimpl.producto.*;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.compras.*;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.*;
import pe.edu.pucp.sigmeta.model.socio.*;
import pe.edu.pucp.sigmeta.model.producto.*;
import pe.edu.pucp.sigmeta.model.compras.*;
import pe.edu.pucp.sigmeta.model.ventas.*;
import pe.edu.pucp.sigmeta.model.cobranza.*;
import pe.edu.pucp.sigmeta.model.enums.*;

/** Regresiones v2: ejecutar en BD desechable, despues de Main y Regression. */
public class RegressionV2 extends Regression {
    static boolean igual(double a,double b){return Math.abs(a-b)<0.0001;}
    static void rechazaSQL(Accion a,String nombre) throws Exception {
        try { a.run(); } catch(SQLException e) {
            if (!"45000".equals(e.getSQLState())) throw e;
            check(true,nombre);return;
        }
        throw new AssertionError("SQL no rechazo: "+nombre);
    }
    static CuentaPorCobrar solicitud(Venta v,Cliente c){
        CuentaPorCobrar x=new CuentaPorCobrar();x.setVenta(v);x.setCliente(c);
        x.setFechaEmision(LocalDate.now());x.setFechaVencimiento(LocalDate.now().plusYears(3));
        x.setMontoOriginal(9999);return x;
    }
    public static void main(String[] args) throws Exception {
        producto=Transaccion.ejecutar(()->new ProductoDAOImpl().load(1));
        Cliente c=cliente("10987654322",TipoPersona.NATURAL);
        c.getContacto().setNombreContacto("Persona de contacto");clientes.modificar(c,4);
        check("Persona de contacto".equals(clientes.obtener(c.getId()).getContacto().getNombreContacto()),"contacto de cliente se conserva en DatosContacto");
        ProveedorBOImpl proveedores=new ProveedorBOImpl();Proveedor prov=proveedores.obtener(1);
        prov.getContacto().setNombreContacto("Encargada compras");proveedores.modificar(prov,4);
        check("Encargada compras".equals(proveedores.obtener(1).getContacto().getNombreContacto()),"contacto de proveedor comparte DatosContacto");
        clientes.asignarLineaCredito(c.getId(),new BigDecimal("1234.56"),30,"B",4);
        check(clientes.obtener(c.getId()).getLimiteCredito().compareTo(new BigDecimal("1234.56"))==0,"limite decimal conserva centavos");
        rechaza(()->clientes.asignarLineaCredito(c.getId(),100,30,"EXCELENTE",4),"calificacion fuera de escala rechazada");
        check(clientes.obtener(c.getId()).getCalificacion()==CalificacionCrediticia.B,"calificacion valida permanece tras rechazo");
        Venta plazo=venta(c,1,10,true);plazo.setPlazoCreditoDias(31);
        rechaza(()->ventas.registrar(plazo,1),"plazo superior al autorizado rechazado");
        plazo.setPlazoCreditoDias(30);ventas.registrar(plazo,1);
        clientes.asignarLineaCredito(c.getId(),new BigDecimal("1234.56"),10,"B",4);
        check(ventas.obtener(plazo.getId()).getPlazoCreditoDias()==30,"cambio del cliente no altera plazo historico");
        clientes.asignarLineaCredito(c.getId(),new BigDecimal("1234.56"),30,"B",4);

        Compra compra=new Compra();compra.setProveedor(prov);compra.setNumero("COMPRA-V2");
        compra.setFechaEmision(LocalDate.now());compra.setEstado(EstadoCompra.REGISTRADA);
        compra.setUsuarioRegistro(new UsuarioBOImpl().obtener(4));
        compra.setSubTotal(5);compra.setIgv(0);compra.setTotal(5);
        DetalleCompra dc=new DetalleCompra();dc.setProducto(producto);dc.setNumeroLinea(1);
        dc.setCantidad(10);dc.setPrecioUnitario(20);dc.setDescuento(0);dc.setImporte(5);
        dc.setUnidadCompra(producto.getUnidadCompra());dc.setFactorConversion(producto.getFactorConversion());
        compra.getDetalles().add(dc);CompraBOImpl compras=new CompraBOImpl();compras.registrar(compra);
        check(igual(dc.getImporte(),200)&&igual(compra.getTotal(),236)&&igual(compra.getIgv(),36),"compra recalcula importe e IGV manipulados");
        DetalleCompraBOImpl detalles=new DetalleCompraBOImpl();DetalleCompra edit=detalles.obtener(dc.getId());
        edit.setCantidad(3);edit.setDescuento(5);edit.setImporte(1);detalles.modificar(edit,4);
        Compra leida=compras.obtener(compra.getId());
        check(igual(detalles.obtener(dc.getId()).getImporte(),55)&&igual(leida.getIgv(),9.90)&&igual(leida.getTotal(),64.90),"editar detalle mantiene mismo calculo de compra");
        edit.setDescuento(1000);rechaza(()->detalles.modificar(edit,4),"descuento superior al bruto rechazado");
        leida.setIgv(0);leida.setSubTotal(999);leida.setTotal(0);compras.modificar(leida,4);
        check(igual(compras.obtener(leida.getId()).getTotal(),64.90),"editar cabecera recalcula desde detalles guardados");

        Producto cambio=new ProductoBOImpl().obtener(producto.getId());
        cambio.setUnidadVenta(UnidadMedida.CAJA);cambio.setFactorConversion(2);
        rechaza(()->new ProductoBOImpl().modificar(cambio,4),"BO impide reinterpretar unidad de stock");
        rechazaSQL(()->Transaccion.ejecutar(()->new ProductoDAOImpl().update(cambio)),"SP impide reinterpretar unidad de stock");
        check(new ProductoBOImpl().obtener(producto.getId()).getUnidadInventario()==producto.getUnidadVenta(),"unidad de inventario conservada");

        CuentaPorCobrarBOImpl cuentas=new CuentaPorCobrarBOImpl();
        Venta contado=ventas.registrar(venta(c,1,10,false),1);
        rechaza(()->cuentas.insertar(solicitud(contado,c),4),"no crear deuda para venta al contado");
        Cliente otro=cliente("10987654323",TipoPersona.NATURAL);
        rechaza(()->cuentas.insertar(solicitud(plazo,otro),4),"no asociar deuda a otro cliente");
        // Simula una venta historica al credito sin CxC. Fixture por DAO, no escritura de produccion.
        Venta sinCuenta=Transaccion.ejecutar(()->{
            Venta v=venta(c,1,100,true);v.setNumero("VTA-LEGACY-V2");v.setPlazoCreditoDias(30);
            v.setSubTotal(100);v.setIgv(18);v.setTotal(118);v.setEstado(EstadoVenta.REGISTRADA);
            v.setUsuarioRegistro(new UsuarioBOImpl().obtener(1));v.setFechaRegistro(LocalDateTime.now());
            new VentaDAOImpl().save(v);DetalleVenta d=v.getDetalles().get(0);d.setVenta(v);d.setImporte(100);
            new DetalleVentaDAOImpl().save(d);return v;
        });
        rechaza(()->ventas.modificar(ventas.obtener(sinCuenta.getId()),1),"venta sin CxC denuncia integridad al modificar");
        CuentaPorCobrar recuperada=cuentas.insertar(solicitud(sinCuenta,c),4);
        check(igual(recuperada.getMontoOriginal(),118)&&recuperada.getFechaVencimiento().equals(sinCuenta.getFechaEmision().plusDays(30)),"recuperacion deriva importe y fechas de venta");
        rechaza(()->cuentas.insertar(solicitud(sinCuenta,c),4),"cuenta duplicada rechazada");
        rechaza(()->cuentas.eliminar(recuperada,4),"BO impide borrar deuda vigente");
        rechazaSQL(()->Transaccion.ejecutar(()->{new CuentaPorCobrarDAOImpl().remove(recuperada);return null;}),"SP impide borrar deuda vigente");
        ventas.anular(sinCuenta.getId(),"Prueba v2",1);
        check(cuentas.obtenerPorVenta(sinCuenta.getId())==null&&ventas.obtener(sinCuenta.getId()).isAnulado(),"anular venta retira cuenta sin cobros atomicamente");

        Venta vencida=venta(c,1,20,true);vencida.setFechaEmision(LocalDate.now().minusDays(40));
        ventas.registrar(vencida,1);CuentaPorCobrar cx=cuenta(vencida);
        // Estado almacenado intencionalmente atrasado: las consultas deben derivarlo.
        Transaccion.ejecutar(()->{cx.setEstado(EstadoCuentaPorCobrar.PENDIENTE);new CuentaPorCobrarDAOImpl().update(cx);return null;});
        check(cuentas.obtenerPorId(cx.getId()).getEstado()==EstadoCuentaPorCobrar.VENCIDA,"obtener deriva vencimiento actual");
        check(cuentas.listarPorEstado(EstadoCuentaPorCobrar.VENCIDA).stream().anyMatch(x->x.getId()==cx.getId())&&cuentas.listarVencidas().stream().anyMatch(x->x.getId()==cx.getId()),"consultas por estado y vencimiento coinciden");
        check(cuentas.listarPorEstado(EstadoCuentaPorCobrar.PENDIENTE).stream().noneMatch(x->x.getId()==cx.getId()),"cuenta vencida no aparece pendiente por estado obsoleto");
        CajaBOImpl cajas=new CajaBOImpl();if(cajas.obtenerCajaAbiertaPorUsuario(2)==null)cajas.abrirCaja(new pe.edu.pucp.sigmeta.model.caja.Caja(),2);
        cobros.registrarCobro(cobro(cx,1),2);CuentaPorCobrar parcial=cuentas.obtenerPorId(cx.getId());
        check(parcial.isVencida()&&parcial.getSituacionPago()==SituacionPago.PARCIAL,"pago parcial y vencimiento se representan a la vez");
        rechaza(()->ventas.anular(vencida.getId(),"Prueba rollback",1),"no anular venta con cobros vigentes");
        check(!ventas.obtener(vencida.getId()).isAnulado()&&igual(cuentas.obtenerPorId(cx.getId()).getMontoPagado(),1),"rollback conserva venta y deuda si anulacion falla");

        Cliente limite=cliente("10987654324",TipoPersona.NATURAL);clientes.asignarLineaCredito(limite.getId(),100,10,"A",4);
        Venta deudaVieja=venta(limite,1,60,true);deudaVieja.setFechaEmision(LocalDate.now().minusDays(30));ventas.registrar(deudaVieja,1);
        rechaza(()->ventas.registrar(venta(limite,1,60,true),1),"deuda vencida tambien consume limite de credito");
        CuentaPorCobrar refinanciada=cuenta(plazo);LocalDate nuevoVencimiento=LocalDate.now().plusDays(60);
        refinanciada.setFechaVencimiento(nuevoVencimiento);cuentas.modificar(refinanciada,4);
        Venta editar=ventas.obtener(plazo.getId());editar.getDetalles().get(0).setPrecioUnitario(15);ventas.modificar(editar,1);
        check(cuenta(plazo).getFechaVencimiento().equals(nuevoVencimiento),"editar venta no borra refinanciacion");
        editar.setFechaEmision(LocalDate.now().minusDays(1));rechaza(()->ventas.modificar(editar,1),"fecha de venta al credito no desincroniza CxC");
        check(ventas.listarPorCliente(limite.getId()).size()==1,"ventas del cliente se consultan bajo demanda");
        System.out.println("REGRESION V2: "+pruebas+" comprobaciones OK");
    }
}
