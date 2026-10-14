---
name: atlas-estilo-portales
description: Cómo se escribe el estilo en los portales Next.js de Atlas — Tailwind remapeado a los tokens del Motor en ERP, portal admin y tableros; las 132 partes CSS del Motor y cómo se añade una; especificidad, tema claro/oscuro y el gate de contraste. Usar al tocar el aspecto de cualquier pantalla de un portal, al añadir un color o una clase, o cuando un estilo «no se deja sobrescribir».
---

# Estilo en los portales de Atlas

Sustituye a la guía genérica de arquitectura CSS, que estaba escrita para **CSS sin framework en
Angular** (`:host`, `::ng-deep`, `@layer`, y explícitamente «sin Tailwind»). En Atlas es justo al
revés: tres de los cuatro portales van con **Tailwind 3 sobre Next.js**, y el cuarto —el Motor—
tiene su propio CSS por partes. Antes de nada, leer `atlas-diseno`: la fuente de verdad del color no
está en el portal que estés tocando.

## 1. Los dos modelos que conviven

| Portal | Marco | Cómo se pinta |
|---|---|---|
| Motor (`AtlasDecisionEngineFrontend`) | Next 16 | CSS propio: `src/styles/parts/*.css`, 132 partes importadas por `global.css` |
| ERP, portal admin, tableros | Next 15 | Tailwind 3, con la rampa `slate` **redefinida** con los tokens del Motor |

## 2. En ERP / admin / tableros: se escribe Tailwind normal

`tailwind.config.ts` **redefine `slate`** (y `background`, `surface`, `surface-muted`…) con los
valores de `theme.css` del Motor. Es deliberado: el portal pinta con unas cuatrocientas clases
`slate-*` crudas repartidas por los componentes, y cambiar el aspecto tocando cada una habría sido un
barrido de cuatrocientos sitios con cuatrocientas oportunidades de dejarse uno.

**Qué significa en la práctica:**

- Escribe `text-slate-700`, `bg-slate-50`, `border-slate-200` como siempre: ya salen en el gris de
  ATLAS, y lo que se escriba mañana también.
- **No añadas un color nuevo a la config** para un caso puntual. Si hace falta «otro verde», lo que
  falta es un token semántico, y ese se decide en `theme.css` del Motor —que tiene un test de
  contraste detrás—, no en un `extend` del portal.
- Aclarar un peldaño de `slate` en la config rompe en silencio la garantía de contraste del Motor.
  `slate-500` está en `#6b6b75` porque mide 4,63:1 en la peor superficie; no es estética.

## 3. En el Motor: una parte por asunto

`src/styles/global.css` importa 132 archivos de `parts/`, y el orden importa: primero `theme.css`
(los tokens), luego `foundation.css`, `typography.css`, `app-shell.css`, y después las partes por
función (`tabs.css`, `data-display.css`, `graph-canvas.css`, `workers-*.css`…).

Para añadir estilo nuevo: **una parte nueva con nombre de asunto**, importada en su sitio del orden,
en vez de engordar una existente o colar reglas sueltas en `global.css`. Nunca vuelvas a decidir «qué
gris es el fondo»: pide `var(--surface)` y el tema responde.

## 4. Tema claro y oscuro

- El tema activo lo decide `data-theme` en la raíz, escrito por **un script en línea antes del
  primer pintado** (`layout.next.tsx`). Así no hay destello blanco al recargar en oscuro, y como no
  forma parte del HTML que renderiza React tampoco provoca discrepancias de hidratación.
- El oscuro **no es el claro invertido**: tiene sus propias partes (`theme-dark-base.css`,
  `theme-dark-surfaces.css`, `theme-dark-features.css`). Sobre fondo oscuro una sombra negra no se
  ve: ahí la profundidad se hace con un borde más claro o con una superficie un tono por encima.
- Hay `forced-colors.css`: el modo de alto contraste del sistema también es un modo real.

## 5. Especificidad, sin `!important` como respuesta

- Selector plano y una clase por asunto. Si algo «no se deja sobrescribir», casi siempre es que dos
  reglas compiten por prefijos distintos, no que falte un `!important`.
- En la capa web de la app del cliente esto está medido: `[a][b]:not([c])>div>*` gana a
  `[a]>div>:nth-child()`, y hay que **igualar prefijos** para que la más específica sea la que quieres.
  Ahí sí hay `!important` a propósito y documentado, porque react-native-web pinta estilos EN LÍNEA
  que no se pueden alcanzar de otra forma.

## 6. Reglas de casa de los portales

- **Lenguaje de usuario en el ERP**: nada de «backend», nombres de endpoint ni cajas «se asigna al
  guardar»; las validaciones se traducen.
- **Una sola ayuda por cabecera**: «¿Qué es esto?», «Más» y la acción. El ⓘ va junto al rótulo del
  campo o de la opción, no en los filtros.
- **Tabla primero, crear arriba, operar en la fila.** Una pestaña es un sustantivo.
- **El carril derecho se reparte**: la consulta tras un ⓘ, los totales en tarjetas junto a la acción.
- **Portal admin: 300 líneas por archivo**, comprobadas por `npm run max-lines` dentro de
  `npm run validate`.
- **En TEST los portales van por HTTP plano**, que no es contexto seguro: `crypto.randomUUID` y
  `crypto.subtle` no existen. Usar `newUuid()` de `lib/uuid.ts` y `sha256Hex()` de `lib/sha256.ts`.
- Cuidado con `flex-1` en barras de filtros: base 0 se come todo el déficit y la búsqueda desaparece.
  El suelo va en `min-width`, y `overflow-hidden` esconde el síntoma en vez de arreglarlo.

## 7. Verificación

- Si tocaste `theme.css`: `theme-contrast.test.ts` (falla por debajo de 4,5:1 contra la peor
  superficie).
- Lint del repo (`max-lines` en el admin, `check:migration-lists` en el backend del ERP).
- Capturas por ancho **y por tema** (`visual-proof`), y rúbrica (`ui-quality-review`).
- Los builds (`next build`) escriben `.next`: **en un worktree**, nunca en el árbol compartido, o
  tumbas el `next dev` de otra sesión con chunks 404.

## Checklist

- [ ] Usé las clases/tokens que ya existen; no añadí un color a la config del portal.
- [ ] Si toqué el color base, pasé el test de contraste del Motor.
- [ ] En el Motor, el estilo nuevo está en su parte, no engordando `global.css`.
- [ ] Lo miré en claro **y** en oscuro.
- [ ] Ningún `!important` sin un comentario que diga contra qué pelea.
