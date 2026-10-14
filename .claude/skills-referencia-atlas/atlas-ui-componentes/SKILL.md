---
name: atlas-ui-componentes
description: Cómo se organizan y se reutilizan los componentes en Atlas — las primitivas de `src/ui` en la app del cliente (React Native), `components/ui` en los portales (Next.js), qué va en una pantalla y qué en una primitiva, y descubrir antes de crear. Usar antes de escribir cualquier componente nuevo en cualquiera de los seis frontends, al revisar un PR que agrega uno, o cuando dos componentes hacen casi lo mismo.
---

# Componentes en Atlas

Sustituye a la guía genérica de «atomic design» y a la de contenedor/presentacional: aquellas están
escritas para Angular (`input()`, `output()`, content projection, standalone/OnPush) y aquí no hay
Angular. Las ideas de fondo —reusar antes de crear, separar lo que trae datos de lo que pinta— sí
valen; lo que cambia es cómo se escriben.

## 1. Primero: buscar. Casi siempre ya existe

Antes de crear nada, mirar el inventario. La app del cliente tiene **más de cuarenta primitivas**
en `AtlasFrontend/apps/consumer-app/src/ui/`:

- Estructura: `Screen`, `ScreenHeader`, `StepHeader`, `SectionHeader`, `Gap`, `Divider`.
- Texto y estado: `AtlasText` (con sus `variant`), `Overline`, `Badge`, `Chip`, `ChipBar`,
  `ProgressBar`, `Skeleton`, `EmptyState`, `ErrorState`, `Stat`, `StatRow`, `KeyValue`.
- Acción y superficie: `Button`, `Card`, `CardHeader`, `IconChip`, `ListRow`, `Accordion`,
  `BrandPanel`, `Avatar`.
- Formulario: `Field`, `IconField`, `AmountField`, `DateField`, `SelectField`, `PhoneField`,
  `PinField`, `OptionGroup`, `CheckRow`, `Switch`, `ConsentRow`.

En los portales Next.js, `AtlasERPFrontend/components/`: `ui/` (`DataTable`, `PageTitle`,
`ScreenState`, `PageSkeleton`, `ConfirmDialog`, `LoadingIndicator`, `BackendGap`, `button`, `card`,
`badge`) y, al lado, `layout/`, `screens/`, `atlas/`, `tutorial/`.

**Si algo «casi» sirve, se parametriza; no se copia.** Copiar una pantalla y tocarle dos estilos es
cómo Atlas acabó con veinte cabeceras distintas, y con tres pantallas sin botón de volver porque se
habían escrito su propio encabezado a mano.

## 2. Qué va en una primitiva y qué en la pantalla

- **La primitiva decide cómo se ve y cómo se comporta un control.** Ahí viven el color, el
  espaciado, los estados (`hover`, `focus`, `disabled`, `loading`, `error`) y el movimiento.
- **La pantalla decide qué datos entran y qué pasa al pulsar.** Una pantalla que declara
  `borderColor` o `fontSize` es una primitiva que falta. Media app llegó a escribir
  `{ borderColor: color.feedback.danger, borderWidth: 1 }` en su propia hoja para pintar el aviso de
  mora, cada una con una opacidad distinta: un aviso que se ve de un color en Inicio y de otro en
  Pagos deja de leerse como el MISMO aviso.
- **Traer datos y pintar son dos trabajos.** En el ERP, `screens/` orquesta (consulta, estado,
  efectos) y `ui/` pinta con lo que recibe. Un componente que no se puede abrir en otra pantalla ni
  probar sin levantar media app está haciendo los dos.

## 3. El contrato de una primitiva

- **Entradas semánticas, no de estilo.** `tone="danger"`, `variant="ghost"`, `padding="tight"`:
  nada de `color` ni `marginTop` como prop. Si hiciera falta, lo que falta es una variante.
- **Los estados no son un anexo.** Toda primitiva interactiva de Atlas trae `loading`, `disabled` y
  el motivo de estar bloqueada; toda vista que trae datos trae vacío, cargando, error y sin permiso
  (`frontend-ux-states`).
- **Cambiar la firma de una primitiva compartida es un barrido.** Pasó con `Select` → `options`:
  dejó 21 selects rotos con `tsc` casi verde. Al tocar una firma, buscar TODOS los sitios de llamada
  y verificar lo retocado en un worktree.
- En el portal admin hay un tope de 300 líneas por archivo (`npm run max-lines`, dentro de
  `npm run validate`): un componente que crece se parte, no se silencia.

## 4. Accesibilidad, que aquí es parte del contrato

- Toda superficie tocable lleva `accessibilityRole` y `accessibilityLabel`. Las cuatro casillas del
  PIN estuvieron mudas y el resultado fue que ni VoiceOver ni las pruebas encontraban dónde escribir:
  había que tocarlo por coordenada.
- Objetivo táctil: `touch.minSize` es 48 (el mayor de iOS 44 y Material 48, para no tener dos
  criterios). En controles pequeños pegados a un texto se usa `hitSlop` — y en el navegador
  `hitSlop` NO existe en react-native-web, así que va `toqueWeb()`/`data-toque` (ver `hit-slop.ts`).
- Cada campo y cada opción de un select llevan su ⓘ con «qué poner». Hay un guardián,
  `npm run check:field-help`, que falla si falta uno.

## 5. Un componente nuevo, paso a paso

1. Buscar si existe (`grep` en `src/ui/` o `components/`); si «casi» existe, parametrizar.
2. Escribirlo con tokens (`atlas-diseno`), nunca con valores sueltos.
3. Definir sus estados antes que su estado feliz.
4. Nombre y rol accesibles, y área táctil.
5. Si trae datos, partirlo: quien consulta arriba, quien pinta abajo.
6. Capturas por ancho y tema (`visual-proof`) y rúbrica (`ui-quality-review`).

## Checklist

- [ ] Busqué antes de crear; no hay otro que haga casi lo mismo.
- [ ] Sus props son semánticas; no acepta colores ni márgenes desde fuera.
- [ ] Tiene vacío, cargando, error y deshabilitado —con motivo—, no sólo el caso feliz.
- [ ] Nombre y rol accesibles, y área táctil de 48 (o `hitSlop`/`data-toque` si es pequeño).
- [ ] Si cambié una firma compartida, revisé todos los sitios de llamada.
