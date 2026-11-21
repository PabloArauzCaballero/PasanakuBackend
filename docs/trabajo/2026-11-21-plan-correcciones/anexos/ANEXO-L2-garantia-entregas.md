# Anexo L2 — Correcciones por hallazgo

Fuente: informe de auditoría del módulo `garantia-entregas` (lote L2, commit auditado `07fc6971` de
`origin/test`, 68 fichas `garantia-entregas-<CLASE>-<NNN>`). Decisiones aplicadas:
`docs/trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md` (2026-11-21). Peldaño de todo lo que sigue:
`DISCOVERED` (plan; nada se ejecutó).

Ejes de rediseño que ordenan este anexo:

- **P-04:** el respaldo empresarial se elimina entero (código, endpoints, tópicos, tablas, cuentas 9.x,
  pruebas, mocks, textos). No se parchea: se retira.
- **P-01/P-02 (modelo A):** el «fondo de garantía» actual (saldo cacheado en `garantia.fondo_garantia`,
  cobertura manual con umbral, sin asiento) se **rediseña** como fondo **del grupo** en cuenta segregada del
  mayor: cubre la cuota de quien no paga, subroga la deuda al fondo, lo recuperado vuelve al fondo y al cierre
  se devuelve a cada participante lo aportado menos la parte igual de lo incobrable.
- **P-03:** el aval se elimina. **P-11:** el organizador se elimina; los grupos los gestiona el sistema.
- **P-05:** el recargo por mora va al perjudicado. **P-10:** entrega automática a cuenta verificada con
  webhook firmado del proveedor. **P-12:** el sistema declara el incumplimiento; descargo por Soporte/Riesgos,
  apelación por otra persona. **P-15:** pasarela y proveedor de desembolso detrás de un puerto con doble de
  firma real en tres niveles (regla 65). Parámetros financieros: configurables, obligatorios, sin valor por
  defecto.

Las ambigüedades A-1 a A-4 del informe quedan resueltas así: A-1 → P-08/P-09/P-10 (con la validación de
Cumplimiento pendiente que DECISIONES lista); A-2 → modelo A (el fondo del grupo cubre; la deuda queda a
nombre de quien no pagó); A-3 → respaldo y aval eliminados, fondo mutual reemplazado por A; A-4 → P-05 y
expediente para sanción/reputación (regla 91.4).

## Resumen

**Microtareas por fase:** F0 = 4 · F1 = 14 · F2 = 9 · F3 = 14 · F4 = 18 · F5 = 0 · **total 59**.

**Hallazgos por fase y severidad** (por microtarea principal de cada ficha):

| Fase | S1 | S2 | S3 | S4 | Total |
|---|---:|---:|---:|---:|---:|
| F0 | 0 | 1 | 3 | 0 | 4 |
| F1 | 8 | 5 | 2 | 1 | 16 |
| F2 | 5 | 3 | 5 | 1 | 14 |
| F3 | 0 | 11 | 5 | 0 | 16 |
| F4 | 0 | 5 | 9 | 4 | 18 |
| F5 | 0 | 0 | 0 | 0 | 0 |
| **Total** | **13** | **25** | **24** | **6** | **68** |

**Se resuelve con** (fichas sin microtarea propia, cerradas por otra corrección):

| Hallazgo | Se resuelve con | Motivo |
|---|---|---|
| `garantia-entregas-SEG-001` (S1) | F2.L2.M02 | `recuperarRespaldo` desaparece con el respaldo (P-04); lo recuperado en el modelo A vuelve al fondo del grupo solo por evento de pago (F3.L2.M05). |
| `garantia-entregas-DINERO-006` (S2) | F2.L2.M02 | La doble cobertura con `turnoId` distintos es del respaldo (`CU23CubrirFaltanteDelCorte`), que se retira. La unicidad por periodo se exige en la cobertura del modelo A (F3.L2.M05). |
| `garantia-entregas-DOC-001` (S4) | F2.L2.M02 | El README y los comentarios hablan del asiento del respaldo; se reescriben en el mismo PR que lo retira. |
| `garantia-entregas-DEBIDO-PROCESO-001` (S1) | F2.L2.M04 | P-03 elimina el aval: no queda ejecución ni subrogación al avalista. |
| `garantia-entregas-COHERENCIA-001` (S3) | F2.L2.M04 | `ejecucion_aval` y R-GAR-04 se retiran con el aval (P-03). |
| `garantia-entregas-PISADA-001` (S4) | F1.L2.M09 | La convivencia legacy/fondeo en CU-22 que sugiere el revert desaparece al retirar `POST /entregas` y reescribir la máquina de estados (F1.L2.M08). |

**Sin acción** (ficha completa): ninguna. Partes sin acción dentro de una ficha: la propuesta «agregar
`AVALISTA` como acreedor» de `DINERO-012` (P-03 elimina el aval); la «cobertura del fondo mutual con umbral de
aprobación» de `APROB-002`/`SEG-003` como mecanismo a conservar (P-01/P-02 la reemplazan por el modelo A).

**Decisiones que siguen abiertas y condicionan microtareas** (no se resuelven acá):

- D-L2-1 — ¿Sigue existiendo la **cesión del turno por subasta** (CU-136/64)? DECISIONES no la menciona; el
  orden por puntualidad (tanda 5) y el descarte de B («turno con precio») pueden volverla innecesaria.
  Condiciona F4.L2.M03, M07, M13, M14, M15 (si se retira, esas microtareas pasan a «se resuelve con» el retiro).
  Confirmar con Pablo.
- D-L2-2 — ¿Sigue existiendo el **reemplazo de participante** y la **disolución anticipada** sin organizador?
  Condiciona F1.L2.M04 y F4.L2.M12. Confirmar con Pablo.
- D-L2-3 — Regla de **residuo de redondeo** en la devolución del fondo y en el reparto de incobrables
  (F3.L2.M06). Se propone una regla determinista; la aprueba Pablo.
- Validaciones de Cumplimiento listadas en DECISIONES (fondo segregado ≠ captación; cobertura y subrogación
  sin expediente previo) condicionan F3.L2.M04 y F3.L2.M05 antes de producción, no su construcción.

---

## F0 — Preparación

### F0.L2.M01 — ADR que supersede ADR-051 y alinea catálogo de flujos, casos de uso y verificador
- Hallazgos: `garantia-entregas-DOC-002` (S3)
- Causa de raíz: P-04 y el modelo A no tienen ADR; ADR-051 (regla 7, D-02/D-03/D-15), CU-22/23/24, los
  catálogos `aporte-mora-cobertura.yaml` y `entrega-del-turno.yaml` y los OpenAPI siguen describiendo «la
  empresa aporta el faltante», y `scripts/validar_flujos.py` lo valida como correcto. Un agente que lea el repo
  reimplementa el respaldo.
- Corrección de raíz: ADR nueva en `docs/Arquitectura/` que deja sin efecto las decisiones de respaldo de
  ADR-051 y fija el modelo A + D (fondo del grupo segregado, subrogación, devolución al cierre con reparto de
  incobrables, recargo al perjudicado, entrega automática). Reescribir `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml`
  (quitar `cubrir-faltante` → `recuperar-respaldo`; pasos del modelo A) y `entrega-del-turno.yaml` (pasos
  automáticos por evento), CU-22/CU-23 en `docs/CasosDeUso`, y la descripción de `fondearPozo` en
  `servicios/entregas/src/main/resources/openapi/entregas.yaml:324-353` y `garantia.yaml:64,434-560`.
  Ajustar `scripts/validar_flujos.py` para que falle si un flujo referencia cuentas 9.x o pasos de respaldo.
  Resuelve la causa porque la documentación y el verificador pasan a sostener la regla vigente.
- CA: Dado el repo actualizado, cuando se corre el verificador de flujos sobre un catálogo que todavía nombra
  `recuperar-respaldo` o la cuenta `9.1.02`, entonces falla; y sobre los catálogos nuevos, pasa.
- DoD: `python3 scripts/validar_flujos.py` en verde con los catálogos nuevos y en rojo con un fixture que
  contiene un paso de respaldo (salida pegada); `grep -rn 'empresa aporta el faltante\|RESERVA_RESPALDO' docs/`
  sin resultados fuera de ADR-051 (marcada «Reemplazada por ADR-0xx»); ADR aprobada por Pablo en el PR.
- Depende de: ninguna (insumo de todo F2/F3 de este lote)
- Esfuerzo: medio

### F0.L2.M02 — ADR que reemplaza R-SEG-04 y R-ORG-06 (ADR-054) para entrega, cobro y cobertura, y registro normativo
- Hallazgos: `garantia-entregas-REG-001` (S3)
- Causa de raíz: los cuatro ojos de la entrega (R-SEG-04, `ck_entrega_segregacion`) y la confirmación humana
  obligatoria de R-ORG-06 se impusieron sin fuente normativa registrada; ADR-054 quedó «pendiente de
  ratificación de Cumplimiento».
- Corrección de raíz: ADR nueva que reemplaza ADR-054 en lo que toca a este módulo: entrega, cobro y cobertura
  sin confirmación humana (P-10, modelo A); sanción y reputación con expediente (regla 91.4, P-12). Registrar en
  `docs/Cumplimiento.md` los controles que sustituyen a los cuatro ojos (cuenta verificada por proveedor,
  enfriamiento, titularidad beneficiario↔cuenta, webhook firmado, monitoreo UIF) y la pregunta abierta a
  Cumplimiento sobre si alguna norma ASFI/UIF exige segregación en la entrega. Resuelve la causa: cada control
  queda con su fuente o con su ausencia declarada.
- CA: Dado `docs/Cumplimiento.md`, cuando se busca R-SEG-04 o R-ORG-06, entonces cada uno tiene su estado
  (reemplazado por ADR-0xx / conservado por 91.4) y el control sustituto con su microtarea.
- DoD: `grep -n 'R-SEG-04\|R-ORG-06' docs/Cumplimiento.md docs/Restricciones.md` muestra estado y
  referencia; ADR revisada por Pablo; la consulta a Cumplimiento queda como issue enlazado.
- Depende de: decisiones P-10, P-12; validación de Cumplimiento (no bloquea la redacción)
- Esfuerzo: chico

### F0.L2.M03 — Pactos consumer-driven para los saltos que mueven dinero y sobreviven al flujo nuevo
- Hallazgos: `garantia-entregas-CONTRATO-002` (S2)
- Causa de raíz: los pactos de `contratos/pactos/` cubren solo eventos de nucleo-financiero; las llamadas
  entregas→aportes (recaudo), garantía→aportes (mora/obligaciones) y garantía→cumplimiento no tienen contrato
  verificado en CI (98.1.4).
- Corrección de raíz: escribir un pacto por cada salto que el diseño nuevo conserva — el evento
  «turno/periodo cobrado» que consume entregas (F3.L2.M01), la consulta de mora/obligaciones que usa garantía
  (F1.L2.M02/M03, F3.L2.M05), el evento de pago confirmado que levanta la restricción (F3.L2.M11) y
  garantía→cumplimiento — con verificación del lado productor. El salto entregas→garantía (respaldo) **no**
  se pacta: se retira (F2.L2.M01).
- CA: Dado un cambio incompatible en el payload de `/aportes/periodos/{id}/recaudo` o del evento de cobro,
  cuando corre la verificación del productor, entonces falla.
- DoD: prueba de verificación del pacto en el productor (aportes) y del consumidor (entregas, garantía) en
  verde; mutación deliberada de un campo → rojo (salida pegada de ambas corridas).
- Depende de: F0.L2.M01 (forma del flujo); coordinación con lote L1 (aportes)
- Esfuerzo: medio

### F0.L2.M04 — Verificación del lado productor del pacto `nucleo-financiero-entregas.json`
- Hallazgos: `garantia-entregas-CONTRATO-003` (S3)
- Causa de raíz: el pacto existe solo del lado consumidor (`PactoConEntregasContratoTest`); entregas no lo
  verifica.
- Corrección de raíz: prueba de verificación del pacto en `servicios/entregas/src/test` que levanta el
  controlador real contra el pacto, dentro de la tarea de pruebas del servicio.
- CA: Dado el pacto vigente, cuando entregas cambia la respuesta de la oferta reservada, entonces la prueba del
  productor falla.
- DoD: prueba nueva en verde; mutación de un campo del DTO → rojo (salida pegada).
- Depende de: ninguna
- Esfuerzo: chico

---

## F1 — Seguridad e integridad del mayor

### F1.L2.M01 — Cerrar BOLA en garantía: ninguna operación de grupo con solo `GRUPO_ADMINISTRAR`
- Hallazgos: `garantia-entregas-SEG-002` (S1)
- Causa de raíz: la guardia (`plataforma/comun-web/.../GuardiaDePermiso.java`) autoriza solo por permiso; no
  hay chequeo de relación con el grupo, y las tablas sin `usuario_id` no reciben RLS. Con P-11 el rol que hoy
  ejerce esas operaciones (organizador) ni siquiera debe existir.
- Corrección de raíz: quitar `GRUPO_ADMINISTRAR` de todas las operaciones de `GarantiaController`,
  `IncumplimientosController` y `CobranzaController`. Las que mueven dinero o estado del grupo (declarar,
  cubrir, devolver, restringir, levantar) pasan a invocarse solo por el sistema (consumidor de eventos o token
  de servicio con alcance, F1.L2.M14); las humanas que sobreviven (resolver descargo, apelación) exigen rol
  SOPORTE/RIESGOS más regla de objeto (quien resuelve no es el imputado; quien apela-resuelve ≠ quien resolvió
  el descargo), aplicada en la escritura. Quitar `GRUPO_ADMINISTRAR` de la semilla de ORGANIZADOR
  (`sql/60_semillas/10-roles-y-permisos.sql:152`). Resuelve la causa: no queda una operación de grupo
  autorizada solo por rol.
- CA: Dado un usuario con rol ORGANIZADOR (o cualquier humano sin rol de sistema), cuando invoca disolución,
  devolución, cobertura, restricción o reemplazo de un grupo cualquiera, entonces recibe 403 y no hay escritura.
- DoD: matriz de autorización negativa por endpoint (sin token, rol insuficiente, otro grupo, otro usuario,
  token de servicio sin alcance) como prueba de integración contra base real con el rol `svc_garantia`;
  verificación `SELECT count(*)` sin cambios tras cada 403 (salida pegada).
- Depende de: F1.L2.M14 (token de servicio); retiro del organizador (P-11, lote L3)
- Esfuerzo: medio

### F1.L2.M02 — Retirar la cobertura manual del fondo con mora, monto y aprobador del cuerpo
- Hallazgos: `garantia-entregas-SEG-003` (S1), `garantia-entregas-APROB-002` (S2)
- Causa de raíz: `CU23CubrirIncumplimiento` decide dinero con `diasMora`, `montoSolicitado` y `aprobadaPor`
  del cuerpo, sin expediente firme, y modela un umbral de aprobación humana con un estado `SOLICITADA` sin
  salida. Es el diseño del fondo mutual administrado por personas que el modelo A reemplaza.
- Corrección de raíz: retirar `POST /incumplimientos/{id}/cobertura` (versión y fecha de retiro en
  `garantia.yaml`, 98.1.3), el estado `SOLICITADA`, el tópico `cobertura_requiere_aprobacion` y el umbral
  `politica_cobertura` (modelo `.puml` → DDL). La cobertura renace en F3.L2.M05 como acto del sistema, con mora
  y monto leídos de aportes. Resuelve la causa: ya no existe un camino donde el cliente afirme los datos que
  deciden la cobertura.
- CA: Dado cualquier usuario, cuando llama a la ruta de cobertura manual, entonces recibe 404/410 y no se
  escribe `cobertura_incumplimiento`.
- DoD: prueba web del retiro (410 con código de contrato) e integración que demuestra que no hay escritura;
  `grep -rn 'aprobadaPor\|SOLICITADA' servicios/garantia/src/main` vacío (salida pegada).
- Depende de: F0.L2.M01
- Esfuerzo: medio

### F1.L2.M03 — Devolución del fondo con precondiciones leídas de sus dueños, no del cuerpo
- Hallazgos: `garantia-entregas-SEG-004` (S1)
- Causa de raíz: `CU29DevolverFondo` decide con `grupoCerrado` y `deudasVivas` del cuerpo
  (`GarantiaController.java:63-69`).
- Corrección de raíz: eliminar ambos campos de la entrada; dentro de la transacción, consultar a grupos el
  estado del grupo (cliente con timeout y token de servicio) y contar las deudas vivas en el propio esquema,
  con la precondición en la escritura. La operación queda solo para el sistema (la dispara el evento de cierre
  de ciclo en F3.L2.M06). Resuelve la causa: las precondiciones salen de su dueño.
- CA: Dado un grupo en curso, cuando se intenta la devolución (con cualquier cuerpo), entonces se rechaza con el
  código de contrato y el fondo no cambia.
- DoD: integración contra base real con doble de grupos en tres niveles (cerrado, en curso, grupos caído →
  rechazo sin escritura); prueba con deudas vivas > 0 → rechazo (salida pegada).
- Depende de: F1.L2.M01, F1.L2.M14
- Esfuerzo: chico

### F1.L2.M04 — `Dinero` de entrada sin signo; disolución y reemplazo con cifras de sus dueños
- Hallazgos: `garantia-entregas-DINERO-007` (S2)
- Causa de raíz: el esquema `Dinero` (`pattern: '^-?\d+\.\d{2}$'`) se usa igual en entrada y salida, y
  `CU66ReemplazarParticipante`/`CU67DisolverGrupo` aceptan masa, totales y deuda asumida del cliente.
- Corrección de raíz: separar `DineroEntrada` (sin signo, `>= 0`) de `Dinero` en `garantia.yaml` y
  `entregas.yaml` con versión; en CU66/CU67 leer totales de aportes/entregas/mayor y no del cuerpo. Si D-L2-2
  retira reemplazo/disolución, la parte de CU66/CU67 se cierra con su retiro.
- CA: Dado un importe de entrada negativo, cuando llega a cualquier operación, entonces se rechaza con 400 del
  contrato; dado un cuerpo con totales distintos de los reales, entonces la disolución usa los reales.
- DoD: pruebas web de validación (negativo, cero, máximo); integración de CU67 con doble de aportes en tres
  niveles (salida pegada).
- Depende de: D-L2-2
- Esfuerzo: medio

### F1.L2.M05 — Cortar la acreditación humana del desembolso
- Hallazgos: `garantia-entregas-SEG-005` (S1) — se completa con F3.L2.M02
- Causa de raíz: `POST /desembolsos/{id}/respuesta` con `exitoso=true` marca ACREDITADA y publica
  `desembolso_acreditado` con asiento; no hay firma ni integración del proveedor.
- Corrección de raíz: retirar el endpoint humano y el permiso de anotar respuesta. La única vía para ACREDITADA
  pasa a ser el webhook firmado del proveedor (F3.L2.M02). Mientras no exista, la orden queda
  `ENVIADA_A_PROVEEDOR` (P-14: no se mueve dinero real). Si Tesorería necesita conciliación, es una vista de
  solo lectura sin asiento.
- CA: Dado un usuario de Tesorería, cuando intenta anotar una respuesta de desembolso, entonces la ruta no
  existe y no se publica `desembolso_acreditado`.
- DoD: prueba web 404/410; integración que verifica que el outbox no tiene `desembolso_acreditado` tras el
  intento (salida pegada).
- Depende de: ninguna
- Esfuerzo: chico

### F1.L2.M06 — El desembolso exige que la cuenta destino sea del beneficiario
- Hallazgos: `garantia-entregas-SEG-006` (S1)
- Causa de raíz: `CU28EmitirDesembolso` valida verificación y enfriamiento de la cuenta pero no su titularidad
  contra el beneficiario; la entrega guarda `beneficiario_participante_id` y la cuenta `usuario_id` sin vínculo.
- Corrección de raíz: persistir en la entrega el `usuario_id` del beneficiario (resuelto de grupos al crear la
  entrega, por evento o API con pacto) y exigir `cuenta.usuario_id = entrega.beneficiario_usuario_id` y misma
  moneda en la escritura de la orden y por trigger en el modelo. Con la entrega automática, la cuenta la elige
  el sistema (la verificada del beneficiario), no quien emite.
- CA: Dada una entrega de A, cuando se emite una orden hacia una cuenta verificada de B, entonces se rechaza y
  no se crea `orden_desembolso`.
- DoD: integración contra base real: cuenta de otro usuario → rechazo; moneda distinta → rechazo; intento por
  SQL directo con el rol de la aplicación → el trigger lo impide (salida pegada).
- Depende de: F2.L2.M05 (estados), pacto con grupos (F0.L2.M03)
- Esfuerzo: medio

### F1.L2.M07 — El número de cuenta se cifra solo en el servidor, con clave dedicada
- Hallazgos: `garantia-entregas-SEG-007` (S1)
- Causa de raíz: atajo de pruebas en `CU18RegistrarCuentaDestino.java:101-108` que acepta un `numeroCifrado` del
  cliente distinto del número que se enmascara y deduplica; además la misma `pimienta` es clave de cifrado y
  pepper de hash.
- Corrección de raíz: eliminar el campo `numeroCifrado` de la entrada y del contrato; cifrar siempre en
  servidor desde `numeroEnClaro` con una clave de cifrado propia (rotable, por configuración obligatoria) y
  separar el pepper del hash. Migrar las cuentas existentes recifrando (datos sintéticos, P-14).
- CA: Dado un pedido con `numeroCifrado`, cuando se registra la cuenta, entonces se rechaza con 400; dado un
  registro válido, entonces descifrar el valor guardado devuelve exactamente el número del que salen máscara y
  hash.
- DoD: prueba unitaria de ida y vuelta; prueba web del rechazo; el servicio no arranca sin la clave de cifrado
  (prueba de arranque) (salidas pegadas).
- Depende de: ninguna
- Esfuerzo: chico

### F1.L2.M08 — Máquina de estados única de la entrega: retirar `ejecutar` y exigir resultado de cada transición
- Hallazgos: `garantia-entregas-DINERO-001` (S1)
- Causa de raíz: dos caminos de cierre (legacy `ejecutarEntrega` con monto libre y CU-28) sin máquina de
  estados única; `emitir` no mueve la entrega y el resultado de `cambiarEstado` se ignora al acreditar.
- Corrección de raíz: retirar `POST /entregas/{id}/ejecucion` y `marcarEntregada` con monto libre; `emitir`
  lleva la entrega a `EN_PROCESO_DESEMBOLSO` con precondición en el `UPDATE`; la acreditación lleva a ENTREGADA
  con `monto_efectivamente_entregado = neto` y falla (rollback, sin asiento) si la transición devuelve false.
  Índice único parcial: una sola orden viva por entrega.
- CA: Dada una entrega con orden en vuelo, cuando se intenta otro cierre o una segunda orden, entonces se
  rechaza y el mayor no recibe un segundo asiento.
- DoD: integración contra base real con dos emisiones concurrentes (una gana, otra 409); acreditación sobre
  entrega en estado inválido → sin asiento en el outbox (salida pegada).
- Depende de: F2.L2.M05
- Esfuerzo: medio

### F1.L2.M09 — Retirar `POST /entregas` legacy y `EFECTIVO_ORGANIZADOR`
- Hallazgos: `garantia-entregas-DINERO-003` (S1); cierra también `garantia-entregas-PISADA-001` (S4)
- Causa de raíz: la liquidación vieja (`CU22LiquidarEntrega.liquidar`) quedó viva junto al fondeo: confía en
  bruto/recaudado del cliente, descarta la clave de idempotencia y admite `EFECTIVO_ORGANIZADOR` (contra
  RN-18/91.5). El historial con revert y reaplicación de CU-22 es el síntoma de esa convivencia.
- Corrección de raíz: retirar la operación del OpenAPI (versión y fecha), el controlador y la rama del caso de
  uso; quitar `EFECTIVO_ORGANIZADOR` del enum del contrato y del modelo `docs/entidades` → DDL (CHECK de
  `entrega_fondo.sql:31`). La única fuente de una entrega es el fondeo por evento (F3.L2.M01). Al cerrar,
  revisar con `git log -p` que no quede código de la versión revertida.
- CA: Dado cualquier usuario, cuando llama `POST /entregas`, entonces la ruta no existe; dado un intento de
  insertar `EFECTIVO_ORGANIZADOR`, entonces la base lo rechaza.
- DoD: prueba web 404/410; integración con INSERT del método prohibido → violación de CHECK (salida pegada);
  `grep -rn EFECTIVO_ORGANIZADOR` vacío fuera de ADR/historial.
- Depende de: F0.L2.M01
- Esfuerzo: medio

### F1.L2.M10 — La retención del saldo de la entrega se crea en nucleo-financiero, no la afirma el cliente
- Hallazgos: `garantia-entregas-DINERO-002` (S2)
- Causa de raíz: `CU28EmitirDesembolso.java:106-111` acepta `saldoRetenido` como booleano de entrada; la
  retención real nunca se integró.
- Corrección de raíz: quitar el campo; dentro de la emisión, pedir a nucleo-financiero la retención del neto
  (operación idempotente con clave = id de la entrega, timeout y circuito), y emitir la orden solo con la
  retención confirmada. La liberación de la retención es la compensación de la saga si el proveedor rechaza.
- CA: Dado un pozo sin saldo suficiente en el mayor, cuando el sistema intenta emitir, entonces no se crea la
  orden y la entrega queda en estado recuperable.
- DoD: integración con doble de nucleo-financiero en tres niveles (retención concedida, saldo insuficiente,
  servicio caído/timeout); retención pedida dos veces con la misma clave → una sola (salida pegada).
- Depende de: F1.L2.M14; operación de retención en nucleo-financiero (lote L1)
- Esfuerzo: medio

### F1.L2.M11 — Importes de garantía `>= 0` y `clave_idempotencia UNIQUE` en el modelo
- Hallazgos: `garantia-entregas-DINERO-009` (S2)
- Causa de raíz: el modelo de garantía es anterior a la regla 91; solo las tablas de respaldo recibieron
  restricciones.
- Corrección de raíz: en `docs/entidades/*.puml` + `docs/Restricciones.md` (fuente de verdad; el SQL es
  generado), agregar `CHECK monto >= 0` a cada importe de las tablas que sobreviven al rediseño A (deuda,
  subrogación, abono de recuperación, movimiento del fondo con tipo que da el signo, castigo, quita) y
  `clave_idempotencia` con índice `UNIQUE` nombrado en las que registran movimiento de dinero. Las tablas del
  aval y del respaldo no se tocan: se retiran (F2.L2.M03/M04). Expandir/contraer (97.3.5).
- CA: Dado un INSERT con importe negativo o con clave de idempotencia repetida, cuando se ejecuta con el rol de
  la aplicación, entonces la base lo rechaza.
- DoD: aplicación del DDL generado en el entorno Docker (F0); verificación de deriva modelo↔base↔jOOQ en verde;
  pruebas de integridad por tabla (negativo → CHECK; clave repetida → unique) (salida pegada).
- Depende de: F2.L2.M03, F2.L2.M04 (para no endurecer tablas que se retiran)
- Esfuerzo: medio

### F1.L2.M12 — `Idempotency-Key` propagada y persistida en las mutaciones de garantía y entregas
- Hallazgos: `garantia-entregas-CONTRATO-001` (S3)
- Causa de raíz: el controlador generado recibe la clave y el caso de uso no la modela
  (`IncumplimientosController.java:70`, `EntregasController.java:65`); otras mutaciones ni la piden.
- Corrección de raíz: cada caso de uso que sobrevive y mueve dinero o estado recibe la clave, la persiste en la
  columna `UNIQUE` de F1.L2.M11 y devuelve la misma respuesta ante el reintento. En las operaciones del sistema
  disparadas por evento, la clave es el `message_id`.
- CA: Dada la misma petición enviada dos veces con la misma clave, cuando se procesa, entonces la segunda
  devuelve el mismo `id` y no hay segunda fila.
- DoD: prueba de idempotencia por operación (dos llamadas → mismo id, `count(*) = 1`) e intento concurrente
  (salida pegada).
- Depende de: F1.L2.M11
- Esfuerzo: chico

### F1.L2.M13 — Una sola versión del cliente HTTP con circuito en `plataforma/comun-web`
- Hallazgos: `garantia-entregas-DUP-X-001` (S3)
- Causa de raíz: garantía y entregas copiaron `ClienteConCircuito` y `ConfiguracionDeCircuito` en vez de usar la
  plataforma; el arreglo del token (F1.L2.M14) habría que hacerlo dos veces.
- Corrección de raíz: mover el adaptador a `plataforma/comun-web` (o usar el que ya exista ahí), borrar las dos
  copias y hacer que los adaptadores de ambos servicios lo usen. Se adelanta a F1 porque es el prerequisito de
  F1.L2.M14.
- CA: Dado el árbol, cuando se busca `class ClienteConCircuito`, entonces aparece una sola vez, en plataforma.
- DoD: `grep -rln 'class ClienteConCircuito' servicios plataforma` → un archivo; pruebas de adaptadores
  (`RecaudoCaidaTest` y equivalentes) en verde (salida pegada).
- Depende de: ninguna
- Esfuerzo: chico

### F1.L2.M14 — Identidad de servicio con alcance acotado en las llamadas entre servicios
- Hallazgos: `garantia-entregas-SEG-009` (S2)
- Causa de raíz: no hay identidad de servicio emitida por plataforma; el cliente propaga el `Authorization` del
  humano y entregas se autentica con una cuenta de usuario (teléfono, clave, TOTP) configurada en
  `application.yml`.
- Corrección de raíz: token de servicio (credencial de cliente con alcance por operación) emitido por
  identidad/plataforma, inyectado por el cliente único de F1.L2.M13; los servicios llamados validan el alcance
  del servicio, no un permiso humano. Retirar `credencial-servicio` de usuario de la configuración.
- CA: Dada una llamada de garantía a aportes, cuando el token es de un humano o de un servicio sin el alcance,
  entonces aportes responde 403.
- DoD: integración con el doble del emisor: token de servicio con alcance → 200; sin alcance → 403; token humano
  → 403; el servicio no arranca sin su credencial (salida pegada).
- Depende de: F1.L2.M13; emisor de tokens de servicio (plataforma/identidad)
- Esfuerzo: medio

---

## F2 — Retiro de lo que contradice el flujo nuevo

### F2.L2.M01 — Retirar el respaldo empresarial de entregas
- Hallazgos: `garantia-entregas-RESPALDO-001` (S1)
- Causa de raíz: ADR-051 implementado en entregas: `CU22EntregarPozoCompleto.java:67-85` llama a
  `RespaldoEmpresarial` (`RespaldoPorHttp` → `/garantia/respaldo/coberturas`) en cada fondeo con faltante.
- Corrección de raíz: borrar el puerto `RespaldoEmpresarial`, `RespaldoPorHttp`, la rama de cobertura del
  fondeo y su configuración (`application.yml:46`); el faltante deja de ser «cubierto por la empresa» y pasa a
  esperar la cobertura del fondo del grupo (F3.L2.M05). Retirar pruebas, mocks (`fondearPozo.json`), Postman y
  clientes que lo ejercitan (`RespaldoCaidaTest`, ramas de CU133). No se deja bandera: se retira.
- CA: Dado un periodo con faltante, cuando se fondea, entonces no se llama a garantía/respaldo y no se publica
  ningún tópico `cobertura_empresarial_*`.
- DoD: `grep -rn 'Respaldo' servicios/entregas/src` vacío; integración del fondeo con faltante sin llamadas
  salientes al respaldo (doble que falla si lo llaman) (salida pegada).
- Depende de: F0.L2.M01
- Esfuerzo: medio

### F2.L2.M02 — Retirar el respaldo empresarial de garantía (endpoints, casos de uso, contingencia, tópicos)
- Hallazgos: `garantia-entregas-RESPALDO-002` (S1); cierra `garantia-entregas-SEG-001` (S1),
  `garantia-entregas-DINERO-006` (S2), `garantia-entregas-DOC-001` (S4)
- Causa de raíz: reserva, ampliación, liberación, cobertura, reversa y recuperación del respaldo siguen
  expuestas (`GarantiaController.java:153-188`, `CU23*Respaldo`, `CU23CubrirFaltanteDelCorte`,
  `ContingenciaDeCaja`, `PlanDeContingenciaRepositorio`, `AsientoPropuesto` con `CAJA_EMPRESA`/`RESERVA_RESPALDO`)
  y escriben en el mayor por seis tópicos.
- Corrección de raíz: retirar del OpenAPI (versión y fecha) y del código todas las operaciones
  `/garantia/respaldo/*`, sus casos de uso, la contingencia de caja, el plan de contingencia y las variantes de
  `AsientoPropuesto` del respaldo; dejar de publicar los seis tópicos. Antes, consulta de verificación: si hay
  reservas o coberturas vigentes, generar sus contra-asientos (nunca `UPDATE`); P-14 indica que no hay dinero
  real. Coordinar con L1 el retiro del consumidor de esos tópicos en nucleo-financiero. Reescribir
  `servicios/garantia/README.md:70` y los comentarios de `MayorDeDoble`/`CU133EntregaConsistenteTest`.
  Resuelve SEG-001 (no queda `recuperarRespaldo`) y DINERO-006 (no queda cobertura por turno del respaldo).
- CA: Dado cualquier usuario o servicio, cuando llama una ruta `/garantia/respaldo/*`, entonces no existe; y
  ningún asiento con `CAJA_EMPRESA` o `RESERVA_RESPALDO` puede originarse en garantía.
- DoD: `grep -rnE 'RESERVA_RESPALDO|CAJA_EMPRESA|respaldo_' servicios/garantia/src` vacío; consulta de
  vigentes pegada (0 o contra-asientos listados con su suma); pruebas web 404/410; suites de garantía en verde
  sin `CU131*`/`CU133ContingenciaTest`/`Respaldo*` (retiradas con el código, no saltadas).
- Depende de: F0.L2.M01, F2.L2.M01; retiro del consumidor en nucleo-financiero (lote L1)
- Esfuerzo: grande

### F2.L2.M03 — Retirar el respaldo del modelo y del esquema (tablas, reglas, cuentas 9.x, acreedor PLATAFORMA)
- Hallazgos: `garantia-entregas-RESPALDO-003` (S1), `garantia-entregas-DINERO-012` (S3)
- Causa de raíz: el modelo exige un término «empresa» en la ecuación del pozo (`fondeo_entrega.monto_cubierto_empresa`,
  `ck_fondeo_entrega_pendiente`, R-DES-03), seis tablas `*_respaldo`, R-GAR-08..13, cuentas `9.1.01`/`9.1.02`
  y `deuda_participante.acreedor` con `PLATAFORMA`.
- Corrección de raíz: en la fuente de verdad (`docs/entidades/*.puml`, `docs/Restricciones.md`): quitar las
  seis tablas, `monto_cubierto_empresa` y su CHECK (la ecuación queda pozo = cobrado + cubierto por fondo del
  grupo), R-GAR-08..13, R-DES-03, la verificación de cuadre de la reserva
  (`sql/50_verificacion/verificaciones.sql:118-134`), las cuentas 9.x de `sql/60_semillas/22-cuentas-de-sistema.sql`
  y `PLATAFORMA` del CHECK de acreedor (el acreedor de la deuda subrogada es el fondo del grupo; `AVALISTA`
  sin acción por P-03). Regenerar DDL, expandir/contraer, backup verificado antes de la contracción (97.3.6).
- CA: Dado el esquema aplicado, cuando se busca cualquier objeto de respaldo o la cuenta 9.1.x, entonces no
  existe; y una deuda con acreedor `PLATAFORMA` se rechaza.
- DoD: aplicación del DDL en el entorno Docker (salida literal); verificación de deriva en verde; consultas de
  verificación (huérfanos, cuentas 9.x = 0, CHECK de acreedor) pegadas; backup restaurado de prueba antes de
  la contracción.
- Depende de: F2.L2.M01, F2.L2.M02; nucleo-financiero sin referencias a 9.x (lote L1)
- Esfuerzo: grande

### F2.L2.M04 — Retirar el aval entre participantes
- Hallazgos: cierra `garantia-entregas-DEBIDO-PROCESO-001` (S1) y `garantia-entregas-COHERENCIA-001` (S3)
- Causa de raíz: P-03 elimina el aval; hoy `CU26EjecutarAval` subroga al avalista sin firmeza ni pago y
  `ejecucion_aval` tiene tres catálogos de estado distintos.
- Corrección de raíz: retirar `POST /incumplimientos/{id}/aval/ejecucion`, `CU26EjecutarAval`, el tópico
  `aval_ejecutado`, las tablas `aval_participante` y `ejecucion_aval` del modelo, R-GAR-04, sus pruebas y
  mocks, y las pantallas/textos que ofrezcan avalar. El fondo del grupo (F3.L2.M05) cubre esos casos.
- CA: Dado el sistema, cuando se busca cualquier operación o tabla de aval, entonces no existe.
- DoD: `grep -rniE 'aval' servicios/garantia/src/main sql/` sin objetos vivos; deriva en verde; pruebas web
  404/410 (salida pegada).
- Depende de: F0.L2.M01
- Esfuerzo: medio

### F2.L2.M05 — Estados de entrega y expediente como tipo de dominio con mapeo único
- Hallazgos: `garantia-entregas-FRAGIL-001` (S3)
- Causa de raíz: obsesión por primitivos: `"AUTORIZADA"` repetido en Java, CHECKs y front sin tipo común; quitar
  un estado (lo primero que exige la entrega automática) rompe en silencio.
- Corrección de raíz: enum de dominio `EstadoDeEntrega` / `EstadoDelExpediente` con mapeo único a la base
  (CHECK generado desde el modelo) y al contrato (enum OpenAPI); prueba que compara los tres conjuntos. Se hace
  en F2 como prerequisito de F2.L2.M06.
- CA: Dado un estado agregado o quitado solo en una capa, cuando corren las pruebas, entonces fallan.
- DoD: prueba de coherencia enum↔CHECK↔OpenAPI en verde y en rojo con un estado de más (salida pegada);
  `grep -rn '"AUTORIZADA"' servicios/entregas/src/main` vacío.
- Depende de: ninguna
- Esfuerzo: medio

### F2.L2.M06 — Retirar los pasos humanos de la entrega (autorización cuatro ojos, fondeo y emisión manuales)
- Hallazgos: `garantia-entregas-APROB-001` (S2)
- Causa de raíz: la segregación R-SEG-04 aplicada al pago del pozo: la entrega avanza solo por actos de
  RESPONSABLE_RIESGOS y TESORERIA (`/autorizacion`, `/pozo-completo`, `POST /desembolsos`).
- Corrección de raíz: retirar el estado `AUTORIZADA`, el permiso `ENTREGA_AUTORIZAR`, `ck_entrega_segregacion`
  (modelo → DDL) y los endpoints humanos de fondeo y emisión; las transiciones fondeado → orden emitida →
  acreditada las ejecuta el sistema (F3.L2.M01, F3.L2.M02) con los controles que reemplazan a los cuatro ojos
  (F1.L2.M06, F1.L2.M10, F3.L2.M03). Las ejecuciones fallidas quedan en estado recuperable con alerta, no en una
  bandeja de aprobación.
- CA: Dado un turno completamente cobrado, cuando nadie interviene, entonces la entrega no requiere ninguna
  acción humana para llegar a orden emitida.
- DoD: pruebas web 404/410 de las rutas retiradas; deriva en verde sin `ck_entrega_segregacion`; la prueba de
  extremo a extremo de F3.L2.M01 recorre el camino sin humanos (salida pegada).
- Depende de: F0.L2.M02, F2.L2.M05, F3.L2.M01, F3.L2.M02
- Esfuerzo: grande

### F2.L2.M07 — R-ORG-06: la base deja de exigir confirmación humana para entrega, cobro y cobertura
- Hallazgos: `garantia-entregas-APROB-003` (S2)
- Causa de raíz: el CHECK de `sql/40_reglas/restricciones.sql:2771-2778` obliga `requiere_confirmacion_humana`
  para `PROPONER_ENTREGA/COBRO/COBERTURA/SANCION`, escrito antes de la automatización.
- Corrección de raíz: en `docs/Restricciones.md` y el modelo: la confirmación humana queda obligatoria solo para
  `PROPONER_SANCION` (91.4); entrega, cobro y cobertura sin humano según la ADR de F0.L2.M02. Regenerar DDL.
- CA: Dada una regla de automatización `PROPONER_ENTREGA` sin confirmación humana, cuando se inserta, entonces
  se acepta; y una `PROPONER_SANCION` sin confirmación, entonces se rechaza.
- DoD: aplicación del DDL; dos pruebas de integridad (aceptada/rechazada) contra base real; deriva en verde
  (salida pegada).
- Depende de: F0.L2.M02
- Esfuerzo: chico

### F2.L2.M08 — Retirar el traspaso del cupo por votación del grupo
- Hallazgos: `garantia-entregas-APROB-010` (S3)
- Causa de raíz: la subasta se agregó sin dar de baja la pantalla vieja; la ruta
  `/pasanaku/grupos/:grupoId/cupos/:cupoId/traspaso` y sus textos esperan una votación que el flujo nuevo no
  prevé (aprobación eliminable).
- Corrección de raíz: retirar la ruta, `PantallaTraspaso`, `textos_del_traspaso.dart` y el endpoint de votación
  de traspaso en grupos (coordinar con L3). Si D-L2-1 retira también la subasta, la cesión de turno desaparece
  del producto; si no, la subasta es el único camino.
- CA: Dada la app, cuando se navega a la ruta de traspaso, entonces no existe y ningún texto menciona votación
  del traspaso.
- DoD: prueba de widget/ruta (ruta desconocida → pantalla de no encontrado); `grep -rn 'votaci' apps/movil/lib/pantallas/pasanaku`
  vacío (salida pegada).
- Depende de: D-L2-1 (solo para saber qué queda); lote L3 (endpoint de votación)
- Esfuerzo: medio

### F2.L2.M09 — Retirar el mercado de ofertas de turno (código muerto)
- Hallazgos: `garantia-entregas-REESCRITURA-001` (S3)
- Causa de raíz: cada carril agregó su mecanismo de cesión sin retirar el anterior; el mercado (CU-130) no puede
  operar (`HechosDeGruposPorHttp.turno` devuelve vacío) y nadie lo llama.
- Corrección de raíz: retirar `GestionDeOfertas`, `RegistroDeOfertas`, `ComprarOfertaDeTurno`, sus endpoints
  (versión y fecha), tablas del modelo, pruebas (`CU130*`, `MercadoWebTest`), mocks y clientes. La subasta
  queda sujeta a D-L2-1.
- CA: Dado el servicio de entregas, cuando se busca cualquier operación del mercado de ofertas, entonces no
  existe.
- DoD: `grep -rn 'Oferta' servicios/entregas/src/main` sin el mercado; deriva en verde; suite de entregas en
  verde sin las pruebas retiradas con el código (salida pegada).
- Depende de: ninguna
- Esfuerzo: medio

---

## F3 — Flujo nuevo

### F3.L2.M01 — Entregas consume el evento de turno cobrado y fondea solo (saga idempotente)
- Hallazgos: `garantia-entregas-FLUJO-QR-001` (S2)
- Causa de raíz: entregas fue diseñado como orquestación invocada por Tesorería; no tiene ningún consumidor y el
  fondeo pregunta a aportes por HTTP cuando un humano lo pide.
- Corrección de raíz: consumidor (pacto de F0.L2.M03) del evento «periodo/turno cobrado» que publica aportes
  tras el webhook firmado de la pasarela (P-06: saldo recargado por QR cuenta como QR), con deduplicación
  transaccional por `message_id`; la saga persiste su estado (fondeado → orden emitida → acreditada) con
  timeout por paso y compensación (liberar retención). Si al vencer la gracia hay faltante, espera la cobertura
  del fondo del grupo (F3.L2.M05) en lugar de bloquearse sin dueño.
- CA: Dado el último aporte del turno confirmado por webhook, cuando llega el evento (una o dos veces), entonces
  la entrega queda fondeada una sola vez sin intervención humana.
- DoD: integración con base real y Kafka del entorno Docker: evento procesado → fondeo; mismo evento dos veces
  → una sola entrega (prueba de duplicado); aportes caído durante la consulta → reintento y estado recuperable
  (caída real del contenedor); traza con `correlationId` (salida pegada).
- Depende de: F0.L2.M03, F2.L2.M01; flujo QR → webhook → aporte cobrado (lote L1)
- Esfuerzo: grande

### F3.L2.M02 — Puerto de proveedor de desembolso con doble firmado y webhook de acreditación
- Hallazgos: `garantia-entregas-SEG-005` (S1, segunda parte; la primera es F1.L2.M05)
- Causa de raíz: falta la integración con el proveedor de desembolso; se reemplazó por un formulario humano.
- Corrección de raíz: puerto `ProveedorDeDesembolso` (emitir orden con clave de idempotencia) y endpoint de
  webhook que verifica la firma siempre (también en desarrollo), deduplica por `referenciaProveedor` con índice
  `UNIQUE` y, solo entonces, marca ACREDITADA y publica el asiento por outbox. Doble del proveedor con firma
  real en tres niveles (regla 65, P-15): correcto (firma válida, monto = orden); límite (evento duplicado,
  timestamp al borde de la ventana); inválido (firma inválida, monto distinto, orden inexistente, rechazo del
  banco → compensación que libera la retención).
- CA: Dado un webhook con firma inválida o monto distinto, cuando llega, entonces no cambia el estado ni hay
  asiento; dado uno válido repetido, entonces hay un solo asiento.
- DoD: las tres pruebas de nivel contra el doble con base real; prueba de duplicado (mismo evento dos veces →
  un asiento, consulta de cuadre pegada); asiento generado con debe/haber y suma (91.6); se declara que quedó
  verificado contra un doble.
- Depende de: F1.L2.M05, F1.L2.M08, F1.L2.M10
- Esfuerzo: grande

### F3.L2.M03 — Verificación de la cuenta destino por un proveedor, no autodeclarada
- Hallazgos: `garantia-entregas-SEG-008` (S2)
- Causa de raíz: `CU18RegistrarCuentaDestino.verificar` deja que el dueño marque su cuenta como verificada con
  un método de texto libre; la verificación real nunca se implementó.
- Corrección de raíz: retirar `POST /cuentas-bancarias/{id}/verificacion` del dueño; puerto
  `VerificadorDeTitularidad` (consulta de titularidad o microdepósito) cuyo resultado firmado marca VERIFICADA,
  comparando el titular con la identidad KYC del usuario (P-08). Doble en tres niveles (titular coincide; nombre
  con diferencias de formato; titular distinto / firma inválida / proveedor caído).
- CA: Dado un usuario, cuando intenta verificar su propia cuenta por API, entonces no puede; y solo un resultado
  firmado del verificador con titular coincidente deja la cuenta VERIFICADA.
- DoD: pruebas de los tres niveles contra el doble con base real; prueba web del retiro; el enfriamiento corre
  desde la verificación (salida pegada).
- Depende de: puerto KYC (P-08, lote correspondiente)
- Esfuerzo: grande

### F3.L2.M04 — Fondo del grupo como cuentas segregadas del mayor (modelo A), sin saldo cacheado
- Hallazgos: `garantia-entregas-HUECO-001` (S2), `garantia-entregas-DINERO-005` (S2), `garantia-entregas-DINERO-013` (S3)
- Causa de raíz: el saldo del fondo vive en `garantia.fondo_garantia` (UPDATE de `saldo_disponible`/`comprometido`)
  y no en el mayor; cobertura, ejecución y devolución publican eventos sin consumidor, y no hay cuadre. Es el
  mismo error en tres síntomas.
- Corrección de raíz: rediseñar sobre A: el fondo del grupo es una cuenta del mayor por grupo (segregada, nunca
  de la empresa); cada movimiento (aporte al fondo, cobertura, recuperación, devolución, reparto de
  incobrables) lo propone garantía como asiento por outbox y lo registra nucleo-financiero; los saldos y el
  tablero de transparencia se leen del mayor. Retirar las columnas de saldo de `fondo_garantia` (modelo →
  DDL) y las sumas ad-hoc de `IndicadoresDeGarantiaRepositorio`/`GestionRepositorio.deudaViva`. Agregar a
  `sql/50_verificacion` el cuadre del fondo contra su libro en el mayor.
- CA: Dado cualquier movimiento del fondo, cuando se consulta el saldo, entonces coincide con el mayor y la
  verificación de cuadre pasa; y no existe ninguna columna de saldo del fondo fuera del mayor.
- DoD: integración de punta a punta (garantía → outbox → nucleo-financiero) con asientos pegados y su suma;
  verificación de cuadre en verde y en rojo con un movimiento inyectado sin asiento; deriva en verde.
- Depende de: F0.L2.M01, F2.L2.M03; validación de Cumplimiento (cuenta segregada ≠ captación) antes de
  producción; cuentas del fondo en nucleo-financiero (lote L1)
- Esfuerzo: grande

### F3.L2.M05 — Cobertura automática del faltante con el fondo del grupo y subrogación de la deuda
- Hallazgos: `garantia-entregas-FLUJO-FALTANTE-001` (S2)
- Causa de raíz: sin decisión sobre el faltante (A-2), la entrega quedaba BLOQUEADA con el pozo completo como
  principal, una incidencia con SLA sintético y el pendiente sin participante deudor.
- Corrección de raíz: al vencer el plazo de gracia (parámetro obligatorio, F3.L2.M14) con cuota impaga, el
  sistema cubre la cuota con el fondo del grupo (asiento F3.L2.M04), crea la deuda del participante con
  acreedor «fondo del grupo» (subrogación) con importe y moneda leídos de aportes, y publica el evento que
  destraba la entrega (F3.L2.M01). Unicidad por (grupo, periodo, participante) y clave por `message_id`. Si el
  fondo no alcanza, la entrega queda en espera explícita con estado visible (98.4.1), sin dinero de la empresa.
  Lo que el deudor paga después (por QR) vuelve al fondo. Retirar la incidencia `FONDO_INCOMPLETO` con SLA
  sintético.
- CA: Dado un participante que no pagó al vencer la gracia, cuando corre el vencimiento, entonces la entrega del
  turno sale completa con dinero del fondo del grupo y existe una deuda a su nombre por ese importe; repetir el
  vencimiento no cubre dos veces.
- DoD: integración con base real: cobertura + deuda + asiento (pegado, debe = haber); prueba de duplicado;
  fondo insuficiente → espera visible sin asiento de empresa; pago posterior del deudor → abono al fondo y
  deuda saldada (salida pegada).
- Depende de: F3.L2.M04, F3.L2.M01, F3.L2.M14; validación de Cumplimiento (cobertura y subrogación sin
  expediente previo)
- Esfuerzo: grande

### F3.L2.M06 — Devolución del fondo al cierre del ciclo con reparto de incobrables y regla de residuo
- Hallazgos: `garantia-entregas-DINERO-008` (S3); completa el rediseño de `SEG-004` (F1.L2.M03)
- Causa de raíz: `DevolucionDelFondo` topa al último aportante y no asigna el residuo; no existe la regla del
  modelo A (devolver lo aportado menos la parte igual de lo incobrable).
- Corrección de raíz: reescribir el reparto: a cada participante, lo que aportó al fondo menos
  (incobrable / n) en partes iguales; aritmética decimal exacta con modo de redondeo declarado y residuo
  asignado por regla determinista (propuesta: al participante de menor orden de turno, a aprobar por Pablo,
  D-L2-3), registrado como línea del reparto. Disparado por el evento de cierre del ciclo de grupos, no por un
  humano; cada devolución es un asiento.
- CA: Dado un fondo con lo aportado y un incobrable que no divide exacto, cuando se cierra el ciclo, entonces la
  suma de devoluciones + incobrables absorbidos = saldo del fondo, sin centavo sin asignar.
- DoD: pruebas unitarias de reparto (exacto, con residuo, incobrable mayor que lo aportado por alguien) y de
  integración con asientos y cuadre pegados; mismo evento de cierre dos veces → una devolución.
- Depende de: F3.L2.M04, F1.L2.M03; D-L2-3
- Esfuerzo: medio

### F3.L2.M07 — Recargo por mora acreditado al perjudicado, con DLT y reproceso
- Hallazgos: `garantia-entregas-RESILIENCIA-001` (S2)
- Causa de raíz: el recargo se anota en el fondo (`IngresoAlFondo`) y, si no encuentra fondo, el mensaje se
  reintenta 10 s y se descarta: delega en un reproceso que no existe y no hay DLT.
- Corrección de raíz: por P-05 el recargo va al perjudicado (quien esperó su turno), no al fondo ni a la
  empresa: el consumidor calcula el recargo con la tasa parametrizada (F3.L2.M14) y propone el asiento hacia la
  billetera del perjudicado. Manejador con DLT, alerta y trabajo de reproceso idempotente por `message_id` en
  `servicios/garantia/.../trabajos/` con ShedLock.
- CA: Dado un aporte atrasado confirmado, cuando se procesa (una o dos veces), entonces el perjudicado recibe el
  recargo una sola vez; y un mensaje que falla queda en la DLT y se reprocesa sin duplicar.
- DoD: integración con Kafka real: asiento del recargo pegado; duplicado → un asiento; fallo forzado → mensaje
  en DLT → reproceso → un asiento (salida pegada).
- Depende de: F3.L2.M14; definición de «perjudicado» en el modelo (ADR de F0.L2.M01)
- Esfuerzo: medio

### F3.L2.M08 — Incumplimiento declarado por el sistema; restricción con firmeza; descargo y apelación
- Hallazgos: `garantia-entregas-DEBIDO-PROCESO-002` (S2)
- Causa de raíz: la apelación existe en el modelo (`apelacion_sancion`) sin caso de uso ni endpoint, y
  `CU27RestringirDeudor` no exige firmeza; la declaración era un acto del organizador.
- Corrección de raíz: por P-12, el sistema declara el incumplimiento al vencer la gracia (consumidor/trabajo,
  sin endpoint humano de declaración); el descargo lo resuelve SOPORTE/RIESGOS y la apelación otra persona
  (identidades tomadas del token, distintas, regla de objeto en la escritura); la restricción y cualquier
  sanción exigen expediente `FIRME` en la escritura. Implementar `POST /incumplimientos/{id}/apelacion` y su
  resolución en `garantia.yaml`.
- CA: Dado un expediente no firme, cuando se intenta restringir o sancionar, entonces se rechaza; y dada una
  resolución de descargo, cuando la misma persona intenta resolver la apelación, entonces se rechaza.
- DoD: integración con base real de la máquina de estados del expediente (declarado → descargo → firme →
  apelación); matriz negativa de autorización (mismo resolutor, imputado, rol insuficiente); salida pegada.
- Depende de: F1.L2.M01, F2.L2.M05
- Esfuerzo: grande

### F3.L2.M09 — El titular ve su expediente y su deuda (RLS) y las pruebas corren con el rol real
- Hallazgos: `garantia-entregas-DEFECTO-002` (S2)
- Causa de raíz: `registro_incumplimiento` y `deuda_participante` no están en `visibles_por_titular`; la RLS
  niega por omisión al imputado y las pruebas corren con el rol dueño, que salta la RLS.
- Corrección de raíz: declarar en `docs/Restricciones.md` la visibilidad por titular de las tablas de
  expediente y deuda (y para SOPORTE/RIESGOS por rol), regenerar las políticas, y hacer que
  `BaseDeGarantia` use `SET ROLE svc_garantia` con contexto de sesión real. `consultarRestriccion` falla
  cerrado si no puede leer.
- CA: Dado un participante imputado, cuando consulta su expediente o presenta descargo, entonces lo ve; y otro
  participante no lo ve.
- DoD: integración con `SET ROLE svc_garantia` y contexto PARTICIPANTE: titular ve, ajeno no, error de lectura
  → restricción vigente (falla cerrada) (salida pegada).
- Depende de: F3.L2.M08
- Esfuerzo: medio

### F3.L2.M10 — La reputación negativa solo nace de un expediente firme
- Hallazgos: `garantia-entregas-DEBIDO-PROCESO-003` (S2)
- Causa de raíz: la semilla `sql/60_semillas/18-sanciones-y-cobranza.sql:16` define `APORTE_ATRASADO` con
  afectación de reputación automática y sin revisión humana.
- Corrección de raíz: separar la calificación por hechos (pieza D: puntualidad calculada de pagos confirmados
  por QR, sobre `transparencia`) de la reputación negativa, que solo se escribe como consecuencia de un
  expediente `FIRME` (91.4.2). Quitar de la matriz toda acción automática de afectación de reputación; los
  parámetros que queden se cargan como configuración aprobada (F3.L2.M14), no como semilla inferida.
- CA: Dado un aporte atrasado sin expediente firme, cuando se procesa, entonces la reputación negativa no cambia
  (la puntualidad sí refleja el hecho).
- DoD: integración: atraso → sin evento de reputación negativa; expediente firme → evento; semilla re-corrida
  idempotente (salida pegada).
- Depende de: F3.L2.M08; pieza D en `transparencia` (lote correspondiente)
- Esfuerzo: medio

### F3.L2.M11 — La restricción se levanta al confirmarse el pago y la pantalla ofrece pagar por QR
- Hallazgos: `garantia-entregas-UX-A11Y-010` (S3)
- Causa de raíz: el levantamiento es un acto humano con motivo (`levantarRestriccion`, `GRUPO_ADMINISTRAR`) y
  la pantalla promete «Se levanta pagando» sin forma de pagar.
- Corrección de raíz: garantía consume el evento de pago confirmado (pacto F0.L2.M03) y levanta la restricción
  cuando la deuda queda saldada; retirar el endpoint humano de levantamiento. En `pantalla_mi_estado.dart`,
  botón que genera la orden de cobro QR por el importe de la deuda (con moneda del contrato, F4.L2.M02) y
  estado «procesando» hasta el evento.
- CA: Dada una persona restringida, cuando paga por QR y el webhook se confirma, entonces la restricción se
  levanta sin intervención humana y la pantalla lo refleja.
- DoD: integración del consumidor (evento → restricción levantada; duplicado → idempotente); prueba de widget
  de los estados (con deuda, procesando, levantada, error); captura en móvil/tablet y ambos temas (95.7).
- Depende de: F3.L2.M05, F4.L2.M02; orden de cobro QR (lote L1)
- Esfuerzo: medio

### F3.L2.M12 — Textos de la web pública y del tour de la app según el modelo A
- Hallazgos: `garantia-entregas-RESPALDO-010` (S2)
- Causa de raíz: los textos de marketing se escribieron contra ADR-051 y prometen que «el fondo cubre» sin
  decir de quién es el fondo; no tienen dueño que los revalide.
- Corrección de raíz: reescribir `cierre.ts:71`, `producto.ts:75`, `transparencia.ts:65`, `hero.ts:31`,
  `app.routes.ts:117` (JSON-LD) y `textos.dart:42-46`, `laminas_del_tour.dart:45-46` para que digan lo que
  pasa: el fondo es del grupo, lo aportan todos, cubre la cuota de quien no paga, esa persona queda debiendo al
  fondo, y al cierre cada uno recupera lo aportado (menos lo incobrable en partes iguales). P-16: los redacta
  Claude y los aprueba Pablo antes de subir.
- CA: Dada la web y la app, cuando se buscan los textos de garantía, entonces ninguno afirma que la empresa
  cubra, y todos coinciden con la ADR de F0.L2.M01.
- DoD: `grep -rn 'cubre' apps/web/src apps/movil/lib` revisado línea a línea contra la ADR; aprobación de Pablo
  en el PR; capturas de las secciones en tres viewports.
- Depende de: F0.L2.M01; P-16
- Esfuerzo: chico

### F3.L2.M13 — Indicadores y textos de backoffice y catálogos de diseño según el modelo A
- Hallazgos: `garantia-entregas-DOC-010` (S3)
- Causa de raíz: indicadores («Impagos cubiertos», «Recuperación de lo cubierto», «Cobertura del fondo») y
  ayudas escritos contra ADR-051.
- Corrección de raíz: renombrar y redefinir los indicadores de `sql/60_semillas/23-indicadores-tableros.sql:16-19`,
  `catalogo-de-tableros.ts`, `explicaciones-tablero.ts`, `explicaciones-operacion.ts` y las muestras de
  `packages/ui`/`packages/diseno_flutter` sobre las cuentas del fondo del grupo (F3.L2.M04); quitar la ayuda de
  doble aprobación de retiros si el lote L1/L4 la retira (P-09). Textos aprobados por Pablo (P-16).
- CA: Dado el backoffice, cuando se abre el tablero de garantía, entonces cada indicador se calcula desde el
  mayor del fondo del grupo y ningún texto menciona cobertura de la empresa.
- DoD: prueba del cálculo de cada indicador contra datos sintéticos del mayor; captura del tablero; semilla
  re-corrida idempotente (salida pegada).
- Depende de: F3.L2.M04; P-16
- Esfuerzo: chico

### F3.L2.M14 — Parámetros financieros configurables y obligatorios, sin valor por defecto ni semilla inferida
- Hallazgos: `garantia-entregas-PARCHE-001` (S3), `garantia-entregas-DATOS-002` (S2)
- Causa de raíz: parámetros puestos para poder arrancar, sin dueño de negocio: valores «SINTETICOS» y
  `factor-fijo` «SOLO TEST» en el `application.yml` principal, y topes/recargos/plazos/quitas sembrados sin
  procedencia ni moneda (`02-politicas.sql:24-30`, `18-sanciones-y-cobranza.sql:49-56`).
- Corrección de raíz: un registro de parámetros del modelo A + D (porcentaje del aporte al fondo, días de
  gracia, tasa de recargo, beneficios por nivel) con moneda, procedencia (quién lo aprobó y cuándo) y vigencia;
  el servicio valida al arrancar y **no arranca** si falta alguno. Quitar las semillas inferidas (las de
  cobertura con umbral se retiran con F1.L2.M02) y mover `factor-fijo` y los valores sintéticos a un perfil de
  prueba. La credencial de servicio de usuario se retira con F1.L2.M14. Los valores reales los fijan Pablo y
  Cumplimiento.
- CA: Dado un arranque sin el parámetro de días de gracia (o cualquier otro), cuando se inicia garantía o
  entregas, entonces el arranque falla con un mensaje que nombra el parámetro.
- DoD: prueba de arranque por parámetro faltante (rojo) y completo (verde); `grep -rn 'SINTETICO\|SOLO TEST' servicios/*/src/main/resources`
  vacío; semilla sin parámetros financieros (salida pegada).
- Depende de: decisión de valores por Pablo y Cumplimiento (no bloquea la construcción)
- Esfuerzo: medio

---

## F4 — Calidad y deuda

### F4.L2.M01 — Moneda ISO-4217 junto a cada importe del modelo de garantía
- Hallazgos: `garantia-entregas-DINERO-004` (S2)
- Causa de raíz: modelo anterior a la regla 91.1.2; muchas tablas de garantía y `entregas.deduccion_entrega`
  llevan importes sin moneda y el código fija BOB.
- Corrección de raíz: en el modelo `.puml`, `moneda CHAR(3)` en cada tabla con importe que sobrevive al
  rediseño (las de respaldo, aval y cobertura manual se retiran en F2/F1); jOOQ regenerado; el código toma la
  moneda del agregado y elimina los `BOB` fijos de `ExpedienteRepositorio`, `GestionRepositorio`,
  `IndicadoresDeGarantia`. Expandir (columna con backfill desde el grupo) y luego contraer (`NOT NULL`).
- CA: Dado un grupo en USD, cuando se registra y relee una deuda o un movimiento, entonces conserva USD y
  ninguna suma mezcla monedas.
- DoD: deriva en verde; integración con grupo USD de punta a punta; `grep -rn '"BOB"' servicios/garantia/src/main`
  vacío (salida pegada).
- Depende de: F2.L2.M03, F2.L2.M04, F3.L2.M04
- Esfuerzo: grande

### F4.L2.M02 — `RestriccionVigente.montoQueLaLevanta` como `Dinero` con moneda
- Hallazgos: `garantia-entregas-DINERO-010` (S2)
- Causa de raíz: el contrato usa un string suelto (`garantia.yaml:689-696`) y la app parchea con `'BOB'`.
- Corrección de raíz: `montoQueLaLevanta: $ref Dinero` en una versión nueva del contrato, cliente Dart
  regenerado, y quitar el literal `'BOB'` de `pantalla_mi_estado.dart:91-93`.
- CA: Dada una restricción en USD, cuando la app la muestra, entonces muestra USD.
- DoD: prueba de contrato del campo; prueba de widget con USD y BOB (salida pegada).
- Depende de: F4.L2.M01
- Esfuerzo: chico

### F4.L2.M03 — Subasta y puja envían la moneda del agregado
- Hallazgos: `garantia-entregas-DINERO-011` (S3)
- Causa de raíz: moneda constante `DineroMonedaEnum.BOB` en el cliente (`subasta_del_cupo.dart:82,125`,
  formularios de subasta y puja).
- Corrección de raíz: tomar la moneda de `montoMinimo`/`montoAporte`; en el servicio, rechazar puja con moneda
  distinta de la subasta (verificar y agregar si falta). Si D-L2-1 retira la subasta, se resuelve con ese retiro.
- CA: Dada una subasta en USD, cuando se puja, entonces se envía USD; y una puja en otra moneda se rechaza.
- DoD: prueba de widget del envío; prueba de integración del rechazo por moneda (salida pegada).
- Depende de: D-L2-1
- Esfuerzo: chico

### F4.L2.M04 — Claves foráneas entre esquemas: ADR que resuelva ADR-017 vs regla 98.1 y su aplicación
- Hallazgos: `garantia-entregas-ACOPLE-001` (S2)
- Causa de raíz: ADR-017 («se parte el despliegue, no el modelo») conserva 76 + 25 FK cruzadas y un trigger que
  lee `cuenta_billetera` desde garantía, en contradicción con 98.1.1-2.
- Corrección de raíz: ADR que decida cuál manda (propuesta: 98.1); si manda 98.1, reemplazar las FK cruzadas
  por referencias lógicas validadas por API/evento, mover `fn_grp_validar_disolucion` fuera de la lectura
  cruzada y que las fixturas de prueba no escriban en esquemas ajenos. Se hace después de F2, porque el retiro
  del respaldo y del aval elimina parte de esas FK.
- CA: Dado el esquema de garantía y entregas, cuando se listan sus FK, entonces ninguna apunta a otro esquema
  (si la ADR adopta 98.1).
- DoD: consulta a `information_schema` pegada (0 FK cruzadas); `SET ROLE svc_garantia` sin lecturas cruzadas;
  deriva en verde.
- Depende de: F2.L2.M03, F2.L2.M04; decisión de arquitectura de Pablo
- Esfuerzo: grande

### F4.L2.M05 — Bitácora de lectura de datos financieros transversal
- Hallazgos: `garantia-entregas-TRAZA-001` (S2)
- Causa de raíz: `registro_acceso_datos` existe (lo usa auditoría) pero ningún mecanismo transversal lo
  alimenta desde garantía y entregas.
- Corrección de raíz: anotación/filtro en `plataforma` para endpoints marcados sensibles (restricciones, deuda,
  cuentas de cobro, desembolsos) que publica la lectura (actor, recurso, `correlationId`, sin contenido) hacia
  auditoría por outbox; aplicarlo en ambos servicios.
- CA: Dada una lectura de la deuda de una persona, cuando ocurre, entonces queda un registro de lectura con
  actor y recurso, separado de la bitácora de escritura.
- DoD: integración: lectura → evento → fila en `registro_acceso_datos`; contenido sin PII (salida pegada).
- Depende de: ninguna (coordinar con el lote de auditoría/plataforma)
- Esfuerzo: medio

### F4.L2.M06 — Logs de entregas solo con identificadores
- Hallazgos: `garantia-entregas-OBSERV-001` (S3)
- Causa de raíz: la bitácora de casos de uso registra beneficiario y montos atribuibles
  (`CU22LiquidarEntrega.java:105,148,189`, `CU28EmitirDesembolso.java:138`).
- Corrección de raíz: loguear solo `entregaId`, `ordenId`, `grupoId`, `correlationId`; prueba de arquitectura
  que impida pasar `Dinero` o identificadores de persona a la bitácora.
- CA: Dada una entrega procesada, cuando se inspecciona el log, entonces no contiene importe ni beneficiario.
- DoD: prueba con capturador de logs sobre el flujo de entrega (sin importe/beneficiario) (salida pegada).
- Depende de: F1.L2.M08, F1.L2.M09 (líneas que sobreviven)
- Esfuerzo: chico

### F4.L2.M07 — Cierre de subastas con ShedLock y recuperación de pasos colgados
- Hallazgos: `garantia-entregas-RESILIENCIA-002` (S2)
- Causa de raíz: `CierreDeSubastas` está programado sin `@SchedulerLock` y la saga retener → fijar puja no
  tiene recuperación de pendientes.
- Corrección de raíz: `@SchedulerLock` en el cierre y un trabajo de recuperación de PENDIENTES (con token de
  servicio) que libera o fija la retención según el estado persistido. Si D-L2-1 retira la subasta, se resuelve
  con ese retiro.
- CA: Dadas dos réplicas, cuando coincide el horario, entonces una sola cierra; y una puja con retención sin
  fijar se recupera.
- DoD: integración con dos instancias del trabajo contra la misma base (un cierre); caída simulada entre
  retener y fijar → recuperación (salida pegada).
- Depende de: D-L2-1, F1.L2.M14
- Esfuerzo: medio

### F4.L2.M08 — `registro_incumplimiento` sin columnas mutables en una tabla append-only
- Hallazgos: `garantia-entregas-DATOS-003` (S3)
- Causa de raíz: dos diseños del expediente conviviendo: fila con `estado`, `version`, `cerrado_en`,
  `monto_recuperado` y, a la vez, append-only con el estado derivado del historial.
- Corrección de raíz: quitar esas columnas del modelo y exponer el estado actual como vista calculada desde el
  historial (coherente con F3.L2.M08).
- CA: Dado un expediente cerrado, cuando se consulta, entonces su estado y fecha de cierre salen de la vista y
  la tabla base no tiene columnas que nunca cambian.
- DoD: deriva en verde; prueba de la vista con historial de varios estados (salida pegada).
- Depende de: F3.L2.M08
- Esfuerzo: chico

### F4.L2.M09 — `deduccion_entrega` y `fondeo_entrega` append-only
- Hallazgos: `garantia-entregas-DATOS-004` (S3)
- Causa de raíz: las tablas de entregas no se incluyeron en el sellado append-only y `FondeoRepositorio` hace
  UPDATE.
- Corrección de raíz: incluirlas en `sql/35_append_only` (revocar UPDATE/DELETE al rol de la aplicación) y
  modelar los cambios del fondeo como filas de historial; correcciones por línea inversa.
- CA: Dada una deducción registrada, cuando el rol de la aplicación intenta modificarla, entonces la base lo
  rechaza.
- DoD: integración con `SET ROLE svc_entregas`: UPDATE/DELETE → error de permiso; fondeo con historial (salida
  pegada).
- Depende de: F3.L2.M01, F3.L2.M05 (forma final del fondeo)
- Esfuerzo: medio

### F4.L2.M10 — Pruebas reescritas contra el flujo nuevo con matriz negativa por endpoint
- Hallazgos: `garantia-entregas-PRUEBAS-001` (S3)
- Causa de raíz: las pruebas se escribieron contra ADR-051 y solo con matriz de rol; no detectan BOLA.
- Corrección de raíz: las pruebas del respaldo y de las aprobaciones se retiran con su código (F2); las que
  sobreviven (`CU23Test`, `CU66Test`, `SeguridadWebTest` de ambos servicios) se reescriben contra el modelo A y
  la entrega automática, y cada endpoint vivo recibe su matriz negativa (otro usuario, otro grupo, rol
  insuficiente, sin token) (80.2.3). `scripts/matriz_de_pruebas.py` en verde para ambos servicios.
- CA: Dado cualquier endpoint vivo de garantía o entregas, cuando se corre el verificador de matriz, entonces
  tiene prueba web y prueba de autorización negativa.
- DoD: `python3 scripts/matriz_de_pruebas.py --servicio entregas` y `--servicio garantia` sin faltantes; suites
  en verde (salida pegada).
- Depende de: F1, F2 y F3 de este lote
- Esfuerzo: grande

### F4.L2.M11 — Mocks de `packages/simulado` generados desde el OpenAPI con los tres niveles
- Hallazgos: `garantia-entregas-PRUEBAS-010` (S3)
- Causa de raíz: mocks por plantilla: escenario adverso idéntico al correcto, códigos de error ajenos, 202/503
  no declarados.
- Corrección de raíz: generador que produce, por operación, los escenarios correcto, límite e inválido con los
  códigos declarados en el contrato; regenerar los de garantía y entregas (después de los retiros de F2) y
  fallar el build si un mock emite un código no declarado.
- CA: Dado un mock, cuando su código o su error no figura en el contrato, entonces el verificador falla.
- DoD: verificador en verde y en rojo con un mock alterado (salida pegada).
- Depende de: F2 de este lote
- Esfuerzo: medio

### F4.L2.M12 — `ejecutarReemplazo` sin 500 por montos nulos
- Hallazgos: `garantia-entregas-DEFECTO-001` (S3)
- Causa de raíz: el caso de uso devuelve montos `null` y `MapeoDeGarantia.dinero` asume no nulos; la prueba web
  mockea el caso de uso.
- Corrección de raíz: la salida del caso de uso no lleva nulos (montos reales o tipo opcional en el contrato) y
  prueba de integración del endpoint sin mock. Si D-L2-2 retira el reemplazo, se resuelve con ese retiro.
- CA: Dado un reemplazo ejecutado, cuando se llama el endpoint, entonces responde 200 con el cuerpo del contrato.
- DoD: integración del endpoint contra base real (salida pegada).
- Depende de: D-L2-2
- Esfuerzo: chico

### F4.L2.M13 — La app consume la paginación de `listarSubastasDeCupo`
- Hallazgos: `garantia-entregas-CONTRATO-010` (S4)
- Causa de raíz: el provider no usa el cursor.
- Corrección de raíz: paginación por cursor en `subasta_del_cupo.dart` con carga incremental y estados de
  carga/vacío/error. Si D-L2-1 retira la subasta, se resuelve con ese retiro.
- CA: Dadas más subastas que una página, cuando se desplaza la lista, entonces aparecen todas.
- DoD: prueba de widget con dos páginas del doble (salida pegada).
- Depende de: D-L2-1
- Esfuerzo: chico

### F4.L2.M14 — Un texto por estado de puja; «ya volvió» solo con LIBERADA
- Hallazgos: `garantia-entregas-UX-A11Y-011` (S3)
- Causa de raíz: mapeo incompleto: SUPERADA y LIBERADA comparten «tu saldo ya volvió»; GANADORA, RECHAZADA y
  PENDIENTE sin representación.
- Corrección de raíz: mapeo exhaustivo (switch sobre el enum generado) en `pantalla_subasta.dart` con un texto
  por estado; afirmar devolución solo con LIBERADA. Si D-L2-1 retira la subasta, se resuelve con ese retiro.
- CA: Dada una puja en cada estado del contrato, cuando se muestra, entonces el texto corresponde y SUPERADA no
  afirma devolución.
- DoD: prueba de widget por estado; capturas (salida pegada).
- Depende de: D-L2-1
- Esfuerzo: chico

### F4.L2.M15 — Errores del servidor de monto asociados al campo en subasta y puja
- Hallazgos: `garantia-entregas-UX-A11Y-012` (S3)
- Causa de raíz: mapeo de error genérico: AP-CU136-03/04/07 se pintan como alerta sobre el botón y
  `CampoMonto.error` no se usa.
- Corrección de raíz: mapear los códigos de monto a `CampoMonto.error` con vínculo accesible (Semantics). Si
  D-L2-1 retira la subasta, se resuelve con ese retiro.
- CA: Dado un rechazo por monto, cuando vuelve del servidor, entonces el campo queda señalado con el mensaje
  vinculado y lo escrito se conserva.
- DoD: prueba de widget por código; verificación con lector de pantalla del vínculo (salida pegada).
- Depende de: D-L2-1
- Esfuerzo: chico

### F4.L2.M16 — Tokens en la bandeja de desembolsos
- Hallazgos: `garantia-entregas-UX-A11Y-013` (S4)
- Causa de raíz: estilos locales con valores literales (`10ch`, `.92em`, `.875rem`).
- Corrección de raíz: usar tokens de `packages/tokens` en `bandeja-de-desembolsos.ts` e `importe-sin-moneda.ts`
  (y revisar si `importe-sin-moneda` sigue teniendo sentido con F4.L2.M01).
- CA: Dados los dos componentes, cuando se buscan literales de tipografía o tamaño, entonces no hay.
- DoD: lint de estilos/grep sin literales; capturas en tres viewports y ambos temas (salida pegada).
- Depende de: ninguna
- Esfuerzo: chico

### F4.L2.M17 — Código y textos muertos; superficie de la entrega para quien cobra
- Hallazgos: `garantia-entregas-CLEAN-001` (S4)
- Causa de raíz: funcionalidad empezada y no conectada: `EstadoDelExpediente.admiteTransicion` sin uso y textos
  de entrega de la app sin pantalla; quien cobra no ve la explicación línea a línea de su entrega (91.3.4).
- Corrección de raíz: usar `admiteTransicion` como precondición de escritura del expediente (F3.L2.M08) o
  borrarlo; reemplazar los textos sin uso por la vista de la entrega del turno (bruto, cobertura del fondo,
  deducciones línea a línea, neto, estado) leída del contrato existente.
- CA: Dado un beneficiario, cuando su entrega está en curso o acreditada, entonces ve su detalle línea a línea;
  y no queda código sin referencias.
- DoD: `grep -rn admiteTransicion` con uso real o sin resultados; prueba de widget de la vista con estados de
  carga/vacío/error; capturas (salida pegada).
- Depende de: F3.L2.M01, F3.L2.M08
- Esfuerzo: chico

### F4.L2.M18 — La ruta de «mi estado» resuelve el usuario por la sesión
- Hallazgos: `garantia-entregas-SEG-010` (S4)
- Causa de raíz: la ruta móvil `/pasanaku/mi-estado?usuario=…&participante=…` pasa identificadores en la query
  (el servidor sí protege).
- Corrección de raíz: quitar `usuario` de la query y resolver el usuario de la sesión; el participante se
  obtiene del grupo/cupo seleccionado.
- CA: Dada la app, cuando se navega a «mi estado», entonces la URL no contiene el id del usuario.
- DoD: prueba de ruta/widget; `grep -n 'usuario=' apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart`
  vacío (salida pegada).
- Depende de: ninguna
- Esfuerzo: chico

---

## F5 — Piloto «Poder de compra»

Ningún hallazgo de este informe toca algo reutilizable para el piloto. Sin microtareas.

---

## Trazabilidad hallazgo → microtarea

| # | Hallazgo | Sev. | Microtarea | Estado de la fila |
|---:|---|---|---|---|
| 1 | garantia-entregas-APROB-001 | S2 | F2.L2.M06 | microtarea propia |
| 2 | garantia-entregas-APROB-002 | S2 | F1.L2.M02 | agrupada con SEG-003 |
| 3 | garantia-entregas-APROB-003 | S2 | F2.L2.M07 | microtarea propia (ADR en F0.L2.M02) |
| 4 | garantia-entregas-APROB-010 | S3 | F2.L2.M08 | microtarea propia |
| 5 | garantia-entregas-RESPALDO-001 | S1 | F2.L2.M01 | microtarea propia |
| 6 | garantia-entregas-RESPALDO-002 | S1 | F2.L2.M02 | microtarea propia |
| 7 | garantia-entregas-RESPALDO-003 | S1 | F2.L2.M03 | microtarea propia |
| 8 | garantia-entregas-RESPALDO-010 | S2 | F3.L2.M12 | microtarea propia |
| 9 | garantia-entregas-FLUJO-QR-001 | S2 | F3.L2.M01 | microtarea propia |
| 10 | garantia-entregas-FLUJO-FALTANTE-001 | S2 | F3.L2.M05 | microtarea propia |
| 11 | garantia-entregas-COHERENCIA-001 | S3 | F2.L2.M04 | se resuelve con F2.L2.M04 |
| 12 | garantia-entregas-DUP-X-001 | S3 | F1.L2.M13 | microtarea propia |
| 13 | garantia-entregas-REESCRITURA-001 | S3 | F2.L2.M09 | microtarea propia |
| 14 | garantia-entregas-PISADA-001 | S4 | F1.L2.M09 | se resuelve con F1.L2.M09 |
| 15 | garantia-entregas-PARCHE-001 | S3 | F3.L2.M14 | agrupada con DATOS-002 |
| 16 | garantia-entregas-FRAGIL-001 | S3 | F2.L2.M05 | microtarea propia |
| 17 | garantia-entregas-DEFECTO-002 | S2 | F3.L2.M09 | microtarea propia |
| 18 | garantia-entregas-DEFECTO-001 | S3 | F4.L2.M12 | microtarea propia (condicionada D-L2-2) |
| 19 | garantia-entregas-HUECO-001 | S2 | F3.L2.M04 | agrupada con DINERO-005 y DINERO-013 |
| 20 | garantia-entregas-RESILIENCIA-001 | S2 | F3.L2.M07 | microtarea propia |
| 21 | garantia-entregas-RESILIENCIA-002 | S2 | F4.L2.M07 | microtarea propia (condicionada D-L2-1) |
| 22 | garantia-entregas-DINERO-001 | S1 | F1.L2.M08 | microtarea propia |
| 23 | garantia-entregas-DINERO-003 | S1 | F1.L2.M09 | microtarea propia |
| 24 | garantia-entregas-DINERO-002 | S2 | F1.L2.M10 | microtarea propia |
| 25 | garantia-entregas-DINERO-004 | S2 | F4.L2.M01 | microtarea propia |
| 26 | garantia-entregas-DINERO-005 | S2 | F3.L2.M04 | agrupada con HUECO-001 |
| 27 | garantia-entregas-DINERO-006 | S2 | F2.L2.M02 | se resuelve con F2.L2.M02 |
| 28 | garantia-entregas-DINERO-007 | S2 | F1.L2.M04 | microtarea propia |
| 29 | garantia-entregas-DINERO-009 | S2 | F1.L2.M11 | microtarea propia |
| 30 | garantia-entregas-DINERO-010 | S2 | F4.L2.M02 | microtarea propia |
| 31 | garantia-entregas-DINERO-008 | S3 | F3.L2.M06 | microtarea propia |
| 32 | garantia-entregas-DINERO-011 | S3 | F4.L2.M03 | microtarea propia (condicionada D-L2-1) |
| 33 | garantia-entregas-DINERO-012 | S3 | F2.L2.M03 | agrupada con RESPALDO-003 (parte AVALISTA: sin acción, P-03) |
| 34 | garantia-entregas-DINERO-013 | S3 | F3.L2.M04 | agrupada con HUECO-001 |
| 35 | garantia-entregas-DEBIDO-PROCESO-001 | S1 | F2.L2.M04 | se resuelve con F2.L2.M04 (P-03) |
| 36 | garantia-entregas-DEBIDO-PROCESO-002 | S2 | F3.L2.M08 | microtarea propia |
| 37 | garantia-entregas-DEBIDO-PROCESO-003 | S2 | F3.L2.M10 | microtarea propia |
| 38 | garantia-entregas-CLEAN-001 | S4 | F4.L2.M17 | microtarea propia |
| 39 | garantia-entregas-ACOPLE-001 | S2 | F4.L2.M04 | microtarea propia |
| 40 | garantia-entregas-CONTRATO-002 | S2 | F0.L2.M03 | microtarea propia |
| 41 | garantia-entregas-CONTRATO-001 | S3 | F1.L2.M12 | microtarea propia |
| 42 | garantia-entregas-CONTRATO-003 | S3 | F0.L2.M04 | microtarea propia |
| 43 | garantia-entregas-CONTRATO-010 | S4 | F4.L2.M13 | microtarea propia (condicionada D-L2-1) |
| 44 | garantia-entregas-DATOS-002 | S2 | F3.L2.M14 | agrupada con PARCHE-001 |
| 45 | garantia-entregas-DATOS-003 | S3 | F4.L2.M08 | microtarea propia |
| 46 | garantia-entregas-DATOS-004 | S3 | F4.L2.M09 | microtarea propia |
| 47 | garantia-entregas-PRUEBAS-001 | S3 | F4.L2.M10 | microtarea propia |
| 48 | garantia-entregas-PRUEBAS-010 | S3 | F4.L2.M11 | microtarea propia |
| 49 | garantia-entregas-UX-A11Y-010 | S3 | F3.L2.M11 | microtarea propia |
| 50 | garantia-entregas-UX-A11Y-011 | S3 | F4.L2.M14 | microtarea propia (condicionada D-L2-1) |
| 51 | garantia-entregas-UX-A11Y-012 | S3 | F4.L2.M15 | microtarea propia (condicionada D-L2-1) |
| 52 | garantia-entregas-UX-A11Y-013 | S4 | F4.L2.M16 | microtarea propia |
| 53 | garantia-entregas-OBSERV-001 | S3 | F4.L2.M06 | microtarea propia |
| 54 | garantia-entregas-DOC-002 | S3 | F0.L2.M01 | microtarea propia |
| 55 | garantia-entregas-DOC-010 | S3 | F3.L2.M13 | microtarea propia |
| 56 | garantia-entregas-DOC-001 | S4 | F2.L2.M02 | se resuelve con F2.L2.M02 |
| 57 | garantia-entregas-SEG-001 | S1 | F2.L2.M02 | se resuelve con F2.L2.M02 |
| 58 | garantia-entregas-SEG-002 | S1 | F1.L2.M01 | microtarea propia |
| 59 | garantia-entregas-SEG-003 | S1 | F1.L2.M02 | microtarea propia (rediseño en F3.L2.M05) |
| 60 | garantia-entregas-SEG-004 | S1 | F1.L2.M03 | microtarea propia (rediseño en F3.L2.M06) |
| 61 | garantia-entregas-SEG-005 | S1 | F1.L2.M05 + F3.L2.M02 | corte en F1, webhook firmado en F3 |
| 62 | garantia-entregas-SEG-006 | S1 | F1.L2.M06 | microtarea propia |
| 63 | garantia-entregas-SEG-007 | S1 | F1.L2.M07 | microtarea propia |
| 64 | garantia-entregas-SEG-008 | S2 | F3.L2.M03 | microtarea propia |
| 65 | garantia-entregas-SEG-009 | S2 | F1.L2.M14 | microtarea propia |
| 66 | garantia-entregas-SEG-010 | S4 | F4.L2.M18 | microtarea propia |
| 67 | garantia-entregas-TRAZA-001 | S2 | F4.L2.M05 | microtarea propia |
| 68 | garantia-entregas-REG-001 | S3 | F0.L2.M02 | microtarea propia |

Control: 68 fichas en el informe (encabezados `### garantia-entregas-…` de la sección 5) · 68 filas trazadas ·
0 faltantes · 0 duplicadas.
