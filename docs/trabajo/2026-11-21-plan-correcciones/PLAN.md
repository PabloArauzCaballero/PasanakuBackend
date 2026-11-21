# Plan — Correcciones de raíz de Pasanaku para producción

- Fecha: 2026-11-21 · Repo: `PabloArauzCaballero/PasanakuBackend` (monorepo) · Base auditada: `origin/test` 07fc6971
- Predecesor: auditorías L1–L6 y su consolidado (`docs/trabajo/2026-11-20-auditoria-consolidado/`)
- Decisiones de negocio: [`DECISIONES.md`](../2026-11-20-auditoria-consolidado/DECISIONES.md). **Este plan no contiene ningún supuesto de negocio.** Lo que no está decidido figura como `DECISION_REQUIRED`.
- Detalle por hallazgo: [`anexos/`](./anexos/). Hay un anexo por lote, con cada uno de los 474 hallazgos trazado a una microtarea.
- Resultado observable: un participante paga su cuota por QR y, sin que ninguna persona apruebe nada, el pago queda cobrado, asentado en el mayor y aplicado al pozo. Cuando el pozo está completo, se le entrega a quien tiene el turno en su cuenta verificada. Si alguien no paga, el fondo del grupo cubre la cuota y esa persona queda debiéndole al fondo. Al cerrar el ciclo, cada participante recupera su aporte al fondo, y la plata de la empresa no participa en ningún momento.
- Kill-test: cualquiera de estos cuatro hechos demuestra que el plan no está cumplido. (1) Un `curl` sin firma al webhook que cambie el estado de una cuota. (2) Un `POST` con un aprobador inventado en el cuerpo que logre un efecto. (3) Una fila nueva en `movimiento_contable` contra las cuentas 9.x. (4) Un `UPDATE` sobre `movimiento_contable` que la base no rechace.

## Alcance

- **IN:** los 474 hallazgos de L1–L6, las decisiones de `DECISIONES.md`, los ADR que esas decisiones cambian, las pruebas que demuestran cada arreglo y los textos de la web y la app.
- **OUT:** el piloto «Poder de compra» (fase 5). Su diseño se incluye acá, pero no se empieza sin la opinión legal y sin al menos un comercio firmado. También queda fuera cualquier despliegue a producción: lo habilita el dossier H15, no este plan.
- **Ambigüedades abiertas:** las de negocio están todas decididas. Las legales están en [`PREGUNTAS-LEGALES.md`](./PREGUNTAS-LEGALES.md) (L-01 a L-12) y se le envían al abogado o a Cumplimiento. Detalle:
  - Valores de los parámetros financieros: porcentaje del fondo por nivel, días de gracia, tasa y tope de recargo, umbrales de alerta UIF y enfriamiento tras cambio de cuenta. Los fijan Pablo y Cumplimiento.
  - Opinión de Cumplimiento sobre el fondo del grupo en cuenta segregada (si es captación ante ASFI).
  - Cobertura automática con subrogación sin expediente previo.
  - Reporte de impagos a una central de riesgo.

## Principios que rigen todas las microtareas

1. **Raíz, no síntoma.** Cada microtarea nombra su causa de raíz. Un arreglo que solo agrega un `if` delante del síntoma no cierra la microtarea.
2. **La autoridad la decide el servidor.** Las identidades salen del token verificado; los importes, del mayor o del dueño del dato; los estados, de la máquina de estados del agregado. Del cuerpo de la petición nunca sale una identidad, un aprobador ni un estado.
3. **Dinero (regla 91).** `BigDecimal` o entero en la menor unidad en todas las capas, siempre con moneda ISO 4217. El mayor es *append-only* y está garantizado por la base (permisos revocados y trigger de cuadre). Toda operación de dinero lleva `clave_idempotencia` con índice `UNIQUE`. Ningún saldo se calcula con `SUM()` ad hoc.
4. **Microservicios (regla 98).** Outbox para todo evento que no se puede perder. Consumidores idempotentes por `message_id`. Timeout explícito y circuit breaker en cada llamada. Nunca más de dos saltos sincrónicos. Prueba de contrato del lado del productor.
5. **Parámetros financieros configurables, obligatorios y sin valor por defecto.** Si falta uno, el servicio no arranca (regla 90.3.4 y 97.5.4).
6. **Escalera de evidencia (regla 30).** Una microtarea pasa a `HECHO` solo con su DoD ejecutado y la salida pegada en `evidencia/`. Las pruebas que necesitan Docker corren en la máquina de Pablo por Remote Control, con su permiso en cada caso.
7. **Revisión.** Un PR por cambio terminado, hacia `dev`. Pablo revisa y mergea; quien implementa no aprueba su propio trabajo.

## Estándares contra los que se mide el cierre

Se mide contra estos estándares, pero no se declara conformidad formal. La conformidad la determina una auditoría externa.

| Área | Referencia |
|---|---|
| Calidad del producto | ISO/IEC 25010: cada hallazgo de los anexos lleva su característica |
| Seguridad de la API | OWASP API Security Top 10 (2023) y OWASP ASVS 5.0, nivel 2 como piso; nivel 3 en autenticación, sesiones y operaciones de dinero |
| App móvil | OWASP MASVS (almacenamiento, red, autenticación y resiliencia) |
| Accesibilidad | WCAG 2.2 AA (reglas 95.4 y 80.7) |
| Interfaces | OpenAPI 3 como contrato primero, RFC 9457 para errores, RFC 9110, ISO 8601 con `America/La_Paz` e ISO 4217 |
| Datos personales y financieros | Regla 90.2: bitácora de lectura separada de la de escritura y minimización |
| Normativa boliviana (ASFI y UIF) | `docs/Cumplimiento.md` y `docs/Restricciones.md`. Lo que no está citado en el repo se registra como «norma a identificar por Cumplimiento» |

---

## H0 — Preparación (F0)

**CA:** Dado el repo en `dev`, cuando una sesión necesita probar una corrección de dinero, entonces tiene un entorno de integración real en la máquina de Pablo, ADR vigentes que coinciden con las decisiones y ningún candado que le impida cerrar.
**DoD:** las salidas de M1 a M6 en `evidencia/F0/`.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| F0.M1 | Marcar `DESCARTADO` las 9 microtareas viejas que traban `blocker_gate`, con el motivo «reemplazadas por el plan de correcciones del 2026-11-21» | `blocker_gate.py` no bloquea una sesión sin trabajo activo | `python .claude/hooks/blocker_gate.py --self-test` y la corrida en el repo con su salida | TODO |
| F0.M2 | Entorno de integración en la máquina de Pablo: `docker compose` del stack local (Postgres, Kafka y los servicios), sin datos reales | Todos los servicios pasan su healthcheck | `docker compose ps` + `curl` a `/actuator/health` de cada servicio | TODO |
| F0.M3 | ADR-055 «Sin aprobaciones en el circuito: el pago por QR mueve el flujo». Reemplaza las partes correspondientes de ADR-049 y ADR-054 | El ADR existe y ADR-049 y ADR-054 quedan marcados «reemplazado parcialmente por ADR-055» | `scripts/validar_flujos.py` + revisión de Pablo | TODO |
| F0.M4 | ADR-056 «Tu garantía vuelve y la puntualidad vale plata» (modelo A + D). Reemplaza a ADR-051: sin respaldo empresarial, sin aval, con fondo del grupo segregado, recargo al perjudicado y orden de turnos por nivel | El ADR existe y ADR-051 queda marcado «reemplazado» | Revisión de Pablo | TODO |
| F0.M5 | Catálogo de flujos actualizado: `aporte-mora-cobertura.yaml` y `entrega-del-turno.yaml` sin `cubrir-faltante` ni `recuperar-respaldo`, con los pasos del modelo A | `scripts/validar_flujos.py` en verde con el catálogo nuevo | Salida del script | TODO |
| F0.M6 | Línea base de verificadores: correr una vez `scripts/verificar_*.py`, `yarn lint`, `yarn typecheck`, Gradle `test` por servicio y `flutter analyze` en la máquina de Pablo y guardar la salida | Existe la línea base contra la que se mide cada fase | Salidas en `evidencia/F0/linea-base/` | TODO |

---

## H1 — Seguridad e integridad del dinero (F1)

**CA:** Dado cualquier endpoint que mueve, aprueba o expone dinero o datos, cuando alguien sin el permiso, sin la propiedad del recurso o con datos inventados en el cuerpo lo llama, entonces recibe 401, 403 o 422 según el contrato, y no ocurre ningún efecto en el mayor ni en el estado.
**DoD:** la matriz de autorización negativa de cada endpoint (otro usuario, otro grupo, rol insuficiente, sin token) en verde contra la base real. A eso se suman las pruebas del mayor (S1.M1 a S1.M3) y los cinco puntos de la regla 91.6 para cada operación de dinero tocada.
**Estado:** TODO

### H1.S1 — El mayor es inviolable

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F1.S1.M1 | Trigger diferido de cuadre por asiento (`SUM(debe) = SUM(haber)`) y bloqueo de líneas nuevas en asientos confirmados (TYA-DINERO-001) | Un `INSERT` tardío o negativo sobre un asiento confirmado falla en la base | Prueba de integración que reproduce el caso de L5 y ahora falla con el error esperado | TODO |
| F1.S1.M2 | `REVOKE UPDATE, DELETE` sobre todas las tablas *append-only* para el rol de la aplicación (`sql/35_append_only`), verificado tabla por tabla | `UPDATE` y `DELETE` sobre esas tablas devuelven `permission denied` | Script que recorre la lista e intenta ambas operaciones | TODO |
| F1.S1.M3 | `CHECK monto >= 0` en cada importe de aporte, deducción, reembolso y movimiento, con su moneda al lado (L1 DINERO-001) | Un reembolso negativo falla en la base y en la API (422) | Prueba de integración + prueba de API | TODO |
| F1.S1.M4 | Índice `UNIQUE` sobre `clave_idempotencia` en toda tabla de operación de dinero que no lo tenga, y captura de la violación como respuesta idempotente (200 con el mismo `id`) | La misma operación enviada dos veces deja un solo efecto | Prueba de doble envío por cada operación | TODO |

### H1.S2 — La identidad sale del token, nunca del cuerpo

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F1.S2.M1 | Componente único en `plataforma/comun-web` que entrega el actor autenticado y su segundo actor (cuatro ojos reales: dos tokens distintos). Se elimina todo campo `aprobadoPor`, `autorizador`, `revisor` o similar de los DTO (L1 SEG-005, L2 SEG-003, L4 SEG-119, L5 DINERO-002) | Un cuerpo que trae un aprobador se rechaza con 422 por campo no declarado | Prueba de contrato + prueba de API con el campo inventado | TODO |
| F1.S2.M2 | Los datos de negocio se leen de su dueño, no del cliente: `grupoCerrado`, `deudasVivas`, días de mora, bruto y recaudado, monto de la entrega, `numeroCifrado`, descuentos (L2 SEG-004, DINERO-001, DINERO-003, SEG-007; L5 SEG-001) | Ninguno de esos campos existe en los DTO de entrada | Diff de los OpenAPI + pruebas de API | TODO |
| F1.S2.M3 | Política de autorización por propiedad del recurso en cada endpoint (BOLA): el organizador o miembro pertenece al grupo, la cuenta destino es del beneficiario, quien pide un retiro es el titular del cupo, la comisión es del usuario (L2 SEG-002, SEG-006; L3 SEG-001; L5 APROB-001) | Otro usuario u otro grupo recibe 403 o 404 | Matriz negativa automatizada (`scripts/verificar_seguridad.py`, ampliado) | TODO |

### H1.S3 — Acceso

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F1.S3.M1 | El inicio de sesión y la renovación del token verifican `usuario.estado`; suspender o dar de baja revoca las sesiones activas (L4 SEG-075) | Un usuario suspendido no obtiene ni renueva token, y su token vigente deja de servir | Prueba de integración: suspender, intentar entrar y renovar | TODO |
| F1.S3.M2 | Segundo factor real para operadores (TOTP con enrolamiento, secreto guardado como hash con *pepper*), sin código fijo en ningún entorno (L4 HUECO-077) | `000000` falla en todos los entornos | Prueba de enrolamiento y de verificación | TODO |

### H1.S4 — Importes en la interfaz

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F1.S4.M1 | `CampoMonto` (Flutter) analiza el separador decimal según el formato regional y nunca borra todos los puntos; el importe viaja como decimal exacto (L6 DINERO-002) | «12.50» se envía como `12.50`; «1.250,50» se envía como `1250.50` | Pruebas unitarias con casos límite + golden + prueba en dispositivo | TODO |
| F1.S4.M2 | El destino de una transferencia se resuelve por identificador único, no por alias visible con `LIMIT 1` (L3 DINERO-002) | Dos usuarios con el mismo alias no pueden recibir una transferencia ajena | Prueba de integración con alias duplicado | TODO |

El resto de los hallazgos de F1 (S2 a S4 de seguridad, dinero y trazabilidad) está en los anexos, con prefijo `F1.<LOTE>`.

---

## H2 — Retiro de lo que contradice el flujo nuevo (F2)

**CA:** Dado el sistema desplegado, cuando se busca cualquier camino por el que la empresa ponga plata por quien no pagó, o por el que una persona apruebe un paso del circuito del pasanaku, entonces no existe en el código, el esquema, las semillas, los contratos, los clientes, los mocks, las pruebas ni los textos.
**DoD:** `grep` de control sin resultados (patrones del kill-test, en `evidencia/F2/`) + suites de los servicios tocados en verde + `scripts/verificar_contratos_limites.py` en verde.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F2.M1 | Eliminar el respaldo empresarial en `garantia` y `entregas`: los `CU23*Respaldo*`, `RespaldoPorHttp`, `CoberturaAplicable` (empresa), `ContingenciaDeCaja`, `PlanDeContingencia`, endpoints `/garantia/respaldo/*`, la ruta del gateway, los eventos `respaldo_*` y `cobertura_empresarial_*` y sus pruebas | Ningún símbolo, ruta ni evento de respaldo existe | `grep -rniE 'respaldo|cubiertoEmpresa|cobertura_empresarial'` sin resultados en `servicios/` y `plataforma/` | TODO |
| F2.M2 | Eliminar el consumidor de asientos de respaldo y cobertura en `nucleo-financiero` y su configuración en `docker-compose.coolify.yml` (L1 RESPALDO-003, RS-04) | El consumidor no existe y el *topic* no se suscribe | `scripts/listar_topicos.py` sin `respaldo` | TODO |
| F2.M3 | Esquema: migración que elimina las tablas `*_respaldo*`, `movimiento_reserva`, `plan_contingencia`, `fondeo_entrega.monto_cubierto_empresa`, R-GAR-08..13 y R-DES-03, y las cuentas 9.x de las semillas. Antes verifica que no haya asientos en esas cuentas en ningún entorno; si los hubiera, los cierra con contra-asiento | La base no tiene esas tablas ni cuentas; la verificación de deriva queda en verde | Salida de la migración + verificación de deriva (regla 97.2) + consulta de asientos 9.x en TEST antes de migrar | TODO |
| F2.M4 | Contratos, pactos, clientes generados (`clientes/*`) y mocks (`packages/simulado`) sin respaldo, regenerados desde el OpenAPI | `cubiertoEmpresa` y `RespaldoApi` no existen | Regeneración + `yarn typecheck` + `flutter analyze` | TODO |
| F2.M5 | Eliminar el aval entre participantes (`CU26EjecutarAval`, `aval_participante`, `referencia_personal.acepta_ser_avalista`, pruebas y textos) | No existe | `grep` + suites | TODO |
| F2.M6 | Eliminar el organizador profesional: postulaciones, habilitación (básica y profesional), contrato, evaluación de desempeño, CU-90, rol `ORGANIZADOR` y sus pantallas de backoffice y app. El servicio `organizador` se reduce a lo que sobreviva (automatización) o se retira con ADR (regla 98.7) | No hay camino que exija ser organizador para crear un grupo | Prueba: un participante con KYC crea un grupo | TODO |
| F2.M7 | Eliminar el comprobante manual y los canales manuales (`comprobante_manual`, `pago.es_manual`, `EFECTIVO_AL_ORGANIZADOR`, transferencia declarada, `REPORTADO_POR_USUARIO`, `EN_VERIFICACION`, la pantalla «declarar aporte» y `POST /aportes/obligaciones/{id}/pagos` declarado) | El único camino que cambia una cuota a cobrada es el webhook firmado o el débito de saldo (F3) | Prueba: el endpoint viejo devuelve 404 o 410 | TODO |
| F2.M8 | Eliminar las aprobaciones ELIMINABLES: admisión humana, cuatro ojos de la entrega, ejecución y fondeo manual de la entrega, confirmación manual del desembolso, doble aprobación de retiros, cobro de comisión marcado a mano, retiro voluntario `PENDIENTE` sin aprobador, R-ORG-06 para entrega, cobro y cobertura. Se conservan, de forma real (F1.S2.M1): reembolsos, reversos, cierre de billetera, PEP, LFT, UIF, requerimientos y sanciones | El inventario de aprobaciones del consolidado §5 queda con cada fila resuelta | Tabla del §5 actualizada con el estado de cada fila | TODO |
| F2.M9 | Eliminar la «cobertura del fondo» vieja (fondo mutual actual sin asiento, canal `FONDO_GARANTIA`, `monto_cubierto_garantia`, `politica_cobertura` vieja), que se reemplaza por el modelo A de F3 | No queda código del fondo viejo | `grep` + suites | TODO |

---

## H3 — Flujo nuevo (F3)

**CA:** Dado un grupo de 3 participantes con datos sintéticos, cuando todos pagan por QR (doble de pasarela con firma real), entonces cada pago queda cobrado y asentado, y la entrega sale sola a la cuenta verificada del beneficiario. Cuando uno no paga, el fondo del grupo cubre, nace su deuda con el fondo, el descargo se abre automáticamente y el recargo se acredita al perjudicado. Al cierre, cada participante recupera su aporte al fondo menos su parte de lo incobrable.
**DoD:** E2E de saga contra la base real en la máquina de Pablo, con los cinco puntos de la regla 91.6 y los seis de la regla 98.8 (incluida la caída de cada servicio de la saga, apagando su contenedor), trazado con `correlationId`.
**Estado:** TODO

### H3.S1 — QR → webhook → cobrado (servicio `aportes`)

Diseño: un puerto `Pasarela` con el adaptador real (cuando se contrate) y un doble declarado con firma HMAC real (regla 65). El webhook (`CU100RecibirWebhookPasarela`) verifica la firma **siempre**, también en desarrollo, y valida la ventana de tiempo, la orden existente y vigente, el monto y la moneda exactos y la idempotencia por id de evento de la pasarela. Si todo está bien, publica `aporte_cobrado` por outbox. `nucleo-financiero` lo asienta y lo aplica al pozo.

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F3.S1.M1 | Orden de cobro con QR, vencimiento y `clave_idempotencia` | Una orden vencida no acepta pagos | Pruebas de unidad e integración | TODO |
| F3.S1.M2 | Webhook con firma verificada y los casos límite | Correcto: cobra una vez. Límite: el mismo evento dos veces cobra una sola vez; un evento al borde de la ventana se acepta. Inválido: firma mala, monto distinto, orden inexistente o vencida, todos rechazados y sin efecto | Prueba de los tres niveles contra el doble (regla 65) | TODO |
| F3.S1.M3 | `aporte_cobrado` por outbox; consumidor idempotente en `nucleo-financiero`; compensación si no puede aplicarse; DLT configurado | El mensaje duplicado se aplica una vez; el fallo termina en compensación, no en silencio | Prueba de duplicado + prueba de caída de `nucleo-financiero` | TODO |
| F3.S1.M4 | Pago de la cuota con saldo de billetera (recargado por QR) y pago automático opcional | Débito y crédito en un solo asiento, idempotente | Asientos pegados + prueba de doble envío | TODO |
| F3.S1.M5 | El «confirmado» del pozo se lee del mayor, no de `monto_pagado` (L1 FLUJO-QR-003) | El recaudo coincide con el mayor | Consulta de cuadre pegada | TODO |
| F3.S1.M6 | Pago que llega después del corte: se aplica a la deuda con el fondo (H3.S3) si la hay; si no, a la cuota pendiente | Nunca queda dinero sin aplicar | Prueba de pago tardío | TODO |

### H3.S2 — Entrega automática (servicio `entregas`)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F3.S2.M1 | Máquina de estados de la entrega sin pasos humanos: pozo completo (cobrado más cubierto por el fondo) → orden de desembolso → enviada → acreditada por webhook firmado del proveedor | Ningún endpoint mueve la entrega de estado a mano | Prueba de saga | TODO |
| F3.S2.M2 | Puerto `ProveedorDeDesembolso` + doble con firma real en tres niveles | Igual que F3.S1.M2, para el desembolso | Prueba de los tres niveles | TODO |
| F3.S2.M3 | Desembolso solo a una cuenta verificada del beneficiario, con enfriamiento tras un cambio de cuenta (parámetro obligatorio) | Una cuenta cambiada dentro del enfriamiento bloquea el desembolso con un motivo visible | Prueba de integración | TODO |
| F3.S2.M4 | Deducciones línea por línea; suma de deducciones ≤ bruto garantizada en la base | La entrega neta explica cada línea | Asientos pegados | TODO |

### H3.S3 — Modelo A «Tu garantía vuelve» (servicio `garantia`)

Diseño de cuentas, a validar con el contador antes de implementar (`DECISION_REQUIRED` sobre los nombres del plan de cuentas):

| Hecho | Debe | Haber |
|---|---|---|
| Aporte al fondo con la cuota | Caja custodia por liquidar | Fondo del grupo (pasivo con el participante) |
| Cobertura de una cuota impaga | Fondo del grupo | Pozo del grupo |
| Nace la deuda del moroso | Cuenta por cobrar al moroso (a favor del fondo) | Fondo del grupo: cobertura por recuperar |
| El moroso paga su deuda | Caja custodia | Cuenta por cobrar al moroso |
| Devolución al cierre | Fondo del grupo | Caja custodia → desembolso a cada participante |
| Incobrable al cierre | Fondo del grupo (reparto en partes iguales) | Cuenta por cobrar al moroso (castigo, que no borra la deuda) |

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F3.S3.M1 | Fondo del grupo en cuenta segregada, porcentaje parametrizado por nivel de puntualidad, saldo leído del mayor | Ningún asiento del fondo toca cuentas de la empresa | Asientos pegados | TODO |
| F3.S3.M2 | Cobertura automática al vencer el plazo de gracia, idempotente por obligación, con límite igual al saldo del fondo verificado dentro de la transacción y bloqueo | Dos coberturas simultáneas de la misma obligación dejan un solo efecto; sin saldo, no se cubre y la entrega espera con el motivo visible | Prueba de concurrencia real | TODO |
| F3.S3.M3 | Subrogación: el moroso queda debiéndole al fondo; lo que paga se imputa primero a esa deuda | La deuda existe y se ve | Prueba de integración | TODO |
| F3.S3.M4 | Recargo por mora automático al vencer el plazo de gracia, anulable con contra-asiento si prospera el descargo, acreditado **al perjudicado** | El recargo nunca llega a cuentas de la empresa | Asientos pegados + prueba de reversa | TODO |
| F3.S3.M8 | Residuo de redondeo en las devoluciones y en el reparto de lo incobrable: de a un centavo, empezando por el de mayor nivel de puntualidad, con desempate por el sorteo del grupo | La suma repartida es igual al total y nunca se pierde un centavo | Prueba con divisiones inexactas | TODO |
| F3.S3.M5 | Cierre del ciclo: devolución a cada uno de su aporte menos su parte del incobrable, calculada desde el mayor; cada devolución es un desembolso con las reglas de F3.S2 | El fondo queda en cero y cuadra | Consulta de cuadre + asientos | TODO |
| F3.S3.M6 | Métrica de solvencia del fondo por grupo, con alerta | Hay alerta cuando el saldo baja del umbral (parámetro) | Prueba de la alerta | TODO |
| F3.S3.M7 | Opinión de Cumplimiento sobre el fondo segregado (captación) y sobre la cobertura con subrogación sin expediente | Respuesta escrita archivada | Documento en `evidencia/F3/` | DECISION_REQUIRED |

### H3.S4 — Grupos: invitación, inicio y sorteo automáticos (servicio `grupos`)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F3.S4.M1 | Ingreso solo por enlace de invitación de un miembro, con token de propósito único y vencimiento; requisitos verificados en el servidor (KYC aprobado, reglamento aceptado en su versión exacta, sin deudas vivas) | Sin invitación válida no se entra; con requisitos incompletos, 422 con el motivo | Pruebas de API, incluido el token reutilizado | TODO |
| F3.S4.M2 | Trabajo programado (ShedLock, timeout y reintento) que inicia el grupo al completarse los cupos | El grupo arranca sin intervención humana | Prueba de integración del trabajo | TODO |
| F3.S4.M3 | Sorteo con compromiso y revelación; orden por nivel de puntualidad (el nivel más alto elige primero; dentro del mismo nivel, sorteo); lista congelada; herramienta de verificación pública | Cualquier participante recalcula el orden y le da igual | Salida de la herramienta (skill `rosca-group-lifecycle` §3) | TODO |
| F3.S4.M4 | Retiro voluntario: quien no cobró recupera lo aportado (cuotas y fondo) al cierre del ciclo; quien ya cobró no se retira sin resolver su deuda | No queda en `PENDIENTE` para siempre | Prueba | TODO |
| F3.S4.M5 | Votaciones del grupo, solo para el reemplazo por invitación (el entrante toma el cupo con su turno y sus obligaciones) y la disolución (liquidación desde el mayor); quórum según el reglamento aceptado; nunca se aprueba por silencio | Sin quórum no se aprueba; el reemplazo no mueve el calendario | Prueba | TODO |
| F3.S4.M6 | Eliminar la subasta y la cesión de turno, la permuta y la condonación por voto | No existen | `grep` + suites | TODO |

### H3.S5 — Identidad y cumplimiento

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F3.S5.M1 | Puerto `VerificadorDeIdentidad` (proveedor automático, sin contratar todavía: doble en tres niveles); solo las excepciones y los PEP van a la bandeja de Cumplimiento; la app deja de consultar cada 30 s y escucha el estado | Un KYC claro se aprueba sin persona; uno dudoso va a la bandeja | Prueba de los tres niveles | TODO |
| F3.S5.M2 | Retiros automáticos con controles: segundo factor, cuenta verificada, enfriamiento, monitoreo UIF; solo las alertas van a Cumplimiento; ADR-049 reemplazado | Un retiro normal sale sin persona; uno con alerta queda retenido con su motivo | Prueba de integración + prueba de alerta | TODO |
| F3.S5.M3 | Detección UIF disparada de verdad (hoy los reportes salen en cero), consumidores de los eventos de cumplimiento, bloqueo por autoridad efectivo (L4 FLUJO-FALTANTE-117, 118, 123) | Un caso sintético genera alerta, caso y reporte | E2E de cumplimiento | TODO |

### H3.S6 — Debido proceso

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F3.S6.M1 | El sistema declara el incumplimiento al vencer el plazo de gracia y abre un expediente con plazo de descargo (parámetro) | No se baja la reputación ni se sanciona antes de que el expediente quede `FIRME` | Prueba con reloj controlado | TODO |
| F3.S6.M2 | Soporte o Riesgos resuelve el descargo; la apelación la resuelve otra persona (cuatro ojos reales, F1.S2.M1); el reclamo abierto suspende los efectos | Quien resolvió no puede resolver la apelación | Prueba de autorización negativa | TODO |

### H3.S7 — Calificación y gamificación (servicio `transparencia`, pieza D)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F3.S7.M1 | La calificación se calcula solo con hechos: pagos confirmados por QR y su fecha, ciclos completos, expedientes firmes. Sin calificaciones entre personas. Se puede reclamar un error | Dos personas con los mismos hechos tienen la misma calificación | Pruebas de unidad del cálculo | TODO |
| F3.S7.M2 | Niveles con beneficios concretos (porcentaje menor de aporte al fondo, prioridad de turno); los nombres y umbrales son parámetros | Subir de nivel cambia el porcentaje del fondo en el ciclo siguiente | Prueba | TODO |
| F3.S7.M3 | Rachas (recuperables por debido proceso), insignias y logro de grupo «Grupo perfecto» (el fondo se devuelve antes y suben todos) | El logro se otorga una vez por ciclo y es idempotente | Prueba | TODO |
| F3.S7.M4 | Pantallas de nivel, racha y avance del grupo («8 de 10 pagaron») con los cuatro estados, WCAG 2.2 AA, movimiento reducido y modo claro y oscuro; quién falta se ve según el reglamento aceptado | Capturas en 3 *viewports* y 2 temas | Prueba visual (regla 95.7) | TODO |

### H3.S8 — Textos y parámetros

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F3.S8.M1 | Textos nuevos de la web pública, el FAQ, el JSON-LD, el tour de la app, los errores y las plantillas: sin «el fondo cubre la entrega» ni «una persona revisa tu ingreso»; explican el modelo A + D. Los redacta Claude y los aprueba Pablo | Pablo aprueba cada texto | `grep` de frases prohibidas sin resultados + aprobación registrada | TODO |
| F3.S8.M2 | Registro central de parámetros financieros, **editable desde el backoffice**: versionado y con fecha de vigencia, con bitácora de quién cambió qué. Ningún parámetro queda fijo en el código ni en un `.yml`. Mientras no esté cargado, la operación que lo usa falla con un error explícito | Un parámetro sin cargar bloquea la operación y nombra lo que falta; un cambio queda auditado | Prueba de integración + prueba de autorización negativa sobre la pantalla | TODO |
| F3.S8.M5 | Comisión: porcentaje único sobre la cuota, cobrado en el mismo QR como línea aparte; nunca sale del pozo; sin descuentos ni exenciones | La línea de comisión se ve y se asienta aparte | Asientos pegados | TODO |
| F3.S8.M6 | Los parámetros se congelan al iniciar cada grupo; un cambio rige solo para grupos nuevos y necesita dos personas distintas (quien propone ≠ quien aprueba, ambas del token) | Un grupo en curso no cambia | Prueba de integración + autorización negativa | TODO |
| F3.S8.M4 | Pantalla de parámetros en el backoffice: los cuatro estados, formulario con error por campo, historial de versiones y vista de «qué grupos afecta» | Capturas en 3 *viewports* y 2 temas | Prueba visual + E2E | TODO |
| F3.S8.M3 | Pablo y Cumplimiento fijan los valores de los parámetros, con procedencia registrada | Valores con fuente y fecha | Documento firmado | DECISION_REQUIRED |

---

## H4 — Calidad y deuda (F4)

**CA:** Dado cada hallazgo S2 a S4 restante, cuando se cierra su microtarea del anexo, entonces el verificador correspondiente lo confirma y no reaparece.
**DoD:** las microtareas `F4.<LOTE>.Mnn` de los anexos, cada una con su DoD.
**Estado:** TODO

Contenido, agrupado por clase:
- **Contratos:** matriz de la §6 de cada informe sin discrepancias y prueba de contrato del lado productor en CI.
- **Datos:** claves foráneas, `CHECK`, nulabilidad, índices con nombre y semillas idempotentes con procedencia.
- **Resiliencia:** timeouts, circuit breakers, outbox y DLT.
- **Observabilidad:** `correlationId`, `service`, `version`, `actorId`, enmascarado de datos personales y bitácora de lectura separada.
- **Pruebas:** autorización negativa, idempotencia, sin mocks presentados como integración y sin pruebas que validen el flujo viejo.
- **UX/a11y:** los cuatro estados, formularios y WCAG.
- **Código:** clean code y ARQ (límite de 300 líneas, capas hexagonales), duplicaciones y reescrituras muertas.
- **Documentación:** ADR, casos de uso y catálogos alineados.
- **Configuración y dependencias.**

## H5 — Piloto «Poder de compra» (F5, condicionado)

**Ya, en paralelo a F1–F3:** el diseño (ADR-057, contrato tipo con comercios, pantallas). **Condición para empezar el código:** la opinión legal sobre la clasificación ante ASFI y la garantía sobre bienes, y al menos un comercio firmado. Sin esas dos, la fase queda en `DECISION_REQUIRED`.

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F5.M1 | Comercio verificado: alta de la empresa (KYB), cuenta verificada, contrato con descuento y precio congelado durante el ciclo | Un comercio sin verificar no aparece | Prueba | DECISION_REQUIRED |
| F5.M2 | Grupo con meta asociada a un producto del catálogo curado (3 a 5 comercios, 1 o 2 categorías) | El precio queda congelado al crear el grupo | Prueba | DECISION_REQUIRED |
| F5.M3 | El pozo pasa a ser «saldo de compra»: canje con QR que escanea el comercio al entregar el producto, y la app le paga en ese momento; la opción de efectivo existe siempre | Si el comercio no entrega, no cobra | E2E | DECISION_REQUIRED |
| F5.M4 | Turno temprano para personas sin historial solo pagando cuotas por adelantado | Baja la exposición sin cobrar interés | Prueba | DECISION_REQUIRED |
| F5.M5 | Comisión al comercio por venta | Nunca se cobra al ahorrista | Asientos pegados | DECISION_REQUIRED |

## Criterio para pedir la habilitación de producción (entrada al dossier H15)

1. Los 32 S1 en `HECHO`, con la evidencia de la regla 91.6 y la regla 98.8 pegada.
2. Los S2 de las clases SEG, DINERO, FLUJO-QR, RESPALDO, APROB, TRAZA y REG en `HECHO`.
3. El kill-test de este plan da los cuatro resultados esperados: sin efecto en (1), (2) y (3), y un rechazo de la base en (4).
4. El E2E de H3 en verde, con caída de cada dependencia ejercitada.
5. Los parámetros financieros fijados con procedencia (F3.S8.M3) y las opiniones de Cumplimiento archivadas (F3.S3.M7).
6. Una prueba de penetración externa sobre la API y la app (OWASP API Top 10 y MASVS), con los hallazgos críticos y altos cerrados.

## Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin pasarela ni proveedor de desembolso contratados | No se puede verificar contra el colaborador real | Dobles con firma real en tres niveles (regla 65); quedan registrados como «verificado contra doble» hasta contratar |
| Docker solo en la máquina de Pablo | Las pruebas de integración dependen de que la máquina esté en línea | Agrupar las corridas por fase; las pruebas de unidad y de contrato corren en la nube |
| Cumplimiento clasifica el fondo del grupo como captación | Hay que rediseñar el modelo A | F3.S3.M7 va antes de implementar F3.S3; F1 y F2 no dependen de eso |
| Eliminar el servicio `organizador` rompe consumidores | Hay regresiones en `grupos` y `transparencia` | F2.M6 empieza con un inventario de consumidores (`scripts/inventario_endpoints.py`, `listar_topicos.py`) |
| 474 hallazgos con duplicados entre lotes | Trabajo hecho dos veces | Cada anexo marca «se resuelve con»; las microtareas transversales de este plan tienen prioridad sobre las de los anexos |

---

## Índice de anexos (detalle por hallazgo)

Cada anexo convierte cada ficha de su informe en una microtarea de raíz, con su causa, su corrección, el CA y el DoD. Al final, cada anexo trae la tabla de trazabilidad «hallazgo → microtarea», verificada por script: no falta ninguna ficha.

| Lote | Anexo | Fichas | Microtareas | F0 | F1 | F2 | F3 | F4 |
|---|---|---|---|---|---|---|---|---|
| L1 | [ANEXO-L1-aportes-nucleo-financiero.md](./anexos/ANEXO-L1-aportes-nucleo-financiero.md) | 45 | 44 | 0 | 13 | 4 | 20 | 7 |
| L2 | [ANEXO-L2-garantia-entregas.md](./anexos/ANEXO-L2-garantia-entregas.md) | 68 | 59 | 4 | 14 | 9 | 14 | 18 |
| L3 | [ANEXO-L3-grupos-organizador.md](./anexos/ANEXO-L3-grupos-organizador.md) | 55 | 27 | 2 | 7 | 6 | 7 | 5 |
| L4 | [ANEXO-L4-identidad-cumplimiento.md](./anexos/ANEXO-L4-identidad-cumplimiento.md) | 176 | 134 | 5 | 26 | 2 | 12 | 89 |
| L5 | [ANEXO-L5-transversal-y-apoyo.md](./anexos/ANEXO-L5-transversal-y-apoyo.md) | 88 | 82 | 5 | 26 | 4 | 9 | 38 |
| L6 | [ANEXO-L6-frontends-compartidos.md](./anexos/ANEXO-L6-frontends-compartidos.md) | 42 | 40 | 2 | 10 | 2 | 5 | 21 |
| **Total** | | **474** | **386** | 18 | 96 | 27 | 67 | 178 |

Notas de integración:
- Las microtareas de este plan maestro (`F1.S2.M1`, `F2.M1`, etc.) son transversales y **van primero**. Cuando una microtarea de un anexo dice «se resuelve con» o depende de «coordinar con el lote Lx», manda la microtarea del plan maestro.
- Los anexos se escribieron mientras se decidían las últimas ambigüedades: D-L2-1, D-L2-2, D-L2-3, D-L3-1, D-L3-2, D-L3-3, D-L5-1, D-L5-2, AMB-L1-1 y AMB-L1-2 ya están **decididas** en `DECISIONES.md`, tanda 7, y esa decisión manda sobre el supuesto que dejó el anexo. Las restantes (AMB-L1-3, AMB-L4-1, AMB-L4-2, AMB-L4-3, AMB-L4-4 y D-L5-3) pasaron a `PREGUNTAS-LEGALES.md`.
- Hay excepciones al orden de fases, declaradas en cada anexo:
  - F2.L3.M15 (retirar el inicio y el sorteo manuales) se mergea **después** de F3.L3.M18 (inicio y sorteo automáticos).
  - F3.L4.M06 necesita adelantar F4.L4.M53.
  - F3.L4.M09 depende de F4.L4.M25.
- Algunos S1 se **cortan en F1**, donde se cierra la parte explotable, y se rehacen en F3 con el flujo nuevo. Por ejemplo: L1 FLUJO-QR-001, L2 SEG-003, SEG-004 y SEG-005.
- L6 tiene 40 candidatos sin verificar (su §5-bis), que se revisan en F4.L6.M40.

## Ejecución (decisión de Pablo: sesiones en paralelo por fase)

1. **F0:** una sesión. Usa la máquina de Pablo para Docker (F0.M2).
2. **F1:** primero, una sesión para las transversales: `F1.S1.*` (esquema del mayor) y `F1.S2.M1` (actor del token en `comun-web`). Después, hasta 4 sesiones en paralelo, con archivos disjuntos: {L1 aportes/nucleo}, {L2 garantía/entregas}, {L3 + L5 servicios de apoyo}, {L4 identidad/cumplimiento}; L6 se suma a la que tenga menos carga.
3. **F2:** el mismo reparto, después de F1.
4. **F3:** cuatro carriles, {cobro QR + entrega}, {modelo A en garantía}, {grupos + transparencia/gamificación}, {identidad/cumplimiento + parámetros en backoffice}. El diseño de «Poder de compra» (ADR-057, contrato tipo y pantallas) corre en paralelo desde F1.
5. **F4:** por lote, en paralelo.
6. Cada sesión trabaja en su propia rama, abre un PR por cambio terminado hacia `dev` y escribe su `PLAN.md` y su `REPORTE.md` con evidencia. Las pruebas que necesitan Docker se agrupan por fase y corren en la máquina de Pablo por Remote Control. Pablo revisa y mergea.
