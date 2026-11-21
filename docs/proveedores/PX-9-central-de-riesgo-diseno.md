---
titulo: "PX9.M02 — Consentimiento y contenido del reporte a la central de riesgo crediticio (solo diseño)"
estado: diseño para revisión de Pablo — CONDICIONADO a la respuesta legal L-04
fecha: 2026-11-21
origen: "PLAN-PROVEEDORES.md §PX-9 (RF-PX9-01 a RF-PX9-04); PREGUNTAS-LEGALES.md L-04; DECISIONES.md tandas 5 y 6"
tags: [proveedores, central-de-riesgo, consentimiento, transparencia, diseno]
---

# PX9.M02 — Reporte a la central de riesgo crediticio: consentimiento y contenido

> [!warning] Condicionado a L-04
> **Nada de este diseño se construye ni se activa hasta que el abogado responda L-04** («¿Puede la empresa
> reportar impagos a una central de riesgo crediticio, con el consentimiento firmado al entrar? ¿Con qué
> requisitos?»). Si la respuesta es «no», este documento se descarta. Si es «sí, con requisitos», los
> requisitos de la respuesta mandan sobre este diseño y se ajusta antes de escribir código.
>
> Tampoco se contrata la central (PX-9) ni se escribe su RFP antes de L-04.

> [!note] No confundir con el «reporte a la central de riesgo operativo»
> `docs/Cumplimiento.md` §1.4 menciona `reportado_central_riesgo_operativo` en `evento_riesgo_operativo`. Eso es
> el reporte de **eventos de riesgo operativo de la empresa** al supervisor. Este documento trata del
> **historial de pago de personas** ante una central de riesgo **crediticio**. Son cosas distintas.

## 1. Para qué

Es la «capa 3» de «Poder de compra» (F5) y parte del costo de irse sin pagar (PREGUNTAS-LEGALES L-04): que el
buen historial de pago en Pasanaku le sirva a la persona fuera de la app, y que una deuda firme con el fondo del
grupo no desaparezca al borrar la app. Lo reporta el servicio **`transparencia`**, que ya es dueño de los
hechos de puntualidad (pieza D, tanda 5).

## 2. Requisitos que este diseño cubre

| ID | Requisito (PLAN-PROVEEDORES §PX-9) | Dónde se cubre |
|---|---|---|
| RF-PX9-01 | Solo con un consentimiento **expreso, separado, versionado y revocable**; sin consentimiento no se comparte nada | §3 |
| RF-PX9-02 | Se reportan **solo hechos de pago** (pagó a tiempo, pagó tarde, deuda firme con el fondo) y **nunca** antes de que el expediente quede `FIRME` (regla 91.4) | §4 |
| RF-PX9-03 | **Derecho de rectificación:** un reclamo abierto suspende el reporte | §5 |
| RF-PX9-04 | **Bitácora** de qué se reportó, cuándo y con qué consentimiento | §6 |

## 3. Consentimiento (RF-PX9-01)

### 3.1 Propiedades

| Propiedad | Cómo se cumple |
|---|---|
| **Expreso** | Una acción afirmativa propia (casilla sin marcar por defecto + botón «Acepto que se reporte mi historial»). Nunca por silencio, por seguir usando la app ni por casilla premarcada |
| **Separado** | Pantalla y registro propios. No va dentro de los términos, de la política de privacidad ni del reglamento del grupo. Aceptar el reglamento no implica aceptar esto |
| **Versionado** | Se guarda la versión exacta del texto y su hash. Un texto nuevo exige un consentimiento nuevo; el anterior no se «arrastra» |
| **Revocable** | Desde Ajustes → Privacidad, en cualquier momento y con el mismo esfuerzo con que se otorgó. La revocación corta los reportes **futuros** desde ese momento |

### 3.2 Dónde se registra (sin catálogo paralelo)

Se **reutiliza** la tabla existente `identidad.consentimiento` (M1: `usuario_id`, `tipo`, `version_documento`,
`hash_documento`, `otorgado`, `fecha_hora`, `ip_origen`, `agente_usuario`, `revocado_en`). Hoy su `CHECK` admite
`MARKETING`, `PRIVACIDAD`, `REGLAMENTO_GRUPO`, `TERMINOS` y `TRATAMIENTO_DATOS`
(`sql/10_tablas/01_identidad_usuarios/consentimiento.sql:17`). El diseño necesita un tipo nuevo,
**`REPORTE_CENTRAL_RIESGO`**.

- Ese cambio **no se hace en este carril**: es una microtarea futura sobre la fuente
  (`docs/entidades/01_identidad_usuarios.puml` y `docs/Restricciones.md`) y se regenera `sql/` con
  `scripts/generar_ddl.py` (regla 97.1). Queda anotada en §9.
- `identidad` es dueño del consentimiento. `transparencia` no lee su base: recibe los eventos
  `consentimiento.otorgado` y `consentimiento.revocado` (con `usuario_id`, `tipo`, `version_documento` y
  `fecha_hora`) por outbox, y mantiene su propia proyección de «quién consintió qué versión» (regla 98.1.1).
- Antes de cada envío, `transparencia` comprueba en su proyección que el consentimiento siga vigente **a la
  fecha del hecho y a la fecha del envío**. Si la proyección está atrasada respecto del dueño más allá del
  desfase tolerable, el envío espera (regla 98.4.2: no se decide con una réplica vieja).

### 3.3 ¿Es obligatorio para entrar a un grupo?

**`DECISION_REQUIRED` de Pablo, sujeto a L-04.** RF-PX9-01 dice «sin consentimiento no se comparte nada», lo que
supone que se puede no consentir. L-04 habla de «consentimiento firmado al entrar», lo que admite leerlo como
condición de ingreso. Son dos diseños distintos:

| Opción | Efecto | Riesgo |
|---|---|---|
| (a) Opcional | Se ofrece al entrar y en Ajustes; quien no consiente usa la app igual y no se reporta nada de él | El incentivo de «costo de irse sin pagar» solo alcanza a quien consintió |
| (b) Requisito de ingreso | Sin consentimiento no se entra a un grupo | Puede no ser un consentimiento libre; lo tiene que validar el abogado |

Este diseño **no elige**. Las pantallas de §7 están escritas para (a); con (b) cambia solo el paso 3 del ingreso.

### 3.4 Qué pasa al revocar

- Desde la revocación no se envía ningún hecho nuevo.
- **Lo ya reportado**: si la revocación obliga a pedir su retiro a la central, o si la deuda firme puede seguir
  reportada, lo responde L-04 (sub-pregunta agregada en §9). Hasta tenerla, el diseño no promete ninguna de las
  dos cosas en el texto al usuario.

### 3.5 Texto del consentimiento (borrador, P-16: lo aprueba Pablo; lo revisa el abogado)

> **¿Querés que tu buen historial de pago te sirva fuera de Pasanaku?**
> Si aceptás, vamos a informar a una central de riesgo crediticio **solo** estos hechos de tus grupos: si
> pagaste cada cuota a tiempo o con atraso, y si quedó una deuda con el fondo de tu grupo que ya fue
> confirmada después de tu descargo. No informamos tu nivel, tus insignias, a tus compañeros de grupo ni nada
> más. Podés retirar este permiso cuando quieras en Ajustes → Privacidad. [Nombre de la central] · versión
> [vX], [fecha].
> ☐ Acepto que se informe mi historial de pago. [Aceptar] [Ahora no]

## 4. Contenido del reporte (RF-PX9-02)

### 4.1 Qué se reporta y qué no

| Se reporta (solo hechos) | Fuente del hecho | Cuándo |
|---|---|---|
| **Cuota pagada a tiempo**: grupo (identificador seudónimo), periodo, fecha de vencimiento, fecha del pago confirmado | Evento de pago confirmado por webhook de la pasarela (`aportes`) | Después del corte del periodo |
| **Cuota pagada con atraso**: lo anterior + días de atraso | Ídem | Después del corte del periodo |
| **Deuda firme con el fondo del grupo**: importe con su moneda (`DECIMAL` + `CHAR(3)`), fecha en que quedó firme, estado (vigente, pagada) | Expediente de incumplimiento en estado `FIRME` (`garantia`), con la deuda leída del mayor | **Solo** con el expediente `FIRME` |
| **Deuda firme cancelada**: fecha de pago | Pago confirmado imputado a la deuda | Al confirmarse |

**Nunca se reporta:** el nivel de puntualidad, rachas, insignias ni el «Grupo perfecto»; calificaciones entre
personas (no existen); datos de otros miembros del grupo; el motivo del descargo ni la evidencia del expediente;
una deuda en estado `PROPUESTA`, `NOTIFICADA`, `EN_DESCARGO` o `EN_APELACION`; una cuota que el fondo cubrió
mientras el expediente no está firme; el recargo por mora (va al perjudicado y es anulable); montos de
cuotas pagadas a tiempo (solo el hecho y la fecha) — **`DECISION_REQUIRED`/L-04**: si la central exige el monto
de la cuota, se agrega con su moneda y se actualiza el texto del consentimiento a una versión nueva.

### 4.2 Reglas

1. **Nada antes de `FIRME`** (regla 91.4): la consulta que arma el lote filtra por el estado del expediente en el
   momento del envío, no al momento del hecho. Si el expediente pasa a `EN_APELACION` o `REVOCADA` después de
   reportado, se envía la rectificación (§5).
2. **Desde el mayor** (regla 91.1.5): el importe de una deuda firme sale de los asientos de la cuenta por cobrar
   al moroso, no de una suma sobre la tabla de pagos.
3. **Idempotencia** (regla 91.1.4 aplicada a un envío externo): clave `(usuario_id, tipo_hecho, referencia_hecho,
   version_reporte)` con índice `UNIQUE` en la bitácora; reenviar un lote no duplica hechos en la central.
4. **Minimización** (regla 90.2): se envía el identificador que la central exija (lo define L-04 y la RFP futura
   de PX-9) y nada más; el grupo viaja como seudónimo; nada por URL; nada en logs (solo `reporte_id` y
   `correlationId`).
5. **Frecuencia y formato**: los fija la central y la respuesta a L-04 (`DECISION_REQUIRED`). El diseño asume
   lotes periódicos idempotentes; si la central opera por API por hecho, cambia el despacho, no las reglas.

## 5. Rectificación (RF-PX9-03)

| Situación | Efecto |
|---|---|
| La persona abre un **reclamo** sobre un hecho reportado o por reportar (por el canal de reclamos existente, `reclamo_cliente`) | El hecho queda **suspendido**: no se envía. Si ya se envió, se envía a la central la marca «en disputa», si la central la admite (L-04 / RFP) |
| El reclamo prospera | Se envía la **rectificación** (o el retiro) y queda en la bitácora como fila nueva, nunca editando la anterior |
| El reclamo se rechaza | Se levanta la suspensión y el hecho se envía en el próximo lote |
| Un expediente pasa a `EN_APELACION` o `REVOCADA` después de reportada la deuda | Igual que un reclamo abierto: suspensión o retiro |

El reclamo lo resuelve una persona distinta de quien resolvió el expediente (P-12, cuatro ojos reales con las
identidades tomadas del token). El plazo de respuesta al reclamo es un parámetro normativo (L-10).

## 6. Bitácora (RF-PX9-04)

Tabla nueva, propiedad de `transparencia`, **append-only** con `UPDATE`/`DELETE` revocados al rol de la
aplicación (regla 91.1.3 por analogía: es un registro probatorio). Propuesta de columnas para la microtarea
futura sobre la fuente `.puml` (no se crea en este carril):

| Columna | Tipo | Nota |
|---|---|---|
| `id` | UUID | PK |
| `usuario_id` | UUID | Identificador interno; nunca el documento |
| `consentimiento_id` | UUID | El consentimiento vigente que habilitó el envío (referencia por valor: vive en `identidad`) |
| `version_consentimiento` | VARCHAR(20) | Versión del texto aceptado |
| `tipo_hecho` | VARCHAR(30) CHECK | `PAGO_A_TIEMPO`, `PAGO_CON_ATRASO`, `DEUDA_FIRME`, `DEUDA_CANCELADA`, `RECTIFICACION`, `RETIRO`, `EN_DISPUTA` |
| `referencia_hecho` | UUID | Pago o expediente de origen |
| `importe` / `moneda` | DECIMAL(14,2) / CHAR(3) | Solo en hechos de deuda; `NULL` en los demás, con `CHECK` que lo asegura |
| `lote_id` | UUID | Lote enviado |
| `hash_lote` | VARCHAR(64) | Integridad de lo enviado |
| `enviado_en` | TIMESTAMPTZ | |
| `acuse_central` | VARCHAR(60) | Identificador de acuse de la central |
| `estado` | VARCHAR(15) CHECK | `PREPARADO`, `ENVIADO`, `ACEPTADO`, `RECHAZADO`, `SUSPENDIDO` (cada cambio de estado es una fila nueva) |
| `clave_idempotencia` | VARCHAR(120) | `UNIQUE` (§4.2.3) |

Lectura: la persona ve en la app **qué se reportó de ella** (derecho de acceso); el acceso del personal a esta
bitácora deja rastro en `registro_acceso_datos` (regla 90.2.7).

## 7. Pantallas (diseño funcional)

| Pantalla | Contenido | Estados |
|---|---|---|
| Ofrecimiento (al entrar al primer grupo y en Ajustes → Privacidad) | El texto de §3.5; casilla sin marcar; «Aceptar» / «Ahora no» con el mismo peso visual | Cargando el texto vigente · con datos · error («No pudimos cargar el texto. Reintentá»), sin conexión (no se puede aceptar sin el texto vigente) |
| Ajustes → Privacidad → Historial de pago | Estado del permiso (otorgado el [fecha], versión [vX]) y botón «Retirar permiso» con confirmación | Cargando · otorgado · no otorgado («No compartimos nada. Podés activarlo cuando quieras») · error |
| «Qué informamos de vos» | Lista de hechos enviados con fecha y estado; botón «Reclamar» por fila | Cargando · con datos · vacío («Todavía no informamos nada») · error · suspendido por reclamo (etiqueta con texto, no solo color) |

Accesibilidad y tema según la regla 95 (WCAG 2.2 AA, modo claro y oscuro, movimiento reducido).

## 8. Seguridad y amenazas (regla 90.6)

| Amenaza | Control |
|---|---|
| Reportar a alguien sin consentimiento | Comprobación del consentimiento vigente a la fecha del hecho y del envío; sin fila vigente, no hay envío |
| Reportar una deuda no firme | Filtro por estado del expediente al armar el lote; prueba con expediente en cada estado no firme |
| Doble reporte del mismo hecho | `UNIQUE` sobre `clave_idempotencia` |
| Filtración del lote | Transporte cifrado y autenticado; lote con hash; ningún dato en logs ni URLs |
| Consulta indebida por personal interno | Rol específico, `registro_acceso_datos` |
| Proyección de consentimientos atrasada | Desfase máximo con alerta; si se supera, el envío espera |

## 9. Lo que queda abierto

| Tema | Tipo | A quién |
|---|---|---|
| L-04 completa | Legal | Abogado |
| ¿Consentimiento opcional o requisito de ingreso? (§3.3) | `DECISION_REQUIRED` | Pablo, con el abogado |
| ¿Qué pasa con lo ya reportado al revocar? (§3.4) | Sub-pregunta agregada a L-04 | Abogado |
| ¿Se informa el monto de las cuotas pagadas a tiempo? (§4.1) | `DECISION_REQUIRED` / L-04 | Pablo y abogado |
| Frecuencia, formato e identificador que exige la central (§4.2) | Depende de la central | RFP de PX-9 tras L-04 |
| Tipo `REPORTE_CENTRAL_RIESGO` en `consentimiento` y tabla de bitácora | Microtareas futuras de esquema (regla 97.1) | Carril de F3 de `transparencia` / `identidad` |
| Texto del consentimiento (§3.5) | P-16 | Pablo aprueba; abogado revisa |
