---
name: atlas-diseno
description: El sistema visual de ATLAS y sus tres subsistemas reales (tokens CSS del Motor → remapeo Tailwind en los portales; tokens.ts de la app del cliente; capa web de la app). Punto de entrada de cualquier trabajo visual en Atlas — dice dónde vive cada cosa, qué gate la mide y qué NO hay que reinventar. Usar antes de tocar un color, un espaciado, un componente o una pantalla en cualquiera de los seis frontends.
---

# El sistema visual de ATLAS

Antes de nada: **Atlas ya tiene sistema de diseño, y está medido.** El error caro aquí no es
elegir mal un gris, es inventar un token que ya existe o pintar con un valor suelto. Esta skill
dice dónde está cada pieza. Los fundamentos de diseño (jerarquía, composición, color, tipografía)
están en `frontend-ui-design`, `visual-hierarchy-composition`, `color-systems` y
`typography-systems`; el gate de calidad visual, en `ui-quality-review`.

## 1. El stack real, que no es el de ningún otro proyecto

| Frontend | Marco | Estilo |
|---|---|---|
| `AtlasFrontend/apps/consumer-app` (app del cliente) | Expo 57 · React Native 0.86 · expo-router · Reanimated 4 | `StyleSheet` + `src/theme/tokens.ts` |
| La misma app **en el navegador** | react-native-web | + capa `src/web/` (`estilo.ts`, atributos `data-atlas`) |
| `AtlasDecisionEngineFrontend` (Motor) | Next.js 16 · React 19 | CSS propio: `src/styles/parts/*.css` (132 partes) |
| `AtlasERPFrontend`, `AtlasAdminPortal`, `AtlasDashboardsFrontend` | Next.js 15 · React 19 | Tailwind 3 **remapeado** a los tokens del Motor |
| `AtlasLandingPage` | HTML/CSS plano | `style.css` |

No hay Angular, ni Flutter, ni Material, ni styled-components, ni SCSS. Cualquier consejo que
dependa de eso no aplica: tradúcelo o descártalo.

## 2. La fuente de verdad del color: el Motor

`AtlasDecisionEngineFrontend/src/styles/parts/theme.css` define el juego de nombres para claro y
oscuro (`--canvas`, `--surface`, `--surface-sunken`, `--surface-raised`, `--ink`, `--text`,
`--muted`, `--faint`, `--line`, `--line-strong`, `--accent`…). Dos cosas que hay que saber:

- **Los grises son NEUTROS a propósito.** Estuvieron tintados de azul y eso daba dos temperaturas
  peleándose con el acento verde en cada pantalla: la interfaz se leía «coloreada» sin que nadie
  eligiera un color. En una consola el color es SEÑAL; el único color debería ser el que significa
  algo.
- **Hay un gate que lo mide.** `AtlasDecisionEngineFrontend/src/theme/theme-contrast.test.ts` lee
  ese CSS, calcula la luminancia y **falla si algún texto baja de 4,5:1 contra la PEOR superficie
  del sistema**. Aclarar un gris «porque queda mejor» rompe esa garantía y el gate lo canta. Por
  eso `--faint` está en `#6b6b75` y no más claro: mide 4,63:1.

El tema activo se decide por `data-theme` en la raíz, escrito por un script en línea antes del
primer pintado (evita el destello blanco y las discrepancias de hidratación).

## 3. Los portales no tienen tokens propios: remapean Tailwind

`AtlasERPFrontend/tailwind.config.ts` **redefine la rampa `slate`** con los valores del Motor. Es
deliberado y hay que respetarlo: el portal pinta con unas cuatrocientas clases `slate-*` crudas
repartidas por los componentes, y cambiar el aspecto tocando cada una habría sido un barrido de
cuatrocientos sitios con cuatrocientas oportunidades de dejarse uno.

**Consecuencia práctica:** en el ERP, el portal admin y los tableros se escribe `text-slate-700`
normal y sale el gris de ATLAS. **No añadas colores nuevos a la config** para un caso puntual: si
hace falta «otro verde», lo que falta es un token semántico, y ese se decide en el `theme.css` del
Motor, con su test de contraste detrás.

## 4. La app del cliente: `tokens.ts` y `src/ui/`

`AtlasFrontend/apps/consumer-app/src/theme/tokens.ts` exporta todo el lenguaje:
`palette`, `color`, `space`, `radius`, `font`, `type`, `motion`, `easing`, `spring`, `press`,
`stroke`, `inputChrome`, `touch`, `shadow`. **Nada de valores sueltos en una pantalla.**

Roles de color que conviene no confundir (están comentados en el propio archivo):

- `surface.sunken` para un HUECO donde se escribe; `surface.raised` para una tarjeta que se lee
  por encima. Un campo no es una tarjeta.
- `border.subtle` separa filas de una lista. **`border.field` (34 %) es el contorno de un control
  donde se escribe o se elige** — existe porque `subtle` daba 1,3:1 contra la tarjeta y WCAG 2.2
  pide 3:1 para identificar un control (1.4.11). Usar `subtle` en un campo lo vuelve invisible;
  usar `field` en un separador convierte una lista en una reja.
- `text.placeholder` es más apagado que `text.tertiary` a propósito: un ejemplo con la forma exacta
  del valor (`1996-04-12`) hace que el campo se lea como relleno ya escrito.

Las primitivas viven en `src/ui/` y se usan, no se reimplementan: `Screen`, `ScreenHeader`,
`StepHeader`, `AtlasText`, `Button`, `Card`, `CardHeader`, `SectionHeader`, `Field`, `IconField`,
`SelectField`, `PhoneField`, `DateField`, `PinField`, `Badge`, `Chip`, `Skeleton`, `EmptyState`,
`ErrorState`, `ListRow`, `ProgressBar`, `Accordion`, `Stat`…

Movimiento: Reanimated, con los muelles de `spring`/`press` de los tokens. Un componente que se
hunde al tocarlo usa el mismo muelle que todos los demás; inventar una duración es introducir un
temperamento distinto en una pantalla donde el de al lado ya tiene el suyo.

## 5. La misma app en el navegador

La web **no es otra app**: son las mismas 42 pantallas. El aspecto de escritorio se añade por fuera:

- `src/web/estilo.ts` — una hoja generada desde `tokens.ts` (ningún color literal) que se engancha
  a las primitivas por atributos `data-atlas` (`webData()` devuelve `{}` en nativo).
- `src/web/` — atmósfera, barra superior, layouts `.web.tsx` de acceso y registro, tarjeta 3D.
- Cortes en `src/ui/responsive.ts`: `TRAMO.tableta` 600, `panelLateral` 940, `escritorio` 1024;
  `ANCHO_REJILLA` 1220.

**Regla de la casa: cualquier retoque visual de la web va en `estilo.ts`/`src/web/`, nunca en las
pantallas.** Y a 390 px la web tiene que ser IDÉNTICA al teléfono; se prueba a 390, 768 y 1280.

Trampas medidas que no hace falta volver a descubrir: los backticks dentro del literal de plantilla
de `estilo.ts` rompen el TS; `filter: blur()` en la atmósfera mata los fps a 2.560 px (react-native-web
repinta sin parar, a diferencia de un documento quieto); React Navigation pinta cada pantalla sobre
`rgb(242,242,242)` con estilo EN LÍNEA y hay que anularlo.

## 6. Las reglas de Atlas que no son de teoría de diseño

Salen de decisiones que ya se tomaron aquí, con su motivo:

- **En el ERP se escribe en lenguaje de usuario.** Nada de «backend», nombres de endpoint ni cajas
  tipo «se asigna al guardar»; las validaciones se traducen.
- **Una sola ayuda por cabecera** («¿Qué es esto?», «Más», y la acción). El ⓘ vive junto al rótulo
  de cada campo y de cada opción de un select, y hay un guardián (`check:field-help` en la app) que
  falla si un campo se queda sin «qué poner».
- **Tabla primero, crear arriba, operar en la fila.** Una pestaña es un sustantivo.
- **El carril derecho se reparte**: la consulta va tras un ⓘ y los totales en tarjetas junto a la
  acción.
- **Portal admin: 300 líneas por archivo.** No es una sugerencia: `npm run max-lines`
  (`scripts/check-max-lines.mjs 300`) entra en `npm run validate`.
- En TEST los portales van por **HTTP plano**: `crypto.randomUUID` y `crypto.subtle` no existen ahí.
  Usar `newUuid()` de `lib/uuid.ts` y `sha256Hex()` de `lib/sha256.ts`.

## 7. Antes de dar una pantalla por terminada

1. Capturas reales por ancho y tema (`visual-proof`). Una revisión visual sin capturas no vale.
2. Rúbrica y catálogo anti-slop (`ui-quality-review`).
3. En la app: `npx tsc --noEmit`, `eslint .`, `npm test` (incluye `check:field-help`) y
   `npx expo export --platform web` — **en un worktree, nunca en el árbol compartido**, porque el
   export escribe `dist/`.
4. En la web: `e2e-web/responsive.mjs` (desborde horizontal, objetivos < 24 px, texto < 11 px,
   errores de página) y `e2e-web/humo.mjs` (las 42 rutas).
5. En el Motor: el test de contraste corre solo si tocaste `theme.css`.

## Checklist

- [ ] Usé los tokens que ya existen; no inventé ninguno ni pinté con un valor suelto.
- [ ] Si toqué color, sé contra qué superficie se mide y pasa 4,5:1 (texto) / 3:1 (control).
- [ ] En la app: nada de estilos en la pantalla que debieran ser primitiva o token.
- [ ] En la web: el cambio está en `src/web/`, y a 390 px sigue siendo el teléfono.
- [ ] Pasé `ui-quality-review` con capturas reales, no describiendo la pantalla.
