---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril W2 — GEO (Angular)"
ola: F2
fase: F11
mundo: Angular
modulo: apps/web/src/app/geo
rama: pablo/feature/carril-W2-geo
estado: en curso
---

# Carril W2 — GEO

**Fase** F11 · **Mundo** Angular · **Casos de uso** ninguno propio (capa de descubribilidad
para modelos generativos sobre F9/F10) · **Puesto** P5 · Dell B · T7

> Este archivo lo escribe solo este carril. La ficha está en `planes/18` (`F11 · GEO`); la
> fase, en `planes/14 Fases F9 a F11 · Sitio público, SEO y GEO.md` §FASE F11.

## Punto de partida real

Paso cero ejecutado: `mirror/dev` (`7bb1936`) trae ya fusionados F9 (sitio público, 15
rutas) y W1/F10 (`ServicioMeta`, JSON-LD, sitemap). Al arrancar, `apps/web/src/app/geo/`
**ya existía** con `robots.ts` / `robots.mjs` / `robots.spec.ts` / `robots.d.mts`
(scaffoldeados en el commit `f1130d9` de migración a Angular, y usados ya por W1). Es
decir: **la política de rastreadores (ADR-042) y `public/robots.txt` ya estaban
implementados y probados** antes de que este carril tocara nada — verificado corriendo
`robots.spec.ts` (3/3 en verde) y comparando el contenido generado contra el ADR.

Lo que faltaba, y es lo que hizo este carril: `llms.txt`, `llms-full.txt` (F11.2), la guía
de redacción (F11.4/F11.5) y la primera medición (F11.6). Los espejos `.md` por página
(F11.3) también ya los generaba `scripts/contenido.mjs` desde antes (parte de F9/F10); no
hubo que crearlos, solo verificarlos.

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | Estado |
| --- | --- | --- | :-: |
| `llms.mjs` — agrupa las páginas indexables sin datos de terceros en categorías editoriales y arma `llms.txt` | Átomo (función pura, sin Angular) | `apps/web/src/app/geo/llms.mjs` | ✅ nuevo |
| `llms.ts` — puente tipado, mismo patrón que `geo/robots.ts` | Átomo | `apps/web/src/app/geo/llms.ts` | ✅ nuevo |
| `llms.d.mts` | Tipos | `apps/web/src/app/geo/llms.d.mts` | ✅ nuevo |
| `llms.spec.ts` — 9 pruebas: listado, exclusión de terceros, determinismo, `llms-full.txt` | Prueba | `apps/web/src/app/geo/llms.spec.ts` | ✅ nuevo |
| `guia-de-redaccion.md` — las nueve técnicas de F11.4 + las diez preguntas de F11.5 + estado real de cobertura | Documentación | `apps/web/src/app/geo/guia-de-redaccion.md` | ✅ nuevo |
| Generación de `public/llms.txt` y `public/llms-full.txt` | Script de build | `apps/web/scripts/contenido.mjs` (agregué el import y dos `writeFileSync`, sin tocar lo de F9/F10) | ✅ |
| `robots.ts`/`robots.mjs`/`robots.spec.ts`/`public/robots.txt` | ya existente | `apps/web/src/app/geo/` | ✅ heredado, verificado, no modificado |
| Espejos `.md` por página (`public/*.md`) | ya existente | generados por `scripts/contenido.mjs` (parte de F9/F10) | ✅ heredado, verificado |

## Decisiones tomadas y por qué

1. **`llms.txt` agrupa por categoría con una lista estática (`CATEGORIAS`) en vez de leer
   una categoría del frontmatter.** El frontmatter de `contenido/**/*.md` (que no puedo
   editar: es de F9) no tiene campo de categoría. Alternativa descartada: pedirle el campo
   a F9 y bloquearme — en cambio declaro el mapeo en mi propio archivo (`geo/llms.mjs`),
   que es mío, y cualquier ruta indexable que no esté en la lista cae en "Más páginas" en
   vez de desaparecer, así un contenido nuevo no rompe el generador en silencio.
2. **`llms-full.txt` reusa el mismo `markdown` que ya produce `scripts/contenido.mjs`** (el
   cuerpo sin frontmatter de cada página), no vuelve a leer `contenido/` — una sola fuente
   de verdad para el espejo `.md`, `llms.txt` y `llms-full.txt`.
3. **La exclusión de terceros se declara dos veces, por diseño (defensa en profundidad):**
   `RUTAS_EXCLUIDAS_LLMS` en `geo/llms.mjs` (igual patrón que `RUTAS_EXCLUIDAS_SITEMAP` en
   `seo/sitemap.mjs`), más el hecho de que `/verificar/*` y `/publico/*` ni siquiera tienen
   `contenido/**/*.md` ni `data.seo` — nunca llegan a la lista de páginas. Verificado con
   grep, ver Gate.
4. **No edité `ServicioMeta`.** Ya declara el campo `alternateMarkdown` en `MetaDeRuta`
   (comentario: «F11 agrega esto acá»), pero **no lo usa** — `aplicar()` no emite ningún
   `<link rel="alternate" type="text/markdown">`. Y `app.routes.ts` (frozen, de F9) no le
   pasa `alternateMarkdown` en ningún `data.seo`. No toqué ninguno de los dos: lo documento
   como hueco (ver abajo) en vez de editar código ajeno.
5. **La guía de redacción vive en `apps/web/src/app/geo/guia-de-redaccion.md`**, no en
   `planes/`, porque la ficha F11 la lista junto con mis otros entregables técnicos
   (`apps/web/src/app/geo/`) y porque es el lugar que consulta quien escribe contenido
   nuevo, junto al código que la aplica.

## Supuestos declarados

- **`docs/Arquitectura/ADR-042 Política de rastreadores de IA.md` es el ADR correcto**,
  aunque `planes/14` y la ficha F10 lo citan como "ADR-038" en el cuerpo del texto — ADR-038
  real es "Acceso administrativo · segundo factor". Es un error de referencia cruzada en el
  plan, no ambigüedad real: el archivo `ADR-042 Política de rastreadores de IA.md` es
  inequívocamente el que describe exactamente lo que `robots.mjs` implementa.
- **`llms.txt` no lleva `/tarifas` ni `/contrato-de-adhesion`** aunque están en el mapa de
  sitio (F9.1) como rutas indexables: esas dos páginas se sirven en `RenderMode.Server`
  desde el backend (tarifario, contrato con hash) y **no tienen `contenido/**/*.md`** — no
  hay ningún `markdown` estático que espejar. No es un bug de mi generador: simplemente no
  hay contenido de origen para esas rutas en `contenido/`. Documentado acá en vez de
  inventarles un `.md`.
- **No corrí `./gradlew generateOpenApiClients` ni `yarn workspace @aportaya/tokens
  build`** (como sí hizo W1) para intentar un `yarn build`/`yarn test:front` completo: no
  encontré `gradlew` en este worktree (es el repo `PasanakuFrontend`, no el monorepo
  completo con backend), y el disco del entorno llegó a quedarse sin espacio durante este
  carril (ver Bloqueos). Corrí el gate específico de mi ficha (generador dos veces, grep
  negativo, `vitest run src/app/geo/`, `yarn lint`, `yarn typecheck`) con evidencia real
  pegada abajo, y no reclamo haber corrido `yarn build` completo.
- **La medición de F11.6 (las diez preguntas en los cuatro motores) no se pudo ejecutar
  de verdad**: este agente corre en un sandbox sin acceso a internet real hacia
  chat.openai.com, claude.ai, perplexity.ai ni gemini.google.com. Documento el
  procedimiento exacto y dejo la primera fila de la tabla marcada como pendiente de
  ejecución humana, en vez de inventar un resultado.

## Huecos encontrados

1. **`ServicioMeta` declara `alternateMarkdown` pero no lo emite.** El campo existe en la
   interfaz `MetaDeRuta` (`apps/web/src/app/seo/servicio-meta.ts`), con el comentario «F11
   agrega esto acá», pero el método `aplicar()` nunca lo lee ni escribe el
   `<link rel="alternate" type="text/markdown" href="…">` que pide F11.3. **Pedido a W1**:
   agregar en `aplicar()` algo equivalente a
   `if (seo?.alternateMarkdown) this.actualizarAlternate(seo.alternateMarkdown)` (mismo
   patrón que `actualizarCanonical`). No lo hice yo porque `apps/web/src/app/seo/` es
   solo lectura para este carril.
2. **Ninguna ruta de `app.routes.ts` pasa `alternateMarkdown` en su `data.seo`.**
   Consecuencia directa del hueco 1: aunque `ServicioMeta` lo implementara hoy, no hay
   ningún valor que leer. `app.routes.ts` es frozen para este carril (de F9). Pedido: al
   resolver el hueco 1, cada `data.seo` de página indexable debería agregar
   `alternateMarkdown: '/<ruta>.md'`.
3. **De las diez preguntas objetivo (F11.5), solo tres tienen un encabezado que es
   exactamente la pregunta**, y tres más tienen una variante muy cercana. Cuatro no
   tienen ningún encabezado dedicado en ningún `contenido/**/*.md`. Tabla completa:

   | # | Pregunta objetivo | Encabezado real encontrado | Página | Estado |
   | :-: | --- | --- | --- | :-: |
   | 1 | ¿Qué es un pasanaku? | `## ¿Qué es un pasanaku?` | `inicio.md` | ✅ exacto |
   | 2 | ¿Cómo funciona un pasanaku digital? | `## El pasanaku, en cinco pasos` | `como-funciona.md` | 🟡 responde, no en forma de pregunta |
   | 3 | ¿Qué es AportaYa? | — (solo en la bajada de `inicio.md`) | `inicio.md` | ⬜ sin encabezado propio |
   | 4 | ¿Cuánto cobra AportaYa? | `## ¿Cuánto cobra AportaYa?` | `preguntas.md` | ✅ exacto |
   | 5 | ¿Es seguro? ¿Dónde está mi dinero? | `## Custodia` / `## Encaje` / `## Trazabilidad` | `seguridad.md` | ⬜ sin encabezado en forma de pregunta |
   | 6 | ¿Qué pasa si alguien del grupo no paga? | `## ¿Qué pasa si alguien del grupo no aporta?` | `preguntas.md` | 🟡 variante cercana ("aporta" vs "paga") |
   | 7 | ¿Puedo retirar mi dinero cuando quiera? | — | — | ⬜ no encontrado en ningún `contenido/**` |
   | 8 | ¿AportaYa está regulada en Bolivia? | `## ¿AportaYa está regulado por ASFI?` | `preguntas.md` | 🟡 variante cercana |
   | 9 | ¿Cómo hago un reclamo? | `## ¿Qué hago si tengo un reclamo?` | `preguntas.md` | 🟡 variante cercana |
   | 10 | ¿Cómo sé que el sorteo de turnos no está arreglado? | `## ¿Cómo sé que el sorteo de turnos no está arreglado?` | `preguntas.md` | ✅ exacto |

   `contenido/**` es de F9 y este carril no lo edita. Pedido a quien posea `contenido/`:
   agregar (o reformular como pregunta) los encabezados de las filas 2, 3, 5 y 7, y unificar
   la redacción de 6, 8 y 9 con la pregunta objetivo exacta. Documentado también en
   `guia-de-redaccion.md`.
4. **`RUTAS_NO_INDEXABLES` de `geo/robots.ts` no incluye `/plazos`.** No es un hueco de
   GEO (esa ruta es indexable a propósito, CU-59), lo anoto solo porque lo noté al leer
   `app.routes.ts`: no hace falta acción de este carril.

## Gate de salida — evidencia

### 1 · Determinismo del generador de espejos (`.md`, `llms.txt`, `llms-full.txt`)

```
$ node scripts/contenido.mjs
contenido: 10 páginas · 10 indexables · robots.txt, sitemap.xml, llms.txt y llms-full.txt generados
$ cp public/llms.txt /tmp/llms_run1.txt && cp public/llms-full.txt /tmp/llms_full_run1.txt
$ node scripts/contenido.mjs
contenido: 10 páginas · 10 indexables · robots.txt, sitemap.xml, llms.txt y llms-full.txt generados
$ diff /tmp/llms_run1.txt public/llms.txt && echo "LLMS.TXT DETERMINISTA: sin diferencias"
LLMS.TXT DETERMINISTA: sin diferencias
$ diff /tmp/llms_full_run1.txt public/llms-full.txt && echo "LLMS-FULL.TXT DETERMINISTA: sin diferencias"
LLMS-FULL.TXT DETERMINISTA: sin diferencias
```

Los espejos `.md` por página los produce el mismo script desde antes (F9/F10); no se
reescriben a mano — confirmado por lectura de `scripts/contenido.mjs` (no hay ningún
`public/*.md` versionado a mano, todos salen del bucle `for (const carpeta of
readdirSync(CONTENIDO))`).

### 2 · `llms.txt`/`llms-full.txt` no exponen rutas con datos de terceros

```
$ grep -n "\](https://aportaya.bo/verificar\|\](https://aportaya.bo/publico" public/llms.txt public/llms-full.txt
   (sin salida)
$ echo "SIN ENLACES: ningún href de llms.txt/llms-full.txt apunta a /verificar/ ni /publico/"
SIN ENLACES: ningún href de llms.txt/llms-full.txt apunta a /verificar/ ni /publico/
```

Nota: un grep literal de la cadena `/publico/` sobre `llms-full.txt` sí encuentra dos
líneas — son prosa de la página `transparencia.md` que **explica que existe** la ruta
pública de verificación (`/publico/sorteos/:id`, `/publico/grupos/:codigo`), no un enlace
ni una entrada indexada. El grep de arriba, restringido a enlaces markdown (`](https://…`),
confirma que no hay ningún `href` hacia esas rutas. `RUTAS_EXCLUIDAS_LLMS` en `geo/llms.mjs`
además impide que esas rutas mismas entren alguna vez como entrada propia (probado en
`llms.spec.ts`).

### 3 · FAQ prerenderizado, nunca en `@defer`

```
$ grep -rln "@defer" apps/web/src/app/paginas/
apps/web/src/app/paginas/verificar/verificar.ts
apps/web/src/app/paginas/tarifas/tarifas.ts
apps/web/src/app/paginas/publico-sorteos/sorteo-verificacion.ts
apps/web/src/app/paginas/publico-grupos/grupo-transparencia.ts
apps/web/src/app/paginas/plazos/plazos.ts
```

`preguntas.ts` **no está en esa lista**: renderiza `contenido.json['preguntas'].html`
directo en el `template` del componente (`[innerHTML]="pagina.html"`), sin `@defer`.
Confirma F9.3: los únicos cuatro fragmentos diferidos son los verificadores
(`verificar`, `publico-sorteos`, `publico-grupos`, `tarifas`/`plazos`), todos con datos
propios de API — ninguno es contenido de FAQ.

### 4 · Pruebas propias (`vitest`)

```
$ yarn vitest run src/app/geo/
 Test Files  2 passed (2)
      Tests  9 passed (9)
```

(`robots.spec.ts`: 3 pruebas, heredado de F9/F11-scaffolding · `llms.spec.ts`: 6 pruebas,
nuevas de este carril.)

### 5 · Lint y typecheck

```
$ yarn lint
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

`yarn typecheck`: los únicos errores son `TS2307: Cannot find module 'clientes/angular/…'`
en `apps/web/src/app/verificadores/*` — preexistentes, no de este carril (esos módulos se
generan con `./gradlew generateOpenApiClients`, que no está disponible en este worktree,
ver Supuestos). **Ningún archivo de `geo/` aparece en la lista de errores.**

### 6 · Medición GEO en los cuatro motores (F11.6)

**Procedimiento documentado** (para quien lo ejecute con acceso real a internet, una vez
por mes):

1. Abrir, sin sesión iniciada y sin historial de conversación previo sobre AportaYa, cada
   una de las diez preguntas de `guia-de-redaccion.md` §Las diez preguntas objetivo, una
   por una, en: ChatGPT (con búsqueda activada), Claude (con búsqueda web activada),
   Perplexity, Gemini.
2. Por cada pregunta y motor, registrar tres columnas: **¿aparece AportaYa?**
   (sí/no/no aplica-Bolivia-fuera de foco), **¿está citada con enlace?** (sí/no),
   **¿la información es correcta?** (sí/no, y si no, qué dijo mal).
3. Volcar la tabla resultante acá, con la fecha de la corrida, reemplazando esta sección.
4. Una respuesta incorrecta es un defecto de **contenido** (falta la sección que la
   responde en primera oración, o no es extraíble) — no un fallo del motor.

**Limitación explícita, sin adornos:** este carril corrió en un agente en un sandbox sin
salida a internet real hacia `chat.openai.com`, `claude.ai`, `perplexity.ai` ni
`gemini.google.com`. **No se ejecutó ninguna consulta real** a los cuatro motores, y no
se inventa ningún resultado. La primera medición real queda **pendiente**, a cargo de
quien tenga acceso a esos servicios (humano, o un agente con navegación real) — el gate
"Primera medición… registrada, con fecha" queda en 🟡, declarado así en vez de marcado en
falso.

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | :-: |
| Determinismo | Espejos `.md`/`llms.txt`/`llms-full.txt` generados, no a mano | diff vacío en dos corridas (arriba, §1) | ✅ |
| Terceros | `llms.txt`/`llms-full.txt` sin rutas `/verificar/`, `/publico/` como enlace | grep negativo (arriba, §2) + `llms.spec.ts` | ✅ |
| FAQ | Prerenderizado, nunca `@defer` | grep de `@defer` en `paginas/` (arriba, §3) | ✅ |
| `robots.txt` | Conforme a ADR-042, ocho bots nombrados | `robots.spec.ts` 3/3 + lectura de `public/robots.txt` (heredado, verificado) | ✅ |
| Espejo `.md` + `alternate` | Cada página indexable tiene `.md` | 10/10 `.md` en `public/` | 🟡 los `.md` sí; el `<link rel="alternate">` del `<head>` **no** — hueco 1 |
| Diez preguntas | Cada una con sección que responde en la primera oración | 3/10 exacto, 3/10 variante cercana, 4/10 sin encabezado — hueco 3 | 🟡 parcial |
| Medición | Primera medición en los 4 motores, con fecha | procedimiento documentado; ejecución real bloqueada por sandbox sin internet (§6) | 🟡 pendiente |
| Entrega | Lint, tipos, pruebas | salida pegada arriba (§4-5) | ✅ |

### Frases prohibidas sin evidencia

Nada de esto se declara "listo" sin más: **9/9 pruebas propias de `geo/` en verde, lint
en verde, typecheck sin errores en `geo/` (5 errores preexistentes fuera de mi alcance),
generador determinista en 2 corridas, grep negativo de terceros sin coincidencias, y 3
huecos declarados en vez de forzados.**

## Bloqueos

- **`ServicioMeta` no emite `<link rel="alternate">` para `alternateMarkdown`** — pedido a
  W1, ver hueco 1. Sin esto, el espejo `.md` existe pero no está enlazado desde el
  `<head>` de la página HTML.
- **Cuatro de las diez preguntas objetivo no tienen encabezado dedicado en `contenido/**`**
  — pedido a quien posea `contenido/` (F9), ver hueco 3.
- **La medición real de F11.6 requiere acceso a internet que este sandbox no tiene** —
  queda para ejecución humana o un agente con navegación real, con el procedimiento ya
  escrito en §6.
- **El entorno se quedó sin espacio en disco (`ENOSPC`) durante este carril**, en el
  directorio temporal del propio agente (no en el repositorio). Se recuperó solo en el
  intento siguiente; no afectó ningún archivo committeado, pero dejo la advertencia por si
  el próximo carril en esta misma máquina lo vuelve a ver.

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[18 Fichas de carril · las 38 unidades de
trabajo]] (ficha `F11`) · [[14 Fases F9 a F11 · Sitio público, SEO y GEO]] ·
[[ADR-042 Política de rastreadores de IA]] · `planes/informes/carril-W1.md`
