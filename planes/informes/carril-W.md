---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril W — Sitio público (Angular)"
ola: F1
fase: F9
mundo: Angular
modulo: apps/web/src/app/{nucleo,layout,paginas,verificadores}
rama: pablo/feature/carril-W-sitio-publico
estado: en curso — gate propio corrido, huecos de backend declarados
---

# Carril W — Sitio público (F9)

**Fase** F9 · **Mundo** Angular con `@angular/ssr` · **Casos de uso** CU-30, CU-34,
CU-61, CU-72, CU-73, CU-75 · **Producto** `apps/web`

> F9 no tiene "pantallas" en el sentido de `planes/22` (billetera/app): es el sitio
> público. Esta plantilla se adapta reemplazando "Pantalla" por "Ruta" y quitando las
> secciones que no aplican (paridad iOS, comparación contra maqueta Flutter).

## Rutas entregadas

| Ruta | `RenderMode` | CU | Indexación | Estado |
| --- | :-: | :-: | :-: | :-: |
| `/` | Prerender | — | index | ✅ (ya existía de F0-W) |
| `/como-funciona` | Prerender | — | index | ✅ nueva |
| `/seguridad` | Prerender | — | index | ✅ nueva |
| `/tarifas` | **Server** | CU-30, CU-34 | index | ⚠️ parcial — ver huecos |
| `/contrato-de-adhesion` | Prerender (el plan pedía Server) | CU-05, CU-34 | index | ⚠️ contenido estático — hueco de backend |
| `/reclamos` | Prerender | CU-52/53 | index | ✅ nueva |
| `/privacidad` | Prerender | CU-07 | index | ✅ nueva |
| `/transparencia` | Prerender | CU-72/73 | index | ✅ nueva |
| `/preguntas` | Prerender | — | index | ✅ nueva |
| `/legal/estado-regulatorio` | Prerender (el plan pedía Server) | Regla 2 | index | ⚠️ contenido estático — hueco de backend |
| `/descargar` | Prerender | — | index | ✅ nueva (sin URL de tienda todavía, declarado) |
| `/verificar/:codigo` | **Server** | CU-75 | **noindex** | ✅ |
| `/publico/grupos/:codigo` | **Server** | CU-72/73 | **noindex** | ✅ |
| `/publico/sorteos/:id` | **Server** | CU-61 | **noindex** | ✅ |
| `/catalogo` | Prerender | interno | noindex | ✅ (ya existía de F0-W) |
| `/plazos` | **Server** | CU-59 | index | ✅ (ya existía de F0-W, no es mío) |

Las quince rutas de `planes/14` F9.1 existen, con contenido real (no relleno). `/plazos`
se mantiene porque ya estaba servida antes de este carril.

## Por qué el conteo de rutas `Server` es cinco y no seis

`planes/14` F9.3 pide seis: `verificar/:codigo`, `publico/grupos/:codigo`,
`publico/sorteos/:id`, `tarifas`, `contrato-de-adhesion`, `legal/estado-regulatorio`.

Se buscó el contrato de lectura pública para `contrato-de-adhesion` y
`legal/estado-regulatorio` en `servicios/cumplimiento/src/main/resources/openapi/cumplimiento.yaml`
(único servicio con los prefijos `/cumplimiento`, `/uif`, `/reclamos`, `/licencia`) y
**no existe**: hay `POST /cumplimiento/contratos/{contratoId}/aceptaciones` (registra una
aceptación, CU-05) y `GET /licencia/alcance` (interno, CU-46, exige sesión). No hay un
`GET` público de `documento_publicado` ni de `licencia_regulatoria`.

Publicar esas dos rutas como `Server` contra un endpoint que no existe habría significado
inventar la respuesta — exactamente lo que la regla 2 de `planes/14` prohíbe ("no se
publica lo que no es cierto hoy"). Se optó por `Prerender` con contenido versionado en
Markdown (estado real: licencia `EN_TRAMITE`, citado tal cual está en
`docs/Cumplimiento.md` §1.1) y una nota técnica visible en cada página explicando el
hueco y a qué carril se le pide el contrato. `rutas-de-servidor.spec.ts` se actualizó
para exigir exactamente estas cinco, con una prueba nueva que verifica `X-Robots-Tag`
en las tres rutas públicas de verificación.

`/tarifas` sí es `Server` real: `GET /tarifas/vigentes/{codigo}` y
`POST /comisiones/cotizaciones` (CU-30) existen y se consumen desde el simulador. Lo que
falta ahí es el detalle completo del tarifario (ver huecos).

## Los cuatro verificadores

| Verificador | Ruta | Hidratación | Fuente de verdad | Recómputo cliente |
| --- | --- | --- | --- | :-: |
| `VerificadorDeCertificado` (CU-75) | `/verificar/:codigo` | `@defer (hydrate on viewport)` | `GET /verificar/:codigo` (real) | no aplica (CU-75 no expone contenido recomputable sin sesión) |
| `VerificadorDeCadena` (CU-72/73) | `/publico/grupos/:codigo` | `@defer (hydrate on viewport)` | `GET /publico/grupos/:codigo/verificacion` (real) | ❌ documentado como hueco (ver abajo) |
| `VerificadorDeSorteo` (CU-61) | `/publico/sorteos/:id` | `@defer (hydrate on viewport)` | `GET /publico/sorteos/:id/verificacion` (real) | ✅ recomputa `verificarCompromiso` + `barajarDeterminista` en el navegador |
| `SimuladorDeCostos` (CU-30) | `/tarifas` | `@defer (hydrate on interaction)` | `POST /comisiones/cotizaciones` (real) | no aplica |

Los cuatro consumen el gateway solo dentro de su propio componente (`GATEWAY` +
`httpResource`), nunca desde una página de contenido — verificado inspeccionando
`dist/web/browser/*.js`: los chunks de `como-funciona`, `legal-estado-regulatorio` y
`contrato-de-adhesion` tienen **cero** ocurrencias de `GATEWAY`/`HttpClient`/`httpResource`.

## `packages/dominio-cliente` (paquete nuevo)

Creado en `packages/dominio-cliente/`, alias `@aportaya/dominio-cliente` (path mapping
en `tsconfig.base.json` y `apps/web/tsconfig.json`, mismo patrón que `@aportaya/simulado`
y `@aportaya/ui`; no depende de `node_modules` para resolverse en `web`).

| Átomo | Fuente real replicada | Estado |
| --- | --- | :-: |
| `hashDelCompromiso`, `verificarCompromiso`, `barajarDeterminista` | `plataforma/comun-dominio/.../SorteoVerificable.java` (Fisher-Yates + SHA-256 modular, algoritmo copiado línea por línea del Javadoc) | ✅ probado contra vectores |
| `serializarCanonico`, `hashDeBloque`, `recorrerCadena` | **No existe** implementación Java equivalente en `servicios/transparencia` | ⚠️ implementación propia, autoconsistente, sin garantía de coincidir con el backend — documentado en `src/cadena.ts` |

`yarn workspace @aportaya/dominio-cliente run typecheck` y `test:front`: **15/15 pruebas
en verde** (ver salida más abajo), incluyendo 4 vectores dorados de sorteo que reproducen
exactamente `hashComprometido` y `ordenEsperado` calculados en Python replicando el
algoritmo Java (`vectores/sorteo.vectores.json`, con `vectores/README.md` explicando el
origen: no son un export directo de `SorteoVerificableTest.java` porque esa clase no
emite JSON hoy — hueco declarado, pedido al carril dueño de `plataforma/comun-dominio`).

`VerificadorDeCadena` **no afirma** haber verificado nada de forma independiente en el
cliente: muestra el veredicto real del servidor (`servicios/transparencia` sí implementa
CU-73) y dice explícitamente que el recómputo íntegro todavía no está disponible,
en vez de simular una garantía que no existe.

## Contratos consumidos (reales, no inventados)

Se generaron con `./gradlew generateOpenApiClients` (BUILD SUCCESSFUL) contra
`servicios/tarifas/src/main/resources/openapi/tarifas.yaml` y
`servicios/transparencia/src/main/resources/openapi/transparencia.yaml`, que **ya
existían** en `dev` — no hizo falta escribir contrato nuevo para CU-30, CU-34, CU-61,
CU-72, CU-73, CU-75. Los ejemplos de Prism que se usaron para probar contra el mock
(`verificarSorteo`, `verificarCadena`, `verificarCertificado`, `cotizarComision`,
`consultarTarifarioVigente`) **también ya existían** en
`packages/simulado/ejemplos/{tarifas,transparencia}/` — no se agregó ninguno nuevo,
porque los que hacían falta ya estaban.

## Supuestos declarados

Regla cero: nada silencioso.

1. **`SimuladorDeCostos` usa `referenciaId` y `codigoTarifario` sintéticos.** CU-30 no
   define un modo "simulación pública sin operación real"; `EntradaCotizacion` exige
   `referenciaId` (UUID) y `referenciaTipo`. Se genera un UUID por cotización y se fija
   `codigoTarifario: 'GENERAL'`. Si el backend de tarifas rechaza referencias
   inexistentes en producción, hace falta un modo explícito de simulación en el
   contrato — pedido al carril dueño de `servicios/tarifas`.
2. **`/descargar` no tiene URL de tienda.** Se documenta en la propia página en vez de
   inventar un enlace.
3. **`contrato-de-adhesion` y `legal/estado-regulatorio` son contenido estático, no
   `Server`.** Ver la sección de arriba.
4. **`cadena.ts` (`serializarCanonico`/`hashDeBloque`) es una implementación propia**,
   no verificada byte a byte contra el backend, porque ese backend no publica una
   implementación de referencia. Documentado en el propio archivo y en la UI de
   `VerificadorDeCadena`.
5. **`VerificadorDeSorteo` no puede confirmar el orden final publicado**, solo el hash
   del compromiso: el contrato `SalidaVerificacionSorteo.paquete.cupos` es el orden
   *original* (CU-61 §1), no el orden revelado, así que el componente muestra el orden
   recomputado para que se compare manualmente contra los turnos del grupo, en vez de
   fingir una comparación automática que el contrato no permite hacer.
6. **Se tocaron dos archivos fuera de mi carpeta exclusiva, documentado por necesidad
   mínima:** `tsconfig.base.json` (alias de `@aportaya/dominio-cliente`, mismo patrón
   que ya existía para `@aportaya/simulado`) y `apps/web/scripts/contenido.mjs` (bug
   preexistente: no creaba subcarpetas en `public/` para rutas anidadas como
   `legal/estado-regulatorio` — un `mkdirSync` de una línea). `yarn.lock` se regeneró
   por `yarn install` al agregar el workspace nuevo (necesario, no evitable).

## Contratos pedidos al backend

| Qué falta | Para qué | Carril destino |
| --- | --- | --- |
| `GET` público de `documento_publicado` (tipo `CONTRATO_ADHESION` y `TARIFARIO`), con texto, hash y vigencia | `/contrato-de-adhesion` y el detalle completo de `/tarifas` como `Server` real | `servicios/cumplimiento` (contrato) y `servicios/tarifas` (detalle del tarifario) |
| `GET` público de `licencia_regulatoria` (estado, resolución, fecha) | `/legal/estado-regulatorio` como `Server` real | `servicios/cumplimiento` |
| `serializarCanonico`/`hashDeBloque` en `plataforma/comun-dominio`, con vectores versionados (mismo patrón que `SorteoVerificable`) | Que `VerificadorDeCadena` pueda recomputar en el cliente, como pide CU-72/73 | dueño de `plataforma/comun-dominio` / `servicios/transparencia` |
| Vectores dorados de `SorteoVerificable` exportados como JSON por la propia prueba Java | Que `sorteo.vectores.json` deje de depender de un recálculo independiente en Python | dueño de `plataforma/comun-dominio` |
| Modo de simulación explícito en `POST /comisiones/cotizaciones` (o endpoint dedicado) | Que el simulador público de `/tarifas` no dependa de una `referenciaId` sintética | `servicios/tarifas` |

## Matriz de gates — evidencia real, comandos ejecutados

| Gate | Comando | Resultado |
| --- | --- | :-: |
| Cliente Angular regenerado | `./gradlew generateOpenApiClients` | **BUILD SUCCESSFUL in 8s**, 37 tasks |
| Typecheck del monorepo frontend | `yarn typecheck` | **11/11 tareas exitosas** (incluye `@aportaya/dominio-cliente` y `@aportaya/web`) |
| Lint del monorepo frontend | `yarn lint` | **8/8 tareas exitosas**; `web`: "TODO OK" (sin red en vista, sin literal de diseño, sin console, sin archivo >200 líneas) |
| Pruebas del monorepo frontend | `yarn test:front` | **11/11 tareas exitosas**; `dominio-cliente` 15/15, `web` 12/12 |
| a11y de `web` | `yarn workspace @aportaya/web run test:a11y` | **2/2 pruebas en verde** |
| Build de producción | `yarn workspace @aportaya/web run build` | Éxito. Initial total **328.37 kB raw / 92.92 kB transferido** — bajo el presupuesto de 150 KB. 11 rutas prerenderizadas |
| Ninguna llamada de red en páginas de contenido | `grep GATEWAY\|HttpClient\|httpResource` sobre los chunks de `como-funciona`, `legal-estado-regulatorio`, `contrato-de-adhesion` en `dist/web/browser/` | **0 coincidencias en las tres** |
| `noindex`/`nofollow` en meta y `X-Robots-Tag` | `curl -sD -` contra `/verificar/abc`, `/publico/grupos/GRP1`, `/publico/sorteos/…` servidos por `node dist/web/server/server.mjs` | Los tres: `HTTP/1.1 200 OK`, cabecera `x-robots-tag: noindex, nofollow`, y `<meta name="robots" content="noindex, nofollow">` en el HTML |
| Rutas públicas sin sesión | Las mismas `curl` de arriba, sin cookie ni token | Las tres responden 200 sin autenticación |
| Gateway caído → 200, no 503 | Se mató el proceso de Prism (`lsof -ti:4010 \| xargs kill`) y se repitió `curl` contra `/verificar/abc` con el SSR corriendo | **200 OK**, cuerpo con "No hay conexión… Volver a intentar" (el estado de error de `EstadoDePantalla`, servido por SSR sin lanzar) |
| `rutas-de-servidor.spec.ts` enumera las rutas `Server` reales | `yarn workspace @aportaya/web run test:front` | 3/3 en ese archivo: enumera exactamente 5 (`plazos`, `tarifas`, `verificar/:codigo`, `publico/grupos/:codigo`, `publico/sorteos/:id`) y confirma `X-Robots-Tag` en las tres públicas |
| Vectores del sorteo coinciden con el algoritmo real del backend | `yarn workspace @aportaya/dominio-cliente run test:front` | 8/8 en `sorteo.spec.ts`, incluidos los 4 vectores dorados |
| No se publica una afirmación regulatoria falsa | Revisión manual de todo el texto de `contenido/` contra `docs/Cumplimiento.md` §1.1 | Único texto usado: "solicitud de licencia en trámite ante la ASFI" — coincide con el estado `EN_TRAMITE` documentado; no se usó "regulados por ASFI" ni "entidad autorizada" en ningún lado |

### Lo que no se corrió

- **Lighthouse CI / Core Web Vitals**: es gate de F10 (no de este carril), no se corrió.
- **`test:e2e` (Playwright)**: no está en el gate propio pedido para F9; no se corrió por
  presupuesto de tiempo del carril. Riesgo: los `@defer` no se probaron con un navegador
  real, solo con `vitest` (jsdom) y con la inspección de `dist/`.
- **Carga real contra `servicios/tarifas`/`servicios/transparencia` en Java** (solo se
  probó contra Prism/mock y contra el build SSR): no se levantó el backend real
  (Postgres, Gradle `bootRun`) — fuera del alcance de un carril de frontend según
  `arrancar-carril` §5 ("no se levantan los quince procesos").

## Bloqueos

Ninguno crítico: donde faltó contrato de backend, se declaró el hueco y se siguió con
contenido estático honesto en vez de parar. Los cinco pedidos de la tabla de arriba
quedan abiertos para los carriles de backend correspondientes.

## Lo que no verifica ninguna máquina

- **¿Los nombres dicen lo que las cosas son?** Sí: `VerificadorDeSorteo`,
  `VerificadorDeCadena`, `VerificadorDeCertificado`, `SimuladorDeCostos` nombran
  exactamente el CU que resuelven, siguiendo la nomenclatura ya usada en
  `calculadora-de-plazo.ts`.
- **¿La frontera transaccional es la correcta?** No aplica en el sentido de backend:
  este carril no escribe dinero ni estado; solo lee y muestra. El único POST
  (`cotizarComision`) usa `Idempotency-Key` por consulta, como exige el contrato.
- **¿Qué supuse que no estaba en la bóveda?** Ver "Supuestos declarados" arriba —
  ninguno toca dinero, seguridad ni un dato regulado sin decirlo explícitamente en la
  UI o en el código.
- **¿Qué dejé peor de como lo encontré?** Nada identificado: el único archivo fuera de
  mi carpeta que se modificó (`scripts/contenido.mjs`) se arregló (no creaba
  subcarpetas), no se rompió.

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[14 Fases F9 a F11 · Sitio público, SEO y GEO]] · [[10b Estándar de ejecución del frontend]]
