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

## Punto de partida real, contra lo que el arranque asumía

El arranque de este carril decía «las quince rutas y los cuatro verificadores que ya
existen». Eso **no es lo que hay en `dev`**. Lo que F9 entregó y quedó fusionado es:

- 3 rutas: `/`, `/plazos`, `/catalogo` (`apps/web/src/app/app.routes.ts`).
- 1 verificador: `calculadora-de-plazo` (`apps/web/src/app/verificadores/`).
- 1 página de contenido en `contenido/paginas/inicio.md` (no quince).
- No existe `apps/web/src/app/layout/`.
- No existe `planes/informes/carril-W.md` (el informe de F9 que el arranque decía leer).
- `geo/` ya trae `robots.ts`/`robots.mjs`/`robots.spec.ts` — no está vacío, aunque la ficha
  de `F11` dice que ese contenido es suyo. No lo toqué: es de otro carril, y ya funciona.

Esto no es un bloqueo — es la superficie real sobre la que se construyó `ServicioMeta`, el
JSON-LD y el sitemap. Lo declaro acá para que `F11` y cualquier auditoría no crean que se
perdieron doce páginas: nunca existieron en esta rama de `dev`.

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | Estado |
| --- | --- | --- | :-: |
| `json-ld.ts` — constructores de schema.org (Organization, WebSite, WebPage, BreadcrumbList) + lista de tipos prohibidos | Átomo (dominio, sin Angular) | `apps/web/src/app/seo/json-ld.ts` | ✅ |
| `sitemap.mjs` + `sitemap.ts` (puente tipado, mismo patrón que `geo/robots.mjs`/`.ts`) | Átomo | `apps/web/src/app/seo/sitemap.{mjs,ts,d.mts}` | ✅ |
| `ServicioMeta` — pone título, meta description, canónica, robots y JSON-LD según `data.seo` de la ruta activa | Servicio de aplicación (Angular `Injectable`, consume `Meta`/`Title`/`Router`/`DOCUMENT`) | `apps/web/src/app/seo/servicio-meta.ts` | ✅ |
| `data.seo` en `app.routes.ts` para `/` y `/plazos` | Cableado de ruta (no reescribe la página) | `apps/web/src/app/app.routes.ts` | ✅ |
| Generación de `public/sitemap.xml` | Script de build | `apps/web/scripts/contenido.mjs` (solo agregué las dos líneas del sitemap) | ✅ |
| `lighthouserc.cjs` | Configuración de CI | `apps/web/lighthouserc.cjs` | ✅ (config lista; sin `yarn lhci` porque no hay `@lhci/cli` en el catálogo — ver Bloqueos) |

`/catalogo` **no** lleva `data.seo`: está en `RUTAS_NO_INDEXABLES` (`geo/robots.ts`), es la
herramienta interna del sistema de diseño, no contenido del sitio público. `ServicioMeta`
igual le fuerza `robots: noindex, nofollow` por prefijo, así que aunque alguien le agregara
`seo` después por error, no se indexaría (defensa en profundidad).

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

- **No hay quince páginas ni cuatro verificadores en `dev`.** Se trabajó con las 3 rutas y
  el 1 verificador reales. `ServicioMeta` está escrito para escalar sin cambios cuando
  aparezcan más páginas: cada una solo necesita su `data.seo`.
- **`angular.json` de `apps/web`** se tocó solo en `budgets`, ver decisión 5.
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
- **LCP real medido (2,7–2,9 s) queda en la franja "a mejorar" de Core Web Vitals (bueno es
  ≤2,5 s), no en verde estricto.** Es una sola corrida local, sin CDN, en un contenedor
  compartido — puede ser ruido del entorno y no del bundle (CLS 0, TBT 0 ms, todo lo demás
  100). Lo dejo declarado en vez de afirmar "CWV en verde" sin más: `F11` o quien retome
  este carril debería correr `lighthouserc.cjs` con `numberOfRuns: 3` en un runner
  dedicado antes de darlo por cerrado.

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | :-: |
| Sitemap | No incluye `/verificar/*` ni `/publico/*`, con prueba | `sitemap.spec.ts` — 6/6 pruebas verdes (ver Gate de salida) | ✅ |
| JSON-LD | Sin `Review`, `AggregateRating`, `FinancialService` | `json-ld.spec.ts` (4/4) + grep negativo (ver abajo) | ✅ |
| Canónicas | `<link rel="canonical">` por ruta, sin duplicar | `ServicioMeta.actualizarCanonical()`, un solo `id` reusado | ✅ |
| Protección de datos | `noindex` gana sobre `data.seo` en rutas no indexables | `ServicioMeta.aplicar()`, línea `noIndexable` | ✅ |
| Budgets | `apps/web/angular.json` — transferencia inicial | build real: 92,91 kB gzip (< 150 kB) | ✅ (crudo con margen documentado, ver decisión 6) |
| Lighthouse | CWV en verde, bloqueante | performance 91–92, a11y 100, buenas prácticas 100, SEO 100, CLS 0, TBT 0 ms, **LCP 2,7–2,9 s (a mejorar, no verde estricto)** | 🟡 parcial, ver Bloqueos |
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

**`yarn workspace @aportaya/web build`** (tras `yarn workspace @aportaya/tokens build` y
`./gradlew generateOpenApiClients`, ambos necesarios para que el build corra y ninguno toca
código fuente mío ni ajeno):
```
contenido: 1 páginas · 1 indexables · robots.txt y sitemap.xml generados
Initial total        | 327.65 kB | 92.91 kB (transferencia estimada)
Lazy chunk files: catalogo 32.13 kB · calculadora-de-plazo 2.06 kB · inicio 980 B · plazos 984 B
Prerendered 2 static routes.
Application bundle generation complete.
```

**`yarn workspace @aportaya/web test:front`**
```
 Test Files  6 passed (6)
      Tests  20 passed (20)
```

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
contenido`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://aportaya.bo/</loc>
    <lastmod>2026-09-09</lastmod>
  </url>
</urlset>
```
Solo la página indexable real (`inicio`). `/plazos` y `/catalogo` no están en `contenido/`
como Markdown indexable, así que no entran al sitemap por ese único camino — correcto para
`/catalogo` (no indexable), pendiente de revisar para `/plazos` si algún día se vuelve
contenido estático indexable (hoy es `RenderMode.Server`, con datos vivos, y no debería
entrar a un sitemap de todos modos).

**Lighthouse** (`npx --yes lighthouse <url> --chrome-flags="--headless --no-sandbox"
--only-categories=performance,accessibility,best-practices,seo`, contra
`PORT=4173 node dist/web/server/server.mjs`, build SSR local, 2026-09-11):

| Ruta | Performance | Accesibilidad | Buenas prácticas | SEO | LCP | CLS | TBT |
| --- | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
| `/` | 91 | 100 | 100 | 100 | 2.9 s | 0 | 0 ms |
| `/plazos` | 92 | 100 | 100 | 100 | 2.7 s | 0 | 0 ms |

Config bloqueante en `apps/web/lighthouserc.cjs` (assertions por categoría y por métrica),
lista para `npx --yes @lhci/cli autorun` cuando haya `@lhci/cli` en el catálogo.

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[14 Fases F9 a F11 · Sitio público, SEO y GEO]] · [[18 Fichas de carril · las 38 unidades de trabajo]]
