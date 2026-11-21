# Plan — Proveedores externos de Pasanaku (nuevo alcance)

- Fecha: 2026-11-21 · Repo: `PasanakuBackend` · Complementa a [`PLAN.md`](./PLAN.md) (fases F0 a F5) y a [`DECISIONES.md`](../2026-11-20-auditoria-consolidado/DECISIONES.md)
- Base de conocimiento usada: las habilidades de la casa `proveedores-externos`, `payments-qr-integration`, `disbursement-payouts`, `kyc-identity-verification` y `payment-reconciliation`; las reglas 90, 91, 96, 97 y 98; y `docs/Cumplimiento.md` (§1.5 seguridad de la información, §5 datos personales, §6 ISO 27001 A.5.19–A.5.22).
- **No se nombra ningún proveedor concreto ni se cita ninguna norma de memoria.** La elección de cada proveedor sale de una solicitud de propuestas (RFP) con los criterios de este plan. Lo normativo que no está en el repo queda como «a identificar por Cumplimiento», en [`PREGUNTAS-LEGALES.md`](./PREGUNTAS-LEGALES.md).

## Resultado observable

Cada proveedor externo que necesita el producto cumple cuatro condiciones:
1. Está contratado, con un contrato de tercero vigente que incluye las cláusulas de datos, auditoría y continuidad.
2. Pasó los casos obligatorios en un entorno de prueba regulado.
3. Está integrado detrás de un puerto propio, con su doble de prueba en tres niveles: correcto, límite e inválido.
4. Se puede reemplazar o apagar sin tocar ningún caso de uso.

Además, el sistema **se niega por código** a usar en producción un adaptador cuyo proveedor no tenga el contrato vigente y las pruebas aprobadas.

## Kill-test

Cualquiera de estas situaciones demuestra que el plan no se cumplió:
1. Un adaptador real activado en producción sin un `contrato_tercero` en estado `VIGENTE` y sin un `entorno_prueba_regulado` aprobado.
2. Un webhook sin firma válida que produce un efecto.
3. Un timeout del proveedor que termina en un cobro o un desembolso duplicado.
4. Un dato personal o financiero en el log de un adaptador.

## Mapa de proveedores del nuevo alcance

| # | Proveedor | Para qué lo usa el producto | Servicio dueño | Decisión que lo origina | Crítico (ASFI / ISO 27001 A.5.19) |
|---|---|---|---|---|---|
| PX-1 | **Pasarela de cobro QR** | Cobro de cuotas, de la comisión (línea aparte en el mismo QR) y de las recargas de la billetera | `aportes` (cuotas), `nucleo-financiero` (recargas) | R-NUEVA-1, P-06, P-15 | Sí |
| PX-2 | **Proveedor de desembolso** (transferencia saliente) | Entrega del pozo, retiros de la billetera, devolución del fondo al cierre, pago a comercios (F5) | `entregas`, `nucleo-financiero`, `garantia` | P-10, P-09, modelo A, F5 | Sí |
| PX-3 | **Banco custodio** | Cuenta de custodia de fondos de usuarios, cuenta segregada del fondo de cada grupo y extractos para conciliar | `nucleo-financiero` | Modelo A (L-01 abierta) | Sí |
| PX-4 | **Verificación de identidad (KYC)**: documento, prueba de vida y comparación biométrica | Alta automática; solo las excepciones y los PEP los revisa una persona | `identidad` | P-08 | Sí (biometría) |
| PX-5 | **Control de listas y PEP** (sanciones, listas de la UIF) | Control al alta y periódico; alertas para Cumplimiento | `cumplimiento` | P-08, P-09 | Sí |
| PX-6 | **Verificación de titularidad de la cuenta bancaria** | Que la cuenta destino sea del beneficiario antes de desembolsar | `entregas` / `identidad` | L2 SEG-006, F3.S2.M3 | Sí |
| PX-7 | **Mensajería** (SMS, push y correo; **sin WhatsApp**) | Códigos de verificación (OTP), invitaciones, recordatorios de pago, enlace de pago rápido, avisos de mora, descargo y nivel | `notificaciones` | Invitación (tanda 5), D, debido proceso | Sí para OTP; no para avisos |
| PX-8 | **Facturación electrónica** | Factura de la comisión de la empresa | `tarifas` | Comisión con porcentaje único | Sí (fiscal) |
| PX-9 | **Central de riesgo crediticio** | Reporte del historial de pago con el consentimiento del usuario (capa 3 de «Poder de compra») | `transparencia` | F5; L-04 abierta | Sí; condicionado |
| PX-10 | **Comercios aliados** | Venta con descuento y canje por QR en la tienda | nuevo agregado en un servicio a decidir por ADR-057 (regla 98.7) | F5 | Sí; condicionado |
| PX-11 | **Infraestructura**: nube, gestor de secretos o KMS, respaldo y observabilidad | Donde corre todo; custodia de llaves | Plataforma | Regla 90.3; R-RIS-03 | Sí |

---

## PX-0 — Marco común de integración

Todo proveedor de la tabla anterior entra por este marco. Ningún caso de uso conoce el SDK ni la URL de un proveedor.

### Arquitectura

```
caso de uso → Puerto de dominio (p. ej. Pasarela, ProveedorDeDesembolso, VerificadorDeIdentidad)
                     │
                     ▼
          Enrutador (cobertura → prioridad → salud → costo)  ← registro de salud medida
                     │
                     ▼
          Adaptador (declara capacidades: crear, consultarEstado?, reembolsar?, desembolsar?)
                     │                         ▲
                     ▼                         │ webhook firmado
               Proveedor externo ──────────────┘ → Ingreso común de webhooks (firma → ventana → crudo → dedupe → outbox → 2xx)
```

### Requisitos del marco (RM)

| ID | Requisito | Origen |
|---|---|---|
| RM-01 | Cada familia de proveedores tiene un puerto en `dominio/puertos` y un adaptador por proveedor en `infraestructura/proveedores/<nombre>`. El caso de uso nunca importa un SDK | ADR-033 (hexagonal), regla 96.1 |
| RM-02 | El adaptador **declara sus capacidades** (`soportaConsultaEstado`, `soportaIdempotencia`, `soportaReembolso`, montos mínimo y máximo, monedas). El enrutador no le pide lo que no soporta. **Un adaptador sin consulta de estado no puede atender montos por encima del umbral parametrizado** | Skill `proveedores-externos` |
| RM-03 | **La clave de idempotencia es de la operación, no del proveedor.** Viaja al proveedor y no cambia al conmutar | Regla 91.1.4; `uq_orden_desembolso_clave` |
| RM-04 | Ingreso común de webhooks (`plataforma/comun-web`). Verifica la firma en tiempo constante, **siempre, también en desarrollo**; rechaza fuera de la ventana temporal; guarda el crudo en `webhook_pasarela` (append-only, sin loguear el cuerpo); deduplica por el id del evento del proveedor con un índice `UNIQUE`; publica por outbox y responde 2xx en menos de 2 s. El proceso posterior es asíncrono | Regla 91.3.2; skill `payments-qr-integration` §4 |
| RM-05 | Los eventos fuera de orden se guardan y se reprocesan al llegar el faltante. Cuando llegan éxito y fallo, **manda el último acuse** | Skill `proveedores-externos` |
| RM-06 | Trabajo de **reconsulta** de operaciones pendientes por más de N minutos (parámetro), idempotente, con backoff, jitter y tope; agotados los intentos, va a una excepción con alerta | Skill `payments-qr-integration` §5 |
| RM-07 | **Un timeout no es un fallo.** Se consulta el estado por la clave antes de reintentar. Sin consulta de estado no hay reintento automático: va a revisión con alerta | Skill `disbursement-payouts` §4; regla 98.2.2 |
| RM-08 | Timeout explícito, *circuit breaker* y *bulkhead* por proveedor; degradación declarada por puerto (falla el pedido, encola o conmuta) | Regla 98.2 |
| RM-09 | **Salud medida:** ventana móvil de operaciones confirmadas sobre iniciadas, por proveedor y operación. Por debajo del umbral, el proveedor se degrada y el tráfico conmuta. La conmutación avisa a operaciones y queda registrada. Hay un interruptor manual en el backoffice con cuatro ojos | Skill `proveedores-externos` |
| RM-10 | Un proveedor nuevo entra con prioridad baja y una porción del tráfico (parámetro), y sube según su salud medida | Ídem |
| RM-11 | **Candado de habilitación por código:** un adaptador real solo se activa en producción si su proveedor tiene un `contrato_tercero` en estado `VIGENTE`, con `clausula_confidencialidad`, `clausula_auditoria` y `clausula_continuidad` en verdadero, y un `entorno_prueba_regulado` con todos los casos obligatorios aprobados. Si no, el servicio no arranca con ese adaptador | `docs/Cumplimiento.md` §1.5; ISO 27001 A.5.19–A.5.22 |
| RM-12 | Credenciales y secretos de firma en el gestor de secretos (PX-11); en la base solo `referencia_credenciales`; rotación sin cortar el servicio (se aceptan dos llaves durante la ventana de rotación) | R-SEG-01; regla 90.3 |
| RM-13 | **Minimización:** al proveedor se manda solo lo que la operación necesita. Nunca se persiste ni se loguea un número de cuenta, el payload de un QR, imágenes ni biometría en claro. Lo que el proveedor devuelve se mapea a un catálogo cerrado propio | Regla 90.2; R-SEG-01 |
| RM-14 | **Costo medido** por operación en `costo_proveedor_operacion`. Si el medido supera al contratado, se abre una alerta de revisión comercial | Skill `proveedores-externos` |
| RM-15 | **Baja de un proveedor:** se desactiva para operaciones nuevas y queda disponible para consultar y conciliar. Nunca se borra | Ídem |
| RM-16 | Observabilidad por llamada: `correlationId`, proveedor, operación, latencia, resultado mapeado y clave de idempotencia, sin datos personales. Hay un tablero de salud por proveedor | Regla 98.5 |
| RM-17 | **Doble por puerto en tres niveles** (regla 65): correcto, límite e inválido. Firma real, latencia y fallos programables. Se usa en local, en CI y en TEST, y nunca se presenta como el proveedor real | Regla 65; regla 80.5.10 |
| RM-18 | **Prueba de contrato** del adaptador contra el contrato publicado del proveedor (consumer-driven), más una prueba de humo contra su sandbox. El sandbox no reemplaza al doble | Regla 98.1.4 |
| RM-19 | **Continuidad:** cada proveedor crítico tiene un plan con RTO/RPO (`plan_continuidad`) y una prueba vigente (`prueba_continuidad`). Para pasarela y desembolso, un **segundo proveedor contratado es obligatorio** antes de producción (decisión de Pablo, tanda 8) | R-RIS-03; ISO 22301 |
| RM-20 | Ningún parámetro de proveedor (umbrales, porcentaje de tráfico, timeouts de negocio, ventanas) queda fijo en el código: va al registro de parámetros del backoffice (F3.S8.M2), con cuatro ojos y vigencia | Tanda 6 y 7 |

### Microtareas del marco

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| PX0.M01 | ADR-058 «Integración con proveedores externos» con RM-01 a RM-20 | ADR aprobado por Pablo | Revisión | TODO |
| PX0.M02 | Ingreso común de webhooks en `comun-web` (RM-04, RM-05), reutilizado por los servicios | Firma inválida → 401 sin efecto; evento duplicado → un solo efecto; evento viejo → rechazado; desorden → reprocesado | Pruebas de integración de los cinco casos contra la base real | TODO |
| PX0.M03 | Enrutador con salud medida, prioridad, porción de tráfico e interruptor manual (RM-08 a RM-10) | Con un doble degradado, el tráfico conmuta y se emite la alerta | Prueba con el doble forzado a fallar | TODO |
| PX0.M04 | Candado de habilitación (RM-11) | Un adaptador real sin contrato vigente impide arrancar el servicio en el perfil de producción | Prueba de arranque | TODO |
| PX0.M05 | Trabajo de reconsulta genérico (RM-06, RM-07), con ShedLock, timeout y visibilidad del último éxito | Una operación sin webhook se resuelve por reconsulta sin duplicar | Prueba con el doble en modo «sin webhook» | TODO |
| PX0.M06 | Integración con el gestor de secretos y rotación de llaves de firma con dos llaves activas (RM-12) | La rotación no rechaza webhooks durante la ventana | Prueba de rotación | TODO |
| PX0.M07 | Medición de costo por operación y alerta (RM-14) | El costo medido queda registrado por operación | Consulta pegada | TODO |
| PX0.M08 | Tablero de salud y costo por proveedor en el backoffice, con los cuatro estados, WCAG 2.2 AA y modo claro y oscuro | Capturas en 3 *viewports* y 2 temas | Prueba visual | TODO |
| PX0.M09 | Pantalla de alta y gestión de proveedores en el backoffice (contrato de tercero, evaluación, entorno de prueba, prioridad y baja), con cuatro ojos para activar o dar de baja | Sin contrato vigente no se puede activar | E2E + autorización negativa | TODO |
| PX0.M10 | Plantilla de RFP y matriz de evaluación (sección «Selección») como documento del repo | Existe y la aprueba Pablo | Revisión | TODO |

---

## Proceso común de contratación (por proveedor)

```
1. RFP con los requisitos de su sección  →  2. evaluación ponderada (evaluacion_tercero)  →
3. revisión legal y de Cumplimiento (cláusulas, país de procesamiento, comunicación al organismo si es crítico)  →
4. contrato_tercero VIGENTE  →  5. adaptador contra el doble + prueba de contrato  →
6. casos obligatorios en entorno_prueba_regulado (sandbox)  →  7. activación con tráfico bajo  →  8. sube por salud medida
```

**Mercado:** no hay proveedores ni contactos previos; todas las RFP salen al mercado abierto (decisión de Pablo, tanda 8).

**Criterios de evaluación comunes.** Los pesos los fija Pablo (`DECISION_REQUIRED`); estos son los criterios:

| Criterio | Qué se pregunta |
|---|---|
| Regulación | ¿Está autorizado o supervisado por la autoridad que corresponde para ese servicio en Bolivia? (Cumplimiento lo determina) |
| Capacidades técnicas | Webhooks firmados, consulta de estado, idempotencia por clave nuestra, sandbox, contrato de API publicado y versionado |
| Seguridad | Certificaciones (por ejemplo, ISO 27001 o PCI DSS si aplica), mTLS, rotación de llaves, respuesta a incidentes y notificación de brechas |
| Datos | Qué datos pide, dónde los procesa (`pais_procesamiento`), retención, borrado y suboperadores |
| Disponibilidad | SLA, ventanas de mantenimiento, historial de incidentes y soporte |
| Costo | Costo por operación, mínimos y costo de salida |
| Salida | Exportación de datos, plazo de aviso y cooperación en la migración |

---

## PX-1 — Pasarela de cobro QR

**Puerto:** `Pasarela` en `aportes`; reemplaza `ProveedorDeSecretoWebhook` como pieza suelta y absorbe `ProveedorDeRecargas` de `nucleo-financiero`, que hoy está duplicado (ver PX-2).
**Flujo:** `obligacion_aporte → orden_cobro → qr_cobro → intento_pago → (webhook firmado) → pago → asiento`.

### Requisitos funcionales
| ID | Requisito |
|---|---|
| RF-PX1-01 | Generar un QR **de monto fijo atado a la orden**, con vigencia explícita (parámetro), uno por orden e intento; regenerar crea un intento nuevo y no pisa el anterior |
| RF-PX1-02 | El QR incluye la cuota y la comisión como **dos líneas** (comisión con porcentaje único, tanda 7); el pago se reparte en dos asientos: pozo y comisión |
| RF-PX1-03 | Recibir la confirmación por webhook firmado; **validar monto y moneda contra la orden**; si no coinciden, no se acredita y se abre una `excepcion_conciliacion` |
| RF-PX1-04 | Por encima del umbral (parámetro), **reconsultar el estado en la API** antes de acreditar, aunque el webhook sea válido |
| RF-PX1-05 | Recarga de billetera por QR con el mismo flujo; el saldo recargado sirve para pagar cuotas (P-06) |
| RF-PX1-06 | Reembolso de un pago (cuatro ojos reales, P-13), contra la pasarela si lo soporta; si no, por PX-2 |
| RF-PX1-07 | Pago tardío (después del corte): se acepta y se aplica primero a la deuda con el fondo del grupo (F3.S1.M6) |
| RF-PX1-08 | Enlace de pago rápido (por push, SMS o correo) con un token de propósito único, de un solo uso y con vencimiento, que no autentica: solo permite pagar esa obligación |
| RF-PX1-09 | Los motivos de rechazo de la pasarela se mapean a un catálogo cerrado propio |
| RF-PX1-10 | Compatibilidad con el estándar de QR interoperable que rija en Bolivia (a identificar por Cumplimiento), para que se pueda pagar desde la app de cualquier banco |

### Requisitos no funcionales y de seguridad
- Respuesta al webhook en menos de 2 s; el proceso es asíncrono por outbox.
- El payload del QR nunca se persiste ni se loguea: solo su referencia y su hash.
- Firma HMAC o asimétrica según el proveedor, con comparación en tiempo constante y ventana temporal.

### Doble y casos obligatorios (RM-17 y la skill §8)
| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | Webhook firmado, monto igual al de la orden | Un pago, dos asientos (pozo y comisión), `aporte_cobrado` |
| Límite | El mismo webhook dos veces · evento en el borde de la ventana · pago justo al vencer el QR · pago después del corte | Efecto único · aceptado · según la política declarada · aplicado a la deuda |
| Inválido | Firma inválida · monto distinto · orden inexistente o vencida · evento viejo (replay) · la pasarela no responde | 401 sin efecto · excepción · rechazo · rechazo · lo resuelve la reconsulta sin duplicar |

### Degradación
Si la pasarela está caída, no se puede pagar. La app lo dice («el cobro por QR no está disponible, reintentá en X»), **la mora no corre** mientras dure el incidente declarado (parámetro de gracia por incidente) y se conmuta al segundo proveedor si existe.

### Microtareas
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| PX1.M01 | Puerto `Pasarela` + doble con firma real en tres niveles | La tabla de casos completa en verde | Suite del doble | TODO |
| PX1.M02 | Orden de cobro y QR con dos líneas (cuota y comisión) | Los asientos de pozo y comisión salen separados y cuadran | Asientos pegados | TODO |
| PX1.M03 | Reconsulta por encima del umbral | Con un umbral bajo, el doble recibe la consulta antes de acreditar | Prueba | TODO |
| PX1.M04 | Enlace de pago rápido con token de un solo uso | Un token usado o vencido muestra un mensaje claro y ofrece uno nuevo | E2E | TODO |
| PX1.M05 | Suspensión de la mora por incidente de la pasarela | La mora no corre durante el incidente | Prueba con reloj controlado | TODO |
| PX1.M06 | RFP, evaluación y contrato del proveedor de QR (y del segundo) | `contrato_tercero` en `VIGENTE` | Registro en el backoffice | DECISION_REQUIRED |
| PX1.M07 | Adaptador real + prueba de contrato + casos en el sandbox | Casos aprobados en `entorno_prueba_regulado` | Salida del sandbox | Bloqueado por PX1.M06 |

---

## PX-2 — Proveedor de desembolso

**Puerto:** `ProveedorDeDesembolso`, compartido por `entregas`, `nucleo-financiero` (retiros) y `garantia` (devolución del fondo). **Se consolida el clon divergente** que hoy existe en `nucleo-financiero/dominio/puertos`. Comprobado en `origin/test`: `ProveedorDeRetiros.instruir(UUID, Dinero)` no devuelve nada y su consulta devuelve un `Optional<Confirmacion>`, mientras que `ProveedorDeRetiro.instruir(...)` devuelve un `Resultado` con estados `ACEPTADO`/`RECHAZADO`/`TIMEOUT`. Son dos contratos distintos para lo mismo. Además, `ProveedorDeRecargas` genera su propio QR, separado de la pasarela de `aportes`: se unifica en PX-1.

### Requisitos funcionales
| ID | Requisito |
|---|---|
| RF-PX2-01 | Una orden por operación con `clave_idempotencia` `UNIQUE`, que **viaja al proveedor**; reintentar la creación devuelve la misma orden |
| RF-PX2-02 | Solo hacia una cuenta **verificada** (PX-6) y fuera del enfriamiento tras un cambio (parámetro); el enfriamiento no se saltea por pedido de soporte |
| RF-PX2-03 | Confirmación de la acreditación **solo** por webhook firmado o por consulta de estado; nunca la marca una persona (L2 SEG-005) |
| RF-PX2-04 | `RECHAZADA_BANCO` (cuenta cerrada o datos mal): la plata vuelve, se registra la incidencia, se corrige y se reintenta con una **orden nueva** |
| RF-PX2-05 | Un timeout es estado desconocido: se consulta por la clave; si el proveedor no tiene consulta de estado, no hay reintento automático (RM-07) |
| RF-PX2-06 | Destinos: beneficiario del turno (entrega), titular (retiro), cada participante (devolución del fondo al cierre) y comercio (F5) |
| RF-PX2-07 | Límites por operación y por día (parámetros), alertas de patrón (misma cuenta para varios beneficiarios, cambio de cuenta reciente, monto fuera del histórico) y monitoreo UIF (P-09) |
| RF-PX2-08 | Cambio de cuenta → aviso por un canal distinto al que hizo el cambio (PX-7) |
| RF-PX2-09 | Confirmación de recepción solicitada al beneficiario; si falta a los N días (parámetro), se dispara seguimiento, sin bloquear la contabilidad |

### Doble y casos obligatorios
| Nivel | Casos |
|---|---|
| Correcto | Desembolso acreditado por webhook firmado; un solo asiento |
| Límite | La misma orden enviada dos veces → una sola transferencia · el webhook antes de la respuesta HTTP · una cuenta que sale del enfriamiento justo a tiempo |
| Inválido | Timeout sin respuesta → consulta de estado, sin duplicar · rechazo bancario → orden nueva · firma inválida → sin efecto · cuenta en enfriamiento → bloqueado con motivo |

### Degradación
Caído el proveedor, las entregas quedan en `ORDENADA` y en cola, sin perderse; la app muestra «tu entrega está en camino, el banco está demorando». Se conmuta al segundo proveedor solo para **órdenes nuevas**: nunca se reenvía una orden en estado desconocido a otro proveedor.

### Microtareas
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| PX2.M01 | Puerto único `ProveedorDeDesembolso`; eliminar `ProveedorDeRetiros` y `ProveedorDeRetiro` duplicados | Un solo puerto en todo el repo | `grep` + suites | TODO |
| PX2.M02 | Doble en tres niveles con timeout programable | La tabla de casos en verde | Suite | TODO |
| PX2.M03 | Máquina de estados con punto de no retorno (`ORDENADA`) y orden nueva ante un rechazo | Ningún reintento reusa la orden rechazada | Prueba | TODO |
| PX2.M04 | Límites, alertas de patrón y conexión con el monitoreo UIF | Un patrón sintético dispara una alerta | Prueba | TODO |
| PX2.M05 | Confirmación de recepción y seguimiento | Sin confirmación a los N días, aparece la alerta | Prueba con reloj | TODO |
| PX2.M06 | RFP, evaluación y contrato (principal y segundo) | `contrato_tercero` en `VIGENTE` | Registro | DECISION_REQUIRED |
| PX2.M07 | Adaptador real + contrato + sandbox | Casos aprobados | Salida | Bloqueado por PX2.M06 |

---

## PX-3 — Banco custodio y conciliación

**Puerto:** `BancoCustodio` (extractos y saldos) en `nucleo-financiero`. Conciliación: `extracto_bancario`, `movimiento_bancario`, `conciliacion`, `excepcion_conciliacion`.

### Requisitos
| ID | Requisito |
|---|---|
| RF-PX3-01 | **Cuentas separadas:** custodia de saldos de usuarios, una cuenta (o subcuenta) segregada **por cada fondo de grupo** del modelo A y una cuenta operativa de la empresa (comisiones). **Nunca se mezclan.** La figura jurídica depende de L-01 |
| RF-PX3-02 | Extracto diario por API o archivo, guardado crudo con hash, origen y periodo; importación idempotente por (cuenta, periodo, hash); importes parseados a decimal exacto con su moneda |
| RF-PX3-03 | Emparejamiento por niveles (referencia, id de la pasarela, monto con fecha, sugerencia con tolerancia declarada); cada emparejamiento guarda su regla y su confianza; unicidad movimiento ↔ pago en la base |
| RF-PX3-04 | Excepciones clasificadas (no identificado, faltante, sobrante, parcial, duplicado, comisión, reverso), con dueño, antigüedad y alerta |
| RF-PX3-05 | **Cierre diario:** saldo del banco = saldo del mayor ± partidas en tránsito. Si no cuadra, el día queda abierto con la diferencia registrada. Ninguna corrección se hace con `UPDATE`: todo es asiento |
| RF-PX3-06 | **Cuadre del fondo de cada grupo:** el saldo de su cuenta segregada coincide con el mayor del fondo; si no, se suspenden las coberturas de ese grupo y salta una alerta |
| RF-PX3-07 | Rendimiento de las cuentas: si el banco paga intereses, **no se reparte** (la pieza E quedó descartada); se registra y su destino es `DECISION_REQUIRED` |

### Microtareas
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| PX3.M01 | Importador de extractos idempotente y crudo | Importar dos veces no duplica | Prueba con un archivo sintético | TODO |
| PX3.M02 | Emparejador por niveles con regla y confianza | Tasa de conciliación automática medida sobre datos sintéticos | Salida del trabajo | TODO |
| PX3.M03 | Bandeja de excepciones en el backoffice (dueño, antigüedad, cuatro estados) | Una excepción vieja dispara alerta | E2E + captura | TODO |
| PX3.M04 | Cierre diario y cuadre por fondo de grupo | Un descuadre sintético deja el día abierto y suspende las coberturas | Prueba | TODO |
| PX3.M05 | Opinión legal sobre la figura de las cuentas segregadas (L-01) y elección del banco | Respuesta archivada y contrato vigente | Documento | DECISION_REQUIRED |

---

## PX-4 — Verificación de identidad (KYC)

**Puerto:** `VerificadorDeIdentidad` en `identidad` (F3.S5.M1).

### Requisitos
| ID | Requisito |
|---|---|
| RF-PX4-01 | **Niveles** atados a acciones y verificados en el servidor: 0 registrado (ser invitado) · 2 identificado (tomar cupo y aportar) · 3 cobrable (cuenta verificada, recibir la entrega). El nivel mínimo por acción es un parámetro |
| RF-PX4-02 | Documento en catálogo cerrado (tipos admitidos: L-06), **normalizado y único** (índice `UNIQUE` sobre la forma normalizada) |
| RF-PX4-03 | Prueba de vida y comparación con el documento por el proveedor; se guardan **la evidencia y el puntaje**, no solo la decisión |
| RF-PX4-04 | Estados `INICIADO → EN_REVISION_AUTOMATICA → {APROBADO, RECHAZADO, REVISION_MANUAL}` y `VENCIDO`. `REVISION_MANUAL` es la bandeja de Cumplimiento, con dueño y tiempo objetivo; ahí van **solo** las excepciones y los PEP (P-08) |
| RF-PX4-05 | Rechazo con motivo en catálogo cerrado y camino de reintento; mensajes que no revelan los criterios antifraude |
| RF-PX4-06 | **Imágenes y biometría:** el dato más sensible. Cifradas, nunca accesibles por URL adivinable, acceso auditado (bitácora de lectura) y retención declarada. Si el proveedor puede quedárselas, se prefiere que **no las devuelva**: guardamos solo la referencia y el resultado |
| RF-PX4-07 | Sospecha de duplicado o multicuenta: abre una revisión, no un bloqueo automático |
| RF-PX4-08 | Vencimiento con aviso previo, nunca de golpe el día del cobro |
| RF-PX4-09 | La app escucha el cambio de estado (evento o push); se elimina la consulta cada 30 s (L6 AP-01) |
| RF-PX4-10 | Funciona con cámaras malas y conexiones lentas: reintentos, ayuda contextual y vía de revisión manual |

### Doble (tres niveles)
- **Correcto:** aprobado con puntaje alto.
- **Límite:** puntaje en el umbral, que va a `REVISION_MANUAL`; documento por vencer.
- **Inválido:** documento duplicado; prueba de vida fallida; proveedor caído (el alta queda pendiente, con un mensaje claro, sin bloquear la cuenta).

### Microtareas
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| PX4.M01 | Puerto, doble y niveles verificados en el servidor | Tomar un cupo con nivel insuficiente da 403 | Prueba de API | TODO |
| PX4.M02 | Normalización y unicidad del documento | `1234567 LP` y `1234567-LP` cuentan como el mismo documento | Prueba | TODO |
| PX4.M03 | Bandeja de excepciones y PEP para Cumplimiento | Solo llegan los casos de revisión manual | E2E | TODO |
| PX4.M04 | Almacenamiento cifrado y bitácora de lectura de la evidencia | Acceder sin permiso da 403 y queda registrado | Prueba | TODO |
| PX4.M05 | Pantallas de la app: captura, estado, rechazo y reintento, con los cuatro estados, Semantics y movimiento reducido | Capturas en el dispositivo | Prueba visual | TODO |
| PX4.M06 | RFP, contrato (país de procesamiento de la biometría) y respuesta a L-06 | Contrato vigente | Registro | DECISION_REQUIRED |

---

## PX-5 — Listas y PEP

**Puerto:** `ControlDeListas` en `cumplimiento`.

| ID | Requisito |
|---|---|
| RF-PX5-01 | Control al alta y **periódico** (frecuencia parametrizada) contra las listas que defina Cumplimiento (a identificar: listas de la UIF, sanciones internacionales y PEP) |
| RF-PX5-02 | Coincidencia → alerta → caso LFT (analista ≠ revisor, identidades tomadas del token, F1.L4.M06); **nunca se le avisa al titular** (deber de reserva) |
| RF-PX5-03 | El resultado se guarda con su evidencia (versión de la lista, puntaje de coincidencia y fecha) |
| RF-PX5-04 | Si el proveedor está caído, el alta queda pendiente de control y **no se habilita dinero** hasta completarlo |
| RF-PX5-05 | Si las listas se importan de una fuente pública en lugar de un proveedor, se importan con su procedencia y su versión (regla 97.4), nunca escritas a mano |

Microtareas: **PX5.M01** puerto + doble (coincidencia exacta, coincidencia difusa en el umbral, proveedor caído) · **PX5.M02** control periódico programado · **PX5.M03** decisión de Cumplimiento sobre las listas y el proveedor (`DECISION_REQUIRED`).

---

## PX-6 — Verificación de titularidad de la cuenta bancaria

| ID | Requisito |
|---|---|
| RF-PX6-01 | Antes de habilitar una cuenta de destino, verificar que el titular coincide con la identidad KYC, por el medio que ofrezca el proveedor o el banco (validación de titular o micro-depósito) |
| RF-PX6-02 | La cuenta se guarda cifrada con su versión de llave (R-SEG-01b), se busca por hash con *pepper* y se muestra enmascarada; el número que se deduplica es el que se cifra (L2 SEG-007) |
| RF-PX6-03 | Alta o cambio → enfriamiento (parámetro) + aviso por otro canal; cambio de cuenta más pedido urgente de desembolso es una señal de alerta |

Microtareas: **PX6.M01** puerto + doble (titular coincide, no coincide, no verificable) · **PX6.M02** enfriamiento y aviso por otro canal · **PX6.M03** elección del medio de verificación junto con PX-2 o PX-3 (`DECISION_REQUIRED`).

---

## PX-7 — Mensajería (SMS, push y correo)

**Decisión de Pablo (tanda 8):** los canales son **SMS, notificaciones push y correo. No se usa WhatsApp.** Reparto por propósito:
- **SMS:** códigos de verificación (OTP) y avisos críticos para quien no tiene la app abierta.
- **Push:** recordatorios, invitaciones, enlace de pago, nivel, rachas y «Grupo perfecto».
- **Correo:** comprobantes, facturas (PX-8) y avisos formales con plazo (mora, descargo y apelación).
- **OTP de respaldo:** un segundo proveedor de SMS o el correo verificado.

El requisito RF-PX7-05 sobre plantillas aprobadas «por el canal» deja de aplicar a WhatsApp; queda para los remitentes de SMS registrados si la norma lo exige.

**Puerto:** el existente en `notificaciones` (CU-83 enruta el envío y CU-80 despacha). Hoy solo existe `MensajeriaSimulada`.

| ID | Requisito |
|---|---|
| RF-PX7-01 | **Un envío por evento, destinatario y canal** (R-NOT-01); la clave de idempotencia es del envío, no del proveedor |
| RF-PX7-02 | Canales por propósito: OTP (canal principal y respaldo), invitación al grupo (enlace con token de un solo uso), recordatorio antes del vencimiento, enlace de pago rápido, aviso de mora y de descargo (con plazos), subida de nivel y «Grupo perfecto» |
| RF-PX7-03 | Consentimiento por canal y por propósito (`consent-management`); horarios y frecuencia acotados para recordatorios y cobranza |
| RF-PX7-04 | **Contenido mínimo:** sin montos atribuibles ni datos de terceros en el texto del mensaje; los detalles se ven dentro de la app (regla 90.2) |
| RF-PX7-05 | Plantillas versionadas y aprobadas por el proveedor del canal cuando el canal lo exige; los textos los redacta Claude y los aprueba Pablo (P-16) |
| RF-PX7-06 | Si falla un canal, no se aborta el resto ni se duplican los envíos que ya salieron (regla 96.7.1); el OTP conmuta del SMS principal al segundo proveedor de SMS o al correo verificado |
| RF-PX7-07 | Webhooks de estado de entrega firmados, por el ingreso común (RM-04) |

Microtareas: **PX7.M01** doble por canal (entregado, demorado, rechazado, webhook duplicado) · **PX7.M02** plantillas nuevas del modelo A + D · **PX7.M03** conmutación del OTP · **PX7.M04** RFP de SMS (principal y respaldo), push y correo; contratos vigentes (`DECISION_REQUIRED` hasta tener los contratos) · **PX7.M05** eliminar del código y de las plantillas toda referencia a WhatsApp.

---

## PX-8 — Facturación electrónica de la comisión

**Puerto:** el existente en `tarifas` (hoy `ServicioDeImpuestosSimulado`).

| ID | Requisito |
|---|---|
| RF-PX8-01 | Una factura por cobro de comisión **confirmado** (evento de pago, nunca marcado a mano: L5 APROB-001), idempotente por cobro |
| RF-PX8-02 | Anulación o nota de crédito si la comisión se reembolsa o se revierte, con su asiento |
| RF-PX8-03 | Contingencia: si el servicio fiscal no responde, se registra el evento significativo y las facturas se regularizan dentro del plazo que fija la norma. `docs/Cumplimiento.md` §4 recoge el CUFD (24 h, extensible a 72), el CUF por documento, la firma digital y los eventos significativos con plazo de hasta 48 h. El detalle de la contingencia lo confirma Cumplimiento |
| RF-PX8-04 | El NIT o los datos fiscales del participante se piden solo si la norma los exige, con un valor por defecto para consumidor final, según lo que confirme Cumplimiento |

Microtareas: **PX8.M01** doble (emitida, rechazada, servicio caído → contingencia) · **PX8.M02** emisión por evento de cobro confirmado · **PX8.M03** RFP y contrato con un **proveedor de facturación autorizado** (decisión de Pablo: no se integra directo con el SIN).

---

## PX-9 — Central de riesgo crediticio (condicionado a L-04)

| ID | Requisito |
|---|---|
| RF-PX9-01 | Solo con un consentimiento expreso, separado, versionado y revocable; sin consentimiento no se comparte nada |
| RF-PX9-02 | Se reportan solo hechos de pago (pagó a tiempo, pagó tarde, deuda firme con el fondo) y **nunca** antes de que el expediente quede `FIRME` (regla 91.4) |
| RF-PX9-03 | Derecho de rectificación: un reclamo abierto suspende el reporte |
| RF-PX9-04 | Bitácora de qué se reportó, cuándo y con qué consentimiento |

Microtareas: **PX9.M01** respuesta legal L-04 (`DECISION_REQUIRED`) · **PX9.M02** diseño del consentimiento y del reporte (solo diseño, en paralelo a F3).

---

## PX-10 — Comercios aliados («Poder de compra», F5)

| ID | Requisito |
|---|---|
| RF-PX10-01 | **Alta de la empresa (KYB):** razón social, NIT, representante legal verificado (PX-4), cuenta verificada (PX-6), control de listas (PX-5) y contrato firmado con descuento, **precio congelado durante el ciclo**, plazo de entrega, garantía y devoluciones a cargo del comercio |
| RF-PX10-02 | Catálogo curado (3 a 5 comercios, 1 o 2 categorías en el piloto); el precio de lista se controla contra el mercado para que el descuento no sea inflado |
| RF-PX10-03 | **Canje:** el participante muestra su QR de canje; el comercio lo escanea con su app o su panel; recién en ese momento se le paga al comercio (PX-2). Si no entrega, no cobra |
| RF-PX10-04 | La comisión por venta se le cobra al comercio, nunca al ahorrista; se factura (PX-8) |
| RF-PX10-05 | Reclamos del comprador al comercio con plazo; la app media sin retener fondos más allá de lo que permita la opinión legal (L-11) |
| RF-PX10-06 | La opción de efectivo existe siempre |

Microtareas (todas de diseño ya, decisión de la tanda 6; el código espera la opinión legal): **PX10.M01** ADR-057 · **PX10.M02** contrato tipo con comercios (lo revisa el abogado) · **PX10.M03** diseño de pantallas del participante y del panel del comercio · **PX10.M04** modelo de datos del comercio y del canje (fuente `generar_ddl.py`, regla 97.1).

---

## PX-11 — Infraestructura (nube, secretos, respaldo y observabilidad)

| ID | Requisito |
|---|---|
| RF-PX11-01 | Contrato de tercero para el proveedor de nube con su anexo de seguridad y de datos personales (ISO 27017 y 27018 figuran 🟡 en `docs/Cumplimiento.md:439-440`) y su país de procesamiento |
| RF-PX11-02 | Gestor de secretos o KMS para credenciales de proveedores, llaves de firma, *pepper* y llaves de cifrado de columnas, con rotación y versión de llave |
| RF-PX11-03 | Respaldo con restauración probada y RTO/RPO por servicio crítico (R-RIS-03) |
| RF-PX11-04 | Observabilidad (logs, trazas y métricas) sin datos personales y con retención declarada; si es un tercero, también lleva contrato |

Microtareas: **PX11.M01** inventario de los terceros de infraestructura actuales y sus contratos · **PX11.M02** gestor de secretos operativo (lo necesita PX0.M06) · **PX11.M03** prueba de restauración documentada.

---

## Orden y dependencias con `PLAN.md`

| Cuándo | Qué |
|---|---|
| F0 | PX0.M01 (ADR-058), PX0.M10 (plantilla de RFP), PX11.M01 y PX11.M02, y lanzar las **RFP** de PX-1, PX-2, PX-3, PX-4 y PX-7 (son las que más tardan) |
| F1 | PX0.M02 (ingreso de webhooks) y PX0.M06 (secretos), porque cierran S1 de seguridad |
| F3 | Puertos, dobles y flujos de PX-1 a PX-8 (dentro de H3); PX0.M03 a M05, M07 a M09 |
| F3 (diseño en paralelo) | PX-9 y PX-10 |
| Pre-producción (H15) | Adaptadores reales, sandbox y `contrato_tercero` VIGENTE de PX-1, PX-2, PX-3, PX-4, PX-5, PX-6, PX-7 y PX-8; **segundo proveedor de PX-1 y de PX-2 contratado (obligatorio, decisión de Pablo)**; pruebas de continuidad |

## Evidencia exigida para cerrar cada proveedor

1. `contrato_tercero` VIGENTE y `evaluacion_tercero` registrados (captura del backoffice sin datos sensibles).
2. Salida de los casos de los tres niveles contra el doble y contra el sandbox.
3. Prueba del candado RM-11: el servicio no arranca con el adaptador sin contrato.
4. Prueba de idempotencia al conmutar de proveedor (misma clave, un solo efecto).
5. Prueba de caída del proveedor (doble apagado) con la degradación declarada observada.
6. Para proveedores de dinero: los cinco puntos de la regla 91.6, incluido el cierre de conciliación del día.
