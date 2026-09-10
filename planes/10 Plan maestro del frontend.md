---
tags:
  - moc
  - plan
  - frontend
titulo: "Plan maestro de desarrollo del frontend — AportaYa"
fecha: 2026-09-09
alcance: apps/movil (Flutter) · apps/backoffice (Angular) · apps/web (Angular SSR) · packages/{tokens,ui,diseno_flutter,simulado,dominio-cliente}
---

# Plan maestro de desarrollo del frontend

> **Para quién es este documento.** Para la IA (o la persona) que va a escribir el
> frontend. Es el espejo de [[00 Plan maestro]]: dice **qué** construir y **en qué
> orden**. El **cómo se escribe** está en
> [[10b Estándar de ejecución del frontend]], y **quién hace qué en cada máquina**
> en [[16 Carriles de frontend]].

> [!important] Stack fijado el 2026-09-09 — [[ADR-044 Frontend en Angular y Flutter]]
> La app del participante se escribe en **Flutter**; el backoffice y el sitio público
> en **Angular**. Reemplaza a Expo / React Native, React + Vite y Astro. Lo que ya
> existía —tokens verificados contra la bóveda, la prueba de propiedad de `Monto`, el
> simulado derivado del contrato, las pruebas de «una pantalla nueva no toca ningún
> registro compartido»— se conserva como **criterio de aceptación** del stack nuevo.
> La transición está en [[16 Carriles de frontend]] §12 y en el tramo `TF` de
> [[17 Plan de acción secuencial · coordinación de cinco máquinas]].

> [!important] Se apoya en el backend, pero no lo espera
> El frontend arranca cuando existe el **contrato OpenAPI** de un caso de uso, no cuando
> existe su implementación. Los contratos se escriben **antes** que el código (skill
> `contratos-api`), así que las olas del frontend van **una ola detrás** de las del
> backend, programando contra el contrato y un servidor simulado. Ver §9.

---

## 1 · Tres productos, dos tecnologías

| Producto | Usuario | Tecnología | SEO |
| --- | --- | --- | :-: |
| **`apps/movil`** — app del participante | Participante, organizador | **Flutter** · Dart 3 · Material 3 con tema propio | — no es web |
| **`apps/backoffice`** — operación, cumplimiento y sistemas | Oficial de cumplimiento, soporte, contabilidad, riesgos, plataforma, seguridad | **Angular** · SPA estática detrás de login | **`noindex`** |
| **`apps/web`** — sitio público | Cualquiera, más auditores y terceros | **Angular + `@angular/ssr`** · *prerender* por omisión, servidor solo en verificación | **Sí. Es la única superficie indexable** |

### Por qué el tercer producto no es un capricho

No se agrega para tener marketing. **La bóveda ya lo exige sin haberlo declarado:**

| Obligación | De dónde sale |
| --- | --- |
| Publicar el tarifario con preaviso, accesible al público | **CU-34**, `documento_publicado`, `R-TAR-08`, ASFI transparencia |
| Verificación pública del sorteo, sin sesión | **CU-61** · `GET /publico/sorteos/:id/verificacion` |
| Cadena de transparencia auditable por un tercero | **CU-72**, **CU-73** · `GET /publico/grupos/:codigo/…` |
| Certificado de reputación verificable por quien lo recibe | **CU-75** · `GET /verificar/:codigo` |
| Informar canales y puntos de reclamo | `punto_reclamo`, ASFI Libro 4 Título I |
| Publicar el contrato de adhesión vigente | **CU-05**, `contrato_adhesion`, `R-CON-06` |

Cuatro casos de uso ya devuelven **rutas públicas sin sesión**. Sin `apps/web` esas
rutas devuelven JSON a un navegador y la obligación de transparencia queda sin
superficie. Lo decide [[ADR-041 Sitio público · el tercer producto]]; con qué se
construye, [[ADR-044 Frontend en Angular y Flutter]].

### Por qué dos tecnologías y no una

| Pregunta | Respuesta |
| --- | --- |
| ¿Por qué no Flutter en los tres? | El sitio público necesita HTML indexable y liviano; Flutter Web dibuja en *canvas*. Y el backoffice es un producto de navegador de escritorio: Angular es su casa natural |
| ¿Por qué no Angular en los tres (Ionic/Capacitor)? | Una billetera necesita ser app: biometría, cámara y push confiables en Android de gama baja. Es el mismo argumento con el que ADR-004 descartó la PWA |
| ¿Qué comparten entonces? | **Lo que se genera**: tokens, clientes de API, ejemplos del contrato, vectores de `Monto`. Y **lo que se exige**: los diez invariantes, los cuatro estados, la voz de marca |

---

## 2 · Los diez invariantes del frontend

Espejo de los diez del backend. Ninguna fase los suspende. **Valen igual en Dart y en
TypeScript**; lo que cambia es la regla de lint que los verifica (§6).

| # | Invariante | De dónde sale | Cómo se verifica |
| :-: | --- | --- | --- |
| 1 | **La vista no llama a la red.** Todo pasa por la capa de dominio | [[Prompt de frontend]] §2 | Lint: sin `HttpClient`/`fetch` fuera de `nucleo/` y `dominio/` (Angular); sin `dio`/`http` fuera de `dominio/` (Flutter) |
| 2 | **Los tipos vienen del contrato**, nunca se reescriben a mano | [[ADR-020 Contratos OpenAPI primero]] | Lint: sin `interface`/clase que duplique un modelo de `clientes/angular` o `clientes/dart` |
| 3 | **Ningún valor de diseño literal** fuera de los tokens generados | skill `disenar-frontend` | Lint: sin hex, `px`, `Color(0x…)`, `EdgeInsets` numérico ni `fontFamily` fuera de `packages/tokens` y sus generados |
| 4 | **Los cuatro estados, siempre**: cargando, vacío, error, éxito | [[Prompt de frontend]] §3 | Prueba por pantalla con datos; `EstadoDePantalla` es el único camino |
| 5 | **Ningún importe se formatea a mano.** Solo el átomo `Monto` | [[ADR-019 Dinero con BigDecimal]] | Lint: sin `toFixed`/`Intl.NumberFormat` (TS) ni `NumberFormat`/`toStringAsFixed` (Dart) fuera de `Monto` |
| 6 | **Toda operación de dinero envía clave de idempotencia**, y el botón se bloquea | [[ADR-044 Frontend en Angular y Flutter]] | Prueba de doble envío por flujo |
| 7 | **El cliente nunca es la garantía.** Valida para ayudar; el servidor protege | [[Prompt general de desarrollo]] §7 | Revisión: ninguna regla de negocio vive solo acá |
| 8 | **Un componente > ~150 líneas mezcla niveles** | [[ADR-023 Composición atómica en Java]] (versión de frontend) | Lint de tamaño; en Flutter, además, `build()` > 80 líneas advierte |
| 9 | **Ninguna página con datos de terceros se indexa.** `/verificar/*` y `/publico/*` van `noindex, nofollow` | `R-SEG-03` | `curl` de cabeceras en el gate; prueba que enumera las rutas de servidor |
| 10 | **No se publica una afirmación regulatoria que no sea cierta hoy** | licencia `EN_TRAMITE` | Revisión de contenido; JSON-LD sin `FinancialService` mientras no haya resolución |

---

## 3 · Stack fijado

### 3.1 · Común a los tres

| Capa | Elección | Nota |
| --- | --- | --- |
| Gestor | **yarn 4** workspaces + **Turborepo** | yarn es el único gestor (nunca pnpm ni npm). `apps/movil` no es un paquete de Node, pero lleva un `package.json` **envoltorio** con los mismos nombres de tarea (`lint`, `typecheck`, `test:front`, `test:a11y`) para que `turbo` lo orqueste igual |
| Contratos | `servicios/*/src/main/resources/openapi/*.yaml` | Los escribe el backend, antes de implementar |
| Clientes de API | **Generados** por `./gradlew generateOpenApiClients` | `clientes/angular/<servicio>` (`typescript-angular`) y `clientes/dart/<servicio>` (`dart-dio`). No se editan, **sin dueño de carril**, el CI regenera y falla si hay diferencia |
| Tokens | **`packages/tokens/tokens.json`** — la única fuente | Genera `tokens.css` y `tokens.dart`. Verificado contra `docs/Views/Sistema-Diseno/estilos.css` por prueba |
| Servidor simulado | **Prism** (`@stoplight/prism-cli`) sobre los contratos | `packages/simulado`: ejemplos por CU y por escenario en un documento fusionado, validación de petición y respuesta contra el esquema. El escenario se pide con `Prefer: code=<estado>, example=<escenario>`. Un solo mock para Dart y TypeScript |
| Vectores compartidos | `packages/tokens/vectores/monto.json` · `packages/dominio-cliente/vectores/*.json` | Lo que dos lenguajes tienen que calcular igual se prueba contra el mismo JSON |
| Logs de cliente | Sin PII, con `x-request-id` propagado al backend | En los dos mundos |
| Empaquetado | Docker para backoffice y web; tiendas + Shorebird para la app | [[ADR-025 Empaquetado y despliegue de los servicios]] · [[ADR-044 Frontend en Angular y Flutter]] |

### 3.2 · Flutter — `apps/movil`

| Capa | Elección | Nota |
| --- | --- | --- |
| Marco | **Flutter** estable (la versión se fija en F0 y se escribe en `.fvmrc`), **Dart 3** con *null safety* y *records* | `fvm` para que las cinco máquinas usen la misma |
| UI base | Material 3 con **tema propio** construido desde `tokens.dart` (`ThemeExtension`) | Ningún `Colors.*` ni `ThemeData` literal fuera del tema |
| Navegación | **`go_router`** con rutas tipadas (`go_router_builder`) · *deep links* con `app_links` | Un `rutas.dart` **por dominio de pantallas**; el shell los importa por un enchufe fijo |
| Estado | **Riverpod** con generación (`riverpod_generator`) · `AsyncValue` como modelo de los estados | Cargando / error / dato **son** `AsyncValue`; el vacío lo decide la vista con `EstadoDePantalla` |
| Formularios | `flutter_form_builder` + validadores derivados del contrato | El esquema del OpenAPI se traduce a validadores en `dominio/validacion.dart`, generado |
| HTTP | `dio` con interceptores: `x-request-id`, idempotencia, `401 → un refresh → un reintento`, traducción de errores | Solo en `dominio/`; las vistas consumen proveedores |
| Nativo | `flutter_secure_storage` (Keystore/Keychain) · `local_auth` · `mobile_scanner` · `firebase_messaging` · `connectivity_plus` · `screen_protector` | Detrás de **puertos** en `dominio/puertos/` con adaptadores en `infraestructura/{android,ios}/` ([[ADR-036 Android primero]]) |
| OTA | **Shorebird** para parches de la capa Dart | Lo nativo pasa por tienda, como antes |
| Pruebas | `flutter_test` por nivel · **goldens** en claro y oscuro · `meetsGuideline(...)` de accesibilidad · **Patrol** para E2E en dispositivo · contrato con `http_mock_adapter` sobre los ejemplos del simulado | §7 |
| Lint | `flutter_lints` + **`custom_lint`** con las reglas propias de §6 | `dart analyze --fatal-infos` |

### 3.3 · Angular — `apps/backoffice`, `apps/web`, `packages/ui`

| Capa | Elección | Nota |
| --- | --- | --- |
| Marco | **Angular ≥ 21** (la versión exacta se fija en F0), *standalone* en todo, **zoneless**, señales | `tsconfig.base.json` estricto del monorepo |
| Enrutador | Angular Router con `loadChildren` **por dominio** y guardias funcionales | `app.routes.ts` tiene un enchufe por directorio de rutas, fijado por el shell; nadie lo edita después |
| Estado de servidor | `resource()` / `httpResource()` + señales; un `Recurso` envuelto por `EstadoDePantalla` | Invalidación **explícita** por acción; nada de recargar todo |
| Formularios | **Signal Forms** de Angular, tipados desde los modelos de `clientes/angular` | Validadores derivados del esquema del contrato (`validarConContrato`) |
| HTTP | `HttpClient` con interceptores funcionales en `nucleo/`: `x-request-id`, idempotencia (`HttpContextToken`), `401 → un refresh → un reintento`, errores | Ningún componente inyecta `HttpClient` |
| UI base | **Angular CDK**: `a11y`, `overlay`, `scrolling` (virtualización), `table`, `drag-drop` donde haga falta | Sin Angular Material: el sistema de diseño es propio |
| Diseño | **`packages/ui`** — biblioteca Angular (`ng-packagr`) con **una entrada secundaria por componente** (`@aportaya/ui/boton`) | Sin barril: un componente nuevo no edita nada compartido |
| Sitio público | **`@angular/ssr`** con `serverRoutes`: `Prerender` por omisión, `Server` en `/verificar/*`, `/publico/*`, `/tarifas`, `/contrato-de-adhesion` · **hidratación incremental** (`@defer (hydrate on viewport)`) para los verificadores | Contenido en Markdown con *frontmatter* validado, procesado en el build |
| Pruebas | **Vitest** (corredor por defecto de Angular) + **Angular Testing Library** · `vitest-axe` · **Playwright** + `@axe-core/playwright` · **Lighthouse CI** | §7 |
| Lint | **`angular-eslint`** con las reglas de plantilla de accesibilidad como **error** + `eslint-plugin-boundaries` + las reglas propias de §6 | `ng lint` |

### Lo que está prohibido

- `HttpClient`, `fetch`, `dio` o `http` dentro de un componente o widget.
- Un hex, un `px`, un `Color(0x…)`, un `EdgeInsets.all(13)` o una `fontFamily` fuera de
  `packages/tokens` y sus generados.
- `toFixed`, `NumberFormat`, `toStringAsFixed` o aritmética sobre importes en el cliente.
- Reescribir a mano un tipo que ya está en `clientes/angular` o `clientes/dart`.
- Una pantalla de datos sin sus cuatro estados.
- Un `div` que hace de botón; un `GestureDetector` sin `Semantics` que hace de botón.
- `Platform.isIOS` / `Platform.isAndroid` en una vista (va en `infraestructura/`).
- Datos sensibles en la URL, en el estado global, en `SharedPreferences` o en `localStorage`.
- Un `routes.ts` o un `rutas.dart` compartido que dos carriles editen.

### Base URL, sesión y CORS

Los tres clientes —app, backoffice y sitio— apuntan a **una sola base URL: el
gateway**. Nunca se habla directo con un servicio: el **prefijo de la ruta** es lo que
enruta al servicio correcto ([[19 Contrato de carril · conflicto cero, skills y calidad verificada]] §3.1).

| Regla | Cómo se implementa |
| --- | --- |
| **Una sola base URL** | Toda petición sale hacia el gateway. El servicio destino se resuelve por el prefijo, no por el host |
| **Refresh vía gateway** | El refresh va a `identidad` **a través del gateway**, como cualquier otra ruta |
| **Un `401` ⇒ un intento** | Un `401` en cualquier servicio dispara **un** intento de refresh y **un** reintento de la petición original. Si el refresh falla, se cierra la sesión global (una sola vez, no en bucle) |
| **CORS en el gateway** | El gateway configura CORS para los orígenes del **backoffice** y del **sitio**. El frontend no gestiona CORS: lo recibe resuelto |
| **Token del operador** | Solo en memoria; refresh en cookie `HttpOnly`. Jamás en `localStorage` |
| **Token del participante** | En `flutter_secure_storage`, con bloqueo biométrico donde la plataforma lo permita |

> **Esta política vive en un solo lugar por producto:** el interceptor de `dominio/`
> en Flutter (F2) y los interceptores de `nucleo/` en Angular (F6, F9). Ninguna pantalla
> ve un `401` ni conoce la URL de `identidad`.

---

## 4 · Las cuatro capas del frontend

Es la composición atómica aplicada a la interfaz, en los dos mundos. **Dirección única.**

```
pantallas/  (Flutter)  ·  rutas/<dominio>/  (Angular)     PÁGINA       compone organismos + resuelve la ruta. Sin lógica
  ↓
organismos/                                                ORGANISMO    sección autónoma: formulario, tabla, panel
  ↓
moleculas/                                                 MOLÉCULA     una responsabilidad: campo con error, fila, proveedor de un recurso
  ↓
atomos/                                                    ÁTOMO        pieza visual mínima: Boton, Campo, Monto, ChipEstado
      ↑
dominio/                                                                un archivo por CU sobre el cliente generado
packages/tokens → tokens.dart · tokens.css                              único lugar con valores literales
```

| Capa | Puede depender de | Nunca hace | Prueba |
| --- | --- | --- | --- |
| `atomos/` | tokens | Conocer una regla de negocio o llamar a la API | Rinde sus variantes y estados; golden / captura en claro y oscuro |
| `moleculas/` | `atomos/`, `dominio/` | Orquestar la pantalla, decidir navegación | Comportamiento: escribe, valida, emite |
| `organismos/` | `moleculas/`, `atomos/`, `dominio/` | HTTP directo | Flujo completo con API simulada, con error y reintento |
| `pantallas/` · `rutas/` | `organismos/` | Cálculos, reglas | E2E |
| `dominio/` | `clientes/dart` · `clientes/angular` | Renderizar | Contrato: la respuesta simulada valida contra el esquema del OpenAPI |

**Lo que sirve a dos productos web sube a `packages/ui`.** Lo que sirve a la app vive en
`packages/diseno_flutter` si es visual, o en `apps/movil` si depende de una API nativa
(cámara, biometría). **Un organismo con la misma responsabilidad existe una vez por
mundo, con el mismo nombre**: `TarjetaSaldo` en Dart y `TarjetaSaldo` en Angular.

---

## 5 · Las 15 fases

| Fase | Nombre | Producto | Superficie | Documento |
| :-: | --- | --- | --- | --- |
| **F0.0** | **Maqueta navegable** con backend simulado | ninguno todavía | el recorrido de los dos productos, presentable · **hecha** | [[11 Fases F0 y F1 · Cimientos y sistema de diseño]] |
| **F0** | Cimientos del frontend | los tres | tokens generados, clientes generados, Prism, tres andamiajes | ídem |
| **F1** | Sistema de diseño — **en dos carriles paralelos**: `F1-W` (Angular) y `F1-M` (Flutter) | `packages/ui` · `packages/diseno_flutter` | tokens, átomos, moléculas, organismos, catálogo | ídem |
| **F2** | Shell móvil | `apps/movil` | navegación, sesión, tema, offline, biometría | [[12 Fases F2 a F5 · App móvil]] |
| **F3** | Móvil · identidad y cuenta | `apps/movil` | CU-01…09, 40, 46 | ídem |
| **F4** | Móvil · billetera | `apps/movil` | CU-10…19, 30…33, 57 | ídem |
| **F5** | Móvil · pasanaku y comunidad | `apps/movil` | CU-20…29, 52, 53, 59…76 | ídem |
| **F6** | Shell backoffice | `apps/backoffice` | enrutador, tabla de datos, roles | [[13 Fases F6 a F8 · Backoffice]] |
| **F7** | Backoffice · operación | `apps/backoffice` | billetera, conciliación, cobranza, reclamos | ídem |
| **F8** | Backoffice · cumplimiento y gobierno · **F8.D** sistemas | `apps/backoffice` | UIF, ASFI, reportes, tablero, comités · plataforma y seguridad | ídem |
| **F9** | Sitio público · estructura y contenido | `apps/web` | páginas, contenido regulatorio, verificación | [[14 Fases F9 a F11 · Sitio público, SEO y GEO]] |
| **F10** | **SEO** | `apps/web` | metadatos, JSON-LD, sitemap, CWV | ídem |
| **F11** | **GEO** — optimización para motores generativos | `apps/web` | `llms.txt`, espejo markdown, política de crawlers | ídem |
| **F12** | Endurecimiento, accesibilidad, E2E y publicación | los tres | tiendas, despliegue | [[15 Fase F12 · Endurecimiento, E2E y publicación]] |
| **F13** · **F14** | Backoffice · ERP · publicidad | `apps/backoffice` | CU-100–106 · CU-110–114 | [[13 Fases F6 a F8 · Backoffice]] §F13–F14 |

---

## 6 · Reglas de lint propias del frontend

Las mismas nueve reglas en los dos mundos; la implementación cambia.

| Regla | Qué prohíbe | Angular (`angular-eslint`) | Flutter (`custom_lint`) | Invariante |
| --- | --- | --- | --- | :-: |
| `aportaya/sin-red-en-vista` | red fuera de la capa de dominio | `HttpClient`, `fetch`, `XMLHttpRequest` fuera de `nucleo/` y `dominio/` | `dio`, `http`, `HttpClient` fuera de `dominio/` | 1 |
| `aportaya/tipos-del-contrato` | reescribir un tipo del contrato | `interface`/`type` con el nombre de un modelo de `clientes/angular` | clase con el nombre de un modelo de `clientes/dart` | 2 |
| `aportaya/sin-literal-de-diseno` | valores de diseño sueltos | hex, `rgb()`, `px`, `font-family` en `.ts`, `.html`, `.css` fuera de `packages/tokens` | `Color(`, `Colors.`, `EdgeInsets` numérico, `fontSize:`, `fontFamily:` fuera de `tokens.dart` y del tema | 3 |
| `aportaya/sin-formato-de-dinero` | formatear dinero fuera de `Monto` | `toFixed`, `Intl.NumberFormat`, concatenar `'Bs '` | `NumberFormat`, `toStringAsFixed`, `'Bs '` | 5 |
| `aportaya/capas-front` | saltar niveles | `eslint-plugin-boundaries`: `atomos/` no importa `dominio/` ni `organismos/` | regla sobre `import` por directorio | — |
| `aportaya/tamano-componente` | componentes gigantes | ≥ 150 líneas advierte · ≥ 200 bloquea | ídem; `build()` > 80 líneas advierte | 8 |
| `aportaya/sin-console-log` | trazas sueltas | `console.*` en runtime | `print`, `debugPrint` en runtime | — |
| `aportaya/sin-plataforma-en-vista` | ramas por SO en la vista | — | `Platform.is*` fuera de `infraestructura/` | ADR-036 |
| Accesibilidad | controles sin semántica | `@angular-eslint/template/*` de accesibilidad **como error** | `GestureDetector`/`InkWell` sin `Semantics(button: true)` | — |

---

## 7 · Estrategia de pruebas

### 7.1 · Angular

| Nivel | Herramienta | Contra qué | Archivo |
| --- | --- | --- | --- |
| **Átomo** | Vitest + Angular Testing Library | Variantes y estados | `<atomo>.spec.ts` |
| **Molécula** | Vitest + ATL | Comportamiento observable | `<molecula>.spec.ts` |
| **Organismo** | Vitest + ATL + `provideHttpClientTesting` con los **ejemplos del contrato** | Flujo con API simulada, **incluidos error y reintento** | `<organismo>.spec.ts` |
| **Contrato** | `ajv` contra el esquema del OpenAPI | Cada ejemplo usado **valida contra el esquema** | `CU<NN>.contrato.spec.ts` |
| **Accesibilidad** | `vitest-axe` | Cero violaciones serias por pantalla | `<pantalla>.a11y.spec.ts` |
| **E2E web** | Playwright + Chromium + `@axe-core/playwright` | Backoffice y sitio público | `<flujo>.e2e.spec.ts` |
| **Visual** | Playwright screenshots del catálogo | Claro y oscuro, por componente | `<componente>.visual.spec.ts` |
| **Rendimiento** | Lighthouse CI | CWV y presupuesto de JS del sitio | `lighthouserc.json` |

### 7.2 · Flutter

| Nivel | Herramienta | Contra qué | Archivo |
| --- | --- | --- | --- |
| **Átomo** | `flutter_test` + **golden** en claro y oscuro | Variantes y estados; el píxel es el criterio | `test/widget/atomos/<atomo>_test.dart` · `test/goldens/` |
| **Molécula** | `flutter_test` | Comportamiento: escribe, valida, emite | `test/widget/moleculas/<molecula>_test.dart` |
| **Organismo** | `flutter_test` + `http_mock_adapter` con los **ejemplos del contrato** + `ProviderContainer` de Riverpod | Flujo con API simulada, **incluidos error y reintento** | `test/widget/organismos/<organismo>_test.dart` |
| **Contrato** | validación del ejemplo contra el esquema (JSON Schema en Dart) | Cada ejemplo usado valida contra el esquema | `test/contrato/cu<NN>_test.dart` |
| **Accesibilidad** | `meetsGuideline(textContrastGuideline)` · `androidTapTargetGuideline` · `iOSTapTargetGuideline` · `labeledTapTargetGuideline` | Contraste, área táctil ≥ 48 dp, etiquetas | `test/a11y/<pantalla>_test.dart` |
| **E2E móvil** | **Patrol** sobre `integration_test` en dispositivo o emulador | Permisos nativos, notificaciones, modo avión | `integration_test/<flujo>_test.dart` |
| **Rendimiento** | `flutter run --profile` + *timeline* en dispositivo real | Arranque en frío, *jank* de listas | evidencia en el informe |

### Las cinco pruebas obligatorias de toda pantalla con datos

1. **Los cuatro estados**: cargando, vacío, error (con reintento) y éxito.
2. **Doble envío**: si produce un efecto, dos toques ⇒ **una** llamada, misma clave de
   idempotencia.
3. **Sin conexión**: muestra el último estado y no permite operar.
4. **Accesibilidad**: `vitest-axe` o `meetsGuideline` sin violaciones; foco visible;
   navegable por teclado (web) o con lector (móvil).
5. **Contrato**: la respuesta que usa el mock valida contra el esquema OpenAPI. Un mock
   que no valida es una pantalla que ya está rota.

> **La prueba 5 es la que evita el desastre clásico del frontend**: pantallas verdes
> contra mocks inventados que no se parecen a lo que la API devuelve. Por eso los
> ejemplos viven en `packages/simulado`, se derivan del esquema y **los consumen los
> dos lenguajes**.

---

## 8 · Metadatos, SEO y GEO — dónde vive cada cosa

Resumen; el detalle está en [[14 Fases F9 a F11 · Sitio público, SEO y GEO]].

| Superficie | Render | Indexación | Metadatos |
| --- | --- | --- | --- |
| `apps/web` — páginas de contenido | `Prerender` | **`index, follow`** | Completos: title, description, canonical, hreflang `es-BO`, OG, Twitter, JSON-LD |
| `apps/web` — `/tarifas`, `/contrato-de-adhesion` | `Server` + caché de NGINX | index | Completos; el contenido viene del backend con hash y vigencia |
| `apps/web` — `/verificar/*`, `/publico/*` | `Server` con `X-Robots-Tag` | **`noindex, nofollow`** | Mínimos. **Nunca** JSON-LD con datos de la persona |
| `apps/backoffice` | SPA estática | **`noindex, nofollow`** + `X-Robots-Tag` en NGINX | Solo `title` |
| `apps/movil` | — | — | *App Store Optimization* (F12) |

**Política de rastreadores de IA: búsqueda sí, entrenamiento no** ([[ADR-042 Política de rastreadores de IA]]).

---

## 9 · Cómo se sincroniza con el backend

**El frontend no espera la implementación: espera el contrato.**

```
backend   Ola 0 ──── Ola 1 ──── Ola 2 ──── Ola 3 ──── Ola 4 ──── Ola 5
frontend            Ola F0 ─── Ola F1 ─── Ola F2 ─── Ola F3 ─── Ola F4
                    (una ola de desfase)
```

| Qué necesita el frontend | Cuándo está |
| --- | --- |
| `openapi/<servicio>.yaml` | Lo escribe el carril de backend **antes** de implementar |
| `clientes/angular` y `clientes/dart` | Los regenera quien corre `./gradlew generateOpenApiClients`; el CI verifica que no haya diferencia |
| Un servidor simulado | **Prism** sobre los contratos, con los ejemplos de `packages/simulado`. No espera a nadie |
| La API real | Solo para el E2E de la Fase F12 y para las pruebas de integración de cada ola |

**Regla dura:** si el contrato de un CU todavía no existe, **el frontend no lo
inventa** (regla cero). Lo pide al carril de backend correspondiente y trabaja en otro
mientras tanto.

---

## 10 · La maqueta manda sobre el cómo

[[AportaYa-Maqueta]] es la **especificación visual y de comportamiento** de las
pantallas; [[20 Maqueta de referencia · deltas del frontend]] dice qué cambió en los
planes por ella, y [[22 Mapa de la maqueta · pantalla, carril y mundo]] dice, pantalla
por pantalla, **en qué mundo se escribe, qué carril la posee, cómo se llama su ruta y
qué organismos compone**. Tres consecuencias:

1. **Un carril de pantallas abre la maqueta antes que el código**, en los dos escenarios
   (optimista y adverso), y su informe lista por pantalla qué reprodujo, qué justificó
   distinto y qué corrigió en la maqueta.
2. **Todo golden o captura va al lado de la captura de la maqueta** en el PR, en claro y
   oscuro. «Se parece» no es un criterio; la comparación sí.
3. **Las piezas que la maqueta fija existen en el paquete de diseño del mundo que
   corresponde con ese nombre** (§6 del mapa). Un carril que reimplementa una en su
   directorio está creando el segundo `Monto`.

## 11 · Gate de fase — el mismo para las 15

```bash
yarn lint && yarn typecheck          # angular-eslint + flutter analyze + custom_lint
yarn test:front                      # vitest (unidad, componente, contrato) + flutter test (unidad, widget, contrato, goldens)
yarn test:a11y                       # vitest-axe + guías de accesibilidad de Flutter
```

- [ ] Los tres comandos en verde
- [ ] **Los cuatro estados** implementados y probados en cada pantalla con datos
- [ ] **Cero literales de diseño** fuera de `packages/tokens` (verificado por lint en los dos mundos)
- [ ] Ningún componente sobre el límite sin justificación escrita
- [ ] Los tipos vienen del cliente generado; ningún ejemplo que no valide contra su esquema
- [ ] Claro y oscuro probados (goldens en Flutter, capturas en Angular); `prefers-reduced-motion` y `disableAnimations` respetados
- [ ] Contraste AA (≥ 4.5:1), foco visible, navegación completa por teclado (web) y por lector (móvil)
- [ ] El checklist de §6 de la skill `disenar-frontend`, ejecutado
- [ ] Supuestos declarados en el informe del carril
- [ ] **Comparación contra la maqueta** ejecutada y registrada (§10 y [[22 Mapa de la maqueta · pantalla, carril y mundo]] §7)

## Ver también

[[10b Estándar de ejecución del frontend]] · [[16 Carriles de frontend]] · [[22 Mapa de la maqueta · pantalla, carril y mundo]] · [[20 Maqueta de referencia · deltas del frontend]] · [[00 Plan maestro]] · [[ADR-044 Frontend en Angular y Flutter]] · [[ADR-041 Sitio público · el tercer producto]] · [[Prompt de frontend]] · [[AportaYa-Identidad]] · [[Index]]
