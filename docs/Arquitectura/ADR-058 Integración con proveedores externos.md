---
tags:
  - arquitectura
  - adr
  - dinero
  - proveedores
  - seguridad
titulo: "ADR-058 — Integración con proveedores externos"
estado: aceptada
fecha: 2026-11-21
---

# ADR-058 — Integración con proveedores externos

> Todo proveedor externo (pasarela QR, desembolso, banco custodio, KYC, listas, titularidad de cuenta,
> mensajería, facturación, central de riesgo, comercios e infraestructura) entra por **un mismo marco**: un
> puerto propio, un adaptador que declara lo que sabe hacer, un ingreso común de webhooks firmados, un doble de
> prueba en tres niveles y un **candado por código** que impide usar en producción un proveedor sin contrato
> vigente y sin pruebas aprobadas. Ningún caso de uso conoce el SDK ni la URL de un proveedor.

> **Constancia de aceptación (veraz).** Redactado el 2026-11-21 a partir de
> [`PLAN-PROVEEDORES.md`](../trabajo/2026-11-21-plan-correcciones/PLAN-PROVEEDORES.md) (PX-0, requisitos
> RM-01 a RM-20) y de las decisiones de Pablo en
> [`DECISIONES.md`](../trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md) (P-15 y tanda 8). **No nombra
> ningún proveedor concreto ni cita normas de memoria**: la elección sale de una solicitud de propuestas (RFP) al
> mercado abierto, y lo normativo que no está en el repositorio queda «a identificar por Cumplimiento».
> **Pendiente de la aprobación de Pablo en el PR** (microtarea PX0.M01). **No habilita dinero real** (P-14).

## Contexto

- **No hay proveedores contratados** (P-15): ni pasarela QR ni proveedor de desembolso. Hoy corren simulados
  (`ClienteProveedorSimulado`, `MensajeriaSimulada`, `ServicioDeImpuestosSimulado`).
- El repositorio tiene **contratos duplicados para lo mismo**. Comprobado en `origin/test` por el plan de
  proveedores: `nucleo-financiero` tiene `ProveedorDeRetiros.instruir(UUID, Dinero)` sin retorno y
  `ProveedorDeRetiro.instruir(...)` con un `Resultado` (`ACEPTADO`/`RECHAZADO`/`TIMEOUT`); y
  `ProveedorDeRecargas` genera su propio QR, separado de la pasarela de `aportes`.
- [[ADR-033 Puertos y adaptadores|ADR-033]] ya fija la forma hexagonal (puerto en `dominio/puertos/`, adaptador
  en `infraestructura/adaptadores/<puerto>/`), pero no dice nada de firma de webhooks, idempotencia al conmutar,
  salud medida, contratos de tercero ni continuidad.
- [[ADR-055 Sin aprobaciones en el circuito|ADR-055]] y
  [[ADR-056 Tu garantía vuelve y la puntualidad vale plata|ADR-056]] ponen el circuito entero en manos de
  controles automáticos que dependen de proveedores: el webhook firmado de la pasarela, el del proveedor de
  desembolso, la verificación de titularidad de la cuenta y la del KYC. Si esos proveedores no están integrados
  con garantías, el circuito automático no es seguro.
- `docs/Cumplimiento.md` (§1.5 seguridad de la información, §5 datos personales, §6 ISO 27001 A.5.19–A.5.22)
  exige gestionar a los terceros críticos con contrato, evaluación y continuidad.
- DECISIONES, tanda 8: canales **SMS, push y correo, sin WhatsApp**; **segundo proveedor de cobro QR y de
  desembolso obligatorio antes de producción**; **ningún contacto previo**, las RFP salen al mercado abierto;
  facturación de la comisión **con un proveedor de facturación autorizado**, no directo con el SIN.

## Decisión

### 1. Arquitectura del marco

```
caso de uso → Puerto de dominio (Pasarela, ProveedorDeDesembolso, VerificadorDeIdentidad, …)
                     │
                     ▼
          Enrutador (cobertura → prioridad → salud → costo)  ← registro de salud medida
                     │
                     ▼
          Adaptador (declara capacidades: crear, consultarEstado?, reembolsar?, desembolsar?)
                     │                         ▲
                     ▼                         │ webhook firmado
               Proveedor externo ──────────────┘ → Ingreso común de webhooks
                                                   (firma → ventana → crudo → dedupe → outbox → 2xx)
```

### 2. Requisitos del marco (RM-01 a RM-20)

Se adoptan tal como los fija el plan de proveedores, con una precisión en RM-01 explicada en «Motivo».

| ID | Requisito | Origen |
|---|---|---|
| RM-01 | Cada familia de proveedores tiene un **puerto** en `dominio/puertos/` y un **adaptador por proveedor**. El caso de uso nunca importa un SDK. Ubicación del adaptador: `infraestructura/adaptadores/<puerto>/<proveedor>` (patrón de ADR-033) | ADR-033, regla 96.1 |
| RM-02 | El adaptador **declara sus capacidades** (`soportaConsultaEstado`, `soportaIdempotencia`, `soportaReembolso`, montos mínimo y máximo, monedas). El enrutador no le pide lo que no soporta. **Un adaptador sin consulta de estado no atiende montos por encima del umbral parametrizado** | Skill `proveedores-externos` |
| RM-03 | **La clave de idempotencia es de la operación, no del proveedor.** Viaja al proveedor y no cambia al conmutar | Regla 91.1.4; `uq_orden_desembolso_clave` |
| RM-04 | **Ingreso común de webhooks** en `plataforma/comun-web`: verifica la firma en tiempo constante, **siempre, también en desarrollo**; rechaza fuera de la ventana temporal; guarda el crudo en `webhook_pasarela` (*append-only*, sin loguear el cuerpo); deduplica por el id de evento del proveedor con índice `UNIQUE`; publica por outbox y responde 2xx en menos de 2 s. El proceso posterior es asíncrono | Regla 91.3.2; skill `payments-qr-integration` §4 |
| RM-05 | Los eventos fuera de orden se guardan y se reprocesan al llegar el faltante. Si llegan éxito y fallo, **manda el último acuse** | Skill `proveedores-externos` |
| RM-06 | Trabajo de **reconsulta** de operaciones pendientes por más de N minutos (parámetro), idempotente, con backoff, *jitter* y tope; agotados los intentos, va a una excepción con alerta | Skill `payments-qr-integration` §5 |
| RM-07 | **Un timeout no es un fallo.** Se consulta el estado por la clave antes de reintentar. Sin consulta de estado no hay reintento automático: va a revisión con alerta | Skill `disbursement-payouts` §4; regla 98.2.2 |
| RM-08 | Timeout explícito, *circuit breaker* y *bulkhead* por proveedor; degradación declarada por puerto (falla el pedido, encola o conmuta) | Regla 98.2 |
| RM-09 | **Salud medida:** ventana móvil de operaciones confirmadas sobre iniciadas, por proveedor y operación. Debajo del umbral, el proveedor se degrada y el tráfico conmuta; la conmutación avisa a operaciones y queda registrada. Interruptor manual en el backoffice con **cuatro ojos** | Skill `proveedores-externos` |
| RM-10 | Un proveedor nuevo entra con prioridad baja y una porción del tráfico (parámetro), y sube según su salud medida | Ídem |
| RM-11 | **Candado de habilitación por código:** un adaptador real solo se activa en producción si su proveedor tiene un `contrato_tercero` en estado `VIGENTE`, con `clausula_confidencialidad`, `clausula_auditoria` y `clausula_continuidad` en verdadero, y un `entorno_prueba_regulado` con todos los casos obligatorios aprobados. Si no, **el servicio no arranca** con ese adaptador | `docs/Cumplimiento.md` §1.5; ISO 27001 A.5.19–A.5.22 |
| RM-12 | Credenciales y secretos de firma en el gestor de secretos; en la base solo `referencia_credenciales`. Rotación sin cortar el servicio: se aceptan **dos llaves** durante la ventana de rotación | R-SEG-01; regla 90.3 |
| RM-13 | **Minimización:** al proveedor se manda solo lo que la operación necesita. Nunca se persiste ni se loguea un número de cuenta, el payload de un QR, imágenes ni biometría en claro. Lo que el proveedor devuelve se mapea a un **catálogo cerrado propio** | Regla 90.2; R-SEG-01 |
| RM-14 | **Costo medido** por operación en `costo_proveedor_operacion`. Si el medido supera al contratado, se abre una alerta de revisión comercial | Skill `proveedores-externos` |
| RM-15 | **Baja de un proveedor:** se desactiva para operaciones nuevas y queda disponible para consultar y conciliar. **Nunca se borra** | Ídem |
| RM-16 | Observabilidad por llamada: `correlationId`, proveedor, operación, latencia, resultado mapeado y clave de idempotencia, **sin datos personales**. Tablero de salud por proveedor | Regla 98.5 |
| RM-17 | **Doble por puerto en tres niveles** (regla 65): correcto, límite e inválido, con firma real, latencia y fallos programables. Se usa en local, en CI y en TEST, y **nunca se presenta como el proveedor real** | Regla 65; regla 80.5.10 |
| RM-18 | **Prueba de contrato** del adaptador contra el contrato publicado del proveedor (*consumer-driven*), más una prueba de humo contra su sandbox. El sandbox no reemplaza al doble | Regla 98.1.4 |
| RM-19 | **Continuidad:** cada proveedor crítico tiene un plan con RTO/RPO (`plan_continuidad`) y una prueba vigente (`prueba_continuidad`). Para **pasarela y desembolso, un segundo proveedor contratado es obligatorio antes de producción** | R-RIS-03; ISO 22301; DECISIONES tanda 8 |
| RM-20 | Ningún parámetro de proveedor (umbrales, porción de tráfico, timeouts de negocio, ventanas) queda fijo en el código: va al **registro de parámetros del backoffice** (plan F3.S8.M2), con cuatro ojos y vigencia | DECISIONES tandas 6 y 7; ADR-056 §6 |

### 3. Mapa de proveedores y puertos

| # | Proveedor | Puerto y servicio dueño | Crítico |
|---|---|---|---|
| PX-1 | Pasarela de cobro QR (cuota y comisión como dos líneas, recargas) | `Pasarela` en `aportes`; absorbe `ProveedorDeRecargas` de `nucleo-financiero` | Sí |
| PX-2 | Desembolso (entrega, retiros, devolución del fondo, comercios) | `ProveedorDeDesembolso`, **único**, compartido por `entregas`, `nucleo-financiero` y `garantia`; reemplaza a `ProveedorDeRetiros` y `ProveedorDeRetiro` | Sí |
| PX-3 | Banco custodio y conciliación (custodia, fondo de cada grupo segregado, cuenta operativa de la empresa; nunca se mezclan) | `BancoCustodio` en `nucleo-financiero` | Sí |
| PX-4 | Verificación de identidad (KYC) | `VerificadorDeIdentidad` en `identidad` | Sí |
| PX-5 | Control de listas y PEP | `ControlDeListas` en `cumplimiento` | Sí |
| PX-6 | Titularidad de la cuenta bancaria | `entregas` / `identidad` | Sí |
| PX-7 | Mensajería: **SMS, push y correo; sin WhatsApp** | El existente en `notificaciones` (CU-83 enruta, CU-80 despacha) | Sí para OTP |
| PX-8 | Facturación electrónica de la comisión, **por un proveedor autorizado** | El existente en `tarifas` | Sí (fiscal) |
| PX-9 | Central de riesgo crediticio | `transparencia`; condicionado a L-04 | Condicionado |
| PX-10 | Comercios aliados («Poder de compra») | A decidir en ADR-057 (regla 98.7) | Condicionado |
| PX-11 | Infraestructura: nube, gestor de secretos o KMS, respaldo y observabilidad | Plataforma | Sí |

Los requisitos funcionales, los casos obligatorios del doble y la degradación de cada proveedor están en
`PLAN-PROVEEDORES.md`, secciones PX-1 a PX-11; este ADR fija el marco que todos comparten.

### 4. Contratación (por proveedor)

```
1. RFP con los requisitos de su sección → 2. evaluación ponderada (evaluacion_tercero) →
3. revisión legal y de Cumplimiento → 4. contrato_tercero VIGENTE → 5. adaptador contra el doble + prueba de contrato →
6. casos obligatorios en entorno_prueba_regulado (sandbox) → 7. activación con tráfico bajo → 8. sube por salud medida
```

Criterios de evaluación: regulación, capacidades técnicas, seguridad, datos (país de procesamiento, retención,
suboperadores), disponibilidad, costo y salida. **Los pesos los fija Pablo** (`DECISION_REQUIRED`).

## Motivo

- **El circuito automático de ADR-055 es tan seguro como su proveedor más débil.** Un webhook sin firma, un
  timeout tratado como fallo o una clave de idempotencia que cambia al conmutar son caminos directos a acreditar o
  desembolsar dos veces. Por eso esos tres puntos (RM-03, RM-04, RM-07) son requisitos del marco y no de cada
  integración.
- **El candado por código (RM-11)** convierte una obligación de cumplimiento (contrato vigente, cláusulas,
  pruebas aprobadas) en algo que no depende de la buena voluntad de quien despliega.
- **Un puerto por familia y un adaptador por proveedor** permiten tener dos proveedores a la vez (RM-19) y
  reemplazar uno sin tocar casos de uso.
- **RM-01, precisión de ubicación:** el plan de proveedores decía `infraestructura/proveedores/<nombre>`. El
  repositorio ya tiene un patrón vigente para adaptadores (ADR-033, `infraestructura/adaptadores/<puerto>/`).
  Se sigue el patrón existente (regla 00 §1.4) y el proveedor queda como subcarpeta del puerto. Si Pablo prefiere la
  ruta del plan, se cambia este renglón y se enmienda ADR-033.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Integrar cada proveedor directo en su caso de uso | Acopla el negocio al SDK; impide el segundo proveedor (RM-19) y el doble (RM-17) |
| Verificar firmas solo en producción | Regla 91.3.2: un webhook sin firma verificada acredita dinero a quien sepa la URL; además el doble dejaría de probar lo mismo que producción |
| Reintentar automáticamente ante un timeout | Un timeout es estado desconocido; reintentar sin consultar duplica cobros o desembolsos (RM-07) |
| Activar un adaptador real por configuración, sin candado | El contrato y las pruebas quedarían como trámite de papel (RM-11) |
| Un solo proveedor de pasarela y de desembolso | DECISIONES tanda 8: el segundo es obligatorio antes de producción |
| WhatsApp como canal | DECISIONES tanda 8 |
| Integración directa con el SIN para facturar | DECISIONES tanda 8: proveedor de facturación autorizado |
| Elegir proveedores por contacto previo | DECISIONES tanda 8: RFP al mercado abierto |

## Consecuencias

- **ADR-035 (canales por defecto)** queda enmendada en los canales: WhatsApp sale del producto y el SMS pasa a
  ser canal de OTP y de avisos críticos cuando haya contrato. Se mantiene que, sin contrato, solo corren
  bandeja, correo y push.
- **Microtareas del marco** (plan de proveedores): PX0.M02 (ingreso común de webhooks) y PX0.M06 (secretos) en
  F1, porque cierran S1 de seguridad; PX0.M03 a M05 y M07 a M09 en F3; PX0.M10 (plantilla de RFP) en F0.
- **Consolidación de puertos:** PX2.M01 deja un solo `ProveedorDeDesembolso` y elimina los dos duplicados; PX-1
  absorbe `ProveedorDeRecargas`.
- **Hasta contratar,** todo flujo que depende de un proveedor queda «verificado contra el doble», nunca
  «verificado» a secas (regla 65 §4.1). El dossier H15 exige los contratos vigentes, el sandbox aprobado y el
  segundo proveedor de PX-1 y PX-2.
- **Costo:** dos proveedores por familia crítica y un marco común es más código y más contratos. Es el precio de
  poder apagar uno sin dejar de cobrar ni de entregar.

## Cómo se verifica

- Este documento: `python3 scripts/verificar_boveda.py` y `grep -c "| RM-[0-2][0-9] |"` → 20.
- El marco, por microtarea (plan de proveedores, «Evidencia exigida para cerrar cada proveedor»): contrato vigente
  y evaluación registrados; casos de los tres niveles contra el doble y contra el sandbox; prueba del candado
  RM-11 (el servicio no arranca con un adaptador sin contrato); prueba de idempotencia al conmutar (misma clave, un
  solo efecto); prueba de caída del proveedor con la degradación declarada; y, para proveedores de dinero, los
  cinco puntos de la regla 91.6.
- Kill-test del plan de proveedores: un adaptador real activo sin contrato vigente, un webhook sin firma con
  efecto, un timeout que duplica, o un dato personal en el log de un adaptador demuestran que no se cumplió.

## Referencias

- [`PLAN-PROVEEDORES.md`](../trabajo/2026-11-21-plan-correcciones/PLAN-PROVEEDORES.md) ·
  [`DECISIONES.md`](../trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md) (P-15, tanda 8) ·
  [`PREGUNTAS-LEGALES.md`](../trabajo/2026-11-21-plan-correcciones/PREGUNTAS-LEGALES.md).
- [[ADR-033 Puertos y adaptadores|ADR-033]] · [[ADR-035 Canales por defecto|ADR-035]] ·
  [[ADR-046 Alcance de la idempotencia|ADR-046]] · [[ADR-055 Sin aprobaciones en el circuito|ADR-055]] ·
  [[ADR-056 Tu garantía vuelve y la puntualidad vale plata|ADR-056]].
- `docs/Cumplimiento.md` §1.5, §5 y §6. Reglas 65, 90, 91 y 98.
