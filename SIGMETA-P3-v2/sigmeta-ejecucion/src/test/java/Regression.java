import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import pe.edu.pucp.sigmeta.boimpl.Transaccion;
import pe.edu.pucp.sigmeta.boimpl.socio.*;
import pe.edu.pucp.sigmeta.boimpl.ventas.*;
import pe.edu.pucp.sigmeta.boimpl.cobranza.*;
import pe.edu.pucp.sigmeta.boimpl.caja.*;
import pe.edu.pucp.sigmeta.boimpl.usuario.*;
import pe.edu.pucp.sigmeta.daoimpl.producto.*;
import pe.edu.pucp.sigmeta.daoimpl.ventas.*;
import pe.edu.pucp.sigmeta.daoimpl.cobranza.*;
import pe.edu.pucp.sigmeta.model.socio.*;
import pe.edu.pucp.sigmeta.model.producto.*;
import pe.edu.pucp.sigmeta.model.ventas.*;
import pe.edu.pucp.sigmeta.model.cobranza.*;
import pe.edu.pucp.sigmeta.model.usuario.*;
import pe.edu.pucp.sigmeta.model.enums.*;
import pe.edu.pucp.sigmeta.seguridad.SesionServicio;

/** Pruebas de integracion. Ejecutar SOLO sobre una base local de pruebas recien instalada. */
public class Regression {
    static final ClienteBOImpl clientes = new ClienteBOImpl();
    static final VentaBOImpl ventas = new VentaBOImpl();
    static final CobroBOImpl cobros = new CobroBOImpl();
    static Producto producto;
    static int pruebas;
    interface Accion { void run() throws Exception; }
    static void check(boolean ok, String nombre) {
        if (!ok) throw new AssertionError(nombre);
        System.out.println("OK " + (++pruebas) + ": " + nombre);
    }
    static void rechaza(Accion a, String nombre) throws Exception {
        try { a.run(); } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            check(true, nombre); return;
        }
        throw new AssertionError("No rechazo: " + nombre);
    }
    static Cliente cliente(String documento, TipoPersona tipo) throws Exception {
        Cliente c = new Cliente(); c.setTipoPersona(tipo);
        c.setTipoDocumento(TipoDocumentoIdentidad.RUC); c.setNumeroDocumento(documento);
        if (tipo == TipoPersona.NATURAL) { c.setNombres("Juan"); c.setApellidos("Prueba"); }
        else c.setRazonSocial("Empresa prueba " + documento);
        return clientes.registrar(c,4);
    }
    static Venta venta(Cliente c, double cantidad, double precio, boolean credito) {
        Venta v = new Venta(); v.setCliente(c); v.setFechaEmision(LocalDate.now());
        v.setCondicionPago(credito ? CondicionPago.CREDITO : CondicionPago.CONTADO);
        DetalleVenta d = new DetalleVenta(); d.setNumeroLinea(1); d.setProducto(producto);
        d.setCantidad(cantidad); d.setPrecioUnitario(precio); v.getDetalles().add(d); return v;
    }
    static CuentaPorCobrar cuenta(Venta v) throws Exception {
        return Transaccion.ejecutar(() -> new CuentaPorCobrarDAOImpl().obtenerPorVenta(v.getId()));
    }
    static Cobro cobro(CuentaPorCobrar cuenta, double monto) {
        Cobro c = new Cobro();c.setCuentaPorCobrar(cuenta);c.setMonto(monto);
        c.setMedioPago(MedioPago.EFECTIVO);return c;
    }
    static int concurrentes(int n, Accion accion) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch inicio = new CountDownLatch(1); AtomicInteger exitos = new AtomicInteger();
        List<Future<?>> fs = new ArrayList<>();
        try {
            for (int i=0;i<n;i++) fs.add(pool.submit(() -> {
                try { inicio.await(); accion.run(); exitos.incrementAndGet(); }
                catch (IllegalArgumentException | IllegalStateException e) { /* rechazo de negocio esperado */ }
                catch (Exception e) { throw new RuntimeException(e); }
            }));
            inicio.countDown(); for (Future<?> f:fs) f.get(30,TimeUnit.SECONDS);
            return exitos.get();
        } finally { pool.shutdownNow(); }
    }
    public static void main(String[] args) throws Exception {
        producto = Transaccion.ejecutar(() -> new ProductoDAOImpl().load(1));
        Cliente natural = cliente("10987654321",TipoPersona.NATURAL);
        check("Juan Prueba".equals(clientes.obtener(natural.getId()).getNombreMostrado()),"persona natural con RUC conserva nombre");
        clientes.asignarLineaCredito(natural.getId(),100,30,"A",4);
        check(concurrentes(2, () -> ventas.registrar(venta(natural,1,60,true),1))==1,"dos ventas concurrentes no exceden credito");
        Cliente juridica = cliente("20987654321",TipoPersona.JURIDICA);
        clientes.asignarLineaCredito(juridica.getId(),1000,30,"A",4);
        Venta v = ventas.registrar(venta(juridica,1,100.0/1.18,true),1);
        CuentaPorCobrar cxc = cuenta(v);
        check(juridica.getRazonSocial().equals(cxc.getCliente().getNombreMostrado()),"CxC juridica mapea nombre");
        Venta vn = ventas.listarTodos().stream().filter(x -> x.getCliente().getId()==natural.getId()).findFirst().orElseThrow();
        check("Juan Prueba".equals(cuenta(vn).getCliente().getNombreMostrado()),"CxC natural mapea nombre");
        CajaBOImpl cajas = new CajaBOImpl();
        if (cajas.obtenerCajaAbiertaPorUsuario(2)==null)cajas.abrirCaja(new pe.edu.pucp.sigmeta.model.caja.Caja(),2);
        check(concurrentes(2, () -> cobros.registrarCobro(cobro(cxc,60),2))==1,"dos cobros concurrentes no exceden saldo");
        CuentaPorCobrar despues = cuenta(v);
        check(Math.abs(despues.getMontoPagado()-60)<0.001 && Math.abs(despues.getSaldoPendiente()-(v.getTotal()-60))<0.001,"saldo coherente con cobros");
        Cobro registrado = cobros.listarPorCuentaPorCobrar(cxc.getId()).get(0);
        check(concurrentes(2, () -> cobros.eliminar(registrado,2))==1,"doble anulacion produce una sola compensacion");
        check(Math.abs(cuenta(v).getSaldoPendiente()-v.getTotal())<0.001,"anulacion restaura saldo exacto");
        Venta repetida = venta(natural,producto.getStockActual()*0.6,1,false);
        DetalleVenta otra= new DetalleVenta();otra.setNumeroLinea(2);otra.setProducto(producto);
        otra.setCantidad(producto.getStockActual()*0.6);otra.setPrecioUnitario(1);repetida.getDetalles().add(otra);
        rechaza(() -> ventas.registrar(repetida,1),"stock agregado de producto repetido");
        OrdenCompraCliente oc = Transaccion.ejecutar(() -> {
            OrdenCompraCliente o = new OrdenCompraCliente();o.setCliente(natural);o.setNumero("OCC-REGRESSION");
            o.setFechaEmision(LocalDate.now());o.setEstado(EstadoOrdenCompraCliente.PENDIENTE);
            o.setSubTotal(10);o.setIgv(1.8);o.setTotal(11.8);
            Usuario u = new Usuario();u.setId(1);o.setUsuarioRegistro(u);o.setFechaRegistro(LocalDateTime.now());
            new OrdenCompraClienteDAOImpl().save(o);
            DetalleOrdenCompraCliente d = new DetalleOrdenCompraCliente();d.setOrdenCompraCliente(o);
            d.setProducto(producto);d.setNumeroLinea(1);d.setCantidad(10);d.setPrecioUnitario(1);d.setImporte(10);
            new DetalleOrdenCompraClienteDAOImpl().save(d);return o;
        });
        Venta ajena = venta(juridica,1,1,false);ajena.setOrdenCompraCliente(oc);
        rechaza(() -> ventas.registrar(ajena,1),"orden de otro cliente rechazada");
        Venta ocho = venta(natural,8,1,false);ocho.setOrdenCompraCliente(oc);ventas.registrar(ocho,1);
        Venta exceso = venta(natural,3,1,false);exceso.setOrdenCompraCliente(oc);
        rechaza(() -> ventas.registrar(exceso,1),"acumulado 8+3 supera orden de 10");
        Venta modificada = ventas.obtener(ocho.getId());modificada.setCliente(juridica);
        rechaza(() -> ventas.modificar(modificada,1),"modificar cliente conserva correspondencia con orden");
        modificada.setCliente(natural);modificada.getDetalles().get(0).setCantidad(6);ventas.modificar(modificada,1);
        Venta cuatro = venta(natural,4,1,false);cuatro.setOrdenCompraCliente(oc);ventas.registrar(cuatro,1);
        check(cuatro.getId()>0,"modificar excluye su propia cantidad del acumulado");
        Set<String> numeros = ConcurrentHashMap.newKeySet();
        check(concurrentes(8, () -> numeros.add(ventas.registrar(venta(natural,1,1,false),1).getNumero()))==8 && numeros.size()==8,"ocho correlativos concurrentes unicos");
        SesionServicio sesion = SesionServicio.iniciar("lrojas","Meta2026");
        rechaza(() -> sesion.ejecutar(id -> new UsuarioBOImpl().verificarRol(4,TipoRol.ADMINISTRADOR)),"sesion impide suplantar ID de administrador");
        check(sesion.ejecutar(id -> new UsuarioBOImpl().verificarRol(id,TipoRol.VENDEDOR)).getId()==1,"sesion deriva responsable autenticado");
        sesion.cerrar();rechaza(() -> sesion.ejecutar(id -> id),"sesion cerrada rechazada");
        System.out.println("REGRESION: "+pruebas+" comprobaciones OK");
    }
}
