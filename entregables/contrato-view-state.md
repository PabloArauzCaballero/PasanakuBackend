# Contrato de estado de vista — releído contra el árbol real

> Publicado por Leo, carril PR8 (`leo/frontend/dialogo-estados`), turno noche 2026-09-21.
> Reemplaza la hipótesis heredada de `mantra-core-health` citada en el encargo (AMB-F3).

## 1. Dónde vive de verdad

No existe un tipo de unión (`ViewState`) publicado y separado del componente. El contrato de
estado vive **implementado dentro de** `packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts`
(`EstadoDePantalla<T>`), que consume directamente `ResourceRef<T | undefined>` de Angular (versión
instalada: `@angular/core@22.1.5`) y deriva sus ramas de `ResourceStatus` (tipo real, confirmado en
`node_modules/@angular/core/types/_api-chunk.d.ts:149`):

```ts
type ResourceStatus = 'idle' | 'error' | 'loading' | 'reloading' | 'resolved' | 'local'
```

`EstadoDePantalla` ya es, de hecho, **el host de estados** que pide H2: 22 pantallas reales de
`apps/web` y `apps/backoffice` ya lo consumen hoy (confirmado por búsqueda de
`import { EstadoDePantalla }`, no por grep suelto — cada uno de los 22 resuelve por el compilador
al mismo módulo). El comentario de `pantalla-de-billetera.ts:12` ya lo describe con estas palabras:
*"Sus cuatro estados los pinta `EstadoDePantalla`"*. Ese es el dato duro del repo, no una opinión
mía: el propio código ya declara **cuatro** estados, no diez.

**Decisión H1.S1.M4:** se **extiende** `EstadoDePantalla` (no se adopta tal cual, no se extrae uno
nuevo). Motivo: ya cumple el rol de host único; lo que falta no es un host — es que dos ramas
(vacío y "recargando") pinten peor de lo que el propio repo ya tiene resuelto en otra parte
(`EstadoVacio`, nunca conectado) y de lo que el tipo real de Angular ya distingue (`reloading` vs
`loading`, hoy colapsados).

## 2. Comparación contra las diez variantes heredadas

| Variante heredada (documento antecedente, `mantra-core-health`) | Estado real en Pasanaku | Evidencia |
|---|---|---|
| Cargando | **confirmada** — status `loading` y `reloading` colapsados hoy bajo `isLoading()` | `estado-de-pantalla.ts:21` |
| Error | **confirmada** — status `error`, con `ErrorTraducido { mensaje, trazaId?, sinConexion?, estado? }` | `estado-de-pantalla.ts:25-30,70-74` |
| Vacío | **confirmada, pero con implementación pobre duplicada** — existe un branch inline sin acción de siguiente paso, mientras que `packages/ui/src/estado-vacio/estado-vacio.ts` (`EstadoVacio`) ya resuelve título + porqué + acción por `MotivoVacio`, y **nunca se usa desde `EstadoDePantalla`** | `estado-de-pantalla.ts:31-34` vs `estado-vacio.ts` completo |
| Listo (éxito) | **confirmada** — `else { <ng-content /> }`, cubre los status `resolved` y `local` | `estado-de-pantalla.ts:35-37` |
| Pendiente de autenticación de ruta | **no existe acá, y no debe existir acá** — es responsabilidad de los guards de ruta (`canMatch`, ver `panel-de-aprobacion.ts:16`), no del estado de un recurso ya montado | búsqueda de `canMatch` en `apps/backoffice/src/app/rutas/publicidad/campanas/campanas.routes.ts` (uso real) |
| Validación | **no existe acá, y no debe existir acá** — es del formulario (`H4`, contrato de borrador), no del estado de lectura de un recurso | — |
| Sin permiso | **se llama distinto y vive partido en dos lugares**: como `MotivoVacio: 'porPermiso'` (cuando el backend devuelve una lista vacía por rol) y como `ErrorTraducido.estado === 403` (cuando el backend rechaza la petición). Hoy ninguno de los dos casos se distingue visualmente del resto de su rama | `estado-vacio.ts:4,38-47`, `estado-de-pantalla.ts:7` |
| No encontrado | **se llama distinto**: `ErrorTraducido.estado === 404` ya viaja en la carga útil del error, pero `EstadoDePantalla` no lo distingue del resto de los errores (mismo mensaje genérico, mismo botón "Volver a intentar" — que no tiene sentido para un 404) | `estado-de-pantalla.ts:70-74` |
| Obsoleto (stale) | **existe con otro nombre**: es el status `reloading` de `ResourceStatus`. Hoy `isLoading()` (helper de Angular) es verdadero para `loading` **y** `reloading` por igual, así que una recarga en segundo plano borra el contenido ya mostrado y lo reemplaza por el esqueleto — pierde el dato que ya tenía | `_api-chunk.d.ts:149`, `estado-de-pantalla.ts:21` |
| Sin conexión | **existe con otro nombre**: `ErrorTraducido.sinConexion?: boolean` ya viaja en la carga útil, puesto por el interceptor de errores (`nucleo/errores.interceptor.ts`), pero `EstadoDePantalla` no lo renderiza distinto de un error genérico | `estado-de-pantalla.ts:7` |

**Ninguna carga útil se pierde**: `mensajeVacio`, `motivoVacio`, `etiquetaDeCarga`, `reintentar`,
`trazaId`, `sinConexion` y `estado` siguen intactos después de H2 (ver diff de
`estado-de-pantalla.ts`).

## 3. Lo que cambia en H2 (host, un solo lugar)

1. El branch vacío pasa a delegar en `<ap-estado-vacio>` (ya existente, ya con "acción de
   siguiente paso" resuelta) en vez de repetir un `<p>` suelto. **Cambio de apariencia
   explicado**: ahora puede aparecer un botón de acción cuando `motivoVacio() === 'porFiltro'`
   (antes nunca aparecía ninguno). Para los 22 consumidores que no pasan una acción explícita, el
   comportamiento visual no cambia — `EstadoVacio.accion` es `undefined` para `sinDatos` y
   `porPermiso` salvo que el consumidor la pase.
2. El status `reloading` deja de caer en el mismo esqueleto que `loading`: si ya hay un valor
   (`hasValue()`), se sigue mostrando el contenido proyectado con una marca `aria-busy="true"` y un
   indicador discreto en vez de reemplazarlo por el esqueleto. Si todavía no hay valor (primera
   carga), se comporta igual que antes.
3. Los errores con `estado === 404` ocultan el botón "Volver a intentar" (reintentar una petición
   a un recurso que no existe no tiene sentido). **El mensaje no se reescribe**: el interceptor de
   errores de cada app ya traduce 404 y sin conexión a texto humano
   (`apps/backoffice/src/app/nucleo/errores.ts:19-28` — confirmado leyendo el código, no
   reinventado); el organismo compartido solo lee `estado` para decidir si ofrece reintentar. Un
   primer intento de este cambio reescribía el mensaje en el organismo y rompía el test existente
   `pantalla-de-billetera.spec.ts` ("sin red: no se queda cargando para siempre", que espera el
   texto exacto del interceptor) — se corrigió antes de correr la suite, no después de verla fallar
   en CI.
4. El cambio de rama se anuncia por región viva (`aria-live="polite"` en el contenedor de estado,
   `role="alert"` se mantiene en error para anuncio inmediato).

## 3bis. Caso negativo del test de exhaustividad (H1.S2.M3)

`RAMA_DE` en `estado-de-pantalla.ts` está tipado `Record<ResourceStatus, …>`: quitarle una clave
debe romper `yarn typecheck` (todas las claves son obligatorias en un `Record` sobre una unión
cerrada). El sondeo real —sacar `reloading`, correr `yarn typecheck`, pegar el error, revertir— y
su salida literal quedan en `evidencia/H1-S2-M3-exhaustividad.txt` (regla: no se afirma sin haber
corrido el comando).

## 4. No cubierto en este documento

- No se agregó un tipo de unión discriminada nuevo exportado (`export type EstadoDeVista<T> = …`)
  porque haría dos fuentes de verdad con `ResourceStatus` de Angular, que ya es la unión real y
  minimalista. La "exhaustividad" se prueba contra `ResourceStatus` (los 6 valores reales), no
  contra una lista inventada de diez.
- La variante "obsoleto" definida acá es una interpretación de `reloading`, no una entidad
  separada — es la única lectura consistente con el tipo real de Angular; se registra por si
  produce necesita otro nombre de cara al usuario.
