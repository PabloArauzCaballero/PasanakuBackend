---
name: web-angular
description: "Escribir las superficies web de AportaYa con Angular: componentes standalone con señales, zoneless, resource y httpResource para los cuatro estados, Signal Forms con validadores del contrato, HttpClient solo en nucleo/ con interceptores de idempotencia y sesión, un enchufe de rutas por dominio con carga perezosa, Angular CDK para tablas y accesibilidad, @aportaya/ui como biblioteca, y @angular/ssr con rutas híbridas e hidratación incremental para el sitio público. Úsala al crear o modificar cualquier componente, servicio, ruta o página de apps/backoffice, apps/web o packages/ui."
---

# Web con Angular

Dos productos, una tecnología: el **backoffice** (SPA estática detrás de login) y el
**sitio público** (SSR híbrido, indexable). La decisión es
[[ADR-044 Frontend en Angular y Flutter]]; lo que el sitio tiene que ser lo fija
[[ADR-041 Sitio público · el tercer producto]]. El **comportamiento** del backoffice
(tablas, expedientes, plazos, permisos) lo manda `web-backoffice`; lo **visual**,
`disenar-frontend`. Esta skill manda la **forma del código Angular**.

## Estructura

```
apps/backoffice/src/app/
├── app.config.ts            provideZonelessChangeDetection · provideRouter(rutas, withComponentInputBinding()) · provideHttpClient(withInterceptors([...]))
├── app.routes.ts            UN ENCHUFE POR DOMINIO (loadChildren) — lo escribe F6 y se congela
├── nucleo/                  interceptores · sesion · permisos · errores · idempotencia · conexion · registro-de-acceso · borrador
├── layout/                  shells (financiero, sistemas): menú por rol, cabecera, sección
└── rutas/<dominio>/         operacion · cumplimiento · sistemas · contabilidad · publicidad
    ├── <dominio>.routes.ts  las rutas de este dominio, con sus guardias `canMatch`
    ├── dominio/             un archivo por CU: cuNN-<verbo>.ts sobre clientes/angular
    ├── textos.ts            los textos de este dominio
    └── <pagina>/            un componente por página, que compone organismos

apps/web/                    igual, más:
├── contenido/**/*.md        la fuente del contenido, con frontmatter validado
├── scripts/contenido.mjs    md → contenido.json, espejos .md, llms.txt, sitemap
└── src/app/
    ├── app.routes.server.ts RenderMode por ruta: Prerender por omisión, Server donde hay datos vivos
    ├── paginas/ · verificadores/ · seo/ (W1) · geo/ (W2)

packages/ui/                 biblioteca @aportaya/ui — una entrada secundaria por componente (@aportaya/ui/boton/boton), sin barril
packages/tokens/generado/tokens.css   GENERADO — el único lugar con literales
clientes/angular/<servicio>/          GENERADO desde el OpenAPI — servicios inyectables; no se edita
```

## Cómo se escribe un componente

- **Standalone, siempre.** Sin `NgModule`.
- **Señales, siempre.** `input()`, `output()`, `computed()`, `linkedSignal()`; nada de
  `@Input()` con setters ni `BehaviorSubject` para estado de vista.
- **Zoneless.** No hay `detectChanges()` a mano ni `setTimeout` para «forzar» un
  repintado; si hace falta, el estado está en el lugar equivocado.
- **Control de flujo nativo** (`@if`, `@for` con `track`, `@switch`, `@defer`); nunca
  `*ngIf`/`*ngFor`.
- **Estilos con `var(--…)`** de `tokens.css`. Un hex o un `px` en un `.css` o en un
  `[style]` es un fallo de lint.
- **`host:`** para clases de estado y atributos ARIA; nada de `HostBinding` decorado.
- Un componente de más de 150 líneas (plantilla incluida) mezcla niveles.

```ts
@Component({
  selector: 'ap-fila-reclamo',
  imports: [Monto, ChipEstado, Fecha],
  host: { '[class.vencido]': 'vencido()', 'role': 'row' },
  template: `…`,
})
export class FilaReclamo {
  reclamo = input.required<ReclamoResumen>();       // tipo de clientes/angular, nunca reescrito
  vencido = computed(() => this.reclamo().vence < hoy());
  abrir = output<string>();
}
```

## Estado de servidor: `resource` y los cuatro estados

Las lecturas son `httpResource()` (o `resource()` sobre el servicio generado) en
`dominio/`, con las entradas como señales:

```ts
// rutas/operacion/dominio/cu52-reclamos.ts
export function reclamos(filtros: Signal<FiltrosCU52>) {
  return httpResource<PaginaCU52>(() => ({ url: rutaCU52, params: filtros() }));
}
```

La pantalla **no pinta los estados a mano**: usa la directiva estructural
`*apEstadoDePantalla` de `@aportaya/ui`, que recibe el `ResourceRef`, el predicado de
vacío y el texto en voz de marca, y es la única que sabe dibujar cargando, vacío, error
con reintento y éxito (más «vacío por permiso» y «vacío por filtro», que son distintos).

Invalidación **explícita** por acción (`recurso.reload()`); nada de recargar todo.

## Formularios

- **Signal Forms** de Angular, con el modelo tipado desde `clientes/angular`.
- Validadores **derivados del contrato**: `validarConContrato(esquemaCU)` traduce
  `required`, `minLength`, `pattern`, `enum` del OpenAPI. No se reescribe una regla.
- Error por campo, en el campo, con `aria-describedby`; anunciado por `LiveAnnouncer`.
- `claveDeIdempotencia()` se genera al abrir el formulario, viaja por
  `HttpContextToken` y **no se regenera al reintentar**.
- El botón de envío se deshabilita y muestra progreso mientras la mutación está en
  curso; deshabilitar no reemplaza a explicar qué falta.
- Formularios largos guardan borrador en `IndexedDB` vía `ServicioBorrador`, sin datos
  personales en claro.

## HTTP: solo en `nucleo/`

Ningún componente inyecta `HttpClient`. Los interceptores funcionales de `nucleo/`:

| Interceptor | Qué hace |
| --- | --- |
| `trazaInterceptor` | `x-request-id` en cada petición |
| `sesionInterceptor` | bearer desde memoria; `401 → un refresh → un reintento`; si falla, cierre global |
| `idempotenciaInterceptor` | lee la clave del `HttpContext` y la manda como `Idempotency-Key` |
| `erroresInterceptor` | `AP-CU<NN>-<nn>` → mensaje de `textos.*`; un `403` responde «no tenés acceso» y nada más |
| `accesoADatosInterceptor` | las rutas marcadas registran el acceso (CU-58, `R-SEG-02`) |
| `reintentoInterceptor` | reintento con retroceso **solo** para `GET` |

El token del operador vive **solo en memoria**; el refresh, en cookie `HttpOnly`.
Jamás `localStorage`.

## Rutas: el enchufe por dominio

`app.routes.ts` lo escribe el shell **una vez** con un `loadChildren` por directorio de
dominio, apuntando a un `<dominio>.routes.ts` que empieza vacío. Cada carril llena el
suyo. Las guardias son funciones (`canMatch`) que leen `ServicioSesion.permisos()`:
**ocultan por comodidad; el servidor protege**. El estado de una tabla (página, orden,
filtros) va en la URL y entra al componente por `withComponentInputBinding()`.

**Prueba del gate:** agregar una ruta vacía no toca `app.routes.ts`, `nucleo/` ni
`layout/`. Y `dist/` no contiene el cliente de un servicio que la ruta cargada no usa.

## Angular CDK, no Angular Material

El sistema de diseño es propio. Del CDK se usa: `a11y` (`cdkTrapFocus`,
`LiveAnnouncer`, `FocusMonitor`), `overlay` (modales, menús), `scrolling`
(`cdk-virtual-scroll-viewport` para toda lista larga), `table` (`cdk-table` para
`TablaDeDatos`) y `SelectionModel`. **Nada de Angular Material**: mete un segundo
sistema de tokens que después nadie saca.

## Sitio público: SSR híbrido

- `app.routes.server.ts` declara `RenderMode.Prerender` por omisión y `Server` solo en
  las rutas con datos vivos (`/verificar/*`, `/publico/*`, tarifario, contrato, estado
  regulatorio), con `headers` (`X-Robots-Tag`, `Cache-Control`) por ruta. **Hay una
  prueba que enumera las rutas `Server`** y falla si aparece una nueva sin cambiarla.
- `provideClientHydration(withIncrementalHydration(), withEventReplay())`. Los
  verificadores van en `@defer (hydrate on viewport)`; el contenido indexable **nunca**
  dentro de un `@defer`.
- El contenido vive en Markdown; `scripts/contenido.mjs` lo procesa **antes** del build.
- `budgets` en `angular.json`: el *bundle* inicial rompe el build si pasa 150 KB
  comprimidos. Las páginas de contenido no cargan ningún cliente de API.
- Con el gateway caído, una ruta `Server` responde `200` con `EstadoError`, no `503`.

## Pruebas

| Nivel | Herramienta | Archivo |
| --- | --- | --- |
| Átomo · molécula | Vitest + Angular Testing Library | `*.spec.ts` |
| Organismo | Vitest + ATL + `provideHttpClientTesting` con los ejemplos de `packages/simulado` | `*.spec.ts` |
| Contrato | `ajv` contra el esquema del OpenAPI | `cuNN.contrato.spec.ts` |
| Accesibilidad | `vitest-axe` como error | `*.a11y.spec.ts` |
| E2E | Playwright + `@axe-core/playwright` | `e2e/*.e2e.spec.ts` |
| Visual | Playwright screenshots de `/catalogo`, claro y oscuro | `*.visual.spec.ts` |
| Rendimiento | Lighthouse CI (sitio) | `lighthouserc.json` |

## Lint (`angular-eslint` + reglas propias)

`sin-red-en-vista` · `tipos-del-contrato` · `sin-literal-de-diseno` ·
`sin-formato-de-dinero` · `capas-front` (`eslint-plugin-boundaries`) ·
`tamano-componente` · `sin-console-log` · las reglas de plantilla de accesibilidad de
`@angular-eslint/template` **como error** (`click-events-have-key-events`,
`interactive-supports-focus`, `label-has-associated-control`, `alt-text`, …).

## Antipatrones

- `HttpClient` o `fetch` en un componente.
- `NgModule`, `*ngIf`, `@Input()` con lógica, `BehaviorSubject` como estado de vista.
- Angular Material «porque ya trae tabla».
- Un `routes.ts` central que un carril de pantallas edita.
- Traer diez mil filas para filtrar en el navegador.
- Un `toFixed` o un `Intl.NumberFormat` fuera de `Monto`.
- Token en `localStorage`.
- Un verificador fuera de `@defer`, o contenido indexable dentro de uno.
- `detectChanges()` a mano en un proyecto *zoneless*.

## Ver también

`web-backoffice` · `disenar-frontend` · `movil-flutter` · `errores-api` · `glosario-dominio` ·
`arquitectura-atomica` · `contratos-api` · `dinero-decimal` · `idempotencia-reintentos` ·
`seguridad-aplicacion` · [[ADR-044 Frontend en Angular y Flutter]] ·
[[ADR-041 Sitio público · el tercer producto]] · [[ADR-042 Política de rastreadores de IA]] ·
[[Flujo de pantallas · backoffice administrador]] · `docs/Arquitectura/Prompts/Prompt de frontend.md`
