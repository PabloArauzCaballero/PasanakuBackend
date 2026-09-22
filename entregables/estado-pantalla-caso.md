# H2 — Inventario de estado mutable de `pantalla-de-caso.ts`

| Estado | Quién lo crea | Quién puede cambiarlo | Quién lo lee | Qué lo invalida | Cuándo se destruye |
|---|---|---|---|---|---|
| `causal` (`signal<string\|null>`) | `PantallaDeCaso` (contenedor), arranca `null` | `alEscribirCausal()`, disparado por el `change` de `ap-grupo-radio` en la presentación | `puedeConfirmar` (computed), la presentación (para marcar la opción elegida) | Nada lo invalida solo: vive mientras el componente está montado | Al destruirse el componente (no se persiste — a propósito, elegir la causal no es el borrador largo) |
| `narrativa` (`signal<string>`) | `PantallaDeCaso`, arranca `''` o el borrador recuperado en `ngOnInit` | `alEscribirNarrativa()`, disparado por el `input` del `textarea` en la presentación | La presentación (valor del `textarea`) | Nada la invalida sola | Al confirmar con éxito (`ServicioBorrador.borrar`) o al destruirse el componente sin confirmar (el borrador queda en `IndexedDB`, no en este signal) |
| `borradorRecuperado` (`signal<boolean>`) | `PantallaDeCaso`, arranca `false` | Solo `ngOnInit`, una vez, si `ServicioBorrador.leer()` devuelve algo | La presentación (aviso "se recuperó un borrador") | No se puede volver a `false` en la vida del componente (correcto: es un aviso de una sola vez) | Al destruirse el componente |
| `casoId`/`caso` (inputs) | El router (`casoId`) / quien monte la pantalla (`caso`, con default de demo) | Nadie dentro del componente — son `input()` | `claveBorrador()`, `progreso` | Cambia si el router navega a otro `casoId` | Al destruirse el componente |

## Hecho representado dos veces (sin derivar) — encontrado

**`puedeConfirmar(causal())`** se llamaba **dos veces** en el propio template original
(línea 31, para mostrar "sin causal"; línea 39, para deshabilitar el botón), evaluando la
misma regla de negocio dos veces por cada `causal()` que cambia, en vez de derivarla una
sola vez. Corregido: `protected readonly puedeConfirmarComputado = computed(() =>
puedeConfirmar(this.causal()))` en el contenedor, pasado como **un solo** `input` a la
presentación (`FormularioDeCaso.puedeConfirmar`), leído dos veces en SU template (leer un
valor ya derivado dos veces no es el problema — evaluarlo dos veces sí lo era).

## `effect` que copia estado derivable

Ninguno: el componente original no usaba `effect()`. El único candidato a "copia" era
`progreso = () => etapasDelCaso(this.caso())` — ya era una función derivada por llamada
(no un signal con estado propio), así que no había nada que copiar; se convirtió a
`computed()` igual, para que la presentación reciba un valor memoizado en vez de
recalcularlo en cada `detectChanges`.
