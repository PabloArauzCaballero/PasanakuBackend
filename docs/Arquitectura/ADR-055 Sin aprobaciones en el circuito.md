---
tags:
  - arquitectura
  - adr
  - dinero
  - grupos
  - producto
titulo: "ADR-055 — Sin aprobaciones en el circuito: el pago por QR mueve el flujo"
estado: aceptada
fecha: 2026-11-21
---

# ADR-055 — Sin aprobaciones en el circuito: el pago por QR mueve el flujo

> Ninguna persona aprueba un paso del circuito del pasanaku: el cobro, la cobertura, la entrega, la admisión,
> el inicio, el sorteo y los retiros avanzan por hechos verificados (un webhook firmado, una regla del
> reglamento aceptado, un control automático). Las personas intervienen solo donde la norma o el debido proceso
> lo exigen, y siempre con identidades tomadas del token.

> **Constancia de aceptación (veraz).** Redactado el 2026-11-21 a partir de las decisiones de Pablo, dueño
> del producto, registradas en
> [`DECISIONES.md`](../trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md) (P-06, P-07, P-08, P-09,
> P-10, P-11, P-12, P-13, P-18 y las tandas 5 y 7). Este ADR **no agrega ninguna regla de negocio**: transcribe
> esas decisiones y marca qué partes de ADR-049, ADR-052 y ADR-054 dejan de regir. Lo que DECISIONES no
> resuelve queda como `DECISION_REQUIRED` al final. **Pendiente de la aprobación de Pablo en el PR** y de la
> validación de Cumplimiento en los puntos de [`PREGUNTAS-LEGALES.md`](../trabajo/2026-11-21-plan-correcciones/PREGUNTAS-LEGALES.md).
> **No habilita dinero real** (P-14: todo sigue condicionado al dossier H15).

## Contexto

La auditoría del 2026-11-20 inventarió unas **79 filas de aprobación humana** en los seis lotes
(`docs/trabajo/2026-11-20-auditoria-consolidado/CONSOLIDADO.md` §5). Muchas no deciden nada: una persona
confirma lo que el sistema ya sabe (que el webhook llegó, que el grupo se llenó, que el pozo está completo) o
copia un valor que debería salir del dueño del dato. Además, varias de esas aprobaciones toman al aprobador
**del cuerpo de la petición** (L1 SEG-005, L2 SEG-003, L4 SEG-119, L5 DINERO-002), así que el control es
falsificable: un cuatro ojos que se puede firmar solo es peor que no tenerlo, porque da una garantía que no
existe.

Las reglas que sostenían esas aprobaciones son:

- **ADR-049** — doble aprobación humana de cada retiro desde un umbral (`ck_retiro_doble_aprobacion`).
- **ADR-052 DD-2** — admisión humana obligatoria a un grupo en producción.
- **ADR-054** — grupos sin organizador, pero con piezas que DECISIONES cambia: puerta por emparejamiento con
  admisión humana (D5), organizador profesional (D6), permutas y disolución automática en el reglamento (D7),
  devolución bajo el tope de ADR-049 (D8).
- **`R-SEG-04`** («quien autoriza no ejecuta») aplicado a la entrega del pozo (`ck_entrega_segregacion`) y
  **`R-ORG-06`** («las acciones sensibles exigen confirmación humana») aplicado a entrega, cobro y cobertura.

`docs/Cumplimiento.md` (fila «Segregación de funciones y definición de niveles y montos de autorización» y el
recuadro que la sigue) ya registra que la norma citada en el repositorio **no exige aprobación humana operación
por operación**: exige niveles y montos de autorización definidos, aprobados y auditables. Una política de
controles automáticos con umbrales es la forma de cumplirlo.

## Decisión

### 1. Lo que avanza solo (se eliminan las aprobaciones humanas)

| # | Paso del circuito | Regla nueva | Decisión de origen | Control que reemplaza a la persona |
|---|---|---|---|---|
| E1 | Cobro de la cuota | **Solo** el webhook firmado de la pasarela (o el débito de saldo de billetera recargado por QR) cambia una cuota a cobrada. Se elimina el comprobante manual: pantalla, endpoint, tabla y canales manuales (`EFECTIVO_AL_ORGANIZADOR`, transferencia declarada, `REPORTADO_POR_USUARIO`, `EN_VERIFICACION`) | P-07, P-06, regla 91.3.1 | Firma verificada siempre (también en desarrollo), ventana temporal, orden vigente, monto y moneda exactos, idempotencia por id de evento (ADR-058 RM-04) |
| E2 | Saldo de billetera | Pagar la cuota con saldo recargado por QR **cuenta como pago por QR** y habilita el pago automático | P-06 | El dinero entró por un QR confirmado por webhook; débito y crédito en un solo asiento idempotente |
| E3 | Generación de cuotas del periodo | Automática por calendario del grupo | CONSOLIDADO §5 (L1 AP-02) | Trabajo programado idempotente con ShedLock |
| E4 | Admisión a un grupo | **Solo por invitación** de un miembro, con token de propósito único y vencimiento. Requisitos verificados en el servidor: KYC aprobado, reglamento aceptado en su versión exacta y sin deudas vivas. **No hay grupos públicos** ni emparejamiento | Tanda 5 «Entrada a los grupos», tanda 4 «Admisión» | Verificación en el servidor; 422 con el motivo si falta un requisito |
| E5 | Organizador | **No existe.** Todos los grupos los gestiona el sistema. Se quitan postulaciones, habilitación (básica y profesional), contrato, evaluación de desempeño y el rol `ORGANIZADOR` | P-11 | — |
| E6 | Inicio del grupo y sorteo | **Automáticos al completarse los cupos**, con sorteo verificable (compromiso y revelación). El orden de turnos va por nivel de puntualidad y, dentro de cada nivel, por sorteo (detalle en ADR-056) | Tanda 4 «Inicio y sorteo», tanda 5 «Orden de turnos» | Trabajo programado con ShedLock, timeout y reintento; herramienta pública de verificación del sorteo |
| E7 | Cobertura de una cuota impaga | Automática con el **fondo del grupo** al vencer el plazo de gracia (modelo A, ADR-056). La empresa no cubre nada | P-01/P-02, P-04 | Idempotencia por obligación, límite igual al saldo del fondo verificado en la transacción |
| E8 | Entrega del pozo | **Automática** a la cuenta verificada del beneficiario, confirmada por webhook firmado del proveedor de desembolso. Se eliminan los cuatro ojos de la entrega, su ejecución y fondeo manual y la confirmación manual del desembolso | P-10 | Cuenta verificada (titularidad), enfriamiento tras un cambio de cuenta, webhook firmado, monitoreo UIF (ADR-058) |
| E9 | Retiros de la billetera | **Automáticos con controles**: segundo factor ligado a la operación, cuenta destino verificada, enfriamiento tras un cambio de cuenta, límites por ventana con moneda y monitoreo UIF. Cumplimiento revisa **solo** lo que dispara una alerta | P-09 | Los cinco controles; la orden con alerta queda retenida con su motivo visible |
| E10 | KYC | **Validación automática por un proveedor** (puerto `VerificadorDeIdentidad`). Desenlaces: aprobada, rechazada, excepción o PEP. Una persona de Cumplimiento revisa **solo** las excepciones y los PEP. La app deja de consultar cada 30 s y escucha el cambio de estado. Un actor de sistema asigna el rol de participante | P-08 | Evidencia y puntaje del proveedor guardados; diligencia PEP con cuatro ojos (`docs/Cumplimiento.md` §2.3) |
| E11 | Declaración del incumplimiento | **La declara el sistema** al vencer el plazo de gracia y abre el expediente con plazo de descargo | P-12 | Reloj del reglamento congelado del grupo |
| E12 | Cobro de la comisión | Por el evento de pago confirmado, en el mismo QR de la cuota como línea aparte (ADR-056). Se elimina el cobro marcado a mano | Tanda 7 D-L5-1/D-L5-2, P-07 | Evento de pago verificado |
| E13 | Retiro voluntario de un participante | No queda en `PENDIENTE` esperando a nadie: quien no cobró recupera al cierre del ciclo lo que aportó (cuotas y aporte al fondo); su cupo lo puede tomar un reemplazo invitado | Tanda 7 D-L3-3 | Cálculo desde el mayor |

### 2. Lo que se conserva con una persona (cuatro ojos reales)

En todos estos casos las dos identidades salen **del token de cada una**, nunca del cuerpo de la petición, y
quien propone no puede aprobar (F1.S2.M1 del plan de correcciones).

| # | Control | Por qué se conserva | Origen |
|---|---|---|---|
| C1 | Reembolsos, reversos y cierre de billetera | Mueven dinero fuera del circuito normal | P-13 |
| C2 | Diligencia PEP, caso LFT (analista ≠ revisor) y reporte UIF | Lo exige la norma | `docs/Cumplimiento.md` §2; CONSOLIDADO §5 |
| C3 | Bloqueo judicial y requerimientos de la autoridad | Lo exige la norma; el detalle de qué acciones LFT son cautelares inmediatas lo decide Cumplimiento | CONSOLIDADO §5; `PREGUNTAS-LEGALES.md` (AMB-L4-1) |
| C4 | Descargo y apelación del expediente de incumplimiento | Debido proceso (regla 91.4): el descargo lo resuelve Soporte o Riesgos y la apelación otra persona. La sanción y la reputación esperan al expediente `FIRME`; el reclamo abierto suspende los efectos | P-12, regla 91.4 |
| C5 | Cambio de un parámetro financiero | Una persona lo propone y otra distinta lo aprueba; rige solo para grupos nuevos | Tanda 7 «Quién cambia un parámetro» |
| C6 | Votaciones del grupo | Solo el reemplazo de un participante por invitación y la disolución; quórum del reglamento aceptado; nunca se aprueba por silencio. No hay permuta ni condonación | Tanda 7 D-L2-2, D-L3-1 |
| C7 | Gobierno interno fuera del circuito (comités, ERP, publicidad, roles internos) | No mueve el flujo del pasanaku | CONSOLIDADO §5 |
| C8 | Interruptor manual de un proveedor en el backoffice | Conmutar o apagar un proveedor de dinero es una acción de riesgo | ADR-058 RM-09 |

### 3. Qué deja de regir en los ADR anteriores

| ADR | Parte | Estado |
|---|---|---|
| ADR-049 | La doble aprobación humana de cada retiro desde un umbral y el umbral como disparador de `EN_REVISION` | **Reemplazada** por E9. Se conserva la máquina de estados `AUTORIZADA`/`EN_REVISION` y el `UPDATE` condicionado por estado, ahora para la orden retenida por alerta |
| ADR-052 | DD-2 «admisión humana obligatoria» | **Reemplazada** por E4 |
| ADR-054 | D1 en lo que dice «lo demás de ADR-051 sigue vigente» | **Reemplazada**: ADR-051 queda superada por ADR-056 |
| ADR-054 | D2 (el enlace es la admisión) | **Vigente con un agregado**: el requisito «sin deudas vivas» y la invitación de un miembro (E4) |
| ADR-054 | D3 y D4 (inicio a la fecha límite con ≥ 75 % y cancelación) | **A confirmar**: DECISIONES dice «al completarse los cupos» y no menciona el 75 %. Ver `DECISION_REQUIRED` 1 |
| ADR-054 | D3 en el orden del sorteo | **Reemplazada** por E6 (orden por nivel y sorteo dentro de cada nivel) |
| ADR-054 | D5 (emparejamiento por algoritmo con admisión humana) | **Reemplazada** por E4: solo invitación |
| ADR-054 | D6 (organizador habilitado para profesionales) | **Reemplazada** por E5 |
| ADR-054 | D7 (reglamento plantilla) en permutas y retiro | **Reemplazada** por C6 y E13. La causal de disolución automática por mora generalizada queda en `DECISION_REQUIRED` 2 |
| ADR-054 | D8 (devolución «por donde vino») en lo que se apoya en el tope de ADR-049 | **Reemplazada** por E9; el resto de D8 sigue condicionado al proveedor |
| ADR-054 | D9 en `visibilidad` | **Reemplazada**: no hay grupos públicos |
| ADR-054 | D10 (orden de carriles) | **Reemplazada** por las fases del plan de correcciones (P-18) |

### 4. Restricciones de control interno

- **`R-SEG-04`** («quien autoriza no ejecuta»): **reemplazada para la entrega del pozo** por E8; **conservada**
  para C1 a C5.
- **`R-ORG-06`** («las acciones sensibles exigen confirmación humana»): **reemplazada para entrega, cobro y
  cobertura** por E1, E7 y E8; **conservada para la sanción** (regla 91.4).

Su texto en `docs/Restricciones.md` es fuente del DDL (`scripts/generar_ddl.py`); se ajusta en F2.M8 junto con
`ck_entrega_segregacion`, regenerando el esquema. El estado de las dos queda registrado en `docs/Cumplimiento.md`.

## Motivo

- **Una aprobación que no decide nada es un punto de falla, no un control.** Si la persona confirma lo que el
  webhook ya probó, el único efecto es demorar o, peor, abrir un camino para confirmar lo que el webhook **no**
  probó (el comprobante manual, el cobro marcado a mano).
- **El control real es el que no se puede falsificar.** Una firma criptográfica verificada siempre, una cuenta
  verificada por un tercero, un enfriamiento medido por la base y un segundo factor ligado a la operación
  resisten mejor que un `aprobadoPor` que viaja en el cuerpo.
- **La norma pide niveles de autorización definidos, no personas por operación** (`docs/Cumplimiento.md`,
  fila de segregación de funciones). Lo que se conserva con persona es exactamente lo que la norma o el debido
  proceso exigen (C1 a C4) y lo que no es circuito (C5 a C8).
- **Un solo camino de cobro**: sin canales manuales, la conciliación tiene una sola fuente para cada pago.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Mantener la doble aprobación de retiros desde un umbral (ADR-049) | P-09 la reemplaza por controles automáticos; hoy además ningún retiro sale (CONSOLIDADO §5) |
| Mantener el comprobante manual como respaldo del QR | P-07 lo elimina; un comprobante no es un pago (regla 91.3.1) |
| Cuatro ojos en la entrega del pozo | P-10; el riesgo que cubría (destino equivocado) lo cubren la titularidad verificada y el enfriamiento |
| Admisión humana o grupos públicos con emparejamiento | Tanda 5: entrada solo por invitación entre conocidos |
| Conservar el organizador como opción profesional | P-11 |
| Dejar al aprobador en el cuerpo «mientras tanto» | Es un control falsificable; F1.S2.M1 lo elimina en F1 |

## Consecuencias

- **Código que se retira (F2):** F2.M7 (comprobante manual y canales manuales), F2.M8 (aprobaciones
  eliminables, con la tabla del CONSOLIDADO §5 resuelta fila por fila), F2.M6 (organizador profesional) y la
  parte de F2.M1 que toca la entrega manual.
- **Código que se construye (F3):** H3.S1 (QR → webhook → cobrado), H3.S2 (entrega automática), H3.S4
  (invitación, inicio y sorteo automáticos), H3.S5 (KYC por proveedor y retiros con controles), H3.S6
  (debido proceso). Cada pieza tiene su microtarea en `docs/trabajo/2026-11-21-plan-correcciones/PLAN.md`.
- **Cuatro ojos reales (F1):** F1.S2.M1 crea el componente único que entrega el actor y el segundo actor desde
  el token; todo campo `aprobadoPor`, `autorizador`, `revisor` o similar sale de los DTO.
- **Sin proveedores contratados todavía (P-15):** cada control automático se construye contra un puerto y un
  doble con firma real en tres niveles (regla 65, ADR-058). Mientras tanto, el circuito automático está
  verificado contra dobles, no contra el proveedor real.
- **Riesgo:** si Cumplimiento entiende que alguna norma exige segregación humana en la entrega o en el cobro,
  este ADR se revisa en ese punto. La pregunta está en `PREGUNTAS-LEGALES.md`.

## Cómo se verifica

- Este documento: `python3 scripts/verificar_boveda.py` (ADR indexado, seis secciones, estado válido, número
  único) y `grep -n "Reemplazado parcialmente por ADR-055"` sobre ADR-049 y ADR-054.
- El circuito: el kill-test del plan de correcciones. Un `curl` sin firma al webhook no cambia una cuota; un
  `POST` con un aprobador inventado en el cuerpo no logra ningún efecto (422 por campo no declarado). Cada
  microtarea de F2 y F3 citada arriba trae su prueba.
- Inventario: al cerrar F2.M8, cada fila del CONSOLIDADO §5 tiene su estado (eliminada con su microtarea o
  conservada con su control C1–C8).

## DECISION_REQUIRED

No se resuelven acá porque DECISIONES no las cubre. Las decide Pablo.

1. **Inicio con cupos incompletos.** DECISIONES dice «inicio automático al completarse los cupos». ADR-054 D3 y
   D4 permitían iniciar en la fecha límite con ≥ 75 % y, si no, cancelar y devolver. ¿Sigue valiendo la regla del
   75 % con fecha límite, o el grupo solo arranca lleno? Mientras no se decida, el código no cambia R-GRP-20.
2. **Disolución automática por mora generalizada** (ADR-054 D7). La tanda 7 fija la disolución **votada**. ¿Se
   conserva además la causal automática?

## Referencias

- [`DECISIONES.md`](../trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md) ·
  [`CONSOLIDADO.md`](../trabajo/2026-11-20-auditoria-consolidado/CONSOLIDADO.md) §5 ·
  [plan de correcciones](../trabajo/2026-11-21-plan-correcciones/PLAN.md) (H1.S2, H2, H3) ·
  [`PREGUNTAS-LEGALES.md`](../trabajo/2026-11-21-plan-correcciones/PREGUNTAS-LEGALES.md).
- [[ADR-049 Doble aprobación de retiros|ADR-049]] · [[ADR-052 Decisiones adoptadas por delegación del dueño|ADR-052]] ·
  [[ADR-054 Grupos sin organizador humano|ADR-054]] · ADR-056 «Tu garantía vuelve y la puntualidad vale plata» y
  ADR-058 «Integración con proveedores externos» (se proponen en los PR que siguen a este).
- `docs/Cumplimiento.md` (segregación de funciones; §2 PEP, LFT y UIF) · `docs/Restricciones.md` (`R-SEG-04`,
  `R-ORG-06`).
