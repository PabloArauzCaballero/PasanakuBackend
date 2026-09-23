# H1.S2.M1 — Elección de la pantalla piloto

## Candidatas relevadas (backoffice, fuera de lo reservado a otros carriles)

Excluidas por alcance (PR6 §3 OUT): toda pantalla que consume el organismo tabla —
`bandeja-de-reclamos.ts`, `cola-de-solicitudes-escaladas.ts`, `pantalla-de-estado.ts`
(grep `nucleo/tabla` → 3 archivos, son de Justin/PR7) — y `pantalla-de-expedientes.ts` /
`tira-de-fotos.ts`, que además traen un hallazgo de tipos preexistente ajeno a este carril
(`entregables/decision-doble-clientes-angular.md` del carril hermano PR11, mismo repo,
mismos errores: `TS2538`/`TS2749` en `ExpedienteEnRevisionFotosEnum`).

| Pantalla | LOC | Estado mutable propio | Regla repetida en el propio archivo | Dependencia de negocio inyectada en el componente que también dibuja |
|---|---|---|---|---|
| `operacion/billetera/pantalla-de-billetera.ts` | 56 | 0 (solo `input` + `httpResource`) | — | ninguna (ya separado: usa `EstadoDePantalla`/`Monto`) |
| `cumplimiento/alertas/pantalla-de-alertas.ts` | 45 | bajo | — | — |
| `cumplimiento/reclamos/pantalla-de-reclamo.ts` | 60 | bajo | — | — |
| `cumplimiento/actas/pantalla-de-acta.ts` | 71 | medio | — | — |
| `cumplimiento/organizadores/pantalla-de-habilitacion.ts` | 71 | medio | — | — |
| **`cumplimiento/casos/pantalla-de-caso.ts`** | **92** | **3 signals (`causal`, `narrativa`, `borradorRecuperado`) + persistencia de borrador** | **`puedeConfirmar(causal())` se llama 2 veces en el mismo template (línea 31 y 39) — la misma regla de negocio evaluada dos veces por el motor de plantillas, en vez de derivada una sola vez** | **sí: `ServicioBorrador` (persistencia) inyectado directo en el componente que arma el template y maneja el DOM (`textarea`)** |

## Criterio de elección (regla 20 — CA binario, no gusto)

`pantalla-de-caso.ts` es la única candidata, de las que no están reservadas a otro carril,
que muestra **las dos condiciones** que el kill-test de `PR6` busca:

1. **Una dependencia de negocio con estado propio** (`ServicioBorrador`, persistencia con
   `leer`/`guardar`/`borrar`) **inyectada en el mismo componente que resuelve el template y
   maneja el DOM** (el `textarea`, `$event.target`) — exactamente lo que el §3 del carril
   llama a separar en contenedor vs. presentación.
2. **Una regla de negocio repetida sin derivar**: `puedeConfirmar(causal())` se evalúa dos
   veces de forma independiente en el propio template (deshabilitar el botón y mostrar la
   ayuda), en vez de una señal `computed` única de la que ambos lean.

Es además la pantalla con más estado mutable propio (3 signals) de las candidatas sin tabla
ni el hallazgo de tipos de `tira-de-fotos.ts`.

## No es de otro carril

- No usa el organismo tabla (`grep -n "nucleo/tabla" pantalla-de-caso.ts` → vacío).
- No es uno de los dos consumidores que migra Justin (`bandeja-de-reclamos.ts`,
  `cola-de-solicitudes-escaladas.ts` — confirmado por `grep`, no por memoria).
- Usa `ServicioBorrador` (el contrato de borrador es de Leo, PR8) **como consumidor**, no
  como dueño: este carril no toca `ServicioBorrador` ni su contrato, solo reubica DÓNDE se
  lo inyecta dentro de `pantalla-de-caso.ts` (al contenedor, no a la presentación). Si Leo
  cambia el contrato antes de que este carril cierre H3, se adopta (`Q-R2`); si no, queda
  declarado.

## Publicación en el daily de equipo (AMB-F7)

**No se publicó en un daily de equipo real**: esta sesión no tiene un equipo activo
publicando en paralelo (no hay un Justin ni un Leo ejecutando su propio carril al mismo
tiempo); es una simulación de un solo carril. Se registra como ambigüedad resuelta por
necesidad: la reserva de archivos se documenta acá, en el propio carril, en vez de en el
daily compartido. Quien retome este carril en un contexto de equipo real sí debe publicarlo
en la primera hora, como pide el plan.
