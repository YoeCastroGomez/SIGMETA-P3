# SIGMETA · Frontend Blazor (Lab 9) — proyecto base del grupo

Blazor Web App, **.NET 10**, Bootstrap. Es solo la capa de presentación: no tiene lógica de negocio ni acceso a la base.
Todas las pantallas (RF001 a RF015) consumen el servicio REST de Java (`sigmeta-ws`) con `Servicios/ApiJava.cs`.
Para usarla hay que tener GlassFish encendido con `sigmeta-ws` desplegado y la base con sus procedimientos (incluido `07_SP_reforma.sql`).
Las tablas muestran datos de ejemplo fijos, que se reemplazan al integrar con Java por servicios web.

## Moneda de los importes

Las pantallas que presenten o reciban importes monetarios deben indicar **soles peruanos (PEN, S/)** y no ofrecer un selector de divisas ni conversiones por tipo de cambio. El formulario de linea de credito ya identifica su importe con S/. Las pantallas pendientes de ventas, cotizaciones, compras y caja deben seguir el mismo criterio cuando se implementen.

## Abrir
1. Visual Studio 2026 → abrir `SigmetaWeb.slnx` → F5.
2. En `/login` se inicia sesión con un usuario de la base. Con los datos de demostración la clave de todos es `Meta2026`
   (`lrojas` Vendedor, `mchavez` Cajero, `jtorres` Almacenero, `aquispe` Administrador). El menú depende del rol (RF001).

## Estructura
```
Components/
  Layout/    MainLayout (barra + menú), NavMenu (4 menús fijos, uno por rol), LoginLayout
  Shared/    Encabezado, Campo (marca obligatorios *), Estado, PantallaPendiente
  Pages/
    Seguridad/  Yoe        RF001, RF002, RF014
    Clientes/   Yoe        RF003
    Catalogo/   Claudia    RF004, RF005, RF006, RF007 (cotizaciones)
    Compras/    Luis Daniel RF011
    Almacen/    Luis Daniel RF012
    Ventas/     Sebastian  RF007 (órdenes), RF008, RF013, despachos
    Caja/       Adriano    RF009, RF010
    Reportes/   Adriano    RF015
wwwroot/app.css            solo los colores del tema y el marco (no tocar sin avisar)
```
Cada carpeta tiene un `Pendientes.razor` con las rutas que faltan. Al hacer una pantalla real, se borra su `@page` de ese archivo.

Las pantallas usan solo clases de Bootstrap (card, row, col, btn, table, badge, alert).
Reglas completas y tabla de clases: ver el documento de reparto del Lab 9.
