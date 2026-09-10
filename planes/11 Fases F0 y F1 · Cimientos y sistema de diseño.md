---
tags:
  - plan
  - fase
  - frontend
titulo: "Fases F0 y F1 — Cimientos y sistema de diseño (Flutter y Angular)"
fases: [F0.0, F0, F1-W, F1-M]
depende_de: []
habilita: [F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12]
---

# Fases F0.0, F0 y F1 — Maqueta, cimientos y sistema de diseño

> **Se ejecuta en:** F0.0 ya está hecha. F0 se parte en **un paso troncal** (P1) y
> **tres andamiajes concurrentes** (P3 Flutter, P4 Angular backoffice, P5 Angular sitio).
> F1 se parte en **dos carriles paralelos**: `F1-W` (biblioteca Angular) y `F1-M`
> (paquete Flutter). Ver [[16 Carriles de frontend]] y el tramo `TF` de
> [[17 Plan de acción secuencial · coordinación de cinco máquinas]].

> [!important] Antes de escribir la primera línea
> [[10b Estándar de ejecución del frontend]] y
> [[00b Estándar de ejecución · código limpio, pruebas y calidad]] aplican en las tres
> fases: regla cero, composición atómica, KISS, tokens, los cuatro estados,
> accesibilidad. **Se declara cada componente por nivel antes de crearlo.**

> [!note] Lo que ya existía y qué pasa con eso
> Los andamiajes de F0 (Expo, React + Vite, Astro) y el primer corte de F1
> (`packages/ui` con dos renderizadores) se hicieron con el stack anterior. **No se
> tiran a ciegas**: [[16 Carriles de frontend]] §12 lista qué se conserva tal cual
> (tokens, prueba contra la bóveda, vectores de `Monto`, `contratos-a-json`, la prueba
> de «una pantalla nueva no toca nada compartido»), qué se porta (los átomos, como
> criterio de aceptación) y qué se borra (todo lo específico de React, Expo, MSW y Astro).

---

# FASE F0.0 — Maqueta navegable con backend simulado

> **Estado: hecha** (agosto de 2026). Entregable:
> [[AportaYa-Maqueta|docs/Views/AportaYa-Maqueta.html]] + su ficha. **No cambia con el
> stack**: la maqueta es HTML de referencia, no código de `apps/`, y es la
> especificación visual que F1-W y F1-M reproducen, cada una en su mundo.

## Por qué fue la primera fase y no un adorno

1. **Quien financia decide sobre algo que puede tocar**, no sobre un documento de pantallas.
2. **Adelanta las decisiones caras al momento en que son baratas.** Cuántas pestañas, dónde
   vive pagar el aporte, si el saldo se relee o se ajusta.
3. **Convierte la prosa en criterio de aceptación.** F1 no discute cómo se ve una
   `TarjetaSaldo`: la compara contra la maqueta.

## Las seis reglas de la maqueta — siguen vigentes

| # | Regla |
| :-: | --- |
| 1 | **Los tokens se copian de `estilos.css`**, no se reinterpretan |
| 2 | **Rutas, códigos de error y servicio dueño salen de la bóveda** |
| 3 | **Ningún dato es real** y el pie lo dice |
| 4 | **Tres escenarios de red**: todo bien · intermitente con reintento idempotente · rechazo con el código del CU |
| 5 | **El saldo se relee del backend**, nunca se ajusta en memoria |
| 6 | **Todo hueco o divergencia se declara**, no se completa con una suposición |

## Cómo la consumen las fases siguientes

| Fase | Qué toma de la maqueta |
| --- | --- |
| **F0.3** Servidor simulado | Las **rutas, los códigos de error y los tres escenarios**: los ejemplos de Prism los reproducen |
| **F1-W / F1-M** | El **criterio de aceptación visual** de cada átomo, molécula y organismo, en los dos mundos |
| **F2–F8** | Cada pantalla se compara contra la suya; lo que difiera se justifica o se corrige |

La primera corrida destapó ocho defectos en la bóveda antes de que existiera código que
los heredara; la lista está en el historial de este documento y en [[Seguridad]] (S-8, S-9).

---

# FASE F0 — Cimientos del frontend

> **Objetivo.** Que `yarn dev:movil`, `yarn dev:backoffice` y `yarn dev:web` levanten
> los tres productos, cada uno mostrando una pantalla real con sus cuatro estados contra
> **el mismo servidor simulado** derivado de los contratos. Sin una sola pantalla de
> negocio todavía.

## Gate de entrada

- [ ] Fase 0 del backend cerrada: el monorepo yarn, el `tsconfig.base.json`, el lint y
      el CI ya existen
- [ ] [[ADR-044 Frontend en Angular y Flutter]] escrito e indexado
- [ ] El `openapi/identidad.yaml` existe con al menos la operación de CU-01 escrita
- [ ] Flutter estable y Android SDK instalados en P3, P4 y P5; Xcode en P1 (§F0.6)

> [!note] Esta fase se ejecuta en cuatro pasos, uno troncal y tres en paralelo
> **F0.T** es del troncal (P1) y va primero: sin tokens generados, clientes generados y
> Prism, los tres andamiajes no tienen contra qué levantar. **F0-M, F0-B y F0-W** son
> tres directorios nuevos y corren a la vez sin colisión.

## Leer antes

`docs/Arquitectura/ADR-044 Frontend en Angular y Flutter.md` ·
`docs/Arquitectura/ADR-041 Sitio público · el tercer producto.md` ·
`docs/Arquitectura/Prompts/Prompt de frontend.md` · `docs/Views/AportaYa-Identidad.md` ·
`docs/Views/Sistema-Diseno/` · skills `disenar-frontend`, `movil-flutter`, `web-angular`,
`web-backoffice`, `entorno-monorepo`

## F0.T · Troncal — lo que los tres andamiajes necesitan primero

Lo hace **P1 por micro-PR**, en este orden, y cada paso tiene su prueba.

### F0.T.1 · `packages/tokens` — la fuente única

```
packages/tokens/
├── tokens.json                  primitivas + roles por tema (claro, oscuro) — ÚNICO archivo con literales
├── vectores/
│   └── monto.json               [{ entrada: "1240.00", salida: "Bs 1.240,00" }, …] — 5.000 casos deterministas
├── scripts/
│   ├── a-css.mjs                → generado/tokens.css   (:root, prefers-color-scheme, data-theme)
│   └── a-dart.mjs               → generado/tokens.dart  (class Tokens extends ThemeExtension<Tokens>)
├── generado/                    no se versiona; lo produce `yarn workspace @aportaya/tokens build`
└── pruebas/
    ├── tokens-contra-boveda.spec.ts   lee docs/Views/Sistema-Diseno/estilos.css y falla si diverge
    └── generados-coinciden.spec.ts    los dos generados tienen exactamente las mismas claves
```

Se **mueve** desde `packages/ui/src/tokens/` (`paleta.ts`, `temas.ts`) y su prueba,
que ya existen y ya pasan. Lo nuevo es el generador de Dart y el JSON de vectores.

**La forma de `tokens.json`**, para que los dos generadores lean lo mismo:

```json
{
  "primitivas": {
    "color":   { "g600": "#1C5A3A", "o500": "#E5852B", "ink": "#10231A", "…": "…" },
    "espacio": { "s1": 4, "s2": 8, "s3": 12, "s4": 16, "s5": 24, "s6": 32, "s7": 48 },
    "radio":   { "sm": 8, "md": 12, "lg": 16, "xl": 24, "pill": 999 },
    "sombra":  { "1": "0 1px 2px rgba(16,35,26,.06)", "2": "…", "3": "…" },
    "fuente":  { "display": "Poppins", "cuerpo": "Inter", "mono": "JetBrains Mono" },
    "tactil":  { "minimo": 48 }
  },
  "roles": {
    "claro":  { "brand": "g600", "accent": "o500", "text": "ink", "brand-texto": "g700", "accent-texto": "o700", "ok-texto": "…", "…": "…" },
    "oscuro": { "brand": "g400", "accent": "o400", "text": "…", "…": "…" }
  }
}
```

Reglas: **un rol apunta a una primitiva por nombre**, nunca a un hex (así el tema oscuro
no puede inventar un color); los roles `-texto` existen porque `accent`, `muted`, `ok` y
`brand` en oscuro no llegan a AA como texto chico (`disenar-frontend` §0); y la prueba
`generados-coinciden` falla si `tokens.css` y `tokens.dart` no tienen exactamente las
mismas claves, en los dos temas.

### F0.T.2 · Dos clientes generados desde el mismo contrato

`buildSrc/src/main/kotlin/aportaya.openapi.gradle.kts` deja de generar `typescript-fetch`
y genera dos:

| Tarea | Generador | Salida | Opciones que importan |
| --- | --- | --- | --- |
| `generarClienteAngular` | `typescript-angular` | `clientes/angular/<servicio>/` | `providedIn=root` · `stringEnums=true` · `withInterfaces=true` · `modelPropertyNaming=original` |
| `generarClienteDart` | `dart-dio` | `clientes/dart/<servicio>/` | `serializationLibrary=json_serializable` · `useEnumExtension=true` |

`./gradlew generateOpenApiClients` corre las dos para los catorce servicios. **Los
importes quedan como `string` en los dos** (los contratos ya los declaran
`type: string, format: decimal`); la prueba `importes-son-cadena` lo verifica en cada
regeneración. El CI regenera y falla si hay diferencia, como antes.

### F0.T.3 · `packages/simulado` — Prism y los ejemplos por CU

Lo que ya existe se conserva: `scripts/contratos-a-json.mjs` (los contratos como JSON
por servicio), `escenarios.ts` (los tres escenarios) y `muestra.ts` (el generador de
muestras desde el esquema). Lo que cambia es **quién sirve**:

```
packages/simulado/
├── ejemplos/<servicio>/CU-NN.json     un archivo por CU, con los tres escenarios:
│                                       { "ok": {…}, "intermitente": {…}, "rechazo": { "codigo": "AP-CU21-03", … } }
├── scripts/
│   ├── contratos-a-json.mjs           (existía)
│   ├── generar-ejemplos.mjs           produce ejemplos/ desde el esquema con `muestra.ts`; se editan a mano SOLO los valores de negocio
│   └── inyectar-ejemplos.mjs          escribe los ejemplos en `examples:` de una copia del OpenAPI para Prism
├── prism.yml                          los catorce contratos en un solo puerto, con `--errors` (valida cada respuesta contra el esquema)
└── src/
    ├── escenarios.ts                  (existía) — el escenario se elige con la cabecera `Prefer: example=<escenario>`
    └── fixtures.ts                    exporta los ejemplos tipados para Vitest; Dart los lee del mismo JSON
```

`yarn dev:mock` levanta Prism en `:4010`. **Los tres productos apuntan ahí en
desarrollo**, y las pruebas de componente y widget consumen los mismos JSON sin red.

**La forma de un archivo de ejemplos**, un CU por archivo y un escenario por clave:

```json
{
  "operacion": "pagarAporte",
  "cu": "CU-21",
  "escenarios": {
    "ok":           { "estado": 200, "cuerpo": { "obligacion_id": "obl_9k2…", "estado": "PAGADA", "importe": "250.00", "…": "…" } },
    "aceptado":     { "estado": 202, "cuerpo": { "obligacion_id": "obl_9k2…", "estado": "EN_PROCESO" } },
    "intermitente": { "estado": 503, "cuerpo": { "codigo": "AP-GW-503" }, "luego": "ok" },
    "rechazo":      { "estado": 409, "cuerpo": { "codigo": "AP-CU21-03", "mensaje": "…" } },
    "adverso":      { "estado": 200, "cuerpo": { "…": "el estado del negocio en el día malo (D-2 de la maqueta)" } }
  }
}
```

`inyectar-ejemplos.mjs` los escribe como `examples:` con nombre en la copia del OpenAPI
que sirve Prism, y el cliente elige con `Prefer: example=intermitente`. `intermitente`
lleva `luego`: la **primera** llamada con efecto corta con `503` y la siguiente con la
misma `Idempotency-Key` responde `ok` — es exactamente el escenario de red de la
maqueta, y es lo que prueba que el reintento no duplica. `adverso` es el escenario de
negocio del mando de la maqueta: mora, encaje bajo el mínimo, fondo cubriendo,
restauración vencida. **Toda pantalla se prueba con `ok` y con `adverso`.**

> **Por qué Prism y no MSW.** MSW intercepta `fetch` en JavaScript: sirve a Angular y
> no a Flutter. Prism es un proceso HTTP que **cualquier cliente** consume, valida cada
> respuesta contra el esquema (`--errors`) y elige el escenario por cabecera. El barrido
> 17 de [[19 Contrato de carril · conflicto cero, skills y calidad verificada]] pasa de
> «mocks sin duplicar» a **«un CU, un archivo de ejemplos»**: sigue siendo un solo recurso
> compartido, ahora consumido por dos lenguajes.

### F0.T.4 · Tareas de raíz y CI

`package.json` de la raíz:

```
dev:mock          prism mock -d packages/simulado/prism.yml -p 4010
dev:movil         yarn workspace @aportaya/movil dev          → flutter run --dart-define=API=http://localhost:4010
dev:backoffice    yarn workspace @aportaya/tokens build && ng serve backoffice
dev:web           yarn workspace @aportaya/tokens build && ng serve web (con SSR de desarrollo)
lint · typecheck · test:front · test:a11y · test:e2e · build     → turbo
```

`apps/movil/package.json` es un **envoltorio**: `lint` → `flutter analyze --fatal-infos && dart run custom_lint`,
`typecheck` → `dart analyze`, `test:front` → `flutter test test/`, `test:a11y` → `flutter test test/a11y`,
`test:e2e` → `patrol test`, `build` → `dart run build_runner build --delete-conflicting-outputs`.
Así `turbo` no distingue mundos.

**Lo generado en Dart se versiona.** `riverpod_generator`, `go_router_builder` y
`json_serializable` producen `*.g.dart`; se **commitean**, y el CI corre `build_runner` y
falla si hay diferencia, con la misma regla que `clientes/`. Así un carril que hace
`git pull` compila sin correr nada, y una máquina sin `build_runner` al día no rompe la
rama de otra.

El CI suma un job `frontend` con **cuatro pasos**: `yarn install --immutable` +
`flutter pub get` · `./gradlew generateOpenApiClients` sin diff · `yarn lint && yarn typecheck`
· `yarn test:front && yarn test:a11y`. Lighthouse y `seo:validar` solo en `apps/web`.

### F0.T.5 · Lo que la ejecución de TF fijó, y que el plan no había previsto

| Decisión | Por qué |
| --- | --- |
| **Los clientes se generan en serie y sin caché** (`--no-parallel --no-build-cache`) | El generador `dart-dio` de openapi-generator no es seguro entre hilos: catorce servicios en paralelo dejaron los modelos de `aportes` dentro de `nucleo-financiero`, y la caché de Gradle los sirvió mezclados en la corrida siguiente. El CI lo hace igual |
| **El cliente Dart fija `sdk: '>=3.8.0'`** en un `doLast` de Gradle | El generador escribe `>=3.5.0`, pero `json_serializable` emite elementos null-aware (`?instance.x`), que son Dart 3.8; sin esto `build_runner` no genera ningún `.g.dart` con campos opcionales |
| **Los `.g.dart` del cliente los produce `yarn workspace @aportaya/movil build`** (`build_runner` en `clientes/dart/<servicio>`) | Solo el que la app consume; `clientes/` no se versiona, así que el paso corre en cada máquina y en el CI |
| **Prism sirve un solo documento fusionado** (`generado/prism/todos.yaml`) con el prefijo `/api/v1` escrito en cada ruta y los componentes prefijados por servicio | Prism corre un documento por proceso y no aplica un `servers.url` relativo; fusionar es lo que mantiene **una sola base URL** en los tres clientes |
| **El escenario se pide con `Prefer: code=<estado>, example=<escenario>`** | Prism elige la respuesta por código y el ejemplo por nombre; `rechazo` (4xx), `aceptado` (202) e `intermitente` (503) necesitan el código además del nombre. `cabeceraDeEscenario(escenario, estado)` lo arma |
| **`Idempotency-Key` con forma UUID** | El contrato la declara `format: uuid` y Prism valida la petición (`--errors`): una clave hexadecimal suelta es un 422 antes de llegar a la operación |
| **Riverpod sin reintentos automáticos** (`ProviderScope(retry: (_, __) => null)`) | Riverpod 3 reintenta solo un proveedor que falló, con retroceso, y mientras tanto lo muestra cargando: en una pantalla de dinero eso esconde el error. La política del proyecto es reintento manual y visible (10b §2) |
| **`Radios`, no `Radio`, en `tokens.dart`** | `Radio` es un widget de Flutter; el nombre chocaba en cada import |
| **Los roles de `tokens.json` nombran primitivas** (`"brand": "g600"`) y la prueba rechaza un hex que ya exista como primitiva | Es lo que impide que un tema «invente» un color escribiéndolo a mano |
| **Escala de bordes en los tokens** (`borde-fino 1`, `borde-foco 3`, `borde-desfase 2`) | Sin ella, todo `1px solid` y todo `outline: 3px` era un literal que el barrido tenía que perdonar |
| **Las reglas propias corren como barrido de texto** (`scripts/verificar_frontend.py`) en el `lint` de cada app | `custom_lint` (Flutter) y las reglas de `angular-eslint` propias entran con F1-M y F1-W; el barrido ya bloquea hoy |
| **`@aportaya/ui` se consume por alias de `tsconfig`** (`@aportaya/ui/*` → `packages/ui/src/*`), no compilada con `ng-packagr` | Una entrada por componente sin barril, como pide el conflicto nº 2; el empaquetado como biblioteca entra en F1-W |
| **Los presupuestos de `angular.json` se miden en bytes crudos** (backoffice 400 kB, sitio 550 kB) | Angular mide crudo; el gate de 150 KB de planes/19 §6 es comprimido y lo mide Lighthouse. Hoy: backoffice 73 KB, sitio 85 KB comprimidos |

## F0.1 · Los tres andamiajes

```
apps/
├── movil/                          FLUTTER · P3
│   ├── pubspec.yaml · .fvmrc · package.json (envoltorio)
│   ├── lib/
│   │   ├── main.dart · app.dart
│   │   ├── navegacion/            go_router: shell con tab bar, deep links, UN ENCHUFE POR DOMINIO
│   │   ├── proveedores/           sesión · tema · conexión · idempotencia · biometría (Riverpod)
│   │   ├── dominio/               cu01_registrar.dart … · cliente dio con interceptores · puertos/
│   │   ├── infraestructura/{android,ios}/
│   │   ├── pantallas/<dominio>/   con su rutas.dart y su textos.dart
│   │   └── organismos/            solo los que dependen de una API nativa
│   ├── test/{unidad,widget,a11y,contrato,goldens}/
│   ├── integration_test/          Patrol
│   └── android/ · ios/
├── backoffice/                     ANGULAR · P4
│   ├── angular.json · package.json
│   └── src/app/
│       ├── nucleo/                interceptores · sesión · permisos · errores · idempotencia · conexión
│       ├── layout/                shell: menú por rol, cabecera, sección
│       ├── rutas/<dominio>/       <dominio>.routes.ts + páginas + su dominio/ + textos.ts
│       ├── app.routes.ts          UN ENCHUFE POR DOMINIO (loadChildren), fijado por F6
│       └── app.config.ts          provideZonelessChangeDetection, provideRouter, provideHttpClient(withInterceptors)
└── web/                            ANGULAR + SSR · P5
    ├── angular.json · package.json · server.ts
    ├── contenido/{paginas,legal,faq,tarifas}/*.md    fuente del contenido, frontmatter validado
    ├── scripts/contenido.mjs      md → generado/contenido.json + espejos .md + llms.txt + sitemap
    └── src/app/
        ├── nucleo/ · layout/
        ├── paginas/<pagina>/      una carpeta por página
        ├── verificadores/         los cuatro organismos con `@defer (hydrate on viewport)`
        ├── seo/                   ServicioMeta, JSON-LD (W1)
        ├── geo/                   política, guía de redacción (W2)
        ├── app.routes.ts · app.routes.server.ts   (RenderMode por ruta)
        └── app.config.ts · app.config.server.ts
packages/
├── tokens/                         F0.T.1
├── ui/                             biblioteca Angular @aportaya/ui — F1-W
├── diseno_flutter/                 paquete Dart aportaya_diseno — F1-M
├── simulado/                       F0.T.3
└── dominio-cliente/                TS: átomos del sorteo y la cadena con vectores dorados (F9)
clientes/
├── angular/<servicio>/             generado · sin dueño
└── dart/<servicio>/                generado · sin dueño
```

> **El enchufe por dominio es lo que reemplaza al enrutamiento por archivos.** Ni
> `go_router` ni Angular Router descubren rutas por el sistema de archivos. Para que un
> carril agregue pantallas **sin editar un registro compartido**, el shell deja
> **escrito de antemano** un enchufe por directorio de dominio:
>
> ```dart
> // apps/movil/lib/navegacion/rutas.dart  (F2 lo escribe UNA vez y se congela)
> final rutas = [...rutasIdentidad, ...rutasBilletera, ...rutasAlianzas, ...rutasPasanaku, ...rutasSoporte, ...rutasNotificaciones];
> ```
> ```ts
> // apps/backoffice/src/app/app.routes.ts  (F6 lo escribe UNA vez y se congela)
> { path: 'operacion',    loadChildren: () => import('./rutas/operacion/operacion.routes') },
> { path: 'cumplimiento', loadChildren: () => import('./rutas/cumplimiento/cumplimiento.routes') },
> { path: 'sistemas',     loadChildren: () => import('./rutas/sistemas/sistemas.routes') },
> { path: 'contabilidad', loadChildren: () => import('./rutas/contabilidad/contabilidad.routes') },
> { path: 'publicidad',   loadChildren: () => import('./rutas/publicidad/publicidad.routes') },
> ```
>
> Cada carril posee **su** `rutas.dart` / `<dominio>.routes.ts`, que empieza vacío. La
> prueba del gate es la misma de antes: **agregar una pantalla vacía no toca ningún
> archivo fuera del directorio del carril.**

## F0.2 · La capa de dominio sobre el cliente generado

Un archivo por caso de uso en el `dominio/` de cada app, que **envuelve** el cliente
generado y lo tipa desde el contrato:

```dart
// apps/movil/lib/dominio/cu21_pagar_aporte.dart
@riverpod
Future<SalidaCU21> pagarAporte(Ref ref, EntradaCU21 entrada, {required String claveIdempotencia}) =>
    ref.read(clienteAportesProvider).pagarAporte(entrada, headers: {'Idempotency-Key': claveIdempotencia});
```

```ts
// apps/backoffice/src/app/rutas/operacion/dominio/cu58-exportar.ts
export function exportar(filtros: Signal<FiltrosCU58>) {
  return httpResource<SalidaCU58>(() => ({ url: rutaCU58, params: filtros() }));
}
```

Reglas:
- **Los tipos vienen del cliente generado.** Nunca se declaran a mano (invariante 2).
- Cada mutación acepta y reenvía la **clave de idempotencia**.
- La respuesta se **valida** contra el esquema OpenAPI en desarrollo y en pruebas: si la
  API devuelve algo que no encaja, se descubre acá y no en la pantalla.
- Errores traducidos: `AP-CU<NN>-<nn>` → mensaje en voz de marca, en un catálogo
  versionado por producto.
- `x-request-id` viaja en cada request.
- **Una sola base URL: el gateway.** `401` ⇒ un refresh, un reintento, y si falla, sesión
  cerrada global. Vive en el interceptor de `dio` (Flutter) y en el de `nucleo/` (Angular).

## F0.3 · Una pantalla real por producto

La misma rebanada vertical que ya probó el andamiaje anterior, ahora en el stack nuevo:

| Producto | Pantalla | Contra | Prueba |
| --- | --- | --- | --- |
| `apps/movil` | `PantallaDeSaldo` (CU-13) | Prism, escenario por cabecera | 5 widget tests: cargando, éxito, vacío, error, sin red · 1 golden claro/oscuro · 1 a11y |
| `apps/backoffice` | `PantallaDeBilletera` con `cuentaId` en la URL | Prism | 6 specs: cargando, éxito, vacío, error, sin red, `403` |
| `apps/web` | `/plazos` (calcular plazo hábil, la única lectura pública con contrato) en `RenderMode.Server` | Prism | 5 specs + prueba que enumera las rutas `Server` |

## F0.4 · Herramientas de calidad

| Pieza | Flutter | Angular |
| --- | --- | --- |
| Unitarias y componente | `flutter_test`, `test/` por nivel | Vitest + Angular Testing Library, proyectos `unidad`, `componente`, `contrato`, `a11y` |
| Simulado | `http_mock_adapter` con `packages/simulado/ejemplos` | `provideHttpClientTesting` con `fixtures.ts` |
| Accesibilidad | `meetsGuideline(...)` en `test/a11y` | `vitest-axe` como **error** |
| Visual | goldens en claro y oscuro, `--update-goldens` solo con revisión | Playwright screenshots del catálogo |
| E2E | **Patrol** | **Playwright** + `@axe-core/playwright` |
| Lint | `flutter_lints` + `custom_lint` con las 9 reglas | `angular-eslint` + `eslint-plugin-boundaries` + las 9 reglas |
| Formato | `dart format` | prettier |
| Docker | — | `Dockerfile.backoffice` (NGINX, estático) y `Dockerfile.web` (Node SSR tras NGINX), multietapa, sin root |
| Tiendas / OTA | `flutter build appbundle` · `flutter build ipa` · **Shorebird** con canal por entorno | — |

## F0.5 · ADR y documentos

| Documento | Qué decide |
| --- | --- |
| **ADR-044** | El stack (hecho). Supera ADR-004, enmienda ADR-041 en el *con qué* |
| **ADR-041 · ADR-042** | Siguen vigentes; F0-W verifica que lo que decidían se cumple con Angular SSR |
| `docs/Arquitectura/Estructura del repositorio.md` | Se actualiza con la estructura de F0.1 |

## F0.6 · Las máquinas

| Puesto | Necesita | Para |
| --- | --- | --- |
| **P1 · Mac** | Flutter, Xcode, CocoaPods, Android Studio, `fvm` | F0.T, pase de iOS, publicación |
| **P3 · Legion** | Flutter, Android Studio con emulador acelerado, `fvm`, Patrol CLI | F0-M, F1-M, E2E móvil |
| **P4 · Dell A** | Node 22, Angular CLI, Chromium para Playwright | F0-B, F1-W |
| **P5 · Dell B** | Node 22, Angular CLI, Chromium, Lighthouse CI | F0-W |
| **Todas** | Java 21 (para `generateOpenApiClients`), un Android físico de gama baja | — |

> **Las Dell no corren el emulador.** Corren `flutter run -d <dispositivo>` sobre un
> Android físico por USB, que es más rápido, más barato en memoria y el parque real.

## Gate de salida F0

```bash
yarn workspace @aportaya/tokens build     # tokens.css + tokens.dart, con las mismas claves
./gradlew generateOpenApiClients          # clientes/angular y clientes/dart, sin diff en CI
yarn dev:mock                             # Prism en :4010, valida contra esquema
yarn dev:movil                            # flutter run contra Prism
yarn dev:backoffice && yarn dev:web
yarn lint && yarn typecheck && yarn test:front && yarn test:a11y
docker build -f apps/web/docker/Dockerfile.web . && docker build -f apps/backoffice/docker/Dockerfile.backoffice .
```

- [ ] Gate común de §11 del plan maestro del frontend
- [ ] Una pantalla real por producto, contra Prism, con sus cuatro estados
- [ ] Los dos generados de tokens tienen las mismas claves; `tokens-contra-boveda` en verde
- [ ] Los importes son `string` en `clientes/dart` y en `clientes/angular` (prueba)
- [ ] **Agregar una pantalla vacía no requiere editar ningún archivo fuera del directorio
      del carril** (probado en los tres productos)
- [ ] `noindex` en el backoffice desde el primer día; `robots.txt` generado en el sitio
- [ ] `grep -r "Platform.is" apps/movil/lib` vacío fuera de `infraestructura/`

---

# FASE F1 — Sistema de diseño, en dos carriles

> **Objetivo.** Que exista el catálogo completo de piezas visuales **en los dos
> mundos**, con tokens generados, tema claro y oscuro y accesibilidad verificada, para
> que las diez fases siguientes **compongan** en vez de inventar.

**El sistema de diseño ya está especificado.** No se diseña acá: se implementa lo que
la skill `disenar-frontend` y `docs/Views/Sistema-Diseno/` ya definieron, con sus hex
exactos. Inventar un color o un espaciado en esta fase es el error más caro del
frontend, porque se propaga a los tres productos.

> [!important] Por qué F1 ahora sí se puede partir
> Antes, `packages/ui` era un solo paquete con dos renderizadores y partirlo era partir
> el sistema de diseño. Ahora son **dos bases de código que no comparten una línea**:
> `packages/ui` (Angular) y `packages/diseno_flutter` (Dart). Lo que las mantiene
> iguales no es que las escriba la misma máquina: es que **los tokens son generados del
> mismo JSON**, que `Monto` pasa los mismos vectores, y que **la revisión visual conjunta
> compara golden contra captura, pieza por pieza**. `F1-W` va a P4 y `F1-M` a P3, a la vez.

## Gate de entrada

- [ ] F0 cerrada · `packages/tokens` **congelado**
- [ ] El catálogo de `docs/Views/Sistema-Diseno/` leído entero por los dos carriles

## F1.1 · Tokens — ya generados, ya congelados

No hay trabajo de tokens en F1. `packages/tokens` es de F0.T y **no se toca**: si a un
carril de F1 le falta un token, es un micro-PR al troncal con la prueba contra la
bóveda actualizada. Familias, para referencia:

| Familia | Valores |
| --- | --- |
| Verde Pasanaku | `g900 #0C2C1D` … `g600 #1C5A3A` (**marca**) … `g100 #E7F2EB` |
| Naranja Aporte | `o700 #BC6217` · **`o500 #E5852B` (acento/CTA)** · `o100 #FDF0DF` |
| Neutros | con sesgo verde, a propósito |
| Semánticos | `ok`, `warn`, `err`, `info` — **separados del acento** |
| Espaciado | `s1 4` · `s2 8` · `s3 12` · `s4 16` · `s5 24` · `s6 32` · `s7 48` |
| Radio | `r-sm 8` · `r-md 12` · `r-lg 16` · `r-xl 24` · `r-pill 999` |
| Sombra | `sh-1` · `sh-2` · `sh-3` |
| Tipografía | `font-d Poppins` · `font-b Inter` · `mono` |

**Dark mode redefine solo tokens**, nunca componentes.

## F1.2 · Átomos — en los dos mundos, con el mismo nombre

Botones (primario `accent`, secundario `brand`, fantasma, peligro, enlace, ícono,
FAB) en sm/base/lg, con los siete estados: normal, hover, active, foco, deshabilitado,
cargando · Campos (texto, con ícono, con addon `Bs`, **monto**, búsqueda, textarea,
select, fecha, stepper, contraseña con ojo, **PIN/OTP**) con normal/foco/error/éxito/
deshabilitado · Selección (checkbox, radio, switch, segmentado, chip) · Indicadores
(badge, chip removible, avatar 24/30/40/56, spinner, barra y anillo de progreso,
skeleton, tooltip, dot) · **`CampoOTP`** · **`EstrellasCalificacion`** · **`CampoCodigo`** ·
**`SelectorSegmentado`** · **`CuentaEnmascarada`** · **`Fecha`** (con zona horaria visible).

| Mundo | Cómo se escribe un átomo | Prueba |
| --- | --- | --- |
| **Angular** (`packages/ui/boton/`) | componente *standalone* con `input()`/`output()` de señales, `host` bindings para estado y a11y, estilos con `var(--…)`, entrada secundaria propia | `boton.spec.ts` (ATL) + `boton.a11y.spec.ts` (axe) + captura en el catálogo |
| **Flutter** (`packages/diseno_flutter/lib/atomos/boton.dart`) | `StatelessWidget` con `Semantics`, estilos desde `Tokens.of(context)`, sin `Colors.*` | `boton_test.dart` + golden claro/oscuro + `meetsGuideline` |

> **`Monto` es el átomo más importante del sistema.** Es el único lugar donde se
> formatea dinero: `tabular-nums`, prefijo `Bs`, coma decimal, `Bs 1.240,00`, **sin pasar
> por `Number` ni por `double`**. Los dos `Monto` cargan `packages/tokens/vectores/monto.json`
> y fallan si un solo caso difiere. La prueba de propiedad (5.000 importes de hasta 19
> dígitos, ida y vuelta exacta) ya existe en TypeScript y se **porta** a Dart.

## F1.3 · Moléculas

Campo de formulario (label + input + ayuda/error) · búsqueda · menú desplegable ·
selector de fecha · **input de monto con moneda** · grupo de filtros · **tarjeta
KPI** · **tarjeta de saldo** · **ítem de pasanaku** · **fila de movimiento** (tipada por
lo que pasó, con saldo corrido) · alerta/banner · toast · tabs · breadcrumb · paginación ·
acordeón · **stepper/wizard** · ítem de notificación · **fila de acciones rápidas** ·
**`BarraDePasos`** · **`FilaDeCotejo`** · **`ChipsDeFiltro`** (en varias líneas, nunca
carrusel) · **`ResumenDePeriodo`** · **`SelectorCuentaBancaria`** · **`SelectorCategoria`**.

## F1.4 · Organismos

**`EstadoDePantalla`** (el único que sabe pintar los cuatro estados) · navbar · sidebar ·
formulario completo · **tabla de datos** (toolbar con búsqueda y filtros, orden por
columna, selección múltiple, paginación del servidor, **virtualización** con
`cdk-virtual-scroll-viewport`) · barra de filtros · modal · diálogo de confirmación ·
**`EstadoVacio`** · **`EstadoError`** (con variante `sinConexion`) · grilla de tarjetas ·
**lista de movimientos agrupada por fecha** · **`MarcoDeCamara`** (Flutter) ·
**`BannerDePauta`** · **`SeccionDeExpediente`** (Angular).

## F1.5 · Piezas móviles (solo `packages/diseno_flutter`)

Marco con status bar · app bar · **tab bar** (3–5 destinos) · **FAB** · **bottom
sheet** (`HojaDeConfirmacion`) · **teclado numérico 3×4** · **entrada de PIN/OTP** ·
snackbar · **tarjeta de saldo móvil** · onboarding · `ProveedorTema`.

## F1.6 · Catálogo vivo — dos catálogos, una revisión

| Mundo | Catálogo | Para qué |
| --- | --- | --- |
| Angular | ruta `/catalogo` en `apps/web`, **`noindex`**, cada pieza en sus variantes y estados, claro y oscuro | referencia, pruebas visuales de Playwright, revisión |
| Flutter | **Widgetbook** (`packages/diseno_flutter/widgetbook/`) con las mismas piezas y estados | referencia, base de los goldens, revisión en dispositivo |

**La revisión visual conjunta** del punto de sincronización abre los dos lado a lado.
Es la única forma de detectar que dos carriles resolvieron lo mismo de dos maneras.

## F1.7 · Los organismos que exige el recorrido del participante (F1-M)

Los fija [[Flujo de pantallas · app del participante]]. F1-M los entrega en
`packages/diseno_flutter`; los que dependen de una API nativa se quedan en `apps/movil`:

| Dominio | Organismos | Dónde vive |
| --- | --- | --- |
| Identidad (M1) | `PanelBienvenida` · `FormularioRegistro` · `CapturaDocumento` · `VisorContrato` · `FormularioLogin` · `RegistroDispositivo` · `FormularioKYCReforzado` (`DeclaracionPEP`) · `FormularioPerfil` · `FormularioCambioContrasena` · `AsistenteBaja` | forma en `diseno_flutter`; captura/biometría en `apps/movil` |
| Billetera (M2) | `TarjetaSaldo` · `AccesosRapidos` · `ListaMovimientos` (`FilaMovimiento`) · `ResumenRecarga` · `PantallaQR` · `FormularioCuentaBancaria` · `FormularioAporte` (`FilaAporte`) · `PantallaResultado` · `CalendarioDeCuotas` | `diseno_flutter`; QR en `apps/movil` |
| Pasanaku (M3) | `CanjearInvitacion` · `FormularioPostulacion` · `TarjetaGrupo` · `ReglamentoGrupo` · `PanelSorteo` (`ListaTurnos`) · `AsistenteOrganizador` · `FormularioGrupo` · `TarjetaReputacion` · `FormularioReseña` | `diseno_flutter` |
| Shell (M) | `BarraPestanas` · `BandejaNotificaciones` (`FilaNotificacion`) | `diseno_flutter` |

Las reglas de la maqueta que valen para todo el sistema: **el ícono dice qué pasó, no si
el número sube o baja**, y **los movimientos se agrupan por día** con el neto del día.

## F1.8 · Los organismos que exige el recorrido del administrador (F1-W)

Los fija [[Flujo de pantallas · backoffice administrador]]. La pieza central es
**`TablaDeDatos`**; el resto son paneles y fichas. **El backoffice no es un segundo
sistema de diseño**: los mismos tokens, a densidad de escritorio.

| Área | Organismos | Carril que compone |
| --- | --- | --- |
| Shell / tablero | `TablaDeDatos`, `PanelKPIs` (`TarjetaKPI`), `PanelEstadoPlataforma` | B (F6) |
| Operación | `PanelDescuadre`, `ResumenCierre`, `PanelAprobacion`/`PanelEjecucion`, `FichaReclamo` (`LineaDeTiempo`), `PanelResolucion`, `EditorDePolitica`/`SimuladorDeReglas`, `EditorDeRoles`, `FormularioTarifario`/`PanelPreaviso`, `ConstructorDeReporte`, `FormularioInstrumentoFondeo`/`VistaPreviaQR`/`HistorialFondeo`, `EditorDePlantilla`/`MatrizDeCanales`/`PanelRuteoProveedor` | B1 (F7) |
| Cumplimiento | `RevisorDeIdentidad` (cola + **`SeccionDeExpediente`** ×9), `PanelTriaje`, `FichaCaso`, `PanelROS`, `FormularioRequerimiento`, `ActaComite` | B2 (F8) |
| Sistemas | `PanelSLO`, `PanelDespliegues`, `PanelColas`, `PanelWebhooks`, `PanelRespaldos`, `PanelSaludProveedor`, `FichaIncidente` | B5 (F8.D) |
| Contabilidad | `PanelPeriodo`, `EditorPresupuesto`, `FichaFactura`, `PanelDepreciacion`, `VisorEstadoFinanciero` | B3 (F13) |
| Publicidad | `FormularioPartner`, `FormularioAnunciante`, `PanelAprobacionCampana`, `ColaModeracion` | B4 (F14) |

Suben a `packages/ui` los transversales; las fichas de un solo dominio viven en su ruta.

## F1.9 · Las piezas que fija la maqueta — con su mundo

[[20 Maqueta de referencia · deltas del frontend]] §2 lista las piezas que la maqueta
sumó al inventario; [[22 Mapa de la maqueta · pantalla, carril y mundo]] §6 dice **en
qué paquete va cada una** (Flutter, Angular o los dos). Esa tabla es **parte del alcance
de F1-M y F1-W**, no un anexo: un carril de pantallas que llega y no encuentra
`RelojDePlazo` o `CalendarioDeCuotas` en su paquete lo va a escribir en su directorio, y
ahí nace el segundo `Monto`.

Resumen por mundo (el detalle, con nivel y dónde se usa, en el mapa):

| Mundo | Piezas que le tocan |
| --- | --- |
| **Solo Flutter** (F1-M) | `BarraDePuntos` · `TarjetaDeRacha` · `OpcionConCosto` · `MarcoDeCamara` (en `apps/movil`) · `BannerDePauta` · `PanelBienvenida` · `CalendarioDeCuotas` · `NotificacionEmergente` |
| **Solo Angular** (F1-W) | `SeccionDeExpediente` · `BandaDeProposito` · `TablaDeDatos` · `PanelDeIngresos` · `VisorEstadoFinanciero` · `VerificadorDeSorteo` (sitio) |
| **Los dos**, mismo nombre | `SelectorSegmentado` · `CodigoQR` · `CuentaEnmascarada` · `Fecha` · `BarraDePasos` · `FilaDeCotejo` · `ChipsDeFiltro` · `ResumenDePeriodo` · `FilaDeMovimiento` · `EstadoVacio` · `RielDeTurnos` · `RelojDePlazo` · `EscaleraDeEtapas` · `MedidorDeRango` · `DesgloseDeCobro` · `ListaDeRequisitos` · `TarjetaDeSolicitud` · `Vale` · `TarjetaDeOferta` · `PanelDeFactores` · `PanelSorteo` · **`EstadoDePantalla`** |

Las piezas «los dos» se construyen **en paralelo** y se revisan **lado a lado** en TF.3:
golden de Flutter contra captura de Angular, misma variante, mismo tema.

**Entregable F1:** los dos inventarios completos —incluido F1.9— con prueba por átomo,
prueba de comportamiento por molécula, accesibilidad limpia y captura o golden en ambos
temas.

## Gate de salida F1 (para F1-W y para F1-M, cada uno)

- [x] Gate común
- [x] Los cuatro grupos de piezas implementados y catalogados (`/catalogo` · Widgetbook)
- [x] **Cero literales de diseño** en todo el paquete (lint del mundo correspondiente)
- [x] `Monto` pasa los 5.000 vectores de `packages/tokens/vectores/monto.json`
- [~] Contraste AA verificado **pieza por pieza**, en claro y en oscuro — Flutter: `textContrastGuideline` sobre todo el catálogo; Angular: axe en jsdom no mide contraste, quedan las capturas de `/catalogo` para la revisión humana
- [x] Área táctil ≥ 48 dp en todas las piezas móviles (`androidTapTargetGuideline`)
- [x] `prefers-reduced-motion` / `disableAnimations` respetados
- [x] El catálogo Angular va `noindex`
- [~] **Revisión visual conjunta ejecutada** (primera pasada 2026-09-10, ver `carril-F1.md`; falta la mirada humana): golden contra captura, con la lista de
      diferencias y su resolución en el informe
- [x] **Las piezas de F1.9 existen** en el paquete del mundo que corresponde, con ese nombre,
      y cada una tiene su caso en el catálogo (`/catalogo` · Widgetbook) en los dos temas
- [~] Cada pieza del catálogo se comparó **contra la maqueta** (`docs/Views/AportaYa-Maqueta.html`)
      y la diferencia, si la hay, está justificada en el informe

## Ver también

[[17 Plan de acción secuencial · coordinación de cinco máquinas]] · [[22 Mapa de la maqueta · pantalla, carril y mundo]] · [[20 Maqueta de referencia · deltas del frontend]] · [[00c Recetario · implementar un caso de uso]] · [[16 Carriles de frontend]] · [[10 Plan maestro del frontend]] · [[10b Estándar de ejecución del frontend]] · [[12 Fases F2 a F5 · App móvil]] · [[ADR-044 Frontend en Angular y Flutter]] · [[AportaYa-Identidad]]
