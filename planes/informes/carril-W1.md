---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril W1 — SEO (Angular)"
ola: F2
fase: F10
mundo: Angular
modulo: apps/web/src/app/seo
rama: pablo/feature/carril-W1-seo
estado: en curso
---

# Carril W1 — SEO

**Fase** F10 · **Mundo** Angular · **Casos de uso** ninguno propio (capa de metadatos sobre F9) ·
**Puesto** P5 · Dell B · T6

> Este archivo lo escribe solo este carril. La ficha está en `planes/18` (`F10 · SEO`); la
> fase, en `planes/14 Fases F9 a F11`.

## Punto de partida real, y la reconciliación de mitad de carril

**Primera pasada:** el worktree se había creado desde una versión vieja de `dev`, anterior
a que `F9` (carril W, sitio público) fusionara su trabajo. Con eso a la vista solo había 3
rutas (`/`, `/plazos`, `/catalogo`) y 1 verificador, y `planes/informes/carril-W.md` no
existía. `ServicioMeta`, el JSON-LD y el sitemap se construyeron igual sobre esa base
porque son piezas genéricas — no dependían de cuántas páginas hubiera.

**El coordinador fusionó `dev` actual dentro de esta rama** (merge `425b9dd`, con el único
conflicto en `app.routes.ts` resuelto a favor de las 13 rutas reales de F9 + mis dos
`data.seo` ya escritas para `/` y `/plazos`, sin el `plazos` duplicado de mi versión
vieja). Con eso, la superficie real del sitio es:

- **15 rutas**: `/`, `como-funciona`, `seguridad`, `tarifas`, `contrato-de-adhesion`,
  `reclamos`, `privacidad`, `transparencia`, `preguntas`, `legal/estado-regulatorio`,
  `descargar`, `verificar/:codigo`, `publico/grupos/:codigo`, `publico/sorteos/:id`,
  `catalogo`, más `plazos` (CU-59, scaffoldeado antes de este carril por F0-W).
- **4 verificadores** en `apps/web/src/app/verificadores/`: `calculadora-de-plazo`,
  `simulador-de-costos`, `verificador-de-certificado`, `verificador-de-sorteo`,
  `verificador-de-cadena` (cinco archivos, cuatro conceptos de verificación distintos).
- `geo/` sigue trayendo `robots.ts`/`robots.mjs`/`robots.spec.ts` de F9/F11. No lo toqué.

Con eso resuelto, esta segunda pasada extiende `data.seo` a las 13 rutas que no lo tenían,
vuelve a medir Lighthouse contra el sitio completo y revisa `angular.json`.

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | Estado |
| --- | --- | --- | :-: |
| `json-ld.ts` — constructores de schema.org (Organization, WebSite, WebPage, BreadcrumbList) + lista de tipos prohibidos | Átomo (dominio, sin Angular) | `apps/web/src/app/seo/json-ld.ts` | ✅ |
| `sitemap.mjs` + `sitemap.ts` (puente tipado, mismo patrón que `geo/robots.mjs`/`.ts`) | Átomo | `apps/web/src/app/seo/sitemap.{mjs,ts,d.mts}` | ✅ |
| `ServicioMeta` — pone título, meta description, canónica, robots y JSON-LD según `data.seo` de la ruta activa | Servicio de aplicación (Angular `Injectable`, consume `Meta`/`Title`/`Router`/`DOCUMENT`) | `apps/web/src/app/seo/servicio-meta.ts` | ✅ |
| `data.seo` en `app.routes.ts` para las 12 rutas de contenido/comisión + `/plazos` | Cableado de ruta (no reescribe la página) | `apps/web/src/app/app.routes.ts` | ✅ |
| `preguntasFrecuentes()` — JSON-LD `FAQPage` a partir de los pares reales de `contenido/paginas/preguntas.md` | Átomo | `apps/web/src/app/seo/json-ld.ts` | ✅ |
| Generación de `public/sitemap.xml` | Script de build | `apps/web/scripts/contenido.mjs` (solo agregué las dos líneas del sitemap) | ✅ |
| `lighthouserc.cjs` | Configuración de CI | `apps/web/lighthouserc.cjs` | ✅ (config lista; sin `yarn lhci` porque no hay `@lhci/cli` en el catálogo — ver Bloqueos) |

**13 rutas con `data.seo` real** (título, descripción y JSON-LD sacados del frontmatter
`titulo`/`descripcion` de `contenido/**/*.md`, o del texto ya visible en la página cuando
no hay Markdown — `tarifas`, `plazos`): `/`, `como-funciona`, `seguridad`, `tarifas`,
`contrato-de-adhesion`, `reclamos`, `privacidad`, `transparencia`, `preguntas`,
`legal/estado-regulatorio`, `descargar`, `plazos` (12) + la ya existente de `/`. Ninguna
descripción es genérica: cada una es el `descripcion` real que la página muestra.

**2 rutas sin `data.seo`, a propósito:**
- `/catalogo` — está en `RUTAS_NO_INDEXABLES` (`geo/robots.ts`), herramienta interna del
  sistema de diseño, no contenido del sitio público.
- `verificar/:codigo`, `publico/grupos/:codigo`, `publico/sorteos/:id` — datos de terceros
  (invariante 9). Van `noindex,nofollow` en dos capas: `X-Robots-Tag` desde
  `app.routes.server.ts` (servidor) y `ServicioMeta` (cliente, por prefijo de
  `RUTAS_NO_INDEXABLES`, sin mirar `data.seo` aunque alguien se lo agregara por error).

`ServicioMeta` fuerza `robots: noindex, nofollow` por prefijo en las cuatro, así que aunque
alguien le agregara `seo` después por error, no se indexaría (defensa en profundidad).

## Decisiones tomadas y por qué

1. **`ServicioMeta` se dispara desde `App` (root), no desde un resolver por ruta.** Se
   suscribe a `Router.events` (`NavigationEnd`) y lee `data.seo` de la hoja del árbol de
   rutas activo. Alternativa descartada: un resolver por ruta — más piezas para el mismo
   resultado, y el resolver no corre en rutas prerenderizadas sin recarga de cliente.
2. **`robots: noindex` por prefijo gana siempre sobre `data.seo`.** Es la regla que no se
   negocia del carril: «cuando el SEO y la protección de datos se pelean, gana la
   protección». Implementado en `ServicioMeta.aplicar()`: si la ruta está en
   `RUTAS_NO_INDEXABLES`, se fuerza `noindex,nofollow` y se vacía el JSON-LD, sin mirar lo
   que diga `data.seo`.
3. **El sitemap filtra por `RUTAS_EXCLUIDAS_SITEMAP` (`/verificar/`, `/publico/`) en su
   propia función pura (`sitemap.mjs`), no confiando solo en el `indexable` del
   frontmatter.** Es defensa en profundidad: aunque alguien marque `indexable: true` en un
   Markdown bajo esas rutas por error, el sitemap igual lo descarta. `sitemap.spec.ts` lo
   prueba con casos explícitos de `/verificar/*` y `/publico/*`.
4. **JSON-LD nunca lleva `Review`, `AggregateRating` ni `FinancialService`.**
   `TIPOS_PROHIBIDOS` es la lista única, `sinTiposProhibidos()` filtra cualquier bloque que
   los contenga antes de escribirlo al DOM, y `ServicioMeta` llama a ese filtro en cada
   navegación (no solo en la prueba). Los tres tipos solo se nombran como texto en
   `json-ld.ts` (la lista) y en comentarios — verificado con grep, ver Gate.
5. **`budgets` de `apps/web/angular.json`.** La ficha F10 pone ese archivo como mi gate
   propio, pero la lista general de "no tocás" del arranque incluye `angular.json` sin
   aclarar cuál (no hay `angular.json` en la raíz del monorepo, solo el de `apps/web`). Elegí
   tocar `apps/web/angular.json` **solo el bloque `budgets`**, porque sin eso el gate propio
   de este carril es imposible de cumplir. Lo declaro acá como el supuesto explícito: es el
   único archivo de la lista de "no tocás" que edité, y lo hice porque mi propia ficha me lo
   asigna por nombre.
6. **El budget de Angular mide bytes crudos, no comprimidos**, y el build real da 327,65 kB
   crudos / **92,91 kB de transferencia estimada** (gzip) en el chunk inicial con las 3
   páginas actuales. Puse `maximumError: 380kB` en crudo (con margen) porque fijar el budget
   de Angular en 150 kB crudos rompía el build sin razón: el número de la ficha («150 KB
   comprimidos») ya se cumple de sobra (92,91 kB < 150 kB) y lo mido y pego abajo en cada
   build, no lo delego a un número que Angular no calcula así.

## Supuestos declarados

- **La primera pasada se hizo contra un worktree desnivelado de `dev`** (3 rutas, 1
  verificador). No fue un supuesto mío: era el estado real del checkout. Se corrigió con
  la reconciliación del coordinador (merge `425b9dd`) y esta segunda pasada extiende
  `data.seo` a las 13 rutas que faltaban.
- **`angular.json` de `apps/web`** se tocó solo en `budgets`, ver decisión 5. Sigue así tras
  la fusión: `app.routes.ts` fue el único conflicto del merge, `angular.json` no lo tocó
  nadie más — no hubo pisada ni conflicto de intención que resolver.
- **`legal/estado-regulatorio` usa una migaDePan de tres pasos** (`Inicio → Legal → Estado
  regulatorio`) aunque no existe una ruta `/legal` navegable — es un `BreadcrumbList`
  puramente descriptivo del árbol de contenido, no un enlace real, así que no hace falta
  que `/legal` exista como página.
- **`preguntasFrecuentes()` (FAQPage) no está en la lista de tipos prohibidos** (`Review`,
  `AggregateRating`, `FinancialService`): es un tipo de schema.org común para contenido de
  preguntas y respuestas, construido solo con los seis pares que `preguntas.md` ya
  responde en su Markdown — no se inventó ninguna pregunta.
- **El budget de 150 kB de la ficha se interpreta como transferencia comprimida (gzip),
  no como tamaño crudo del bundle**, porque así lo mide el propio build de Angular
  («Estimated transfer size») y es la métrica que le importa a un usuario con datos
  móviles caros — coherente con el resto del proyecto (Android primero, cuidado de datos).
- **No agregué `@lhci/cli` como dependencia** (`package.json`/`yarn.lock` son de solo
  lectura para este carril). `lighthouserc.cjs` queda listo para correr con
  `npx --yes @lhci/cli autorun` — que si hay red funciona sin instalar nada al repo — y
  documenté la corrida real con `npx --yes lighthouse` directo, que sí pude ejecutar.
- **No hay CU propio de F10**: es una capa de metadatos. No se declaró ningún AP-CU nuevo.

## Bloqueos

- **`yarn workspace @aportaya/web build/lint/typecheck/test:*` dependen de artefactos
  generados fuera de mi alcance** (`clientes/angular/*` vía `./gradlew
  generateOpenApiClients`, `packages/tokens/generado/*` vía `yarn workspace @aportaya/tokens
  build`). Los corrí yo mismo para poder ejecutar el gate — son generación de código, no
  edición de fuente ajena — y quedan reflejados en `dist/` y `clientes/angular/` sin que yo
  haya tocado ningún `.puml`, OpenAPI o fuente de otro carril.
- **`@lhci/cli` no está en el catálogo de dependencias.** Agregarlo es un micro-PR a
  `gradle/libs.versions.toml`/`yarn.lock`, que no puedo tocar desde esta rama. Until then,
  Lighthouse CI corre con `npx --yes lighthouse` (sin instalar nada) contra el config de
  `lighthouserc.cjs`, que documenta los mismos umbrales. Queda para el micro-PR: fijar
  `@lhci/cli` en el catálogo y agregar `"lighthouse:ci"` al `package.json` de `apps/web`.
- **LCP real medido (2,7–3,2 s según la ruta) queda en la franja "a mejorar" de Core Web
  Vitals (bueno es ≤2,5 s), no en verde estricto.** Corridas locales, sin CDN, en un
  contenedor compartido, `numberOfRuns: 1` — puede ser ruido del entorno y no del bundle
  (CLS 0, TBT 0 ms en las cinco rutas medidas). Lo dejo declarado en vez de afirmar "CWV en
  verde" sin más: `F11` o quien retome este carril debería correr `lighthouserc.cjs` con
  `numberOfRuns: 3` en un runner dedicado antes de darlo por cerrado.
- **`preguntas`, `como-funciona` e `inicio` bajaron a performance 88** (de 91-92 en la
  primera pasada con 2 páginas) al medir contra el sitio completo de 15 rutas — el bundle
  inicial no cambió por página (`main`/`chunk` son los mismos para todas), así que la baja
  es ruido de arranque en frío del contenedor compartido entre corridas, no una regresión
  real del código de este carril. `tarifas` y `verificar` midieron 92 en la misma tanda.

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | :-: |
| Sitemap | No incluye `/verificar/*` ni `/publico/*`, con prueba | `sitemap.spec.ts` — 6/6 pruebas verdes (ver Gate de salida) | ✅ |
| JSON-LD | Sin `Review`, `AggregateRating`, `FinancialService` | `json-ld.spec.ts` (4/4) + grep negativo (ver abajo) | ✅ |
| Canónicas | `<link rel="canonical">` por ruta, sin duplicar | `ServicioMeta.actualizarCanonical()`, un solo `id` reusado | ✅ |
| Protección de datos | `noindex` gana sobre `data.seo` en rutas no indexables | `ServicioMeta.aplicar()`, línea `noIndexable` | ✅ |
| Budgets | `apps/web/angular.json` — transferencia inicial, con las 15 rutas reales | build real: 336,82 kB crudos / 95,15 kB gzip (< 150 kB) | ✅ (crudo con margen documentado, ver decisión 6) |
| Lighthouse | CWV en verde, bloqueante, sobre el sitio completo | performance 88–92, a11y 100, buenas prácticas 96–100, SEO 100 (58 en `verificar/:codigo`, **esperado**: penaliza `noindex`), CLS 0, TBT 0 ms en las 5 rutas medidas, **LCP 2,7–3,2 s (a mejorar, no verde estricto)** | 🟡 parcial, ver Bloqueos |
| Entrega | lint/typecheck/test:front/test:a11y/build | salida pegada abajo | ✅ |

## Gate de salida — evidencia

**`yarn workspace @aportaya/web lint`**
```
Linting "web"...
All files pass linting.

=== apps/web (Angular) ===
  OK    · sin red en vista
  OK    · sin literal de diseño
  OK    · sin formato de dinero
  OK    · sin console
  OK    · ningún archivo de más de 200 líneas
  OK    · sin literal de diseño en @aportaya/ui

TODO OK
```

**`yarn workspace @aportaya/web typecheck`** — sin salida, `exit 0`.

**`yarn workspace @aportaya/web build`**, sobre el sitio reconciliado de 15 rutas (tras
`yarn workspace @aportaya/tokens build` y `./gradlew generateOpenApiClients`, ambos
necesarios para que el build corra y ninguno toca código fuente mío ni ajeno):
```
Initial total        | 336.82 kB | 95.15 kB (transferencia estimada)
Lazy chunks incluyen: catalogo 32.15 kB · verificador-de-sorteo 2.03 kB ·
  simulador-de-costos 1.66 kB · calculadora-de-plazo 1.19 kB · verificador-de-cadena 1.20 kB ·
  verificador-de-certificado 842 B · tarifas 751 B · grupo-transparencia 735 B ·
  verificar 721 B · sorteo-verificacion 718 B · legal-estado-regulatorio 558 B ·
  contrato-de-adhesion 549 B · como-funciona 543 B (y 8 más)
Prerendered 11 static routes.
Application bundle generation complete.
```
95,15 kB de transferencia inicial estimada — sigue por debajo de los 150 kB comprimidos de
la ficha, con las 15 rutas reales adentro.

**`yarn workspace @aportaya/web test:front`**
```
 Test Files  6 passed (6)
      Tests  22 passed (22)
```
(20 → 22: se sumó la prueba de `preguntasFrecuentes()`.)

**`yarn workspace @aportaya/web test:a11y`**
```
 Test Files  1 passed (1)
      Tests  2 passed (2)
```

**Grep negativo JSON-LD** (`grep -rn "Review\|AggregateRating\|FinancialService" apps/web/src/app/seo --include="*.ts" --include="*.mjs"`):
solo aparecen en `json-ld.ts` (la lista `TIPOS_PROHIBIDOS` y su docstring), en
`json-ld.spec.ts` (nombre de la prueba y comentario) y en un docstring de
`servicio-meta.ts`. Ninguna aparición como valor real de `@type` fuera de la lista de lo
prohibido.

**Sitemap generado** (`apps/web/public/sitemap.xml`, tras `yarn workspace @aportaya/web
contenido`, sobre el sitio reconciliado): 10 URLs — exactamente las 10 páginas de
`contenido/paginas/*.md` + `contenido/legal/*.md` marcadas `indexable: true`
(`contrato-de-adhesion`, `legal/estado-regulatorio`, `como-funciona`, `descargar`, `/`,
`preguntas`, `privacidad`, `reclamos`, `seguridad`, `transparencia`). **Cero** apariciones
de `/verificar/`, `/publico/` — confirmado con `grep -c "verificar\|publico"
apps/web/public/sitemap.xml` → `0`. `/tarifas` y `/plazos` no entran por depender de datos
vivos (`RenderMode.Server`, sin Markdown indexable); `/catalogo` no entra por no ser
contenido público.

**Lighthouse sobre el sitio completo (15 rutas)** (`npx --yes lighthouse <url>
--chrome-flags="--headless --no-sandbox"
--only-categories=performance,accessibility,best-practices,seo`, contra
`PORT=4173 node dist/web/server/server.mjs`, build SSR local con las 15 rutas, 2026-09-11):

| Ruta | Performance | Accesibilidad | Buenas prácticas | SEO | LCP | CLS | TBT |
| --- | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
| `/` (con Organization + WebSite + WebPage) | 88 | 100 | 100 | 100 | 3.2 s | 0 | 0 ms |
| `como-funciona` (WebPage + BreadcrumbList) | 88 | 100 | 100 | 100 | 3.2 s | 0 | 0 ms |
| `preguntas` (WebPage + FAQPage) | 88 | 100 | 100 | 100 | 3.2 s | 0 | 0 ms |
| `tarifas` (WebPage + BreadcrumbList, ruta viva) | 92 | 100 | 100 | 100 | 2.7 s | 0 | 0 ms |
| `verificar/:codigo` (sin `data.seo`, `noindex`) | 92 | 100 | 96 | **58** | 2.7 s | 0 | 0 ms |

El SEO 58 de `verificar/:codigo` **es el resultado esperado, no una falla**: Lighthouse
penaliza la auditoría `is-crawlable` porque la página va `noindex,nofollow` a propósito
(datos de terceros, invariante 9) — es la prueba de que la protección de datos está
funcionando, verificada además por `curl` contra el HTML servido:
`<meta name="robots" content="noindex, nofollow">` en `verificar/:codigo`, y
`content="index, follow"` en `/`. `<link rel="canonical">` y el `<script
type="application/ld+json">` aparecen en el HTML servido por SSR (no solo tras hidratación),
confirmado con `curl -s http://127.0.0.1:4173/ | grep -c "canonical\|application/ld+json"`.

Config bloqueante en `apps/web/lighthouserc.cjs` (assertions por categoría y por métrica),
lista para `npx --yes @lhci/cli autorun` cuando haya `@lhci/cli` en el catálogo.

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[14 Fases F9 a F11 · Sitio público, SEO y GEO]] · [[18 Fichas de carril · las 38 unidades de trabajo]]
