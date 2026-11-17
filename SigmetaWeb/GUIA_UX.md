# Guía de interfaz de SIGMETA (patrón UX)

Referencia para que todas las pantallas se usen igual. Las pantallas de ventas (RF007, RF008, RF013, RF014)
ya lo aplican: cópialas como ejemplo.

## La idea

Cada pantalla responde tres preguntas **sin que el usuario tenga que pensar**:

1. **¿En qué estado está esto?** → `<Estado>`, `<PasosDocumento>`, `<ChipsFiltro>`
2. **¿Qué puedo hacer ahora?** → una acción principal visible; `<BotonCondicionado>` bloquea lo que no se puede y dice por qué
3. **¿Qué sigue y quién lo hace?** → `<SiguientePaso>` después de guardar y el responsable en `<PasosDocumento>`

Las reglas de negocio **siguen en el BO**. La pantalla solo las anticipa con datos que ya trae el Java.
Si el BO rechaza algo igual, su mensaje sale en `<AvisoError>` como siempre.

## Componentes (carpeta `Components/Shared`)

| Componente | Para qué | Ejemplo en |
|---|---|---|
| `<PasosDocumento Pasos="..." />` | Recorrido del documento: hecho / actual / pendiente y **a quién le toca** | `ComprobanteVenta`, `OrdenForm`, `CotizacionForm` |
| `<BotonCondicionado Texto Motivo AlHacerClic />` | Botón que se **bloquea** con `Motivo` y lo explica (en vez de dejar hacer clic y recibir un 409) | `ComprobanteVenta` (factura sin RUC, anular con despacho) |
| `<SiguientePaso Mensaje Detalle TextoAccion Ruta />` | Aviso verde de éxito **con el siguiente paso** y su botón | `ComprobanteVenta`, `OrdenForm`, `Cotizaciones` |
| `<ChipsFiltro Opciones Seleccionado SeleccionadoChanged />` | Filtros rápidos por estado **con su cantidad** | todas las listas de ventas |
| `<ModalMotivo Visible Titulo AlConfirmar AlCancelar>…</ModalMotivo>` | Ventana única para anular / rechazar / corregir: dice **qué va a pasar** y exige motivo | `Ventas`, `Cotizaciones`, `NotasCredito` |
| `<ListaVacia Titulo Detalle TextoAccion Ruta AlQuitarFiltros />` | Lista sin resultados con salida (crear o quitar filtros) | todas las listas de ventas |

Tipos y ayudas en `ComponentesFlujo.cs`: `PasoDocumento`, `EstadoPaso`, `OpcionChip`,
`Tonos.Clase("ambar")` (colores de etiqueta), `Busqueda.Coincide(texto, campos…)` (sin tildes ni mayúsculas),
`Volver.A(...)` / `Volver.ConAviso(...)` (volver a la lista con sus filtros).

## Reglas para listas

- **Chips por estado con su cantidad** arriba de la tabla. Si el RF pide filtrar en el BO (como cotizaciones), el chip llama al Java con el filtro.
- **Búsqueda rápida** (número, cliente, documento) con `Busqueda.Coincide`.
- **Filtros y página en la dirección** (`?estado=…&q=…&pagina=…`) con `Nav.GetUriWithQueryParameters` y `NavigateTo(..., replace: true)`.
  Así, al volver de un formulario la lista está igual. Ver `SincronizarUrl()` en `Ventas.razor`.
- Los enlaces a un formulario llevan `?volver=` con la dirección actual (`Volver.Parametro(RutaActual)`).
- **Una sola acción principal por fila** (botón relleno) según el estado y el rol; lo demás como enlaces (`btn-link`).
- Columna de **situación + siguiente paso** cuando el documento pasa por varios roles ("Cobrar en caja · Cajero").

## Reglas para formularios

- **Secciones numeradas** en el orden en que se llenan (1, 2, 3).
- **Resumen fijo a la derecha** (`position: sticky`): totales, **lista de lo que falta** (✓ / •) y el botón Guardar
  bloqueado con `BotonCondicionado` hasta que todo esté completo.
- Mostrar **información útil al decidir** (línea de crédito del cliente, stock en almacén, lo pendiente en la orden)
  y **bloquear opciones imposibles** (por ejemplo, «Crédito» si el cliente no tiene línea).
- Modo **solo consulta** claramente marcado y con la razón ("Tiene el comprobante F001-12: anúlelo primero").
- Al guardar, **ir al siguiente paso** (venta → emitir comprobante, orden → registrar venta) o volver con `Volver.ConAviso`.

## Colores (los mismos de `<Estado>`)

verde = listo / vigente · ámbar = falta algo / por vencer · azul = en curso · rojo = anulado / rechazado · gris = trámite normal

## Estilos

Los estilos de cada componente van en su `.razor.css` (CSS aislado) y usan las variables de Bootstrap
(`var(--bs-primary)`, `var(--bs-border-color)`…), así funcionan en tema claro y oscuro sin tocar `app.css`.
