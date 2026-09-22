# Contrato de formulario y borrador (H4.S1.M1)

> **Peldaño:** `DISCOVERED`. Fuente: los dos modales reales elegidos como candidatos H3.S2.M1 —
> `apps/backoffice/src/app/rutas/contabilidad/cobros/ficha-de-cobro.ts` y
> `apps/backoffice/src/app/rutas/contabilidad/compras/ficha-de-factura.ts` — leídos completos.

## 1. Dueño del borrador — declarado

**El contenedor (`FichaDeCobro` / `FichaDeFactura`) es el único dueño del borrador.** No hay dos
borradores autoritativos hoy: los `signal()` locales (`monto`, `forma`) viven exclusivamente en el
componente que envuelve `<ap-dialogo>`, y el `Dialogo` en sí **no tiene ningún estado de
formulario** — solo `abierto` (apertura) y `cargando` (para el spinner del botón de confirmar). El
diálogo es "tonto" respecto del formulario: proyecta el contenido, no lo conoce. Esto ya cumple la
regla del encargo ("nunca las dos cosas a la vez") **tal como está escrito hoy**, sin necesitar un
cambio de arquitectura — el trabajo de H4 no es *resolver* una duplicación de borrador (no la hay),
es *demostrar* y *blindar* que se mantiene así al migrar al diálogo con política de descarte.

## 2. Qué ya está resuelto en el código real (evidencia, no propuesta)

| Requisito | Estado real | Evidencia |
|---|---|---|
| Valores iniciales | `monto = signal('')`, `forma = signal('TRANSFERENCIA')` | `ficha-de-cobro.ts:39-40` |
| Validación | `errorDeMonto` computed, bloquea el excedente contra el saldo (regla de dominio `cobroExcedeElSaldo`, `AP-CU104-02` del backend queda de respaldo — comentario línea 12) | `ficha-de-cobro.ts:47-49` |
| Bloqueo de doble envío | `enviando` signal, gate en `confirmar()`: no hay guardia explícita contra reentrancia mientras `enviando()` es true en el propio método (**brecha real, ver §4**) | `ficha-de-cobro.ts:51,60-68` |
| Error remoto conserva el borrador | `error: () => this.enviando.set(false)` — no limpia `monto` ni `forma`, y no cierra el diálogo | `ficha-de-cobro.ts:67` |
| Cancelar | `cambiarApertura(false)` → emite `cerrada` | `ficha-de-cobro.ts:56-59` |

## 3. Campo tocado / modificado — no está resuelto

`CampoMonto` y `GrupoRadio` (moléculas de `packages/ui`) no fueron leídos en detalle en esta sesión
(fuera del foco de lectura priorizado); no se afirma si exponen estado "tocado" (`touched`) o
"modificado" (`dirty`) por campo. **Esto es exactamente lo que la política de descarte del diálogo
necesita como predicado** (`hay cambios sin guardar`): hoy el candidato más simple y honesto, sin
inventar un campo `dirty` que no existe, es comparar `monto() !== '' || forma() !== valorInicial`
contra los valores iniciales capturados al abrir. Se registra como decisión técnica mínima
(regla 00 §1.5), no como hallazgo de un campo ya existente.

## 4. Brecha real: doble envío

`confirmar()` (línea 71-85 de `ficha-de-cobro.ts`) no verifica `if (this.enviando()) return` antes
de disparar la petición: si alguien pulsa el botón dos veces rápido antes de que Angular repinte el
`[cargando]="enviando()"` del botón (que sí debería deshabilitarlo — no se leyó `Boton` para
confirmar si `cargando` deshabilita el `click` nativo), hay una ventana de doble envío no cerrada
por este archivo. **No verificado en runtime** (regla 95.3.4 exige bloqueo — no confirmado que
`ap-boton` lo dé gratis; queda como hallazgo para el equipo, fuera de reescribir `Boton`, que no
está en el alcance IN de este carril).

## 5. Política para entidad que cambia durante la edición (Q-L1)

**No definida por producto.** Se aplica la rama conservadora exigida por el encargo: si la entidad
(`cuenta: CuentaPorCobrar` / `factura: FacturaDeProveedor`, ambas `input.required`) cambia mientras
el diálogo está abierto con un borrador sucio, **no se pierde el borrador en silencio** y **no se
guarda contra la entidad vieja** — la implementación concreta de esa rama (por ejemplo, recongelar
el `input` al abrir con un snapshot local, y comparar el `input` corriente contra ese snapshot para
decidir si avisar) no se escribió en esta sesión (ver bloqueo de entorno). Se registra como
`A MEDIAS`: el supuesto está tomado y escrito, el código que lo aplica no.

## 6. No cubierto

- No se leyeron `CampoMonto`, `GrupoRadio`, `Boton` en profundidad (fuera del foco priorizado de
  esta sesión) — el comportamiento de "tocado"/"modificado" y de deshabilitado por `cargando` en
  `ap-boton` queda sin confirmar.
- Ningún test de `formulario`/`error de guardado` corrido (bloqueo de entorno).
- No se verificó si algún modal existente evita a propósito la protección de descarte por una razón
  de producto (Q-L3): con la lectura de código disponible, ninguno de los dos candidatos tiene hoy
  ninguna protección — no hay dato para sospechar que sea intencional, así que ante la duda se
  protegerá cuando se implemente (regla del encargo, Q-L3).
