# SIGMETA · Frontend Blazor (Lab 9) — proyecto base del grupo

Blazor Web App, **.NET 10**, Bootstrap. Solo capa de presentación (Lab 9):
sin lógica de negocio, sin consultas ni registros, sin BD ni servicios web. La navegación sí funciona.
Las tablas muestran datos de ejemplo fijos, que se reemplazan al integrar con Java por servicios web.

## Abrir
1. Visual Studio 2026 → abrir `SigmetaWeb.slnx` → F5.
2. En `/login`, "Demostración: entrar como" muestra el sistema con el menú de cada rol (RF001).
   Para cambiar de rol: "Cerrar sesión" y entrar con otro. El login no valida la clave.

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
wwwroot/app.css            estilos comunes (no tocar sin avisar)
```
Cada carpeta tiene un `Pendientes.razor` con las rutas que faltan. Al hacer una pantalla real, se borra su `@page` de ese archivo.

Reglas completas: ver el documento de reparto del Lab 9.
