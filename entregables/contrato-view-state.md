# Contrato de estado de pantalla — releído contra el código real (H1.S2.M1)

> **Peldaño de evidencia:** `DISCOVERED` (regla 30). Todo lo que sigue está citado con ruta y
> línea del repo real `PasanakuBackend`, leído el 2026-09-22, SHA `a23bcb117effe7ce66d069a97ebd2c25c8390306`
> (rama `dev` remota, ver `H1.S1.M1`). **No hay un tipo de estado (`ViewState`/enum) separado en el
> repo**: lo que documenta este archivo es el contrato real que ya existe, repartido en dos
> organismos de `packages/ui/src`.

## 0. Corrección de la hipótesis heredada (AMB-F3)

El documento antecedente (otro proyecto, usado solo como plantilla de estructura) supone **diez**
variantes con nombre propio: pendiente de autenticación de ruta, cargando, vacío, listo,
validación, sin permiso, no encontrado, obsoleto, sin conexión y error.

**Eso no existe así en Pasanaku.** Lo que existe es:

- Un organismo `EstadoDePantalla<T>` (`packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts`)
  que resuelve **cuatro ramas** sobre un `ResourceRef<T>` de Angular: `cargando` (línea 21),
  `error` (línea 25), `vacío` (línea 31, incluye el caso `idle` — sin petición todavía, línea 63) y
  `listo` (línea 36, `<ng-content>`).
- Un organismo `EstadoVacio` (`packages/ui/src/estado-vacio/estado-vacio.ts`) que **no es una rama
  del host**: es una molécula de contenido para pintar *dentro* de la rama vacía o en listas, con
  un `motivo: MotivoVacio` (`'sinDatos' | 'porFiltro' | 'porPermiso'`, línea 4 de
  `estado-de-pantalla.ts`) que trae su propio texto de "por qué" y su acción (líneas 34-47 de
  `estado-vacio.ts`).
- Un tipo de carga útil de error, `ErrorTraducido` (línea 7 de `estado-de-pantalla.ts`):
  `{ mensaje: string; trazaId?: string; sinConexion?: boolean; estado?: number }`. **`sinConexion`
  y `estado` (código HTTP) viajan como *propiedades del error*, no como ramas propias.**

## 1. Tabla comparativa — heredada vs. real

| Variante heredada (AMB-F3) | Veredicto | Evidencia |
|---|---|---|
| Pendiente de autenticación de ruta | **No existe** | No hay ninguna rama de "auth pendiente" en `estado-de-pantalla.ts` ni en `estado-vacio.ts`. Es responsabilidad de un guard de ruta (fuera de este organismo) — no localizado ningún `CanActivate`/resolver equivalente en el alcance de este carril; no se afirma su existencia ni su ausencia fuera de `packages/ui`. |
| Cargando | **Confirmada** | `estado-de-pantalla.ts:21` — `@if (recurso().isLoading())`, con `etiquetaDeCarga()` accesible (`role="status"`, línea 22). |
| Vacío | **Confirmada, con matiz** | `estado-de-pantalla.ts:31` decide *que* está vacío (predicado `vacio()` + `idle`, línea 59-68); el *contenido* del vacío (por qué + acción) lo pinta `EstadoVacio` (otro organismo, no una plantilla de esta rama) o el propio consumidor con `mensajeVacio` (input string simple, línea 54, no una plantilla). |
| Listo | **Se llama distinto — "con datos" es la rama `else` (`ng-content`)** | `estado-de-pantalla.ts:36`. No tiene nombre propio en el código; es simplemente el contenido proyectado por defecto. |
| Validación | **No existe como rama de este host** | Es un caso de `H4` (contrato de formulario), no del host de pantalla. No hay rama de "validación" en `estado-de-pantalla.ts`. |
| Sin permiso | **Se llama distinto — es un `MotivoVacio`, no una rama propia** | `estado-vacio.ts:4,46`: `'porPermiso'` es un motivo *dentro* de la rama vacía, con texto fijo ("Tu rol no puede ver esta información.") y sin acción. Un 403 real del backend no está mapeado automáticamente a este motivo: alguien tiene que decidirlo y pasarlo. |
| No encontrado | **No existe como rama propia — cae en `error` con `estado: 404`** | `ErrorTraducido.estado?: number` (línea 7) permite transportar el código HTTP, pero `estado-de-pantalla.ts` no distingue 404 de 500 en su plantilla (líneas 25-30): ambos pintan el mismo bloque `role="alert"` con el mismo botón "Volver a intentar" — reintentar un 404 no tiene sentido y hoy no se filtra. |
| Obsoleto | **No existe** | Ningún campo de "dato desactualizado" ni de fecha de corte en `ErrorTraducido` ni en `EstadoDePantalla`. Si una vista necesita indicar que el dato mostrado quedó viejo, hoy no hay dónde ponerlo. |
| Sin conexión | **Confirmada como propiedad del error, no como rama propia** | `ErrorTraducido.sinConexion?: boolean` (línea 7) existe en el tipo, pero la plantilla (líneas 25-30) no lo usa: no hay texto ni icono distinto para sin conexión vs. error genérico. El campo está declarado y no consumido — es carga útil sin plantilla. |
| Error | **Confirmada** | `estado-de-pantalla.ts:25-30`, `role="alert"`, mensaje traducido (`error()` computed, línea 70-74, con fallback genérico si el interceptor no adjuntó `mensaje`) + `trazaId` opcional (línea 28) + botón "Volver a intentar" (línea 29). **No expone el error crudo del backend** (regla 95.2.3): siempre pasa por `ErrorTraducido`. |

## 2. Contrato real vigente (lo que hay que preservar, no reducir)

Reducir esto a `cargando/error/éxito` está expresamente prohibido por el encargo, y además sería
**menos** de lo que ya existe hoy: el repo ya tiene cargando / error (con `trazaId` y `sinConexion`
como carga útil) / vacío (con motivo y acción) / listo. Cuatro ramas de primer nivel, dos de ellas
con carga útil rica.

```ts
// packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts (real, no propuesto)
export type MotivoVacio = 'sinDatos' | 'porFiltro' | 'porPermiso'
export type ErrorTraducido = { mensaje: string; trazaId?: string; sinConexion?: boolean; estado?: number }
// Ramas del host: cargando (isLoading) · error (ErrorTraducido) · vacío (idle | predicado+MotivoVacio) · listo (ng-content)
```

## 3. Brechas reales para H2 (no se inventan, se registran)

1. El host **no** recibe plantillas con contexto tipado por rama: solo `ng-content` para "listo" y
   `mensajeVacio` como string plano para "vacío". H2.S1.M1 tiene que resolver esto sin romper a los
   **11 consumidores actuales** (`grep` de `ap-estado-de-pantalla` en `apps/`, ver evidencia).
2. `sinConexion` y `estado` (código HTTP) están declarados en `ErrorTraducido` pero **no
   consumidos** por la plantilla: hoy un 404, un 500 y un "sin conexión" se ven idénticos.
3. No hay rama "obsoleto" ni carga útil de fecha de corte.
4. `role="status"` / `role="alert"` dan región viva **implícita** (`status` implica
   `aria-live="polite"`, `alert` implica `aria-live="assertive"` — ARIA 1.2 §5.2.7/§5.2.1), pero no
   hay `aria-live` explícito ni verificación de que el cambio de rama se anuncie al cambiar
   `@if`/`@else` (Angular destruye/crea el nodo; el lector de pantalla depende de que el contenedor
   con el rol persista o de que el navegador anuncie el nodo nuevo). **No verificado en runtime**
   (ningún E2E de accesibilidad corrido en esta sesión — ver bloqueo de entorno en el reporte).

## 4. No cubierto

- No se ejecutó ningún test contra este contrato en esta sesión (ver bloqueo de `node_modules`/git
  en el `REPORTE.md` del carril). Esta tabla es `DISCOVERED`, no `TESTED`.
- No se revisó `apps/movil` (Flutter): el organismo de este contrato es Angular puro
  (`packages/ui`), fuera del alcance declarado.
- No se confirmó la existencia o ausencia de un guard de "pendiente de autenticación de ruta" fuera
  de `packages/ui` (no está en el alcance IN de este carril).
