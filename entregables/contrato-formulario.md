# Contrato de formulario y borrador dentro del diálogo

## 1. Quién es el dueño

**El contenedor** (el componente de pantalla que abre el diálogo — `FichaDeCobro`,
`PanelDeAprobacionDeCampana`, y todo consumidor futuro) es el dueño único del borrador. No hay
un segundo borrador ni en `Dialogo` ni en ningún organismo de campo: `Dialogo` no tiene estado de
formulario, `Campo`/`CampoMonto`/`GrupoRadio` son controlados por `model()` (dos vías) sin estado
propio persistente entre aperturas — el contenedor los posee con sus propias señales (`monto`,
`forma`, `motivo`, `decision`).

Esto **ya era así** antes de este carril (arquitectura confirmada leyendo el código, no asumida):
este repo no usa Reactive Forms (`FormGroup`) — usa señales (`model()`) por campo, dueñas del
contenedor. El contrato no cambia esa arquitectura; la hace explícita y le agrega la única pieza
que faltaba: **la política de descarte** (`entregables/contrato-dialogo.md` §3).

No confundir con `apps/backoffice/src/app/nucleo/borrador.ts` (`ServicioBorrador`, propiedad de
Richard — no se toca en este carril): esa es persistencia en `IndexedDB` para formularios **largos**
(ROS, actas) que sobreviven un refresco de página. El borrador de este contrato es el estado en
memoria mientras el diálogo está abierto, protegido contra el cierre accidental — un problema
distinto, no una duplicación.

## 2. Qué se preserva

| Caso | Cómo se prueba (`ficha-de-cobro.spec.ts`, `panel-de-aprobacion.spec.ts`) |
|---|---|
| Valor inicial | el diálogo abre con `monto=''`, `forma='TRANSFERENCIA'` — estado limpio conocido |
| Campo tocado / modificado | `sucio()` pasa a `true` en cuanto el valor difiere del inicial |
| Validación | `errorDeMonto()` / `motivoVacio()` ya existían; no se tocó su lógica de negocio (fuera del alcance del diálogo, regla del encargo) |
| Envío | `confirmar()` / `acciones.aprobar/rechazar()`, sin cambios de este carril |
| Bloqueo del doble envío | ya existía: `enviando()`/`cargando` deshabilita el botón de confirmar vía `Boton` |
| Error remoto conserva el borrador | `confirmar()` en `FichaDeCobro`: el `catch`/`error:` de la suscripción solo apaga `enviando`, nunca limpia `monto`/`forma` — el dato sigue en pantalla. Confirmado leyendo el código, no se necesitó cambiarlo: ya estaba bien |
| Cancelar / reapertura | `abrir()` de `PanelDeAprobacionDeCampana` resetea `motivo`/`intentoConfirmar` **al abrir**, no al cerrar — así una reapertura siempre arranca limpia, y un cierre bloqueado por la política de descarte no pierde nada porque no llegó a cerrarse |

## 3. Q-L1 — la entidad cambia mientras se edita

**No aplica a los dos modales migrados**: ninguno de los dos (`FichaDeCobro`, cobro sobre una
cuenta ya cargada por el padre; `PanelDeAprobacionDeCampana`, aprobar/rechazar por id de ruta) hace
polling ni recibe la entidad por un recurso que se revalide mientras el diálogo está abierto — la
entidad llega una vez, por `input()`, y no cambia bajo el diálogo abierto en el flujo actual.

Se registra la política igual, para el próximo consumidor que sí tenga esa situación (rama
conservadora, según el supuesto ya tomado en el encargo): si la entidad se recarga mientras el
diálogo está abierto y el borrador está sucio, **no se sobrescribe el borrador en silencio** — se
mantiene lo que el usuario escribió hasta que decida, y el envío final valida contra el identificador
de la entidad original, no contra la nueva versión (para no guardar contra la entidad equivocada).
Esto queda **sin caso de prueba** en este carril porque no hay una pantalla real que lo ejercite
todavía — sección "No cubierto" de `entregables/PR8-carril.md`.
