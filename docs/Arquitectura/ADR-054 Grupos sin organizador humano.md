---
tags:
  - arquitectura
  - adr
  - grupos
  - producto
  - dinero
titulo: "ADR-054 — Grupos sin organizador humano: confianza y emparejamiento"
estado: aceptada
fecha: 2026-10-31
---

# ADR-054 — Grupos sin organizador humano: confianza y emparejamiento

> El alta de un grupo exigía un organizador humano habilitado por backoffice que aprobaba a cada
> integrante, apretaba «iniciar» y convocaba el sorteo, sin custodiar ni cobrar nada (RN-18). Para una
> familia que ya se conoce, esas tres decisiones no deciden nada y convertían el alta en doce pasos.
> Este ADR **reabre el grupo autogestionado** que el modelo siempre tuvo y que
> [[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]] D-12 apagó por prudencia: el sistema
> admite, inicia, sortea, cobra, entrega y devuelve con reglas deterministas; las personas aportan y cobran.

> **Constancia de aceptación (veraz).** Aceptado por el dueño del proyecto (Pablo) el 2026-10-31
> («Sí a todo, D1 a D10, arrancá con el ADR») sobre la propuesta
> `docs/trabajo/2026-10-31-simplificar-grupos-devolucion-pagos/PROPUESTA.md`. Pablo es quien ADR-051
> nombró como ratificador de D-12 (Producto). **Pendiente de ratificación de Cumplimiento** en dos puntos
> declarados abajo (lectura de R-ORG-06 y titularidad por consulta al proveedor) y **de Legal** en el texto
> del reglamento plantilla. Este ADR **no habilita dinero real**: sigue vigente la condición del dossier
> H15 citada en ADR-051. Los importes de los ejemplos son **sintéticos**.

## Contexto

**Lo que decía el repositorio antes de este ADR.**

- ADR-051 D-12 (2026-10-11): «¿Grupos sin organizador humano? **No.** Todo grupo lo crea y administra un
  organizador habilitado por backoffice. El «organizador digital» deja de ser una vía de alta». Adoptada
  «para el modo simulado y como diseño de producción sujeto a ratificación» de **Producto**, con la
  alternativa descartada escrita así: «Mantener «organizador digital» como vía de alta — … Si Producto lo
  quiere, es una decisión explícita nueva». Esta es esa decisión.
- El modelo **nunca dejó de admitir** el grupo sin organizador: `grupo.organizador_id` es `NULL`-able,
  `grupo.es_autogestionado` existe (`docs/entidades/02_grupos_turnos.puml:104,673`;
  `sql/10_tablas/02_grupos_turnos/grupo.sql:23-24`), el contrato `EntradaGrupo.organizadorId` es
  `[string, null]` con la descripción «Un grupo sin organizador lo lleva la plataforma, y entonces nadie
  cobra por administrarlo (RN-18)» (`servicios/grupos/src/main/resources/openapi/grupos.yaml`), el trigger
  `grupos.fn_org_validar_contrato_grupo` devuelve `NEW` cuando `organizador_id IS NULL`
  («grupo autogestionado: no hay organizador que deba contrato», `docs/Restricciones.md:2703`) y el alta
  ya escribe `ES_AUTOGESTIONADO = organizador.isEmpty()`
  (`servicios/grupos/.../infraestructura/CreacionRepositorio.java:103-104`). Lo único que lo impide son
  dos guardas: `MapeoDeAltaDeGrupo.java:38-44` (`AP-CU20-02`) y `CU20CrearGrupo.java:88-90`.
- El modelo ya distingue **cómo se conforma** un grupo (`tipo_conformacion`: `MANUAL_POR_INVITACION`,
  `EMPAREJAMIENTO_AUTOMATICO`, `MIXTA`) y **quién lo ve** (`visibilidad`: `PRIVADO`, `ENLACE_DIRECTO`,
  `PUBLICO_DIRECTORIO`), con sus `CHECK` (`grupo.sql:38,40`).
- El «organizador digital» tiene paquete propio con `nivelAutonomia` (`SOLO_SUGIERE`, `EJECUTA_CON_AVISO`,
  `AUTONOMO` — «para grupos autogestionados maduros», `docs/entidades/07_organizador_automatizacion.md`)
  y código vivo (`CU95DefinirAutomatizacion`, `CU96EjecutarTarea` en `servicios/organizador`).
- El emparejamiento algorítmico está diseñado en [[CU-68 Postular a un grupo y ser emparejado]] §5-7
  (`postulacion_emparejamiento`, `propuesta_grupo`, `puntaje_cohesion`, `riesgo_estimado`) y solo tiene
  código el tramo de postular a un grupo existente (`CU68Postular`, `CU68AceptarIngreso`).
- La admisión humana de backoffice es un interruptor global
  (`aportaya.grupos.admision.humana-obligatoria`, ADR-052 DD-2: `true` en producción).
- La regla de inicio al 75 % y primera cuota acreditada (R-GRP-20, decisión del dueño 2026-10-30) ya está
  mergeada en `test` y `dev` (`CU69IniciarGrupo`, PRs #364/#365).

**El problema.** Con D-12, una familia de 10 recorre doce pasos para arrancar, tres de ellos decisiones
humanas vacías (firmar contrato de organizador, aprobar a cada invitado —en producción lo resuelve
backoffice—, apretar «iniciar»). El detalle paso a paso, con rutas, está en
`docs/trabajo/2026-10-31-simplificar-grupos-devolucion-pagos/evidencia/hallazgos.md` §A.

## Decisión

Se adoptan las diez decisiones de la propuesta, tal como Pablo las aprobó, con una precisión en D9
explicada en «Motivo».

| ID | Decisión | Cómo queda |
|---|---|---|
| D1 | **Existen grupos sin organizador humano.** Supersede ADR-051 D-12 **solo** en su «No»: D-12 pasa a «Sí, como grupo autogestionado operado por la plataforma; el organizador humano habilitado es una opción, no un requisito». Lo demás de ADR-051 (D-02, D-03, D-04, D-15, D-16) sigue vigente | `grupo.es_autogestionado = true`, `organizador_id IS NULL`. El creador es **anfitrión**: participante común que ocupa el cupo 1 y solo puede anular el enlace |
| D2 | **El enlace es la admisión** en el grupo de confianza. Canjearlo con debida diligencia vigente (`R-UIF-09`) y sin `restriccion_usuario` crea el participante `ACEPTADO_PENDIENTE_FIRMA` con cupo reservado; firmar el reglamento lo deja `ACTIVO`. Nadie aprueba a nadie | Grupo de confianza = `es_autogestionado = true` **y** `tipo_conformacion = MANUAL_POR_INVITACION` **y** `visibilidad ∈ {PRIVADO, ENLACE_DIRECTO}` |
| D3 | **Inicio y sorteo automáticos.** El grupo se conforma al firmarse el último cupo, o en la **fecha límite** con ≥ `ceil(0,75 × cupos_totales)` achicándose a los ocupados (R-GRP-20). El sorteo se compromete solo cuando todas las primeras cuotas están `ACREDITADO`, con revelación a plazo fijo para que quien quiera aporte entropía | Fecha límite = `fecha_inicio` del grupo (el primer día de cobro elegido). El plazo de revelación es un parámetro de configuración, no una constante |
| D4 | **Si no llega al 75 % en la fecha límite, se cancela y devuelve solo**: `estado = CANCELADO`, `cancelado_en`, y cada aporte acreditado vuelve **íntegro** a la billetera de quien lo puso, en la misma unidad de trabajo (saga de compensación), sin comisión | Transición nueva `ABIERTO_A_INSCRIPCION → CANCELADO`; evento `grupo.cancelado_sin_inicio` |
| D5 | **Puerta B — emparejamiento por algoritmo, también sin organizador.** CU-68 §5-7 materializa la `propuesta_grupo` como grupo autogestionado. La admisión humana de backoffice (ADR-052 DD-2) **sigue aplicando a este tipo** por interruptor hasta que Cumplimiento lo apague | `es_autogestionado = true` **y** `tipo_conformacion = EMPAREJAMIENTO_AUTOMATICO`. El interruptor pasa de global a **por `tipo_conformacion`** |
| D6 | **El organizador habilitado queda para profesionales**, fuera del camino principal de la app. CU-90/91/92/93 no cambian; la habilitación «Aprendiz» desde la app (`CU90HabilitacionBasica`) deja de ser la puerta del alta | `es_autogestionado = false` y `organizador_id` con contrato vigente (R-ORG-02), como hoy |
| D7 | **Reglamento plantilla «confianza»**, generado de los tres números (monto, periodicidad, cupos) con parámetros de catálogo: gracia, recargo, permutas, quórum, retiro sin aprobación del grupo, **disolución automática por mora generalizada**. El texto legal lo entrega Legal | Parámetros en las columnas existentes (`dias_gracia`, `aplica_recargo_mora`, `quorum_decisiones`, `configuracion_grupo.permite_permuta_turnos`). El umbral de mora generalizada **no existe en el modelo**: lo agrega el carril de esquema |
| D8 | **Devolver por donde vino.** La cuenta de origen del último pago QR acreditado del titular se registra como instrumento verificado por `metodo_verificacion = consulta al proveedor` (ya previsto en CU-18) **si y solo si** el proveedor devuelve nombre y documento del pagador y coinciden con el titular. El enfriamiento se conserva para cuentas agregadas a mano. Ejecución automática bajo el tope de doble aprobación (ADR-049) | **Condicionado al proveedor.** Si el proveedor contratado no devuelve el titular, esta decisión no se implementa y se dice |
| D9 | **Modelado sin columna nueva.** El «tipo de grupo» de la propuesta se expresa con los tres ejes que ya existen: `es_autogestionado` (quién opera), `tipo_conformacion` (cómo entra la gente) y `visibilidad` (quién lo ve) | Ninguna tabla ni enumeración nueva. Ver «Motivo» |
| D10 | **Orden de carriles**: ADR → esquema (umbral de mora, transición `CANCELADO`, reglamento plantilla) → `grupos` → app → devolución por cancelación y retiro → pagos en un toque y decisión comercial de la pasarela → puerta B → devoluciones que dependen del proveedor | Ver «Lo que falta en el código» |

**Límites.** El monto por aporte y la bolsa por turno de un grupo autogestionado se acotan por el catálogo
`limite_operativo_billetera` del **nivel de debida diligencia más bajo entre sus integrantes**
(`seeders/minimos/03-limites-operativos.json`, hoy `PROVISIONAL`). `requiere_kyc_minimo` sigue en
`BASICO`. **Ningún número nuevo** (regla 97.5.4): quien no alcanza el límite recibe la oferta de subir de
nivel ([[CU-02 Elevar nivel de debida diligencia]]), como hoy `AP-CU10-01`.

**R-ORG-06.** «Las acciones sensibles exigen confirmación humana» rige las **reglas configurables** de
CU-95 y las acciones de la regla 91.4 (sancionar, ejecutar aval, castigar deuda). El ciclo central del
grupo autogestionado —admitir por enlace, conformar, sortear, generar obligaciones, cobrar por webhook
verificado, entregar el pozo completo por saga, cancelar y devolver— **no es una regla de
automatización**: es el flujo de CU-69, CU-60, CU-21, CU-22 y CU-67, que ya ejecuta el sistema con o sin
organizador. **Pendiente de ratificación de Cumplimiento.**

## Motivo

- **Quitar al organizador no quita un control de dinero.** RN-18 y la regla 91.5 ya prohibían que
  custodiara o cobrara; la cuenta del grupo tiene titular «grupo» (`R-GRP-04`). Lo que se quita es un
  cuello de botella humano sobre decisiones que, entre gente que se conoce, no deciden nada.
- **La «responsabilidad identificable» que ADR-051 pedía la asume la plataforma**, que es quien de hecho
  ya ejecuta cada operación (webhook, saga de aporte, entrega, sorteo). La debida diligencia reforzada
  existía para quien «administra plata ajena»; sin administrador, la barrera correcta es el KYC de cada
  participante (`R-UIF-09`), que no cambia.
- **Todo lo demás es idéntico**, como pidió el dueño: cuenta del grupo, tarifa congelada con hash,
  reglamento firmado con hash, obligaciones por cupo, acreditación solo por webhook (regla 91.3.1),
  sorteo commit-reveal (`R-GRP-05`), entrega del pozo completo con cargos línea por línea (ADR-051 D-04),
  mayor append-only, idempotencia, UIF, debido proceso.
- **D9 sin columna nueva** porque la regla 00 §1.1 y la 97.4.6 obligan a localizar el equivalente antes de
  crear una pieza: el equivalente existe en tres columnas con `CHECK`, y el alta ya las escribe. Una
  columna `tipo` duplicaría `es_autogestionado` × `tipo_conformacion` y abriría combinaciones
  contradictorias. La propuesta recomendaba `tipo` antes de verificar esto; se corrige acá y se le
  informa al dueño.
- **Reusar antes que crear**: inicio al 75 % (`CU69IniciarGrupo`), estado `CANCELADO` y `cancelado_en`,
  `cancelar(motivo)` del agregado, organizador digital (CU-95/96), consulta al proveedor como método de
  CU-18, saga de compensación del aporte (ADR-028 S1). Lo nuevo son **disparadores** (evento o cron en
  lugar de un botón) y una transición.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| **Mantener D-12 y solo acortar el formulario** | El formulario no era el problema: los pasos 3, 8 y 10 (contrato de organizador, aprobar invitados, iniciar) seguían siendo humanos. |
| **Grupo autogestionado también para desconocidos sin admisión humana** | Desconocidos + plata es la zona de riesgo que motivó DD-2. Se conserva el interruptor para `EMPAREJAMIENTO_AUTOMATICO` hasta que Cumplimiento decida. |
| **Columna `tipo` nueva (`CONFIANZA` / `ABIERTO` / `ORGANIZADO`)** | Catálogo paralelo de `es_autogestionado` × `tipo_conformacion`, que ya existen con `CHECK` y código. Regla 97.4.6. |
| **Borrar CU-90/91/92 y la habilitación Aprendiz** | Rompe contratos publicados (regla 98.1.3) y elimina el segmento de organizador profesional que ADR-051 protegía. Se saca del camino principal, no del sistema. |
| **Marcar ADR-051 como `superada por ADR-054`** | Cinco de sus seis decisiones siguen vigentes. El verificador solo admite estados completos; se deja `aceptada` y este ADR nombra la cláusula que reemplaza. |
| **Devolución al banco sin verificar titularidad** | Fraude de toma de cuenta. Solo se automatiza cuando el proveedor confirma el titular; si no, el enfriamiento de CU-18 sigue. |
| **Inventar fecha límite, plazo de revelación o umbral de mora en el código** | Parámetros financieros y operativos sin fuente (regla 97.5.4). Van a configuración y catálogo. |

## Consecuencias

### 1. Alta de grupo — antes / después

| | Antes (ADR-051 D-12) | Después (este ADR) |
|---|---|---|
| Quién crea | Organizador habilitado (contrato Aprendiz o CU-90) | Cualquier usuario con debida diligencia vigente; es **anfitrión** y participante |
| Campos | 7 obligatorios | 3 (monto, periodicidad, cupos); el resto se deriva: nombre editable, `modalidad_turnos = SORTEO_ALEATORIO`, `visibilidad = ENLACE_DIRECTO`, `fecha_inicio` = primer día de cobro |
| Admisión | Pedir cupo → organizador acepta → backoffice resuelve (prod) → firmar | Abrir enlace → firmar. Nadie aprueba |
| Inicio | Botón del organizador | Último cupo firmado, o fecha límite con ≥ 75 % |
| Sorteo | Convocado por el organizador | Compromiso automático al acreditarse todas las primeras cuotas |
| Si no se llena | Cada uno pide el retiro (CU-65 2a) | `CANCELADO` y devolución íntegra automática |
| Guardas de código | `AP-CU20-02` si no hay organizador habilitado | `AP-CU20-02` solo cuando `es_autogestionado = false` |

Ejemplo sintético: 10 primos, Bs 200 mensuales. Hoy: 10 aprobaciones de KYC, 1 contrato de organizador,
9 solicitudes aprobadas (y resueltas por backoffice en producción), 1 «iniciar». Después: 10 aprobaciones
de KYC (no cambia, es de Cumplimiento), 9 enlaces abiertos y firmados, y el sistema hace lo demás.

### 2. Devolución — antes / después

| Caso | Antes | Después |
|---|---|---|
| Grupo que no arranca | Retiro a pedido de cada uno | Automática, íntegra, al vencer la fecha límite (D4) |
| Irse de un grupo de confianza | Aprobación del grupo según reglamento; plan de pago negociado | Sin aprobación; acreedor cobra al cierre del ciclo (como hoy); deudor sigue el calendario original como plan (D7) |
| Disolución | Acuerdo con quórum; prelación de 5 niveles con fondo mutual | Además, causal automática por mora generalizada (D7); con la reserva empresarial (ADR-051 D-15) y sin fondo mutual, la prelación queda en deudas propias → devolución a quienes no cobraron, a prorrata |
| Saldo al banco | Cuenta a mano, verificación, enfriamiento, Tesorería | «Por donde vino» con titular confirmado por el proveedor, bajo el tope de ADR-049 (D8, condicionado) |

### 3. Pagos

Sin cambio de regla: la acreditación sigue siendo **solo por webhook firmado** (regla 91.3.1). Cambia el
camino de la persona: «Pagar mi cuota» emite el QR por el importe exacto directo a la cuenta del grupo
(CU-21 medio `QR`, ya en contrato y app); la billetera deja de ser el paso obligado. Nada de esto cobra un
boliviano hasta que exista un proveedor contratado y evaluado por CU-99: hoy el seed apunta a `example.bo`
y TEST corre `ClienteProveedorSimulado`.

### 4. Lo que **no** cambia

RN-18 · regla 91 completa · `R-UIF-09` y la aprobación del KYC (hoy manual; es de Cumplimiento) ·
`R-GRP-04`, `R-GRP-05`, `R-GRP-13`, `R-GRP-20` · `R-ORG-02` para grupos con organizador · mayor
append-only · idempotencia · debido proceso (regla 91.4) · UIF · ADR-051 D-02/03/04/15/16 · ADR-052 DD-2
para `EMPAREJAMIENTO_AUTOMATICO`.

### 5. Consecuencias financieras

- El riesgo de mora **no cambia de dueño** por este ADR: ya era de la reserva empresarial (ADR-051 D-03).
- Un grupo cancelado sin inicio **no genera comisión** ni costo de entrega; sí generó costo de QR de las
  primeras cuotas devueltas. Ese costo lo absorbe la plataforma; su magnitud depende del proveedor y se
  mide en `costo_proveedor_operacion` (CU-99), no se estima acá.
- La comisión por turno (recomendación 2 % / piso Bs 10 / techo Bs 300,
  `docs/trabajo/2026-10-30-regla-sorteo-75-y-comision-por-turno/ANALISIS-COMISION.md`) **sigue pendiente**
  y se cobra igual en grupos autogestionados: es el servicio de la plataforma.

### Lo que falta en el código (no lo hace este ADR; carril por carril, en el orden de D10)

| # | Pieza | Dueño | Qué |
|---|---|---|---|
| 1 | Esquema | lane de modelo (`.puml` → DDL → jOOQ, regla 97.1) | Umbral de mora generalizada para la causal automática; transición `ABIERTO_A_INSCRIPCION → CANCELADO` en `historial_estado_grupo`; reglamento plantilla «confianza» |
| 2 | `grupos` — alta | `MapeoDeAltaDeGrupo`, `CU20CrearGrupo`, `grupos.yaml` | Aceptar `organizadorId = null` ⇒ `es_autogestionado = true`; anfitrión ocupa el cupo 1; `nombre`, `diaCobro`, `modalidadTurnos`, `fechaDeInicio` opcionales **solo** en ese caso (contrato versionado, regla 98.1.3) |
| 3 | `grupos` — admisión | `CU69EnlaceDeGrupo` | Si `es_autogestionado` y `MANUAL_POR_INVITACION`: `ACEPTADO_PENDIENTE_FIRMA` directo; interruptor `admision.humana-obligatoria` por `tipo_conformacion` |
| 4 | `grupos` — inicio y cancelación | `CU69IniciarGrupo` + tarea cron (CU-96) | Disparo en `fecha_inicio`; `CANCELADO` + evento `grupo.cancelado_sin_inicio` si < 75 % |
| 5 | `aportes` / `nucleo-financiero` | consumidor del evento | Devolución íntegra de las primeras cuotas acreditadas (compensación S1), idempotente por `message_id` |
| 6 | `grupos` — sorteo | consumidor «todas las primeras cuotas acreditadas» | `CU60Sortear.comprometer` automático; revelación programada |
| 7 | App | `apps/movil/lib/pantallas/pasanaku/` (leer antes las skills D1/D2/D4 de `AGENTS.md`) | Puerta A de 3 campos; «Compartir» como paso 2; pantalla del grupo sin acciones de administrar; sin «Pedir cupo» ni «Solicitudes» para autogestionados; «Pagar mi cuota» con QR exacto |
| 8 | Backoffice | `apps/backoffice` | Lista de grupos autogestionados con sus eventos automáticos; sin acciones de admisión para `MANUAL_POR_INVITACION` |
| 9 | `grupos` — puerta B | nuevo: `propuesta_grupo`, `postulacion_emparejamiento` (CU-68 §5-7) | Motor de propuestas; materialización como autogestionado |
| 10 | `nucleo-financiero` / `identidad` | `CU100RecibirWebhookPasarela`, CU-18, CU-28 | «Por donde vino» (D8), condicionado al proveedor |
| 11 | Documentación | CU-20, CU-60, CU-65, CU-67, CU-68, CU-69, `Restricciones.md` | Reflejar «actor principal: el sistema» donde corresponda; `AP-CU20-02` solo para grupos con organizador; nueva restricción para la cancelación automática |

## Cómo se verifica

- **Este ADR**: `python3 scripts/verificar_boveda.py` (indexado, 6 secciones, estado válido, número único) y
  `python3 scripts/verificar_criterios.py` con RC 0. Salida en
  `docs/trabajo/2026-10-31-adr-054-grupo-sin-organizador/evidencia/dod.txt`.
- **Cada carril de la tabla anterior** tiene su propio `PLAN.md` y la evidencia que exige la regla 98.8
  (prueba de contrato, duplicado, caída del dependido, traza). Lo mínimo que este ADR exige de ellos:
  - alta autogestionada sin organizador devuelve `201` y `es_autogestionado = true`; con
    `es_autogestionado = false` y sin contrato sigue rechazando `AP-CU20-02` (prueba de regresión);
  - canje del enlace en grupo autogestionado deja `ACEPTADO_PENDIENTE_FIRMA` sin fila en solicitudes;
  - cancelación en la fecha límite con < 75 %: grupo `CANCELADO`, billeteras con el importe original,
    asientos cuadrados, segunda entrega del evento sin efecto;
  - sorteo comprometido sin intervención humana y rechazado con `AP-CU60-11` mientras falte una cuota.
- Esta aceptación **no ejecutó pruebas del backend**: las referencias de código son lecturas del
  repositorio (`DISCOVERED`, regla 30).

## Ratificación pendiente

| Punto | Quién | Qué ratifica |
|---|---|---|
| Lectura de R-ORG-06 (el ciclo central no es «regla de automatización») | Cumplimiento | Que admitir, iniciar, sortear, cobrar, entregar y cancelar en un grupo autogestionado no exigen confirmación humana |
| D8 titularidad por consulta al proveedor | Cumplimiento + proveedor | Que el dato del proveedor vale como verificación de titularidad |
| D7 texto del reglamento plantilla | Legal | El texto; los parámetros son de catálogo |
| D5 admisión humana para `EMPAREJAMIENTO_AUTOMATICO` | Cumplimiento | Cuándo se apaga el interruptor |

## Referencias

- `docs/trabajo/2026-10-31-simplificar-grupos-devolucion-pagos/PROPUESTA.md` y `evidencia/hallazgos.md`
  (expediente de esta decisión, con rutas y líneas).
- [[ADR-051 Garantía del pozo, comisión y autogestión|ADR-051]] (D-12 reemplazada en su «No»; el resto vigente) ·
  [[ADR-052 Decisiones adoptadas por delegación del dueño|ADR-052]] (DD-2, DD-5) ·
  [[ADR-049 Doble aprobación de retiros|ADR-049]] · ADR-028 (saga S1).
- [[CU-20 Crear grupo y congelar tarifario]] · [[CU-60 Sortear los turnos]] · [[CU-65 Retirarse de un grupo]] ·
  [[CU-67 Disolver el grupo anticipadamente]] · [[CU-68 Postular a un grupo y ser emparejado]] ·
  [[CU-69 Invitar a un contacto y registrar sus referencias]] · [[CU-18 Registrar y verificar una cuenta bancaria de destino]] ·
  [[CU-21 Cobrar el aporte del período]] · [[CU-95 Definir una regla de automatización]].
- `docs/entidades/02_grupos_turnos.puml` · `docs/entidades/07_organizador_automatizacion.md` ·
  `docs/Restricciones.md` (R-GRP-04, R-GRP-05, R-GRP-13, R-GRP-20, R-ORG-02, R-ORG-06, R-UIF-09).
